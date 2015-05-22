package com.depuysynthes.action.nexus;

import java.text.SimpleDateFormat;
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
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>NexusSolrCartAction.java<p/>
 * <b>Description: Handles cart manpulation and solr searches for the
 * DePuy NeXus site</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 2.0
 * @since May 20, 2015
 * <b>Changes: </b>
 ****************************************************************************/

public class NexusSolrCartAction extends SBActionAdapter {
	
	public void build(SMTServletRequest req) throws ActionException {
		Storage store = retrieveContainer(req);
		ShoppingCartVO cart = store.load();
		
		if (Convert.formatBoolean(req.getParameter("clearCart"))) {
			// Determine whether we are deleting a single item or the entire cart
			if (req.hasParameter("removeItem")){
				cart.remove(req.getParameter("removeItem"));
			} else {
				cart.flush();
			}
		} else {
			// Build a product vo that can be placed in the cart vo
			String dateLot = new SimpleDateFormat("ddMMMMyyyy").format(Convert.getCurrentTimestamp());
			cart.getItems().get(req.getParameter("productId"));
			ProductVO product = new ProductVO();
			product.setProductId(req.getParameter("productId"));
			product.setShortDesc(req.getParameter("desc"));
			product.addProdAttribute("orgName", req.getParameter("orgName"));
			product.addProdAttribute("gtin", req.getParameter("gtin"));
			product.addProdAttribute("lotNo", StringUtil.checkVal(req.getParameter("lotNo"), dateLot));
			if (dateLot.equals(product.getProdAttributes().get("lotNo")))
				product.addProdAttribute("dateLot", true);
			product.addProdAttribute("uom", req.getParameter("uom"));
			product.addProdAttribute("qty", StringUtil.checkVal(req.getParameter("qty"),"1"));
			ShoppingCartItemVO item = new ShoppingCartItemVO(product);
			item.setProductId(product.getProductId());
			cart.add(item);
		}
		store.save(cart);
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
		// Load the cart first since it is always needed
		ShoppingCartVO cart = retrieveContainer(req).load();
		req.setAttribute("cart", cart.getItems());
		
		// Check if we are building a file, create the report generator and set the pertinent information
		if (req.hasParameter("buildFile")) {
			AbstractSBReportVO report = new NexusCartExcelReport();
			Map<String, Object> data = new HashMap<>();
			data.put("cart", cart.getItems());
			data.put("hospital", StringEncoder.urlDecode(req.getCookie("hospital").getValue()));
			data.put("room", StringEncoder.urlDecode(req.getCookie("room").getValue()));
			data.put("surgeon", StringEncoder.urlDecode(req.getCookie("surgeon").getValue()));
			data.put("time", StringEncoder.urlDecode(req.getCookie("time").getValue()));
			data.put("caseId", StringEncoder.urlDecode(req.getCookie("caseId").getValue()));
			data.put("baseDomain", req.getHostName());
			
			report.setData(data);
			report.setFileName("cart.xls");
			req.setAttribute(Constants.BINARY_DOCUMENT, report);
			req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
			return;
		}
		
		// Build the filter queries so that we don't get any items
		// that are already in the cart or from a different organization
		buildFilterQueries(req, cart);
		
		// Do the solr search
	    	ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
	    	log.debug((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
	    	actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
	    	SMTActionInterface sai = new SolrAction(actionInit);
	    	sai.setDBConnection(dbConn);
	    	sai.setAttributes(attributes);
		sai.retrieve(req);
	}

	
	/**
	 * Build the filter queries from the request object and the user's cart
	 * @param req
	 * @param cart
	 */
	private void buildFilterQueries(SMTServletRequest req, ShoppingCartVO cart) {
		String[] newFq;
		int i=0;
		if (req.hasParameter("fq")) {
			String[] fq = req.getParameterValues("fq");
			newFq = new String[fq.length+cart.getItems().size() + 1];
			for (; i<fq.length; i++){
				newFq[i] = fq[i];
			}
		} else {
			newFq = new String[cart.getItems().size() + 1];
		}
		
		// Remove all items in the cart from the search
		for (String id : cart.getItems().keySet())
			newFq[i++] = "-documentId:"+id;
		
		newFq[i] = "organizationName:" + req.getParameter("orgName");
		
		req.setParameter("fq", newFq, true);
	}
}
