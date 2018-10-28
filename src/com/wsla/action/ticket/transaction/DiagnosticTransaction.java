package com.wsla.action.ticket.transaction;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.action.ticket.TicketOverviewAction;
import com.wsla.data.ticket.DiagnosticRunVO;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: DiagnosticTransaction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages adding a new diagnostic on the ticket
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 24, 2018
 * @updates:
 ****************************************************************************/

public class DiagnosticTransaction extends SBActionAdapter {

	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "diagnostic";
	
	/**
	 * 
	 */
	public DiagnosticTransaction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public DiagnosticTransaction(ActionInitVO actionInit) {
		super(actionInit);
	}
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		try {
			this.saveDiagnosticRun(req);
		} catch (Exception e) {
			log.error("Unable to save asset", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}

	/**
	 * Stores the user information
	 * @param req
	 * @return
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 * @throws com.siliconmtn.exception.DatabaseException
	 */
	public void saveDiagnosticRun(ActionRequest req) 
	throws InvalidDataException, DatabaseException {
		
		// Get the WSLA User
		UserVO user = (UserVO)getAdminUser(req).getUserExtendedInfo();
		
		// Add a ledger entry
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		TicketLedgerVO ledger = new TicketLedgerVO(req);
		ledger.setDispositionBy(user.getUserId());
		ledger.setSummary(LedgerSummary.RAN_DIAGNOSTIC.summary);
		db.save(ledger);
		
		DiagnosticRunVO dr = new DiagnosticRunVO(req);
		
		TicketOverviewAction toa = new TicketOverviewAction(attributes, dbConn);
		toa.saveDiagnosticRun(dr);
	}
}

