package com.biomed.smarttrak.admin.report;

// Java 8
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// WC custom
import com.biomed.smarttrak.vo.AccountVO;
import com.biomed.smarttrak.vo.UserVO;

// SMTBaseLibs
import com.siliconmtn.data.report.ExcelReport;

// WebCrescendo
import com.smt.sitebuilder.action.AbstractSBReportVO;

/*****************************************************************************
 <p><b>Title</b>: UserUtilizationReportVO.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 21, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserUtilizationReportVO extends AbstractSBReportVO {

	private Map<AccountVO, List<UserVO>> accounts;
	private final String reportTitle = "Activity Rollup Report";
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4463261563458766845L;
	
	/**
	* Constructor
	*/
	public UserUtilizationReportVO() {
		/* Sorted by company, showing:
		 * User Name
		 * User Title
		 * User Phone
		 * User Email
		 * User Update Email Preferences
		 * Monthly page views for last 12 months
		 * Summary at bottom of totals.
		 */
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("generateReport...");

		ExcelReport rpt = new ExcelReport(getHeader());
		rpt.setTitleCell(reportTitle);

		List<Map<String, Object>> rows = new ArrayList<>(accounts.size() * 5);
		rows = generateDataRows(rows);

		rpt.setData(rows);
		return rpt.generateReport();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		Map<AccountVO,List<UserVO>> accounts = (Map<AccountVO,List<UserVO>>) o;
		this.accounts = accounts;
	}
	
	/**
	 * this method is used to generate the data rows of the excel sheet.
	 * @param rows
	 * @return
	 */
		private List<Map<String, Object>> generateDataRows(
			List<Map<String, Object>> rows) {

		return rows;
	}
	
	/**
	 * builds the header map for the excel report
	 * @return
	 */
	protected HashMap<String, String> getHeader() {

		HashMap<String, String> headerMap = new LinkedHashMap<String, String>();

		return headerMap;
	} 
	
}
