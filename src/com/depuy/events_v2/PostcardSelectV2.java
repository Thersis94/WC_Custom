package com.depuy.events_v2;

// JDK 1.7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.Cookie;

// J2EE 1.4.0 Libs
import javax.servlet.http.HttpSession;

//wc-depuy libs
import com.depuy.events.vo.CoopAdVO;
import com.depuy.events_v2.vo.ConsigneeVO;

// SMT BaseLibs
import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.depuy.events_v2.vo.DePuyEventSurgeonVO;
import com.depuy.events_v2.vo.PersonVO;
import com.depuy.events_v2.vo.report.CustomReportVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

// SB Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.event.EventFacadeAction;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;


/****************************************************************************
 * <b>Title</b>: PostcardSelectV2.java<p/>
 * <b>Description: Manages retrieval of data for the DePuy Patient Seminars Management site.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 15, 2014
 * @updates
 * 		JM/Wingo - Q4 2014 & Q1 2015 - "phase 2" upgrades per DePuy.
 ****************************************************************************/
public class PostcardSelectV2 extends SBActionAdapter {
	
	private static final int MITEK_READ_ONLY_ROLE = 1500;
	
	
	//GUIDS tied to the short term survey (a Survey portlet)
	private enum SurveyConst {
		surveyAction("c0a8021edb7fd91f57d3396eea06b0e9","c0a80237c2034fbfe0cea6bddfbf537d"),
		rsvpCodeQuestion("c0a8021edb832b385a61675da76470a2", "c0a80237c2034fcfc5194464583dff35"),
		venueCostQuestion("c0a802378e86f39749342d9a1b36ba68", "c0a80237c2034fcfc5194464583dff35"),
		refreshmentCostQuestion("c0a802378e87b9f229fbb0ae1e1c3e9a", "c0a80237c2034fddc5194464da186e94"),
		attendeeCountQuestion("c0a8021edb84f3d3e235a9961df88afa", "c0a80237c2034fd0c51944641a8a690d");
		
		private String orthoQuestionGrpId, mitekQuestionGrpId;
		SurveyConst(String ortho, String mitek) {
			this.orthoQuestionGrpId = ortho;
			this.mitekQuestionGrpId = mitek;
		}
		public String getKey(boolean isMitek) {
			return (isMitek) ? mitekQuestionGrpId : orthoQuestionGrpId;
		}
	}
	
	
	public static final String ACTION_ITEMS_CNT = "outstanding";
	
	public enum ReqType {
		//create-wizard screens
		getstarted, selectType, eventInfo, leads,
		//timeline screens
		summary, status, promote, manage, dayof, postseminar, 
		//list pages
		active /** implied default **/, completed, 
		//reports
		reportForm,  rsvpBreakdown, report,
		//ordering consumables for hospital sponsored seminars
		hospitalSponsored, outstanding,
		//misc utils
		isSurveyComplete
	}
	
	
	/*
	 * Enums for the different Comparator sorting order.
	 * these are intentionally lower-case to match the incoming query string parameter.
	 */
	public enum SortOrder {
		type	{  Comparator<DePuyEventSeminarVO> getComparator() { return new SeminarComparator().new TypeComparator(); } },
		product	{  Comparator<DePuyEventSeminarVO> getComparator() { return new SeminarComparator().new ProductComparator(); } },
		rsvp	{  Comparator<DePuyEventSeminarVO> getComparator() { return new SeminarComparator().new RSVPComparator(); } },
		date	{  Comparator<DePuyEventSeminarVO> getComparator() { return new SeminarComparator().new DateComparator(); } },
		status	{  Comparator<DePuyEventSeminarVO> getComparator() { return new SeminarComparator().new StatusComparator(); } },
		owner	{  Comparator<DePuyEventSeminarVO> getComparator() { return new SeminarComparator().new OwnerComparator(); } };
		
		abstract Comparator<DePuyEventSeminarVO> getComparator();
	};
	
	public PostcardSelectV2() {
		super();
	}

	/**
	 * @param arg0
	 */
	public PostcardSelectV2(ActionInitVO arg0) {
		super(arg0);
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		HttpSession ses = req.getSession();
		String eventPostcardId = StringUtil.checkVal(req.getParameter("eventPostcardId"));
		ReqType reqType = null;
		try {
			reqType = ReqType.valueOf(req.getParameter("reqType"));
		} catch (Exception e) {
			//ignore these, default reqType (list active sems) will be used
		}
		
		UserDataVO user = (UserDataVO) ses.getAttribute(Constants.USER_DATA);
		SBUserRole roles = (SBUserRole) ses.getAttribute(Constants.ROLE_DATA);
		if (roles == null) roles = new SBUserRole();
		int roleId = roles.getRoleLevel();
		String profileId = (user != null && roleId < 90) ? user.getProfileId() : null;
		
		//set a special role level for mitek coordinators so they can have read-only access to all seminars
		//this has to use roleName because the roleLevel is 10, the same as joint recon's
		//joint recon does not use the 'read only/show all' capability
		log.debug("name=" + roles.getRoleName());
		if ("Mitek_Coordinator".equalsIgnoreCase(roles.getRoleName())) 
			roleId = MITEK_READ_ONLY_ROLE;
	
		Object data = null;
		try {
			//this call will fail-fast if we only need a count (and already have it loaded on session)
			Object outstanding = loadOutstandingItems( req, profileId, reqType);
			
			if ( ReqType.outstanding == reqType ){
				data = outstanding;
			} else if (ReqType.isSurveyComplete == reqType) {
				verifySurveyComplete(req.getParameter("rsvpCode"), req.hasParameter("isMitek"));
				return;
				
			} else if (eventPostcardId.length() > 0) {
				//load one postcard in it's entirety
				data = loadOneSeminar(eventPostcardId, actionInit.getActionId(), reqType, profileId, req.getParameter("sort"), roleId);
				
			} else if (ReqType.report == reqType && req.hasParameter("isCustomReport")) {
				//need to load the list, then full details for each one.
				List<DePuyEventSeminarVO> list = loadSeminarList(actionInit.getActionId(), reqType, profileId, null, roleId);
				List<DePuyEventSeminarVO> fullList = new ArrayList<>(list.size());
				
				//use a report object so we can apply filters in advance of loading more data
				CustomReportVO rpt = new CustomReportVO(req);
				for (DePuyEventSeminarVO sem: list) {
					if (rpt.semPassesFilters(sem)) {
						DePuyEventSeminarVO sem2 = loadOneSeminar(sem.getEventPostcardId(), actionInit.getActionId(), ReqType.report, null, null, roleId);
						sem2.setRsvpCount(sem.getRsvpCount());
						fullList.add(sem2);
					}
				}
				loadSurveyReponses(fullList, req.hasParameter("isMitek"));
				data = fullList;
			
			} else {
				//load the list of postcards (screen# 1)
				Cookie c = req.getCookie("seminarSortType");
				String sortType =  c != null ? c.getValue() : null;
				data = loadSeminarList(actionInit.getActionId(), reqType, profileId, sortType, roleId);
			}
			
		} catch (SQLException sqle) {
			log.error("could not retrieve seminar data", sqle);
		}
		
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		mod.setActionData(data);
	}
	
	/**
	 * runs a simple PIVOT query to colate the data server-side instead of WC-side.
	 * This query only grabs the data used to display the Active and Completed Seminars pages.
	 * @param profileId
	 * @param string 
	 * @return
	 * @throws SQLException
	 */
	private List<DePuyEventSeminarVO> loadSeminarList(String actionGroupId, ReqType reqType, String profileId, String sortType, int roleId) throws SQLException {
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		Set<String> profileIds = new HashSet<String>();
		Map<String, DePuyEventSeminarVO> mapData = new HashMap<>();
//		Integer rptType = Convert.formatInteger(req.getParameter("rptType"), 0);
		
		StringBuilder sql = new StringBuilder(1000);
		sql.append("select distinct event_entry_id, RSVP_CODE_TXT, start_dt, type_nm, profile_id, ");
		sql.append("surgeon_nm, event_nm, city_nm, state_cd, status_flg, event_postcard_id, postcard_file_status_no, ");
		sql.append("rsvp_no, [4] as 'hip', [5] as 'knee', [6] as 'shoulder', ");  //in a PIVOT, we're turning the data (values) into column headings.  hence the square brackets.
		sql.append("quantity_no, upfront_fee_flg, postcard_cost_no, territory_no, "); 
		sql.append("event_desc, language_cd, postcard_mail_dt, product_id ");
		sql.append("from (select e.event_entry_id, e.RSVP_CODE_TXT, e.start_dt, et.type_nm, ep.event_postcard_id, ");
		sql.append("ep.PROFILE_ID, ep.postcard_file_status_no, s.surgeon_nm, e.event_nm, e.city_nm, ");
		sql.append("e.state_cd, ep.status_flg, lxr.JOINT_ID, lxr.product_id, sum(rsvp.GUESTS_NO) as 'rsvp_no', ");
		sql.append("ep.language_cd, ep.postcard_mail_dt, ");
		sql.append("(ep.quantity_no+deap.postcard_qnty_no) as quantity_no, ep.upfront_fee_flg, ");
		sql.append("ep.territory_no, ep.postcard_cost_no, cast(e.event_desc as varchar(500)) as event_desc ");
		sql.append("from EVENT_ENTRY e ");
		sql.append("inner join EVENT_TYPE et on e.EVENT_TYPE_ID=et.EVENT_TYPE_ID ");
		sql.append("inner join EVENT_GROUP eg on et.ACTION_ID=eg.ACTION_ID ");
		sql.append("inner join SB_ACTION sb on eg.ACTION_ID=sb.ACTION_ID ");
		sql.append("inner join EVENT_POSTCARD_ASSOC epa on e.EVENT_ENTRY_ID=epa.EVENT_ENTRY_ID ");
		sql.append("inner join EVENT_POSTCARD ep on epa.EVENT_POSTCARD_ID=ep.EVENT_POSTCARD_ID ");
		sql.append("left outer join ").append(customDb).append("DEPUY_EVENT_ADDTL_POSTCARD deap on deap.EVENT_ENTRY_ID=epa.EVENT_ENTRY_ID ");
		sql.append("left outer join EVENT_RSVP rsvp on e.EVENT_ENTRY_ID=rsvp.EVENT_ENTRY_ID and rsvp.rsvp_status_flg=1 ");
		sql.append("left outer join ").append(customDb).append("DEPUY_EVENT_SURGEON s on ep.EVENT_POSTCARD_ID=s.EVENT_POSTCARD_ID ");
		sql.append("left outer join ").append(customDb).append("DEPUY_EVENT_SPECIALTY_XR lxr on ep.EVENT_POSTCARD_ID=lxr.EVENT_POSTCARD_ID ");
		//sql.append("left outer join ").append(customDb).append("DEPUY_EVENT_COOP_AD cad on ep.EVENT_POSTCARD_ID=cad.EVENT_POSTCARD_ID and cad.ad_type_txt != 'radio' ");
		sql.append("where sb.action_group_id=? ");
		//--conditionally grab only the events this non-admin is affiliated with --
		//but only for PUBLIC_REGISTERED_LEVEL, which is Joint Recon.  Mitek users can see all, but in read-only mode.
		if (profileId != null && roleId != MITEK_READ_ONLY_ROLE) {
			//this has to be a nested query to avoid duplicates in the RS - JM 03-28-14
			sql.append("and (ep.event_postcard_id in (select event_postcard_id from ").append(customDb).append("DEPUY_EVENT_PERSON_XR where profile_id=?) or ");
			sql.append("ep.profile_id=?) ");  //postcards I'm a part of (REPs & TGMs), or postcards that are mine (coordinators)
		} else if (profileId == null) {
			sql.append("and ep.status_flg != 0 ");  //exclude any that haven't been submitted yet, for admins only
		}
		if (ReqType.completed == reqType) {
			sql.append("and ep.status_flg = ").append(EventFacadeAction.STATUS_COMPLETE).append(" ");
		} else if (ReqType.report != reqType) {
			sql.append("and (ep.status_flg != ").append(EventFacadeAction.STATUS_COMPLETE).append(" or ep.status_flg=0) ");
		}
		sql.append("group by e.event_entry_id, ep.event_postcard_id, e.RSVP_CODE_TXT, e.start_dt, ");
		sql.append("et.type_nm, ep.PROFILE_ID, ep.postcard_file_status_no, e.event_nm, s.surgeon_nm, ");
		sql.append("e.city_nm, e.state_cd, ep.status_flg, lxr.JOINT_ID, lxr.product_id, ep.language_cd, ep.postcard_mail_dt, ");
		sql.append("ep.territory_no, ep.quantity_no, postcard_cost_no, upfront_fee_flg, postcard_qnty_no, cast(e.event_desc as varchar(500)) ");
		sql.append(") baseQry ");
		sql.append("pivot (count(joint_id) for joint_id in ([4],[5],[6])) as pvtQry "); //PIVOT is an implicit group-by
		log.debug(sql);
		log.debug(actionGroupId);
		log.debug(profileId);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, actionGroupId);
			if (profileId != null && roleId != MITEK_READ_ONLY_ROLE) {
				ps.setString(2, profileId);
				ps.setString(3, profileId);
			}
			ResultSet rs = ps.executeQuery();
			DePuyEventSeminarVO vo = null;
			while (rs.next()) {
				if (mapData.containsKey(rs.getString("event_postcard_id"))) {
					vo = mapData.get(rs.getString("event_postcard_id"));
					DePuyEventSurgeonVO surgeon = new DePuyEventSurgeonVO();
					surgeon.setSurgeonName(rs.getString("surgeon_nm"));
					vo.addSurgeon(surgeon);
					   
				} else {
					vo = new DePuyEventSeminarVO().populateFromListRS(rs);
					if (roleId == MITEK_READ_ONLY_ROLE)
						vo.setReadOnly(!profileId.equals(vo.getProfileId()));
					//set aside profileIds for the event owners, these will need to be retrieved from ProfileManager
					profileIds.add(rs.getString("profile_id"));
				}
				mapData.put(vo.getEventPostcardId(), vo);
			}
		}
		
		List<DePuyEventSeminarVO> data = new ArrayList<>(mapData.values());
		
		//retrieve the profiles for the names we need to display
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		Map<String, UserDataVO> profiles = null;
		try {
			profiles = pm.searchProfileMap(dbConn, new ArrayList<String>(profileIds));
			
			//loop the data and attach the profiles where they belong
			for (DePuyEventSeminarVO vo : data) {
				if (profiles.containsKey(vo.getProfileId()))
						vo.setOwner(profiles.get(vo.getProfileId()));
			}
			
		} catch (DatabaseException e) {
			//this is not fatal, so we're not going to throw it.  Only one data element will be missing from the page.
			log.error("could not load user profiles " + e.getMessage());
		} catch (NullPointerException npe) {
			log.error("could not attach user profiles " + npe.getMessage());
		}
		
		//If we have a sortType, ensure we sort the data.
		if(data.size() > 1)
			data = sortData(data, sortType);
		
		log.debug("loaded " + data.size() + " Seminars");
		return data;
	}
	
	
	/**
	 * Sort Method that switches on a enum of Types for the different Comparators.
	 * @param data
	 * @param sortType
	 * @return
	 */
	private List<DePuyEventSeminarVO> sortData(List<DePuyEventSeminarVO> data, String sortType) {
		if (sortType == null || sortType.length() == 0) sortType = "date";
		try {
			Collections.sort(data, SortOrder.valueOf(sortType).getComparator());
		} catch (Exception e) {
			
		}
		log.debug("data sorted");
		return data;
	}

	/**
	 * loads a single postcard, all of it!
	 * @param eventPostcardId
	 * @param actionGroupId
	 * @param reqType
	 * @param profileId
	 * @return
	 * @throws SQLException
	 */
	protected DePuyEventSeminarVO loadOneSeminar(String eventPostcardId, String actionGroupId, 
			ReqType reqType, String profileId, String sortOrder, int roleId) throws SQLException {
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		Set<String> profileIds = new HashSet<String>();
		DePuyEventSeminarVO vo = null;
		
		log.debug("reqType=" + reqType);
		StringBuilder sql = new StringBuilder(1000);
		sql.append("select *, ep.profile_id as owner_profile_id, pxr.profile_id as person_profile_id, ");
		sql.append("ep.status_flg as pc_status_flg, pxr.create_dt as approval_dt  ");
		sql.append("from EVENT_ENTRY e ");
		sql.append("inner join EVENT_TYPE et on e.EVENT_TYPE_ID=et.EVENT_TYPE_ID ");
		sql.append("inner join EVENT_GROUP eg on et.ACTION_ID=eg.ACTION_ID ");
		sql.append("inner join SB_ACTION sb on eg.ACTION_ID=sb.ACTION_ID ");
		sql.append("inner join EVENT_POSTCARD_ASSOC epa on e.EVENT_ENTRY_ID=epa.EVENT_ENTRY_ID ");
		sql.append("inner join EVENT_POSTCARD ep on epa.EVENT_POSTCARD_ID=ep.EVENT_POSTCARD_ID ");
		sql.append("left outer join ").append(customDb).append("DEPUY_EVENT_SPECIALTY_XR lxr on ep.EVENT_POSTCARD_ID=lxr.EVENT_POSTCARD_ID ");
		sql.append("left outer join ").append(customDb).append("DEPUY_EVENT_PERSON_XR pxr on ep.EVENT_POSTCARD_ID=pxr.EVENT_POSTCARD_ID ");
		sql.append("where sb.action_group_id=? and ep.event_postcard_id=? ");
		//--conditionally grab only the events this non-admin is affiliated with -- 
		if (profileId != null && roleId != MITEK_READ_ONLY_ROLE) {
			sql.append("and (pxr.PROFILE_ID=? or ep.PROFILE_ID=?) ");
		}
		log.debug(sql + "|" + actionGroupId + "|" + eventPostcardId + "|" + profileId);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, actionGroupId);
			ps.setString(2, eventPostcardId);
			if (profileId != null && roleId != MITEK_READ_ONLY_ROLE) {
				ps.setString(3, profileId);
				ps.setString(4, profileId);
			}
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (vo == null) {
					vo = new DePuyEventSeminarVO(rs);
					vo.setProfileId(rs.getString("owner_profile_id"));
					if (roleId == MITEK_READ_ONLY_ROLE)
						vo.setReadOnly(!profileId.equals(vo.getProfileId()));
					profileIds.add(vo.getProfileId());
				}
				//REP, TGM & ADV (approver) will need to be retrieved from ProfileManager
				PersonVO p = new PersonVO(rs.getString("postcard_role_cd"), rs.getString("person_profile_id"));
				p.setApproveDate(rs.getDate("approval_dt"));
				vo.addPerson(p);
				profileIds.add(rs.getString("person_profile_id"));
				
				//add the joint, duplicates are handled by the Set
				vo.addJoint(rs.getString("joint_id"));
				
			}
		}
		
		//retrieve the profiles for the names we need to display
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		Map<String, UserDataVO> profiles = null;
		try {
			profiles = pm.searchProfileMap(dbConn, new ArrayList<String>(profileIds));
			
			//profile for the owner
			if (profiles.containsKey(vo.getProfileId()))
				vo.setOwner(profiles.get(vo.getProfileId()));
			
			//assign profiles to the TGMs and REPs
			for (PersonVO p : vo.getPeople()) {
				if (profiles.containsKey(p.getProfileId())) {
					p.setData(profiles.get(p.getProfileId()).getDataMap());
				}
			}
			
		} catch (DatabaseException e) {
			//this is not fatal, so we're not going to throw it.  Only one data element will be missing from the page.
			log.error("could not load user profiles " + e.getMessage());
		} catch (NullPointerException npe) {
			log.error("could not attach user profiles " + npe.getMessage());
		}
		
		if (loadSurgeons(reqType) && vo != null) {
			try {
				retrieveSurgeons(vo);
			} catch (ActionException ae) {
				log.error("could not load surgeons", ae);
			}
		}
		
		//load CoopAds (newspaper & radio)
		if (loadCoopAds(reqType) && vo != null) {
			try {
				retrieveCoopAds(vo);
			} catch (ActionException e) {
				log.error("could not load CoopAds for seminar " + eventPostcardId, e);
			}
		}
		
		if (loadConsignees(reqType) && vo != null) {
			try {
				retrieveConsignees(vo);
			} catch (ActionException e) {
				log.error("could not load CoopAds for seminar " + eventPostcardId, e);
			}
		}
		
		if (loadLeads(reqType) && vo != null) {
			try {
				retrieveLeads(vo, sortOrder);
			} catch (ActionException e) {
				log.error("could not load leads data for seminar " + eventPostcardId, e);
			}
		}

		return vo;
	}
	
	/**
	 * helper to isolate the logic of whether or not we need to load the Ads for the given request
	 * @param reqType
	 * @return
	 */
	private boolean loadCoopAds(ReqType reqType) {
		return (ReqType.reportForm != reqType);
	}
		
	/**
	 * helper to isolate the logic of whether or not we need to load the Consignees for the given request
	 * @param reqType
	 * @return
	 */
	private boolean loadSurgeons(ReqType reqType) {
		return (ReqType.eventInfo == reqType || ReqType.summary == reqType || ReqType.report == reqType || ReqType.reportForm == reqType);
	}
	
	/**
	 * helper to isolate the logic of whether or not we need to load the Consignees for the given request
	 * @param reqType
	 * @return
	 */
	private boolean loadConsignees(ReqType reqType) {
		return (ReqType.eventInfo == reqType || ReqType.summary == reqType || ReqType.report == reqType);
	}
	
	/**
	 * helper to isolate the logic of whether or not we need to load the leads for the given request
	 * @param reqType
	 * @return
	 */
	private boolean loadLeads(ReqType reqType) {
		return (ReqType.leads == reqType);
	}
	
	/**
	 * calls the CoopAdsAction to load Ad data for the given Seminar
	 * @param vo
	 * @throws ActionException
	 */
	protected void retrieveCoopAds(DePuyEventSeminarVO vo) throws ActionException {
		CoopAdsActionV2 caa = new CoopAdsActionV2(actionInit);
		caa.setAttributes(attributes);
		caa.setDBConnection(dbConn);
		List<CoopAdVO> ads = caa.retrieve(null, vo.getEventPostcardId());
		for (CoopAdVO ad : ads)
				vo.addAdvertisement(ad);
		
		return;
	}
	
	
	/**
	 * load the surgeon/speakers for this seminar
	 * @param vo
	 * @throws ActionException
	 */
	protected void retrieveSurgeons(DePuyEventSeminarVO vo) throws ActionException {
		StringBuilder sql = new StringBuilder(100);
		sql.append("select * from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_EVENT_SURGEON where event_postcard_id=? order by order_no");
		log.debug(sql + "|" + vo.getEventPostcardId());
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, vo.getEventPostcardId());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				vo.addSurgeon(new DePuyEventSurgeonVO(rs));
				log.debug("added " + rs.getString("surgeon_nm"));
			}
		} catch (SQLException sqle) {
			log.error("could not load consignees", sqle);
		}
	}
	
	
	/**
	 * load the cosignees for this seminar
	 * @param vo
	 * @throws ActionException
	 */
	protected void retrieveConsignees(DePuyEventSeminarVO vo) throws ActionException {
		StringBuilder sql = new StringBuilder(100);
		sql.append("select * from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_EVENT_POSTCARD_CONSIGNEE where event_postcard_id=?");
		log.debug(sql + "|" + vo.getEventPostcardId());
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, vo.getEventPostcardId());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				vo.addConsignee(new ConsigneeVO(rs));
				log.debug("added " + rs.getString("contact_nm"));
			}
		} catch (SQLException sqle) {
			log.error("could not load consignees", sqle);
		}
	}
	
	/**
	 * calls the leads data tool to load leads data for the Seminar
	 * @param vo
	 * @throws ActionException
	 */
	private void retrieveLeads(DePuyEventSeminarVO sem, String sort) throws ActionException {
		LeadsDataToolV2 leads = LeadsDataToolV2.newInstance(sem, actionInit);
		leads.setAttributes(attributes);
		leads.setDBConnection(dbConn);
		leads.targetLeads(sem, sort);
		return;
	}
	
	
	/**
	 * Get a list of the outstanding items for this user
	 * @param req
	 */
	private Collection<DePuyEventSeminarVO> loadOutstandingItems(SMTServletRequest req, 
			String profileId, ReqType reqType) {

		//if this is a count and the value was already calculated, we don't have any work to do here.
		//all pages display the count, but only one (ReqType.outstanding) needs the data.
		if (reqType != ReqType.outstanding &&  req.getSession().getAttribute(ACTION_ITEMS_CNT) != null)
			return null;
		
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String actionId = actionInit.getActionId();
		Map<String, DePuyEventSeminarVO> data = new HashMap<>();

		StringBuilder sql = new StringBuilder(1300);
		sql.append("select distinct e.event_entry_id, e.RSVP_CODE_TXT, e.start_dt, ep.invite_file_flg, ep.invite_file_url, ");
		sql.append("et.type_nm, ep.event_postcard_id, ep.PROFILE_ID, ep.postcard_file_status_no, "); 
		sql.append("e.event_nm, e.city_nm, e.state_cd, ep.status_flg, ep.CONSUMABLE_ORDER_DT,  ");
		sql.append("cad.status_flg as ad_status_flg, ep.language_cd, online_flg, e.create_dt ");
		sql.append("from EVENT_ENTRY e ");
		sql.append("inner join EVENT_TYPE et on e.EVENT_TYPE_ID=et.EVENT_TYPE_ID "); 
		sql.append("inner join EVENT_GROUP eg on et.ACTION_ID=eg.ACTION_ID ");
		sql.append("inner join SB_ACTION sb on eg.ACTION_ID=sb.ACTION_ID ");
		sql.append("inner join EVENT_POSTCARD_ASSOC epa on e.EVENT_ENTRY_ID=epa.EVENT_ENTRY_ID "); 
		sql.append("inner join EVENT_POSTCARD ep on epa.EVENT_POSTCARD_ID=ep.EVENT_POSTCARD_ID "); 
		sql.append("left outer join ").append(customDb).append("DEPUY_EVENT_COOP_AD cad on ep.EVENT_POSTCARD_ID=cad.EVENT_POSTCARD_ID "); 
		sql.append("and cad.ad_type_txt != 'radio' and cad.status_flg in (2,7) ");
		sql.append("left outer join ").append(customDb).append("DEPUY_EVENT_PERSON_XR pxr on ep.EVENT_POSTCARD_ID=pxr.EVENT_POSTCARD_ID ");
		sql.append("where sb.action_group_id=? ");
		sql.append("and ( ");
		//Non-admins don't need to be alerted of pending submissions or surgeon-related stuff
		if (profileId == null) {
			sql.append("ep.STATUS_FLG in (5, 13) ");
		} else {
			sql.append("ep.STATUS_FLG=13 ");
		}
		//postcard file is pending
		sql.append("or (ep.STATUS_FLG=15 and ep.postcard_file_status_no=2) ");
		//ad issues -- gets sorted out in the JSP
		sql.append("or (ep.STATUS_FLG=15  ) ");

		sql.append(") ");
		//Use profile id if this is not an admin request (null)
		if (profileId != null) {
			sql.append("and (pxr.PROFILE_ID=? or ep.PROFILE_ID=?) ");
		}
		sql.append("group by e.event_entry_id, ep.event_postcard_id, e.RSVP_CODE_TXT, ");
		sql.append("e.start_dt, ep.invite_file_flg, ep.invite_file_url, et.type_nm, ep.PROFILE_ID, ep.postcard_file_status_no, ");
		sql.append("e.event_nm, e.city_nm, e.state_cd, ep.status_flg, ep.CONSUMABLE_ORDER_DT,  ");
		sql.append("cad.status_flg, ep.language_cd, online_flg, e.create_dt ");
		sql.append("order by e.create_dt, ep.event_postcard_id");
		log.debug(sql+" | "+actionId + "|" + profileId);

		String id = "";
		DePuyEventSeminarVO vo = null;
		OutstandingItems oa = new OutstandingItems(dbConn);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, actionId);
			if (profileId != null) {
				ps.setString(2, profileId);
				ps.setString(3, profileId);
			}
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				id = rs.getString("event_postcard_id");
				if (data.containsKey(id)) {
					vo = data.get(id);
				} else {
					vo = new DePuyEventSeminarVO().populateFromListRS(rs);
				}
				oa.attachActionItems(vo);
				if (vo.getActionItems() != null && vo.getActionItems().size() > 0)
					data.put(id, vo);
			}
		} catch (SQLException sqle) {
			log.error("could not load action items", sqle);
		}
		
		//now that we've addressed duplicates, do a final loop and count how many items we have to report
		int issueCnt = 0;
		for (DePuyEventSeminarVO vo2 : data.values())
			issueCnt += (vo2.getActionItems() != null) ? vo2.getActionItems().size() : 0;

		//update the item count
		req.getSession().setAttribute(ACTION_ITEMS_CNT, issueCnt);
		
		return data.values();
	}
	
	
	/**
	 * checks the survey_response table to see if the coordinator has completed 
	 * a survey for the given seminar.
	 * @param rsvpCode
	 */
	private void verifySurveyComplete(String rsvpCode, boolean isMitek) {
		boolean resp = false;
		StringBuilder sql = new StringBuilder(100);
		sql.append("select value_txt from survey_response ");
		sql.append("where survey_question_id=? and action_group_id=?");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, SurveyConst.rsvpCodeQuestion.getKey(isMitek));
			ps.setString(2, SurveyConst.surveyAction.getKey(isMitek));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String val = StringUtil.checkVal(rs.getString(1)).toLowerCase();
				if (val.startsWith(StringUtil.checkVal(rsvpCode).toLowerCase())) {
					resp = true;
					break;
				}
			}
		} catch (SQLException sqle) {
			log.error("could not load survey complete", sqle);
		}
		log.debug("survey complete? " + resp);
		super.putModuleData(resp);
	}
	
	
	/**
	 * load specific responses to the short term surveys - needed for Custom Reports only.
	 * @param sem
	 */
	protected void loadSurveyReponses(List<DePuyEventSeminarVO> seminars, boolean isMitek) {
		StringBuilder sql = new StringBuilder(300);
		sql.append("select a.value_txt as rsvp_cd, b.value_txt as venue_cost, c.value_txt as refresh_cost, d.value_txt as attendee_cnt ");
		sql.append("from SURVEY_RESPONSE a ");
		sql.append("left outer join SURVEY_RESPONSE b on b.SURVEY_QUESTION_ID=? and a.TRANSACTION_ID=b.TRANSACTION_ID ");
		sql.append("left outer join SURVEY_RESPONSE c on c.SURVEY_QUESTION_ID=? and a.TRANSACTION_ID=c.TRANSACTION_ID ");
		sql.append("left outer join SURVEY_RESPONSE d on d.SURVEY_QUESTION_ID=? and a.TRANSACTION_ID=d.TRANSACTION_ID ");
		sql.append("where a.VALUE_TXT in ('~!'");
		for (@SuppressWarnings("unused") Object o : seminars) sql.append(",?");
		sql.append(") and a.SURVEY_QUESTION_ID=? and a.ACTION_GROUP_ID=? ");
		log.debug(sql);
		
		int x = 1;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(x++,  SurveyConst.venueCostQuestion.getKey(isMitek));
			ps.setString(x++,  SurveyConst.refreshmentCostQuestion.getKey(isMitek));
			ps.setString(x++,  SurveyConst.attendeeCountQuestion.getKey(isMitek));
			for (DePuyEventSeminarVO vo: seminars) ps.setString(x++, vo.getRSVPCodes());
			ps.setString(x++,  SurveyConst.rsvpCodeQuestion.getKey(isMitek));
			ps.setString(x++,  SurveyConst.surveyAction.getKey(isMitek));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String rsvpCd = rs.getString(1);
				for (DePuyEventSeminarVO vo : seminars) {
					if (! vo.getRSVPCodes().equals(rsvpCd)) continue;
					vo.addSurveyResponse("venue_cost", rs.getString(2));
					vo.addSurveyResponse("refresh_cost", rs.getString(3));
					vo.addSurveyResponse("attendee_cnt", rs.getString(4));
				}
			}
		} catch (SQLException sqle) {
			log.error("could not load custom report survey responses", sqle);
		}
	}
}
