package com.depuy.events_v2;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.depuy.events_v2.vo.AttendeeSurveyVO;
import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.depuy.events_v2.vo.RsvpBreakdownVO;
import com.depuy.events_v2.vo.report.AttendeeSurveyReportVO;
import com.depuy.events_v2.vo.report.ComplianceReportVO;
import com.depuy.events_v2.vo.report.EventPostalLeadsReportVO;
import com.depuy.events_v2.vo.report.LocatorReportVO;
import com.depuy.events_v2.vo.report.PostcardSummaryReportVO;
import com.depuy.events_v2.vo.report.RsvpBreakdownReportVO;
import com.depuy.events_v2.vo.report.RsvpSummaryReportVO;
import com.depuy.events_v2.vo.report.SeminarRollupReportVO;
import com.depuy.events_v2.vo.report.SeminarSummaryReportVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.event.EventRSVPAction;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
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
	
	public enum ReportType {
		mailingList, summary, locator, leads, rsvpSummary,  seminarRollup, 
		rsvpBreakdown, /* leadAging, */ compliance, customSummary, attendeeSurvey
	}

	public ReportBuilder(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	public ReportBuilder() {
		super();
	}

	@SuppressWarnings("incomplete-switch")
	public void generateReport(SMTServletRequest req, Object data) throws ActionException {
		ReportType type = null;
		try {
			type= ReportType.valueOf(req.getParameter("rptType"));
		} catch (Exception e) {
			throw new ActionException("unknown report type", e);
		}
		
		AbstractSBReportVO rpt = null;
		DePuyEventSeminarVO sem = null;
		
		switch (type) {
			case compliance:
				sem = (DePuyEventSeminarVO) data;
				rpt = generateComplianceReport(sem);
				break;
			case summary:
				//test if this is a list or a single vo
				if ( data instanceof List )
					sem = (DePuyEventSeminarVO) ((List<?>) data).get(0);
				else
					sem = (DePuyEventSeminarVO) data;
				SiteVO site = (SiteVO ) req.getAttribute(Constants.SITE_DATA);
				sem.setBaseUrl(site.getFullSiteAlias() + "/binary/org/DEPUY/" + site.getSiteId());
				rpt = generateSeminarSummaryReport(sem);
				break;
			case mailingList:
				sem = (DePuyEventSeminarVO) data; 
				rpt = this.generatePostcardRecipientsReport(sem, Convert.formatDate(req.getParameter("startDate")));
				break;
			case seminarRollup:
				rpt = this.generateSeminarRollupReport(data, Convert.formatDate(req.getParameter("rptStartDate")), Convert.formatDate(req.getParameter("rptEndDate")));
				break;
			case rsvpBreakdown:
				rpt = this.generateRSVPBreakdownReport(
							Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("rptStartDate2")), 
							Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("rptEndDate2")));
				break;
			case rsvpSummary:
				rpt = this.generateRSVPSummaryReport(data);
				break;
//			case leadAging:
//				rpt = this.generateLeadsAgingReport();
//				break;
			
			case locator: 
				rpt = this.generateLocatorReport(data, req.getParameter("radius"));
				break;
			case customSummary:
				rpt = this.generateCustomSeminarReport(req, data);
				break;
			case attendeeSurvey:
				rpt = generateAttendeeSurveyReport(req);
				break;
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
	public AbstractSBReportVO generateSeminarRollupReport(Object listObj, Date start, Date end) {
		@SuppressWarnings("unchecked")
		List<DePuyEventSeminarVO> data = (List<DePuyEventSeminarVO>) listObj;
		List<DePuyEventSeminarVO> finalData = new ArrayList<DePuyEventSeminarVO>(data.size());
		
		PostcardSelectV2 retriever = new PostcardSelectV2(actionInit);
		retriever.setDBConnection(dbConn);
		retriever.setAttributes(attributes);
		
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String actionId = (String) mod.getAttribute(ModuleVO.ATTRIBUTE_1);
		
		//loop through the events, filter by date, then load the coop-ad for each one
		for (DePuyEventSeminarVO sem : data) {
			Date eDate = sem.getEarliestEventDate();
			if ((start != null && eDate.before(start)) || (end != null && eDate.after(end))) continue;
			
			try {
				DePuyEventSeminarVO semFull  = (DePuyEventSeminarVO) retriever.loadOneSeminar(sem.getEventPostcardId(), actionId, null, null, null);
				semFull.setRsvpCount(sem.getRsvpCount()); //this only gets set on the initial query, not the detailed lookup
				finalData.add(semFull);
			} catch (SQLException e) {
				log.error("could not load seminar " + sem.getEventPostcardId(), e);
			}
		}
		
		SeminarRollupReportVO rpt = new SeminarRollupReportVO();
		rpt.setData(finalData);
		return rpt;
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
		
		sem.setLeadsData(ldt.pullLeads(sem, ReportType.mailingList, start));
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
	
	
	/**
	 * generates a PDF version of the compliance form, customized for the Seminar
	 * @param data
	 * @return
	 */
	public AbstractSBReportVO generateComplianceReport(Object data) {
		AbstractSBReportVO rpt = new ComplianceReportVO();
		rpt.setData(data);
		return rpt;
	}
	
	@SuppressWarnings("unchecked")
	public AbstractSBReportVO generateRSVPSummaryReport(Object data) {
		RsvpSummaryReportVO rpt = new RsvpSummaryReportVO();
		List<EventEntryVO> eventsList = new ArrayList<EventEntryVO>();
		List<DePuyEventSeminarVO> seminars = (List<DePuyEventSeminarVO>) data;
		
		EventRSVPAction rsvpAction = new EventRSVPAction(this.actionInit);
		rsvpAction.setAttributes(this.attributes);
		rsvpAction.setDBConnection(dbConn);
		
		for (DePuyEventSeminarVO sem : seminars) {
			for (EventEntryVO  event: sem.getEvents()) {
				//call the RSVP action for summations by event
				event.setContactName(sem.getOwner().getFirstName() + " " + sem.getOwner().getLastName());
				event.setRsvpSummary(rsvpAction.rsvpSummary(event.getActionId()));
				eventsList.add(event);
			}
		}
		rpt.setData(eventsList);
		
		return rpt;
	}
	
	
	public AbstractSBReportVO generateLocatorReport(Object data, String radius) {
		LocatorReportVO rpt = new LocatorReportVO();
		rpt.setData(data);
		rpt.setRadius(radius);
		return rpt;
	}
	
	/**
	 * Used for building a report that summarizes all seminars with specific
	 * fields included.
	 * @param req
	 * @param data
	 * @return
	 */
	public AbstractSBReportVO generateCustomSeminarReport(SMTServletRequest req, Object data ){
		SeminarSummaryAction ssa = new SeminarSummaryAction(this.actionInit);
		ssa.setAttributes(this.attributes);
		ssa.setDBConnection(dbConn);
		SeminarSummaryReportVO rpt = null;
		
		String reportId = StringUtil.checkVal( req.getParameter("reportId") );
		if ( reportId.isEmpty() ){
			//use current values
			rpt = new SeminarSummaryReportVO(req);
			rpt.setData(data);
		} else {
			//use saved parameters
			try {
				rpt = ssa.getSavedReport(reportId);
				rpt.setData(data); 
			} catch (InvalidDataException | SQLException e) {
				log.error(e);
			}
		}
		return rpt;
	}

	/**
	 * Generates a report listing survey attendees and their respective answers
	 * to the seminar attendee survey
	 * @param req
	 * @return
	 */
	public AbstractSBReportVO generateAttendeeSurveyReport(SMTServletRequest req){
		final String dfSchema = "DATA_FEED.dbo."; //Hard coded here because the constant in ReportFacadeAction is not visible to this class
		List<AttendeeSurveyVO> voList = new ArrayList<>();
		//Concatenated string of rsvp codes to include in the report.
		//Assume all to be included if no code is specified
		String rsvpCodeString = StringUtil.checkVal( req.getParameter("attendeeSurveySeminars") );
		String [] rsvpCodes = rsvpCodeString.split(";");
		Boolean selectAll = Convert.formatBoolean( req.getParameter("allAttendeeSeminars") );
		
		StringBuilder sql = new StringBuilder();
		sql.append("select c.CUSTOMER_ID, c.CALL_SOURCE_CD, c.SELECTION_CD, ");
		sql.append("cr.RESPONSE_TXT, q.QUESTION_TXT, p.FIRST_NM, p.LAST_NM, p.PROFILE_ID ");
		sql.append("from ").append(dfSchema).append("CUSTOMER c ");
		sql.append("inner join ").append(dfSchema).append("CUSTOMER_RESPONSE cr on c.CUSTOMER_ID=cr.CUSTOMER_ID ");
		sql.append("inner join ").append(dfSchema).append("QUESTION_MAP qm on qm.QUESTION_MAP_ID=cr.QUESTION_MAP_ID ");
		sql.append("inner join ").append(dfSchema).append("QUESTION q on q.QUESTION_ID=qm.QUESTION_ID ");
		sql.append("left join PROFILE p on p.PROFILE_ID=c.PROFILE_ID ");
		sql.append("where c.CALL_SOURCE_CD='EVENT' ");
		
		//set the specific events, if any exists
		if ( selectAll ){
			sql.append("and c.SELECTION_CD like 'EVENT_%' ");
		} else {
			sql.append(" and c.SELECTION_CD in ( ");
			for ( int i = 0 ; i < rsvpCodes.length; i++ ){
				sql.append("?");
				if ( i+1 < rsvpCodes.length )
					sql.append(",");
			}
			sql.append(" ) ");
		}
		sql.append("order by c.SELECTION_CD, c.CUSTOMER_ID");
		log.debug(sql);

		try( PreparedStatement ps = dbConn.prepareStatement(sql.toString() )){
			int i = 0;
			if (! selectAll )
				for ( String s : rsvpCodes ){
					ps.setString(++i, s);
				}
			ResultSet rs = ps.executeQuery();
			//Needed to decrypt name values
			ProfileManager pm = ProfileManagerFactory.getInstance(this.attributes);
			AttendeeSurveyVO vo = null;
			
			String oldCust = null;
			String newCust = null;
			
			while( rs.next() ){
				newCust = rs.getString("customer_id");
				
				//if customer_id differs, we are on to a new person
				if( ! StringUtil.checkVal(oldCust).equals(StringUtil.checkVal(newCust))){
					//don't add empty vo
					if(vo != null)
						voList.add(vo);
					vo = new AttendeeSurveyVO();
					vo.setProfileId(rs.getString("profile_id"));
					//get the rsvp code by trimming the selection_cd prefix
					vo.setRsvpCode( StringUtil.checkVal(rs.getString("selection_cd")).replaceFirst("EVENT_", "") );
					vo.setFirstName( StringUtil.checkVal(
							pm.getStringValue("FIRST_NM", rs.getString("first_nm")), "N/A" ) );
					vo.setLastName( StringUtil.checkVal(pm.getStringValue("LAST_NM", rs.getString("last_nm")), "N/A" ) );
				} 
				//otherwise, just add this q and a pair to the vo
				vo.getQuestionList().add(rs.getString("question_txt"));
				vo.getAnswerList().add(StringUtil.checkVal(rs.getString("response_txt"), "N/A" ) );
				
				oldCust = newCust;
			}
			//add trailing entry
			if (vo != null) { voList.add(vo); }
			
		} catch (SQLException e){
			log.error(e);
			return null;
		}
		
		AttendeeSurveyReportVO rpt = new AttendeeSurveyReportVO();
		rpt.setData(voList);
		return rpt;
	}
}
