package com.fastsigns.product.keystone;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.util.PropertySetStrategy;

import com.fastsigns.product.keystone.vo.InvoiceReportVO;
import com.fastsigns.product.keystone.vo.InvoiceVO;
import com.fastsigns.security.FastsignsSessVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.json.PropertyStrategyWrapper;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractBaseAction;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: CatalogAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 1, 2012
 ****************************************************************************/
public class InvoicesAction extends AbstractBaseAction {

	public InvoicesAction() {
	}

	/**
	 * @param actionInit
	 */
	public InvoicesAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		FastsignsSessVO sessVo = (FastsignsSessVO) req.getSession().getAttribute(KeystoneProxy.FRAN_SESS_VO);
		String webId = (String)req.getSession().getAttribute(FastsignsSessVO.FRANCHISE_ID);
		
		if (sessVo.getProfile(webId).getAccountId() == null) {
			mod.setErrorMessage("Not authorized or no data to display");
			return; //not logged in, or no account to retrieve
		}
		if(req.hasParameter("invoice_id")) {
			String name = req.getParameter("invoice_id");
			Map<String, String> postData = new HashMap<String, String>();
			postData.put("doAjax", "true");
			postData.put("invoice_id", name);
			
			KeystoneProxy proxy = new KeystoneProxy(attributes);
			proxy.setSessionCookie(req.getCookie(Constants.JSESSIONID));
			proxy.setModule("invoices");
			proxy.setAction("get_print_invoice");
			proxy.setPostData(postData);
			
			try {
				byte [] byteData = proxy.getData();
				AbstractSBReportVO rpt = new InvoiceReportVO();
				rpt.setData(byteData);
				rpt.setFileName(name + ".pdf");
				req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
				req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
			} catch (InvalidDataException e) {
				mod.setError(e);
				mod.setErrorMessage("Unable to load Invoices");
			}
			return ;
		}
		
		//TODO reactivate caching proxy
		//KeystoneProxy proxy = new CachingKeystoneProxy(attributes, 10);
		KeystoneProxy proxy = new KeystoneProxy(attributes);
		proxy.setSessionCookie(req.getCookie(Constants.JSESSIONID));
		proxy.setModule("invoicesAccounts");
		proxy.setAction("getByAccountId");
		proxy.setAccountId(sessVo.getProfile(webId).getAccountId());
		
		try {
			//tell the proxy to go get our data
			byte[] byteData = proxy.getData();
			
			//transform the response into something meaningful to WC
			mod.setActionData(formatDisplayData(byteData));
		
		} catch (InvalidDataException e) {
			mod.setError(e);
			mod.setErrorMessage("Unable to load Invoices");
		}
		
		setAttribute(Constants.MODULE_DATA, mod);
	}
	
	
	private Collection<?> formatDisplayData(byte[] byteData) throws InvalidDataException {
		JsonConfig cfg = new JsonConfig();
		cfg.setPropertySetStrategy(new PropertyStrategyWrapper(PropertySetStrategy.DEFAULT));
		cfg.setRootClass(InvoiceVO.class);
		
		try {
			JSONArray jsonArr = JSONArray.fromObject(new String(byteData));
			return JSONArray.toCollection(jsonArr, cfg);

		} catch (Exception e) {
			log.error("could not parse JSON", e);
			throw new InvalidDataException(e);
		}
	}
	
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		//this method supports the "make a payment" feature.
		//It submits a transaction to Keystone with the payment detail collected by the browser.
		FastsignsSessVO sessVo = (FastsignsSessVO) req.getSession().getAttribute(KeystoneProxy.FRAN_SESS_VO);
		String webId = (String)req.getSession().getAttribute(FastsignsSessVO.FRANCHISE_ID);
		
		Object msg = null;
		try {
			KeystoneProxy proxy = new KeystoneProxy(attributes);
			proxy.setTimeout(45000); //allow 45 seconds, which gives us 15secs to process the WC-side before we lose the browser
			proxy.setSessionCookie(req.getCookie(Constants.JSESSIONID));
			proxy.setModule("payments");
			proxy.setAction("add_payment_to_invoice");
			proxy.addPostData("franchise_id", sessVo.getFranchise(webId).getFranchiseId());
			proxy.addPostData("doJson", "true");
			proxy.addPostData("eComm", "true");
			proxy.addPostData("payment_method_id", "73a61696f100b3858511e212a3feea6b");
			proxy.addPostData("invoice_id", req.getParameter("invoice_id"));
			proxy.addPostData("amount", StringUtil.removeNonNumericExceptDecimal(req.getParameter("amount")));
			proxy.addPostData("ccInfo[ccNum]", StringUtil.removeNonNumeric(req.getParameter("cardNumber")));
			proxy.addPostData("ccInfo[ccExpMo]", StringUtil.removeNonNumeric(req.getParameter("cardMonth")));
			proxy.addPostData("ccInfo[ccExpYear]", StringUtil.removeNonNumeric(req.getParameter("cardYear")));
			proxy.addPostData("ccInfo[ccName]", req.getParameter("cardName"));
			proxy.addPostData("ccInfo[ccSec]", StringUtil.removeNonNumeric(req.getParameter("cvvNumber")));
			proxy.addPostData("ccInfo[ccZip]", StringUtil.removeNonNumeric(req.getParameter("billingZip")));
			
			//submit the transaction
			byte[] byteData = proxy.getData();
			JSONObject resp = JSONObject.fromObject(new String(byteData));
			log.debug("resp=" + resp);
			
			//evaluate the response
			if (!resp.optBoolean("success")) {
				msg = resp.optString("message", null);
				throw new Exception("eComm transaction failed - " + msg);
			}
			
		} catch (Exception e) {
			log.error("could not submit invoice payment transaction", e);
			if (msg == null) msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		}
		

		//redirect the browser back to the page
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder url = new StringBuilder(page.getFullPath());
		url.append("?display=").append(ProductFacadeAction.ReqType.invoices);
		
		if (msg != null) {
			//if we had a failure, return to the user to the payment form.
			url.append("&pay=").append(req.getParameter("invoice_id"));
		} else {
			//else return a success message to the list page
			msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		}
		url.append("&msg=").append(msg);
		
		req.setAttribute(Constants.REDIRECT_REQUEST, true);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}
		
	
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
