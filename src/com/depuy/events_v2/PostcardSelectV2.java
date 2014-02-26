package com.depuy.events_v2;

// JDK 1.7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


// J2EE 1.4.0 Libs
import javax.servlet.http.HttpSession;


//wc-depuy libs
import com.depuy.events.vo.CoopAdVO;

// SMT BaseLibs
import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.depuy.events_v2.vo.PersonVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.security.UserDataVO;

// SB Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.event.EventFacadeAction;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;


/****************************************************************************
 * <b>Title</b>: EventPostcardSelect.java<p/>
 * <b>Description: Manages EventActions for the SB Sites</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2005<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 1.0
 * @since Mar 15, 2006
 ****************************************************************************/
public class PostcardSelectV2 extends SBActionAdapter {
	
	public enum ReqType {
		//create-wizard screens
		getstarted, selectType, eventInfo, leads,
		//timeline screens
		summary, status, promote, manage, dayof, postseminar, 
		//list pages
		active /** implied default **/, completed, 
		//reports
		reportForm,  rsvpBreakdown
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
		Integer roleId = (roles != null) ? roles.getRoleLevel() : SecurityController.PUBLIC_ROLE_LEVEL;
		String profileId = (roleId < SecurityController.ADMIN_ROLE_LEVEL) ? user.getProfileId() : null;
	
		Object data = null;
		try {
			if (eventPostcardId.length() > 0) {
				//load one postcard in it's entirety
				data = loadOneSeminar(eventPostcardId, actionInit.getActionId(), reqType, profileId, req.getParameter("sort"));
				
			} else {
				//load the list of postcards (screen# 1)
				data = loadSeminarList(actionInit.getActionId(), reqType, profileId, req.getParameter("sortType"));
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
	private List<DePuyEventSeminarVO> loadSeminarList(String actionGroupId, ReqType reqType, String profileId, String sortType) throws SQLException {
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		Set<String> profileIds = new HashSet<String>();
		List<DePuyEventSeminarVO> data = new ArrayList<DePuyEventSeminarVO>();
//		Integer rptType = Convert.formatInteger(req.getParameter("rptType"), 0);
		
		StringBuilder sql = new StringBuilder();
		sql.append("select distinct event_entry_id, RSVP_CODE_TXT, start_dt, type_nm, profile_id, ");
		sql.append("surgeon_nm, event_nm, city_nm, state_cd, status_flg, event_postcard_id, postcard_file_status_no, ");
		sql.append("rsvp_no, [4] as 'hip', [5] as 'knee', [6] as 'shoulder', run_dates_txt, ad_status_flg ");  //in a PIVOT, we're turning the data (values) into column headings.  hence the square brackets.
		//sql.append(" ");
		sql.append("from (select e.event_entry_id, e.RSVP_CODE_TXT, e.start_dt, et.type_nm, ep.event_postcard_id, ");
		sql.append("ep.PROFILE_ID, ep.postcard_file_status_no, s.surgeon_nm, e.event_nm, e.city_nm, ");
		sql.append("e.state_cd, ep.status_flg, lxr.JOINT_ID, COUNT(rsvp.EVENT_RSVP_ID) as 'rsvp_no', ");
		sql.append("cad.run_dates_txt, cad.status_flg as ad_status_flg ");
		sql.append("from EVENT_ENTRY e ");
		sql.append("inner join EVENT_TYPE et on e.EVENT_TYPE_ID=et.EVENT_TYPE_ID ");
		sql.append("inner join EVENT_GROUP eg on et.ACTION_ID=eg.ACTION_ID ");
		sql.append("inner join SB_ACTION sb on eg.ACTION_ID=sb.ACTION_ID ");
		sql.append("inner join EVENT_POSTCARD_ASSOC epa on e.EVENT_ENTRY_ID=epa.EVENT_ENTRY_ID ");
		sql.append("inner join EVENT_POSTCARD ep on epa.EVENT_POSTCARD_ID=ep.EVENT_POSTCARD_ID ");
		sql.append("left outer join EVENT_RSVP rsvp on e.EVENT_ENTRY_ID=rsvp.EVENT_ENTRY_ID ");
		sql.append("left outer join ").append(customDb).append("DEPUY_EVENT_SURGEON s on ep.EVENT_POSTCARD_ID=s.EVENT_POSTCARD_ID ");
		sql.append("left outer join ").append(customDb).append("DEPUY_EVENT_SPECIALTY_XR lxr on ep.EVENT_POSTCARD_ID=lxr.EVENT_POSTCARD_ID ");
		sql.append("left outer join ").append(customDb).append("DEPUY_EVENT_COOP_AD cad on ep.EVENT_POSTCARD_ID=cad.EVENT_POSTCARD_ID and cad.ad_type_txt != 'radio' ");
		//--conditionally grab only the events this non-admin is affiliated with -- 
		if (profileId != null) {
			sql.append("left outer join ").append(customDb).append("DEPUY_EVENT_PERSON_XR pxr on ep.EVENT_POSTCARD_ID=pxr.EVENT_POSTCARD_ID ");
			sql.append("where (pxr.PROFILE_ID=? or ep.PROFILE_ID=?) and sb.action_group_id=? ");
		} else {
			sql.append("where sb.action_group_id=? and ep.status_flg != 0 ");  //exclude any that haven't been submitted yet.
		}
		if (ReqType.completed == reqType) {
			sql.append("and ep.status_flg = ").append(EventFacadeAction.STATUS_COMPLETE).append(" ");
		} else {
			sql.append("and (ep.status_flg != ").append(EventFacadeAction.STATUS_COMPLETE).append(" or ep.status_flg is null) ");
		}
		sql.append("group by e.event_entry_id, ep.event_postcard_id, e.RSVP_CODE_TXT, e.start_dt, ");
		sql.append("et.type_nm, ep.PROFILE_ID, ep.postcard_file_status_no, e.event_nm, s.surgeon_nm, ");
		sql.append("e.city_nm, e.state_cd, ep.status_flg, lxr.JOINT_ID, cad.run_dates_txt, cad.status_flg ");
		sql.append(") baseQry ");
		sql.append("pivot (count(joint_id) for joint_id in ([4],[5],[6])) as pvtQry "); //PIVOT is an implicit group-by
		log.debug(sql);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			if (profileId != null) {
				ps.setString(1, profileId);
				ps.setString(2, profileId);
				ps.setString(3, actionGroupId);
			} else {
				ps.setString(1, actionGroupId);
			}
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				//set aside profileIds for the event owners, these will need to be retrieved from ProfileManager
				profileIds.add(rs.getString("profile_id"));
				data.add(new DePuyEventSeminarVO().populateFromListRS(rs));
			}
		} finally { 
			try { ps.close(); } catch (Exception e) { }
		}
		
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
	private DePuyEventSeminarVO loadOneSeminar(String eventPostcardId, String actionGroupId, 
			ReqType reqType, String profileId, String sortOrder) throws SQLException {
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		Set<String> profileIds = new HashSet<String>();
		DePuyEventSeminarVO vo = null;
		
		StringBuilder sql = new StringBuilder();
		sql.append("select *, ep.profile_id as owner_profile_id, pxr.profile_id as person_profile_id, ");
		sql.append("ep.status_flg as pc_status_flg, pxr.create_dt as approval_dt  ");
		sql.append("from EVENT_ENTRY e ");
		sql.append("inner join EVENT_TYPE et on e.EVENT_TYPE_ID=et.EVENT_TYPE_ID ");
		sql.append("inner join EVENT_GROUP eg on et.ACTION_ID=eg.ACTION_ID ");
		sql.append("inner join SB_ACTION sb on eg.ACTION_ID=sb.ACTION_ID ");
		sql.append("inner join EVENT_POSTCARD_ASSOC epa on e.EVENT_ENTRY_ID=epa.EVENT_ENTRY_ID ");
		sql.append("inner join EVENT_POSTCARD ep on epa.EVENT_POSTCARD_ID=ep.EVENT_POSTCARD_ID ");
		sql.append("left outer join ").append(customDb).append("DEPUY_EVENT_SURGEON s on ep.EVENT_POSTCARD_ID=s.EVENT_POSTCARD_ID ");
		sql.append("left outer join ").append(customDb).append("DEPUY_EVENT_SPECIALTY_XR lxr on ep.EVENT_POSTCARD_ID=lxr.EVENT_POSTCARD_ID ");
		sql.append("left outer join ").append(customDb).append("DEPUY_EVENT_PERSON_XR pxr on ep.EVENT_POSTCARD_ID=pxr.EVENT_POSTCARD_ID ");
		sql.append("where sb.action_group_id=? and ep.event_postcard_id=? ");
		//--conditionally grab only the events this non-admin is affiliated with -- 
		if (profileId != null) {
			sql.append("and (pxr.PROFILE_ID=? or ep.PROFILE_ID=?) ");
		}
		log.debug(sql + "|" + actionGroupId + "|" + eventPostcardId + "|" + profileId);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, actionGroupId);
			ps.setString(2, eventPostcardId);
			if (profileId != null) {
				ps.setString(3, profileId);
				ps.setString(4, profileId);
			}
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (vo == null) {
					vo = new DePuyEventSeminarVO(rs);
					vo.setProfileId(rs.getString("owner_profile_id"));
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
		} finally { 
			try { ps.close(); } catch (Exception e) { }
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
		
		//load CoopAds (newspaper & radio)
		if (loadCoopAds(reqType)) {
			try {
				retrieveCoopAds(vo);
			} catch (ActionException e) {
				log.error("could not load CoopAds for seminar " + eventPostcardId, e);
			}
		}
		
		if (loadLeads(reqType)) {
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
	private void retrieveCoopAds(DePuyEventSeminarVO vo) throws ActionException {
		CoopAdsActionV2 caa = new CoopAdsActionV2(actionInit);
		caa.setAttributes(attributes);
		caa.setDBConnection(dbConn);
		List<CoopAdVO> ads = caa.retrieve(null, vo.getEventPostcardId());
		for (CoopAdVO ad : ads) {
			if ("radio".equalsIgnoreCase(ad.getAdType())) {
				vo.setRadioAd(ad);
			} else {
				vo.setNewspaperAd(ad);
			}
		}
		return;
	}
	
	/**
	 * calls the leads data tool to load leads data for the Seminar
	 * @param vo
	 * @throws ActionException
	 */
	private void retrieveLeads(DePuyEventSeminarVO sem, String sort) throws ActionException {
		LeadsDataToolV2 leads = new LeadsDataToolV2(actionInit);
		leads.setAttributes(attributes);
		leads.setDBConnection(dbConn);
		leads.targetLeads(sem, sort);
		return;
	}
	
}
