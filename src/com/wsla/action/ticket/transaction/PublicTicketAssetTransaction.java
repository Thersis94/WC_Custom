package com.wsla.action.ticket.transaction;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;

/****************************************************************************
 * <b>Title</b>: PublicTicketAssetTransaction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> TODO Put Something Here
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Nov 26, 2018
 * @updates:
 ****************************************************************************/
public class PublicTicketAssetTransaction extends TicketAssetTransaction {

	/**
	 * 
	 */
	public PublicTicketAssetTransaction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public PublicTicketAssetTransaction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		//make the user somehow
		//add it to req
		
		//req.getSession().setAttribute(Constants.USER_DATA, "value");
		log.debug("########### hit build public ticket micro action");
		super.build(req);
	
	}

}
