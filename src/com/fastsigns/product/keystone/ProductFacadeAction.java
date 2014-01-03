package com.fastsigns.product.keystone;

import java.util.Map;

import com.fastsigns.action.franchise.CenterPageAction;
import com.fastsigns.product.keystone.checkout.CheckoutUtil;
import com.fastsigns.security.FastsignsSessVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
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
	
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		ReqType type = null;
		
		//verify we have some basic information in session.  All child actions are banking on this!
		if (req.getSession().getAttribute(KeystoneProxy.FRAN_SESS_VO) == null) {
			loadDefaultSession(req, true, attributes);
		} else if(((FastsignsSessVO)req.getSession().getAttribute(KeystoneProxy.FRAN_SESS_VO)).getFranchise(CenterPageAction.getFranchiseId(req)) == null) {
			loadDefaultSession(req, false, attributes);
		} else if (req.getSession().getAttribute(FastsignsSessVO.FRANCHISE_ID) == null) {
			//ensure we have a webId; almost all transactions revolve around this value
			req.getSession().setAttribute(FastsignsSessVO.FRANCHISE_ID, CenterPageAction.getFranchiseId(req));
		}
		
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
	
	public void build(SMTServletRequest req) throws ActionException {
		ReqType type = getReqType(req);
		log.debug("display=" + type);
		
		//verify we have some basic information in session.  All child actions are banking on this!
		if (req.getSession().getAttribute(KeystoneProxy.FRAN_SESS_VO) == null) {
			loadDefaultSession(req, true, attributes);
		} else if(((FastsignsSessVO)req.getSession().getAttribute(KeystoneProxy.FRAN_SESS_VO)).getFranchise(CenterPageAction.getFranchiseId(req)) == null) {
			loadDefaultSession(req, false, attributes);
		} else if (req.getSession().getAttribute(FastsignsSessVO.FRANCHISE_ID) == null) {
			//ensure we have a webId; almost all transactions revolve around this value
			req.getSession().setAttribute(FastsignsSessVO.FRANCHISE_ID, CenterPageAction.getFranchiseId(req));
		}
				
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
	 * helper method that set ups the default FastsignsSessVO object on session.
	 * This is done here (at the parent level) because most nested actions will rely on it.
	 * @param req
	 */
	public static void loadDefaultSession(SMTServletRequest req, boolean createSessVo, Map<String, Object> attributes) {
		
		CheckoutUtil util = new CheckoutUtil(attributes);
		String webId = CenterPageAction.getFranchiseId(req);
		FastsignsSessVO sessVo = null;
		if(createSessVo)
			sessVo = new FastsignsSessVO();
		else
			sessVo = (FastsignsSessVO) req.getSession().getAttribute(KeystoneProxy.FRAN_SESS_VO);
		try {
			sessVo = util.loadFranchiseVO(sessVo, webId);
		} catch (InvalidDataException e) {
			log.error(e);
		}
		req.getSession().setAttribute(KeystoneProxy.FRAN_SESS_VO, sessVo);
		req.getSession().setAttribute(FastsignsSessVO.FRANCHISE_ID, webId);
		
	}
}
