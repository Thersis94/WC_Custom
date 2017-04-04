package com.biomed.smarttrak.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// App Libs
import com.biomed.smarttrak.admin.GridChartAction;
import com.biomed.smarttrak.admin.vo.GridVO;
import com.biomed.smarttrak.vo.grid.BiomedExcelReport;
import com.biomed.smarttrak.vo.grid.SMTChartFactory;
import com.biomed.smarttrak.vo.grid.SMTChartFactory.ProviderType;
import com.biomed.smarttrak.vo.grid.SMTChartOptionFactory;
import com.biomed.smarttrak.vo.grid.SMTChartOptionFactory.ChartType;
import com.biomed.smarttrak.vo.grid.SMTChartOptionIntfc;
import com.biomed.smarttrak.vo.grid.SMTGridIntfc;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
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
		// Get the request data
		String gridId = req.getParameter("gridId");
		String[] grids = req.getParameterValues("grid");
		boolean full = Convert.formatBoolean(req.getParameter("full"), false);
		boolean stacked = Convert.formatBoolean(req.getParameter("isStacked"), false);
		ProviderType pt = ProviderType.valueOf(StringUtil.checkVal(req.getParameter("pt"), "GOOGLE").toUpperCase());
		
		// Process the data
		if (grids != null && grids.length > 0) {
			this.putModuleData(loadAllGrids(grids, full, stacked, pt));
		} else {
			GridVO grid = getGridData(gridId);
			if (req.hasParameter("excel")) buildExcelFile(req, grid);
			else if (! StringUtil.isEmpty(gridId)) { 
				ChartType type = ChartType.valueOf(StringUtil.checkVal(req.getParameter("ct"), "NONE").toUpperCase());
				this.putModuleData(retrieveChartData(grid, type, full, stacked, pt));
			}
		}
		
	}
	
	/**
	 * Retrieve the data to populate multiple graphs at page load time
	 * @param grids
	 * @param full
	 * @param stacked
	 * @param pt
	 * @return
	 */
	public Map<String, SMTGridIntfc> loadAllGrids(String[] grids, boolean full, boolean stacked, ProviderType pt) {
		Map<String, SMTGridIntfc> data = new HashMap<>(24);
		String schema = getAttribute(Constants.CUSTOM_DB_SCHEMA) + "";
		
		// Parse the map data and place in a map
		Map<Object, String> items = new HashMap<>(grids.length);
		for(String grid : grids) {
			String[] vals = grid.split("\\|");
			items.put(vals[0], vals[1]);
		}
		
		// Retrieve the data
		GridChartAction gca = new GridChartAction(actionInit);
		gca.setAttributes(getAttributes());
		gca.setDBConnection(getDBConnection());
		List<GridVO> gridData = gca.retrievePageChartData(new ArrayList<Object>(items.keySet()), schema);
		
		// Loop the grid data and format for a chart
		for (GridVO grid : gridData) {
			data.put(grid.getGridId(), retrieveChartData(grid, ChartType.valueOf(items.get(grid.getGridId())), full, stacked, pt));
		}
		
		return data;
	}
	
	/**
	 * Builds the excel file and sets it to be streamed
	 * @param req
	 * @param grid
	 */
	public void buildExcelFile(ActionRequest req, GridVO grid) {
			// Add it the the Report
			BiomedExcelReport rpt = new BiomedExcelReport();
			rpt.setData(grid);
			String fileName = StringUtil.removeNonAlphaNumeric(grid.getTitle(), true);
			rpt.setFileName(fileName + ".xls");
			
			// Set the appropriate parameters to stream the file
			req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
			req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
	}
	
	/**
	 * Retrieves the chart data
	 * @param req
	 * @param grid
	 * @param type
	 */
	public SMTGridIntfc retrieveChartData(GridVO grid, ChartType type, boolean full, boolean stacked, ProviderType pt) {
		SMTGridIntfc gridData = SMTChartFactory.getInstance(pt, grid, type);

		
		// Get the chart options
		SMTChartOptionIntfc options = SMTChartOptionFactory.getInstance(type, ProviderType.GOOGLE, full);
		options.addOptionsFromGridData(grid);
		log.debug("options: " + options);
		
		// Add the chart specific options
		gridData.addCustomValues(options.getChartOptions());
		gridData.addCustomValue("width", "100%");
		gridData.addCustomValue("height", "100%");
		
		// Add configurable attributes
		if(stacked) gridData.addCustomValue("isStacked", true);
		
		return gridData;
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


