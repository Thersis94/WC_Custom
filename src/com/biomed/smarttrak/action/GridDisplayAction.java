package com.biomed.smarttrak.action;

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
		String gridId = req.getParameter("gridId");
		ChartType type = ChartType.valueOf(StringUtil.checkVal(req.getParameter("ct"), "NONE").toUpperCase());
		
		// Parse the row and series data
		GridVO grid = getGridData(gridId);
		
		// If the request is for a table
		if (!ChartType.TABLE.equals(type)) {
			
			// return the bean for conversion to json
			this.putModuleData(grid.getTableMap());

		} else if (req.hasParameter("excel")){

			buildExcelFile(req, grid);
			
		} else {
			retrievChartData(req, grid, type);
			
			// return the bean for conversion to json
			this.putModuleData(retrievChartData(req, grid, type));
		}
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
			String fileName = StringUtil.removeNonAlphaNumeric(grid.getTitle(), false);
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
	public SMTGridIntfc retrievChartData(ActionRequest req, GridVO grid, ChartType type) {
		ProviderType pt = ProviderType.valueOf(StringUtil.checkVal(req.getParameter("pt"), "GOOGLE").toUpperCase());
		SMTGridIntfc gridData = SMTChartFactory.getInstance(pt, grid, type);
		
		// Get the chart options
		SMTChartOptionIntfc options = SMTChartOptionFactory.getInstance(type, ProviderType.GOOGLE);
		options.addOptionsFromGridData(grid);
		
		// Add the chart specific options
		gridData.addCustomValues(options.getChartOptions());
		
		// Associate the dynamic options
		if(req.hasParameter("w"))
			gridData.addCustomValue("width", Convert.formatInteger(req.getParameter("w"), 250));
		
		if(req.hasParameter("h"))
			gridData.addCustomValue("height", Convert.formatInteger(req.getParameter("h"), 250));
		
		if(req.hasParameter("isStacked"))
		gridData.addCustomValue("isStacked", Convert.formatBoolean(req.getParameter("isStacked"), false));
		
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


