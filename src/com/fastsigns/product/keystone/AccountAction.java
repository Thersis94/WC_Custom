package com.fastsigns.product.keystone;

import java.util.Collection;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.util.PropertySetStrategy;

import com.fastsigns.product.keystone.vo.AccountVO;
import com.fastsigns.security.FastsignsSessVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.json.PropertyStrategyWrapper;
import com.smt.sitebuilder.action.AbstractBaseAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: AccountAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 5, 2012
 ****************************************************************************/
public class AccountAction extends AbstractBaseAction {

	public AccountAction() {
	}

	/**
	 * @param actionInit
	 */
	public AccountAction(ActionInitVO actionInit) {
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
		
		//TODO reactivate caching proxy
		//KeystoneProxy proxy = new CachingKeystoneProxy(attributes, 10);
		KeystoneProxy proxy = new KeystoneProxy(attributes);
		proxy.setSessionCookie(req.getCookie(Constants.JSESSIONID));
		proxy.setModule("accounts");
		proxy.setAction("viewRollupData");
		if (req.hasParameter("groupId")) {
			//get all the accounts in this hierarchy
			proxy.addPostData("groupId",req.getParameter("groupId"));
		} else {
			//get 'this' account only (default)
			proxy.setAccountId(sessVo.getProfile(webId).getAccountId());
		}
		
		try {
			//tell the proxy to go get our data
			byte[] byteData = proxy.getData();
			
			//transform the response into something meaningful to WC
			mod.setActionData(formatData(byteData));
			
		} catch (InvalidDataException e) {
			log.error(e);
			mod.setError(e);
			mod.setErrorMessage("Unable to load Account Details");
		}
		
		//put the accountId on the request for easy access; we need it in the View.
		req.setAttribute("accountId", sessVo.getProfile(webId).getAccountId());
		setAttribute(Constants.MODULE_DATA, mod);
	}

	@SuppressWarnings("unchecked")
	private Collection<Node> formatData(byte[] byteData) throws InvalidDataException {
		List<Node> data = null;
		
		JsonConfig cfg = new JsonConfig();
		cfg.setPropertySetStrategy(new PropertyStrategyWrapper(PropertySetStrategy.DEFAULT));
		cfg.setRootClass(Node.class);
		
		try {
			JSONObject baseObj = JSONObject.fromObject(new String(byteData));
			JSONArray nodesArr = JSONArray.fromObject(baseObj.get("tree"));
			data = (List<Node>) JSONArray.toCollection(nodesArr, cfg);
			
			for (Node n : data) {
				//log.debug(n.getUserObject());
				JSONObject obj = JSONObject.fromObject(n.getUserObject());
				n.setUserObject(JSONObject.toBean(obj, AccountVO.class));
			}
		} catch (Exception e) {
			log.error("could not parse JSON", e);
			throw new InvalidDataException(e);
		}
		
		return data;
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
