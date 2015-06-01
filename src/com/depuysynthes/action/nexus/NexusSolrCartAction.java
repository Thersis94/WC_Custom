package com.depuysynthes.action.nexus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;

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
import com.smt.sitebuilder.action.search.SolrResponseVO;
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

	// Names for the cookies related to this action
	public static final String HOSPITAL = "hospital";
	public static final String ROOM = "room";
	public static final String SURGEON = "surgeon";
	public static final String TIME = "time";
	public static final String CASE_ID = "caseId";
	
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
			item.setProductId(product.getProductId()+product.getProdAttributes().get("lotNo"));
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
			AbstractSBReportVO report;
			String filename;
			String caseId = getCookie(req, CASE_ID);
			if (caseId.length() != 0) {
				filename = "case-" + caseId;
			} else {
				filename = "DePuyUDI-" + new SimpleDateFormat("YYYYMMdd").format(Convert.getCurrentTimestamp());;
			}
			
			if ("excel".equals(req.getParameter("buildFile"))) {
				report = new NexusCartExcelReport();
				report.setFileName(filename + ".xls");
			} else {
				report = new NexusCartPDFReport();
				report.setFileName(filename + ".pdf");
			}
			Map<String, Object> data = new HashMap<>();
			data.put("cart", cart.getItems());
			data.put("hospital", getCookie(req, HOSPITAL));
			data.put("room", getCookie(req, ROOM));
			data.put("surgeon", getCookie(req, SURGEON));
			data.put("time", getCookie(req, TIME));
			data.put("caseId", getCookie(req, CASE_ID));
			data.put("baseDomain", req.getHostName());
			data.put("format", req.getParameter("format"));
			
			report.setData(data);
			req.setAttribute(Constants.BINARY_DOCUMENT, report);
			req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
			return;
		}
		
		// Build a list of all the companies that are currently in the database
		// and add that to the request object
		if (req.getAttribute("orgs") == null)
			buildCompanyList(req);
		
		// Build the organization filter query
		req.setParameter("fq", "organizationName:" + req.getParameter("orgName"));
		
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
	 * Queries the solr server with a blank request and builds a list of 
	 * organizations in the solr server from that
	 * @param req
	 * @throws ActionException 
	 */
	private void buildCompanyList(SMTServletRequest req) throws ActionException {
		// Save the search data here so we can make the blank request
		String searchData = req.getParameter("searchData");
		req.setParameter("searchData", "", true);
		List<String> orgs = new ArrayList<>();
	    	ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
	    	log.debug((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
	    	actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
	    	SMTActionInterface sai = new SolrAction(actionInit);
	    	sai.setDBConnection(dbConn);
	    	sai.setAttributes(attributes);
		sai.retrieve(req);

	    	mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
	    	
	    	SolrResponseVO resp = (SolrResponseVO) mod.getActionData();
	    	for (FacetField facet : resp.getFacets()) {
	    		if("organizationName".equals(facet.getName())) {
	    			for (Count name : facet.getValues()) {
	    				orgs.add(name.getName());
	    			}
	    		}
	    			
	    	}
	    	req.setAttribute("orgs", orgs);
	    	req.setParameter("searchData", searchData, true);
		
	}



	/**
	 * Checks if a cookie exists and returns either the cookie's value or an
	 * empty string
	 */
	private String getCookie(SMTServletRequest req, String name) {
		Cookie c = req.getCookie(name);
		if (c == null) return "";
		return StringEncoder.urlDecode(c.getValue());
	}
}
