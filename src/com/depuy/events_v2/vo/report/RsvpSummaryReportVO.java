package com.depuy.events_v2.vo.report;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
import com.siliconmtn.data.report.ExcelReport;
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
		
		ExcelReport rpt = new ExcelReport(this.getHeader());
		
		List<Map<String, Object>> rows = new ArrayList<>(events.size());
		
		rows = generateDataRows(rows);
		
		rpt.setData(rows);
		
		return rpt.generateReport();
				
	}
	

	/**
	 * this method is used to generate the data rows of the excel sheet.
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateDataRows(
			List<Map<String, Object>> rows) {
		
	
		for (EventEntryVO vo : events){
			Map<String, Object> row = new HashMap<String, Object>();
			Integer rsvpTotal = vo.getRsvpTotal("call") + vo.getRsvpTotal("web");
			row.put("SEMINAR_NO", vo.getRSVPCode());
			row.put("SEMINAR_HOST", vo.getContactName());
			row.put("WEB_RSVPS", vo.getRsvpTotal("web"));
			row.put("CALL_IN_RSVPS", vo.getRsvpTotal("call"));
			row.put("TOTAL_RSVPS", rsvpTotal);
			row.put("SEMINAR_DATE", Convert.formatDate(vo.getStartDate(), "EEEE, MMMM dd, yyyy"));
		
			rows.add(row);
		}
		
		
		return rows;
	}

	/**
	 * builds the header map for the excel report
	 * @return
	 */
	private HashMap<String, String> getHeader() {
					
		HashMap<String, String> headerMap = new LinkedHashMap<String, String>();
		headerMap.put("SEMINAR_NO","Seminar#");
		headerMap.put("SEMINAR_HOST","Seminar Host");
		headerMap.put("WEB_RSVPS","Web RSVPs");
		headerMap.put("CALL_IN_RSVPS", "Call-in RSVPs");
		headerMap.put("TOTAL_RSVPS", "Total RSVPs");
		headerMap.put("SEMINAR_DATE", "Seminar Date");
		
		return headerMap;
	}

}
