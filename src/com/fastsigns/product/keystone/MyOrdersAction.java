package com.fastsigns.product.keystone;

import java.util.Collection;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.util.PropertySetStrategy;

import com.fastsigns.product.keystone.vo.OrderVO;
import com.fastsigns.security.FastsignsSessVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.json.PropertyStrategyWrapper;
import com.smt.sitebuilder.action.AbstractBaseAction;
import com.smt.sitebuilder.common.ModuleVO;
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
public class MyOrdersAction extends AbstractBaseAction {

	public MyOrdersAction() {
	}

	/**
	 * @param actionInit
	 */
	public MyOrdersAction(ActionInitVO actionInit) {
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
		KeystoneProxy proxy = new KeystoneProxy(attributes);
		proxy.setSessionCookie(req.getCookie(Constants.JSESSIONID));
		proxy.setModule("jobs");
		proxy.setAction("getOrders");
		proxy.setAccountId(sessVo.getProfile(webId).getAccountId());
		
		try {
			//tell the proxy to go get our data
			byte[] byteData = proxy.getData();
			
			//transform the response into something meaningful to WC
			mod.setActionData(formatData(byteData));
			
		} catch (InvalidDataException e) {
			mod.setError(e);
			mod.setErrorMessage("Unable to load Order History");
		}
		
		setAttribute(Constants.MODULE_DATA, mod);
	}
	
	
	private Collection<?> formatData(byte[] byteData) throws InvalidDataException {
		JsonConfig cfg = new JsonConfig();
		cfg.setPropertySetStrategy(new PropertyStrategyWrapper(PropertySetStrategy.DEFAULT));
		cfg.setRootClass(OrderVO.class);
		
		try {
			JSONObject data = JSONObject.fromObject(new String(byteData));
			JSONArray jsonArr = JSONArray.fromObject(data.get("data"));
			return JSONArray.toCollection(jsonArr, cfg);

		} catch (Exception e) {
			log.error("could not parse JSON", e);
			throw new InvalidDataException(e);
		}
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
