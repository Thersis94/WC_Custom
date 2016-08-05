package com.depuy.events_v2.vo.report;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/*****************************************************************************
 <p><b>Title</b>: EventPostalLeadsReportVO</p>
 <p>compiles leads data for postal and email sends</p>
 <p>Copyright: Copyright (c) 2000 - 2014 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Jan 20, 2014
 ***************************************************************************/

public class EventPostalLeadsReportVO extends AbstractSBReportVO {
    private static final long serialVersionUID = 1l;
    private List<EventEntryVO> events = new ArrayList<EventEntryVO>();
    private List<UserDataVO> leads = new ArrayList<UserDataVO>();
    private Date rsvpDate = null;;
    private String lalbelText = "";

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
    	DePuyEventSeminarVO postcard = (DePuyEventSeminarVO) o;
    	this.events = postcard.getEvents();
    	this.leads = postcard.getLeadsData();
    	rsvpDate = postcard.getRSVPDate();
    	lalbelText = postcard.getLabelText();
    }
    
	public byte[] generateReport() {
		log.debug("starting generateReport()");
		
		ExcelReport rpt = new ExcelReport(this.getHeader(events.size()));
		
		List<Map<String, Object>> rows = new ArrayList<>(events.size());
		
		rpt.setTitleCell(getTitleNote());
		
		rows = generateDataRows(rows);
		
		rpt.setData(rows);
		
		return rpt.generateReport();
		
/*		
		StringBuilder rpt = new StringBuilder(this.getHeader(events.size()));
		String eventsData = this.buildEvents(); //only needs called once
		String rsvpDateStr = Convert.formatDate(rsvpDate, "EEEE, MMMM dd, yyyy");
		
		for (UserDataVO vo : leads) {
			rpt.append("<tr><td>").append(StringUtil.checkVal(vo.getProfileId())).append("</td>");
			rpt.append("<td>").append(StringUtil.checkVal(vo.getPrefixName())).append("</td>");
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
		return rpt.toString().getBytes();*/
	}
	
	private HashMap<String, String> getHeader(int eventCnt) {
		
		HashMap<String, String> headerMap = new LinkedHashMap<String, String>();
		
		headerMap.put("PROFILE_ADDRESSID", "Profile Address Id");
		headerMap.put("TITLE", "Title");
		headerMap.put("FIRST_NAME", "First Name");
		headerMap.put("LAST_NAME", "Last Name");
		headerMap.put("SUFFIX", "Suffix");
		headerMap.put("ADDRESS1", "Address1");
		headerMap.put("ADDRESS2", "Address2");
		headerMap.put("CITY", "City");
		headerMap.put("STATE", "State");
		headerMap.put("RSVP_DEADLINE", "RSVP Deadline");
		headerMap.put("RSVP_PHONE", "RSVP phone");
		
		for (int x=0; x < eventCnt; x++) {
			headerMap.put("DATE_OF_EVENT", "Date of Event");
			headerMap.put("TIME", "Time");
			headerMap.put("VENUE_NAME", "Venue Name");
			headerMap.put("ADDRESS1", "Address1");
			headerMap.put("ADDRESS2", "Address2");
			headerMap.put("CITY", "City");
			headerMap.put("STATE", "State");
			headerMap.put("ZIP", "Zip");
			headerMap.put("SURGEON_NAME", "Surgeon Name");
			headerMap.put("EVENTCODE", "Event Code");
			
		}
		
		return headerMap;
	}
	
	
	private String buildEvents() {
		StringBuilder row = new StringBuilder();
		StringEncoder se = new StringEncoder();
		for (EventEntryVO event : this.events) {
			row.append("<td>").append(Convert.formatDate(event.getStartDate(), "EEEE, MMMM dd, yyyy")).append("</td>");
			row.append("<td>").append(se.decodeValue(event.getLocationDesc())).append("</td>");
			row.append("<td>").append(se.decodeValue(event.getEventName())).append("</td>");
			row.append("<td>").append(se.decodeValue(event.getAddressText())).append("</td>");
			row.append("<td>").append(se.decodeValue(event.getAddress2Text())).append("</td>");
			row.append("<td>").append(se.decodeValue(event.getCityName())).append("</td>");
			row.append("<td>").append(event.getStateCode()).append("</td>");
			row.append("<td>").append(event.getZipCode()).append("</td>");
			row.append("<td>").append(lalbelText).append("</td>");
			row.append("<td>").append(event.getRSVPCode()).append("</td>");
		}
		return row.toString();
	}
	
	private StringBuilder getFooter() {
		return new StringBuilder("</table>");
	}
		
}
