package com.wsla.action.ticket.transaction;

import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.action.ticket.TicketEditAction;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: ticketCallCenterTransaction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> This transaction handles changes to normal flow caused by call centers
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Mar 21, 2019
 * @updates:
 ****************************************************************************/
public class ticketCallCenterTransaction extends BaseTransactionAction  {
	public static final String AJAX_KEY = "callCenter";
	public static final String ATTR_REPAIR_CODE = "attr_unitRepairCode";
	/**
	 * 
	 */
	public ticketCallCenterTransaction() {
		super();
	}
	
	public ticketCallCenterTransaction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		this();
		this.dbConn = dbConn;
		this.attributes = attributes;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("########## build called ");
		TicketEditAction tea = new TicketEditAction();
		tea.setActionInit(actionInit);
		tea.setAttributes(getAttributes());
		tea.setDBConnection(getDBConnection());
		
		TicketVO ticket = tea.getBaseTicket(StringUtil.checkVal(req.getParameter("ticketId")));
		
		UserDataVO profile = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		UserVO user = (UserVO)profile.getUserExtendedInfo();

		boolean hasRepairCode  = StringUtil.checkVal(req.getParameter("attribute")).equals(ATTR_REPAIR_CODE);
		
		String attribute = StringUtil.checkVal(req.getParameter("attribute"));
		String attributeValue = StringUtil.checkVal(req.getParameter("attributeValue"));
		
		processTransaction(ticket, user, hasRepairCode, attribute, attributeValue );
	 
	}

	/**
	 * processes the call center transaction
	 * @param user 
	 * @param ticket 
	 * @param hasRepairCode 
	 * @param attributeValue 
	 * @param attribute 
	 * 
	 */
	private void processTransaction(TicketVO ticket, UserVO user, boolean hasRepairCode, String attribute, String attributeValue) {
		log.debug(" processing transaction");
		TicketAssetTransaction tat = new  TicketAssetTransaction();
		tat.setActionInit(actionInit);
		tat.setAttributes(getAttributes());
		tat.setDBConnection(getDBConnection());
		
		TicketDataTransaction tdt = new TicketDataTransaction();
		tdt.setActionInit(actionInit);
		tdt.setAttributes(getAttributes());
		tdt.setDBConnection(getDBConnection());
		
		if(hasRepairCode) {
			try {
				//save the attr value 
				tdt.saveDataAttribute(ticket.getTicketId(), attribute, attributeValue, true);
				//change it quickly to problem solved status
				changeStatus(ticket.getTicketId(), user.getUserId(), StatusCode.PROBLEM_RESOLVED, null, null);
				//close the ticket
				changeStatus(ticket.getTicketId(), user.getUserId(), StatusCode.CLOSED, null, null);
				//add a ledger for the call center resolve
				tat.addLedger(ticket.getTicketId(), user.getUserId(), ticket.getStatusCode(), LedgerSummary.RESOLVED_DURING_CALL.summary, null);
			} catch (Exception e) {
				log.error("could not edit ticket",e);
				setModuleData(null,  0, e.getLocalizedMessage());
				
			}
		}
		
	}
}
