package com.depuysynthes.action;
//Java 8
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//Smt base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.commerce.catalog.ProductAttributeContainer;
import com.siliconmtn.commerce.catalog.ProductAttributeVO;
import com.siliconmtn.commerce.catalog.ProductCategoryContainer;
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
//WebCrescendo
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.menu.MenuBuilder;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: AjaxMenuLoader.java<p/>
 * <b>Description: Loads the two product catalogs needed for the DePuySynthes.com 
 * website ("mega-menu").  The View will use the catalogs to return rendered HTML.
 * The View also uses MenuContainer, which has already been loaded and put on 
 * the request object for us.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Apr 21, 2013
 * @updates
 * 	James McKain, 09-23-13: 
 * 		added merging of Products into the Procedures catalog, aleviates 
 * 		secondary AJAX calls from mega-menu.
 *  RJR code clean up 05/17/201
 ****************************************************************************/
public class AjaxMenuLoader extends SimpleActionAdapter {

	public AjaxMenuLoader() {
	}

	/**
	 * @param arg0
	 */
	public AjaxMenuLoader(ActionInitVO arg0) {
		super(arg0);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.info("Starting menu loader - retrieve");

		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
 		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		
		if (req.hasParameter(Constants.AJAX_MODULE_ID) 
				&& !req.getParameter(Constants.AJAX_MODULE_ID).startsWith("hcp_") 
				&& !req.getParameter(Constants.AJAX_MODULE_ID).startsWith("asc_")) {
			loadSitePageMenu(req, site, mod);
			return;
		}

 		Map<String, ProductCategoryContainer> catalogs = new HashMap<>();
 		ProductCatalogUtil pc = new ProductCatalogUtil(this.actionInit);
		pc.setDBConnection(dbConn);
		pc.setAttributes(attributes);

		// retrieve the product catalog from the DB, pruned by prodRootNode
		String[] tokens = pc.separateIds((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		mod.addCacheGroup(tokens[0]); //the catalogId
		String prodRootNode = tokens[1];
		Tree prodTree = pc.loadCatalog(tokens[0], prodRootNode, true, req);
   		
		// retrieve the procedure catalog from the DB, pruned by procRootNode
   		tokens = pc.separateIds((String)mod.getAttribute(ModuleVO.ATTRIBUTE_2));
		mod.addCacheGroup(tokens[0]); //the catalogId
		String procRootNode = tokens[1];
		//the merge method will put this onto the Map for us...
		Tree procTree = pc.loadCatalog(tokens[0], procRootNode, true, req);

		String attributeId = "DS_PROC_TABS_PRODUCTS";
		if (site.getOrganizationId().indexOf("_EMEA") > -1)
			attributeId = "DS_PROC_TABS_PRODUCTS_EMEA";
   		//merge products into the procedures they're assigned to
   		procTree = mergeProductsIntoProcedures(prodTree, procTree, attributeId);

   		catalogs.put("procedures", new ProductCategoryContainer(procTree));
   		catalogs.put("products", new ProductCategoryContainer(prodTree));
   		
   		
        //set catalogs/data onto the original module VO.
   		mod.setActionData(catalogs);
   		setAttribute(Constants.MODULE_DATA, mod);
	}
	
	/**
	 * relates the procedures to products
	 */
	private Tree mergeProductsIntoProcedures(Tree prodTree, Tree procTree, String attributeId) {
		//TODO this method is large and complex, break down this method when possible
		List<Node> procsList = procTree.preorderList(true);
		List<Node> prodsList = prodTree.preorderList(true);
		List<String> completedProcs = new ArrayList<>();
		
		//turn the Products Collection into a Map we can grab-at easily using productId
		Map<String, Node> products = new HashMap<>();
		for (Node n : prodsList) {
			ProductCategoryVO vo = (ProductCategoryVO) n.getUserObject();
			if (vo == null) continue;
			
			n.setFullPath("product"); //indicator to mega-menu that this is a product, not a procedure.
			products.put(n.getNodeId(), n);
		}
		
		//loop through the Procedures; only caring about the "product"-level nodes (not categories)
		for (Node n : procsList) {
			ProductCategoryVO vo = (ProductCategoryVO) n.getUserObject();
			if (vo == null || vo.getProducts() == null || vo.getProducts().isEmpty()) continue;
			if (StringUtil.isEmpty(vo.getUrlAlias())) continue;

			// After we complete the list it will be pruned and all the child parent relationships will be re-examined.
			// If the same procedure has products assosiated with it in multiple branches we will end up with
			// duplicates of every product for the procedure showing up on the site.
			if (completedProcs.contains(n.getNodeId())) continue;

			//load the Procedure and it's attribute container
			ProductVO proc = vo.getProducts().get(0);

			ProductAttributeContainer attrs = proc.getAttributes();
			if (attrs == null) continue; //no attributes whatsoever!
			attrs.setCurrentAttributeId(attributeId);
			Node attrNode = attrs.getCurrent();
			if (attrNode == null) continue; //no products to merge for this one.
			ProductAttributeVO attrib = (ProductAttributeVO) attrNode.getUserObject();

			//for each product bound to this procedure, tie a copy of that product to the Procedures Tree
			String[] ids = StringUtil.checkVal(attrib.getValueText()).split(",");
			for (String productId : ids) {
				Node prodVo = products.get(productId);
				if (prodVo == null) continue; //product no longer exists, or possibly inactive
				
				//if the node already has 'this' child attached, don't re-attach it
				boolean skipRecord = false;
				for (Node c : n.getChildren()) {
					if (c != null && c.getNodeId().equals(prodVo.getNodeId())) skipRecord = true;
				}
				if (skipRecord) continue;

				ProductCategoryVO catVo = (ProductCategoryVO) prodVo.getUserObject();
				Node prodPar = products.get(catVo.getParentCode());
				if (prodPar != null) {
					ProductCategoryVO parCatVo = (ProductCategoryVO) prodPar.getUserObject();
					//if the parent has no URL it's probably a sub-category.  iterate up and grab it's parent (the Division!)
					if (StringUtil.isEmpty(parCatVo.getUrlAlias())) {
						prodPar = products.get(prodPar.getParentId());
						parCatVo = (ProductCategoryVO) prodPar.getUserObject();
					}
					if (StringUtil.isEmpty(parCatVo.getUrlAlias())) {
						prodPar = products.get(prodPar.getParentId());
						parCatVo = (ProductCategoryVO) prodPar.getUserObject();
					}
					catVo.setCategoryUrl(parCatVo.getUrlAlias()); //placeholder for the Division URL this product belongs in
					
				}
				
				Node prodNode = new Node(prodVo.getNodeId(), n.getNodeId(), n.getNodeName());
				prodNode.setNodeName(prodVo.getNodeName());
				prodNode.setDepthLevel(n.getDepthLevel()+1);
				prodNode.setUserObject(catVo);
				prodNode.setFullPath(prodVo.getFullPath());
				
				n.addChild(prodNode);
			}
			
			//if the node has no childen we don't want to display it
			//set a name we can use in the JSPs to filter on, without removing it from the Tree which breaks things
			//we can't key off the presence of children, because valid products (@lowest level) will have no children either.
			if (n.getChildren() == null || n.getChildren().isEmpty())
				n.setParentName("nodisplay");
			
			// Add this procedure to the list of procedures we have set up the products for
			completedProcs.add(n.getNodeId());
			
		}
		
		return procTree;
	}

	/**
	 * load the site's menus instead of a product catalog.  This is used in the Providers section.
	 * Ajax calls don't naturally put the site menus on the request, so we need to retrieve them ourselves.
	 * @param req
	 * @param site
	 * @param mod
	 */
	private void loadSitePageMenu(ActionRequest req, SiteVO site, ModuleVO mod) {
		log.debug("loading site-page menus");
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		if (role == null) role = new SBUserRole(site.getSiteId());
		
		//AI is being hard-coded because the ajax servlet only runs at the parent-site level, and we're trying to load a sub-sites menus. - JM 01-13-14
		ActionInitVO ai = new ActionInitVO();
		ai.setServiceUrl(req.getParameter("loadAliasPath")); //this is significant, needs to be the subsite's site alias ("providers")
		ai.setActionId(req.getParameter("loadSiteId")); //this is significant, needs to be the SUBSITE'S siteId
		log.debug("parPath=" + site.getAliasPathName());
		log.debug("siteId=" + site.getSiteId());
		ModuleVO menuMod = new ModuleVO(null, role.getCachePmid(site.getSiteId()), true, "MENU");
		attributes.put(Constants.MODULE_DATA, menuMod);
		ActionInterface ac = null;
		try {
			ac = new MenuBuilder(ai);
			ac.setAttributes(attributes);
			ac.setDBConnection(dbConn);
			ac.retrieve(req);
			menuMod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
			
		} catch (ActionException ae) {
			log.error("could not load menus", ae);
			
		} finally {
			mod.addCacheGroup(site.getSiteId());
			//this line of code does not work, because AJAX menus are only loaded on the parent site; there will never be a parentAlias here -JM 03-8-14
			//if (site.getAliasPathParentId() != null) mod.addCacheGroup(site.getAliasPathParentId());
			mod.setActionData(menuMod.getActionData());
			attributes.put(Constants.MODULE_DATA, mod);
			ac = null;
		}
	}
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
}