package com.depuy.events.vo.report;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import com.depuy.events.vo.DePuyEventEntryVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;
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

public class RsvpBreakdownReportVO extends AbstractSBReportVO {
    private static final long serialVersionUID = 1l;
    private List<DePuyEventEntryVO> events = new ArrayList<DePuyEventEntryVO>();
    private List<String> referalTypes = new ArrayList<String>();

    /**
     * 
     */
    public RsvpBreakdownReportVO() {
        super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName("RSVP-Breakdown.xls");
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
    	this.events = (List<DePuyEventEntryVO>) events;
    	
    	Iterator<?> iter = events.iterator();
    	while (iter.hasNext()) {
    		DePuyEventEntryVO vo = (DePuyEventEntryVO) iter.next();
    		Iterator<String> refIter = vo.getRsvpSummary().keySet().iterator();
    		while (refIter.hasNext()) {
    			String referer = refIter.next();
    			if (!referalTypes.contains(referer)) 
    				referalTypes.add(referer);
    		}
    	}
    	
    	log.debug("referalListSize=" + referalTypes.size());
    }
    
	public byte[] generateReport() {
		log.debug("starting generateReport()");
		StringBuffer rpt = new StringBuffer(this.getHeader());
		DePuyEventEntryVO vo = null;
		Integer rowNo = 2;
		//for each postcard
		Iterator<DePuyEventEntryVO> iter = events.iterator();
		while (iter.hasNext()) {
			vo = iter.next();
			Map<String, Integer> rsvpStats = vo.getRsvpSummary();
			Integer rsvpTotal = 0;
			
			rpt.append("<tr><td>").append(vo.getRSVPCode()).append("</td>");
			rpt.append("<td>").append(vo.getContactName()).append("</td>");
			rpt.append("<td>").append(Convert.formatDate(vo.getStartDate(), "MM/dd/yyyy")).append("</td>");
			
			Iterator<String> refIter = referalTypes.iterator();
			while (refIter.hasNext()) {
				String key = refIter.next();
				if (rsvpStats.containsKey(key)) {
					Integer cnt = rsvpStats.get(key);
					rsvpTotal += cnt;
					rpt.append("<td>").append(cnt).append("</td>");
					rpt.append("<td>").append(new Float(cnt)/new Float(vo.getRsvpTotal())*100).append("%</td>");
				} else {
					rpt.append("<td>0</td><td>0</td>");
				}
			}
			
			
			rpt.append("<td>").append(rsvpTotal).append("</td>");
			rpt.append("</tr>\r");
			rowNo++;
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
		hdr.append("<th>Seminar_Date</th>");
		
		Iterator<String> iter = referalTypes.iterator();
		while (iter.hasNext()) {
			hdr.append("<th nowrap>").append(iter.next()).append("</th>");
			hdr.append("<th>% of Total</th>");
		}
		hdr.append("<th>Total_RSVPs</th>");
		hdr.append("</tr>\r");
		
		return hdr;
	}

	
	private StringBuffer getFooter() {
		return new StringBuffer("</table>");
	}
		
}
