package com.wsla.action.ticket;

// JDK 1.8.x
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.TicketDataVO;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: TicketTransactionAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the small transaction updates to the service order
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 22, 2018
 * @updates:
 ****************************************************************************/

public class TicketTransactionAction extends SBActionAdapter {
	/**
	 * Key for the Facade / Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "ticketTransaction";
	
	/**
	 * Key to be passed to utilize this action
	 */
	public static final String SELECT_KEY = "transactionType";
	
	// Map for the key to method xref
	private static Map<String, String> keyMap = new HashMap<>(16);

	/**
	 * Assigns the keys for the select type to method mapping.  In the generic vo
	 * the key is the method name.  The value is a boolean which indicates whether
	 * or not the request object is needed in that method 
	 */
	static {
		keyMap.put("asset", "saveAsset");

	}
	
	/**
	 * 
	 */
	public TicketTransactionAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TicketTransactionAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		String strMethod = keyMap.get(req.getStringParameter(SELECT_KEY));
		log.info("Processing: " + strMethod);
		try {
			if (StringUtil.isEmpty(strMethod)) 
				throw new ActionException("List type Not Found in KeyMap");
			
			Method method = this.getClass().getMethod(strMethod, req.getClass());
			putModuleData(method.invoke(this, req));

		} catch (Exception e) {
			log.error("Unable to retrieve list: ", e);
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
		db.save(td);
	}
}

