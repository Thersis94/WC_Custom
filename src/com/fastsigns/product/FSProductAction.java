package com.fastsigns.product;

// JDK 1.6.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;


//SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
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
import com.siliconmtn.commerce.catalog.ProductAttributeContainer;
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
	
	private final int CATEGORIES = 0;
	private final int PRODUCTS = 1;
	
	/**
	 * 
	 */
	public FSProductAction() {
	}

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
			build(req);
			return;
		}
		
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		
		// Get the individual parameters to determine what data to retrieve
		ModuleVO mod = ((ModuleVO) getAttribute(Constants.MODULE_DATA));
		log.debug(req.getRequestURI());
		String[] catArr = StringUtil.checkVal(mod.getAttribute(SBModuleVO.ATTRIBUTE_1)).split("~");
		if (catArr == null || catArr.length != 2) log.error(StringUtil.getToString(catArr, false, false, ","));
		
		String categoryId = catArr[1];
		
		String pId = StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + "1"));
		String prodChildKey = StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + "2"));
		String catImageKey = StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + "3"));
		if(StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + "4")).length() > 0) {
			prodChildKey = catImageKey;
			catImageKey = StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + "4"));
		}
		
		// Get the product catalog from cache if possible or load the entire thing and put it in there now
		ModuleVO cModule = loadCatalogFromCache(page, catArr[0]);
		@SuppressWarnings("unchecked")
		List<Tree> catalog = (List<Tree>) cModule.getActionData();
		
		log.debug("Gathering information for: " + pId + "|" + prodChildKey + "|" + catImageKey + "|" + categoryId);
		
		Node n = null;
		int size = 0;
		// Determine what page is being requested and get the data
		try {
			if (pId.length() == 0) {
				log.debug("categories by type");
				n = getNode(catalog, categoryId, CATEGORIES);
				this.putModuleData(n, n.getNumberChildren() + 1, false);
				
			} else if (pId.length() > 1 && prodChildKey.length() == 0) {
				log.debug("products by category");
				n = this.getNode(catalog, pId, CATEGORIES);
				if (n.getUserObject() != null) size = ((ProductCategoryVO)n.getUserObject()).getProducts().size() + 1;
				this.putModuleData(n, size, false);
				buildCanonicals(req, pId, null, null, null);
				
			} else if (catImageKey.length() > 0) {
				log.debug("product info");
				n = this.getNode(catalog, catImageKey, PRODUCTS);
				if (n.getUserObject() != null) size = ((ProductVO)n.getUserObject()).getAttributes().getAllAttributes().size() + 1;
				this.putModuleData(n, size, false);
				buildCanonicals(req, prodChildKey, catImageKey, pId, n);
				
			} else {
				log.debug("child products");
				n = this.getNode(catalog, prodChildKey, PRODUCTS);
				// We need to set the parent name for the product here because we are going to the
				// category list for the current parent for the product at the time of this request
				// since the product can be under multiple categories we do not want this to be cached
				n.setParentName(this.getNode(catalog, pId, CATEGORIES).getNodeName());
				this.putModuleData(n, n.getNumberChildren() + 1, false);
				buildCanonicals(req, prodChildKey, null, pId, n);
				
			}
		} catch(Exception e) {
			log.error("Unable to retrieve catalog", e);
		}
		req.setAttribute("fsProductName", n.getNodeName());
		// Assign the page title and other data
		this.assignPageInfo(page, n);
	}
	
	/**
	 * Loads the entire catalog, either from cache or from database
	 * @param page
	 * @param catalogId
	 * @return
	 * @throws ActionException
	 */
	private ModuleVO loadCatalogFromCache(PageVO page, String catalogId) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		ModuleVO cachedMod = null;
		
		log.debug("Loading Catalog with id: " + catalogId);
		if (!page.isPreviewMode())
			cachedMod = super.readFromCache("FS_CAT_"+catalogId);
		
		if (cachedMod == null || cachedMod.getActionData() == null) {
			if(cachedMod != null) log.error("Something is wrong with the cached product catalog " + catalogId);
			log.debug("Value not Cached, reading from DB!");
			cachedMod = new ModuleVO();
			cachedMod.setActionId("FS_CAT_"+catalogId);
			cachedMod.setActionData(this.loadCatalog(catalogId, page.isPreviewMode()));
			
	        if (!page.isPreviewMode()) {
	        	cachedMod.setCacheable(true);
	        	cachedMod.setPageModuleId(catalogId);
	        	
	        	// We only cache this with the catalogId group because even if the page changes, the data that is cached remains the same
	        	cachedMod.addCacheGroup(catalogId);
	        	super.writeToCache(cachedMod);
	        }
		}
		log.debug("Products and categories from catalog " + catalogId + " loaded.");
		
    	//cached object should be returned by copy, not reference (when leveraging cache)
		mod.setActionData(cachedMod.getActionData());
		setAttribute(Constants.MODULE_DATA, mod);
		log.debug(mod.getActionData());
		return mod;
	}
	
	/**
	 * Loads the entire catalog from the database. and places it into two trees.
	 * One that contains all the categories and their products.
	 * A second that contains all the products with their parent child relations
	 * as well as all their attributes
	 * @param catalogId
	 * @param isPreview
	 * @return
	 * @throws ActionException 
	 */
	private List<Tree> loadCatalog(String catalogId, boolean isPreview) throws ActionException {
		List<Node> categories = new ArrayList<Node>();
		List<Node> products = new ArrayList<Node>();
		List<Tree> catalog = new ArrayList<Tree>();
		String sql = buildLoadCatalogSql(isPreview);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, catalogId);
			ps.setString(2, catalogId);
			ResultSet rs = ps.executeQuery();
			log.debug(sql.toString()+"|"+catalogId);
			
			String previous=null;
			Node n;
			ProductVO p;
			while (rs.next()) {
				if (rs.getInt("rank") == 1) {
					if (!rs.getString("PRODUCT_CATEGORY_CD").equals(previous)) {
						previous = rs.getString("PRODUCT_CATEGORY_CD");
						if (rs.getString("PARENT_CD") == null) {
							n = new Node(rs.getString("PRODUCT_CATEGORY_CD"), null);
						} else {
							n = new Node(rs.getString("URL_ALIAS_TXT"), rs.getString("PARENT_CD"));
						}
						n.setUserObject(new ProductCategoryVO(rs));
						n.setNodeName(rs.getString("category_nm"));
						categories.add(n);
					}
					p = new ProductVO();
					p.setProductId(rs.getString("PRODUCT_ID"));
					p.setUrlAlias(rs.getString("CATEGORY_PRODUCT_URL"));
					p.setImage(rs.getString("PRODUCT_IMAGE"));
					p.setShortDesc(rs.getString("PRODUCT_DESCRIPTION"));
					p.setProductName(rs.getString("C_PROD_NM"));
					p.setDescText(rs.getString("DESC_TXT"));
					((ProductCategoryVO)categories.get(categories.size()-1).getUserObject()).addProduct(p);
				} else {
					n = new Node(rs.getString("URL_ALIAS_TXT"), rs.getString("PARENT_CD"));
					p = new ProductVO(rs);
					p.setProductName(rs.getString("CATEGORY_NM"));
					p.setDescText(rs.getString("CATEGORY_DESC"));
					
					n.setParentName(rs.getString("C_PROD_NM"));
					n.setNodeName(rs.getString("CATEGORY_NM"));
					
					n.setUserObject(p);
					products.add(n);
				}
			}
		} catch (SQLException sqle) {
			log.error("cannot load catalog " + catalogId, sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		loadAttributes(products, catalogId, isPreview);
		
		Tree pTree = new Tree(products);

		setLeaves(pTree, categories);
		catalog.add(CATEGORIES, new Tree(categories));
		catalog.add(PRODUCTS, pTree);
		
		return catalog;
	}
	
	/**
	 * Set which products in the category list don't have any children or images
	 * that would be used as a gallery in lieu of children
	 * @param products
	 * @param categories
	 */
	private void setLeaves(Tree products, List<Node> categories) {
		String s;
		Node foundNode;
		for (Node n : categories) {
			for (ProductVO p : ((ProductCategoryVO)n.getUserObject()).getProducts()) {
				s = p.getUrlAlias();
				if (s == null) continue;
				foundNode = products.findNode(s);
				if (foundNode == null || foundNode.getUserObject() == null) continue;
				if (((ProductVO) foundNode.getUserObject()).getAttrib1Txt() != null ||
						foundNode.getChildren().size() > 0)
					p.setAttrib1Txt("notLeaf");
			}
		}
	}

	/**
	 * Creates the sql query we will use to build the product catalog.
	 * @param isPreview
	 * @return
	 */
	private String buildLoadCatalogSql(boolean isPreview) {
		StringBuilder sql = new StringBuilder(2100);
		sql.append("SELECT 1 as rank, pc.PRODUCT_CATEGORY_CD, pc.PARENT_CD, p.PRODUCT_ID, pc.CATEGORY_NM, pc.CATEGORY_DESC, pc.META_KYWD_TXT, pc.META_DESC, ");
		sql.append("pc.TITLE_NM, pc.ORDER_NO, pc.IMAGE_URL, pc.THUMBNAIL_IMG, pc.CUST_CATEGORY_ID, pc.URL_ALIAS_TXT, pc.SHORT_DESC, pc.ATTRIB1_TXT, p.URL_ALIAS_TXT as CATEGORY_PRODUCT_URL, p.IMAGE_URL as PRODUCT_IMAGE, p.PRODUCT_NM as C_PROD_NM, p.SHORT_DESC as PRODUCT_DESCRIPTION, p.PRODUCT_NM, p.DESC_TXT, p.DISPLAY_ORDER_NO ");
		sql.append("FROM PRODUCT_CATEGORY pc ");
		sql.append("inner join PRODUCT_CATEGORY_XR pcx on pc.PRODUCT_CATEGORY_CD = pcx.PRODUCT_CATEGORY_CD ");
		if (isPreview){
			sql.append("or (pc.CATEGORY_GROUP_ID=pcx.PRODUCT_CATEGORY_CD and pc.CATEGORY_GROUP_ID is not null) ");
		}
		sql.append("inner join PRODUCT p on p.PRODUCT_ID = pcx.PRODUCT_ID and p.STATUS_NO = 5 ");
		sql.append("where pc.product_catalog_id=? and pc.active_flg = 1 ");
		if (isPreview){
			sql.append("and pc.PRODUCT_CATEGORY_CD not in (select pc1.PRODUCT_CATEGORY_CD from PRODUCT_CATEGORY pc1 left join PRODUCT_CATEGORY pc2 on pc1.PRODUCT_CATEGORY_CD=pc2.CATEGORY_GROUP_ID where pc2.CATEGORY_GROUP_ID is not null) ");
			sql.append("and p.PRODUCT_ID not in (select p1.PRODUCT_ID from PRODUCT p1 left join PRODUCT p2 on p1.PRODUCT_ID=p2.PRODUCT_GROUP_ID where p2.PRODUCT_GROUP_ID is not null) ");
		} else {
			sql.append("and CATEGORY_GROUP_ID is null and p.PRODUCT_GROUP_ID is null ");
		}
		
		sql.append("union ");
		
		sql.append("SELECT 2 as rank, null, p2.URL_ALIAS_TXT as PARENT_CD, p.PRODUCT_ID, p.PRODUCT_NM as CATEGORY_NM, p.DESC_TXT as CATEGORY_DESC, p.META_KYWD_TXT, p.META_DESC, ");
		sql.append("p.TITLE_NM, null, p.IMAGE_URL, p.THUMBNAIL_URL, null as CUST_CATEGORY_ID, p.URL_ALIAS_TXT, p.SHORT_DESC, null as ATTRIB1_TXT, null as CATEGORY_PRODUCT_URL, p2.PRODUCT_NM as C_PROD_NM, null, null, null, null, null ");
		sql.append("FROM PRODUCT p ");
		sql.append("left join PRODUCT p2 on p.PARENT_ID = p2.PRODUCT_ID ");
		sql.append("WHERE p.product_catalog_id=? and p.status_no=5 ");
		if (isPreview) {
			sql.append("and p.product_id not in (select product_group_id from product where product_group_id is not null and product_group_id != product_id) ");
		} else {
			sql.append("and p.PRODUCT_GROUP_ID is null ");
		}
		
		sql.append("ORDER BY rank, ORDER_NO, PRODUCT_CATEGORY_CD, DISPLAY_ORDER_NO ");
		
		return sql.toString();
	}
	
	/**
	 * Loads all the attributes for this catalog and assigns them to products
	 * @param data
	 * @param catalogId
	 * @param isPreview
	 */
	private void loadAttributes(List<Node> data, String catalogId, boolean isPreview) {
		Map<String, ProductAttributeContainer> attributes = loadAttributesFromDatabase(catalogId, isPreview);
		ProductVO p;
		for (Node n : data) {
			p = (ProductVO) n.getUserObject();
			if (attributes.get(p.getUrlAlias()) == null) {
				p.setAttributes(new ProductAttributeContainer());
				continue;
			}
			p.setAttributes((ProductAttributeContainer) attributes.get(p.getUrlAlias()));
			for (Node a : ((ProductAttributeContainer) attributes.get(p.getUrlAlias())).getAllAttributes()) {
				if (a.getNodeName().equals("IMAGE")) {
					p.setAttrib1Txt("notLeaf");
					break;
				}
			}
		}
	}

	/**
	 * Loads all the attributes for the catalog
	 * @param catalogId
	 * @param isPreview
	 * @return
	 */
	private Map<String, ProductAttributeContainer> loadAttributesFromDatabase(String catalogId, boolean isPreview) {
		StringBuilder sql = new StringBuilder(800);
		Map<String, ProductAttributeContainer> attributes = new HashMap<String, ProductAttributeContainer>();
		String lastProductId = "";
		Node n;
		VideoVO v;
		
		sql.append("select p.URL_ALIAS_TXT, pa.TYPE_NM, pax.*, v.* FROM PRODUCT_ATTRIBUTE_XR pax ");
		sql.append("left join product p on p.PRODUCT_ID = pax.PRODUCT_ID ");
		sql.append("left join PRODUCT_ATTRIBUTE pa on pa.ATTRIBUTE_ID = pax.ATTRIBUTE_ID ");
		sql.append("left join VIDEO v on pax.attrib3_txt = v.video_id ");
		sql.append("where ");
		if(isPreview) {
			sql.append("p.product_id not in (select product_group_id from product where product_group_id is not null and product_group_id != product_id) ");
			sql.append("and pax.attribute_id not in (select attribute_group_id from product_attribute where attribute_group_id is not null and attribute_group_id != attribute_id) ");
		} else {
			sql.append("pa.ATTRIBUTE_GROUP_ID is null and p.product_group_id is null ");
		}
		sql.append("and p.status_no=5 and p.product_catalog_id=? ");
		sql.append("order by p.url_alias_txt ");
		log.debug(sql.toString()+"|"+catalogId);
		try {
			PreparedStatement ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1,catalogId);
			
			ResultSet rs = ps.executeQuery();
			List<Node> data = new ArrayList<Node>();
			
			while(rs.next()) {
				//when the productId changes, roll the data into an AttibuteContainer and store it
				if (!lastProductId.equals(rs.getString("URL_ALIAS_TXT"))) {
					attributes.put(lastProductId, new ProductAttributeContainer(data));
					data = new ArrayList<Node>();
					lastProductId = StringUtil.checkVal(rs.getString("URL_ALIAS_TXT"));
				}
				
				if (rs.getString("video_id") != null) {
					v = new VideoVO(rs);
					v.setAspectRatioHeight(Integer.parseInt(StringUtil.checkVal(rs.getString("ATTRIB1_TXT"))));
					v.setAspectRatioWidth(Integer.parseInt(StringUtil.checkVal(rs.getString("ATTRIB2_TXT"))));
					v.setAttribute("position", StringUtil.checkVal(rs.getString("VALUE_TXT")));
					n = new Node(rs.getString("product_attribute_id"), rs.getString("URL_ALIAS_TXT"));
					n.setUserObject(v);
				} else {
					n = new Node(rs.getString("product_attribute_id"), rs.getString("URL_ALIAS_TXT"));
					n.setUserObject(new ProductAttributeVO(rs));
				}
				n.setNodeName(rs.getString("type_nm"));
				data.add(n);
			}
			if (data.size() > 0)
				attributes.put(lastProductId, new ProductAttributeContainer(data));
		} catch (SQLException e) {
			log.error("Unable to get product attributes for catalog " + catalogId, e);
		}
		
		
		return attributes;
	}
	
	/**
	 * Create the canonical url for this page based on supplied strings
	 */
	private void buildCanonicals(SMTServletRequest req, String firstItem, String secondItem, String thirdItem, Node n) {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		
		// Check if there is an attribute that sets the urls.  If so we use that and ignore everything else
		if (n != null && checkAttribUrl((ProductVO)n.getUserObject(), page)) return;

		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		if (!req.hasParameter("prefix") && thirdItem != null) {
			page.setCanonicalPageUrl(buildUrl(req, thirdItem, firstItem, secondItem, false));
		} else {
			page.setCanonicalPageUrl(buildUrl(req, firstItem, secondItem, null, false));
		}
		if (!Convert.formatBoolean(site.getMobileFlag())){
			if (req.hasParameter("prefix")) {
				page.setCanonicalMobileUrl(buildUrl(req, firstItem, secondItem, null, true));
			} else {
				page.setCanonicalMobileUrl(buildUrl(req, thirdItem, firstItem, secondItem, true));
			}
		}
	}
	
	/**
	 * Creates a canonical url based on items passed to the method and what kind of url we are creating.
	 * @param req
	 * @param firstItem
	 * @param secondItem
	 * @param thirdItem
	 * @param isMobile
	 * @return
	 */
	private String buildUrl(SMTServletRequest req, String firstItem, String secondItem, String thirdItem, boolean isMobile) {
		StringBuilder url = new StringBuilder(200);
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		
		log.debug("Building URL with " + firstItem + " " +secondItem + " " + thirdItem);

		if(isMobile) url.append("http://"+site.getMobileSiteUrl());
		
		if (req.hasParameter("prefix")) {
			appendLevel(url, req.getParameter("prefix"));
		} else if (isMobile || req.getRequestURL().indexOf("qs") > -1){
			if(isMobile) appendLevel(url, "products");
			appendLevel(url, req.getRequestURI().substring(req.getRequestURI().lastIndexOf('/')+1));
			appendLevel(url, "qs/");
		} else {
			appendLevel(url, req.getRequestURI().substring(req.getRequestURI().lastIndexOf('/')) + "-");
		}
		
		appendLevel(url, firstItem);
		
		appendLevel(url, secondItem);
		
		if (!req.hasParameter("prefix")) {
			appendLevel(url, thirdItem);
		}
		
		return url.toString().toLowerCase();
	}
	
	/**
	 * Ensures that whatever we are trying to add to the url is not null and
	 * end up with more or fewer slashes than there are supposed to be.
	 * @param url
	 * @param level
	 */
	private void appendLevel(StringBuilder url, String level) {
		if (level == null) return;
		if ((url.length() == 0 || (url.charAt(url.length()-1) != '/') && url.charAt(url.length()-1) != '-')
				&& level.charAt(0) != '/') url.append("/");
		url.append(level);
	}
	
	/**
	 * Check for an attribute that sets an override for the canonical and relative urls.
	 * @param req
	 * @return
	 */
	private boolean checkAttribUrl(ProductVO p, PageVO page) {
		if (p == null) return false;
		for (Node n : p.getAttributes().getAllAttributes()) {
			if (n.getUserObject() instanceof ProductAttributeVO && 
					"CANONURL".equals(((ProductAttributeVO) n.getUserObject()).getAttributeType())) {
				ProductAttributeVO attr = (ProductAttributeVO) n.getUserObject();
				page.setCanonicalPageUrl(attr.getAttribute1());
				page.setCanonicalMobileUrl(attr.getAttribute2());
				
				log.debug("Got canonical from product attribute");
				return true;
				
			}
		}
		return false;
	}
	
	/**
	 * Set the page info from the productVO if that is what we are dealing with
	 * @param page
	 * @param n
	 */
	private void assignPageInfo(PageVO page, Node n) {
		log.debug("Assigning Page Information");
		if (n.getUserObject() instanceof ProductVO ) {
			ProductVO product = (ProductVO)n.getUserObject(); 
			
			// Set the title, meta keyword and meta desc only if we get something that will override the default
			if (StringUtil.checkVal(product.getTitle()).length() > 0){
				page.setTitleName(product.getTitle());
			}

			if (StringUtil.checkVal(product.getMetaDesc()).length() > 0) {
				page.setMetaDesc(product.getMetaDesc());
			}
			
			if (StringUtil.checkVal(product.getMetaKywds()).length() > 0) {
				page.setMetaKeyword(product.getMetaKywds());
			}
		} else if (n.getUserObject() instanceof ProductCategoryVO) {
			ProductCategoryVO cat = (ProductCategoryVO)n.getUserObject();
			
			// Set the title, meta keyword and meta desc only if we get something that will override the default
			if (StringUtil.checkVal(cat.getTitle()).length() > 0){
				page.setTitleName(cat.getTitle());
			}

			if (StringUtil.checkVal(cat.getMetaDesc()).length() > 0) {
				page.setMetaDesc(cat.getMetaDesc());
			}
			
			if (StringUtil.checkVal(cat.getMetaKeyword()).length() > 0) {
				page.setMetaKeyword(cat.getMetaKeyword());
			}
			
		}
	}
		
	/**
	 * Gets the requested node from the requested tree
	 * @param catalog
	 * @param itemAlias
	 * @param list
	 * @return
	 * @throws SQLException
	 */
	private Node getNode(List<Tree> catalog, String itemAlias, int list) {
		Node n = catalog.get(list).findNode(itemAlias);
		if(n == null) {
			log.error(itemAlias + " does not exist in " + catalog.get(list));
			n = new Node();
		}
		return n;
	}
	
	/**
	 * Gets the minimal amount of information needed to create a list of the 
	 * different categories and products under them for use in the menu
	 * @param req
	 */
	public void build(SMTServletRequest req) throws ActionException {
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
					node.setFullPath(rs.getString("CUST_CATEGORY_ID") + rs.getString("URL_ALIAS_TXT"));
					node.setParentId(menuName);
					node.setUserObject(rs.getInt("ORDER_NO"));
					nodes.add(node);
				}
				node = new Node();
				node.setNodeName(rs.getString("PRODUCT_NM"));
				node.setFullPath(rs.getString("CUST_CATEGORY_ID") + rs.getString("prod_url_alias"));
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
		StringBuilder sql = new StringBuilder(1000);
		sql.append("select pc.CATEGORY_NM, pc.CUST_CATEGORY_ID, pc.URL_ALIAS_TXT, p.PRODUCT_NM, p.URL_ALIAS_TXT as 'prod_url_alias', pc.ORDER_NO ");
		sql.append("from PRODUCT_CATEGORY pc ");
		sql.append("inner join PRODUCT_CATEGORY_XR pcx on pc.PRODUCT_CATEGORY_CD = pcx.PRODUCT_CATEGORY_CD ");
		sql.append("inner join PRODUCT p on pcx.PRODUCT_ID = p.PRODUCT_ID ");
		if (isPreview) {
			sql.append("left join PRODUCT p2 on p.PRODUCT_ID = p2.PRODUCT_GROUP_ID ");
			sql.append("left join PRODUCT_CATEGORY pc2 on pc.PRODUCT_CATEGORY_CD = pc2.PRODUCT_CATEGORY_CD ");
		}
		sql.append("where pc.parent_cd = ? ");
		if (isPreview) {
			sql.append("and (pc.category_group_id is not null or (pc2.category_group_id is null and pc.category_group_id is null)) ");
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
		StringBuilder sql = new StringBuilder(230);
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
	 * this method was designed for Sitemaps and Lucene indexing -- grab
	 * the entire catalog at once, and let the calling action sort it out!
	 * Our responsibility (here) is simply to get the data (accurately!)
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
