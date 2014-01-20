package com.depuy.events_v2.vo.report;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.depuy.events_v2.vo.RsvpBreakdownVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;
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
	    			if (!referrers.contains(stat))  {
	    				if (stat == null || stat.equals("")) stat = "No Referrer";
	    				referrers.add(stat);
	    			}
	    		}
	    	}
	}

	public byte[] generateReport() {
		log.debug("starting generateReport()");
		StringBuffer rpt = new StringBuffer(this.getHeader());
		
		for (RsvpBreakdownVO vo : events) {
			//summate the overall total RSVPs
			int rsvpTotal = 0;
			log.debug(vo.getRsvpCode());
			for (Integer cnt : vo.getReferralStats().values()) {
				log.debug(cnt);
				rsvpTotal += cnt;
			}
			
			rpt.append("<tr><td>").append(vo.getRsvpCode()).append("</td>");
			rpt.append("<td>").append(vo.getOwner().getFirstName()).append(" ").append(vo.getOwner().getLastName()).append("</td>");
			rpt.append("<td>").append(Convert.formatDate(vo.getSeminarDate(), "MM/dd/yyyy")).append("</td>");

			Map<String, Integer> rsvpStats = vo.getReferralStats();
			for (String stat : referrers) {
				if (rsvpStats.containsKey(stat)) {
					Integer cnt = rsvpStats.get(stat);
					rpt.append("<td>").append(cnt).append("</td>");
					float percent = (Float.valueOf(cnt) / Float.valueOf(rsvpTotal)) * 100;
					rpt.append("<td>").append(Math.round(percent)).append("%</td>");
				} else {
					rpt.append("<td>0</td><td>0</td>");
				}
			}

			rpt.append("<td>").append(rsvpTotal).append("</td>");
			rpt.append("</tr>\r");
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

		for (String stat : referrers) {
			hdr.append("<th nowrap>").append(stat).append("</th>");
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
