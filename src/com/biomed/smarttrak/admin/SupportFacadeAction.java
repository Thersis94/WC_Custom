/**
 *
 */
package com.biomed.smarttrak.admin;

import com.siliconmtn.action.ActionInterface;
import com.smt.sitebuilder.action.support.SupportTicketActivityAction;
import com.smt.sitebuilder.action.support.SupportTicketAttachmentAction;
import com.smt.sitebuilder.action.support.SupportTicketFacadeAction;


/****************************************************************************
 * <b>Title</b>: SupportFacadeAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> TODO
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author raptor
 * @version 1.0
 * @since Feb 24, 2017
 ****************************************************************************/
public class SupportFacadeAction extends SupportTicketFacadeAction {

	/**
	 * @param attachment
	 * @return
	 */
	protected ActionInterface getAction(ActionType actionType) {
		ActionInterface a;

		switch(actionType) {
			case ACTIVITY:
				a = new SupportTicketActivityAction(this.actionInit);
				break;
			case ATTACHMENT:
				a = new SupportTicketAttachmentAction(this.actionInit);
				break;
			case TICKET:
			default:
				a = new SmarttrakSupportTicketAction(this.actionInit);
				break;
		}

		a.setAttributes(getAttributes());
		a.setDBConnection(getDBConnection());
		return a;
	}
}
