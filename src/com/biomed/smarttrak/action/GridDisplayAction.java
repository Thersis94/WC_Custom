package com.biomed.smarttrak.action;

// JDK 1.8
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// App Libs
import com.biomed.smarttrak.admin.GridChartAction;
import com.biomed.smarttrak.admin.vo.GridDetailVO;
import com.biomed.smarttrak.admin.vo.GridVO;
import com.biomed.smarttrak.vo.grid.BiomedExcelReport;
import com.biomed.smarttrak.vo.grid.GoogleChartCellVO;
import com.biomed.smarttrak.vo.grid.GoogleChartColumnVO;
import com.biomed.smarttrak.vo.grid.GoogleChartRowVO;
import com.biomed.smarttrak.vo.grid.SMTChartFactory;
import com.biomed.smarttrak.vo.grid.SMTChartFactory.ProviderType;
import com.biomed.smarttrak.vo.grid.SMTChartOptionFactory;
import com.biomed.smarttrak.vo.grid.SMTChartOptionFactory.ChartType;
import com.biomed.smarttrak.vo.grid.SMTChartOptionIntfc;
import com.biomed.smarttrak.vo.grid.SMTGridIntfc;
import com.biomed.smarttrak.vo.grid.SMTGridRowIntfc;
import com.biomed.smarttrak.vo.grid.GoogleChartVO.DataType;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
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
public class GridDisplayAction extends SimpleActionAdapter {

	public static final String GRID_ID = "gridId";
	
	// Constants for the label types
	public static final int ALL_LABELS = 0, VALUE_ONLY = 1, TOTAL_ONLY = 2, NO_LABEL = 3;

	private static final String[] PIE_CHART_COLORS = { "#3366cc","#dc3912","#ff9900","#109618","#990099","#0099c6","#8f8f8f","#e53ac3","#f96125","#316395" };

	public GridDisplayAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public GridDisplayAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
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
		String gridId = req.getParameter(GRID_ID);
		String[] grids = req.getParameterValues("grid");
		boolean full = Convert.formatBoolean(req.getParameter("full"), false);
		boolean stacked = Convert.formatBoolean(req.getParameter("isStacked"), false);
		int labelType = Convert.formatInteger(req.getParameter("labelType"));
		ProviderType pt = ProviderType.valueOf(StringUtil.checkVal(req.getParameter("pt"), "GOOGLE").toUpperCase());

		// Get the list of columns and convert to integer list
		List<Integer> columns = new ArrayList<>();
		if (! StringUtil.isEmpty(req.getParameter("columns"))) {
			List<String> sCols = Arrays.asList(req.getParameter("columns").split("\\,"));
			columns = sCols.stream().map(Convert::formatInteger).collect(Collectors.toList());
		}
		// Get the list of rows and convert to integer list
		List<Integer> rows = new ArrayList<>();
		if (! StringUtil.isEmpty(req.getParameter("rows"))) {
			List<String> sCols = Arrays.asList(req.getParameter("rows").split("\\,"));
			rows = sCols.stream().map(Convert::formatInteger).collect(Collectors.toList());
		}

		// Process the data
		if (grids != null && grids.length > 0) {
			putModuleData(loadAllGrids(grids, full, stacked, pt));
		} else {
			GridVO grid = loadSingleGrid(type, gridId, req);
			
			if (req.hasParameter("excel")) {
				buildExcelFile(req, grid);
			} else if (! StringUtil.isEmpty(gridId)) { 
				putModuleData(retrieveChartData(grid, type, full, stacked, pt, columns, rows, labelType));
			}
		}
	}
	
	
	/**
	 * Load the grid with the supplied id.
	 * @param type
	 * @param gridId
	 * @param req
	 * @return
	 */
	private GridVO loadSingleGrid(ChartType type, String gridId, ActionRequest req ) {
		boolean display = Convert.formatBoolean(req.getParameter("display"));
		if (display && ChartType.TABLE == type) display = false;
		GridVO grid = getGridData(gridId, display);
		
		// If this grid has legacy data load that instead.
		if (!StringUtil.isEmpty(grid.getLegacyId()) && ChartType.TABLE == type) 
			grid = getGridData(grid.getLegacyId(), display);
		
		return grid;
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

		List<Object> params =  Arrays.asList(new Object[]{req.getParameter(GRID_ID),req.getParameter(GRID_ID) });
		DBProcessor dbp = new DBProcessor(getDBConnection());
		List<Object> data = dbp.executeSelect(sql.toString(), params, new GridVO());

		if (! data.isEmpty()) {
			GridVO grid = (GridVO) data.get(0);
			log.debug("transposing reqParam gridId from " + req.getParameter(GRID_ID) + " to " + grid.getGridId());
			req.setParameter(GRID_ID, grid.getGridId());
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

			if (!ChartType.TABLE.equals(ct)) {
				pruneColumns(grid);
			}

			if (columns.length() > 0) {
				List<String> sCols = Arrays.asList(columns.split("\\,"));
				cols = sCols.stream().map(Convert::formatInteger).collect(Collectors.toList());
			}

			// Retrieve the data for all of the charts
			data.put(id, retrieveChartData(grid, ct, full, stacked, pt, cols, Collections.emptyList(), ALL_LABELS));
		}

		return data;
	}


	/**
	 * The retrieve all grids method does not differentiate between grid types. 
	 * Here all details that do not belong in a non-table display are removed.
	 * @param grid
	 */
	private void pruneColumns(GridVO grid) {
		List<GridDetailVO> details = new ArrayList<>();
		for (GridDetailVO detail : grid.getDetails()) {
			if (StringUtil.isEmpty(detail.getDetailType()) || "DATA".equals(detail.getDetailType()))
				details.add(detail);
		}
		grid.setDetails(details);
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
	public SMTGridIntfc retrieveChartData(GridVO grid, ChartType type, boolean full, 
			boolean stacked, ProviderType pt, List<Integer> cols, List<Integer> rows, int labelType) {
		SMTGridIntfc gridData = SMTChartFactory.getInstance(pt, grid, type, full, cols, rows);

		// Get the chart options
		SMTChartOptionIntfc options = SMTChartOptionFactory.getInstance(type, ProviderType.GOOGLE, full);
		options.addOptionsFromGridData(grid);
		log.debug("options: " + options);

		// Load the custom pie chart colors.
		if (ChartType.PIE == type) {
			options.getChartOptions().put("colors", PIE_CHART_COLORS.clone());
		}

		// Load company specific colors
		setColors(grid, options);
		
		if (labelType > ALL_LABELS) {
			modifyLabels(gridData, options, labelType);
		}

		// Pie charts need to have their labels modified in order to
		// get all pertinant information to the user.
		// Since this modifies labels it needs to be done after the colors have been set.
		if (ChartType.PIE == type && labelType < TOTAL_ONLY) {
			modifyPieLabels(grid);
		}

		// Add the chart specific options
		gridData.addCustomValues(options.getChartOptions());
		gridData.addCustomValue("width", "100%");
		gridData.addCustomValue("height", "100%");

		// Add configurable attributes
		if(stacked) gridData.addCustomValue("isStacked", true);

		if (Convert.formatBoolean(grid.getAbbreviateNumbers())) setValueFormats(gridData);

		return gridData;
	}


	/**
	 * Remove labels from the graph based on its label type.
	 * @param gridData
	 * @param options
	 * @param labelType
	 */
	private void modifyLabels(SMTGridIntfc gridData, SMTChartOptionIntfc options, int labelType) {
		
		if (labelType == VALUE_ONLY || labelType == NO_LABEL) {
			removeAnnotations(gridData);
		}
		
		if (labelType == TOTAL_ONLY || labelType == NO_LABEL) {
			options.getChartOptions().put("legend", "none");
		}
	}

	
	/**
	 * Find and remove any annotation columns
	 * @param gridData
	 */
	private void removeAnnotations(SMTGridIntfc gridData) {
		List<Integer> annotations = new ArrayList<>();
		for (int i = 0; i < gridData.getCols().size(); i++) {
			GoogleChartColumnVO col = (GoogleChartColumnVO) gridData.getCols().get(i);
			if ("annotation".equals(col.getRole()))
				annotations.add(i);
		}
		
		// Sort into descending order to prevent items from shifting with removal.
		Collections.sort(annotations, Collections.reverseOrder());
		
		for (Integer i : annotations) {
			gridData.getCols().remove(i.intValue());
			for (SMTGridRowIntfc row : gridData.getRows()) {
				GoogleChartRowVO gRow = (GoogleChartRowVO) row;
				gRow.getC().remove(i.intValue());
			}
		}
	}

	/**
	 * Set the colors based on companies.
	 * @param grid
	 * @param options
	 */
	private void setColors(GridVO grid, SMTChartOptionIntfc options) {
		int size = grid != null && grid.getDetails() != null ? grid.getDetails().size() : 0;
		if (size == 0) return; //nothing to do - query won't match any data.
		
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(200);
		sql.append("select distinct company_nm, alias_nm, short_nm_txt, graph_color from ").append(customDb).append("biomedgps_company ");
		sql.append("where company_nm in (");
		DBUtil.preparedStatmentQuestion(size, sql);
		sql.append(") or alias_nm in (");
		DBUtil.preparedStatmentQuestion(size, sql);
		sql.append(") or short_nm_txt in (");
		DBUtil.preparedStatmentQuestion(size, sql);
		sql.append(")");

		int i = 1;
		Map<String, String> colorMap = new HashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (GridDetailVO detail : grid.getDetails()) {
				// Each value is used in three seperate in blocks
				ps.setString(i + size*2, detail.getLabel());
				ps.setString(i + size, detail.getLabel());
				ps.setString(i++, detail.getLabel());
			}

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String color = rs.getString("graph_color");
				if (!StringUtil.isEmpty(color)) {
					colorMap.put(rs.getString("company_nm"), color);
					colorMap.put(rs.getString("alias_nm"), color);
					colorMap.put(rs.getString("short_nm_txt"), color);
				}
			}
		} catch (SQLException e) {
			log.error("Failed to set custom colors. Retaining standard colors", e);
		}

		//update the colors...by reference?  It's a String[] - not sure this code does what it suggestions, if anything.  -JM- 01.23.2018
		String[] colors = (String[]) options.getChartOptions().get("colors");
		for (int j=0; j < size; j++) {
			String name = grid.getDetails().get(j).getLabel();
			if (StringUtil.isEmpty(name) || !colorMap.containsKey(name)) continue;
			colors[j] = colorMap.get(name);
		}
	}


	/**
	 * Fromat the value displays
	 * @param gridData
	 */
	private void setValueFormats(SMTGridIntfc gridData) {
		GoogleChartColumnVO col = new GoogleChartColumnVO();
		col.setRole("annotation");
		col.setDataType(DataType.STRING);
		gridData.addColumn(col);

		for (SMTGridRowIntfc row : gridData.getRows()) {
			GoogleChartRowVO gRow = (GoogleChartRowVO) row;
			for (int i=1; i < gRow.getC().size(); i++) {
				GoogleChartCellVO c = gRow.getC().get(i);
				c.setFormat(formatCellValue(StringUtil.checkVal(c.getValue())));
			}
		}
	}


	/**
	 * Shorten the supplied value to a single decimal dollar amount
	 * appended with a shorthand character for the magnitude.
	 * @param value
	 * @return
	 */
	private String formatCellValue(String value) {
		if (value.length() <= 3) return value;
		// Decimal and 0 added by the system
		if (value.indexOf('.') > -1)
			value = value.substring(0, value.indexOf('.'));
		String suffix = "";
		int pos = value.length()%3;
		if (pos == 0) pos = 3;
		switch ((int)Math.ceil((double)value.length()/3)) {
			case 2:
				suffix = " K";
				break;
			case 3:
				suffix = " M";
				break;
			case 4:
				suffix = " B";
				break;
			case 5:
				suffix = " T";
				break;
			default:
				suffix = "";
		}

		StringBuilder formatted = new StringBuilder(pos + 4);

		formatted.append("$").append(value.substring(0, pos)).append(".");
		formatted.append(value.charAt(pos)).append(suffix);

		return formatted.toString();
	}


	/**
	 * Check to see if the label needs to be modified
	 * and do so if necessary.
	 */
	private void modifyPieLabels(GridVO grid) {
		// Add up all values to see if the chart was generated
		// with percentages instead of actual values.
		BigDecimal total = new BigDecimal(0);
		for (GridDetailVO detail : grid.getDetails()) {
			total = total.add(new BigDecimal(Convert.formatDouble(detail.getValue1())));
		}

		// If the total is 100 the percentage is functionally 
		// the same as the value and appending it to the 
		// label will result in needless duplication of data.
		if (total.compareTo(new BigDecimal(100)) == 0) return;

		boolean format =  Convert.formatBoolean(grid.getAbbreviateNumbers());
		for (GridDetailVO detail : grid.getDetails()) {
			if (format) {
				detail.setLabel(detail.getLabel() + " - " + formatCellValue(StringUtil.removeNonNumeric(detail.getValue1())));
			} else {
				detail.setLabel(detail.getLabel() + " - " + detail.getValue1());
			}
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

		gca.retrieveData(gridId, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA), display);
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);

		return (GridVO) mod.getActionData();
	}

}