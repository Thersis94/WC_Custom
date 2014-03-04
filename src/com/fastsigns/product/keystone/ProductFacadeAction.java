package com.fastsigns.product.keystone;

import java.util.Map;

import javax.servlet.http.HttpSession;

import com.fastsigns.action.franchise.CenterPageAction;
import com.fastsigns.product.keystone.checkout.CheckoutUtil;
import com.fastsigns.security.FastsignsSessVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: MyAccountAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 3, 2012
 ****************************************************************************/
public class ProductFacadeAction extends SimpleActionAdapter {

	public ProductFacadeAction() {
	}

	/**
	 * @param actionInit
	 */
	public ProductFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * Constants that define the transactions this Action understands
	 */
	protected static enum ReqType {
		catalog, orders, assets, account, profile, invoices, history, dsol, product, search
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		attributes.put(Constants.SITE_DATA, site);
		ReqType type = null;
		HttpSession ses = req.getSession();
		
		//TODO - remove this block after testing passes. -JM 03-04-14
//		boolean first = req.hasParameter("firstEcommCall");
//		if (!first && !req.hasParameter("amid") && !page.getAliasName().equals("cart")) {
//			franId = site.getSiteId().replaceAll("^(.*)_([\\d]{1,5})_(.*)$", "$2");
//			ses.setAttribute(FastsignsSessVO.FRANCHISE_ID, franId);
//			req.setParameter("firstEcommCall", "true");
//			if (site.getAliasPathName() != null)
//				ses.setAttribute(FastsignsSessVO.ECOM_ALIAS_PATH, site.getAliasPathName());
//		}
		
		configureSession(ses, req, attributes);
		
		
		//since multiple actions can co-exist on a page, only honor request parameters 
		// for the action in the main column.
		if (mod.getDisplayColumn().equals(page.getDefaultColumn()) && req.hasParameter("display")) {
			type = getReqType(req);
		} else {
			type = ReqType.catalog;
		}
		log.debug("display=" + type);
		
		SMTActionInterface ai = loadAction(type);
		ai.setDBConnection(dbConn);
		ai.setAttributes(attributes);
		ai.retrieve(req);
	}
	
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {
		HttpSession ses = req.getSession();
		ReqType type = getReqType(req);
		log.debug("display=" + type);
		
		configureSession(ses, req, attributes);
				
		SMTActionInterface ai = loadAction(type);
		ai.setDBConnection(dbConn);
		ai.setAttributes(attributes);
		ai.build(req);
	}
	
	
	/**
	 * getReqType wraps the cast from String to ReqType to trap the exception 
	 * (easily exploitable since passed in URLs)
	 * @param req
	 * @return
	 */
	protected ReqType getReqType(SMTServletRequest req) {
		ReqType val = ReqType.catalog;
		try {
			val = ReqType.valueOf(req.getParameter("display"));
		} catch (Exception e) {
			log.error(e);
			//ignore malicious/mischievous requests
		}
		return val;
	}
	
	
	/**
	 * simple action loader used by the retrieve and build methods above
	 * @param type
	 * @return
	 */
	private SMTActionInterface loadAction(ReqType type) {		
		SMTActionInterface ai = null;
		switch (type) {
			case orders:
				ai = new MyOrdersAction(actionInit);
				break;
			case assets:
				ai = new MyAssetsAction(actionInit);
				break;
			case account:
				ai = new AccountAction(actionInit);
				break;
			case profile:
				ai = new MyProfileAction(actionInit);
				break;
			case invoices:
				ai = new InvoicesAction(actionInit);
				break;
			case history:
				ai = new PaymentHistoryAction(actionInit);
				break;
			case dsol:
				ai = new DSOLAction(actionInit);
				break;
			case product:
				ai = new ProductDetailAction(actionInit);
				break;
			case search:
				ai = new ProductSearchAction(actionInit);
				break;
			case catalog:
			default:
				ai = new CatalogAction(actionInit);
		}
		return ai;
	}
	
	
	/**
	 * validates basic information exists in session, calls to load it if not.
	 * @param ses
	 * @param req
	 */
	protected static void configureSession(HttpSession ses, SMTServletRequest req, Map<String, Object> attributes) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		String franchiseId = CenterPageAction.getFranchiseId(req);
		FastsignsSessVO franSessVo = (FastsignsSessVO)ses.getAttribute(KeystoneProxy.FRAN_SESS_VO);
		
		//if the request does not have franchiseInfo for this Franchise, load it.
		if (franSessVo == null || franSessVo.getFranchise(franchiseId) == null) {
			loadDefaultSession(req, franSessVo, franchiseId, attributes);
			
		} else if (ses.getAttribute(FastsignsSessVO.FRANCHISE_ID) == null) {
			//ensure we have a webId; almost all transactions revolve around this value
			ses.setAttribute(FastsignsSessVO.FRANCHISE_ID, franchiseId);
		}
		
		if (site.getAliasPathName() != null && site.getAliasPathName() != StringUtil.checkVal(ses.getAttribute(FastsignsSessVO.ECOM_ALIAS_PATH)))
			ses.setAttribute(FastsignsSessVO.ECOM_ALIAS_PATH, site.getAliasPathName());
	}
	
	
	/**
	 * helper method that set ups the default FastsignsSessVO object on session.
	 * This is done here (at the parent level) because most nested actions will rely on it.
	 * @param req
	 */
	public static void loadDefaultSession(SMTServletRequest req, FastsignsSessVO sessVo, String webId, Map<String, Object> attributes) {
		HttpSession ses = req.getSession();
		CheckoutUtil util = new CheckoutUtil(attributes);
		if (sessVo == null)
			sessVo = new FastsignsSessVO();
		
		try {
			sessVo = util.loadFranchiseVO(sessVo, webId);
		} catch (InvalidDataException e) {
			log.error(e);
		} finally {
			util = null;
		}
		ses.setAttribute(KeystoneProxy.FRAN_SESS_VO, sessVo);
		ses.setAttribute(FastsignsSessVO.FRANCHISE_ID, webId);
	}
}
