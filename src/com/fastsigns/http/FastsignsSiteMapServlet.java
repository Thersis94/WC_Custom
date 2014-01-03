package com.fastsigns.http;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import com.fastsigns.action.franchise.MetroAction;
import com.fastsigns.action.franchise.vo.MetroContainerVO;
import com.fastsigns.action.franchise.vo.MetroProductVO;
import com.fastsigns.product.ProductSitemapCreator;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserRoleVO;
import com.smt.sitebuilder.action.menu.MenuObj;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.http.SiteMapServlet;

/****************************************************************************
 * <b>Title</b>: FastsignsSiteMapServlet.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Sep 28, 2011
 * @update June 19, 2012 - Streamlined process Request
 * @update 10/14/13 - refactored to use Template Method Pattern of the superclass. -JM
 ****************************************************************************/
public class FastsignsSiteMapServlet extends SiteMapServlet {
	private static final long serialVersionUID = 4478479593983348006L;
	
	
	/**
	 * overload the Template Method Pattern of the superclass to add some 
	 * custom Fastsigns pages to the sitemap.
	 */
	@Override
	protected List<Node> loadCustomPages(SMTServletRequest req, SiteVO site, UserRoleVO role) {
		// Retrieve the custom FTS Product Pages
		List<Node> pages = this.getProducts(site);
		
		//load FTS metro pages, but only on the main website!
		if ((site.getOrganizationId() + "_7").equals(site.getSiteId()))
			pages.addAll(this.getFTSMetros(site, req));
		
		return pages;
	}

	

    /**
     * 
     * @param site
     * @return
     */
    protected List<Node> getProducts(SiteVO site) {
    	ProductSitemapCreator psc = new ProductSitemapCreator();
    	List<Node> data = new ArrayList<Node>();
    	Connection conn = this.getDBConnection();
    	try {
    		data = psc.getPages(conn, site, (String)sc.getAttribute(Constants.CONTEXT_PATH));
    	} catch(Exception e) {
    		log.error("Unable to get Products", e);
    	} finally {
    		try {
    			conn.close();
    		} catch(Exception e) {}
    	}
    	
    	return data;
    }
    
    
    /**
     * 
     * @return
     */
    protected List<Node> getFTSMetros(SiteVO site, SMTServletRequest req) {
    	SMTDBConnection conn = this.getDBConnection();
    	MetroAction ma = new MetroAction(new ActionInitVO());
    	ma.setAttribute(Constants.CUSTOM_DB_SCHEMA, (String)sc.getAttribute(Constants.CUSTOM_DB_SCHEMA));
    	ma.setDBConnection(conn);
    	String countryCd = ((SiteVO)req.getAttribute("siteData")).getCountryCode();
    	List<Node> data = new ArrayList<Node>();
    	try {
        	List<MetroContainerVO> metros = ma.getMetroSitemap(countryCd);
        	log.debug("found " + metros.size() + " metro areas");
        	
    		//iterate the metros.  Remove LSTs, remove hidden products
        	for (MetroContainerVO mc : metros) {
        		if (mc.getLstFlag().equals(1)) continue;
        		
	            MenuObj mo = new MenuObj();
	            mo.setFullPath("/metro-" + mc.getAreaAlias());
	            mo.setFileExtension("");
	            mo.setLastModified(mc.getLastUpdate());
	            mo.setLevel(2);


        		Node n = new Node(mc.getMetroAreaId(), null);
    			n.setNodeName(mc.getAreaName());
    			n.setRoot(false);
    			n.setUserObject(mo);
    			data.add(n);
    			
    			//loop and add all product pages for this metro area
    			for (MetroProductVO prod : mc.getProductPages().values()) {
    				mo = new MenuObj();
    	            mo.setFullPath("/metro-" + mc.getAreaAlias() + "/" + prod.getAliasNm());
    	            mo.setFileExtension("");
    	            mo.setLastModified(prod.getLastUpdate());
    	            mo.setLevel(3);


            		n = new Node(prod.getMetroProductId(), prod.getMetroAreaId());
        			n.setNodeName(prod.getProductNm());
        			n.setRoot(false);
        			n.setUserObject(mo);
        			data.add(n);
        			//log.debug("added metro " + n.getNodeName());
    			}
        	}
    		
    	} catch(Exception e) {
    		log.error("Unable to get FTS metro pages", e);
    	} finally {
    		try {
    			conn.close();
    		} catch(Exception e) {}
    	}
    	
    	log.debug("added " + data.size() + " metro pages");
    	return data;
    }
}
