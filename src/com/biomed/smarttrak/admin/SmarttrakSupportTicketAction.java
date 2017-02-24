/**
 *
 */
package com.biomed.smarttrak.admin;

import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.action.support.SupportTicketAction;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: SupportTicketAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Custom Support Ticket Action that extends the Base
 * SupportTicketAction for Biomed Smarttrak.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Feb 24, 2017
 ****************************************************************************/
public class SmarttrakSupportTicketAction extends SupportTicketAction {

	public SmarttrakSupportTicketAction() {
		super();
	}

	public SmarttrakSupportTicketAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Override basic retrieval callback and additionally retrieve managers for
	 * assignment.
	 */
	public void retrieveCallback(ActionRequest req, List<Object> items) throws ActionException {
		super.retrieveCallback(req, items);

		//Load Managers for assigning tickets.
		loadManagers(req);
	}

	/**
	 * Load smarttrak managers for use with assigning tickets.
	 * @param req
	 * @throws ActionException
	 */
	protected void loadManagers(ActionRequest req) throws ActionException {
		AccountAction aa = new AccountAction(this.actionInit);
		aa.setAttributes(getAttributes());
		aa.setDBConnection(getDBConnection());
		aa.loadManagerList(req, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
	}

	/**
	 * Helper method that loads Profile Data for a Smarttrak User.
	 * @param orgId 
	 * @param profileId 
	 * @param req
	 * @return
	 */
	protected UserDataVO getProfileData(String profileId, String orgId, ActionRequest req) throws ActionException {
		UserDataVO u = null;
	
		//Get Profile Data for Ticket Reporter
		AccountUserAction aua = new AccountUserAction(this.actionInit);
		aua.setDBConnection(getDBConnection());
		aua.setAttributes(getAttributes());
		List<Object> users = aua.loadAccountUsers(req, profileId);

		if(!users.isEmpty()) {
			u = (UserDataVO) users.get(0);
		}

		return u;
	}
}