package com.depuysynthes.huddle;

import java.util.Properties;

import com.depuysynthes.huddle.solr.HuddleProductCatalogSolrIndex;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.data.Tree;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.commerce.product.ProductCatalogAction;
import com.smt.sitebuilder.action.commerce.product.ProductDataTool;
import com.smt.sitebuilder.util.solr.SolrActionUtil;

/****************************************************************************
 * <b>Title</b>: HuddleProductAdminAction.java<p/>
 * <b>Description: Non-Approval version of the product admin that automatically
 * indexes all products in solr.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Feb 1, 2016
 ****************************************************************************/
public class HuddleProductAdminAction extends SimpleActionAdapter {

	public HuddleProductAdminAction() {
		super();
		
	}

	public HuddleProductAdminAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	public void list(SMTServletRequest req) throws ActionException {
		ProductDataTool bfa = new ProductDataTool(actionInit);
		bfa.setAttributes(getAttributes());
		bfa.setDBConnection(dbConn);
		bfa.list(req);
	}
	
	public void update(SMTServletRequest req) throws ActionException {
		ProductDataTool bfa = new ProductDataTool(actionInit);
		bfa.setAttributes(getAttributes());
		bfa.setDBConnection(dbConn);
		bfa.update(req);
		if (req.hasParameter("productId")) {
			// Since this is a product update only that needs to be updated.
			indexProduct(req.getParameter("productId"), req.getParameter("catalogId"), req);
		} else if (req.hasParameter("productCategoryCode") || req.hasParameter("attributeId")) {
			// Attribute and category updates can affect many products,
			// therefore the entire index needs to be remade.
			indexProduct(null, req.getParameter("catalogId"), req);
		}
	}
	
	private void indexProduct(String productId, String catalogId, SMTServletRequest req) {
		
		ProductCatalogAction pc = new ProductCatalogAction(actionInit);
		pc.setAttributes(attributes);
		pc.setDBConnection(dbConn);
		
		Tree t = pc.loadEntireCatalog(catalogId, true, req, productId);
		
		//fire the VO to Solr, leverage the same lookup the "full rebuild" indexer uses, which joins to Site Pages
		Properties props = new Properties();
		props.putAll(getAttributes());
		HuddleProductCatalogSolrIndex indexer = new HuddleProductCatalogSolrIndex(props);
		indexer.setDBConnection(getDBConnection());
		indexer.pushSingleProduct(t);
	}
	
	public void delete(SMTServletRequest req) throws ActionException {
		ProductDataTool bfa = new ProductDataTool(actionInit);
		bfa.setAttributes(getAttributes());
		bfa.setDBConnection(dbConn);
		bfa.delete(req);
		
		if (!req.hasParameter("productId")) return;
		
		String bType = StringUtil.checkVal(req.getParameter("bType"));
		if ("Product".equals(bType)) {
			//fire the delete to Solr
			SolrActionUtil util = new SolrActionUtil(getAttributes());
			util.removeDocument(req.getParameter("productId"));
		} else if (bType.contains("Product")) {
			// We are deleting an attribute or a category on a product
			// and need to update the related solr document.
			indexProduct(req.getParameter("productId"), req.getParameter("catalogId"), req);
		}
		
	}
	
}
