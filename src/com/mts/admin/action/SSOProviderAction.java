package com.mts.admin.action;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <p><b>Title:</b> SSOProviderAction.java</p>
 * <p><b>Description:</b> Manages the SSO customers who have access to the platform.
 * Proxies calls to login_module_xr to register login modules to the website.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Aug 26, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class SSOProviderAction extends SimpleActionAdapter {

	public SSOProviderAction() {
		super();
	}

	public SSOProviderAction(ActionInitVO init) {
		super(init);
	}

	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {

	}

	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {

	}
}
