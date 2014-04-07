package com.universal.signals.action;

// JDK 1.6.x
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
import com.siliconmtn.http.SMTServletRequest;
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
	
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("********** Building");
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		String catalogId = StringUtil.checkVal(mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		mod = null;
		String cat = this.buildCatUrlFromQueryString(req);
		String cat1 = StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + "1"));
		log.debug("catalogId, cat: " + catalogId + ", " + cat);
		try {
			List<ProductCategoryVO> data = this.retrieveCat(req, catalogId, cat);
			this.putModuleData(data, data.size(), false);
			
			if (data.size() == 0) {
				//if ((cat2.length() > 1 || cat3.length() > 1) && ! "detail".equalsIgnoreCase(cat1)) {
				if ((cat.indexOf("|") > -1) && ! "detail".equalsIgnoreCase(cat1)) {
					data = this.retrieveSubCat(req, catalogId, cat);
					this.putModuleData(data, data.size(), false);
					if (data.size() ==0) {
						log.debug("retrieve product list info");
						Collection<ProductVO> prods = this.retrieveProductList(req, catalogId, cat);
						// if the data is still empty, get the group data
						if (prods.size() == 0) prods = this.getGroupInfo(req, catalogId);
						this.putModuleData(prods, prods.size(), false);
						req.setAttribute("usaProdList", Boolean.TRUE);
					}
				} else {
					// no subcats exist, check for products
					Collection<ProductVO> prods = this.retrieveProductList(req, catalogId, cat);
					if (prods.size() == 0) {
						// check for group data
						prods = this.getGroupInfo(req, catalogId);
						if (prods.size() == 0) {
							// if the group data is empty check for detail
							log.debug("Retrieving product detail info...");
							ProductVO detail = this.retrieveProductDetail(req, catalogId);
							if (detail.getProductId() != null) {
								this.checkAvailability(req, detail);
								this.putModuleData(detail, 1, false);
								req.setAttribute("usaProdDetail", Boolean.TRUE);
							} else {
								// no data, default to prod list
								this.putModuleData(prods, prods.size(), false);
								req.setAttribute("usaProdList", Boolean.TRUE);
							}
						} else {
							this.putModuleData(prods, prods.size(), false);
							req.setAttribute("usaProdList", Boolean.TRUE);
						}
					} else {
						this.putModuleData(prods, prods.size(), false);
						req.setAttribute("usaProdList", Boolean.TRUE);
					}
				}
			}
			log.debug("data size: " + data.size());
		} catch (Exception e) {
			log.error("Unable to retrieve catalog data", e);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	private String buildCatUrlFromQueryString(SMTServletRequest req) {
		String cat = "";
		String qsParam = null;
		for (int i=1; i<11; i++) {
			log.debug("evaluating cat qs param #" + i);
			qsParam = StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + i));
			if (qsParam.length() == 0) break; // if param key has no value, we're done.
			if (i == 1) {
				cat = qsParam;
			} else {
				cat = cat + "|" + qsParam;
			}	
		}		
		return cat;
	}
	
	/**
	 * 
	 * @param req
	 * @param catalogId
	 * @return
	 * @throws SQLException
	 */
	public Collection<ProductVO> getGroupInfo(SMTServletRequest req, String catalogId) 
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

		for (int i=0; rs.next(); i++) {
			// set the page info
			if (i == 0)
				this.setPageData(req, rs.getString("title_nm"), rs.getString("meta_kywd_txt"), rs.getString("meta_desc"));
			
			String prodId = rs.getString("product_id");
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
	public ProductVO retrieveProductDetail(SMTServletRequest req, String catalogId) throws SQLException {
		StringBuilder s = new StringBuilder();
		s.append("select * from product a ");
		s.append("left outer join product_attribute_xr b on a.product_id = b.product_id ");
		s.append("left outer join product_attribute c on b.attribute_id = c.attribute_id ");
		s.append("where a.product_catalog_id = ? and a.product_id = ? AND a.PRODUCT_GROUP_ID IS NULL ");
		s.append("order by a.product_id, b.attribute_id, attrib2_txt, order_no");
		log.debug("Product Detail SQL: " + s + "|" + req.getParameter(SMTServletRequest.PARAMETER_KEY + "2"));
		
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		ps.setString(1, catalogId);
		ps.setString(2, req.getParameter(SMTServletRequest.PARAMETER_KEY + "2"));
		String aName = null;
		List<ProductAttributeVO> pAttributes = null;
		ResultSet rs = ps.executeQuery();
		ProductVO product = null;
		while (rs.next()) {
			if (product == null) { 
				product = new ProductVO(rs);
			}
			
			if(aName != null && !aName.equals(rs.getString("attribute_nm"))){
				product.addProdAttribute(aName, pAttributes);
				pAttributes = null;
			}
			
			if(pAttributes == null){ 
				pAttributes = new ArrayList<ProductAttributeVO>();
				aName = rs.getString("attribute_nm");
			}
			
			pAttributes.add(new ProductAttributeVO(rs));
			
		}
		
		// if we found no products for a category or no attributes, initialize objects.
		if (product == null) product = new ProductVO();
		if(pAttributes == null)	pAttributes = new ArrayList<ProductAttributeVO>();
		
		// sort the product attributes by level and display order
		Collections.sort(pAttributes, new ProductAttributeComparator());
		// add attributes to product VO
		product.addProdAttribute(aName, pAttributes);
		pAttributes = null;
		this.setPageData(req, product.getTitle(), product.getMetaKywds(), product.getMetaDesc());
		return product;
	}
	
	/**
	 * Calls web service to check availability of the product being selected.
	 * @param req
	 * @param product
	 */
	private void checkAvailability(SMTServletRequest req, ProductVO product) {
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
	 * @param cat
	 * @return
	 * @throws SQLException
	 */
	public Collection<ProductVO> retrieveProductList(SMTServletRequest req, String catalogId, String cat) 
	throws SQLException {
		//String cat4 = StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + "4"));
		StringBuilder s = new StringBuilder();
		s.append("select * from product a ");
		s.append("inner join PRODUCT_CATEGORY_XR b on a.PRODUCT_ID = b.PRODUCT_ID ");
		s.append("inner join PRODUCT_CATEGORY c on b.PRODUCT_CATEGORY_CD = c.PRODUCT_CATEGORY_CD ");
		s.append("inner join PRODUCT_CATALOG d on a.product_catalog_id=d.product_catalog_id and d.product_catalog_id = ? ");
		s.append("and d.status_no = ? where c.short_desc = ? AND a.PRODUCT_GROUP_ID IS NULL ");
		s.append("order by product_nm ");
		log.debug("Prod List SQL: " + s + "|" + cat);
		
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
		while(rs.next()) {
			// set the page info
			ctr++;
			
			if (ctr >= start && ctr <= end) {
				data.put(rs.getString("product_id"), new ProductVO(rs));
				log.debug("Nav: " + ctr + "|" + start + "|" + end + "|" + rs.getString("product_id"));
			}
		}
		
		// Get the attributes and add the nav piece
		req.setAttribute("prodNav", new NavManager(ctr, rpp, page, req.getRequestURL() + ""));
		return data.values();
	}
	
	/**
	 * 
	 * @param req
	 * @param title
	 * @param kywd
	 * @param desc
	 */
	public void setPageData(SMTServletRequest req, String title, String kywd, String desc) {
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
	public void getProductAttributes(Map<String, ProductVO> data, String inList)
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
	public List<ProductCategoryVO> retrieveCat(SMTServletRequest req, String catalogId, String cat) 
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
	
	public List<ProductCategoryVO> retrieveSubCat(SMTServletRequest req, String catalogId, String cat) 
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
