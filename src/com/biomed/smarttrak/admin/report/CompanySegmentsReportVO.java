package com.biomed.smarttrak.admin.report;

// Java 8
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//WC custom
import com.biomed.smarttrak.vo.CompanyVO;
import com.siliconmtn.data.GenericVO;
//SMTBaseLibs
import com.siliconmtn.data.report.ExcelReport;

// WebCrescendo
import com.smt.sitebuilder.action.AbstractSBReportVO;

/*****************************************************************************
 <p><b>Title</b>: CompanySegmentsReportVO.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Mar 10, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class CompanySegmentsReportVO extends AbstractSBReportVO {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4394712706866499640L;
	private static final String REPORT_TITLE = "Company Segments Export";
	private static final String CO_ID = "CO_ID";
	private static final String CO_NM = "CO_NM";
	protected static final String DATA_KEY_COMPANIES = "COMPANIES";
	protected static final String DATA_KEY_SEGMENTS = "SEGMENTS";
	protected static final String KEY_FALSE = "False";
	protected static final String KEY_TRUE = "True";
	private List<CompanyVO> companies;
	private Map<String,String> segments;
	
	/**
	* Constructor
	*/
	public CompanySegmentsReportVO() {
        super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName(REPORT_TITLE+".xls");
        companies = new ArrayList<>();
        segments = new LinkedHashMap<>();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("generateReport...");

		ExcelReport rpt = new ExcelReport(getHeader());
		rpt.setTitleCell(REPORT_TITLE);

		List<Map<String, Object>> rows = new ArrayList<>(companies.size() * 5);
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
		Map<String,Object> dataMap = (Map<String,Object>)o;
		companies = (List<CompanyVO>) dataMap.get(DATA_KEY_COMPANIES);
		segments = (Map<String,String>) dataMap.get(DATA_KEY_SEGMENTS);
	}
	
	/**
	 * this method is used to generate the data rows of the excel sheet.
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateDataRows(
			List<Map<String, Object>> rows) {

		Map<String,Object> row;
		for (CompanyVO co : companies) {
			row = new HashMap<>();
			row.put(CO_ID, co.getCompanyId());
			row.put(CO_NM, co.getCompanyName());

			for (Map.Entry<String,String> seg : segments.entrySet()) {
				row.put(seg.getKey(), hasSegment(co,seg.getKey()));
			}
			rows.add(row);
		}

		return rows;
	}

	/**
	 * Determines if a company is associated with a segment (i.e. section).
	 * @param company
	 * @param segmentId
	 * @return
	 */
	protected String hasSegment(CompanyVO company, String segmentId) {
		if (company.getCompanySections() == null || 
				company.getCompanySections().isEmpty()) return KEY_FALSE;
		for (GenericVO vo : company.getCompanySections()) {
			if (segmentId.equals(vo.getKey())) return KEY_TRUE;
		}
		return KEY_FALSE;
	}

	/**
	 * builds the header map for the excel report
	 * @return
	 */
	protected HashMap<String, String> getHeader() {
		HashMap<String, String> headerMap = new LinkedHashMap<>();
		headerMap.put(CO_ID,"Company ID");
		headerMap.put(CO_NM,"Company Name");
		for (Map.Entry<String,String> segment : segments.entrySet()) {
			headerMap.put(segment.getKey(),segment.getValue());
		}
		return headerMap;
	}

}
