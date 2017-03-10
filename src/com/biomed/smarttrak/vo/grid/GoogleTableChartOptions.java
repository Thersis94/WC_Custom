package com.biomed.smarttrak.vo.grid;

// JDK 1.8
import java.util.HashMap;
import java.util.Map;

// APP Libs
import com.biomed.smarttrak.admin.vo.GridDetailVO;

/********************************************************************
 * <b>Title: </b>GoogleTableChartOptions.java<br/>
 * <b>Description: </b>Extends the google base options for  Table Charts<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author james
 * @version 3.x
 * @since Mar 1, 2017
 * Last Updated:
 * 	
 *******************************************************************/
public class GoogleTableChartOptions extends GoogleBaseChartOptions {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public GoogleTableChartOptions(boolean full) {
		super(full);
		this.createChartOptions();
	}
	
	/**
	 * Creates the attributes for the pie chart
	 */
	protected void createChartOptions() {
		super.createChartOptions("top");
		this.chart.put("allowHtml", true);
		this.chart.put("sort", "disable");
		this.chart.put("isStacked", true);
		
		Map<String, String> cssClassNames = new HashMap<>();
		cssClassNames.put("tableCell", "");
		cssClassNames.put("tableRow", "");
		cssClassNames.put("oddTableRow", "");
		cssClassNames.put("tableCell", "");
		cssClassNames.put("selectedTableRow", "selectedTableRow");
		
		chart.put("cssClassNames", cssClassNames);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.vo.grid.GoogleBaseChartOptions#addRowOptions(com.biomed.smarttrak.admin.vo.GridDetailVO)
	 */
	@Override
	public void addRowOptions(GridDetailVO detail) {
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.vo.grid.GoogleBaseChartOptions#addCellOptions(com.biomed.smarttrak.admin.vo.GridDetailVO)
	 */
	@Override
	public void addCellOptions(GridDetailVO detail) {
		//cell.put("className", "bs-data");
	}
}

