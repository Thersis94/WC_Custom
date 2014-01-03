package com.depuy.events.vo.report;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.depuy.events.vo.DePuyEventPostcardVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;

/*****************************************************************************
 <p><b>Title</b>: LeadAgingReportVO.java</p>
 <p>summarizes leads tied to an Event based on MM/YY of entry into the database.</p>
 <p>Copyright: Copyright (c) 2013 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Oct 11, 2013
 ***************************************************************************/

public class LeadAgingReportVO extends AbstractSBReportVO {
    private static final long serialVersionUID = 1l;
    private DePuyEventPostcardVO postcard;
	private Map<String, Integer> counts = new LinkedHashMap<String, Integer>();

    /**
     * 
     */
    public LeadAgingReportVO() {
        super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
    }
    
    /**
     * Assigns the event postcard data retrieved from the parent action
     * variables
     * @param data (List<DePuyEventPostcardVO>)
     * @throws SQLException
     */
    public void setData(Object o) {
    	this.postcard = (DePuyEventPostcardVO) o;

		//tabulate the data
		counts = tabulateLeads();
		
        setFileName("Leads-Aging-Report " + postcard.getRSVPCodes() + ".xls");
    }
    
	public byte[] generateReport() {
		log.debug("starting generateReport()");
		StringBuilder rpt = new StringBuilder();
		addHeader(rpt);
		
		//print the map into the report
		addMapData(rpt);
		
		addFooter(rpt);
		
		return rpt.toString().getBytes();
	}
	
	/**
	 * counts all the leads by month/year and puts the totals into a Map
	 * @return
	 */
	private Map<String, Integer> tabulateLeads() {
		Iterator<UserDataVO> iter = postcard.getLeadsData().iterator();
		UserDataVO vo = null;
		
		while (iter.hasNext()) {
			vo = (UserDataVO) iter.next();
			String mmyy = Convert.formatDate(vo.getBirthDate(), "MM/yy");
			Integer cnt = counts.get(mmyy);
			if (cnt == null) cnt = Integer.valueOf(0);
			
			counts.put(mmyy, Integer.valueOf(cnt + 1));
			//log.debug("incremented " + mmyy + " to " + (cnt+1));
			vo = null;
		}
		
		return counts;
	}
	
	/**
	 * prints the Map of lead counts into the report
	 * @param rpt
	 */
	private void addMapData(StringBuilder rpt) {
		for (String mmyy: counts.keySet())
			rpt.append("<tr><td>").append(mmyy).append("</td><td>").append(counts.get(mmyy)).append("</td></tr>\r");
		
	}
	
	private void addHeader(StringBuilder hdr) {
		hdr.append("<table border='1'>\r");
		hdr.append("<tr><td colspan=\"2\"><h3>Lead Aging Report for Seminar ");
		hdr.append(postcard.getRSVPCodes()).append("</h3></td></tr>\r");
		hdr.append("<tr style='background-color:#ccc;'>");
		hdr.append("<th>Enrollment Month/Year</th>");
		hdr.append("<th># Leads</th>");
		hdr.append("</tr>\r");
	}
	private void addFooter(StringBuilder hdr) {
		hdr.append("</table>\r");
	}
	
		
		
}
