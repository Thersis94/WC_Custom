/**
 *
 */
package com.biomed.smarttrak.admin;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.support.SupportTicketAttachmentAction;
import com.smt.sitebuilder.action.support.SupportTicketFacadeAction;


/****************************************************************************
 * <b>Title</b>: SupportFacadeAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Support Facade Routing some functionality through custom
 * Smarttrak Actions.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Feb 24, 2017
 ****************************************************************************/
public class SupportFacadeAction extends SupportTicketFacadeAction {

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		ActionInterface a = getAction(ActionType.TICKET);

		a.delete(req);
	}

	/**
	 * @param attachment
	 * @return
	 */
	protected ActionInterface getAction(ActionType actionType) {
		ActionInterface a;

		switch(actionType) {
			case ACTIVITY:
				a = new SmarttrakSupportTicketActivityAction(this.actionInit);
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