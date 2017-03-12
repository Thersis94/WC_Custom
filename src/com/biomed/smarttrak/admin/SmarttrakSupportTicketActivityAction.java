package com.biomed.smarttrak.admin;

import com.siliconmtn.action.ActionInitVO;
import com.smt.sitebuilder.action.support.SupportTicketActivityAction;
import com.smt.sitebuilder.action.support.TicketActivityVO;

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
}
