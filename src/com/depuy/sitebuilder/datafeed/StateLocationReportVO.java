
package com.depuy.sitebuilder.datafeed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: StateLocationReportVO.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> processes a request for a non HTML Fulfillment report vo
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Aug 18, 2016<p/>
 * @updates:
 ****************************************************************************/
public class StateLocationReportVO extends AbstractDataFeedReportVO {

	private static final long serialVersionUID = 1L;
	private String startDate=null;
	private String endDate=null;

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.AbstractDataFeedReportVO#setRequestData(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void setRequestData(SMTServletRequest req) {
		startDate = StringUtil.checkVal(req.getParameter("startDate"));
		endDate = StringUtil.checkVal(req.getParameter("endDate"));
		
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("starting generateReport()");

		ExcelReport rpt = new ExcelReport(this.getHeader());

		List<Map<String, Object>> rows = new ArrayList<>();
		
		StringBuilder sb = new StringBuilder(100);

		sb.append("Channel Report From ").append(this.startDate).append(" to ").append(endDate);

		rpt.setTitleCell(sb.toString());

		rows = generateDataRows(rows);

		rpt.setData(rows);

		return rpt.generateReport();
	}

	/**
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateDataRows(
			List<Map<String, Object>> rows) {
		// TODO Auto-generated method stub
		return rows;
	}

	/**
	 * @return
	 */
	private Map<String, String> getHeader() {
		
		HashMap<String, String> headerMap = new LinkedHashMap<>();
		headerMap.put("COL_0","");
		headerMap.put("COL_1","");
		headerMap.put("COL_2","");
		headerMap.put("COL_3","");
		return headerMap;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@Override
	public void setData(Object o) {
		// TODO Auto-generated method stub
		
	}

}
