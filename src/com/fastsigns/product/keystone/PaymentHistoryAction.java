package com.fastsigns.product.keystone;

import com.fastsigns.product.keystone.parser.KeystoneDataParser;
import com.fastsigns.security.FastsignsSessVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.AbstractBaseAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: PaymentHistoryAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 3, 2012
 ****************************************************************************/
public class PaymentHistoryAction extends AbstractBaseAction {

	public PaymentHistoryAction() {
	}

	/**
	 * @param actionInit
	 */
	public PaymentHistoryAction(ActionInitVO actionInit) {
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
		
		//KeystoneProxy proxy = new CachingKeystoneProxy(attributes, 10);
		KeystoneProxy proxy = KeystoneProxy.newInstance(attributes);
		proxy.setSessionCookie(req.getCookie(Constants.JSESSIONID));
		proxy.setModule("payments");
		proxy.setAction("get_payments_by_account_id");
		proxy.setAccountId(sessVo.getProfile(webId).getAccountId());
		proxy.setParserType(KeystoneDataParser.DataParserType.PaymentHistory);
		
		try {
			//tell the proxy to go get our data
			mod.setActionData(proxy.getData().getActionData());
			
		} catch (Exception e) {
			log.error(e);
			mod.setError(e);
			mod.setErrorMessage("Unable to load Payment History");
		}

		setAttribute(Constants.MODULE_DATA, mod);
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
	 * @see com.siliconmtn.action.SMTActionInterface#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
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
