package com.biomed.smarttrak.action;

// JDK 1.8
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.math.BigDecimal;

// App Libs
import com.biomed.smarttrak.admin.GridChartAction;
import com.biomed.smarttrak.admin.vo.GridDetailVO;
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
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
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
		ChartType type = ChartType.valueOf(StringUtil.checkVal(req.getParameter("ct"), "NONE").toUpperCase());
		
		// Check to see if there is a mapping for the grid when displaying a table
		if (ChartType.TABLE.equals(type)) lookupTableMap(req);
		
		// Get the request data
		String gridId = req.getParameter("gridId");
		String[] grids = req.getParameterValues("grid");
		boolean full = Convert.formatBoolean(req.getParameter("full"), false);
		boolean stacked = Convert.formatBoolean(req.getParameter("isStacked"), false);
		ProviderType pt = ProviderType.valueOf(StringUtil.checkVal(req.getParameter("pt"), "GOOGLE").toUpperCase());
		
		boolean display = Convert.formatBoolean(req.getParameter("display"));
		
		// Get the list of columns and convert to integer list
		List<Integer> columns = new ArrayList<>();
		if (! StringUtil.isEmpty(req.getParameter("columns"))) {
			List<String> sCols = Arrays.asList(req.getParameter("columns").split("\\,"));
			columns = sCols.stream().map(Integer::parseInt).collect(Collectors.toList());
		}

		// Process the data
		if (grids != null && grids.length > 0) {
			this.putModuleData(loadAllGrids(grids, full, stacked, pt));
		} else {
			if (display && ChartType.TABLE.equals(type)) display = false;
			GridVO grid = getGridData(gridId, display);
			if (req.hasParameter("excel")) buildExcelFile(req, grid);
			
			else if (! StringUtil.isEmpty(gridId)) { 
				this.putModuleData(retrieveChartData(grid, type, full, stacked, pt, columns	));
			}
		}
		
	}
	
	/**
	 * Looks up the gridId for a chart that utilizes a different data set for the table representation
	 * @param req If a mapping is found, the gridId on the request object is overridden with the new value
	 */
	public void lookupTableMap(ActionRequest req) {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(164);
		
		sql.append("select grid_id from ").append(schema).append("biomedgps_grid ");
		sql.append("where lower(slug_txt) in ( ");
		sql.append("select lower(gtm.slug_txt) from ").append(schema).append("biomedgps_grid g ") ;
		sql.append("inner join ").append(schema).append(" biomedgps_grid_table_map gtm on g.slug_txt = gtm.grid_graphic_id ");
		sql.append("where lower(g.grid_id) = lower(?) or lower(g.slug_txt) = lower(?) ) ");

		List<Object> params =  Arrays.asList(new Object[]{req.getParameter("gridId"),req.getParameter("gridId") });
		DBProcessor dbp = new DBProcessor(getDBConnection());
		List<Object> data = dbp.executeSelect(sql.toString(), params, new GridVO());
		
		if (! data.isEmpty()) {
			GridVO grid = (GridVO) data.get(0);
			req.setParameter("gridId", grid.getGridId());
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
		Map<Object, GenericVO> items = new HashMap<>(grids.length);
		for(String grid : grids) {
			String[] vals = grid.split("\\|");
			String columns = "";
			if (vals.length == 3) columns = vals[2];
			items.put(vals[0], new GenericVO(vals[1], columns));
		}
		
		// Retrieve the data
		GridChartAction gca = new GridChartAction(actionInit);
		gca.setAttributes(getAttributes());
		gca.setDBConnection(getDBConnection());
		List<GridVO> gridData = gca.retrievePageChartData(new ArrayList<Object>(items.keySet()), schema);
		
		// Loop the grid data and format for a chart
		for (GridVO grid : gridData) {
			List<Integer> cols = new ArrayList<>();
			
			// Since the iD can be the grid id or the slug (Backwards compatibility)
			// Figure out which is which and assign to the id
			String id = grid.getGridId();
			if (items.get(grid.getGridId()) == null)  id = grid.getSlug();
			
			// Parse pout the passed in data and format for calling each chart
			ChartType ct = ChartType.valueOf(items.get(id).getKey() + "");
			String columns = StringUtil.checkVal(items.get(id).getValue());
			if (columns.length() > 0) {
				List<String> sCols = Arrays.asList(columns.split("\\,"));
				cols = sCols.stream().map(Integer::parseInt).collect(Collectors.toList());
			}
			
			// Retrieve the data for all of the charts
			data.put(id, retrieveChartData(grid, ct, full, stacked, pt, cols));
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
	 * @param cols List of columns to display.  Blank equals all
	 */
	public SMTGridIntfc retrieveChartData(GridVO grid, ChartType type, boolean full, boolean stacked, ProviderType pt, List<Integer> cols) {
		// Pie charts need to have their labels modified in order to
		// get all pertinant information to the user.
		if (ChartType.PIE == type) {
			modifyLabel(grid);
		}
		
		SMTGridIntfc gridData = SMTChartFactory.getInstance(pt, grid, type, full, cols);
		
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
	 * Check to see if the label needs to be modified
	 * and do so if necessary.
	 */
	private void modifyLabel(GridVO grid) {
		// Add up all values to see if the chart was generated
		// with percentages instead of actual values.
		BigDecimal total = new BigDecimal(0);
		for (GridDetailVO detail : grid.getDetails()) {
			total = total.add(new BigDecimal(detail.getValue1()));
		}
		
		// If the total is 100 the percentage is functionally 
		// the same as the value and appending it to the 
		// label will result in needless duplication of data.
		if (total.compareTo(new BigDecimal(100)) == 0) return;
		
		for (GridDetailVO detail : grid.getDetails()) {
			detail.setLabel(detail.getLabel() + " - " + detail.getValue1());
		}
		
	}

	/**
	 * Calls the GridChartAction to retrieve the grid and grid details for the provided ID
	 * @param gridId
	 * @return
	 */
	public GridVO getGridData(String gridId, boolean display) {
		GridChartAction gca = new GridChartAction(actionInit);
		gca.setAttributes(getAttributes());
		gca.setDBConnection(getDBConnection());
		
		gca.retrieveData(gridId, attributes.get(Constants.CUSTOM_DB_SCHEMA) + "", display);
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		
		return (GridVO) mod.getActionData();
	}

}


