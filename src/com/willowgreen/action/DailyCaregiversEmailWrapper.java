package com.willowgreen.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: GriefEmailWrapper.java<p/>
 * <b>Description: Facade around the email sign-up (Contact Us Portlet) and reporting
 * (simplified ContactDataTool)</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Apr 28, 2013
 ****************************************************************************/
public class DailyCaregiversEmailWrapper extends EmailWrapper {
	
	private static final String GATEKEEPER_QUEST_ID = "c0a80241a10d8161cd749175f14e2a9d"; //from database
	private static final String FREE_SERIES = "c0a80241a143abeb8ff0e2bc206b9831"; //DiFC free trial campaign - 16 emails
	private static final String MAIN_SERIES = "c0a80237a94882127e4d342eb3d66a4f"; // DiFC 365 email series
	
	public DailyCaregiversEmailWrapper() {
		super();
	}
	
	public DailyCaregiversEmailWrapper(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	
	public void build(SMTServletRequest req) throws ActionException {
		String programNm = (actionInit.getName().toLowerCase().indexOf("free") > -1) ? "DIFC-FREE" : "DIFC";
		req.setAttribute("series", programNm);
		super.build(req);
	}
	
	
	/**
	 * checks to see if the user has already recieved all of the emails in the series.
	 * 
	 * @param emailAddress
	 * @return
	 */
	protected boolean isEnrolled(String emailAddress, String emailCampaignId) {
		boolean isEnrolled = false;
		String profileId = null;
		
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		try {
			UserDataVO user = new UserDataVO();
			user.setEmailAddress(emailAddress);
			profileId = pm.checkProfile(user, dbConn);
		} catch (DatabaseException de) {
			log.error("could not find profileId for " + emailAddress, de);
		} finally {
			pm = null;
		}
		log.debug("profileId=" + profileId);
		if (profileId == null) return isEnrolled;
		
		StringBuilder sql = new StringBuilder(250);
		sql.append("select count(a.campaign_instance_id), count(b.campaign_instance_id) ");
		sql.append("from email_campaign_instance b left outer join email_campaign_log a ");
		sql.append("on a.campaign_instance_id=b.campaign_instance_id and a.profile_id=? ");
		sql.append("where b.email_campaign_id=?");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, profileId);
			ps.setString(2, emailCampaignId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				//if they have recieved emails, but not all of them, they're still enrolled.
				//or they've opt-out and don't want to be a subscriber anyways!
				if (MAIN_SERIES.equals(emailCampaignId)) {
					//deduct the alternate intro email.  The user will only get one of the two (gatekeeper or main)
					isEnrolled = (rs.getInt(1) > 0 && rs.getInt(1) < (rs.getInt(2)-1));
				} else {
					isEnrolled = (rs.getInt(1) > 0 && rs.getInt(1) < rs.getInt(2));
				}
				//log.debug("rcvd=" + rs.getInt(1) + " series=" + rs.getInt(2));
			}
		} catch (SQLException sqle) {
			log.error("could not lookup email count", sqle);
		}

		return isEnrolled;
	}
	
	
	protected void loadReport(SMTServletRequest req, String contactActionId, String emailCampaignId) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		
		Map<String, UserDataVO> profiles = new HashMap<String, UserDataVO>();
		List<ReportVO> data = new LinkedList<ReportVO>();
		
		StringBuilder sql = new StringBuilder(400);
		sql.append("select a.profile_id, a.DEALER_LOCATION_ID, cast(e.value_txt as nvarchar(10)) as 'is_gatekeeper', ");
		sql.append("MIN(c.CREATE_DT) as 'first_dt', MAX(c.CREATE_DT) as 'last_dt', ");
		sql.append("COUNT(c.CAMPAIGN_LOG_ID) as 'email_cnt', b.ALLOW_COMM_FLG, ");
		sql.append("a.contact_submittal_id, wc.record_no, a.create_dt ");
		sql.append("from CONTACT_SUBMITTAL a ");
		sql.append("left outer join CONTACT_DATA e on a.CONTACT_SUBMITTAL_ID=e.CONTACT_SUBMITTAL_ID and e.CONTACT_FIELD_ID='").append(GATEKEEPER_QUEST_ID).append("' "); //isGatekeeper
		sql.append("left outer join ORG_PROFILE_COMM b on a.PROFILE_ID=b.PROFILE_ID and b.ORGANIZATION_ID=? ");
		sql.append("left outer join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("WILLOWGREEN_COUNTER wc on a.CONTACT_SUBMITTAL_ID=wc.CONTACT_SUBMITTAL_ID ");
		sql.append("left outer join EMAIL_CAMPAIGN_LOG c on a.PROFILE_ID=c.PROFILE_ID and campaign_instance_id in (select campaign_instance_id from email_campaign_instance where EMAIL_CAMPAIGN_ID=?) ");
		sql.append("where a.ACTION_ID=? "); //this is actually actionGroupId, on the data level
		if (role.getRoleLevel() < SecurityController.ADMIN_ROLE_LEVEL)
			sql.append("and a.DEALER_LOCATION_ID=? ");
		sql.append("group by a.PROFILE_ID, a.DEALER_LOCATION_ID, a.contact_submittal_id, a.create_dt, b.ALLOW_COMM_FLG, cast(e.value_txt as nvarchar(10)), record_no, a.create_dt ");
		sql.append("order by record_no desc");
		//log.debug(sql + "|" + role.getProfileId() + "|" + emailCampaignId + "|" + contactActionId);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, site.getOrganizationId());
			ps.setString(2, emailCampaignId);
			ps.setString(3, contactActionId);
			if (role.getRoleLevel() < SecurityController.ADMIN_ROLE_LEVEL)
				ps.setString(4, role.getProfileId());
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				profiles.put(rs.getString(1), null);
				
				if (rs.getString(2) != null)
					profiles.put(rs.getString(2), null);
				
				data.add(new ReportVO(rs));
			}
			
		} catch (SQLException sqle) {
			log.error("could not load report", sqle);
		}
		
		//put a profile to all these peeps!
		int activeCnt = 0, gatekeeperCnt = 0;
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		try {
			profiles = pm.searchProfileMap(dbConn, new ArrayList<String>(profiles.keySet()));
			for (ReportVO rpt : data) {
				rpt.setUser(profiles.get(rpt.getProfileId()));
				rpt.setSubmitter(profiles.get(rpt.getDealerLocationId()));
				
				//determine if the user is currently getting emails
				if (FREE_SERIES.equals(emailCampaignId)) { //free series
					if (rpt.getEmailCnt() < 16 && rpt.getAllowCommFlg() == 1)
						activeCnt++;
				} else if (rpt.isGatekeeper() && rpt.getEmailCnt() < 62 && rpt.getAllowCommFlg() == 1) {
					gatekeeperCnt++;
				} else if (!rpt.isGatekeeper() && rpt.getEmailCnt() < 366 && rpt.getAllowCommFlg() == 1) {
					activeCnt++; 
				}
			}
			
		} catch (DatabaseException de) {
			log.error("could not retrieve profiles", de);
		} finally {
			pm = null;
		}
		
		req.setAttribute("gatekeeperNo", gatekeeperCnt);
		req.setAttribute("activeNo", activeCnt);
		req.setAttribute("enrolledNo", data.size());
		super.putModuleData(data);
	}
	
	

	/**
	 * converts a gatekeeper subscription  (62 emails) into a full (365 emails) one
	 */
	protected void convert(SMTServletRequest req) throws ActionException {
		String msg = "";
		String sql = "update contact_data set value_txt=0, update_dt=getDate() where contact_field_id=? and contact_submittal_id=?";
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, GATEKEEPER_QUEST_ID);
			ps.setString(2, req.getParameter("convert"));
			ps.executeUpdate();
			msg = "Enrollment+convered+successfully";
			
		} catch (SQLException sqle) {
			log.error("could not convert enrollment, csId=" + req.getParameter("del"), sqle);
			msg = "Enrollment+could+not+be+convered";
		}
		
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		super.sendRedirect(page.getFullPath(), msg, req);
	}
	
}
