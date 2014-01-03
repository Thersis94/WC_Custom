package com.depuy.events.vo.report;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import com.depuy.events.vo.DePuyEventEntryVO;
import com.depuy.events.vo.DePuyEventPostcardVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/*****************************************************************************
 <p><b>Title</b>: EventPostalLeadsReportVO.java</p>
 <p>compiles leads data for postal and email sends</p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Nov 6, 2006
 ***************************************************************************/

public class EventPostalLeadsReportVO extends AbstractSBReportVO {
    private static final long serialVersionUID = 1l;
    private List<DePuyEventEntryVO> events = new ArrayList<DePuyEventEntryVO>();
    private List<UserDataVO> leads = new ArrayList<UserDataVO>();
    private Date rsvpDate = new Date();

    /**
     * 
     */
    public EventPostalLeadsReportVO() {
        super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName("Leads-Report.xls");
    }
    
    /**
     * Assigns the event postcard data retrieved from the parent action
     * variables
     * @param data (List<DePuyEventPostcardVO>)
     * @throws SQLException
     */
    public void setData(Object o) {
    	DePuyEventPostcardVO postcard = (DePuyEventPostcardVO) o;
    	this.events = postcard.getDePuyEvents();
    	this.leads = postcard.getLeadsData();
    	rsvpDate = postcard.getRSVPDate();
    }
    
	public byte[] generateReport() {
		log.debug("starting generateReport()");
		StringBuilder rpt = new StringBuilder(this.getHeader(events.size()));
		Iterator<UserDataVO> iter = leads.iterator();
		UserDataVO vo = null;
		String eventsData = this.buildEvents(); //only needs called once
		String rsvpDateStr = Convert.formatDate(rsvpDate, "EEEE, MMMM dd, yyyy");
		List<String> dupls = new ArrayList<String>(leads.size());

		while (iter.hasNext()) {
			vo = (UserDataVO) iter.next();
			String key = "";
			try {
				key = vo.getFirstName().concat(vo.getLastName()).concat(vo.getAddress());
			} catch (Exception e) {}
			
			if (dupls.contains(key)) {
				continue;
			} else {
				dupls.add(key);
			}
			
			rpt.append("<tr><td>").append(StringUtil.checkVal(vo.getPrefixName())).append("</td>");
			rpt.append("<td>").append(StringUtil.checkVal(vo.getFirstName())).append("</td>");
			rpt.append("<td>").append(StringUtil.checkVal(vo.getLastName())).append("</td>");
			rpt.append("<td>").append(StringUtil.checkVal(vo.getSuffixName())).append("</td>");
			rpt.append("<td>").append(vo.getAddress()).append("</td>");
			rpt.append("<td>").append(StringUtil.checkVal(vo.getAddress2())).append("</td>");
			rpt.append("<td>").append(vo.getCity()).append("</td>");
			rpt.append("<td>").append(vo.getState()).append("</td>");
			rpt.append("<td type='text'>").append(vo.getZipCode()).append("</td>");
			rpt.append("<td>").append(rsvpDateStr).append("</td>");
			rpt.append("<td>1-800-256-1146</td>");
			rpt.append(eventsData).append("</tr>\r");
			vo = null;
		}

		rpt.append(this.getFooter());
		return rpt.toString().getBytes();
	}
	
	private StringBuilder getHeader(int eventCnt) {
		StringBuilder hdr = new StringBuilder();
		hdr.append("<table border='1'>\r<tr style='background-color:#ccc;'>");
		hdr.append("<th>Title</th>");
		hdr.append("<th>First_Name</th>");
		hdr.append("<th>Last_Name</th>");
		hdr.append("<th>Suffix</th>");
		hdr.append("<th>Address1</th>");
		hdr.append("<th>Address2</th>");
		hdr.append("<th>City</th>");
		hdr.append("<th>State</th>");
		hdr.append("<th>Zip</th>");
		hdr.append("<th>RSVP_Deadline</th>");
		hdr.append("<th>RSVP_phone</th>");
		
		for (int x=0; x < eventCnt; x++) {
			hdr.append("<th>Date of Event</th>");
			hdr.append("<th>Time</th>");
			hdr.append("<th>Venue Name</th>");
			hdr.append("<th>Address1</th>");
			hdr.append("<th>Address2</th>");
			hdr.append("<th>City</th>");
			hdr.append("<th>State</th>");
			hdr.append("<th>Zip</th>");
			hdr.append("<th>Surgeon Name</th>");
			hdr.append("<th>EventCode</th>");
		}
		hdr.append("</tr>\r");
		
		return hdr;
	}
	
	
	private String buildEvents() {
		StringBuilder row = new StringBuilder();
		StringEncoder se = new StringEncoder();
		DePuyEventEntryVO event = null;
		Iterator<DePuyEventEntryVO> iter = this.events.iterator();
		while (iter.hasNext()) {
			event = (DePuyEventEntryVO) iter.next();
			row.append("<td>").append(Convert.formatDate(event.getStartDate(), "EEEE, MMMM dd, yyyy")).append("</td>");
			row.append("<td>").append(se.decodeValue(event.getLocationDesc())).append("</td>");
			row.append("<td>").append(se.decodeValue(event.getEventName())).append("</td>");
			row.append("<td>").append(se.decodeValue(event.getAddressText())).append("</td>");
			row.append("<td>").append(se.decodeValue(event.getAddress2Text())).append("</td>");
			row.append("<td>").append(se.decodeValue(event.getCityName())).append("</td>");
			row.append("<td>").append(event.getStateCode()).append("</td>");
			row.append("<td>").append(event.getZipCode()).append("</td>");
			row.append("<td>").append(event.getEventDescFinal()).append("</td>");
			row.append("<td>").append(event.getRSVPCode()).append("</td>");
		}
		return row.toString();
	}
	
	private StringBuilder getFooter() {
		return new StringBuilder("</table>");
	}
		
}
