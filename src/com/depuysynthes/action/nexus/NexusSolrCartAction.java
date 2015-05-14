package com.depuysynthes.action.nexus;

import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.commerce.cart.storage.Storage;
import com.siliconmtn.commerce.cart.storage.StorageFactory;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

public class NexusSolrCartAction extends SBActionAdapter {
	
	public void build(SMTServletRequest req) throws ActionException {
		Storage store = retrieveContainer(req);
		
		ShoppingCartVO cart = store.load();
		if (Convert.formatBoolean(req.getParameter("clearCart"))) {
			if (req.hasParameter("removeItem")){
				cart.remove(req.getParameter("removeItem"));
			} else {
				cart.flush();
			}
		} else {
			ProductVO product = new ProductVO();
			product.setProductId(req.getParameter("productId"));
			product.setShortDesc(req.getParameter("desc"));
			product.addProdAttribute("orgName", req.getParameter("orgName"));
			product.addProdAttribute("gtin", req.getParameter("gtin"));
			product.addProdAttribute("lotNo", req.getParameter("lotNo"));
			product.addProdAttribute("dateLot", req.getParameter("dateLot"));
			product.addProdAttribute("uom", req.getParameter("uom"));
			product.addProdAttribute("qty", req.getParameter("qty"));
			ShoppingCartItemVO item = new ShoppingCartItemVO(product);
			item.setProductId(product.getProductId());
			cart.add(item);
		}
		store.save(cart);
		req.setAttribute("cart", cart.getItems());
	}
	

	
	/**
	 * Retrieves the Storage container
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private Storage retrieveContainer(SMTServletRequest req) 
			throws ActionException {
		Map<String, Object> attrs = new HashMap<>();
		attrs.put(GlobalConfig.HTTP_REQUEST, req);
		attrs.put(GlobalConfig.HTTP_RESPONSE, attributes.get(GlobalConfig.HTTP_RESPONSE));
		
		Storage container = null;
		
		try {
			container = StorageFactory.getInstance(StorageFactory.SESSION_STORAGE, attrs);
		} catch (Exception ex) {
			throw new ActionException(ex);
		}
		return container;
	}



	public void retrieve(SMTServletRequest req) throws ActionException {
		ShoppingCartVO cart = retrieveContainer(req).load();
		if (req.hasParameter("buildFile")) {
			NexusCartReport report = new NexusCartReport();
			
			report.setData(cart.getItems());
			report.setFileName("cart.xls");
			req.setAttribute(Constants.BINARY_DOCUMENT, report);
			req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
			
		} 
		req.setAttribute("cart", cart.getItems());
		
		if (req.hasParameter("fq")) {
			String[] fq = req.getParameterValues("fq");
			String[] newFq = new String[fq.length+cart.getItems().size()];
			int i=0;
			for (; i<fq.length; i++){
				newFq[i] = fq[i];
			}
			for (String id : cart.getItems().keySet())
				newFq[i++] = "-documentId:"+id;
			req.setParameter("fq", newFq, true);
		} else {
			String[] newFq = new String[cart.getItems().size()];
			int i=0;
			for (String id : cart.getItems().keySet())
				newFq[i++] = "-documentId:"+id;
			req.setParameter("fq", newFq, true);
		}
		
	    	ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
	    	actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
	    	SMTActionInterface sai = new SolrAction(actionInit);
	    	sai.setDBConnection(dbConn);
	    	sai.setAttributes(attributes);
		sai.retrieve(req);
		
		
	}
	
	public void update(SMTServletRequest req) throws ActionException {
		super.update(req);
	}
}
