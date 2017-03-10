package com.biomed.smarttrak.vo.grid;

import com.biomed.smarttrak.admin.vo.GridVO;
// WC Libs
import com.smt.sitebuilder.action.AbstractSBReportVO;

/********************************************************************
 * <b>Title: </b>BiomedExcelReport.java<br/>
 * <b>Description: </b>Takes the Excel files generated for biomed and streams<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author james
 * @version 3.x
 * @since Mar 4, 2017
 * Last Updated:
 * 	
 *******************************************************************/
public class BiomedExcelReport extends AbstractSBReportVO {
	
	// Data to be used to create the excel report
	private GridVO grid;
	 
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public BiomedExcelReport() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		byte[] data = null;
		
		GridExcelManager excel = new GridExcelManager();
		try {
			// Get the data
			data = excel.getExcelFile(grid);
		} catch(Exception e) {
			log.error("Unable to generate excel file", e);
		}
		
		return data;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@Override
	public void setData(Object o) {
		grid = (GridVO)o;
	}

}

