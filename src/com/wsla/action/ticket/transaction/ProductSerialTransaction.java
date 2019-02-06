package com.wsla.action.ticket.transaction;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.action.ticket.TicketOverviewAction;
import com.wsla.data.product.ProductSerialNumberVO;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: ProductSerialTransaction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages product serial micro-transactions
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Dec 12, 2018
 * @updates:
 ****************************************************************************/

public class ProductSerialTransaction extends BaseTransactionAction {

	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "productSerial";
	
	/**
	 * 
	 */
	public ProductSerialTransaction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public ProductSerialTransaction(ActionInitVO actionInit) {
		super(actionInit);
	}
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		try {
			log.debug("????????? started? ");
			if (req.hasParameter("isSerialUpdate")) {
				log.debug("???????? in if? ");
				putModuleData(editProductSerialNumber(req));
			}
				
		} catch (Exception e) {
			log.error("Unable to update product serial data", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}

	/**
	 * Updates data for a given product serial record
	 * 
	 * @param req
	 * @return
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public TicketVO editProductSerialNumber(ActionRequest req) throws InvalidDataException, DatabaseException {
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		UserVO user = null;
		
		// Get the WSLA User
		if(req.hasParameter("userId") && req.hasParameter("publicUserForm")) {
			//coming in from the public user portal the id is on the form.
			user = new UserVO();
			user.setUserId(req.getParameter("userId"));
		}else {
			//coming in from he secure wsla portal the user object is available
			user = (UserVO)getAdminUser(req).getUserExtendedInfo();
		}
		
		// Get existing base ticket data
		TicketVO ticket = new TicketVO(req);
		dbp.getByPrimaryKey(ticket);
		
		// Get existing product serial data
		ProductSerialNumberVO psn = new ProductSerialNumberVO();
		psn.setProductSerialId(StringUtil.checkVal(req.getParameter("productSerialId")));
		dbp.getByPrimaryKey(psn);
		
		// Make an appropriate update based on the data. There are actually three use cases here.
		//     1. Serial number didn't previously exist on the record
		//     2. A serial had been previously submitted, and a new one was now submitted
		//     3. Another serial was found in the system (nothing to do but update the ticket)
		if (StringUtil.isEmpty(psn.getSerialNumber())) {
			psn.setProductId(req.getParameter("productId"));
			psn.setSerialNumber(req.getParameter("serialNumber"));
			dbp.save(psn);
		} else if (ticket.getProductSerialId().equals(psn.getProductSerialId()) && !psn.getSerialNumber().equals(req.getParameter("serialNumber"))) {
			TicketOverviewAction toa = new TicketOverviewAction(getAttributes(), getDBConnection());
			toa.addProductSerialNumber(req, ticket);
			psn = ticket.getProductSerial();
		}
		
		// Update the ticket
		ticket.setPurchaseDate(req.getDateParameter("purchaseDate"));
		ticket.setProductSerialId(psn.getProductSerialId());
		ticket.setProductSerial(psn);
		ticket.setProductWarrantyId(req.getParameter("productWarrantyId"));
		ticket.setStatusCode(Convert.formatBoolean(psn.getValidatedFlag()) ? StatusCode.USER_CALL_DATA_INCOMPLETE : StatusCode.UNLISTED_SERIAL_NO);
		dbp.save(ticket);
		
		// Set the status & next step
		TicketLedgerVO ledger = changeStatus(ticket.getTicketId(), user.getUserId(), ticket.getStatusCode(), LedgerSummary.SERIAL_UPDATED.summary, null);
		buildNextStep(ledger.getStatusCode(), null, false);
		
		return ticket;
	}
}

