package com.biomed.smarttrak.admin.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.data.report.CSVReport;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.support.TicketVO;

/****************************************************************************
 * <b>Title</b>: SupportReportVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Custom Report VO for managing Support Report Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Mar 14, 2017
 ****************************************************************************/
public class SupportReportVO extends AbstractSBReportVO {

	private static final long serialVersionUID = -5442930109879798411L;
	private List<TicketVO> tickets = null;
	public static final String TITLE_TXT = "DirectAccess";
	public enum SupportReportCol {CREATOR("Creator"), ACCOUNT("Account"), TITLE("Title"),
		STATUS("Status"), PAGE("Page"), DATE("Incident Date"), DESC("Description"),
		ASSIGNED("Assigned To"), MINUTES("Minutes Worked"), COST("Additional Costs");
		String title;
		SupportReportCol(String title) {
			this.title = title;
		}

		public String getTitle() {
			return title;
		}
	}
	/**
	 * 
	 */
	public SupportReportVO() {
		super();
		setContentType("text/csv");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("Direct_Access_Report.csv");
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("generateReport...");

		CSVReport rpt = new CSVReport(getHeader());

		List<Map<String, Object>> rows = generateDataRows();

		rpt.setData(rows);
		return rpt.generateReport();
	}


	/**
	 * Build Report Data Rows.
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateDataRows() {
		List<Map<String, Object>> rows = new ArrayList<>();

		for(TicketVO t : tickets) {
			Map<String, Object> row = new HashMap<>();
			row.put(SupportReportCol.CREATOR.name(), t.getFirstName() + " " + t.getLastName());
			row.put(SupportReportCol.ACCOUNT.name(), t.getOrganizationId());
			row.put(SupportReportCol.TITLE.name(), TITLE_TXT);
			row.put(SupportReportCol.STATUS.name(), t.getStatusNm());
			row.put(SupportReportCol.PAGE.name(), t.getReferrerUrl());
			row.put(SupportReportCol.DATE.name(), Convert.formatDate(t.getCreateDt(), Convert.DATE_SLASH_PATTERN));
			row.put(SupportReportCol.DESC.name(), StringUtil.checkVal(t.getDescText()).replace("\n", "").replace("\r", ""));
			row.put(SupportReportCol.ASSIGNED.name(), StringUtil.checkVal(t.getAssignedFirstNm()) + " " + StringUtil.checkVal(t.getAssignedLastNm()));
			row.put(SupportReportCol.MINUTES.name(), t.getTotalEffortNo());
			row.put(SupportReportCol.COST.name(), t.getTotalCostNo());
			rows.add(row);
		}

		return rows;
	}


	/**
	 * @return
	 */
	private Map<String, String> getHeader() {
		Map<String, String> headers = new LinkedHashMap<>();
		for(SupportReportCol src : SupportReportCol.values()) {
			headers.put(src.name(), src.getTitle());
		}
		return headers;
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		tickets = (List<TicketVO>)o;
	}

}
