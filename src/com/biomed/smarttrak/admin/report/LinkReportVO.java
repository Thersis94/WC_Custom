package com.biomed.smarttrak.admin.report;

// Java 8
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//WC custom
import com.biomed.smarttrak.vo.LinkVO;

//SMTBaseLibs
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.util.Convert;

// WebCrescendo
import com.smt.sitebuilder.action.AbstractSBReportVO;

/*****************************************************************************
 <p><b>Title</b>: LinkReportVO.java</p>
 <p><b>Description: </b>Creates the Broken Links Excel report.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Apr 2, 2017
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
		setFileName("SmartTRAK Broken Links.xls");
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("generateReport...");
		ExcelReport rpt = new ExcelReport(getHeader());
		rpt.setTitleCell("SmartTRAK Broken Links Report - " + Convert.formatDate(Calendar.getInstance().getTime(),  Convert.DATE_LONG));

		List<Map<String, String>> rows = new ArrayList<>(links.size());
		generateDataRows(rows);
		rpt.setData(rows);

		return rpt.generateReport();
	}


	/**
	 * @param rows
	 */
	private void generateDataRows(List<Map<String, String>> rows) {
		for (LinkVO vo : links) {
			Map<String, String> hdr = new HashMap<>();
			hdr.put("SECTION",vo.getSection());
			hdr.put("DATE", Convert.formatDate(vo.getLastChecked(), Convert.DATE_SLASH_PATTERN));
			hdr.put("CODE", Integer.toString(vo.getOutcome()));
			hdr.put("PAGE", "<a href=\"" + vo.getPublicUrl() + "\">" + vo.getSection() + "</a>");
			hdr.put("EDIT", "<a href=\"" + vo.getAdminUrl() + "\">Edit</a>");
			hdr.put("URL", vo.getUrl());
			rows.add(hdr);
		}
	}


	/**
	 * @return
	 */
	private Map<String, String> getHeader() {
		Map<String, String> hdr = new LinkedHashMap<>();
		hdr.put("SECTION","Section");
		hdr.put("DATE","Date checked");
		hdr.put("CODE","Response Code");
		hdr.put("PAGE","Affected Page");
		hdr.put("EDIT","Admin Page (Edit)");
		hdr.put("URL","Broken Link");
		return hdr;
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