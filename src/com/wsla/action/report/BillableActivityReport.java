package com.wsla.action.report;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: BillableActivityReport.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Retrieves the data for the billable activity report
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 25, 2019
 * @updates:
 ****************************************************************************/

public class BillableActivityReport extends SBActionAdapter {

	public static final String AJAX_KEY = "billing";
	
	/**
	 * 
	 */
	public BillableActivityReport() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public BillableActivityReport(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.info("BAR");
		if (! req.hasParameter("json")) return;
	}

}

