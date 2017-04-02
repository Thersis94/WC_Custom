package com.biomed.smarttrak.admin.report;

// Java 8
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//WC custom
import com.biomed.smarttrak.vo.LinkVO;

//SMTBaseLibs
import com.siliconmtn.data.report.ExcelReport;

// WebCrescendo
import com.smt.sitebuilder.action.AbstractSBReportVO;

/*****************************************************************************
 <p><b>Title</b>: AccountReportVO.java</p>
 <p><b>Description: </b>Creates the account(s) report as HTML.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Mar 7, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class LinkReportVO extends AbstractSBReportVO {

	private static final long serialVersionUID = 98123765765432882L;

	private transient List<LinkVO> links;

	/**
	 * Constructor
	 */
	public LinkReportVO() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("Links Report.xls");
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("generateReport...");
		ExcelReport rpt = new ExcelReport(getHeader());
		rpt.setTitleCell("SmartTRAK Hyperlinks Report");

		List<Map<String, Object>> rows = new ArrayList<>(links.size() * 5);
		generateDataRows(rows);

		rpt.setData(rows);
		return rpt.generateReport();
	}

	/**
	 * @param rows
	 */
	private void generateDataRows(List<Map<String, Object>> rows) {
		// TODO Auto-generated method stub

	}

	/**
	 * @return
	 */
	private Map<String, String> getHeader() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		this.links = (List<LinkVO>)o;
	}
}