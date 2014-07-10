package com.fastsigns.product;

import java.sql.Connection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.menu.MenuObj;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ProductSitemapCreator.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jan 12, 2011<p/>
 * <b>Changes: JM, 4/17/12 - revised to consider Apache rewrite rules and "product catalog" wrapper.</b>
 ****************************************************************************/
public class ProductSitemapCreator {
	protected static Logger log = Logger.getLogger(ProductSitemapCreator.class);
	

	public ProductSitemapCreator() {
	}
	
	/**
	 * Loads a list of Pages for this website that have Product Catalogs, then forwards that information
	 * to the method that loads the catalog(s)
	 * @param conn
	 * @param siteId
	 * @return
	 */
	public List<Node> getPages(Connection conn, SiteVO site, String ctx) {
		List<Node> data = new ArrayList<Node>();
		Map<String,String> config = this.getProductImplData(conn, site.getSiteId());
		if (config == null || config.size() == 0) return data;
		
		config.put(Constants.CONTEXT_PATH, ctx);
		config.put("siteId", site.getSiteId());
		config.put("organizationId", site.getOrganizationId());
		
		log.debug(StringUtil.getToString(config));
		data = getProductData(conn, config);
		return data;
	}
	
	/**
	 * 
	 * @param conn
	 * @param siteId
	 * @return
	 */
	private Map<String, String> getProductImplData(Connection conn, String siteId) {
    	StringBuilder sb = new StringBuilder();
    	sb.append("select full_path_txt, c.action_id, c.attrib1_txt, c.attrib2_txt from page a ");
    	sb.append("inner join page_module b on a.PAGE_ID = b.PAGE_ID ");
    	sb.append("inner join SB_ACTION c on b.ACTION_ID = c.action_id  ");
    	sb.append("where MODULE_TYPE_ID = 'FTS_PROD_CAT' and SITE_ID = ? ");
    	sb.append("and action_nm='Products by Sign Type'");
    	
    	Map<String,String> data = new HashMap<String, String>();
    	PreparedStatement ps = null;
    	try {
    		ps = conn.prepareStatement(sb.toString());
    		ps.setString(1, siteId);
    		ResultSet rs = ps.executeQuery();
    		if (rs.next()) {
    			data.put("pageUrl", rs.getString(1));
    			data.put("actionId", rs.getString(2));
    			data.put(SBModuleVO.ATTRIBUTE_1, rs.getString(3));
    			data.put(SBModuleVO.ATTRIBUTE_2, rs.getString(4));
    		}
    	} catch(Exception e) {
    		log.error("Unable to get product pages", e);
    	}
    	
    	return data;
	}
	
    /**
     * 
     * @param conn
     * @param orgId
     */
    public List<Node> getProductData(Connection conn, Map<String, String> config) {
    	//load the action
    	String filterCategory = "";
    	Tree tree = null;
    	ActionInitVO ai = new ActionInitVO(null, config.get("actionId"), null);
		try {
			String[] vals = config.get(SBModuleVO.ATTRIBUTE_1).split("~");
			FSProductAction fpi = new FSProductAction(ai);
			fpi.setDBConnection(new SMTDBConnection(conn));
			tree = fpi.loadEntireCatalog(vals[0], false);
			filterCategory = vals[1];

		} catch (Exception e) {
			log.error("could not load FS products", e);
			return null;
		}
		
    	return this.assignFullPath(tree.preorderList(), config.get("pageUrl"), filterCategory);
    }
    
    /**
     * Assigns the full path based upon the pre-order list
     * @param data
     * @return
     */
    public List<Node> assignFullPath(List<Node> categories, String pageAlias, String filterCategory) {
		String fullPath = null;
    	Map<String, String> fp = new LinkedHashMap<String, String>();
    	List<Node> pages = new ArrayList<Node>();
    	boolean proceed = false;
    	for (Node n : categories) {
    		ProductCategoryVO cat = (ProductCategoryVO) n.getUserObject();
    		
    		//omit all top-level PRODUCTS. they're not used on the Page's listing directly like this
    		if (n.getDepthLevel() == 2 && cat.getProductId() != null) 
    			continue;
    		
    		//determine which top-level category we're in.  We only want to iterate "this" category (By Sign Type)
    		if (n.getDepthLevel() == 1 && n.getNodeId().equals(filterCategory)) {
    			//log.debug("turned on at " + n.getNodeId());
    			proceed = true;
    		} else if (n.getDepthLevel() == 1) {
    			proceed = false;
    		}
    		
    		if (!proceed || n.getNodeId().equals(filterCategory)) continue;
    		
    		
    		if (fp.containsKey(n.getParentId())) {
    			String prefix = fp.get(n.getParentId());
    			if (prefix.length() > 5) prefix += "/"; 
    			fullPath = prefix + cat.getUrlAlias();
        		fp.put(n.getNodeId(), fullPath);
        		
    		} else {
    			String preservedPath = null;
    			if (cat.getCustCategoryId() != null) {
    				//these are our abbreviated category URLs rewritten by Apache
    				fullPath = "/" + cat.getCustCategoryId() + cat.getUrlAlias();
    				preservedPath = "/" + cat.getCustCategoryId();
    			} else {
    				fullPath = pageAlias + "/qs/" + cat.getUrlAlias();
    				preservedPath = fullPath;
    			}
    			fp.put(n.getNodeId(), preservedPath);
    		}
			
    		MenuObj m = new MenuObj();
    		m.setFileExtension("");
    		m.setFullPath(fullPath);
    		m.setLastModified(cat.getLastUpdate());
    		Node newNode = new Node(n.getNodeId(), n.getParentId());
    		newNode.setFullPath(fullPath);
    		newNode.setUserObject(m);
    		
    		pages.add(newNode);
    	}
    	
    	return pages;
    }

}
