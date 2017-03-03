package com.biomed.smarttrak.admin.report;

// Java 8
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// WC custom
import com.biomed.smarttrak.vo.UserActivityVO;

// SMTBaseLibs
import com.siliconmtn.data.report.ExcelReport;

// WebCrescendo
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.util.PageViewVO;

/*****************************************************************************
 <p><b>Title</b>: UserActivityReportVO.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 27, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserActivityReportVO extends AbstractSBReportVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4515509174093168302L;
	
	private Map<String, UserActivityVO> activity;
	private static final String REPORT_TITLE = "Activity Report";
	private static final String NAME = "NAME";
	private static final String PAGE = "PAGE";
	private static final String VISIT_DATE = "VISIT_DATE";

	/**
	* Constructor
	*/
	public UserActivityReportVO() {
        super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName(REPORT_TITLE.replace(' ', '-')+".xls");
        activity = new LinkedHashMap<>();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("generateReport...");

		ExcelReport rpt = new ExcelReport(getHeader());
		rpt.setTitleCell(REPORT_TITLE);

		List<Map<String, Object>> rows = new ArrayList<>(activity.size());
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
		this.activity =  (Map<String,UserActivityVO>) o;
	}
	
	/**
	 * this method is used to generate the data rows of the excel sheet.
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateDataRows(List<Map<String, Object>> rows) {
		// loop the account map
		Map<String,Object> row;
		for (Map.Entry<String, UserActivityVO> users : activity.entrySet()) {
			int cnt = 1;
			UserActivityVO user = users.getValue();
			// loop users
			for (PageViewVO page : user.getPageViews()) {
				row = new HashMap<>();
				row.put(NAME, cnt > 1 ? "" : user.getFirstName() + " " + user.getLastName());
				row.put(PAGE, page.getPageDisplayName());
				row.put(VISIT_DATE, page.getVisitDate());
				rows.add(row);
				cnt++;
			}
			
			// add blank separator row.
			addBlankRow(rows);
		}
		return rows;
	}
	
	/**
	 * Adds blank row to the list of data rows.
	 * @param rows
	 */
	protected void addBlankRow(List<Map<String,Object>> rows) {
		Map<String,Object> row = new HashMap<>();
		row.put(NAME, "");
		row.put(PAGE, "");
		row.put(VISIT_DATE, "");
		rows.add(row);
	}

	/**
	 * builds the header map for the excel report
	 * @return
	 */
	protected HashMap<String, String> getHeader() {
		HashMap<String, String> headerMap = new LinkedHashMap<>();
		headerMap.put(NAME,"Name");
		headerMap.put(PAGE,"Page");
		headerMap.put(VISIT_DATE,"Visit Date");
		return headerMap;
	} 
	
}