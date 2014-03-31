package com.depuysynthes.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.AbstractBaseAction;
import com.smt.sitebuilder.action.commerce.product.ProductCatalogAction;
import com.smt.sitebuilder.action.tools.StatVO;

/****************************************************************************
 * <b>Title</b>: ProductCatalogLoader.java<p/>
 * <b>Description: This load facades all calls to load Product Catalogs out of WC
 * for the DS website.  Typically this involves 'pruning' the catalog down to a branch
 * level, and returning the branch as it's own Tree (a "full" catalog).</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since May 16, 2013
 * @updates
 * 	-JM 03.30.14 - rebuilt PageViewComparator because it was not transitive.
 ****************************************************************************/
public class ProductCatalogUtil extends AbstractBaseAction {

	public ProductCatalogUtil() {
		super();
	}
	
	public ProductCatalogUtil(ActionInitVO ai) {
		super(ai);
	}
	
	
	/**
	 * Loads a product catalog for the calling class; and optionally prunes it down to a branch
	 * If pruning occurs, the Tree returned will be rooted at the branch level (the 
	 * branch will be turned into its own Tree).
	 * @param catalogId
	 * @param rootNodeId
	 * @param loadAttributes
	 * @param req
	 * @return
	 */
	public Tree loadCatalog(String catalogId, String rootNodeId,  boolean loadAttributes, SMTServletRequest req) {
		ProductCatalogAction pca = new ProductCatalogAction(this.actionInit);
		pca.setDBConnection(dbConn);
		pca.setAttributes(attributes);
		Tree t = pca.loadEntireCatalog(catalogId, loadAttributes, req);
		
		//find the country (node) within the catalog that we're suppose to display
		if (rootNodeId != null) {
			log.debug("pruning catalog to " + rootNodeId);
			Node branch = t.findNode(rootNodeId);
			
			if (branch != null) {
				//take all the children of our branch node and flatten them to a List
				List<Node> baseList = branch.getChildren();
				branch.setChildren(new ArrayList<Node>());
				
				List<Node> children = new ArrayList<Node>();
				Tree.createPreorder(baseList, children);
				
				//purge any references to other Nodes since the list has been flattened.
				//This will get rebuild below in "new Tree()"
				for (Node n : children) n.setChildren(new ArrayList<Node>());
				
				//build a new Tree that reflects our newNode as the root, and all it's children
				t = new Tree(children, branch);
				log.debug("new tree size=" + t.preorderList().size());
			}
		}
		
		return t;
	}
	
	/**
	 * prunes an already-loaded catalog down to a specific child level.
	 * used by AjaxMenuLoader, which needs to entire product tree to affiliate Procedures, but not for display...
	 * use the full tree, pass it here to be pruned, then forward the pruned tree onward to display/View.
	 * @param t
	 * @param rootNodeId
	 * @return
	 */
	public Tree pruneCatalog(Tree t, String rootNodeId) {
		if (rootNodeId == null) return t;
		
		log.debug("pruning catalog to " + rootNodeId);
		Node branch = t.findNode(rootNodeId);
		
		if (branch != null) {
			//take all the children of our branch node and flatten them to a List
			List<Node> baseList = branch.getChildren();
			branch.setChildren(new ArrayList<Node>());
			
			List<Node> children = new ArrayList<Node>();
			Tree.createPreorder(baseList, children);
			
			//purge any references to other Nodes since the list has been flattened.
			//This will get rebuild below in "new Tree()"
			for (Node n : children) n.setChildren(new ArrayList<Node>());
			
			//build a new Tree that reflects our newNode as the root, and all it's children
			t = new Tree(children, branch);
			log.debug("new tree size=" + t.preorderList().size());
		}
		
		return t;
	}
	
	
	/**
	 * assign pageView counts to each product (or procedure) in this catalog
	 * @param prods
	 * @param pageViews
	 * @param pageAlias
	 * @return 
	 */
	
	public List<Node> assignPageviewsToCatalog(List<Node> prods, Map<String, StatVO> pageViews, String pageAlias) {
		log.debug("assigning pageViews using baseUrl: " + pageAlias);
		if (pageViews == null) pageViews = Collections.emptyMap();
		
		for (Node n : prods) {
			ProductCategoryVO cat = (ProductCategoryVO) n.getUserObject();
			String prodAlias = cat.getUrlAlias();
			if (prodAlias == null || prodAlias.length() == 0) continue; //skip anything that can't be seen on the website
			cat.setCategoryUrl(pageAlias + prodAlias);
			
			//log.debug("getting stats for " + pageAlias + prodAlias);
			StatVO stats = pageViews.get(pageAlias + prodAlias);
			if (stats != null) {
				log.debug(pageAlias + prodAlias + "=" + stats.getHitCnt());
				cat.setNumProdAssoc(stats.getHitCnt());
			}
		}
		return prods;
	}
	
	
	public List<Node> assignPageviewsToCatalogUsingDivision(List<Node> prods, Map<String, StatVO> pageViews, String siteAlias, boolean isProducts, boolean isFullCatalog) {
		log.debug("assigning pageViews using baseUrl: " + siteAlias);
		if (pageViews == null) pageViews = Collections.emptyMap();
		String suffixUrl = (isProducts) ? "/products/qs/" : "/procedures/qs/";
		String divisionAlias = "";
		String prodAlias = null;
		StringBuilder url = null;
		int rootDepthLvl = (isFullCatalog) ? 2 : 1;
		
		for (Node n : prods) {
			ProductCategoryVO cat = (ProductCategoryVO) n.getUserObject();
			//log.debug("cat " + n.getNodeName() + " dp=" + n.getDepthLevel());
			if (n.getDepthLevel() == rootDepthLvl) {
				//this is a division, update the url prefix
				divisionAlias = cat.getUrlAlias();
				continue;
			}
			
			prodAlias = cat.getUrlAlias();
			if (prodAlias == null || prodAlias.length() == 0) continue; //skip anything that can't be seen on the website
			
			url = new StringBuilder(siteAlias).append(divisionAlias).append(suffixUrl).append(prodAlias);
			cat.setCategoryUrl(url.toString());
			
			//log.debug("getting stats for " + url);
			StatVO stats = pageViews.get(url.toString());
			if (stats != null) {
				log.debug(url + "=" + stats.getHitCnt());
				cat.setNumProdAssoc(stats.getHitCnt());
			}
		}
		return prods;
	}
	
	public String[] separateIds(String combinedKey) {
		String[] ids = new String[2];
		String[] tokens = combinedKey.split("~");
		if (tokens == null) return ids;
		if (tokens.length == 2) return tokens;
		
		//else return an array that is always lenth=2 and has an empty 2nd value.
		ids[0] = tokens[0];
		return ids;
	}
	
	
	/**
	 * **************************************************************************
	 * <b>Title</b>: HCPLandingPageAction.PageViewComparator.java<p/>
	 * <b>Description: Compares two Node objects for equality using getDepthLevel, 
	 * which has been 'stuffed' with pageView stats (# pageViews).
	 * Note: this is an INVERSE comparator -- highest values first!</b> 
	 * <p/>
	 * <b>Copyright:</b> Copyright (c) 2013<p/>
	 * <b>Company:</b> Silicon Mountain Technologies<p/>
	 * @author James McKain
	 * @version 1.0
	 * @since May 14, 2013
	 ***************************************************************************
	 */
	public class PageViewComparator implements Comparator<Node> {
		public int compare(Node n1, Node n2) {
			
			Integer cat1Cnt = Integer.valueOf(0);
			Integer cat2Cnt = Integer.valueOf(0);
			ProductCategoryVO cat;
			
			if (n1 != null && n1.getUserObject() != null) {
				cat = (ProductCategoryVO) n1.getUserObject();
				cat1Cnt = cat.getNumProdAssoc();
			}
			
			if (n2 != null && n2.getUserObject() != null) {
				cat = (ProductCategoryVO) n2.getUserObject();
				cat2Cnt = cat.getNumProdAssoc();
			}
			
			//if we get this far, compare them fairly.
			return cat1Cnt.compareTo(cat2Cnt);
		}
	}
	
	
	
	/**
	 * This is a utility class, not a WC action.  It's using the Action
	 * framework to have access to Attributes and dbConn in a consistent way.
	 * The methods below are only implemented to apease the Interface. 
	 */
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void delete(SMTServletRequest req) throws ActionException {
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#copy(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void copy(SMTServletRequest req) throws ActionException {
	}

}
