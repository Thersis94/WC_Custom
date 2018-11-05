package com.wsla.action.ticket.transaction;

import com.siliconmtn.action.ActionException;

// JDK 1.8.x

// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: TicketPDFCreator.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Builds a PDF formatted view of the service order
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Nov 5, 2018
 * @updates:
 ****************************************************************************/
public class TicketPDFCreator extends SBActionAdapter {

	/**
	 * 
	 */
	public TicketPDFCreator() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TicketPDFCreator(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		
	}
}
