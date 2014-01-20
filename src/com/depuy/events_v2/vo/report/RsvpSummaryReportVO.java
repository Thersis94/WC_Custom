package com.depuy.events_v2.vo.report;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
import com.siliconmtn.util.Convert;

/*****************************************************************************
 <p><b>Title</b>: RsvpSummaryReportVO.java</p>
 <p>generates a table of active events and # of rsvps (by type) for each.</p>
 <p>Copyright: Copyright (c) 2000 - 2008 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Mar 24, 2008
 ***************************************************************************/

public class RsvpSummaryReportVO extends AbstractSBReportVO {
    private static final long serialVersionUID = 1l;
    private List<EventEntryVO> events = new ArrayList<EventEntryVO>();

    /**
     * 
     */
    public RsvpSummaryReportVO() {
        super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName("RSVP-Summary.xls");
    }
    
    /**
     * Assigns the event postcard data retrieved from the parent action
     * variables
     * @param data (List<DePuyEventPostcardVO>)
     * @throws SQLException
     */
    @SuppressWarnings("unchecked")
    public void setData(Object o) {
    	List<?> events = (List<?>) o;
    	this.events = (List<EventEntryVO>) events;
    }
    
	public byte[] generateReport() {
		log.debug("starting generateReport()");
		StringBuffer rpt = new StringBuffer(this.getHeader());
		EventEntryVO vo = null;

		//for each postcard
		Iterator<EventEntryVO> iter = events.iterator();
		while (iter.hasNext()) {
			vo = iter.next();
			Integer rsvpTotal = vo.getRsvpTotal("call") + vo.getRsvpTotal("web");

			rpt.append("<tr><td>").append(vo.getRSVPCode()).append("</td>");
			rpt.append("<td>").append(vo.getContactName()).append("</td>");
			rpt.append("<td>").append(vo.getRsvpTotal("web")).append("</td>");
			rpt.append("<td>").append(vo.getRsvpTotal("call")).append("</td>");
			rpt.append("<td>").append(rsvpTotal).append("</td>");
			rpt.append("<td>").append(Convert.formatDate(vo.getStartDate(), "EEEE, MMMM dd, yyyy")).append("</td>");
			rpt.append("</tr>\r");
			vo = null;
		}
		
		rpt.append(this.getFooter());
		return rpt.toString().getBytes();
	}
	
	private StringBuffer getHeader() {
		StringBuffer hdr = new StringBuffer();
		hdr.append("<table border='1'>\r<tr style='background-color:#ccc;'>");
		hdr.append("<th>Seminar#</th>");
		hdr.append("<th>Seminar_Host</th>");
		hdr.append("<th>Web_RSVPs</th>");
		hdr.append("<th>Call-in_RSVPs</th>");
		hdr.append("<th>Total_RSVPs</th>");
		hdr.append("<th>Seminar_Date</th>");
		hdr.append("</tr>\r");
		
		return hdr;
	}

	
	private StringBuffer getFooter() {
		return new StringBuffer("</table>");
	}
		
}
