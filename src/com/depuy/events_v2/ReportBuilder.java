package com.depuy.events_v2;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.depuy.events_v2.PostcardSelectV2.ReqType;
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
import com.depuy.events_v2.vo.report.CustomReportVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.action.ActionRequest;
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
		mailingList(true), summary(true), locator(true), mitekLocator(true), leads(true), 
		rsvpSummary(true),  seminarRollup(true), rsvpBreakdown(false), 
		compliance(true), customReport(true), attendeeSurvey(false);

		//signals the controller whether we need it to load Seminar data before calling ReportBuilder
		private boolean requiresSeminar = false;  

		ReportType(boolean rs) {
			this.requiresSeminar = rs;
		}
		public boolean requiresSeminar() { return requiresSeminar; }
	}

	public ReportBuilder(ActionInitVO actionInit) {
		super(actionInit);
	}

	public ReportBuilder() {
		super();
	}

	@SuppressWarnings("incomplete-switch")
	public void generateReport(ActionRequest req, Object data) throws ActionException {
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
				rpt = generatePostcardRecipientsReport(sem, Convert.formatDate(req.getParameter("startDate")));
				break;
			case seminarRollup:
				rpt = generateSeminarRollupReport(data, Convert.formatDate(req.getParameter("rptStartDate")), 
						Convert.formatDate(req.getParameter("rptEndDate")), req.hasParameter("isMitek"));
				break;
			case rsvpBreakdown:
				rpt = generateRSVPBreakdownReport(
						Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("rptStartDate2")), 
						Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("rptEndDate2")));
				break;
			case rsvpSummary:
				rpt = generateRSVPSummaryReport(data);
				break;
				//			case leadAging:
				//				rpt = this.generateLeadsAgingReport();
				//				break;

			case locator: 
				rpt = generateLocatorReport(data, req.getParameter("radius"));
				break;
			case mitekLocator: 
				rpt = generateLocatorReportMitek(data, req.getParameter("radius"));
				break;
			case attendeeSurvey:
				rpt = generateAttendeeSurveyReport(req);
				break;
			case customReport:
				rpt = generateCustomSeminarReport(req, data);
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
		rpt.setStart(start);
		rpt.setEnd(end);
		return rpt;
	}


	/**
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	public AbstractSBReportVO generateSeminarRollupReport(Object listObj, Date start, Date end, boolean isMitek) {
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
				DePuyEventSeminarVO semFull  = (DePuyEventSeminarVO) retriever.loadOneSeminar(sem.getEventPostcardId(), actionId, ReqType.report , null, null, -1);
				semFull.setRsvpCount(sem.getRsvpCount()); //this only gets set on the initial query, not the detailed lookup
				finalData.add(semFull);
			} catch (SQLException e) {
				log.error("could not load seminar " + sem.getEventPostcardId(), e);
			}
		}

		retriever.loadSurveyReponses(finalData, isMitek);
		
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
		LeadsDataToolV2 leads = LeadsDataToolV2.newInstance(sem, actionInit);
		leads.setAttributes(attributes);
		leads.setDBConnection(dbConn);

		sem.setLeadsData(leads.pullLeads(sem, ReportType.mailingList, start));
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
		rpt.setAamdUrl((String)getAttribute("aamdUrl"));
		return rpt;
	}
	public AbstractSBReportVO generateLocatorReportMitek(Object data, String radius) {
		return generateLocatorReport(data, radius); //they're the same now.  -JM 12.27.2016
	}

	/**
	 * Used for building a report that summarizes all seminars with specific
	 * fields included.
	 * @param req
	 * @param data
	 * @return
	 */
	public AbstractSBReportVO generateCustomSeminarReport(ActionRequest req, Object data) {
		CustomReportVO rpt = new CustomReportVO(req);
		rpt.setData(data);
		return rpt;
	}

	/**
	 * Generates a report listing survey attendees and their respective answers
	 * to the seminar attendee survey.  These are the feedback surveys collected
	 * at the seminar and mailed to TMS, then fed to SMT via the data_feed system.
	 * @param req
	 * @return
	 */
	public AbstractSBReportVO generateAttendeeSurveyReport(ActionRequest req) {
		final String dfSchema = (String)attributes.get(Constants.DATA_FEED_SCHEMA);
		Set<String> profileIds = new HashSet<>();
		Map<String, AttendeeSurveyVO> data = new LinkedHashMap<>(); //keep in the order defined by the query
		Map<String, String> questionMap = new LinkedHashMap<>(); //keep in order defined by the query
		questionMap.put("RSVP_CD",  "Seminar #");
		questionMap.put("SEM_DT",  "Seminar Date");
		questionMap.put("PRODUCT_CD",  "Primary Joint");
		questionMap.put("NAME", "Name");
		questionMap.put("ADDRESS", "Address");
		questionMap.put("ADDRESS2","Address2");
		questionMap.put("CITY", "City");
		questionMap.put("STATE","State");
		questionMap.put("ZIP","Zip");
		questionMap.put("PHONE", "Phone");
		questionMap.put("EMAIL", "Email");
		questionMap.put("BIRTH_YR",  "Birth Year");
		questionMap.put("GENDER",  "Gender");
		questionMap.put("REFERER",  "Referral Source");
		questionMap.put("TARGET",  "Call Target");
		questionMap.put("INFO_KIT",  "Previously Received Info-Kit?");
		
		
		//Concatenated string of rsvp codes to include in the report.
		//Assume all to be included if no code is specified
		String [] rsvpCodes = req.getParameterValues("surveySeminars");
		Boolean selectAll = Convert.formatBoolean(req.getParameter("allSurveySeminars"));

		StringBuilder sql = new StringBuilder(500);
		sql.append("select c.customer_id, c.SELECTION_CD, c.product_cd, c.profile_id, c.lead_type_id, ");
		sql.append("case c.call_reason_cd when 'OTHER' then c.call_reason_other_txt else call_reason_cd end as call_reason_cd, ");
		sql.append("c.call_target_cd, c.attempt_dt, cr.RESPONSE_TXT, qm.QUESTION_CD, q.QUESTION_TXT ");
		sql.append("from ").append(dfSchema).append("CUSTOMER c ");
		sql.append("inner join ").append(dfSchema).append("CUSTOMER_RESPONSE cr on c.CUSTOMER_ID=cr.CUSTOMER_ID ");
		sql.append("inner join ").append(dfSchema).append("QUESTION_MAP qm on qm.QUESTION_MAP_ID=cr.QUESTION_MAP_ID ");
		sql.append("inner join ").append(dfSchema).append("QUESTION q on q.QUESTION_ID=qm.QUESTION_ID ");
		sql.append("where c.CALL_SOURCE_CD='EVENT' ");

		//set the specific events, if any exists
		if (selectAll) {
			sql.append("and c.SELECTION_CD like 'EVENT_[0-9][0-9][0-9][0-9]' "); //only ones with four trailing digits are patient seminars
		} else {
			sql.append(" and c.SELECTION_CD in (");
			for (int i=0; i < rsvpCodes.length; i++) {
				if (i > 0) sql.append(",");
				sql.append("?");
			}
			sql.append(") ");
		}
		sql.append("order by c.SELECTION_CD, c.CUSTOMER_ID, q.question_txt");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			if (!selectAll) {
				int i = 0;
				for (String s : rsvpCodes) {
					ps.setString(++i, s);
				}
			}
			ResultSet rs = ps.executeQuery();
			AttendeeSurveyVO vo = null;
			while (rs.next()) {
				String questCd = rs.getString("question_cd");
				//maintain a master list of questions, these will become column headings in the report
				if (!questionMap.containsKey(questCd))
					questionMap.put(questCd, rs.getString("question_txt"));

				//when customer_id changes, we're on to the next person
				String customerId = rs.getString("customer_id");
				if (data.containsKey(customerId)) {
					vo = data.get(customerId);
				} else {
					vo = new AttendeeSurveyVO();
					vo.setProfileId(rs.getString("profile_id"));
					profileIds.add(vo.getProfileId());
					//get the rsvp code by trimming the selection_cd prefix
					vo.setRsvpCode(StringUtil.checkVal(rs.getString("selection_cd")).replaceFirst("EVENT_0", "").replaceFirst("EVENT_",""));
					vo.addResponse("RSVP_CD", vo.getRsvpCode()); //for when we pass the map to ExcelReport
					vo.addResponse("SEM_DT",  Convert.formatDate(rs.getDate("attempt_dt"), Convert.DATE_SLASH_PATTERN));
					vo.addResponse("PRODUCT_CD",  rs.getString("PRODUCT_CD"));
					vo.addResponse("REFERER",  StringUtil.checkVal(rs.getString("call_reason_cd")));
					vo.addResponse("TARGET",  rs.getString("call_target_cd"));
					vo.addResponse("INFO_KIT", (rs.getInt("lead_type_id") == 1 ? "Yes" : "No"));
				}
				//add this Q&A pair to the vo
				vo.addResponse(questCd, StringUtil.checkVal(rs.getString("response_txt")));

				//put the vo back on our data Map
				data.put(customerId, vo);
			}

		} catch (SQLException sqle) {
			log.error("could not load survey responses", sqle);
		}
		
		//lookup profiles for all users
		ProfileManager pm = ProfileManagerFactory.getInstance(this.attributes);
		try {
			Map<String, UserDataVO> users = pm.searchProfileMap(dbConn, new ArrayList<String>(profileIds));
			//tie a profile back to each survey response
			for (String customerId : data.keySet()) {
				AttendeeSurveyVO vo = data.get(customerId);
				UserDataVO user = users.get(vo.getProfileId());
				if (user == null) continue;
				vo.addResponse("NAME", user.getFullName());
				vo.addResponse("ADDRESS", StringUtil.checkVal(user.getAddress()));
				vo.addResponse("ADDRESS2", StringUtil.checkVal(user.getAddress2()));
				vo.addResponse("CITY", StringUtil.checkVal(user.getCity()));
				vo.addResponse("STATE", StringUtil.checkVal(user.getState()));
				vo.addResponse("ZIP", StringUtil.checkVal(user.getZipCode()));
				vo.addResponse("PHONE", StringUtil.checkVal(user.getMainPhone()));
				vo.addResponse("EMAIL", StringUtil.checkVal(user.getEmailAddress()));
				vo.addResponse("BIRTH_YR", StringUtil.checkVal(user.getBirthYear()));
				vo.addResponse("GENDER", StringUtil.checkVal(user.getGenderCode()));
				data.put(customerId, vo);
			}
			
		} catch (DatabaseException de) {
			log.error("could not load user profiles", de);
		} finally {
			pm = null;
			profileIds = null;
		}

		log.debug("loaded " + data.size() + " survey responses");
		AttendeeSurveyReportVO rpt = new AttendeeSurveyReportVO(questionMap);
		rpt.setData(data.values());
		return rpt;
	}
}
