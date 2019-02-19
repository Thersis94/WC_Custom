package com.depuysynthes.action;

// Java 8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// JSON
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

// WC_Custom
import com.depuysynthes.scripts.DSMediaBinImporterV2;
import com.depuysynthes.solr.ProductCatalogSolrIndex;
// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.commerce.catalog.ProductAttributeContainer;
import com.siliconmtn.commerce.catalog.ProductAttributeVO;
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.session.SMTCookie;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WebCrescendo
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.tools.PageViewReportingAction;
import com.smt.sitebuilder.action.tools.StatVO;
import com.smt.sitebuilder.admin.action.OrganizationAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: DSProductCatalogAction.java<p/>
 * <b>Description: cacheable action that loads an entire product catalog.
 * The Views for this action are basically search/results modules, so they need the entire catalog.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since May 14, 2013
 * @update
 * 		JM - 08.22.16 - added support for dynamic product attributes (tabs), which   
 * 			queries the database to load a list of Mediabin assets.
 ****************************************************************************/
public class DSProductCatalogAction extends SimpleActionAdapter {

	public DSProductCatalogAction() {
		super();
	}

	public DSProductCatalogAction(ActionInitVO actionInit) {
		super(actionInit);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		if (Convert.formatBoolean(req.getParameter("selectList"))) {
			// admintool selectList request
			processAdminSelectList(req);
			return;
		} else {
			// default, retrieve widget data.
			super.retrieve(req);
		}
		
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.ActionController#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		boolean isPreview = page.isPreviewMode();
		String cacheKey = mod.getPageModuleId();
		ModuleVO cachedMod = super.readFromCache(cacheKey);

		if (cachedMod == null || isPreview) {
			cachedMod = loadProductData(req);
			if (cachedMod != null) {
				// If we are in preview mode we do not want this to have anything to do with the cache
				// Otherwise we make use of the cache as normal.
				cachedMod.setCacheable(!isPreview);
				cachedMod.setPageModuleId(cacheKey);

				//If we are in preview mode we do not cache this 
				if (!isPreview)
					super.writeToCache(cachedMod);
			}
		}

		req.setAttribute("products", filterTree(cachedMod.getActionData(), req));

		mod.setActionData(cachedMod.getActionData());
		updatePageData(req, mod, page);
		setAttribute(Constants.MODULE_DATA, mod);
	}


	/**
	 * based on sorting, filtering, and RPP, return a list of products to display in the main column.
	 * @param actionData
	 * @param req
	 * @return
	 */
	private List<ProductCategoryVO> filterTree(Object actionData, ActionRequest req) {
		Tree t = (Tree) actionData;
		if (t == null) return Collections.emptyList();

		req.setValidateInput(Boolean.FALSE); //turn off html encoding for things like "Hip & Pelvis"
		String[] filters = req.getParameterValues("fq");
		if (filters == null) filters = new String[0];
		for (String f : filters)
			log.debug("applying filter : " + f);
		req.setValidateInput(Boolean.TRUE);


		//find all the products that match the hierarchy filter
		List<ProductCategoryVO> data = filterRecords(filters, t);

		//sort the data
		Comparator<ProductCategoryVO> sorter = makeComparator(req);
		Collections.sort(data, sorter);

		log.debug("loaded products=" + data.size());
		return pruneList(data, req);
	}


	/**
	 * returns a list of products/categories matching the filter restrictions
	 * @param filters
	 * @param t
	 * @return
	 */
	private List<ProductCategoryVO> filterRecords(String[] filters, Tree t) {
		//using a map here automatically weeds-out duplicates (when the same product appears in multiple categories)
		Map<String, ProductCategoryVO> data = new HashMap<>();
		for (Node n : t.getPreorderList()) {
			ProductCategoryVO cat = (ProductCategoryVO) n.getUserObject();
			List<ProductVO> prods = cat.getProducts();
			if (prods == null || prods.isEmpty()) continue;

			//test if this categories hierarchy matches the filters
			boolean passes = filters.length == 0; //no filters = everything passes
			for (String f : filters) {
				if (n.getFullPath().indexOf('~') > -1) f +="~"; //use tokenizers as needed
				if (n.getFullPath().startsWith(f)) {
					passes = true;
					log.debug(n.getFullPath() + " matched " + f);
					break;
				}
			}

			if (passes)
				data.put(n.getNodeId(), cat);
		}
		return new ArrayList<>(data.values());
	}


	/**
	 * returns a comparator fitted for sorting the list of products
	 * @param req
	 * @return
	 */
	private Comparator<ProductCategoryVO> makeComparator(ActionRequest req) {
		SMTCookie c = req.getCookie("dsSort");
		String type = c != null ? c.getValue() : req.getParameter("dsSort");
		if ("titleZA".equals(type)) {
			return new ZAComparator();
		} else if ("recentlyAdded".equals(type)) {
			return new RecentComparator();
		}
		//a-z is the default
		return new AZComparator();
	}


	/**
	 * returns a subset of the list considering RPP and page#
	 * @param data
	 * @param req
	 * @return
	 */
	private List<ProductCategoryVO> pruneList(List<ProductCategoryVO> data, ActionRequest req) {
		req.setAttribute("resultCnt", data.size());

		//isolate the 'page' of results being requested (rpp * page#)
		SMTCookie rppCook = req.getCookie("dsRpp");
		int rpp = Convert.formatInteger(rppCook != null ? rppCook.getValue() : req.getParameter("rpp"), 12); //cookie->request->default
		int page = Convert.formatInteger(req.getParameter("page"), 0);
		int start = page * rpp;
		if (start < 0 || start > data.size()) start = 0;
		int end = start + rpp;
		if (end > data.size()) end = data.size();

		//set prev & next URLs for the view
		if (start > 0) {
			req.setAttribute("prev", page-1);
		}
		if (end < data.size()) {
			req.setAttribute("next", page+1);
		}

		log.debug("returning " + (end-start) + " products from idx=" + start);
		return data.subList(start, end);
	}


	/**
	 * load the entire product catalog so it can be cached.
	 * @param req
	 * @return
	 */
	private ModuleVO loadProductData(ActionRequest req) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String catalogId = (String) mod.getAttribute(ModuleVO.ATTRIBUTE_1);
		String rootNodeId = (String) mod.getAttribute(ModuleVO.ATTRIBUTE_2);

		ProductCatalogUtil pc = new ProductCatalogUtil(actionInit);
		pc.setAttributes(getAttributes());
		pc.setDBConnection(dbConn);
		Tree t = pc.loadCatalog(catalogId, rootNodeId, true, req);

		//try to assign pageView stats
		attachPageviewsToCatalog(req, pc, t);

		//process dynamic attributes - goes to Solr for data
		processDynamicAttributes(site.getOrganizationId(), t);

		//sort the tree and bind hierarchy levels
		t.buildNodePaths("~", true);

		//asset nodes and leafs based on the use-case of this tool:
		setLeafs(t.getRootNode());

		//sort the list alphabetically
		sortTreeAlphabetically(t);

		mod.setActionData(t);
		mod.setCacheTimeout(86400*2); //refresh every 48hrs
		return mod;
	}


	/**
	 * @param t
	 */
	protected void sortTreeAlphabetically(Tree t) {
		sortBranchNodes(t.getRootNode(), new AZNodeComparator());
	}

	/**
	 * @param rootNode
	 */
	private void sortBranchNodes(Node node, Comparator<Node> comp) {
		//sort all the leafs at this level
		Collections.sort(node.getChildren(), comp);
		//for each leaf that's actually a branch, traverse down it 
		for (Node c : node.getChildren())
			sortBranchNodes(c, comp);
	}

	/**
	 * asserts who's who at the branch/leaf levels - helper to bootstrap2 theme.
	 * @param rootNode
	 */
	private void setLeafs(Node baseNode) {
		for (Node n : baseNode.getChildren()) {
			ProductCategoryVO vo = (ProductCategoryVO)n.getUserObject();
			//its a leaf if it's CHILDREN have products and no children.
			n.setLeaf(!vo.getProducts().isEmpty() && n.getNumberChildren() == 0);
			if (n.isLeaf()) n.setDepthLevel(10000); //this is a product!

			if (n.getNumberChildren() > 0) {
				setLeafs(n);
				assignParentLeaf(n);
			}
		}
	}


	/**
	 * Sets the parent to be a leaf if it's offspring are all products
	 * @param n
	 */
	private void assignParentLeaf(Node n) {
		for (Node x : n.getChildren()) {
			//move the leaf designator up a level.
			if (x.getDepthLevel() == 10000) {
				n.setLeaf(true);
				x.setLeaf(false);
			}
		}
	}

	/**
	 * iterates the product catalog and attaches pageview #s to each of the products.
	 * @param req
	 * @param pc
	 * @param t
	 */
	@SuppressWarnings("unchecked")
	private void attachPageviewsToCatalog(ActionRequest req, ProductCatalogUtil pc, Tree t) {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		try {
			//load PageView stats
			PageViewReportingAction pva = new PageViewReportingAction(actionInit);
			pva.setDBConnection(dbConn);
			pva.retrieve(req);
			ModuleVO pageViewMod = (ModuleVO) pva.getAttribute(Constants.MODULE_DATA);
			Map<String, StatVO> pageViews = (Map<String, StatVO>)pageViewMod.getActionData();

			//merge stats into the catalog data
			//NOTE: these method calls are what also assign the URLs to each product, 
			//we must always call into them, even when there are no pageViews.  -JM 03.31.14
			//added support for DS-Select subsite.  -JM 05.20.14
			if (page.getDepthLevel() > 2 || "/asc/products".equals(page.getFullPath())) {
				//these are 'this' page plus a query string
				pc.assignPageviewsToCatalog(t.getPreorderList(), pageViews, page.getFullPath() + "/" + attributes.get(Constants.QS_PATH));
			} else {
				// /hcp/products root level page; we'll need to use the data from the Tree to build URL strings for these...
				pc.assignPageviewsToCatalogUsingDivision(t.getPreorderList(), pageViews, 
						"/" + site.getAliasPathName() + "/", page.getFullPath().contains("/products"), false);
			}

		} catch (Exception e) {
			log.error("could not load pageView stats", e);
		}
	}


	/**
	 * Searches through the products for dynamic attributes, which is "type=MEDIABIN".
	 * Takes the list to Solr for data retrieval.
	 * marries the data from Solr back into the products.
	 * @param req
	 * @param pc
	 * @param t
	 */
	private void processDynamicAttributes(String orgId, Tree t) {
		List<Node> prods = t.getPreorderList();

		for (Node n : prods) {
			ProductCategoryVO cat = (ProductCategoryVO) n.getUserObject();
			String prodAlias = cat.getUrlAlias();

			//skip anything that can't be seen on the website
			//skip product families (these aren't actual products)
			if (StringUtil.isEmpty(prodAlias) || cat.getProducts().isEmpty()) 
				continue;

			iterateProducts(cat, orgId);
		}
	}


	/**
	 * called from processDynamicAttributes - iterates the product passed from the 
	 * iteration running in the parent method.  Extracted for readability per SonarQube.
	 * @param cat
	 * @param site
	 */
	private void iterateProducts(ProductCategoryVO cat, String orgId) {
		for (ProductVO product : cat.getProducts()) {
			ProductAttributeContainer attrs = product.getAttributes();
			if (attrs == null || attrs.getAllAttributes() == null || attrs.getAllAttributes().isEmpty())
				continue;

			iterateAttributes(attrs.getAllAttributes(), product.getFullProductName(), orgId);
		}
	}


	/**
	 * called from iterateProducts - iterates the given list of ProductAttributes
	 * to determine if a dynamic lookup is needed, then invokes the appropriate helper method.
	 * Extracted for readability per SonarQube.
	 * @param attrs
	 * @param sousProductName
	 * @param orgId
	 */
	private void iterateAttributes(List<Node> attrs, String sousProductName, String orgId) {
		for (Node n : attrs) {
			ProductAttributeVO attrVo = (ProductAttributeVO) n.getUserObject();

			//skip attributes not bound to this product, 
			//or products with no attributes, 
			//or ones that aren't the MEDIABIN dynamic type we need to perform work on.
			if (attrVo == null || attrVo.getProductAttributeId() == null || !"MEDIABIN".equals(attrVo.getAttributeType())) 
				continue; 

			enactOnAttribute(attrVo, sousProductName, orgId);
		}
	}

	/**
	 * called from iterateProducts above - determines if a dynamic lookup is 
	 * needed for this attribute, and calls the appropriate builder method.
	 * Extracted for readability per SonarQube.
	 * @param attrVo
	 * @param sousProductName
	 * @param orgId
	 */
	private void enactOnAttribute(ProductAttributeVO attrVo, String sousProductName, String orgId) {
		//Note: attrVo is tested for null by the invoking method.
		String type = attrVo.getTitle();
		if ("mediabin-sous".equals(type)) {
			loadMediabinAssetsUsingProdNm(attrVo, sousProductName, orgId);
		} else if ("mediabin-static".equals(type)) {
			loadMediabinAssetsUsingID(attrVo);
		}
	}


	/**
	 * Query the mediabin table using a wildcard on the product_nm 
	 * field, using fullProductName from the ProductVO
	 * @param attrVo
	 * @param product
	 */
	private void loadMediabinAssetsUsingProdNm(ProductAttributeVO attrVo, String sousProductName, String orgId) {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select dpy_syn_mediabin_id, file_nm, title_txt, prod_nm from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("DPY_SYN_MEDIABIN ");
		sql.append("where prod_nm like ? and opco_nm like ? and import_file_cd=? ");
		sql.append("order by title_txt");
		log.debug(sql + " " + sousProductName + " " + orgId);

		MediaBinAssetVO vo;
		Map<String, MediaBinAssetVO> assets = new LinkedHashMap<>();
		MediaBinDistChannels mbChannel = new MediaBinDistChannels(orgId);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, "%" + sousProductName + "%");
			ps.setString(2, "%" + mbChannel.getOpCoNm() + "%");
			ps.setInt(3, mbChannel.getTypeCd());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (!isExactSousMatch(sousProductName, rs.getString(4))) 
					continue;

				vo = new MediaBinAssetVO();
				vo.setDpySynMediaBinId(rs.getString(1));
				vo.setFileNm(rs.getString(2));
				vo.setTitleTxt(rs.getString(3));
				assets.put(vo.getDpySynMediaBinId(),  vo);
			}

		} catch (SQLException sqle) {
			log.error("could not load mediabin assets by sousProductName", sqle);
		}

		attrVo.setValueText(this.generateHtmlTable(assets));
	}


	/**
	 * Make sure we got an exact match to the sousProdName String - SQL limits 
	 * our ability to tokenize or regex the string fully.
	 * Method is public and static so we can use it in the admin JSP via a <jsp:useBean> syntax.
	 * @param sousProductName
	 * @param dbTokenStr
	 * @return
	 */
	public static boolean isExactSousMatch(String sousProductName, String dbTokenStr) {
		String[] tokens = StringUtil.checkVal(dbTokenStr).split(DSMediaBinImporterV2.TOKENIZER);
		for (String t : tokens)
			if (t.equals(sousProductName)) return true;

		return false;
	}


	/**
	 * Query the mediabin table using the pkIds visible in the JSON data.
	 * @param attrVo
	 * @param product
	 */
	private void loadMediabinAssetsUsingID(ProductAttributeVO attrVo) {
		//the assets are already in a specific order; used a LinkedHashMap to keep them that way.
		Map<String, MediaBinAssetVO> assets = new LinkedHashMap<>();
		try {
			JSONArray arr = JSONArray.fromObject(attrVo.getValueText());
			for (int x=0; x < arr.size(); x++)
				assets.put(((JSONObject)arr.get(x)).getString("id"), null);	
		} catch (Exception e) {
			log.error("could not parse mediabin IDs from JSON", e);
			attrVo.setValueText(""); //flush this so we're not printing raw JSON to the end user.
			return;
		}

		// check for nothing to do
		if (assets.isEmpty()) return;

		StringBuilder sql = new StringBuilder(200);
		sql.append("select dpy_syn_mediabin_id, file_nm, title_txt from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("DPY_SYN_MEDIABIN ");
		sql.append("where dpy_syn_mediabin_id in ('~'");
		for (int x=assets.size(); x > 0; x--) sql.append(",?");
		sql.append(")");
		log.debug(sql);

		int x=1;
		MediaBinAssetVO vo;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (String mediabinId : assets.keySet()) ps.setString(x++, mediabinId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				vo = new MediaBinAssetVO();
				vo.setDpySynMediaBinId(rs.getString(1));
				vo.setFileNm(rs.getString(2));
				vo.setTitleTxt(rs.getString(3));
				assets.put(vo.getDpySynMediaBinId(),  vo);
			}

		} catch (SQLException sqle) {
			log.error("could not load mediabin assets by ID", sqle);
		}

		attrVo.setValueText(this.generateHtmlTable(assets));
	}


	/**
	 * takes the data map of mediabin assets and generates HTML to be used in the View.
	 * Normally this is bad practice (view logic in the controller), but if we do the 'work' here it can be cached with the
	 * action and not need to run on every page-view.  It would also muddy the View logic,
	 * which expects the HTML to come from the database (admintool WYSIWYG).
	 * @param assets
	 * @return
	 */
	private String generateHtmlTable(Map<String, MediaBinAssetVO> assets) {
		StringBuilder html = new StringBuilder(300 * assets.size());
		html.append("<table class=\"resourcesTable\" width=\"100%\"><tbody>");

		int x = 0;
		for (MediaBinAssetVO vo: assets.values()) {
			if (vo == null) continue; //typically null for a JSON pointer to an asset that no longer exists in Mediabin
			if (x % 2 == 0 && x > 0) html.append("</tr>"); //close prev row
			if (x % 2 == 0) html.append("<tr>"); //open new row
			if (StringUtil.checkVal(vo.getFileNm()).toLowerCase().endsWith(".pdf")) {
				html.append("<td class=\"resource resourceLink\">");
				html.append("<a class=\"noIcon\" href=\"/json?amid=MEDIA_BIN_AJAX&mbid=");
				html.append(vo.getDpySynMediaBinId()).append("&name=").append(vo.getFileNm()).append("\" target=\"_blank\">");
				html.append("<span class=\"iconPdf\">").append(vo.getTitleTxt()).append("</span></a></td>");
			} else {
				html.append("<td class=\"resource resourceLink\">");
				html.append("<a class=\"noIcon\" href=\"javascript:;\" onclick=\"playVideoInShadowbox('/json?amid=MEDIA_BIN_AJAX&mbid=");
				html.append(vo.getDpySynMediaBinId()).append("','',640,360);\">");
				html.append("<span class=\"iconMov\">").append(vo.getTitleTxt()).append("</span></a></td>");
			}
			++x;
		}
		if (++x % 2 > 0) html.append("<td>&nbsp;</td></tr>"); //finish the row and close it 
		else html.append("</tr>"); //close the row

		html.append("</tbody></table>");
		return html.toString();
	}


	/**
	 * Finds the ProductVO that matches the URL string and puts it on the request object.
	 * @param req
	 * @param mod
	 * @param page
	 */
	private void updatePageData(ActionRequest req, ModuleVO mod, PageVO page) {
		String reqParam1 = req.getParameter(ActionRequest.PARAMETER_KEY + "1");

		//If we don't have a request Parameter to parse, return.
		if (StringUtil.isEmpty(reqParam1)) return;
		log.debug(reqParam1);

		Tree t = (Tree) mod.getActionData();
		List<Node> nodes = t.preorderList();

		/*
		 * Loop over all ProductCategoryVOs in the tree.  If the vo has products,
		 * loop over the products.  If we find a match attempt to update the 
		 * page MetaDesc, MetaKywds and Title.  Also place the product on the 
		 * request to avoid a loop in the Views.  Finally break out of the loops
		 * on a find.
		 */
		for (Node n : nodes) {
			ProductCategoryVO p = (ProductCategoryVO) n.getUserObject();
			if (!p.getProducts().isEmpty()) {
				ProductVO pr = p.getProducts().get(0);
				//Set Page Data and request object
				if (reqParam1.equalsIgnoreCase(pr.getUrlAlias())) {
					this.updatePageVO(page, pr, p);
					req.setAttribute("prodCatVo",  p);
					break;
				}
			}
		}
	}


	/**
	 * overwrite the page title and meta fields with those of the Product we're displaying.
	 * @param page
	 * @param pr
	 * @param p
	 */
	private void updatePageVO(PageVO page, ProductVO pr, ProductCategoryVO p) {
		//page title
		String val = StringUtil.checkVal(p.getCategoryName(), pr.getFullProductName());
		val = StringUtil.checkVal(pr.getProductName(), val);
		val = StringUtil.checkVal(pr.getTitle(), val);

		//page title
		if (!StringUtil.isEmpty(val))
			page.setTitleName(val);

		//meta keywords
		val = pr.getMetaKywds() != null ? pr.getMetaKywds(): p.getMetaKeyword();
		if (!StringUtil.isEmpty(val))
			page.setMetaKeyword(val);

		//meta desc
		val = pr.getMetaDesc() != null ? pr.getMetaDesc(): p.getMetaDesc();
		if (!StringUtil.isEmpty(val))
			page.setMetaDesc(val);

		//set canonical URL of the page to the case-proper UrlAlias of this product
		page.setCanonicalPageUrl(page.getRequestURI() + "/" + attributes.get(Constants.QS_PATH) + pr.getUrlAlias());
	}


	/**
	 * converts the JSON object stored in the product attribute into 
	 * a List<String> assetIds that we can use to lookup in Solr/Mediabin-table/CMS/etc. 
	 * @param jsonText
	 * @return List<String> values
	 * @throws InvalidDataException
	 */
	public static List<String> convertFromJSON(String jsonText) throws InvalidDataException {
		return ProductCatalogSolrIndex.convertFromJSON(jsonText);
	}



	/***************************************************************************
	 * <b>Title:</b> Product Comparators<br/>
	 * <b>Description:</b> Comparators used by this action for view/cosmetics 
	 * <br/>
	 * <b>Copyright:</b> Copyright (c) 2017<br/>
	 * <b>Company:</b> Silicon Mountain Technologies<br/>
	 * @author James McKain
	 * @version 1.0
	 * @since Sep 28, 2017
	 ***************************************************************************/
	public class AZComparator implements Comparator<ProductCategoryVO> {
		public int compare(ProductCategoryVO o1, ProductCategoryVO o2) {
			return StringUtil.checkVal(o1.getCategoryName()).compareToIgnoreCase(o2.getCategoryName());
		}
	}
	public class ZAComparator implements Comparator<ProductCategoryVO> {
		public int compare(ProductCategoryVO o1, ProductCategoryVO o2) {
			return StringUtil.checkVal(o2.getCategoryName()).compareToIgnoreCase(o1.getCategoryName());
		}
	}
	public class RecentComparator implements Comparator<ProductCategoryVO> {
		public int compare(ProductCategoryVO o1, ProductCategoryVO o2) {
			return o1.getLastUpdate().compareTo(o2.getLastUpdate());
		}
	}
	public class AZNodeComparator implements Comparator<Node> {
		public int compare(Node o1, Node o2) {
			return StringUtil.checkVal(o1.getNodeName()).compareToIgnoreCase(o2.getNodeName());
		}
	}
	
	/**
	 * Retrieves/builds select list data for the DS Product Catalog Widget
	 * @param req
	 */
	private void processAdminSelectList(ActionRequest req) {
		String catId = req.getParameter("productCatalogId");
		StringBuilder sql = buildSelectListQuery(catId);
		List<Node> items = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			if (StringUtil.isEmpty(catId)) {
				ps.setString(1, req.getParameter(OrganizationAction.ORGANIZATION_ID));
			} else {
				ps.setString(1, catId);
				ps.setString(2, catId);
			}
			// Use a ProductVO as generic VO for the data
			ResultSet rs = ps.executeQuery();
			items = parseSelectListResults(rs,catId);
		} catch (SQLException sqle) {
			log.error("Error retrieving product catalog items, ", sqle);
		}
		super.putModuleData(items);
	}


	/**
	 * Builds query for list method.
	 * @param catalogId
	 * @return
	 */
	private StringBuilder buildSelectListQuery(String catalogId) {
		StringBuilder sql;
		if (StringUtil.isEmpty(catalogId)) {
			sql = new StringBuilder(100);
			sql.append("select product_catalog_id, catalog_nm from product_catalog ");
			sql.append("where organization_id=? and status_no=5 order by catalog_nm ");
		} else {
			sql = new StringBuilder(500);
			sql.append("select 1 as rank, product_catalog_id, category_nm, product_category_cd ");
			sql.append("from PRODUCT_CATEGORY ");
			sql.append("where PARENT_CD is null and PRODUCT_CATALOG_ID=? and category_group_id is null ");
			sql.append("union ");
			sql.append("select 2 as rank, a.product_catalog_id, a.category_nm, a.product_category_cd "); 
			sql.append("from PRODUCT_CATEGORY a ");
			sql.append("inner join product_category b on a.parent_cd=b.product_category_cd ");
			sql.append("where b.PARENT_CD is null and b.PRODUCT_CATALOG_ID=? and a.category_group_id is null ");
			sql.append("and b.category_group_id is null ");
			sql.append("order by rank, category_nm ");
		}
		log.debug("prod cat util list query: " + sql);
		return sql;
	}


	/**
	 * Parses the result set into a List of Nodes
	 * @param rs
	 * @param catalogId
	 * @return
	 * @throws SQLException
	 */
	private List<Node> parseSelectListResults(ResultSet rs, String catalogId) 
			throws SQLException {
		Node item = null;
		List<Node> items = new ArrayList<>();
		while (rs.next()) {
			item = new Node();
			if (StringUtil.isEmpty(catalogId)) {
				// is product catalog item
				item.setNodeId(rs.getString("product_catalog_id"));
				item.setNodeName(rs.getString("catalog_nm"));
			} else {
				// is product category item
				item.setNodeId(rs.getString("product_category_cd"));
				item.setNodeName(rs.getString("category_nm"));
				item.setDepthLevel(rs.getInt("rank"));
			}
			items.add(item);
		}
		return items;
	}
}