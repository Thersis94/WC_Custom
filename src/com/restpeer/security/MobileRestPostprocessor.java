package com.restpeer.security;

import com.restpeer.common.RPConstants;
import com.restpeer.common.RPConstants.MemberType;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> MobileRestPostprocessor.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> 
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Jun 07 2019
 * @updates:
 ****************************************************************************/

public class MobileRestPostprocessor extends SBActionAdapter {

	public MobileRestPostprocessor() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public MobileRestPostprocessor(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		super.build(req);
		
		// Create the core user data required by the app
		RegistrationPostprocessor rp = new RegistrationPostprocessor(getDBConnection(), getAttributes());
		rp.setMemberType(MemberType.CUSTOMER);
		rp.build(req);
		
		// Redirect to the cart so that user can pay for the service
		req.setParameter(Constants.REDIRECT_URL, RPConstants.CART_PATH);
	}

}
