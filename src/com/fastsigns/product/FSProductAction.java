package com.fastsigns.product;

// JDK 1.6.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


//SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
//import com.siliconmtn.commerce.ProductAttributeVO;
//import com.siliconmtn.commerce.ProductVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.commerce.product.ProductCatalogAction;
import com.smt.sitebuilder.action.video.VideoVO;
import com.siliconmtn.commerce.catalog.ProductAttributeVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: FSProductAction.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Displays the Fast Signs Product catalog in a category
 * centric manner.  The management of the catalog is maintained using the 
 * Standard SMT product actions
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Oct 23, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class FSProductAction extends SBActionAdapter {
	
	public static final String IMAGE_ATTRIBUTES = "FS_IMAGE";
	public static final String VIDEO_ATTRIBUTES = "FS_VIDEO";
	//public static Map<String, String> reverseMap = getProdMap();
	/**
	 * 
	 */
	public FSProductAction() {
	}

//	private static HashMap<String, String> getProdMap() {
//		Map<String, String> m = new HashMap<String, String>();
//		m.put("signs-graphics", "sg-");
//		m.put("interior-decor", "dec-");
//		m.put("exhibits-displays", "dis-");
//		m.put("promotional-products", "pp-");
//		m.put("printing-mailing", "pm-");
//		m.put("point-of-purchase-signs", "pop-");
//		m.put("interactive-digital", "int-");
//
//
//		return null;
//	}

	/**
	 * @param actionInit
	 */
	public FSProductAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {	
		// If we are simply creating the menu item we skip out on most of the method.
		if (req.hasParameter("menuItem")) {
			prodTreeByCat(req);
			return;
		}
		
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		boolean isPreview = page.isPreviewMode();
		
		// Get the individual parameters to determine what data to retrieve
		ModuleVO mod = ((ModuleVO) getAttribute(Constants.MODULE_DATA));
		log.debug(req.getRequestURI());
		String[] catArr = StringUtil.checkVal(mod.getAttribute(SBModuleVO.ATTRIBUTE_1)).split("~");
		if (catArr == null || catArr.length != 2) log.error(StringUtil.getToString(catArr, false, false, ","));
		
		String catalogId = catArr[0];
		String categoryId = catArr[1];
		
		boolean hasSubCat = Convert.formatBoolean(mod.getAttribute(SBModuleVO.ATTRIBUTE_2));
		String pId = StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + "1"));
		String prodChildKey = StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + "2"));
		String catImageKey = StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + "3"));
		if(StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + "4")).length() > 0) {
			prodChildKey = catImageKey;
			catImageKey = StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + "4"));
		}
		
		log.debug("Has Sub Cat: " + hasSubCat + "|" + pId + "|" + prodChildKey + "|" + catImageKey + "|");
		log.debug("Module: " + mod);
		
		// Determine what page is being requested and get the data
		try {
			if (hasSubCat) {
				//log.debug("Has Sub Cat");
				if (pId.length() == 0) {
					log.debug("categories by type");
					getCatByType(categoryId, isPreview);
				} else if (pId.length() > 1 && prodChildKey.length() == 0) {
					log.debug("products by category");
					this.getProdBySubCat(req, pId, catalogId, isPreview);
				} else if (catImageKey.length() > 0) {
					log.debug("product info");
					this.getProdImages(req, catImageKey, prodChildKey, catalogId, isPreview);
				} else {
					log.debug("child products");
					this.getChildProducts(prodChildKey, catalogId, req, isPreview);
//					if(mod.getDataSize() == 0){
//						
//						this.sendRedirect("/" + reverseMap.get(pId) + pId, "", req);
//					}
				}
			} else {
				log.debug("No Sub Cat");
				if (prodChildKey.length() > 0) {
					this.getProdImages(req, prodChildKey, pId, catalogId, isPreview);
				} else {
					this.getProdByCat(req, categoryId, pId);
				}
			}
		} catch(Exception e) {
			log.error("Unable to retrieve catalog", e);
		}

		//add to cache group for approval compatibility.
		mod.addCacheGroup(catalogId);
		
		// Assign the page title and other data
		this.assignPageInfo(req, mod);
		
		log.debug(page);
		
		buildCanonicalUrl(req, pId, prodChildKey, catImageKey);
	}
	
	/**
	 * Build the canonical and relative mobile urls for the product we are looking at
	 * @param req
	 * @param pId
	 * @param prodChildKey
	 * @param catImageKey
	 */
	private void buildCanonicalUrl(SMTServletRequest req, String pId, String prodChildKey, String catImageKey) {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		
		// Check if there is an attribute that sets the urls.  If so we use that and ignore everything else
		String[] attribUrls = checkAttribUrl(req);
		if (attribUrls != null && attribUrls[0].length() > 0 && attribUrls[1].length() > 0) {
			page.setCanonicalPageUrl(attribUrls[0]);
			page.setCanonicalMobileUrl(attribUrls[1]);
			return;
		}
		
		// Start building the mobile url if we are not on a mobile site.
		StringBuilder mobileUrl = new StringBuilder();
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		boolean notMobile = !Convert.formatBoolean(site.getMobileFlag());
		if(notMobile) mobileUrl.append("http://"+site.getMobileSiteUrl());
		
		// Start building the canonical and mobile urls for this product page.
		StringBuilder canonicalUrl = new StringBuilder();
		boolean byProduct;
		
		if (req.hasParameter("prefix")) {
			canonicalUrl.append("/" + req.getParameter("prefix"));
			if(notMobile) mobileUrl.append("/" + req.getParameter("prefix"));
			byProduct = true;
		} else {
			canonicalUrl.append(req.getRequestURI().substring(req.getRequestURI().lastIndexOf('/')) + "-");
			byProduct = false;
		}
		
		// Determine what page is being requested and get the data
		if (pId.length() > 0) {
			if (byProduct) {
				if (prodChildKey.length() == 0) {
					canonicalUrl.append(pId.toLowerCase());
					if(notMobile) mobileUrl.append(pId.toLowerCase());
				} else {
					canonicalUrl.append(prodChildKey.toLowerCase());
					if(notMobile) mobileUrl.append(prodChildKey.toLowerCase());	
					if (catImageKey.length() > 0) {
						canonicalUrl.append("/" + catImageKey.toLowerCase());
						if(notMobile) mobileUrl.append("/" + catImageKey.toLowerCase());
					}
				}
			} else {
				canonicalUrl.append(pId.toLowerCase());
				if(notMobile) mobileUrl.append("/products" + req.getRequestURI().substring(req.getRequestURI().lastIndexOf('/')) +"/qs/"+pId);
				if (catImageKey.length() > 0) {
					if(notMobile) mobileUrl.append("/" + prodChildKey+ "/" + catImageKey);
				} else {
					if(notMobile) mobileUrl.append("/" + prodChildKey);
				}
			}
			page.setCanonicalPageUrl(canonicalUrl.toString());
		} else {
			page.setCanonicalPageUrl(req.getRequestURI().substring(req.getRequestURI().lastIndexOf('/')));
			
			if(notMobile) {
				if (!req.getRequestURI().contains("products"))
					mobileUrl.append("/products");
				mobileUrl.append(req.getRequestURI().substring(req.getRequestURI().lastIndexOf('/')));
			}
		}
		
		if(notMobile) page.setCanonicalMobileUrl(mobileUrl.toString());
	}
	
	/**
	 * Check for an attribute that sets an ovveride for the canonical and relative urls.
	 * @param req
	 * @return
	 */
	private String[] checkAttribUrl(SMTServletRequest req) {
		ProductVO product = null;
		if (req.getAttribute("fsProductName") != null && req.getAttribute("fsProductName") instanceof ProductVO)
			product = (ProductVO) req.getAttribute("fsProductName");
		else return null;
		
		StringBuilder sb = new StringBuilder();

		sb.append("select attrib1_txt, attrib2_txt from product_attribute_xr ");
		sb.append("where product_id = ? and attribute_id = 'FS_CANONICAL_URL' ");
		log.debug(sb.toString()+"|"+product.getProductId());
		
		PreparedStatement ps;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, product.getProductId());
			
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return new String[]{rs.getString(1), rs.getString(2)};
			}
		} catch (SQLException e) {
			log.error("Attempt to get canonical url from attributes failed. ", e);
		}
		return null;
	}

	/**
	 * Gets the minimal amount of information needed to create a list of the 
	 * different categories and products under them for use in the menu
	 * @param req
	 */
	private void prodTreeByCat(SMTServletRequest req) {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		boolean isPreview = page.isPreviewMode();
		String menuName = req.getParameter("menuName");
		
		try {
			PreparedStatement ps = dbConn.prepareStatement(buildMenuSQL(isPreview));
			ps.setString(1, getCategoryCd(req));
			
			ResultSet rs = ps.executeQuery();
			
			List<Node> nodes = new ArrayList<Node>();
			Node node = null;
			String catName = "";
			while (rs.next()) {
				if(!catName.equals(rs.getString("CATEGORY_NM"))) {
					catName = rs.getString("CATEGORY_NM");
					node = new Node();
					node.setNodeId(rs.getString("CATEGORY_NM"));
					node.setNodeName(rs.getString("CATEGORY_NM"));
					node.setFullPath(rs.getString("CUST_CATEGORY_ID")+rs.getString("URL_ALIAS_TXT"));
					node.setParentId(menuName);
					node.setUserObject(rs.getInt("ORDER_NO"));
					nodes.add(node);
				}
				node = new Node();
				node.setNodeName(rs.getString("PRODUCT_NM"));
				node.setFullPath(rs.getString("CUST_CATEGORY_ID")+rs.getString("prod_url_alias"));
				node.setParentId(rs.getString("CATEGORY_NM"));
				node.setUserObject(rs.getInt("ORDER_NO"));
				nodes.add(node);
			}
			
			// Add a root node to connect every item in the list.
			node = new Node();
			node.setNodeId(menuName);
			node.setNodeName(req.getParameter("menuLink"));
			nodes.add(node);
			
			// Create a data tree and add it to the module container
			this.putModuleData(new Tree(nodes).preorderList(false), nodes.size(), false);
		} catch (SQLException e) {
			log.error("Unable to get product and category information from database. ", e);
		}
	}

	/**
	 * Create the sql query for getting the list of products for the site menu
	 * @param isPreview
	 * @return
	 */
	private String buildMenuSQL(boolean isPreview) {
		StringBuilder sql = new StringBuilder();
		sql.append("select CATEGORY_NM, CUST_CATEGORY_ID, pc.URL_ALIAS_TXT, p.PRODUCT_NM, p.URL_ALIAS_TXT as 'prod_url_alias', pc.ORDER_NO ");
		sql.append("from PRODUCT_CATEGORY pc ");
		sql.append("inner join PRODUCT_CATEGORY_XR pcx on pc.PRODUCT_CATEGORY_CD = pcx.PRODUCT_CATEGORY_CD ");
		sql.append("inner join PRODUCT p on pcx.PRODUCT_ID = p.PRODUCT_ID ");
		if (isPreview) {
			sql.append("left join PRODUCT p2 on p.PRODUCT_ID = p2.PRODUCT_GROUP_ID ");
			sql.append("left join PRODUCT_CATEGORY pc2 on pc.PRODUCT_CATEGORY_CD = pc2.PRODUCT_CATEGORY_CD ");
		}
		sql.append("where pc.parent_cd = ? ");
		if (isPreview) {
			sql.append("and (pc.category_group_id is not null or (pc2.product_category_cd is null and pc.category_group_id is null)) ");
			sql.append("and (p.product_group_id is not null or (p2.product_id is null and pc.category_group_id is null)) ");
		} else {
			sql.append("and pc.category_group_id is null and p.product_group_id is null ");
		}
		sql.append(" and pc.active_flg=1 and p.status_no=5 ");
		sql.append("order by pc.order_no, CATEGORY_NM");
		
		return sql.toString();
	}
	
	/**
	 * Gets the category we will be building this menu item from based on the
	 * Ajax Module Id that we were given.
	 * @param req
	 * @return
	 * @throws SQLException
	 */
	private String getCategoryCd(SMTServletRequest req) throws SQLException {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT pc.PRODUCT_CATEGORY_CD FROM PRODUCT_CATEGORY pc ");
		sql.append("inner join product_catalog cat on pc.product_catalog_id = cat.product_catalog_id ");
		sql.append("where url_alias_txt = ? and status_no = 5 and organization_id = ?");
		
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);

		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ps.setString(1, req.getParameter("amid"));
		ps.setString(2, site.getOrganizationId());
		
		ResultSet rs = ps.executeQuery();
		
		if (rs.next())
			return rs.getString(1);
		
		throw new SQLException("No parent code found for organization " + site.getOrganizationId()
				+ " and Ajax Module " + req.getParameter("amid"));
	}
	
	/**
	 * 
	 * @param req
	 * @param mod
	 */
	public void assignPageInfo(SMTServletRequest req, ModuleVO mod) {
		String attr = StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + "1"));
		PageVO page = (PageVO)req.getAttribute(Constants.PAGE_DATA);
		log.debug("Assigning Page Information");
		
		if (attr.length() > 0) {
			
			Object tmp = mod.getActionData();
			Object data = null;
			if (tmp instanceof List<?>) {
				if (((List<?>) tmp).size() > 0) data = ((List<?>) tmp).get(0);
			} else {
				data = tmp;
			}
			
			ProductVO product = new ProductVO();
			if (data instanceof com.siliconmtn.data.Node) {
				Object nData = ((Node)data).getUserObject();
				
				if (nData instanceof ProductVO ) {
					product = (ProductVO)nData; 
				} 
			} else if (data instanceof ProductAttributeVO) {
				product = (ProductVO)req.getAttribute("fsChildName");
			} else if (req.getAttribute("fsProductName") != null && req.getAttribute("fsProductName") instanceof ProductVO)
				product = (ProductVO) req.getAttribute("fsProductName");
			
			// Make sure the product was retrieved
			if (product != null) {
				// Set the title, meta keyword and meta desc
				if (StringUtil.checkVal(product.getTitle()).length() > 0){
					page.setTitleName(product.getTitle());
				}
	
				if (StringUtil.checkVal(product.getMetaDesc()).length() > 0) {
					page.setMetaDesc(product.getMetaDesc());
				}
				
				if (StringUtil.checkVal(product.getMetaKywds()).length() > 0) {
					page.setMetaKeyword(product.getMetaKywds());
				}
				
			}
		}
	}
	
	
	/**
	 * This lookup is done using the category ID, which is set in the Admintool, not passed on URLs
	 * @param cat
	 * @param org
	 * @throws SQLException
	 */
	public void getCatByType(String categoryId, boolean isPreview) throws SQLException {
		StringBuilder s = new StringBuilder();
		s.append("select a.*,b.*, c.*, c.url_alias_txt as 'prod_url_alias' from PRODUCT_CATEGORY a ");
		s.append("inner join PRODUCT_CATEGORY_XR b on a.PRODUCT_CATEGORY_CD = b.PRODUCT_CATEGORY_CD ");
		if (isPreview) s.append("or a.CATEGORY_GROUP_ID = b.PRODUCT_CATEGORY_CD ");
		s.append("inner join PRODUCT c on b.PRODUCT_ID = c.PRODUCT_ID ");
		if (isPreview) {
			s.append("or b.PRODUCT_ID = c.PRODUCT_GROUP_ID ");
			s.append("left join PRODUCT c2 on c.PRODUCT_ID = c2.PRODUCT_GROUP_ID ");
			s.append("left join PRODUCT_CATEGORY a2 on a.PRODUCT_CATEGORY_CD = a2.CATEGORY_GROUP_ID ");
			s.append("where (a.parent_cd = ? or a.PRODUCT_CATEGORY_CD = ? or a.category_group_id = ?) ");
			s.append("and (a.category_group_id is not null or (a2.product_category_cd is null and a.category_group_id is null)) ");
			s.append("and (c.product_group_id is not null or (c2.product_id is null and c.product_group_id is null)) ");
		} else {
			s.append("where (a.parent_cd = ? or a.PRODUCT_CATEGORY_CD = ? ) "); 
			s.append("and a.category_group_id is null and c.product_group_id is null ");
		}		
		s.append("and a.active_flg=1 and c.status_no=5 "); //get active categories, and active products (ONLY!)
		s.append("order by a.parent_cd, a.order_no, a.CATEGORY_NM");
		log.debug("Cat By type SQL: " + s + "|" + categoryId);
		
		PreparedStatement ps = null;
		List<Node> nodes = new ArrayList<Node>();
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, categoryId);
			ps.setString(2, categoryId);
			if(isPreview) ps.setString(3, categoryId);
			
			ResultSet rs = ps.executeQuery();
			String origCatName = "";
			ProductCategoryVO vo = null;
			while(rs.next()) {
				String catName = StringUtil.checkVal(rs.getString("category_nm"));
				if (!catName.equalsIgnoreCase(origCatName)) { //the category changed!
					// Add the existing cat to the node list
					if (vo != null) {
						Node n = null;
						if (isPreview && vo.getCategoryGroupId() != null) n = new Node(vo.getCategoryGroupId(), vo.getParentCode());
						else n = new Node(vo.getCategoryCode(), vo.getParentCode());
						n.setUserObject(vo);
						n.setNodeName(vo.getCategoryName());
						nodes.add(n);
						log.debug("Node Data: " + ((ProductCategoryVO)n.getUserObject()).getProducts().size());
					}

					// create the new Category
					vo = new ProductCategoryVO(rs);
				}
					
				//add the product to this category
				ProductVO p = new ProductVO(rs);
				p.setUrlAlias(rs.getString("prod_url_alias"));
				vo.addProduct(p);
				
				origCatName = catName;
			}
			
			// append the last (trailing) entry
			if (vo != null) {
				Node n = new Node(vo.getCategoryCode(), vo.getParentCode());
				n.setUserObject(vo);
				n.setNodeName(vo.getCategoryName());
				nodes.add(n);
			}
			
			if (log.isDebugEnabled()) 
				log.debug("Nodes Size: " + new Tree(nodes).preorderList().size());
			
			// Create a data tree and add it to the module container
			this.putModuleData(new Tree(nodes).preorderList(), nodes.size(), false);
			
		} finally {
			try {
				ps.close();
			} catch(Exception e){}
		}
	}
	
	/**
	 * 
	 * @param req
	 * @param cat
	 * @throws SQLException
	 */
	public void getProdByCat(SMTServletRequest req, String categoryId, String prodParAlias)
	throws SQLException {
		StringBuilder s = new StringBuilder();
		s.append("select a.*, b.order_no from product a inner join product_category_xr b ");
		s.append("on a.product_id = b.product_id ");
		s.append("where b.product_category_cd = ? and a.parent_id is not null and a.status_no=5 and a.product_group_id is null ");
		if (prodParAlias.length() > 0) s.append("and a.parent_id in (select product_id from product where url_alias_txt=?) ");
		s.append("union ");
		s.append("select *, 0 from product ");
		s.append("where product_id in ( ");
		s.append("select parent_id from product a inner join product_category_xr b ");
		s.append("on a.product_id = b.product_id ");
		s.append("where b.product_category_cd = ? and parent_id is not null ");
		s.append("and a.status_no=5 and a.product_group_id is null ");
		if (prodParAlias.length() > 0) s.append("and parent_id in (select product_id from product where url_alias_txt=? and product_group_id is null)");
		s.append(") and status_no=5 and product_group_id is null ");
		s.append("order by parent_id, display_order_no, order_no, product_nm ");
		log.debug("Cat SQL: " + s + "|" + categoryId + "|" + prodParAlias);
		
		PreparedStatement ps = null;
		List<Node> nodes = new ArrayList<Node>();
		try {
			int ctr = 1;
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(ctr++, categoryId);
			if (prodParAlias.length() > 0) ps.setString(ctr++, prodParAlias);
			ps.setString(ctr++, categoryId);
			if (prodParAlias.length() > 0) ps.setString(ctr++, prodParAlias);
			
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				ProductVO vo = new ProductVO(rs);
				Node n = new Node(vo.getProductId(), vo.getParentId());
				n.setUserObject(vo);
				n.setNodeName(vo.getProductName());
				nodes.add(n);
			}
			
			//Create a data tree and add it to the module container
			this.putModuleData(new Tree(nodes).preorderList(), nodes.size(), false);
		} finally {
			try {
				ps.close();
			} catch(Exception e){}
		}
	}
	
	/**
	 * 
	 * @param prodId
	 * @param org
	 * @throws SQLException
	 */
	public void getChildProducts(String prodAlias, String catalogId, SMTServletRequest req, boolean isPreview) throws SQLException {
		StringBuilder s = new StringBuilder();
		s.append("select a.*, b.value_txt as 'attrib1_txt', pax.product_id as 'imgs' from product a ");
		s.append("left outer join product_attribute_xr b on (b.product_id = a.product_id ");
		if (isPreview) s.append("or a.product_group_id = b.product_id ");
		s.append(") and b.attribute_id like '%_AD' "); 
		s.append("left join product_attribute_xr pax on pax.product_id = a.product_id and pax.attribute_id like '%_IMAGE' ");
		if (isPreview)s.append("left join product a2 on a.product_id = a2.product_group_id ");
		s.append("where a.product_catalog_id=? and a.status_no=5 ");
		if (isPreview) s.append("and (a.product_group_id is not null or (a2.product_id is null and a.product_group_id is null)) ");
		else s.append("and a.product_group_id is null ");
		s.append("and (a.url_alias_txt=? or a.parent_id in (select product_id from product where url_alias_txt=?)) ");
		s.append("and (pax.product_attribute_id = (SELECT TOP(1)product_attribute_id ");
		s.append("from product_attribute_xr where product_id = a.product_id and attribute_id like '%_IMAGE') ");
		s.append("or pax.product_attribute_id is null) ");
		s.append("order by a.parent_id, a.display_order_no, a.product_nm");
		log.debug(s + "|" + catalogId + "|" + prodAlias);
		PreparedStatement ps = null;
		List<Node> nodes = new ArrayList<Node>();
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, catalogId);
			ps.setString(2, prodAlias);
			ps.setString(3, prodAlias);
			ResultSet rs = ps.executeQuery();
			int i = 0;
			while(rs.next()) {
				ProductVO vo = new ProductVO(rs);
				if(i == 0){
					addVideos(vo);
					req.setAttribute("fsProductVideos", vo.getProdAttributes());
					req.setAttribute("fsProductName", vo);
				}
				i++;
				Node n = new Node(vo.getProductId(), vo.getParentId());
				n.setUserObject(vo);
				n.setNodeName(vo.getProductName());
				
				if (rs.getString("imgs") == null) {
					n.setLeaf(false);
				} else {
					n.setLeaf(true);
				}
				nodes.add(n);
			}
			
			if (nodes.size() == 1) {
				// There is only the parent product, so we treat it like we would a product at the end of the chain.
				this.getProdImages(req, prodAlias, prodAlias, catalogId, isPreview);
				req.setParameter(SMTServletRequest.PARAMETER_KEY + "3", prodAlias);
			} else {
				//Create a data tree and add it to the module container
				log.debug("Number of Products: " + nodes.size());
				this.putModuleData(nodes, nodes.size(), false);
			}
			
			
		} finally {
			try {
				ps.close();
			} catch(Exception e){}
		}
	}
	
	/**
	 * Check for videos from the video library related to this product
	 */
	private void addVideos(ProductVO product) throws SQLException {
		StringBuilder sb = new StringBuilder();

		sb.append("select * from product_attribute_xr pax ");
		sb.append("left join video v on pax.attrib3_txt = v.video_id ");
		sb.append("where pax.product_id = ? and pax.attribute_id = 'FS_VIDEO_LIB' ");
		sb.append("order by order_no");
		log.debug(sb.toString()+"|"+product.getProductId());
		
		PreparedStatement ps = dbConn.prepareStatement(sb.toString());
		
		ps.setString(1, product.getProductId());
		
		ResultSet rs = ps.executeQuery();
		VideoVO video;
		while (rs.next()) {
			video = new VideoVO(rs);
			video.setAspectRatioHeight(Integer.parseInt(StringUtil.checkVal(rs.getString("ATTRIB1_TXT"))));
			video.setAspectRatioWidth(Integer.parseInt(StringUtil.checkVal(rs.getString("ATTRIB2_TXT"))));
			video.setAttribute("position", StringUtil.checkVal(rs.getString("VALUE_TXT")));
			
			product.addProdAttribute(rs.getString("product_attribute_id"), video);
		}
		
	}
	
	/**
	 * 
	 * @param req
	 * @param cat
	 * @param org
	 * @param pId
	 * @throws SQLException
	 */
	public void getProdBySubCat(SMTServletRequest req, String catUrlAlias, String catalogId, boolean isPreview)
	throws SQLException {
		StringBuilder s = new StringBuilder();
		s.append("select a.product_id, a.parent_id, a.product_nm, a.desc_txt, a.url_alias_txt, ");
		s.append("a.meta_kywd_txt,a.meta_desc,a.title_nm,a.short_desc, b.order_no, ");
		s.append("a.thumbnail_url, a.image_url, a.cust_product_no, d.value_txt as 'attrib1_txt', ");
		s.append("COALESCE(MIN(sp.product_id ), '0') as child, COALESCE(MIN(pax.product_attribute_id ), '0') as attr ");
		s.append("from product a inner join product_category_xr b on a.product_id = b.product_id ");
		if (isPreview) s.append("or a.product_group_id = b.product_id ");
		s.append("inner join product_category c on b.product_category_cd=c.product_category_cd ");
		if (isPreview) s.append("or c.category_group_id = b.product_category_cd ");
		s.append("left join product_attribute_xr pax on a.product_id = pax.product_id and pax.attribute_id like '%_IMAGE' ");
		s.append("left outer join product_attribute_xr d on d.product_id = a.product_id ");
		s.append("and d.attribute_id like '%_AD' ");
		s.append("left join product sp on sp.parent_id = a.product_id ");
		if (isPreview) {
			s.append("left join product a2 on a.product_id = a2.product_group_id ");
			s.append("left join product_category c2 on c.product_category_cd = c2.category_group_id ");
		}
		s.append("where c.url_alias_txt = ? and c.product_catalog_id=? and a.status_no=5 ");
		if (isPreview) {
			s.append("and (a.product_group_id is not null or (a2.product_id is null and a.product_group_id is null)) ");
			s.append("and (c.category_group_id is not null or (c2.product_category_cd is null and c.category_group_id is null)) ");
		} else s.append("and a.product_group_id is null and c.category_group_id is null ");
		s.append("group by a.product_id, a.parent_id, a.product_nm, a.desc_txt, a.url_alias_txt, a.meta_kywd_txt,a.meta_desc, ");
		s.append("a.title_nm,a.short_desc, b.order_no, a.thumbnail_url, a.image_url, a.cust_product_no, d.value_txt ");
		
		s.append("union ");
		
		s.append("select pc.product_category_cd, pc.parent_cd, pc.category_nm, pc.category_desc, ");
		s.append("pc.url_alias_txt, pc.meta_kywd_txt, pc.meta_desc, pc.title_nm, pc.short_desc, 0, ");
		s.append("pc.thumbnail_img, pc.image_url, pc.cust_category_id as 'cust_product_no', ");
		s.append("pc.attrib1_txt, '0' as child, '0' as attr from product_category pc ");
		if (isPreview) s.append("left join product_category pc2 on pc.product_category_cd = pc2.category_group_id " );
		s.append("where pc.url_alias_txt = ? and pc.product_catalog_id=? and pc.active_flg=1 ");
		if (isPreview) s.append("and (pc.category_group_id is not null or (pc2.product_category_cd is null and pc.category_group_id is null)) ");
		else s.append("and category_group_id is null ");
		s.append("order by order_no, product_nm ");
		log.debug("SubCat SQL: " + s + "|" + catUrlAlias + "|" + catalogId);
		
		PreparedStatement ps = null;
		List<Node> nodes = new ArrayList<Node>();
		
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, catUrlAlias);
			ps.setString(2, catalogId);
			ps.setString(3, catUrlAlias);
			ps.setString(4, catalogId);
			int i = 0;
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				ProductVO vo = new ProductVO(rs);
				if(i == 0)
					req.setAttribute("fsProductName", vo);
				i++;
				Node n = new Node(vo.getProductId(), vo.getParentId());
				n.setUserObject(vo);
				n.setNodeName(vo.getProductName());
				
				// If this product does not have any children or attributes we set it as a leaf
				if (!"0".equals(rs.getString("child")) || !"0".equals(rs.getString("attr"))) {
					n.setLeaf(true);
				} else {
					n.setLeaf(false);
				}
				
				nodes.add(n);
			}
			
			//Create a data tree and add it to the module container
			this.putModuleData(nodes, nodes.size(), false);
		} finally {
			try {
				ps.close();
			} catch(Exception e){}
		}
	}
	
	/**
	 * Retrieves the images or videos for the provided product
	 * @param productId
	 * @param orgId
	 * @throws SQLException
	 */
	private void getProdImages(SMTServletRequest req, String prodAlias, String parProdAlias, String catalogId, boolean isPreview) 
	throws SQLException {
		StringBuilder s = new StringBuilder();
		s.append("select '1' as product_rank, a.*, c.*, d.* from PRODUCT a "); 
		s.append("left outer join product_attribute_xr c on a.product_id = c.product_id "); 
		if (isPreview) s.append("or c.product_id = a.product_group_id ");
		s.append("left outer join product_attribute d on c.attribute_id = d.attribute_id "); 
		if (isPreview) {
			s.append("or c.attribute_id = d.attribute_group_id ");
			s.append("left join product a2 on a.product_id = a2.product_group_id ");
			s.append("left join product_attribute d2 on d2.attribute_group_id = d.attribute_id ");
		}
		s.append("where a.url_alias_txt in (?) "); 
		s.append("and a.product_catalog_id=? and a.status_no=5 "); 
		if (isPreview) {
			s.append("and (a.product_group_id is not null or (a2.product_id is null and a.product_group_id is null)) ");
			s.append("and (d.attribute_group_id is not null or (d2.attribute_id is null and d.attribute_group_id is null)) ");
		} else {
			s.append("and d.attribute_group_id is null and a.PRODUCT_GROUP_ID IS NULL "); 
		}
		
		s.append("union "); 
		
		s.append("select '2' as product_rank, a.*,c.*, d.* from PRODUCT a "); 
		s.append("left outer join product_attribute_xr c on a.product_id = c.product_id "); 
		s.append("left outer join product_attribute d on c.attribute_id = d.attribute_id "); 
		if (isPreview) s.append ("left join product_attribute d2 on d.attribute_id = d2.attribute_group_id ");
		s.append("where a.PARENT_ID in (select PRODUCT_ID from PRODUCT where url_alias_txt in (?) "); 
		s.append("and product_catalog_id=? and status_no=5) and status_no=5 "); 
		if (isPreview) s.append("and (d.attribute_group_id is not null or (d2.attribute_id is null and d.attribute_group_id is null)) ");
		else s.append("and d.attribute_group_id is null "); 
		log.debug("Image SQL: " + s + " | " + prodAlias + " | " + parProdAlias + " | " + catalogId);
		
		PreparedStatement ps = null;
		List<ProductAttributeVO> data = new ArrayList<ProductAttributeVO>();
		List<ProductVO> children = new ArrayList<ProductVO>();
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, prodAlias);
			//ps.setString(2, parProdAlias);
			ps.setString(2, catalogId);
			ps.setString(3, prodAlias);
			ps.setString(4, catalogId);
			
			ResultSet rs = ps.executeQuery();
			for(int i = 0; rs.next(); i++) {
								
				if (i == 0 && rs.getInt("product_rank") == 1) {
					ProductVO pvo =  new ProductVO(rs);
					addVideos(pvo);
					req.setAttribute("fsProductVideos", pvo.getProdAttributes());
					req.setAttribute("fsProductName",pvo);
				} else {
					data.add(new ProductAttributeVO(rs));
				}
				
				if(rs.getInt("product_rank") == 1 && rs.getString("TYPE_NM") != null && rs.getString("TYPE_NM").equals("HTML")) {
					data.add(new ProductAttributeVO(rs));
				}
				
				if (i == 1) req.setAttribute("fsChildName", new ProductVO(rs));
				if (rs.getInt("product_rank") == 2) children.add(new ProductVO(rs));
				
			}
			
			if (children.size() > 0)
				req.setAttribute("fsChildren", children);
			
			// Get the parent product name and add it to the request
			if (data.size() != 0)
				req.setAttribute("fsParentName", getParentName(data.get(0).getParentId()));
			
			//Create a data tree and add it to the module container
			this.putModuleData(data, data.size(), false);
		} finally {
			try {
				ps.close();
			} catch(Exception e){}
		}
	}
	
	/**
	 * Gets the title of the parent product for use in the back link on the product view page
	 * @param parentId
	 * @return
	 * @throws SQLException
	 */
	private String getParentName(String parentId) throws SQLException {
		PreparedStatement ps = dbConn.prepareStatement("SELECT PRODUCT_NM FROM PRODUCT WHERE PRODUCT_ID=?");
		log.debug("Get name of parent: " + parentId);
		ps.setString(1, parentId);
		ResultSet rs = ps.executeQuery();
		if(rs.next()) {
			log.debug(StringUtil.checkVal(rs.getString("PRODUCT_NM")));
			return StringUtil.checkVal(rs.getString("PRODUCT_NM"));
		}
		else {
			log.debug("No results found for some reason.");
			return "";
		}
	}

	/**
	 * this method was designed for Sitemaps and Lucene indexing -- grab
	 * the entire catalog at once, and let the calling action sort it out!
	 * Our responsability (here) is simply to get the data (accurately!)
	 * @param catalogId
	 * @return Tree
	 */
	public Tree loadEntireCatalog(String catalogId) {
		ProductCatalogAction pca = new ProductCatalogAction(this.actionInit);
		pca.setDBConnection(dbConn);
		pca.setAttributes(attributes);
		return pca.loadEntireCatalog(catalogId, false, null);
	}
}
