package com.depuy.http;

import java.util.ArrayList;
import java.util.List;

import com.depuysynthes.action.ProductCatalogUtil;
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserRoleVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.menu.MenuObj;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.http.SiteMapServlet;

/****************************************************************************
 * <b>Title</b>: DePuySiteMapServlet.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 16, 2013
 ****************************************************************************/
public class DePuySiteMapServlet extends SiteMapServlet {
	private static final long serialVersionUID = 44784795996815006L;
	
	private static final String HCP_PREFIX = "/hcp/";
	
	
	/**
	 * overload the Template Method Pattern of the superclass to add some 
	 * custom DePuy pages to the sitemap.
	 */
	@Override
	protected List<Node> loadCustomLowPriPages(SMTServletRequest req, SiteVO site, UserRoleVO role) {
		List<Node> pages = null;
		
		// Retrieve the custom Products & Procedures for DS & DS-EMEA (only)
		if ((site.getOrganizationId()).equals("DPY_SYN")) {
			pages = this.loadDSProducts(site, req, "DS_PRODUCTS", "/products/qs/");
			pages.addAll(this.loadDSProducts(site, req, "DS_PROCEDURES", "/procedures/qs/"));
		} else if ((site.getOrganizationId()).equals("DPY_SYN_EMEA")) {
			pages = this.loadDSProducts(site, req, "DS_PRODUCTS_EMEA", "/products/qs/");
			pages.addAll(this.loadDSProducts(site, req, "DS_PROCEDURES_EMEA", "/procedures/qs/"));
		} else {
			pages = super.loadCustomLowPriPages(req, site, role);
		}

		return pages;
	}

	
	/**
	 * loads all the Product & Procedure URLs from the DS product catalogs.
	 * @param site
	 * @param req
	 * @return
	 */
    protected List<Node> loadDSProducts(SiteVO site, SMTServletRequest req, String catalogId, String sitePg) {
    	List<Node> data = new ArrayList<Node>();
    	SMTDBConnection dbConn = new SMTDBConnection(this.getDBConnection());
    	ProductCatalogUtil pc = new ProductCatalogUtil();
		pc.setDBConnection(dbConn);
    	
    	try {
    		Tree prodTree = pc.loadCatalog(catalogId, null, false, req);
    		List<String> completed = new ArrayList<String>();
    		String divisionUrl = null;
            String countryNodeId = null;
        		
    		for (Node n : prodTree.preorderList()) {
    			ProductCategoryVO vo = (ProductCategoryVO) n.getUserObject();
    			
        		//top level categories define our countries
        		if (n.getParentId() == null) {
        			countryNodeId = n.getNodeId();
//        			log.debug("changed country to " + vo.getUrlAlias());
        			continue; //these are not products (to index)
        		} else if (n.getParentId().equals(countryNodeId)) {
        			divisionUrl = vo.getUrlAlias();
//        			log.debug("set division to: " + n.getNodeName());
        			continue; //these are child categories, not products (to index)
        		} else if (StringUtil.checkVal(vo.getUrlAlias()).length() == 0) {
//        			log.debug("not a product: " + vo.getCategoryName());
        			continue;
        		} else if (completed.contains(divisionUrl + vo.getUrlAlias())) {
//        			log.debug("already indexed " + divisionUrl + vo.getUrlAlias());
        			continue;
        		}
        		
    			MenuObj mo = new MenuObj();
	            mo.setFullPath(HCP_PREFIX + divisionUrl + sitePg + vo.getUrlAlias());
	            mo.setLastModified(vo.getLastUpdate());
	            mo.setFileExtension("");
	            mo.setContextName(contextPath);
	            
	            n.setUserObject(mo);
	            data.add(n);
	            completed.add(divisionUrl + vo.getUrlAlias());
    		}
    	} catch(Exception e) {
    		log.error("Unable to get Products", e);
    	} finally {
    		try {
    			dbConn.close();
    		} catch(Exception e) {}
    	}
    	return data;
    }
}
