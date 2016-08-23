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
		
		List<Map<String, Object>> rows = new ArrayList<>(leads.size());
		
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
		
		String rsvpDateStr = Convert.formatDate(rsvpDate, "EEEE, MMMM dd, yyyy");
		StringBuilder keySb = new StringBuilder(32);
		StringEncoder se = new StringEncoder();
		EventEntryVO evo = null;
		for (UserDataVO vo : this.leads){
			Map<String, Object> row = new HashMap<>();
		
			row.put("PROFILE_ADDRESSID", StringUtil.checkVal(vo.getProfileId()));
			row.put("TITLE", StringUtil.checkVal(vo.getPrefixName()));
			row.put("FIRST_NAME", StringUtil.checkVal(vo.getFirstName()));
			row.put("LAST_NAME", StringUtil.checkVal(vo.getLastName()));
			row.put("SUFFIX", StringUtil.checkVal(vo.getSuffixName()));
			row.put("ADDRESS1", vo.getAddress());
			row.put("ADDRESS2", StringUtil.checkVal(vo.getAddress2()));
			row.put("CITY", vo.getCity());
			row.put("STATE", vo.getState());
			row.put("ZIP", vo.getZipCode());
			row.put("RSVP_DEADLINE", rsvpDateStr);
			row.put("RSVP_PHONE", "<td>1-800-256-1146</td>");
			
			
			//need both the index and the vo
			for (int x=0; x < events.size(); x++) {
				evo = events.get(x);
			
			row.put(keySb.append("DATE_OF_EVENT_").append(x).toString()
					, Convert.formatDate(evo.getStartDate(), "EEEE, MMMM dd, yyyy"));
				keySb.setLength(0);
			row.put(keySb.append("TIME_").append(x).toString()
					,se.decodeValue(evo.getLocationDesc()));
				keySb.setLength(0);
			row.put(keySb.append("VENUE_NAME_").append(x).toString()
					,se.decodeValue(evo.getEventName()));
				keySb.setLength(0);
			row.put(keySb.append("ADDRESS1_").append(x).toString()
					,se.decodeValue(evo.getAddressText()));
				keySb.setLength(0);
			row.put(keySb.append("ADDRESS2_").append(x).toString()
					,se.decodeValue(evo.getAddress2Text()));
				keySb.setLength(0);
			row.put(keySb.append("CITY_").append(x).toString()
					,se.decodeValue(evo.getCityName()));
				keySb.setLength(0);
			row.put(keySb.append("STATE_").append(x).toString()
					,evo.getStateCode());
				keySb.setLength(0);
			row.put(keySb.append("ZIP_").append(x).toString()
					,evo.getZipCode());
				keySb.setLength(0);
			row.put(keySb.append("SURGEON_NAME_").append(x).toString()
					,lalbelText);
				keySb.setLength(0);
			row.put(keySb.append("EVENTCODE_").append(x).toString()
					,evo.getRSVPCode());
				keySb.setLength(0);
				evo = null;
		}
		
		rows.add(row);
		}

		return rows;
	}

	private HashMap<String, String> getHeader(int eventCnt) {
		
		StringBuilder keySb = new StringBuilder(32);
		
		HashMap<String, String> headerMap = new LinkedHashMap<>();
		
		headerMap.put("PROFILE_ADDRESSID", "Profile Address Id");
		headerMap.put("TITLE", "Title");
		headerMap.put("FIRST_NAME", "First Name");
		headerMap.put("LAST_NAME", "Last Name");
		headerMap.put("SUFFIX", "Suffix");
		headerMap.put("ADDRESS1", "Address1");
		headerMap.put("ADDRESS2", "Address2");
		headerMap.put("CITY", "City");
		headerMap.put("STATE", "State");
		headerMap.put("ZIP", "Zip");
		headerMap.put("RSVP_DEADLINE", "RSVP Deadline");
		headerMap.put("RSVP_PHONE", "RSVP phone");
		
		//use string builder to make a unique key for each events column
		
		for (int x=0; x < eventCnt; x++) {
			headerMap.put(keySb.append("DATE_OF_EVENT_").append(x).toString()
					, "Date of Event");
				keySb.setLength(0);
			headerMap.put(keySb.append("TIME_").append(x).toString()
					, "Time");
				keySb.setLength(0);
			headerMap.put(keySb.append("VENUE_NAME_").append(x).toString()
					, "Venue Name");
				keySb.setLength(0);
			headerMap.put(keySb.append("ADDRESS1_").append(x).toString()
					, "Address1");
				keySb.setLength(0);
			headerMap.put(keySb.append("ADDRESS2_").append(x).toString()
					, "Address2");
				keySb.setLength(0);
			headerMap.put(keySb.append("CITY_").append(x).toString()
					, "City");
				keySb.setLength(0);
			headerMap.put(keySb.append("STATE_").append(x).toString()
					, "State");
				keySb.setLength(0);
			headerMap.put(keySb.append("ZIP_").append(x).toString()
					, "Zip");
				keySb.setLength(0);
			headerMap.put(keySb.append("SURGEON_NAME_").append(x).toString()
					, "Surgeon Name");
				keySb.setLength(0);
			headerMap.put(keySb.append("EVENTCODE_").append(x).toString()
					, "Event Code");
				keySb.setLength(0);
			
		}
		
		return headerMap;
	}
}
