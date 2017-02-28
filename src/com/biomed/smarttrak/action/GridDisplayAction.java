package com.biomed.smarttrak.action;

import com.biomed.smarttrak.admin.GridChartAction;
import com.biomed.smarttrak.admin.vo.GridVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/********************************************************************
 * <b>Title: </b>GridDisplayAction.java<br/>
 * <b>Description: </b>Retrieves the data for the desired grid and formats the data into a hierarchy
 * of JSON data native to the type of chart library being used<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author james
 * @version 3.x
 * @since Feb 27, 2017
 * Last Updated:
 * 	
 *******************************************************************/
public class GridDisplayAction extends SBActionAdapter {

	/**
	 * 
	 */
	public GridDisplayAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public GridDisplayAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String gridId = req.getParameter("gridId");
		GridVO grid = getGridData(gridId);

		log.info("**********: " + grid);
	}
	
	/**
	 * Calls the GridChartAction to retrieve the grid and grid details for the provided ID
	 * @param gridId
	 * @return
	 */
	public GridVO getGridData(String gridId) {
		GridChartAction gca = new GridChartAction(actionInit);
		gca.setAttributes(getAttributes());
		gca.setDBConnection(getDBConnection());
		gca.retrieveData(gridId, attributes.get(Constants.CUSTOM_DB_SCHEMA) + "", false);
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA); 
		return (GridVO) mod.getActionData();
		
	}

}

