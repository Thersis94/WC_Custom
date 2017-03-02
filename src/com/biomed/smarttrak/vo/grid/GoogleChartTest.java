package com.biomed.smarttrak.vo.grid;

import com.biomed.smarttrak.vo.grid.GoogleChartVO.DataType;
import com.google.gson.Gson;

/********************************************************************
 * <b>Title: </b>GoogleChartTest.java<br/>
 * <b>Description: </b><<<< Some Desc Goes Here >>>><br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author james
 * @version 3.x
 * @since Feb 28, 2017
 * Last Updated:
 * 	
 *******************************************************************/
public class GoogleChartTest {

	/**
	 * 
	 */
	public GoogleChartTest() {
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		GoogleChartVO chart = new GoogleChartVO();
		chart.addCustomValue("style", "'border: 1px solid green;'");
		// Add the rows
		for(int x=0; x < 4; x++) {
			GoogleChartRowVO row = new GoogleChartRowVO();
			
			// Add the columns and cells
			for (int i=0; i < 5; i++) {
				if (x == 0) {
					GoogleChartColumnVO col = new GoogleChartColumnVO();
					DataType type = DataType.STRING;
					
					col.setId("ID_" + i);
					col.setLabel("Column: " + i);
					col.setDataType(type);
					if (i == 3) col.addCustomValue("className", "myCustomClass");
					chart.addColumn(col);
				}
				
				GoogleChartCellVO cell = new GoogleChartCellVO();
				cell.setValue( Math.random() * 100);
				row.addCell(cell);
			}
			
			chart.addRow(row);
		}
		
		Gson g = new Gson();
		String json = g.toJson(chart);
		System.out.println(json);
	}

}

