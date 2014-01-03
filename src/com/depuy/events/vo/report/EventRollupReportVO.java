package com.depuy.events.vo.report;

import java.util.List;
import java.util.Iterator;

import com.depuy.events.vo.CoopAdVO;
import com.depuy.events.vo.DePuyEventEntryVO;
import com.depuy.events.vo.DePuyEventPostcardVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/*****************************************************************************
 <p><b>Title</b>: PostcardSummaryReportVO.java</p>
 <p>compiles a report for post cards sent</p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Nov 6, 2006
 ***************************************************************************/

public class EventRollupReportVO extends AbstractSBReportVO {
    private static final long serialVersionUID = 1l;
    private List<?> postcards = null;

    /**
     * 
     */
    public EventRollupReportVO() {
        super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName("Rollup-Summary.xls");
    }
    
    /**
     * Assigns the event postcard data retrieved from the parent action
     * variables
     * @param data (List<DePuyEventPostcardVO>)
     * @throws SQLException
     */
    public void setData(Object o) {
    	List<?> postcards = (List<?>) o;
    	this.postcards = postcards;
    }
    
	public byte[] generateReport() {
		log.debug("starting EventRollupReport");
		
		StringBuffer rpt = new StringBuffer(this.getHeader());
		Iterator<?> iter = postcards.iterator();
		while (iter.hasNext()) {
			DePuyEventPostcardVO postcard = (DePuyEventPostcardVO) iter.next();
			List<DePuyEventEntryVO> events = postcard.getDePuyEvents();
			Iterator<DePuyEventEntryVO> eIter = events.iterator();
			while (eIter.hasNext()) { //for each event on each postcard...
				rpt.append(buildRow((DePuyEventEntryVO)eIter.next(), postcard));
			}
		}
		rpt.append(this.getFooter());
		
		return rpt.toString().getBytes();
	}
	
	private String buildRow(DePuyEventEntryVO vo, DePuyEventPostcardVO pc) {
		Integer leadsCnt = pc.getLeadsCount();
		Integer attendCnt = vo.getAttendanceCnt();
		Integer rsvpCnt = vo.getRsvpTotal();
		CoopAdVO ad = pc.getCoopAd();
		vo.setStatusFlg(pc.getStatusFlg()); //power of a higher authority!
		
		StringBuffer row = new StringBuffer();
		row.append("<tr><td>").append(pc.getProductName()).append("</td>");
		row.append("<td>").append(vo.getRSVPCode()).append("</td>");
		row.append("<td>").append(vo.getStatusName()).append("</td>");
		row.append("<td>").append(pc.getOwner().getFirstName()).append(" ").append(pc.getOwner().getLastName()).append("</td>");
		row.append("<td>").append(vo.getEventName()).append("</td>");
		row.append("<td>").append(vo.getCityName()).append("</td>");
		row.append("<td>").append(vo.getStateCode()).append("</td>");
		row.append("<td>").append(Convert.formatDate(vo.getStartDate(), "MM/dd/yyyy")).append("</td>");
		row.append("<td>").append(vo.getLocationDesc()).append("</td>");
		row.append("<td>").append(vo.getEventDesc()).append("</td>");
		row.append("<td>").append(rsvpCnt).append("</td>");
		row.append("<td>").append(attendCnt).append("</td>");
		row.append("<td>").append(new Float(attendCnt)/new Float(rsvpCnt)*100).append("%</td>");
		row.append("<td>").append(new Float(attendCnt)/new Float(leadsCnt)*100).append("%</td>");
		row.append("<td>").append(leadsCnt).append("</td>");
		row.append("<td>").append((vo.getStatusFlg() == 2) ? "Yes" : "").append("</td>");
		row.append("<td></td>");
		row.append("<td></td>");
		row.append("<td>AD-").append(ad.getStatusFlg()).append("</td>");
		row.append("<td>").append(StringUtil.checkVal(ad.getNewspaper1Text())).append("</td>");
		row.append("<td>").append(StringUtil.checkVal(ad.getNewspaper1Phone())).append("</td>");
		//row.append("<td>").append(StringUtil.checkVal(ad.getNewspaper2Text())).append("</td>");
		//row.append("<td>").append(StringUtil.checkVal(ad.getNewspaper2Phone())).append("</td>");
		//row.append("<td>").append(StringUtil.checkVal(ad.getNewspaper3Text())).append("</td>");
		//row.append("<td>").append(StringUtil.checkVal(ad.getNewspaper3Phone())).append("</td>");
		row.append("<td>").append(StringUtil.checkVal(ad.getApprovedPaperName())).append("</td>");
		row.append("<td>").append(StringUtil.checkVal(ad.getAdDatesText())).append("</td>");
		row.append("<td>").append(StringUtil.checkVal(ad.getTotalCostNo())).append("</td>");
		row.append("<td>").append(StringUtil.checkVal(ad.getCostToRepNo())).append("</td>");
		row.append("<td>").append(StringUtil.checkVal(ad.getTerritoryNo())).append("</td>");
		row.append("<td></td>");
		row.append("<td></td></tr>\r");
		
		return row.toString();
	}

	
	private StringBuffer getHeader() {
		StringBuffer hdr = new StringBuffer();
		hdr.append("<table border='1'>\r");
		hdr.append("<tr><th colspan='29' align='left'><h2>Local Market Seminars Comprehensive Data</h2></th></tr>\r");
		hdr.append("<tr><th>Joint</th>");
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
		//hdr.append("<th>Addtl Newspaper#1</th>");
		//hdr.append("<th>Addtl Newspaper#1 Phone</th>");
		//hdr.append("<th>Addtl Newspaper#2</th>");
		//hdr.append("<th>Addtl Newspaper#2 Phone</th>");
		hdr.append("<th>Approved Newspaper</th>");
		hdr.append("<th>Date Ad will Run</th>");
		hdr.append("<th>Total Ad Cost</th>");
		hdr.append("<th>Cost to Territory</th>");
		hdr.append("<th>Territory No. (for charge-back)</th>");
		hdr.append("<th>Sent Ad Info</th>");
		hdr.append("<th>Ad Received</th>");
		hdr.append("</tr>\r");
		
		return hdr;
	}
	
	private StringBuffer getFooter() {
		return new StringBuffer("</table>");
	}
		
}
