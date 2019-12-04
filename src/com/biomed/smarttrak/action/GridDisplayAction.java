package com.biomed.smarttrak.action;

import java.math.BigDecimal;
// JDK 1.8
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// App Libs
import com.biomed.smarttrak.admin.GridChartAction;
import com.biomed.smarttrak.admin.vo.GridDetailVO;
import com.biomed.smarttrak.admin.vo.GridVO;
import com.biomed.smarttrak.vo.grid.BiomedExcelReport;
// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.report.chart.SMTChartDetailVO;
import com.siliconmtn.data.report.chart.SMTChartFactory;
import com.siliconmtn.data.report.chart.SMTChartFactory.ProviderType;
import com.siliconmtn.data.report.chart.SMTChartIntfc;
import com.siliconmtn.data.report.chart.SMTChartOptionFactory;
import com.siliconmtn.data.report.chart.SMTChartOptionFactory.ChartType;
import com.siliconmtn.data.report.chart.SMTChartOptionIntfc;
import com.siliconmtn.data.report.chart.SMTChartVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
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
	public static final int ALL_LABELS = 0;
	public static final int VALUE_ONLY = 1;
	public static final int TOTAL_ONLY = 2;
	public static final int NO_LABEL = 3;

	private static final String LOAD_TABLE = "loadTable";
	private static final String ENABLED = "enabled";
	
	
	enum ColorTheme {
		COL_1(Collections.unmodifiableList(Arrays.asList("#5EBCD2"))),
		COL_2(Collections.unmodifiableList(Arrays.asList("#85CBCF", "#3984B6"))),
		COL_3(Collections.unmodifiableList(Arrays.asList("#85CBCF", "#3984B6", "#1D2E81"))),
		COL_4(Collections.unmodifiableList(Arrays.asList("#9ED5CD", "#44A7CB", "#2E62A1", "#192574"))),
		COL_5(Collections.unmodifiableList(Arrays.asList("#B7DFCB", "#5ABAD1", "#3984B6", "#264992", "#161F63"))),
		COL_6(Collections.unmodifiableList(Arrays.asList("#B7DFCB", "#70C3D0", "#419DC5", "#316BA7", "#223B89", "#151E5E"))),
		COL_7(Collections.unmodifiableList(Arrays.asList("#C6E3CB", "#83CACF", "#47AED0", "#3984B6", "#2C5A9C", "#1E3082", "#141C59"))),
		COL_8(Collections.unmodifiableList(Arrays.asList("#CEE6CA", "#91D0CE", "#56B9D2", "#3F97C2", "#3371AA", "#274B93", "#1B277C", "#131A55"))),
		COL_9(Collections.unmodifiableList(Arrays.asList("#D5E9CA", "#9ED5CD", "#69C0D1", "#44A7CB", "#3984B6", "#2E62A1", "#233F8C", "#192473", "#121850"))),
		COL_10(Collections.unmodifiableList(Arrays.asList("#DCECC9", "#AADACC", "#78C6D0", "#48B3D3", "#3E94C0", "#3474AC", "#2A5599", "#203686", "#18216B", "#11174B"))),
		COL_11(Collections.unmodifiableList(Arrays.asList("#DCECC9", "#AFDCCC", "#83CACF", "#56B9D2", "#43A1C7", "#3984B6", "#3067A5", "#274B93", "#1E3082", "#172068", "#11174B"))),
		COL_12(Collections.unmodifiableList(Arrays.asList("#DCECC9", "#B3DDCC", "#8ACDCE", "#62BED2", "#46AACE", "#3D91BE", "#3577AE", "#2D5E9E", "#24448E", "#1C2B7F", "#162065", "#11174B")));
		
		private List<String> colorPallet;
		
		ColorTheme(List<String> colorPallet) {
			this.colorPallet = colorPallet;
		}
		
		public List<String> getPallet() {
			return colorPallet;
		}
	}
	
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
		
		// Tables need to be handles specially. Default to a column type and set loadTable to ensure proper rendering
		if (type == ChartType.TABLE) {
			req.setParameter(LOAD_TABLE, "true");
			type = ChartType.COLUMN;
		}
		boolean loadTable = Convert.formatBoolean(req.getParameter(LOAD_TABLE));
		// Check to see if there is a mapping for the grid when displaying a table
		if (loadTable) lookupTableMap(req);

		// Get the request data
		String gridId = req.getParameter(GRID_ID);
		String[] grids = req.getParameterValues("grid");
		boolean full = Convert.formatBoolean(req.getParameter("full")) || loadTable;
		boolean stacked = Convert.formatBoolean(req.getParameter("isStacked"), false);
		int labelType = Convert.formatInteger(req.getParameter("labelType"));
		ProviderType pt = ProviderType.valueOf(StringUtil.checkVal(req.getParameter("pt"), "HIGH_CHARTS").toUpperCase());

		// Get the list of columns and convert to integer list
		List<Integer> columns = new ArrayList<>();
		if (! StringUtil.isEmpty(req.getParameter("columns"))) {
			List<String> sCols = Arrays.asList(req.getParameter("columns").split("\\,"));
			columns = sCols.stream().map(Convert::formatInteger).collect(Collectors.toList());
		}

		// Process the data
		if (grids != null && grids.length > 0) {
			putModuleData(loadAllGrids(grids, full, stacked, pt));
		} else {
			GridVO grid = loadSingleGrid(gridId, req);
			
			if (req.hasParameter("excel")) {
				buildExcelFile(req, grid);
			} else if (! StringUtil.isEmpty(gridId)) { 
				SMTChartVO chart = convertChart(grid, type, columns);
				SMTChartOptionIntfc options = buildOptions(chart, type, pt, full, labelType, grid.getAbbreviateNumbers(), Convert.formatBoolean(req.getParameter(LOAD_TABLE)));
				addDetailTypes(options, grid);
				SMTChartIntfc gridData = retrieveChartData(chart, type, stacked, pt, options);
				
				putModuleData(gridData);
			}
		}
	}
	
	
	private void addDetailTypes(SMTChartOptionIntfc options, GridVO grid) {
		Map<String, Object> additionalDetails = loadParamMap("additionalOptions", options.getChartOptions());
		List<String> rowDetails = new ArrayList<>();
		for (GridDetailVO gDetail : grid.getDetails()) {
			rowDetails.add(gDetail.getDetailType());
		}
		additionalDetails.put("rowDetails", rowDetails);
	}

	/**
	 * Convert the Biomed grid to a standard SMT Chart.
	 * @param grid
	 * @param type
	 * @param columns
	 * @param stacked
	 * @return
	 */
	private SMTChartVO convertChart(GridVO grid, ChartType type, List<Integer> columns) {
		SMTChartVO chart = new SMTChartVO();
		chart.setChartId(grid.getGridId());
		chart.setCreateDate(grid.getCreateDate());
		chart.setDecimalDisplay(grid.getDecimalDisplay());
		chart.setDisclaimer(grid.getDisclaimer());
		chart.setPrimaryXTitle(grid.getPrimaryXTitle());
		chart.setPrimaryYTitle(grid.getPrimaryYTitle());
		chart.setSecondaryYTitle(grid.getSecondaryYTitle());
		chart.setSeriesLabel(grid.getSeriesLabel());
		chart.setSubtitle(grid.getSubtitle());
		chart.setTitle(grid.getTitle());
		chart.setUpdateDate(grid.getUpdateDate());

		List<SMTChartDetailVO> data;
		if (ChartType.COLUMN == type) {
			data = convertColumnData(grid, columns);
		} else {
			data = convertChartData(grid, columns);
		}
		
		chart.processData(data, true);
		
		return chart;
	}
	
	
	/**
	 * Create the data for a column chart
	 * @param grid
	 * @param type
	 * @param columns
	 * @return
	 */
	private List<SMTChartDetailVO> convertColumnData(GridVO grid, List<Integer> columns) {
		List<SMTChartDetailVO> data = new ArrayList<>(grid.getDetails().size());
		List<String> series = new ArrayList<>(grid.getDetails().size());
		for (GridDetailVO gDetail : grid.getDetails()) {
			// Each row needs to determine names seperately
			List<String> names = new ArrayList<>();
			String serie = getSerie(gDetail, series);
			for (int i=0; i<grid.getSeries().length; i++) {
				if (!columns.isEmpty() && !columns.contains(i+1) ||
						grid.getSeries()[i] == null) continue;
				String name = grid.getSeries()[i];
				if (names.contains(name))
					name = findNewName(names, name);
				names.add(name);
				
				addDetail(gDetail, data, i, grid, serie, name);
			}
		}
		return data;
	}

	
	/**
	 * Create the data for a normal chart
	 * @param grid
	 * @param type
	 * @param columns
	 * @return
	 */
	private List<SMTChartDetailVO> convertChartData(GridVO grid, List<Integer> columns) {
		List<SMTChartDetailVO> data = new ArrayList<>(grid.getDetails().size());
		List<String> names = new ArrayList<>();
		
		for (GridDetailVO gDetail : grid.getDetails()) {
			String name = gDetail.getLabel();
			if (names.contains(name))
				name = findNewName(names, name);
			names.add(name);
			List<String> series = new ArrayList<>();
			for (int i=0; i<grid.getSeries().length; i++) {
				if (!columns.isEmpty() && !columns.contains(i+1)
						|| grid.getSeries()[i] == null) continue;
				String serie = grid.getSeries()[i];
				if (series.contains(serie))
					serie = findNewName(series, serie);
				series.add(serie);
				addDetail(gDetail, data, i, grid, serie, name);
			}
		}
		
		return data;
	}

	/**
	 * Ensure that the current column name doesn't already exist.
	 * @param names
	 * @param name
	 * @return
	 */
	private String findNewName(List<String> names, String name) {
		StringBuilder newName = new StringBuilder(StringUtil.checkVal(name));
		while (names.contains(newName.toString()))
			newName.append(" ");
		return newName.toString();
	}

	/**
	 * Create a unique series name for the row to prevent rows from being overwitten
	 * by items with the same serie.
	 * @param gDetail
	 * @param series
	 * @return
	 */
	private String getSerie(GridDetailVO gDetail, List<String> series) {
		StringBuilder serie = new StringBuilder(gDetail.getLabel());
		while(series.contains(serie.toString())) {
			serie.append(" ");
		}
		series.add(serie.toString());
		return serie.toString();
	}

	/**
	 * Add column/row information to the list.
	 * @param gDetail
	 * @param data
	 * @param type
	 * @param i
	 * @param grid
	 * @param series 
	 * @param stacked
	 */
	private void addDetail(GridDetailVO gDetail, List<SMTChartDetailVO> data, int i, GridVO grid, String serie, String name) {
		String value;
		if (i>=gDetail.getValues().length || (StringUtil.isEmpty(grid.getSeries()[i]) 
				&& StringUtil.isEmpty(gDetail.getValues()[i]))) {
			value = "";
		} else {
			value = gDetail.getValues()[i];
		}
		
		SMTChartDetailVO cDetail = new SMTChartDetailVO();
		cDetail.setLabel(name);
		cDetail.setSerie(serie);
		cDetail.setOrder(gDetail.getOrder());
		cDetail.setValue(value);
		data.add(cDetail);
	}

	/**
	 * Load the grid with the supplied id.
	 * @param type
	 * @param gridId
	 * @param req
	 * @return
	 */
	private GridVO loadSingleGrid(String gridId, ActionRequest req ) {
		boolean display = Convert.formatBoolean(req.getParameter("display"));
		if (display && Convert.formatBoolean(req.getParameter(LOAD_TABLE))) display = false;
		GridVO grid = getGridData(gridId, display);
		
		if (req.hasParameter("customTitle"))
			grid.setTitle(req.getParameter("customTitle"));
		
		// If this grid has legacy data load that instead.
		if (!StringUtil.isEmpty(grid.getLegacyId()) && Convert.formatBoolean(req.getParameter(LOAD_TABLE))) 
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
	public Map<String, SMTChartIntfc> loadAllGrids(String[] grids, boolean full, boolean stacked, ProviderType pt) {
		Map<String, SMTChartIntfc> data = new HashMap<>(24);
		String schema = getAttribute(Constants.CUSTOM_DB_SCHEMA) + "";

		// Parse the map data and place in a map
		Map<Object, Map<String, String>> items = new HashMap<>(grids.length);
		for(String grid : grids) {
			String[] vals = grid.split("\\|");
			Map<String, String> values = new HashMap<>(3);
			
			values.put("type", vals[1]);
			if (vals.length >= 3) values.put("columns", vals[2]);
			if (vals.length >= 4) values.put("labelType", vals[3]);
			if (vals.length >= 5) values.put("customTitle", vals[4]);
			items.put(vals[0], values);
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
			if (!StringUtil.isEmpty(items.get(id).get("customTitle")))
				grid.setTitle(items.get(id).get("customTitle")); 

			// Parse pout the passed in data and format for calling each chart
			ChartType ct = ChartType.valueOf(items.get(id).get("type") + "");
			String columns = StringUtil.checkVal(items.get(id).get("columns"));
			int labelType = Convert.formatInteger(items.get(id).get("labelType"));
			boolean loadTable = ct == ChartType.TABLE;

			if (!loadTable) {
				pruneColumns(grid);
			}

			if (columns.length() > 0) {
				List<String> sCols = Arrays.asList(columns.split("\\,"));
				cols = sCols.stream().map(Convert::formatInteger).collect(Collectors.toList());
			}
			
			SMTChartVO chart = convertChart(grid, ct, cols);

			// Retrieve the data for all of the charts
			SMTChartOptionIntfc options = buildOptions(chart, ct, pt, full, labelType, grid.getAbbreviateNumbers(), loadTable);
			addDetailTypes(options, grid);
			SMTChartIntfc loadedGrid = retrieveChartData(chart, ct, stacked, pt, options);
			addDetailTypes(options, grid);
			data.put(id, loadedGrid);
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
	 * @param labelType 
	 * @param i 
	 */
	public SMTChartIntfc retrieveChartData(SMTChartVO grid, ChartType type, boolean stacked, ProviderType pt, SMTChartOptionIntfc options) {
		SMTChartIntfc theChart = SMTChartFactory.getInstance(pt);
		
		theChart.addCustomValues(options.getChartOptions());
		
		// Add configurable attributes
		if(stacked) {
			Map<String, Object> plotOptions = loadParamMap("plotOptions", theChart.getCustomValue());
			Map<String, Object> columnOptions = loadParamMap("column", plotOptions);
			columnOptions.put("stacking", "normal");
		}
		
		theChart.processData(grid, type);
		return theChart;
	}
	
	private SMTChartOptionIntfc buildOptions(SMTChartVO grid, ChartType type, ProviderType pt, boolean full, int labelType, int abbreviateFlg, boolean loadTable) {

		SMTChartOptionIntfc options = SMTChartOptionFactory.getInstance(type, pt, full);


		if (ChartType.PIE != type) {
			options.getChartOptions().put("colors", loadColors(grid.getSeries().keySet().size()).toArray());
		} else {
			int count = 0;
			for (Map<String, SMTChartDetailVO> detail :  grid.getSeries().values())
				count += detail.size();
			options.getChartOptions().put("colors", loadColors(count).toArray());
		}
		
		options.addOptionsFromGridData(grid);
		
		Map<String, Object> additionalOptions = new HashMap<>(6);

		additionalOptions.put("labelType", labelType);
		additionalOptions.put("abbreviateFlg", Convert.formatBoolean(abbreviateFlg));
		additionalOptions.put("seriesLabel", grid.getSeriesLabel());
		additionalOptions.put(LOAD_TABLE, loadTable);
		String prefix = determinePrefix(grid.getSeries());
		additionalOptions.put("prefix", prefix);
		additionalOptions.put("disclaimer", grid.getDisclaimer());
		
		if (type == ChartType.PIE) {
			additionalOptions.put("modifyPieLabels", modifyPieLabels(grid));
		}
		
		if (full) {
			Map<String, Object> yAxis = loadParamMap("yAxis", options.getChartOptions());
			Map<String, Object> stackLabels = new HashMap<>();
			stackLabels.put(ENABLED, true);
			stackLabels.put("format", prefix +"{total:,.0f}");
			stackLabels.put("prefix", prefix);
			yAxis.put("stackLabels", stackLabels);
		}
		
		options.getChartOptions().put("additionalOptions", additionalOptions);
		
		Map<String, Object> legend = new HashMap<>(1);
		if (full && ChartType.COLUMN == type) {
			legend.put(ENABLED, true);
		} else {
			legend.put(ENABLED, false);
		}
		
		options.getChartOptions().put("legend", legend);

		loadParamMap("credits", options.getChartOptions()).put("enabled", false);
		
		return options;
	}

	
	/**
	 * check for a Euro or Dollar prefix to use in stacked totals
	 * @param series
	 * @return
	 */
	private String determinePrefix(Map<String, Map<String, SMTChartDetailVO>> series) {
		
		for (Map<String, SMTChartDetailVO> serie : series.values()) {
			for (SMTChartDetailVO detail : serie.values()) {
				if (StringUtil.isEmpty(detail.getValue()) ||
						Character.isDigit(detail.getValue().charAt(0))) continue;
				char c = detail.getValue().charAt(0);
				if (c == '€' || c == '$') return StringUtil.checkVal(c);
			}
		}
		
		return "";
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> loadParamMap(String key, Map<String, Object> container) {
		Map<String, Object> loadedMap = (Map<String, Object>) container.get(key);
		if (loadedMap == null) {
			loadedMap = new HashMap<>();
			container.put(key, loadedMap);
		}
		return loadedMap;
	}
	
	private List<String> loadColors(int count) {
		if (count > 12) count = 12;
		if (count == 0) count = 1;
		String palletName = "COL_" + count;
		ColorTheme pallet = ColorTheme.valueOf(palletName);
		return pallet.getPallet();
	}


	/**
	 * Remove labels from the graph based on its label type.
	 * @param gridData
	 * @param options
	 * @param labelType
	
	private void modifyLabels(SMTChartIntfc gridData, SMTChartOptionIntfc options, int labelType) {
		
		if (labelType == VALUE_ONLY || labelType == NO_LABEL) {
			removeAnnotations(gridData);
		}
		
		if (labelType == TOTAL_ONLY || labelType == NO_LABEL) {
			options.getChartOptions().put("legend", "none");
		}
	} */

	
	/**
	 * Find and remove any annotation columns
	 * @param gridData
	 
	private void removeAnnotations(SMTChartIntfc gridData) {
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
				if (i.intValue() < gRow.getC().size())
					gRow.getC().remove(i.intValue());
			}
		}
	}*/

	
	// This may be upgraded and added in a later feature. For now it can be ignored.
	/**
	 * Set the colors based on companies.
	 * @param grid
	 * @param options
	
	private void setColors(SMTChartVO grid, SMTChartOptionIntfc options) {
		int size = grid != null && grid.getSeries() != null ? grid.getSeries().size() : 0;
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

		String[] colors = (String[]) options.getChartOptions().get("colors");
		List<String> colorList = new ArrayList<>(Arrays.asList(colors));
		
		// Loop over the columns, getting either a default color or a company color for each one.
		for (int j=0; j < size; j++) {
			String name = grid.getDetails().get(j).getLabel();
			if (StringUtil.isEmpty(name) || !colorMap.containsKey(name)) {
				colorList.add(j, colors[j%colors.length]);
			} else {
				colorList.add(j, colorMap.get(name));
			}
		}
		
		// Place the new color set in the conig.
		 options.getChartOptions().put("colors", colorList.toArray());
	} */


	/**
	 * Check to see if the label needs to be modified
	 * and do so if necessary.
	 * @param grid 
	 * @param gridData 
	 */
	private boolean modifyPieLabels(SMTChartVO chart) {
		// Add up all values to see if the chart was generated
		// with percentages instead of actual values.
		BigDecimal total = new BigDecimal(0);
		for (Map<String, SMTChartDetailVO> series : chart.getSeries().values()) {
			for (SMTChartDetailVO detail : series.values()) {
				String numeric = StringUtil.removeNonNumericExceptDecimal(detail.getValue());
				if (StringUtil.isEmpty(numeric)) continue;
				total = total.add(new BigDecimal(numeric));
			}
		}

		// If the total is 100 the percentage is functionally 
		// the same as the value and appending it to the 
		// label will result in needless duplication of data.
		return total.compareTo(new BigDecimal(100)) == 0;
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

		return gca.retrieveData(gridId, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA), display);
	}

}