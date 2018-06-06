package com.biomed.smarttrak.vo.grid;

// JDK 1.8.x
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Log4j 1.2.17
import org.apache.log4j.Logger;

// App Libs
import com.biomed.smarttrak.admin.vo.GridDetailVO;
import com.biomed.smarttrak.admin.vo.GridVO;
import com.biomed.smarttrak.admin.vo.GridVO.RowStyle;
import com.biomed.smarttrak.vo.grid.SMTChartOptionFactory.ChartType;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

// SMT Base Libs
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/********************************************************************
 * <b>Title: </b>GoogleChartVO.java<br/>
 * <b>Description: </b>Hierarchical Structure for the GoogleCharts JSON Data<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Feb 28, 2017
 * Last Updated:
 * 	
 *******************************************************************/
public class GoogleChartVO implements Serializable, SMTGridIntfc {

	private static final long serialVersionUID = 1L;
	protected static final Logger log = Logger.getLogger(GoogleChartVO.class);
	private static final String COLUMN_NAME = "Column_";

	/**
	 * Allowed datatypes for a column field
	 */
	public enum DataType {
		BOOLEAN("boolean"), NUMBER("number"), STRING("string"), 
		DATE("date"), DATETIME("datetime"), TIMEOFDAY("timeofday");

		// Assign the name (get and set)
		private final String name;
		private DataType(String name) {	this.name = name; }
		public String getName() { return name; }
	}

	/**
	 * Collection of column objects for the charts
	 */
	@SerializedName("cols")
	private List<SMTGridColumnIntfc> columns;

	/**
	 * Collection of Row Objects
	 */
	@SerializedName("rows")
	private List<SMTGridRowIntfc> rows;

	/**
	 * Chart options, styles and Javascript elements
	 */
	@SerializedName("p")
	@Expose
	private final Map<String, Object> options;

	@SerializedName("disclaimer")
	private String disclaimer;


	private GoogleChartVO() {
		super();
		columns = new ArrayList<>(10);
		rows = new ArrayList<>(64);
		options = new LinkedHashMap<>(32);
	}

	/**
	 * Creates the chart vo by populating it with data from the grid
	 * @param grid
	 */
	public GoogleChartVO(GridVO grid, ChartType type, boolean full, List<Integer> columns, List<Integer> rows) {
		this();
		setDisclaimer(grid.getDisclaimer());
		// Different charts require that the data be processed differently
		switch(type) {
			case PIE:
			case DONUT:
			case GEO:
			case TABLE:
				processGridPie(grid, columns, rows);
				break;
			default:
				processGridBar(grid, full, columns);
		}
	}

	/**
	 * Converts the grid data into a google grid object.
	 * NOTE: fCols and fRows are transposed from the above invocation - rows one way is columns the other.
	 * @param grid Grid data
	 */
	public void processGridBar(GridVO grid, boolean full, List<Integer> fCols) {
		String[] series = grid.getSeries();
		log.debug("fCols = " + fCols);

		// Get the row data that corresponds to the series
		List<GridDetailVO> details = grid.getDetails();

		// Loop the rows and and the data
		//TODO "10" here is a binding specific to SmartTRAK (DB Schema).  Mind this trap when porting code to BaseLibs.
		for (int i=0; i < 10; i++) {
			GoogleChartRowVO row = new GoogleChartRowVO();

			// Add the label cell data
			GoogleChartCellVO cell = new GoogleChartCellVO();
			cell.setValue(series[i]);
			row.addCell(cell);

			double total = 0;
			String formatter = null;
			// Loop the rows
			for (int x=0; x < details.size(); x++) {
				GridDetailVO detail = details.get(x);
				if (StringUtil.isEmpty(detail.getValues()[i])) continue;
				cell = new GoogleChartCellVO();

				// Store the data as a double and as a formatted string
				cell.setValue(Convert.formatDouble(detail.getValues()[i], 0.0, true));
				cell.setFormat(detail.getValues()[i]);
				total += (Double)cell.getValue();

				if (formatter == null && cell.getFormat().contains("$")) formatter = "$";
				else if (formatter == null && cell.getFormat().contains("%")) formatter = "%";

				// Determine if there is a columns filter and apply
				if (fCols.isEmpty() || fCols.contains(i + 1))
					row.addCell(cell);
			}

			// Only add annotations if the col was supposed to be displayed and it is a full size image
			if (full && row.getC().size() > 1) {
				cell = new GoogleChartCellVO();

				// Format the data 
				DecimalFormat df = new DecimalFormat();
				StringBuilder val = new StringBuilder(df.format(total));
				if ("$".equals(formatter)) val.insert(0, "$");
				else if ("%".equals(formatter)) val.append("%");

				cell.setFormat(val.toString());
				cell.setValue(total);
				row.addCell(cell);
			}

			// Make sure there is data in the elements
			if (row.getC().size() > 1)
				addRow(row);
		}

		// Get the column labels first
		int val = (int) 'A';
		GoogleChartColumnVO col = new GoogleChartColumnVO();
		col.setId(COLUMN_NAME + ((char) val++));
		col.setDataType(DataType.STRING.getName());
		col.setLabel(StringUtil.checkVal(grid.getSeriesLabel()));
		addColumn(col);

		// Get the column data
		for (int i=0; i < details.size(); i++) {
			GridDetailVO detail = details.get(i);
			if (detail == null) continue;
			col = new GoogleChartColumnVO();
			col.setId(COLUMN_NAME + ((char) val++));
			col.setDataType(DataType.NUMBER.getName());
			col.setLabel(detail.getLabel());

			// Determine if there is a columns filter and apply
			addColumn(col);
		}

		col = new GoogleChartColumnVO();
		col.setDataType(DataType.STRING.getName());
		col.setRole("annotation");
		addColumn(col);
	}


	/**
	 * Converts the grid data into a google grid object
	 * @param grid Grid data
	 */
	public void processGridPie(GridVO grid, List<Integer> fCols, List<Integer> fRows) {
		String[] series = grid.getSeries();
		log.debug("fCols = " + fCols);
		log.debug("fRows = " + fRows);

		// Get the row data that corresponds to the series
		int rowCnt = 0;
		for(GridDetailVO detail : grid.getDetails()) {
			GoogleChartRowVO row = new GoogleChartRowVO();

			// Add the label cell data
			GoogleChartCellVO cell = new GoogleChartCellVO();
			cell.setValue(detail.getLabel());

			if (!StringUtil.isEmpty(detail.getDetailType())) {
				cell.addCustomValue("className", RowStyle.valueOf(detail.getDetailType()).getName());
			}

			row.addCell(cell);

			addCellsToRow(row, detail, grid, fCols);

			if (fRows.isEmpty() || fRows.contains(rowCnt))
				addRow(row);

			log.debug(" row=" + row);
			++rowCnt;
		}

		// Get the column labels first
		int val = (int) 'A';
		GoogleChartColumnVO col = new GoogleChartColumnVO();
		col.setId(COLUMN_NAME + ((char) val++));
		col.setDataType(DataType.STRING.getName());
		col.setLabel(grid.getSeriesLabel());
		addColumn(col);

		// Get the column data
		for (int j=0; j < grid.getNumberColumns(); j++) {
			col = new GoogleChartColumnVO();
			col.setId(COLUMN_NAME + ((char) val++));
			col.setDataType(DataType.NUMBER.getName());
			col.setLabel(series[j]);

			// Determine if there is a columns filter and apply
			if (fCols.isEmpty() || fCols.contains(j + 1))
				addColumn(col);
		}
	}


	/**
	 * Loop based on data in columns.  Since first row is label, loop one less row
	 * @param detail
	 * @param grid
	 * @param fCols
	 */
	private void addCellsToRow(GoogleChartRowVO row, GridDetailVO detail, GridVO grid, List<Integer> fCols) {
		String[] values = detail.getValues();
		for (int i=0; i < grid.getNumberColumns(); i++) {
			String value = values[i];
			GoogleChartCellVO cell = new GoogleChartCellVO();

			if (StringUtil.isEmpty(value)) {
				cell.setValue(null);
			} else {
				cell.setValue(Convert.formatDouble(detail.getValues()[i]));
				cell.setFormat(detail.getValues()[i]);
			}

			if (!StringUtil.isEmpty(detail.getDetailType()))
				cell.addCustomValue("className", RowStyle.valueOf(detail.getDetailType()).getName());

			// Determine if there is a columns filter and apply
			if (fCols.isEmpty() || fCols.contains(i + 1))
				row.addCell(cell);
		}
	}

	/**
	 * Adds a column to the data set
	 * @param column
	 */
	public void addColumn(SMTGridColumnIntfc column) {
		columns.add(column);
	}

	/**
	 * Adds a set of row data to the chart
	 * @param row
	 */
	public void addRow(SMTGridRowIntfc row) {
		rows.add(row);
	}

	/**
	 * @param options the p to set
	 */
	public void addCustomValue(String key, Object value) {
		this.options.put(key, value);
	}

	/*
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.vo.grid.SMTGridIntfc#addCustomValues(java.util.Map)
	 */
	public void addCustomValues(Map<String, Object> values) {
		if (values == null) return;
		this.options.putAll(values);
	}

	/**
	 * @return the cols
	 */
	public List<SMTGridColumnIntfc> getCols() {
		return columns;
	}

	/**
	 * @return the rows
	 */
	public List<SMTGridRowIntfc> getRows() {
		return rows;
	}

	/**
	 * @return the p
	 */
	public Map<String, Object> getCustomValue() {
		return options;
	}

	/* (non-Javadoc)
	 * @see com.biomed.smarttrak.vo.grid.SMTGridIntfc#addDisclaimer(java.lang.String)
	 */
	@Override
	public void setDisclaimer(String disclaimer) {
		this.disclaimer = disclaimer;
	}

	/* (non-Javadoc)
	 * @see com.biomed.smarttrak.vo.grid.SMTGridIntfc#getDisclaimer()
	 */
	@Override
	public String getDisclaimer() {
		return disclaimer;
	}
}