package com.universal.signals.action;

// Java 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// DOM4j
import org.dom4j.Element;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.commerce.catalog.ProductAttributeVO;
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.NavManager;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.commerce.product.ProductCatalogAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.universal.util.WebServiceAction;

/****************************************************************************
 * <b>Title</b>: USAProductAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Jan 27, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class ProductAction extends SBActionAdapter {
	
	public static final String STATUS_AVAILABLE = "Available";
	public static final String STATUS_SOLD_OUT = "Sold Out";
	public static final String PARAM_DETAIL = "detail";
	public static final String PARAM_FEATURED = "featured";

	/**
	 * 
	 */
	public ProductAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public ProductAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	public void build(ActionRequest req) throws ActionException {

	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(ActionRequest req) throws ActionException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		String sitePrefix = site.getSiteId() + "_";
		log.debug("sitePrefix: " + sitePrefix);
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		// get the catalog ID from the module attributes
		String catalogId = StringUtil.checkVal(mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		mod = null;
		// build the category url value that has been requested
		String cat = this.buildCatUrlFromQueryString(req);
		log.debug("catalogId, catUrl: " + catalogId + ", " + cat);
		
		try {
			String cat1 = StringUtil.checkVal(req.getParameter(ActionRequest.PARAMETER_KEY + "1"));
			log.debug("paramKey1 (cat1): " + cat1);
			if (PARAM_DETAIL.equalsIgnoreCase(cat1)) {
				// retrieve product detail
				log.debug("retrieving product detail info...");
				ProductVO detail = this.retrieveProductDetail(req, catalogId, sitePrefix);
				
				if (detail.getProductId() != null) {
					// found product detail, so check availability
					this.checkAvailability(req, detail);
				}
				
				this.putModuleData(detail, 1, false);
				req.setAttribute("usaProdDetail", Boolean.TRUE);
				
			} else if (PARAM_FEATURED.equalsIgnoreCase(cat1)) {
				// retrieve featured category product list
				log.debug("retrieving featured category product list...");
				ProductCategoryVO pCat = this.retrieveCategoryProductList(req, catalogId, cat, false);
				this.putModuleData(pCat, pCat.getProducts().size(), false);
				req.setAttribute("usaProdFeatured", Boolean.TRUE);
				
			} else {
				// retrieve the category
				List<ProductCategoryVO> data = this.retrieveCat(req, catalogId, cat);
				
				if (data.size() == 0) {
					// no data found from retrieving category
					if (cat.indexOf("|") > -1) {
						// attempt to retrieve subcategories for this category
						data = this.retrieveSubCat(req, catalogId, cat);
						
						if (data.size() == 0) {
							// no subcategory data found, so look for products belonging to this category
							log.debug("retrieve product list info");
							Collection<ProductVO> prods = this.retrieveProductList(req, catalogId, cat, true);
							
							// no products were found, look for group data
							if (prods.size() == 0) {
								prods = this.getGroupInfo(req, catalogId);
							}
							this.putModuleData(prods, prods.size(), false);
							req.setAttribute("usaProdList", Boolean.TRUE);
							
						} else {
							// found subcategory data
							this.putModuleData(data, data.size(), false);
							
						}
					} else {
						// no subcategories exist, check for products
						Collection<ProductVO> prods = this.retrieveProductList(req, catalogId, cat, true);
						
						if (prods.size() == 0) {
							// no products were found, so check for group data
							prods = this.getGroupInfo(req, catalogId);
						}
						// return product list data
						this.putModuleData(prods, prods.size(), false);
						req.setAttribute("usaProdList", Boolean.TRUE);
					}
					
				} else {
					// found category data
					this.putModuleData(data, data.size(), false);
				}
				log.debug("data size: " + data.size());
			}
		} catch (Exception e) {
			log.error("Unable to retrieve catalog data", e);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	/**
	 * Loops the parameter keys and builds and category URL based on those keys.
	 * @param req
	 * @return
	 */
	private String buildCatUrlFromQueryString(ActionRequest req) {
		String cat = "";
		String qsParam = null;
		for (int i = 1; i < 11; i++) {
			//log.debug("evaluating cat qs param #" + i);
			qsParam = StringUtil.checkVal(req.getParameter(ActionRequest.PARAMETER_KEY + i));
			if (qsParam.length() == 0) break; // if param key has no value, we're done.
			if (i == 1) {
				// if 'detail' or 'featured' skip param 1.
				if (qsParam.equalsIgnoreCase(PARAM_DETAIL) || 
						qsParam.equalsIgnoreCase(PARAM_FEATURED)) {
					continue;
				} else {
					cat = qsParam;
				}
			} else {
				cat = cat + "|" + qsParam;
			}
		}
		// remove the pipe if it is at position 0
		if (cat.indexOf("|") == 0 && cat.length() > 0) cat = cat.substring(1);
		return cat;
	}
	
	/**
	 * 
	 * @param req
	 * @param catalogId
	 * @return
	 * @throws SQLException
	 */
	private Collection<ProductVO> getGroupInfo(ActionRequest req, String catalogId) 
	throws SQLException {
		String url = StringUtil.checkVal(req.getRequestURL().toString());
		String cat = url.substring(url.lastIndexOf("/") + 1);
		String s = "select * from product where parent_id = ? and product_catalog_id = ? AND PRODUCT_GROUP_ID IS NULL ";
		log.debug("group info SQL: " + s + "|" + cat + "|" + catalogId);
		Map<String, ProductVO>  data = new LinkedHashMap<String, ProductVO> ();
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		ps.setString(1, cat);
		ps.setString(2, catalogId);

		ResultSet rs = ps.executeQuery();
		String inQuery = "'test'";
		String prodId = null;
		for (int i=0; rs.next(); i++) {
			// set the page info
			if (i == 0)
				this.setPageData(req, rs.getString("title_nm"), rs.getString("meta_kywd_txt"), rs.getString("meta_desc"));
			
			prodId = rs.getString("product_id");
			//prodId = StringUtil.replace(rs.getString("product_id"), catalogPrefix, "");
			inQuery += ",'" + prodId + "'";
			data.put(prodId, new ProductVO(rs));
		}
		
		getProductAttributes(data, inQuery);
		return data.values();
	}
	
	/**
	 * 
	 * @param req
	 * @param catalogId
	 * @return
	 * @throws SQLException
	 */
	private ProductVO retrieveProductDetail(ActionRequest req, 
			String catalogId, String sitePrefix) throws SQLException {
		String siteProductId = sitePrefix + req.getParameter(ActionRequest.PARAMETER_KEY + "2");
		StringBuilder s = new StringBuilder();
		s.append("select * from product a ");
		s.append("left outer join product_attribute_xr b on a.product_id = b.product_id ");
		s.append("left outer join product_attribute c on b.attribute_id = c.attribute_id ");
		s.append("where a.product_catalog_id = ? and a.product_id = ? AND a.PRODUCT_GROUP_ID IS NULL ");
		s.append("order by a.product_id, b.attribute_id, attrib2_txt, order_no");
		log.debug("Product Detail SQL: " + s + "|" + siteProductId);
		
		PreparedStatement ps = null;
		ProductVO product = null;
		List<ProductAttributeVO> primaryAttributes = new ArrayList<>();
		List<ProductAttributeVO> secondaryAttributes = new ArrayList<>();
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, catalogId);
			// prefix product ID with site prefix as product IDs in the PRODUCT table
			// are prefixed upon import to ensure uniqueness
			ps.setString(2, siteProductId);
			String aLevel = null;
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (product == null) { 
					product = new ProductVO(rs);
					// set the vo's product ID to the custom product number for JSTL use
					product.setProductId(product.getCustProductNo());
				}
	
				// put all product attributes in a List for later processing.
				if (rs.getString("product_attribute_id") != null) {
					aLevel = rs.getString("attrib2_txt");
					//log.debug("product_attribute_id | aLevel: " + rs.getString("product_attribute_id") + "|" + aLevel);
					switch(aLevel) {
						case "0":
						case "1":
							// add all level '0' or level '1' attributes
							primaryAttributes.add(new ProductAttributeVO(rs));
							//log.debug("added primary attribute: " + rs.getString("VALUE_TXT"));
							break;
						case "2":
							// add ONLY 1 of the level '2' attributes so we have a placeholder
							if (secondaryAttributes.size() == 0) {
								secondaryAttributes.add(new ProductAttributeVO(rs));
								//log.debug("added secondary attribute: " + rs.getString("VALUE_TXT"));
							}
							break;
						case "3":
							// add ONLY 1 of the level '3' attributes so we have a placeholder
							if (secondaryAttributes.size() < 2) {
								secondaryAttributes.add(new ProductAttributeVO(rs));
							}
							break;
						}
				}
			}
		} finally {
			DBUtil.close(ps);
		}

		// if we found no products for a category, initialize an empty ProductVO
		if (product == null) product = new ProductVO();
		
		/* Sort attributes and add them to product VO according to business rules.*/
		processProductAttributes(product, primaryAttributes, secondaryAttributes);
		primaryAttributes = null;
		secondaryAttributes = null;
		this.setPageData(req, product.getTitle(), product.getMetaKywds(), product.getMetaDesc());
		return product;
	}
	
	/**
	 * Processes product attributes for the product VO passed in.  The product
	 * attributes master list is sorted by a custom comparator.  Then the List is
	 * looped through to separate standard from custom attributes.   Standard attributes
	 * are added to the product's product attribute map (key = attributeName) first
	 * and then custom attributes are added.  Standard attributes MUST be added
	 * first and custom attributes MUST - and both must be added in sequence (standard
	 * attributes sequenced by attrib2_txt and then display_order_no, and custom
	 * attributes sequenced by value_txt).  This is critical for displaying the attributes
	 * on the JSTL view properly and for ensuring that the attributes are specified
	 * in the correct sequence when an order request XML block is generated.
	 * @param product
	 * @param pAttributes
	 */
	private void processProductAttributes(ProductVO product, List<ProductAttributeVO> pAttributes,
			List<ProductAttributeVO> sAttributes) {
		log.debug("processProductAttributes...");
		
		/* Sort to guarantee order by attrib2 text value (standard options) or
		 * by value_txt value (custom options). */
		Collections.sort(pAttributes, new ProductAttributeComparator());
		List<ProductAttributeVO> custom = new ArrayList<>();
		List<ProductAttributeVO> standard = null;
		List<String> attrKeysList = new ArrayList<>();
		String prevStd = null;
		String currStd = null;
		
		for (ProductAttributeVO p : pAttributes) {
			if (p.getAttributeName().equalsIgnoreCase("custom")) {
				custom.add(p);
			} else {
				currStd = p.getAttributeName();
				if (currStd.equals(prevStd)) {
					standard.add(p);
				} else {
					// add List to product attribute map, attribute name is the key
					if (standard != null) 	{
						product.addProdAttribute(prevStd, standard);
						attrKeysList.add(prevStd);
					}
					// re-initialize the standard List and add current attribute
					standard = new ArrayList<>();
					standard.add(p);
				}
				prevStd = currStd;
			}
		}
		
		// clean up any dangling standard attributes and add standard option to 
		// product attribute map, attribute name is the key.
		if (standard != null && standard.size() > 0) {
			product.addProdAttribute(prevStd, standard);
			attrKeysList.add(prevStd);
		}
		
		/*
		 * Loop the secondary attributes and add a key for each additional unique 
		 * attribute.  These represent the 2nd and 3rd-level attributes which we
		 * manipulate/populate in the JSTL.
		 */
		processSecondaryAttributes(product, sAttributes, attrKeysList);
		
		// ALWAYS LAST! : now add the custom list to the product attribute map using specified key
		if (custom.size() > 0) {
			product.addProdAttribute("custom", custom);
		}
	}
	
	/**
	 * Loops the secondary attributes list and adds a key/value to the product's
	 * product attributes map representing the additional levels of product 
	 * attribute hierarchy for the product.
	 * @param product
	 * @param sAttributes
	 */
	private void processSecondaryAttributes(ProductVO product, 
			List<ProductAttributeVO> sAttributes, List<String> attrKeysList) {
		log.debug("processSecondaryAttributes...");
		if (sAttributes == null || sAttributes.isEmpty()) return;
		
		// add a mapping for each of the secondary attribute types
		String keyName = null;
		int count = 1;
		for (ProductAttributeVO pAttr : sAttributes) {
			keyName = pAttr.getAttributeName();
			if (attrKeysList.contains(keyName)) {
				keyName = keyName+"-DUPE"+count;
				attrKeysList.add(keyName);
			}
			product.addProdAttribute(keyName, new ArrayList<>());
		}

	}
	
	/**
	 * Calls web service to check availability of the product being selected.
	 * @param req
	 * @param product
	 */
	private void checkAvailability(ActionRequest req, ProductVO product) {
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		WebServiceAction wsa = new WebServiceAction(this.actionInit);
		wsa.setAttributes(attributes);
		wsa.setAttribute(WebServiceAction.CATALOG_SITE_ID, site.getSiteId());
		Element stockReq = null;
		try {
			stockReq = wsa.checkProductAvailability(product);
		} catch (Exception e) {
			log.error("Error retrieving product availability from WebService, ", e);
			// default behavior in case of error is to allow product to be 'available'
			product.setStatusMsg(STATUS_AVAILABLE);
			return;
		}
		this.parseAvailability(product, stockReq);
	}
	
	/**
	 * Helper method to parse the response from the web service
	 * @param product
	 * @param stockReq
	 */
	private void parseAvailability(ProductVO product, Element stockEle) {
		// set default status
		String status = STATUS_AVAILABLE;
		if (stockEle != null) {
			if (! stockEle.getName().equalsIgnoreCase("Error")) {
				// get the ProductID child element
				Element childEle = stockEle.element("ProductID");
				if (childEle != null) {
					status = this.parseAvailabilityText(StringUtil.checkVal(childEle.getTextTrim()).toLowerCase(), status);	
				}
			}
		}
		product.setStatusMsg(status);
		log.debug("status msg: " + product.getStatusMsg());
	}
	
	/**
	 * Determines the status message for this product based on the response
	 * from the web service.
	 * @param text
	 * @return
	 */
	private String parseAvailabilityText(String text, String currStatus) {
		if (text.contains("unavailable") || text.contains("out of stock")) {
			return STATUS_SOLD_OUT;
		} else {
			return currStatus;
		}
	}
	
	/**
	 * 
	 * @param req
	 * @param catalogId
	 * @param cat
	 * @param useNav
	 * @return
	 * @throws SQLException
	 */
	private Collection<ProductVO> retrieveProductList(ActionRequest req, String catalogId, String cat, boolean useNav) 
	throws SQLException {
		StringBuilder s = new StringBuilder();
		s.append("select * from product a ");
		s.append("inner join PRODUCT_CATEGORY_XR b on a.PRODUCT_ID = b.PRODUCT_ID ");
		s.append("inner join PRODUCT_CATEGORY c on b.PRODUCT_CATEGORY_CD = c.PRODUCT_CATEGORY_CD ");
		s.append("inner join PRODUCT_CATALOG d on a.product_catalog_id=d.product_catalog_id and d.product_catalog_id = ? ");
		s.append("and d.status_no = ? where c.short_desc = ? AND a.PRODUCT_GROUP_ID IS NULL ");
		s.append("order by a.DISPLAY_ORDER_NO ");
		log.debug("Product list SQL: " + s + "|" + cat);
		
		int page = Convert.formatInteger(req.getParameter("page"),1); 
		int rpp = Convert.formatInteger(req.getParameter("rpp"),10);
		int start = (page - 1) * rpp + 1;
		int end = rpp * page;
		
		Map<String, ProductVO>  data = new LinkedHashMap<String, ProductVO> ();
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		ps.setString(1, catalogId);
		ps.setInt(2, ProductCatalogAction.STATUS_LIVE);
		ps.setString(3, cat);

		ResultSet rs = ps.executeQuery();
		int ctr = 0;
		ProductVO prod = null;
		while(rs.next()) {
			// set the page info
			if (useNav) {
				ctr++;
				if (ctr >= start && ctr <= end) {
					prod = new ProductVO(rs);
					prod.setProductId(prod.getCustProductNo());
					data.put(prod.getProductId(), prod);
					//log.debug("Nav: " + ctr + "|" + start + "|" + end + "|" + rs.getString("product_id"));
				}
			} else {
				prod = new ProductVO(rs);
				prod.setProductId(prod.getCustProductNo());
				data.put(prod.getProductId(), prod);
			}
		}
		
		// Get the attributes and add the nav piece
		if (useNav) {
			req.setAttribute("prodNav", new NavManager(ctr, rpp, page, req.getRequestURL() + ""));
		}
		return data.values();
	}
	
	/**
	 * Retrieves the product list for a given category along with information about that category.
	 * @param req
	 * @param catalogId
	 * @param cat
	 * @param useNav
	 * @return
	 * @throws SQLException
	 */
	private ProductCategoryVO retrieveCategoryProductList(ActionRequest req, String catalogId, String cat, boolean useNav) 
			throws SQLException {

		StringBuilder s = new StringBuilder();
		s.append("select a.*, c.*, c.image_url as 'category_image_url' from product a ");
		s.append("inner join PRODUCT_CATEGORY_XR b on a.PRODUCT_ID = b.PRODUCT_ID ");
		s.append("inner join PRODUCT_CATEGORY c on b.PRODUCT_CATEGORY_CD = c.PRODUCT_CATEGORY_CD ");
		s.append("inner join PRODUCT_CATALOG d on a.product_catalog_id=d.product_catalog_id and d.product_catalog_id = ? ");
		s.append("and d.status_no = ? where c.short_desc = ? AND a.PRODUCT_GROUP_ID IS NULL ");
		s.append("order by product_nm ");
		log.debug("Category product list SQL: " + s + "|" + cat);
		
		int page = Convert.formatInteger(req.getParameter("page"),1); 
		int rpp = Convert.formatInteger(req.getParameter("rpp"),10);
		int start = (page - 1) * rpp + 1;
		int end = rpp * page;
		
		List<ProductVO> pData = new ArrayList<>();
		ProductCategoryVO pCat = null;
		//Map<String, ProductVO>  data = new LinkedHashMap<String, ProductVO> ();
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		ps.setString(1, catalogId);
		ps.setInt(2, ProductCatalogAction.STATUS_LIVE);
		ps.setString(3, cat);

		ResultSet rs = ps.executeQuery();
		
		int ctr = 0;
		ProductVO prod = null;
		while(rs.next()) {
			// set the page info
			if (ctr == 0) {
				pCat = new ProductCategoryVO(rs);
				// we need to set the category image url.
				pCat.setImageUrl(rs.getString("category_image_url"));
			}
			
			if (useNav) {
				ctr++;
				if (ctr >= start && ctr <= end) {
					prod = new ProductVO(rs);
					// swap product ID for JSTL use
					prod.setProductId(prod.getCustProductNo());
					pData.add(prod);
					//log.debug("Nav: " + ctr + "|" + start + "|" + end + "|" + rs.getString("product_id"));
				}
			} else {
				prod = new ProductVO(rs);
				// swap product ID for JSTL use
				prod.setProductId(prod.getCustProductNo());
				pData.add(prod);
			}
		}
		
		if (pData.size() > 0) pCat.setProducts(pData);
		
		// Get the attributes and add the nav piece
		if (useNav) {
			req.setAttribute("prodNav", new NavManager(ctr, rpp, page, req.getRequestURL() + ""));
		}
		return pCat;
	}

	
	/**
	 * 
	 * @param req
	 * @param title
	 * @param kywd
	 * @param desc
	 */
	private void setPageData(ActionRequest req, String title, String kywd, String desc) {
		PageVO sPage = (PageVO)req.getAttribute(Constants.PAGE_DATA);
		sPage.setTitleName(title);
		sPage.setMetaDesc(kywd);
		sPage.setMetaKeyword(desc);
		req.setAttribute(Constants.PAGE_DATA, sPage);
	}
	
	/**
	 * 
	 * @param data
	 * @param inList
	 * @throws SQLException
	 */
	private void getProductAttributes(Map<String, ProductVO> data, String inList)
	throws SQLException {
		StringBuilder s = new StringBuilder();
		s.append("select * from product_attribute a inner join ");
		s.append("product_attribute_xr b on a.attribute_id = b.attribute_id ");
		s.append("where product_id in (").append(inList).append(") ");
		s.append("order by product_id, a.attribute_id");
		log.debug("Product Attribute SQL: " + s);
		
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			String prodId = rs.getString("product_id");
			data.get(prodId).addProdAttribute(rs.getString("attribute_id"), rs.getString("value_txt"));
		}
	}
	
	/**
	 * 
	 * @param req
	 * @return
	 */
	private List<ProductCategoryVO> retrieveCat(ActionRequest req, String catalogId, String cat) 
	throws SQLException {
		StringBuilder s = new StringBuilder();
		
		if (cat.length() > 0) {
			s.append("select * from PRODUCT_CATEGORY where PARENT_CD in ( ");
			s.append("select PRODUCT_CATEGORY_CD from product_category  ");
			s.append("where CATEGORY_URL = ? and PARENT_CD is null ");
			s.append("and product_catalog_id = ?) ");
		} else {
			s.append("select * from PRODUCT_CATEGORY where product_catalog_id = ? and parent_cd is null ");
		}
		s.append("order by create_dt asc ");
		log.debug("Prod Cat SQL: " + s + "|" + catalogId + "|" + cat);
		
		List<ProductCategoryVO>  data = new ArrayList<ProductCategoryVO> ();
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		int i = 0;
		if (cat.length() > 0) ps.setString(++i, cat);
		ps.setString(++i, catalogId);
		
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			ProductCategoryVO vo = new ProductCategoryVO(rs);
			data.add(vo);
		}
		return data;
	}
	
	/**
	 * Retrieves subcategory data.
	 * @param req
	 * @param catalogId
	 * @param cat
	 * @return
	 * @throws SQLException
	 */
	private List<ProductCategoryVO> retrieveSubCat(ActionRequest req, String catalogId, String cat) 
	throws SQLException {
		StringBuilder s = new StringBuilder();
		
		if (cat.length() > 0) {
			s.append("select * from PRODUCT_CATEGORY where PARENT_CD in ( ");
			s.append("select PRODUCT_CATEGORY_CD from product_category  ");
			s.append("where SHORT_DESC = ? and PARENT_CD is not null ");
			s.append("and product_catalog_id = ?) ");
		}
		s.append("order by category_nm asc ");
		log.debug("Prod sub cat SQL: " + s + "|" + cat + "|" + catalogId);
		
		List<ProductCategoryVO>  data = new ArrayList<ProductCategoryVO> ();
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		int i = 0;
		ps.setString(++i, cat);
		ps.setString(++i, catalogId);
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			ProductCategoryVO vo = new ProductCategoryVO(rs);
			data.add(vo);
		}
		return data;
	}

}
