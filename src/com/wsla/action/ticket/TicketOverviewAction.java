package com.wsla.action.ticket;

import java.util.List;

// SMT Base Libs 3.x
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;

// WC Libs 3.x
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.ticket.TicketVO;

/****************************************************************************
 * <b>Title</b>: TicketOverviewAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the overview data for the service order ticket
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 15, 2018
 * @updates:
 ****************************************************************************/

public class TicketOverviewAction extends SBActionAdapter {

	/**
	 * 
	 */
	public TicketOverviewAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TicketOverviewAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.info("Testing ....");
	}
	
	
	public List<TicketVO> getTicketList(ActionRequest req) {
		
		return null;
	}

}

