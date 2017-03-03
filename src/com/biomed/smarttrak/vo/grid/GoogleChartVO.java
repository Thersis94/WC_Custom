package com.biomed.smarttrak.vo.grid;

// JDK 1.8.x
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.biomed.smarttrak.admin.vo.GridDetailVO;
import com.biomed.smarttrak.admin.vo.GridVO;
import com.biomed.smarttrak.vo.grid.SMTChartOptionFactory.ChartType;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;


/********************************************************************
 * <b>Title: </b>GoogleChartVO.java<br/>
 * <b>Description: </b>Hierarchical Structure for the GoogleCharts JSON Data<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author james
 * @version 3.x
 * @since Feb 28, 2017
 * Last Updated:
 * 	
 *******************************************************************/
public class GoogleChartVO implements Serializable, SMTGridIntfc {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected static final Logger log = Logger.getLogger(GoogleChartVO.class);
	
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
	private List<SMTGridColumnIntfc> cols;
	
	/**
	 * Collection of Row Objects
	 */
	private List<SMTGridRowIntfc> rows;
	
	/**
	 * Chart options, styles and Javascript elements
	 */
	private Map<String, Object> p;

	/**
	 * 
	 */
	public GoogleChartVO() {
		super();
		cols = new ArrayList<>(10);
		rows = new ArrayList<>(64);
		p = new LinkedHashMap<>(32);
	}
	
	/**
	 * Creates the chart vo by populating it with data from the grid
	 * @param grid
	 */
	public GoogleChartVO(GridVO grid, ChartType type) {
		this();
		
		// Different charts require that the data be processed differently
		switch(type) {
			case PIE:
			case DONUT:
				processGridPie(grid);
				break;
			default:
				processGridBar(grid);
		}
		
	}
	
	/**
	 * Converts the grid data into a google grid object
	 * @param grid Grid data
	 */
	public void processGridBar(GridVO grid) {
		String[] series = grid.getSeries();
		Set<Integer> validRows = new HashSet<>();
		
		// Get the row data that corresponds to the series
		List<GridDetailVO> details = grid.getDetails();
		for(int i=0; i < details.size(); i++) {
			GridDetailVO detail = details.get(i);
			GoogleChartRowVO row = new GoogleChartRowVO();
			
			// Add the label cell data
			GoogleChartCellVO cell = new GoogleChartCellVO();
			cell.setValue(series[i]);
			row.addCell(cell);
			
			String[] values = detail.getValues();
			for (int x=0; x < values.length; x++) {
				String value = values[x];
				if (StringUtil.isEmpty(value)) continue;
				
				cell = new GoogleChartCellVO();
				cell.setValue(Convert.formatDouble(detail.getValues()[x]));
				row.addCell(cell);
				validRows.add(x);
			}
			
			addRow(row);
		}
		
		// Get the column labels first
		int val = (int) 'A';
		GoogleChartColumnVO col = new GoogleChartColumnVO();
		col.setId("Column_" + ((char) val++));
		col.setDataType(DataType.STRING.getName());
		col.setLabel("");
		addColumn(col);
		
		// Get the column data
		for (int i=0; i < details.size(); i++) {
			GridDetailVO detail = details.get(i);
			if (detail == null) continue;
			col = new GoogleChartColumnVO();
			col.setId("Column_" + ((char) val++));
			col.setDataType(DataType.NUMBER.getName());
			col.setLabel(detail.getLabel());
			
			addColumn(col);
		}
	}
	
	/**
	 * Converts the grid data into a google grid object
	 * @param grid Grid data
	 */
	public void processGridPie(GridVO grid) {
		String[] series = grid.getSeries();
		Set<Integer> validRows = new HashSet<>();
		
		// Get the row data that corresponds to the series
		for(GridDetailVO detail : grid.getDetails()) {
			GoogleChartRowVO row = new GoogleChartRowVO();
			
			// Add the label cell data
			GoogleChartCellVO cell = new GoogleChartCellVO();
			cell.setValue(detail.getLabel());
			row.addCell(cell);
			
			String[] values = detail.getValues();
			for (int i=0; i < values.length; i++) {
				String value = values[i];
				if (StringUtil.isEmpty(value)) continue;
				
				cell = new GoogleChartCellVO();
				cell.setValue(Convert.formatDouble(detail.getValues()[i]));
				row.addCell(cell);
				validRows.add(i);
			}
			
			addRow(row);
		}
		
		// Get the column labels first
		int val = (int) 'A';
		GoogleChartColumnVO col = new GoogleChartColumnVO();
		col.setId("Column_" + ((char) val++));
		col.setDataType(DataType.STRING.getName());
		col.setLabel("");
		addColumn(col);
		
		// Get the column data
		for (int j: validRows) {
			col = new GoogleChartColumnVO();
			col.setId("Column_" + ((char) val++));
			col.setDataType(DataType.NUMBER.getName());
			col.setLabel(series[j]);
			
			addColumn(col);
		}
	}
	
	/**
	 * Adds a column to the data set
	 * @param column
	 */
	public void addColumn(SMTGridColumnIntfc column) {
		cols.add(column);
	}
	
	/**
	 * Adds a set of row data to the chart
	 * @param row
	 */
	public void addRow(SMTGridRowIntfc row) {
		rows.add(row);
	}
	
	/**
	 * @param p the p to set
	 */
	public void addCustomValue(String key, Object value) {
		this.p.put(key, value);
	}

	/*
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.vo.grid.SMTGridIntfc#addCustomValues(java.util.Map)
	 */
	public void addCustomValues(Map<String, Object> values) {
		if (values == null) return;
		this.p.putAll(values);
	}
	
	/**
	 * @return the cols
	 */
	public List<SMTGridColumnIntfc> getCols() {
		return cols;
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
		return p;
	}

}

