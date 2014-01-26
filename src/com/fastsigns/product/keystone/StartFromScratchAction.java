package com.fastsigns.product.keystone;

import javax.servlet.http.HttpSession;

import com.fastsigns.action.franchise.CenterPageAction;
import com.fastsigns.product.keystone.parser.KeystoneDataParser;
import com.fastsigns.security.FastsignsSessVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.AbstractBaseAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

public class StartFromScratchAction extends AbstractBaseAction {
	
	public StartFromScratchAction(){
		
	}
	
	public StartFromScratchAction(ActionInitVO actionInit){
		super(actionInit);
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		//Clear out old template data that may exist.
		HttpSession sess = req.getSession();
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String franchiseId = (String)sess.getAttribute(FastsignsSessVO.FRANCHISE_ID);
		
		if (franchiseId == null)
			ProductFacadeAction.configureSession(sess, req, attributes);
		
		//Use Cached action and set necessary pieces for cache groups to be used. 
		attributes.put(Constants.SITE_DATA, req.getAttribute(Constants.SITE_DATA));
		attributes.put("wcFranchiseId", CenterPageAction.getFranchiseId(req));
		KeystoneProxy proxy = KeystoneProxy.newInstance(attributes);
		proxy.setSessionCookie(req.getCookie(Constants.JSESSIONID));
		proxy.setModule("products");
		proxy.setAction("getDsolMaterials");
		proxy.addPostData("franchiseId", franchiseId);
		proxy.setParserType(KeystoneDataParser.DataParserType.FromScratch);
		
		
		//tell the proxy to go get our data
		try {
			mod.setActionData(proxy.getData().getActionData());
			
		} catch (Exception e) {
			log.error(e);
			mod.setError(e);
			mod.setErrorMessage("Unable to load Materials list");
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
