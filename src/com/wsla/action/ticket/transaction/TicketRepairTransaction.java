package com.wsla.action.ticket.transaction;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.util.Convert;
// WC Libs
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: TicketRepairTransaction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages repair related micro-transactions
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Nov 20, 2018
 * @updates:
 ****************************************************************************/

public class TicketRepairTransaction extends BaseTransactionAction {

	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "repair";
	
	/**
	 * 
	 */
	public TicketRepairTransaction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TicketRepairTransaction(ActionInitVO actionInit) {
		super(actionInit);
	}
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		try {
			changeRepairStatus(req);
		} catch (Exception e) {
			log.error("Unable to change repair status", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}

	/**
	 * Changes the status of the repair... started, finished, not repairable, etc
	 * 
	 * @param req
	 * @throws DatabaseException 
	 */
	private void changeRepairStatus(ActionRequest req) throws DatabaseException {
		boolean isStart = Convert.formatBoolean(req.getParameter("isStart"));
		if (!isStart) return;
		
		TicketVO ticket = new TicketVO(req);
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		TicketLedgerVO ledger = changeStatus(ticket.getTicketId(), user.getUserId(), StatusCode.CAS_IN_REPAIR, LedgerSummary.REPAIR_STATUS_CHANGED.summary, null);

		// Build next step
		buildNextStep(ledger.getStatusCode(), null, false);
	}
}

