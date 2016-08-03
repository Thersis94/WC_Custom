package com.depuy.events_v2.vo.report;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.depuy.events_v2.vo.RsvpBreakdownVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.util.Convert;

/*****************************************************************************
 * <p>
 * <b>Title</b>: RsvpSummaryReportVO.java
 * </p>
 * <p>
 * generates a table of active events and # of rsvps (by type) for each.
 * </p>
 * <p>
 * Copyright: Copyright (c) 2000 - 2014 SMT, All Rights Reserved
 * </p>
 * <p>
 * Company: Silicon Mountain Technologies
 * </p>
 * 
 * @author James McKain
 * @version 1.0
 * @since Jan 20, 2014
 ***************************************************************************/

public class RsvpBreakdownReportVO extends AbstractSBReportVO {
	private static final long serialVersionUID = 1l;
	private List<RsvpBreakdownVO> events = null;
	private Set<String> referrers = null;
	private Date start, end;

	public RsvpBreakdownReportVO() {
		super();
		referrers = new TreeSet<String>();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("RSVP-Referrals-Breakdown.xls");
	}

	/**
	 * Assigns the event postcard data retrieved from the parent action
	 * variables
	 * 
	 * @param data
	 *             (List<DePuyEventPostcardVO>)
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	public void setData(Object o) {
		this.events = new ArrayList<RsvpBreakdownVO>(((Collection<RsvpBreakdownVO>) o));
		
		//tabulate a list of ALL referral types across all the events we're displaying, so columns align across the set.
		for (RsvpBreakdownVO vo : events) {
	    		for (String stat : vo.getReferralStats().keySet()) {
	    			if (stat == null || stat.equals("")) stat = "No Referrer";
	    			if (!referrers.contains(stat))
	    				referrers.add(stat);
	    		}
	    	}
	}

	public byte[] generateReport() {
		
		ExcelReport rpt = new ExcelReport(this.getHeader());
		
		List<Map<String, Object>> rows = new ArrayList<>(events.size());
		
		rpt.setTitleCell(getTitleNote());
		
		rows = generateDataRows(rows);
		
		rpt.setData(rows);
		
		return rpt.generateReport();
	}

	/**
	 * this method is used to generate the data rows of the excel sheet.
	 * @param rows
	 * @param header
	 * @return
	 */
	private List<Map<String, Object>> generateDataRows(
			List<Map<String, Object>> rows) {
		
		for (RsvpBreakdownVO vo : events){
			Map<String, Object> row = new HashMap<String, Object>();
			//sum the overall total RSVPs
			int rsvpTotal = 0;
			for (Integer cnt : vo.getReferralStats().values()) {
				//log.debug("count"+ cnt);
				rsvpTotal += cnt;
			}
			row.put("SEMINAR_NO",vo.getRsvpCode());
			
			StringBuilder host = new StringBuilder(32);
			host.append(vo.getOwner().getFirstName()).append(" ").append(vo.getOwner().getLastName());
			row.put("SEMINAR_HOST", host.toString());
			
			row.put("SEMINAR_DATE", Convert.formatDate(vo.getSeminarDate(), "MM/dd/yyyy"));
			
			Map<String, Integer> rsvpStats = vo.getReferralStats();
			
			StringBuilder sb =  new StringBuilder(25);
			
			String statName = null;
						
			for (String stat : referrers) {
				
				statName = sb.append(stat).append("-percent").toString();
				sb.setLength(0);
				
				//log.debug("stat: " + stat);
				if (rsvpStats.containsKey(stat)) {
					Integer cnt = rsvpStats.get(stat);
					row.put(stat, cnt);
					float percent = (Float.valueOf(cnt) / Float.valueOf(rsvpTotal)) * 100;
					row.put(statName,sb.append(Math.round(percent)).append("%").toString());
					sb.setLength(0);
				} else {
					row.put(stat,0);
					row.put(statName,"0%");
				}
			}
			
			row.put("TOTAL_RSVP", rsvpTotal);
			rows.add(row);
		}
		
		return rows;
	}

	/**
	 * used to build the title note at the top of the excel document.
	 * @return
	 */
	private String getTitleNote() {
		
		//make the note for row 1
		StringBuilder dateRange = new StringBuilder(51);
		if (end == null && start != null) {
			dateRange.append("after ").append(Convert.formatDate(start, Convert.DATE_SLASH_PATTERN));
		} else if (start == null && end != null) {
			dateRange.append("before ").append(Convert.formatDate(end, Convert.DATE_SLASH_PATTERN));
		} else {
			dateRange.append("between ").append(Convert.formatDate(start, Convert.DATE_SLASH_PATTERN));
			dateRange.append(" and ").append(Convert.formatDate(end, Convert.DATE_SLASH_PATTERN));
		}
		
		StringBuilder note = new StringBuilder();
		note.append("This report only represents Seminar attendees enrolled ");
		note.append(dateRange);
		
		return note.toString();
	}

	/**
	 * builds the header map for the excel report
	 * @return
	 */
	private LinkedHashMap<String, String> getHeader() {
		
		LinkedHashMap<String, String> headerMap = new LinkedHashMap<String, String>();
		headerMap.put("SEMINAR_NO","Seminar#");
		headerMap.put("SEMINAR_HOST","Seminar Host");
		headerMap.put("SEMINAR_DATE","Seminar Date");
		
		StringBuilder sb =  new StringBuilder(25);
		
		String statName = null;
				
		for (String stat : referrers) {
			
			statName = sb.append(stat).append("-percent").toString();
			sb.setLength(0);
				
			//log.debug("stat: " + stat );
			headerMap.put(stat, stat);
			headerMap.put(statName,"% of Total");
			
        }
		
		headerMap.put("TOTAL_RSVP","Total RSVPs");
		return headerMap;
		
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

}
