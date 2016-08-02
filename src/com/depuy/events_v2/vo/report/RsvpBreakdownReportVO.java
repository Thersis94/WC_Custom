package com.depuy.events_v2.vo.report;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.depuy.events_v2.vo.RsvpBreakdownVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.security.UserDataVO;
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
		
		HashMap<String, String> header = this.getHeader();
		
		ExcelReport rpt = new ExcelReport(header);
		
		List<Map<String, Object>> rows = new ArrayList<>(events.size());
		
		rows = generateHeaderRows(rows,header);
		
		rows = generateDataRows(rows, header);
		
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
			List<Map<String, Object>> rows, HashMap<String, String> header) {
		
		for (RsvpBreakdownVO vo : events){
			Map<String, Object> row = new HashMap<String, Object>();
			//sum the overall total RSVPs
			int rsvpTotal = 0;
			for (Integer cnt : vo.getReferralStats().values()) {
				rsvpTotal += cnt;
			}
			row.put("SEMINAR_NO",vo.getRsvpCode());
			
			StringBuilder host = new StringBuilder(32);
			host.append(vo.getOwner().getFirstName()).append(" ").append(vo.getOwner().getLastName());
			row.put("SEMINAR_HOST", host.toString());
			
			row.put("SEMINAR_DATE", Convert.formatDate(vo.getSeminarDate(), "MM/dd/yyyy"));
			
			Map<String, Integer> rsvpStats = vo.getReferralStats();
			
			//TODO try and understand what this is doing, i feel like this might not be just making two cells
			for (String stat : referrers) {
				if (rsvpStats.containsKey(stat)) {
					Integer cnt = rsvpStats.get(stat);
					row.put("count", cnt);
					float percent = (Float.valueOf(cnt) / Float.valueOf(rsvpTotal)) * 100;
					row.put("percent",Math.round(percent));
				} else {
					row.put("count",0);
					row.put("percent",0);
				}
			}
			rows.add(row);
		}
		
		return rows;
	}

	/**
	 * generates the header section of the excel sheet.  then the rows map is
	 *     pass on to other methods or objects to be filled with data
	 * @param rows
	 * @param header 
	 * @return
	 */
	private List<Map<String, Object>> generateHeaderRows(
			List<Map<String, Object>> rows, HashMap<String, String> header) {
		
		Map <String, Object> row1 = new HashMap<String, Object>();
		
		row1.put(ExcelReport.TITLE_CELL, getTitleNote() );
		rows.add(row1);
		//make the header rows
		Map <String, Object> row2 = new HashMap<String, Object>();
		
		for (String key : header.keySet()){
			row2.put(key, header.get(key));
		}
		
		rows.add(row2);
		
		return rows;
	}

	/**
	 * used to build the title note at the top of the excel document.
	 * @return
	 */
	private Object getTitleNote() {
		
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

	private HashMap<String, String> getHeader() {
		
		HashMap<String, String> headerMap = new HashMap<String, String>();
		headerMap.put("SEMINAR_NO","Seminar#");
		headerMap.put("SEMINAR_HOST","Seminar Host");
		headerMap.put("SEMINAR_DATE","Seminar Date");
		headerMap.put("COUNT","count");
		headerMap.put("PRECENT","percent");
		headerMap.put("TOTAL_RSVP","Total_RSVPs");
		
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
