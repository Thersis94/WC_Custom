package com.biomed.smarttrak.fd;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;

/****************************************************************************
 * <b>Title</b>: FinancialDashAdminAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Feb 06, 2017
 ****************************************************************************/

public class FinancialDashAdminAction extends FinancialDashAction {
	
	public FinancialDashAdminAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public FinancialDashAdminAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		dashType = DashType.ADMIN;
		super.retrieve(req);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		dashType = DashType.ADMIN;
		super.build(req);
	}
}
