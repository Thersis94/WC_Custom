package com.wsla.action.ticket.transaction;

import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.data.ticket.RefundReplacementVO;

/****************************************************************************
 * <b>Title</b>: RefundReplacementTransaction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> //TODO Change Me
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 4, 2019
 * @updates:
 ****************************************************************************/

public class RefundReplacementTransaction extends BaseTransactionAction {
	
	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "rar";
	
	/**
	 * 
	 */
	public RefundReplacementTransaction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public RefundReplacementTransaction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * @param dbConn
	 * @param attributes
	 */
	public RefundReplacementTransaction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super(dbConn, attributes);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		
		
		if (req.getBooleanParameter("createRar")) {
			RefundReplacementVO rrvo = new RefundReplacementVO(req);
			try {
				this.saveInitial(rrvo);
				putModuleData(rrvo);
			} catch (InvalidDataException | DatabaseException e) {
				log.error("Unable to save: " + rrvo, e);
				putModuleData(rrvo, 0, false, e.getLocalizedMessage(), true);
			}
			
			log.info(rrvo);
		}
		
		
	}
	
	
	public void saveInitial(RefundReplacementVO rrvo) throws InvalidDataException, DatabaseException {
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.save(rrvo);
	}
}

