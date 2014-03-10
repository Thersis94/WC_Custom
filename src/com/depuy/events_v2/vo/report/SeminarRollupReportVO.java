package com.depuy.events_v2.vo.report;

import java.util.List;

import com.depuy.events.vo.CoopAdVO;
import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
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
		log.debug("starting SeminarRollupReport");
		StringBuilder rpt = getHeader();

		for (DePuyEventSeminarVO sem : postcards) {
			for (EventEntryVO vo : sem.getEvents()) {
				buildRow(vo, sem, rpt);
				log.debug("added row for Event: " + vo.getActionId());
			}
		}

		rpt = getFooter(rpt);
		return rpt.toString().getBytes();
	}
	
	private void buildRow(EventEntryVO vo, DePuyEventSeminarVO sem, StringBuilder rpt) {
		Integer leadsCnt = sem.getTotalSelectedLeads();
		Integer attendCnt = 0;
		Integer rsvpCnt = sem.getRsvpCount();
		CoopAdVO ad = sem.getNewspaperAd();
		
		rpt.append("<tr><td>").append(sem.getJointLabel()).append("</td>");
		rpt.append("<td>").append(vo.getEventTypeDesc()).append("</td>");
		rpt.append("<td>").append(vo.getRSVPCode()).append("</td>");
		rpt.append("<td>").append(sem.getStatusName()).append("</td>");
		rpt.append("<td>").append(sem.getOwner().getFirstName()).append(" ").append(sem.getOwner().getLastName()).append("</td>");
		rpt.append("<td>").append(vo.getEventName()).append("</td>");
		rpt.append("<td>").append(vo.getCityName()).append("</td>");
		rpt.append("<td>").append(vo.getStateCode()).append("</td>");
		rpt.append("<td>").append(Convert.formatDate(vo.getStartDate(), Convert.DATE_SLASH_PATTERN)).append("</td>");
		rpt.append("<td>").append(vo.getLocationDesc()).append("</td>");
		rpt.append("<td>").append(sem.getSurgeon().getSurgeonName()).append("</td>");
		rpt.append("<td>").append(rsvpCnt).append("</td>");
		rpt.append("<td>").append(attendCnt).append("</td>");
		rpt.append("<td>").append(Float.valueOf(attendCnt)/Float.valueOf(rsvpCnt)*100).append("%</td>");
		rpt.append("<td>").append(Float.valueOf(attendCnt)/Float.valueOf(leadsCnt)*100).append("%</td>");
		rpt.append("<td>").append(leadsCnt).append("</td>");
		rpt.append("<td></td>");
		rpt.append("<td></td>");
		rpt.append("<td></td>");
		rpt.append("<td>").append(ad.getStatusName()).append("</td>");
		rpt.append("<td>").append(StringUtil.checkVal(ad.getNewspaper1Text())).append("</td>");
		rpt.append("<td>").append(StringUtil.checkVal(ad.getNewspaper1Phone())).append("</td>");
		rpt.append("<td>").append(StringUtil.checkVal(ad.getApprovedPaperName())).append("</td>");
		rpt.append("<td>").append(StringUtil.checkVal(ad.getAdDatesText())).append("</td>");
		rpt.append("<td>").append(StringUtil.checkVal(ad.getTotalCostNo())).append("</td>");
		rpt.append("<td>").append(StringUtil.checkVal(ad.getCostToRepNo())).append("</td>");
		rpt.append("<td>").append(StringUtil.checkVal(sem.getTerritoryNumber())).append("</td>");
		rpt.append("<td></td>");
		rpt.append("<td></td></tr>\r");
		
		return;
	}

	private StringBuilder getHeader() {
		StringBuilder hdr = new StringBuilder();
		hdr.append("<table border='1'>\r");
		hdr.append("<tr><th colspan='30' align='left'><h2>Local Market Seminars Comprehensive Data</h2></th></tr>\r");
		hdr.append("<tr><th>Joint</th>");
		hdr.append("<th>Seminar Type</th>");
		hdr.append("<th>Seminar Code</th>");
		hdr.append("<th>Seminar Status</th>");
		hdr.append("<th>Seminar Holder</th>");
		hdr.append("<th>Seminar Location</th>");
		hdr.append("<th>City</th>");
		hdr.append("<th>State</th>");
		hdr.append("<th>Seminar Date</th>");
		hdr.append("<th>Seminar Time</th>");
		hdr.append("<th>Speaker</th>");
		hdr.append("<th>Reservations (Total)</th>");
		hdr.append("<th>Attended Seminar</th>");
		hdr.append("<th>%-Attendance (RSVP)</th>");
		hdr.append("<th>%-Attendance (Leads)</th>");
		hdr.append("<th>Leads</th>");
		hdr.append("<th>PC Approved</th>");
		hdr.append("<th>PC Sent</th>");
		hdr.append("<th>Comments</th>");
		hdr.append("<th>Coop Ad Status</th>");
		hdr.append("<th>Newspaper</th>");
		hdr.append("<th>Newspaper Phone</th>");
		hdr.append("<th>Approved Newspaper</th>");
		hdr.append("<th>Date Ad will Run</th>");
		hdr.append("<th>Total Ad Cost</th>");
		hdr.append("<th>Cost to Territory</th>");
		hdr.append("<th>Territory No.</th>");
		hdr.append("<th>Sent Ad Info</th>");
		hdr.append("<th>Ad Received</th>");
		hdr.append("</tr>\r");

		return hdr;
	}

	private StringBuilder getFooter(StringBuilder sb) {
		sb.append("</table>");
		return sb;
	}

}
