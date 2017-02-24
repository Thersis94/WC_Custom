package com.biomed.smarttrak.admin;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SBActionAdapter;

/********************************************************************
 * <b>Title: </b>GridChartAction.java<br/>
 * <b>Description: </b><<<< Some Desc Goes Here >>>><br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author james
 * @version 3.x
 * @since Feb 24, 2017
 * Last Updated:
 * 	
 *******************************************************************/
public class GridChartAction extends SBActionAdapter {

	/**
	 * 
	 */
	public GridChartAction() {
	}

	/**
	 * @param actionInit
	 */
	public GridChartAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.info("Retrieving ...");
	}
}

