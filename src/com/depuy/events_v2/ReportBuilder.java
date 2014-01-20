/**
 * 
 */
package com.depuy.events_v2;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.depuy.events_v2.vo.RsvpBreakdownVO;
import com.depuy.events_v2.vo.report.EventPostalLeadsReportVO;
import com.depuy.events_v2.vo.report.EventRollupReportVO;
import com.depuy.events_v2.vo.report.PostcardSummaryReportVO;
import com.depuy.events_v2.vo.report.RsvpBreakdownReportVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ReportBuilder.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 19, 2014
 ****************************************************************************/
public class ReportBuilder extends SBActionAdapter {

	public ReportBuilder(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	public ReportBuilder() {
		super();
	}

	public void generateReport(SMTServletRequest req, Object data) throws ActionException {
		int type = Convert.formatInteger(req.getParameter("rptType"));
		AbstractSBReportVO rpt = null;
		
		switch (type) {
			case 4:
				DePuyEventSeminarVO vo = (DePuyEventSeminarVO) data;
				SiteVO site = (SiteVO ) req.getAttribute(Constants.SITE_DATA);
				vo.setBaseUrl(site.getFullSiteAlias() + "/binary/org/DEPUY/" + site.getSiteId());
				rpt = generateSeminarSummaryReport(vo);
				break;
			case 5:
			case 2:
			case 1:
				DePuyEventSeminarVO sem = (DePuyEventSeminarVO) data; 
				rpt = this.generatePostcardRecipientsReport(sem, Convert.formatDate(req.getParameter("startDate")));
				break;
			case 8:
				rpt = this.generateSeminarRollupReport(Convert.formatDate(req.getParameter("rptStartDate")), Convert.formatDate(req.getParameter("rptEndDate")));
				break;
			case 9:
				rpt = this.generateRSVPBreakdownReport(
							Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("rptStartDate2")), 
							Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("rptEndDate2")));
				break;
//			case 10:
//				rpt = this.generateLeadsAgingReport();
//				break;
		}
		
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
		
	}
	
	
	/**
	 * generates a summary of RSVP referral sources and for each Event in the 
	 * group, counts the number of each type.
	 * @param start
	 * @param end
	 * @return
	 */
	public AbstractSBReportVO generateRSVPBreakdownReport(Date start, Date end) {
		Map<String, RsvpBreakdownVO> data = new HashMap<String, RsvpBreakdownVO>();
		List<String> profileIds = new ArrayList<String>();
		StringBuilder sql = new StringBuilder();
		sql.append("select d.profile_id, a.event_entry_id, a.start_dt, a.rsvp_code_txt, b.referral_txt, count(*) as cnt ");
		sql.append("from event_entry a ");
		sql.append("inner join EVENT_RSVP b on a.EVENT_ENTRY_ID = b.EVENT_ENTRY_ID ");
		sql.append("inner join EVENT_POSTCARD_ASSOC c on a.EVENT_ENTRY_ID = c.EVENT_ENTRY_ID ");
		sql.append("inner join EVENT_POSTCARD d on d.EVENT_POSTCARD_ID = c.EVENT_POSTCARD_ID ");
		sql.append("inner join event_type et on a.event_type_id=et.event_type_id ");
		sql.append("inner join event_group eg on et.action_id=eg.action_id ");
		sql.append("inner join sb_action sb on eg.action_id=sb.action_id ");
		sql.append("where sb.action_group_id=? ");
		if (start != null) sql.append("and b.create_dt >= ? ");
		if (end != null) sql.append("and b.create_dt <= ? ");
		sql.append("group by a.event_entry_id, a.start_dt, a.rsvp_code_txt, d.profile_id, b.referral_txt ");
		sql.append("order by start_dt desc");
		log.debug(sql + "|" + actionInit.getActionId());
		
		int x = 1;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(x++, actionInit.getActionId());
			if (start != null) ps.setDate(x++, Convert.formatSQLDate(start));
			if (end != null) ps.setDate(x++, Convert.formatSQLDate(end));
			
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				String eventId = rs.getString("event_entry_id");
				if (data.containsKey(eventId)) {
					data.get(eventId).addReferralStat(rs.getString("referral_txt"), rs.getInt("cnt"));
				} else {
					//new owner we need to capture
					profileIds.add(rs.getString("profile_id"));
					data.put(eventId, new RsvpBreakdownVO(rs));
				}
			}
			
			//lookup profiles
			ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
			Map<String, UserDataVO> profiles = pm.searchProfileMap(dbConn, profileIds);
			
			//merge owner profiles 
			for(String eventId : data.keySet()) {
				RsvpBreakdownVO vo = data.get(eventId);
				if (profiles.containsKey(vo.getProfileId()))
					vo.setOwner(profiles.get(vo.getProfileId()));
			}
			
		} catch (Exception e) {
			log.error("Error retrieving RSVP Breakdown Report", e);
		} finally { 
			try { ps.close(); } catch(Exception e) { }
		}
		
		RsvpBreakdownReportVO rpt = new RsvpBreakdownReportVO();
		rpt.setData(data.values());
		return rpt;
	}
	
	/**
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	public AbstractSBReportVO generateSeminarRollupReport(Date start, Date end) {
		Object data = null;
		EventRollupReportVO rpt = new EventRollupReportVO();
		rpt.setData(data);
		return null;
	}
	
	
	/**
	 * returns a report of patient leads with their mailing address, to be sent postcards.
	 * @param sem
	 * @param start
	 * @return
	 * @throws ActionException
	 */
	public AbstractSBReportVO generatePostcardRecipientsReport(DePuyEventSeminarVO sem, Date start) 
			throws ActionException {
		LeadsDataToolV2 ldt = new LeadsDataToolV2();
		ldt.setAttributes(attributes);
		ldt.setDBConnection(dbConn);
		
		sem.setLeadsData(ldt.pullLeads(sem, 5, start));
		AbstractSBReportVO rpt = new EventPostalLeadsReportVO();
		rpt.setData(sem);
		return rpt;
	}
	
	
	/**
	 * generates a summary of a single Seminar
	 * @param data
	 * @return
	 */
	public AbstractSBReportVO generateSeminarSummaryReport(Object data) {
		PostcardSummaryReportVO rpt = new PostcardSummaryReportVO();
		rpt.setData(data);
		return rpt;
	}
	
}
