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

// WSLA Libs
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.TicketDataVO;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: TicketAssetTransaction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Micro changes to the asset feature
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 23, 2018
 * @updates:
 ****************************************************************************/

public class TicketAssetTransaction extends SBActionAdapter {
	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "asset";
	
	/**
	 * 
	 */
	public TicketAssetTransaction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TicketAssetTransaction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		try {
			this.saveAsset(req);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Unable to save asset", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * Saves a file asset loaded into the system
	 * @param req
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	public void saveAsset(ActionRequest req) throws InvalidDataException, DatabaseException {
		// Get the DB Processor
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		// Get the WSLA User
		UserVO user = (UserVO)getAdminUser(req).getUserExtendedInfo();
		
		// Add a ledger entry
		TicketLedgerVO ledger = new TicketLedgerVO(req);
		ledger.setDispositionBy(user.getUserId());
		ledger.setSummary(LedgerSummary.ASSET_LOADED.summary);
		db.save(ledger);
		
		// Build the Ticket Data
		TicketDataVO td = new TicketDataVO(req);
		td.setLedgerEntryId(ledger.getLedgerEntryId());
		td.setMetaValue(req.getParameter("fileName"));
		
		db.save(td);
	}
}

