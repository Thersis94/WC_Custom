package com.depuysynthes.action;

import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.tools.PageViewReportingAction;
import com.smt.sitebuilder.action.tools.StatVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ProductCatalogAction.java<p/>
 * <b>Description: cacheable action that loads an entire product catalog.
 * The Views for this action are basically search/results modules, so they need the entire catalog.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since May 14, 2013
 ****************************************************************************/
public class DSProductCatalogAction extends SimpleActionAdapter {

	
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
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
				//log.debug("writing to cache, groups=" + StringUtil.getToString(mod.getCacheGroups(), false, false,",") + " id=" + mod.getPageModuleId());
				
				//If we are in preview mode we do not cache this 
				if (!isPreview)
					super.writeToCache(cachedMod);
			 }
		}

		mod.setActionData(cachedMod.getActionData());
		updatePageData(req, mod, page);
		setAttribute(Constants.MODULE_DATA, mod);
	}
	
	
	@SuppressWarnings("unchecked")
	private ModuleVO loadProductData(SMTServletRequest req) {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		String catalogId = (String) mod.getAttribute(ModuleVO.ATTRIBUTE_1);
		String rootNodeId = (String) mod.getAttribute(ModuleVO.ATTRIBUTE_2);
		
		ProductCatalogUtil pc = new ProductCatalogUtil(actionInit);
		pc.setAttributes(getAttributes());
		pc.setDBConnection(dbConn);
		Tree t = pc.loadCatalog(catalogId, rootNodeId, true, req);
		
		//try to assign pageView stats
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
			if (page.getDepthLevel() > 2 || page.getFullPath().equals("/select/products")) {
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
		
		mod.setActionData(t);
		mod.setCacheTimeout(86400*2); //refresh every 48hrs
		mod.addCacheGroup(catalogId);
		return mod;
	}
	
	private void updatePageData(SMTServletRequest req, ModuleVO mod, PageVO page) {
		String reqParam1 = req.getParameter(SMTServletRequest.PARAMETER_KEY + "1");

		//If we don't have a request Parameter to parse, return.
		if(reqParam1 == null || reqParam1.length() == 0) return;
		log.debug(reqParam1);
		
		/*
		 * If we do, loop the tree and place the product on the request and update
		 * the Page.
		 */
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
			if (p.getProducts().size() > 0) {
				ProductVO pr = p.getProducts().get(0);
				//Set Page Data and request object
				if (reqParam1.equalsIgnoreCase(pr.getUrlAlias())) {
					log.debug("Found a Product Match");
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
		String val = StringUtil.checkVal(pr.getTitle(), StringUtil.checkVal(pr.getProductName(), 
				StringUtil.checkVal(p.getCategoryName(),  pr.getFullProductName())));	
		if (StringUtil.checkVal(val).length() > 0) {
			page.setTitleName(val);
		} 
	
		//meta keywords
		val = pr.getMetaKywds() != null ? pr.getMetaKywds(): p.getMetaKeyword();
		if (StringUtil.checkVal(val).length() > 0)
			page.setMetaKeyword(val);
		
		//meta desc
		val = pr.getMetaDesc() != null ? pr.getMetaDesc(): p.getMetaDesc();
		if (StringUtil.checkVal(val).length() > 0)
			page.setMetaDesc(val);
		
		//TODO set canonical URL of the page to the case-proper UrlAlias of this product
		//page.setCanonicalUrl(page.getRequestURI() + "/" + attributes.get(Constants.QS_PATH) + pr.getUrlAlias());
	}
	
}
