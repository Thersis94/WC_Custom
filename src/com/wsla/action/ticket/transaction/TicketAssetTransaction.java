package com.wsla.action.ticket.transaction;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
// WC Libs
import com.wsla.action.BasePortalAction;
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.action.ticket.CASSelectionAction;
// WSLA Libs
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketAssignmentVO;
import com.wsla.data.ticket.TicketAssignmentVO.TypeCode;
import com.wsla.data.ticket.TicketDataVO;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.TicketVO;
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

public class TicketAssetTransaction extends BaseTransactionAction {
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
			if (req.hasParameter("isApproved")) {
				approveAssets(req);
			} else {
				saveAsset(req);
			}
		} catch (InvalidDataException | DatabaseException | SQLException e) {
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
		TicketDataVO td = new TicketDataVO(req);
		
		// Get the DB Processor
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		// Get the WSLA User
		UserVO user = (UserVO)getAdminUser(req).getUserExtendedInfo();
		
		// Add a ledger entry
		TicketLedgerVO ledger = changeStatus(td.getTicketId(), user.getUserId(), StatusCode.USER_DATA_APPROVAL_PENDING, LedgerSummary.ASSET_LOADED.summary, null);
		
		// Build the next step
		Map<String, Object> params = new HashMap<>();
		params.put("ticketId", ledger.getTicketId());
		buildNextStep(ledger.getStatusCode(), new BasePortalAction().getResourceBundle(req), params, false);
		
		// Build the additional Ticket Data
		td.setLedgerEntryId(ledger.getLedgerEntryId());
		td.setMetaValue(req.getParameter("fileName"));
		
		db.save(td);
	}
	
	/**
	 * Approves the assets, and moves the ticket to the next status
	 * 
	 * @param req
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 * @throws SQLException 
	 */
	public void approveAssets(ActionRequest req) throws InvalidDataException, DatabaseException, SQLException {
		boolean isApproved = Convert.formatBoolean(req.getParameter("isApproved"));
		if (!isApproved)
			return;
		
		ResourceBundle bundle = new BasePortalAction().getResourceBundle(req);
		
		// Change status to user data complete indicating it was approved.
		TicketVO ticket = new TicketVO(req);
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		TicketLedgerVO ledger = changeStatus(ticket.getTicketId(), user.getUserId(), StatusCode.USER_DATA_COMPLETE, LedgerSummary.ASSET_APPROVED.summary, null);
		buildNextStep(ledger.getStatusCode(), bundle, new HashMap<>(), false);
		
		// Assign the nearest CAS
		CASSelectionAction csa = new CASSelectionAction(getDBConnection(), getAttributes());
		List<GenericVO> locations = csa.getUserSelectionList(ticket.getTicketId(), user.getLocale());
		if (!locations.isEmpty()) {
			GenericVO casLocation = locations.get(0);
			
			TicketAssignmentVO tAss = new TicketAssignmentVO(req);
			tAss.setLocationId(casLocation.getKey().toString());
			tAss.setTypeCode(TypeCode.CAS);

			TicketAssignmentTransaction tat = new TicketAssignmentTransaction(getDBConnection(), getAttributes());
			tat.assign(tAss, user, bundle);
			setNextStep(tat.getNextStep());
		}
	}
}

