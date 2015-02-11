package com.depuy.events_v2.vo.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.depuy.events_v2.vo.AttendeeSurveyVO;
import com.siliconmtn.data.report.ExcelReport;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <b>Title</b>: AttendeeSurveyReportVO.java<p/>
 * <b>Description: Proxies a call to our ExcelReport for format the data into an Excel file.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Erik Wingo
 * @version 1.0
 * @since Nov 11, 2014
 ****************************************************************************/
public class AttendeeSurveyReportVO extends AbstractSBReportVO {

	private static final long serialVersionUID = 1L;
	private Collection<AttendeeSurveyVO> data = null;
	private Map<String, String> questionMap = null;
	
	/**
	 * Default Constructor
	 */
	public AttendeeSurveyReportVO() {
		isHeaderAttachment(Boolean.TRUE);
		setContentType("application/vnd.ms-excel");
		setFileName("Attendee Survey Report.xls");
	}
	
	public AttendeeSurveyReportVO(Map<String, String> questionMap) {
		this();
		this.questionMap = questionMap;
	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	public byte[] generateReport() {		
		ExcelReport rpt = new ExcelReport(questionMap);
		
		List<Map<String, Object>> rows = new ArrayList<>(data.size());
		for (AttendeeSurveyVO vo : data)
			rows.add(vo.getResponses());
		
		rpt.setData(rows);
		
		return rpt.generateReport();
	}

	@SuppressWarnings("unchecked")
	@Override
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	public void setData(Object o) {
		if (o != null &&  o instanceof Collection )
			data = (Collection<AttendeeSurveyVO>) o;
	}
}