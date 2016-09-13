package com.depuy.events_v2.vo.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.depuy.events.vo.CoopAdVO;
import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/*****************************************************************************
 <p><b>Title</b>: SeminarRollupReportVO.java</p>
 <p>compiles a report for postcards sent - used by depuy internally for monitoring seminars over time.</p>
 <p>Copyright: Copyright (c) 2000 - 2014 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Nov 6, 2014
 ***************************************************************************/

public class SeminarRollupReportVO extends AbstractSBReportVO {
	private static final long serialVersionUID = 1l;
	private List<DePuyEventSeminarVO> postcards = null;

	public SeminarRollupReportVO() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("Seminar Rollup.xls");
	}

    /**
     * Assigns the event postcard data retrieved from the parent action
     * variables
     * @param data (List<DePuyEventPostcardVO>)
     * @throws SQLException
     */
	@SuppressWarnings("unchecked")
	public void setData(Object o) {
		List<DePuyEventSeminarVO> postcards = (List<DePuyEventSeminarVO>) o;
		this.postcards = postcards;
	}
    
	public byte[] generateReport() {
		
		ExcelReport rpt = new ExcelReport(this.getHeader());
		
		List<Map<String, Object>> rows = new ArrayList<>(postcards.size());
		
		rpt.setTitleCell(getTitleNote());

		rows = generateDataRows(rows);
		
		rpt.setData(rows);
		return rpt.generateReport();
	}
	
	/**
	 * this method is used to generate the data rows of the excel sheet.
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateDataRows(	List<Map<String, Object>> rows) {
		
		for (DePuyEventSeminarVO sem : postcards) {
			for (EventEntryVO vo : sem.getEvents()) {
				
				Integer leadsCnt = sem.getTotalSelectedLeads();
				Integer attendCnt = Convert.formatInteger(sem.getSurveyResponse("attendee_cnt"), 0);
				Integer rsvpCnt = sem.getRsvpCount();
				int x = 0;
				List<CoopAdVO> ads = sem.getAllAds();
				
				do {
					Map<String, Object> row = new HashMap<String, Object>();
					CoopAdVO ad = (ads != null && ads.size() > x) ? ads.get(x) : new CoopAdVO();
					++x;
					
					row.put("JOINT", sem.getJointLabel());
					row.put("SEMINAR_TYPE", vo.getEventTypeDesc());
					row.put("SEMINAR_CODE", vo.getRSVPCode());
					row.put("SEMINAR_STATUS", sem.getStatusName());
					
					StringBuilder host = new StringBuilder(32);
					host.append(sem.getOwner().getFirstName()).append(" ").append(sem.getOwner().getLastName());
					row.put("SEMINAR_HOLDER", host.toString());
					
					row.put("SEMINAR_LOCATION", vo.getEventName());
					row.put("CITY",vo.getCityName());
					row.put("STATE",vo.getStateCode());
					row.put("SEMINAR_DATE", Convert.formatDate(vo.getStartDate(), Convert.DATE_SLASH_PATTERN));
					row.put("SEMINAR_TIME",StringUtil.checkVal(vo.getLocationDesc()));
					
					String surgeon = (sem.getSurgeon() != null) ? sem.getSurgeon().getSurgeonName() : "";
					row.put("SPEAKER",surgeon);
					
					row.put("RESERVATIONS_TOTAL", rsvpCnt);
					row.put("ATTENDED_SEMINAR", attendCnt);
					
					
					double val = Math.rint(Double.valueOf(attendCnt)/Double.valueOf(rsvpCnt));
					if (val > 0) {
						row.put("PERCENT_ATTENDANCE_RSVP", val*100);
					} else {
						row.put("PERCENT_ATTENDANCE_RSVP","");
					}
					val = Math.rint(Double.valueOf(attendCnt)/Double.valueOf(leadsCnt)); 
					if (val > 0) {
						row.put("PERCENT_ATTENDANCE_LEADS", val*100);
					} else {
						row.put("PERCENT_ATTENDANCE_LEADS","");
					}
					
					row.put("LEADS",leadsCnt);
					row.put("PC_APPROVED", sem.getPostcardFileStatusFlg() == 3 ? "Yes" : "No");
					row.put("PC_SENT", Convert.formatDate(sem.getPostcardSendDate(), Convert.DATE_SLASH_PATTERN));
					row.put("COMMENTS", "");
					row.put("COOP_AD_STATUS", ad.getStatusName());
					row.put("NEWSPAPER", StringUtil.checkVal(ad.getNewspaper1Text()));
					row.put("NEWSPAPER_PHONE", StringUtil.checkVal(ad.getNewspaper1Phone()));
					row.put("APPROVED_NEWSPAPER", StringUtil.checkVal(ad.getApprovedPaperName()));
					row.put("DATE_AD_WILL_RUN", StringUtil.checkVal(ad.getAdDatesText()));
					row.put("TOTAL_AD_COST", StringUtil.checkVal(ad.getTotalCostNo()));
					row.put("COST_TO_TERRITORY", StringUtil.checkVal(ad.getCostToRepNo()));
					row.put("TERRITORY_NO", StringUtil.checkVal(sem.getTerritoryNumber()));
					row.put("SENT_AD_INFO", "");
					row.put("AD_RECEIVED", "");
					rows.add(row);
				} while (x < ads.size());
				
				//log.debug("added row for Event: " + vo.getActionId() + " code=" + vo.getRSVPCode());
				
			}
		}
		
		return rows;
	}

	/**
	 * builds the header map at the top of the columns in the generated report
	 * @return
	 */
	private Map<String, String> getHeader() {
		
		HashMap<String, String> headerMap = new LinkedHashMap<String, String>();
		headerMap.put("JOINT", "Joint");
		headerMap.put("SEMINAR_TYPE", "Seminar Type");
		headerMap.put("SEMINAR_CODE", "Seminar Code");
		headerMap.put("SEMINAR_STATUS", "Seminar Status");
		headerMap.put("SEMINAR_HOLDER", "Seminar Holder");
		headerMap.put("SEMINAR_LOCATION", "Seminar Location");
		headerMap.put("CITY", "City");
		headerMap.put("STATE", "State");
		headerMap.put("SEMINAR_DATE", "Seminar Date");
		headerMap.put("SEMINAR_TIME", "Seminar Time");
		headerMap.put("SPEAKER", "Speaker");
		headerMap.put("RESERVATIONS_TOTAL", "Reservations (Total)");
		headerMap.put("ATTENDED_SEMINAR", "Attended Seminar");
		headerMap.put("PERCENT_ATTENDANCE_RSVP", "%-Attendance (RSVP)");
		headerMap.put("PERCENT_ATTENDANCE_LEADS", "%-Attendance (Leads)");
		headerMap.put("LEADS", "Leads");
		headerMap.put("PC_APPROVED", "PC Approved");
		headerMap.put("PC_SENT", "PC Sent");
		headerMap.put("COMMENTS", "Comments");
		headerMap.put("COOP_AD_STATUS", "Coop Ad Status");
		headerMap.put("NEWSPAPER", "Newspaper");
		headerMap.put("NEWSPAPER_PHONE", "Newspaper Phone");
		headerMap.put("APPROVED_NEWSPAPER", "Approved Newspaper");
		headerMap.put("DATE_AD_WILL_RUN", "Date Ad will Run");
		headerMap.put("TOTAL_AD_COST", "Total Ad Cost");
		headerMap.put("COST_TO_TERRITORY", "Cost to Territory");
		headerMap.put("TERRITORY_NO", "Territory No.");
		headerMap.put("SENT_AD_INFO", "Sent Ad Info");
		headerMap.put("AD_RECEIVED", "Ad Received");

		return headerMap;
	}

	/**
	 * used to build the title note at the top of the excel document.
	 * @return
	 */
	private String getTitleNote() {
		
			String note = "Local Market Seminars Comprehensive Data";
			
		return note;
	}
	
}
