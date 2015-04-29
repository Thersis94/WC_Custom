package com.depuy.http;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;

import com.depuysynthes.action.ProductCatalogUtil;
import com.depuysynthes.lucene.MediaBinSolrIndex;
import com.depuysynthesinst.SolrSearchWrapper;
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserRoleVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.menu.MenuObj;
import com.smt.sitebuilder.action.search.SolrActionIndexVO;
import com.smt.sitebuilder.action.search.SolrActionVO;
import com.smt.sitebuilder.action.search.SolrQueryProcessor;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.http.SiteMapServlet;
import com.smt.sitebuilder.search.SearchDocumentHandler;

/****************************************************************************
 * <b>Title</b>: DePuySiteMapServlet.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 16, 2013
 * @updates
 * 		JM 10.15.14 - Added DSI/Solr support
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
			pages = this.loadDSProducts(site, req, "DS_PRODUCTS", "/products/" + sc.getAttribute(Constants.QS_PATH));
			pages.addAll(this.loadDSProducts(site, req, "DS_PROCEDURES", "/procedures/" + sc.getAttribute(Constants.QS_PATH)));
		} else if ((site.getOrganizationId()).equals("DPY_SYN_EMEA")) {
			pages = this.loadDSProducts(site, req, "DS_PRODUCTS_EMEA", "/products/" + sc.getAttribute(Constants.QS_PATH));
			pages.addAll(this.loadDSProducts(site, req, "DS_PROCEDURES_EMEA", "/procedures/" + sc.getAttribute(Constants.QS_PATH)));
		} else if ((site.getOrganizationId()).equals("DPY_SYN_INST")) {
			pages = this.loadDSISolrAssets(site, req);
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
    
    /**
     * loads a list of assets for DSI, out of Solr.
     * Then applies business logic to determine the pageURLs for each asset, based on hierarchy.
     * @param site
     * @param req
     * @return
     */
    protected List<Node> loadDSISolrAssets(SiteVO site, SMTServletRequest req) {
	    List<Node> data = new ArrayList<Node>();
	    Map<String, Object> attributes = new HashMap<String, Object>();
	    SolrSearchWrapper solrWrapper = new SolrSearchWrapper();
	    
	    attributes.put(Constants.SOLR_BASE_URL, sc.getAttribute(Constants.SOLR_BASE_URL));
	    attributes.put(Constants.SOLR_COLLECTION_NAME, sc.getAttribute(Constants.SOLR_COLLECTION_NAME));

	    SolrActionVO qData = new SolrActionVO();
	    qData.setNumberResponses(20000);
		qData.setOrganizationId(site.getOrganizationId()); //DPY_SYN_INST only
		qData.setRoleLevel(0); //public assets only
		qData.addIndexType(new SolrActionIndexVO(SearchDocumentHandler.INDEX_TYPE, MediaBinSolrIndex.INDEX_TYPE));
		SolrQueryProcessor sqp = new SolrQueryProcessor(attributes);
		SolrResponseVO resp = sqp.processQuery(qData);
		
		if (resp == null || resp.getTotalResponses() == 0) return data;
		
		for (SolrDocument sd : resp.getResultDocuments()) {
		    try {
			    MenuObj mo = new MenuObj();
			    mo.setFullPath(solrWrapper.buildDSIUrl(sd));
			    if (mo.getFullPath() == null) continue; //asset does not have a valid DSI url and should not be promoted
			    
			    mo.setLastModified((Date)sd.getFieldValue(SearchDocumentHandler.UPDATE_DATE));
			    mo.setFileExtension("");
			    mo.setContextName(contextPath);
	
			    Node n = new Node();
			    n.setUserObject(mo);
			    data.add(n);

		    } catch(Exception e) {
			    log.error("Unable to make URL for Solr asset", e);
		    }
		}
		
		log.debug("size=" + data.size());
	    return data;
    }
}