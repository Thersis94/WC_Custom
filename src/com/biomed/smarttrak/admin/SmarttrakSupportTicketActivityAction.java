package com.biomed.smarttrak.admin;

import java.util.LinkedHashMap;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.parser.DirectoryParser;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.support.SupportTicketAction;
import com.smt.sitebuilder.action.support.SupportTicketActivityAction;
import com.smt.sitebuilder.action.support.TicketActivityVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: SmarttrakSupportTicketActivityAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Custom Action overrides default Email Behavior
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Mar 12, 2017
 ****************************************************************************/
public class SmarttrakSupportTicketActivityAction extends SupportTicketActivityAction {

	public SmarttrakSupportTicketActivityAction() {
		super();
	}

	public SmarttrakSupportTicketActivityAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Helper method that sends an email built off the TicketActivityVO.
	 * @param message
	 * @param t
	 */
	@Override
	protected void sendEmailToCustomer(TicketActivityVO act) {
		/*
		 * TODO - Need to Get email to customer and add to BiomedSupportEmailUtil.
		 * and remove call to super.
		 */
		super.sendEmailToCustomer(act);
		
		//new BiomedSupportEmailUtil(getDBConnection(), getAttributes()).sendEmail(act);
	}

	@Override
	protected LinkedHashMap<String, Object> getParams(ActionRequest req) throws ActionException {
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();

		//Check for TicketId
		if(!StringUtil.isEmpty(req.getParameter(SupportTicketAction.TICKET_ID))) {
			params.put(SupportTicketAction.TICKET_ID, req.getParameter(SupportTicketAction.TICKET_ID));
		} else if(req.hasParameter(DirectoryParser.PARAMETER_PREFIX + "1")) {
			params.put(SupportTicketAction.TICKET_ID, req.getParameter(DirectoryParser.PARAMETER_PREFIX + "1"));
			req.setParameter(SupportTicketAction.TICKET_ID, req.getParameter(DirectoryParser.PARAMETER_PREFIX + "1"));
		} else {
			throw new ActionException("Missing Ticket Id on request.");
		}

		//Check for role Level.  If Registered, filter internal messages out.
		SBUserRole r = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		if(AdminControllerAction.DEFAULT_ROLE_LEVEL >= r.getRoleLevel()) {
			params.put("internalFlg", 0);
		}

		//Check for ActivityId
		if(!StringUtil.isEmpty(req.getParameter(ACTIVITY_ID))) {
			params.put(ACTIVITY_ID, req.getParameter(ACTIVITY_ID));
		}

		return params;
	}
}
