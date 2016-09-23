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
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.contact.ContactFacadeAction;
import com.smt.sitebuilder.action.contact.SubmittalAction;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: EmailWrapper.java<p/>
 * <b>Description: Facade around the email sign-up (Contact Us Portlet) and reporting
 * (simplified ContactDataTool)</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 8, 2012
 ****************************************************************************/
public class EmailWrapper extends SimpleActionAdapter {
	
	public EmailWrapper() {
		super();
	}
	
	public EmailWrapper(ActionInitVO actionInit) {
		super(actionInit);
	}
	

	public void delete(SMTServletRequest req) throws ActionException {
		super.delete(req);
		String msg = "";
		String sql = "delete from contact_submittal where contact_submittal_id=?";
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, req.getParameter("del"));
			ps.executeUpdate();
			msg = "Enrollment+deleted+successfully";
			
		} catch (SQLException sqle) {
			log.error("could not delete enrollment, csId=" + req.getParameter("del"), sqle);
		}
		
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		super.sendRedirect(page.getFullPath(), msg, req);
	}
	
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		
		if (mod.getDisplayPage().endsWith("report.jsp")) {
			//handle deletions
			if (req.hasParameter("del")) {
				this.delete(req);
				return;
			} else if (req.hasParameter("convert")) {
				convert(req);
				return;
			}
			
			//if the View is our report, load the report data.
			loadReport(req, (String)mod.getAttribute(SBModuleVO.ATTRIBUTE_1), (String)mod.getAttribute(SBModuleVO.ATTRIBUTE_2));
			
		} else {
			String contactActionId = (String)mod.getAttribute(SBModuleVO.ATTRIBUTE_1);
			actionInit.setActionId(contactActionId);
			req.setParameter("actionGroupId", contactActionId);
			
			SMTActionInterface ai = new ContactFacadeAction(actionInit);
			ai.setDBConnection(dbConn);
			ai.setAttributes(attributes);
			ai.retrieve(req);
			
			ModuleVO cMod = (ModuleVO)getAttribute(Constants.MODULE_DATA);
			cMod.setPageModuleId(mod.getPageModuleId());
			cMod.setActionId(mod.getActionId());
			super.setAttribute(Constants.MODULE_DATA, cMod);
		}
	}
	
	
	public void build(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		
		if (req.hasParameter("pfl_EMAIL_ADDRESS_TXT") && 
				!isEnrolled(req.getParameter("pfl_EMAIL_ADDRESS_TXT"), (String)mod.getAttribute(SBModuleVO.ATTRIBUTE_2), (String)mod.getAttribute(SBModuleVO.ATTRIBUTE_1))) {
			//bind the submitting user to this record via DealerLocationId
			UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
			if (user != null)
				req.setParameter(Constants.DEALER_LOCATION_ID_KEY, user.getProfileId());
			
			actionInit.setActionId((String)mod.getAttribute(SBModuleVO.ATTRIBUTE_1));
			SMTActionInterface ai = new ContactFacadeAction(actionInit);
			ai.setDBConnection(dbConn);
			ai.setAttributes(attributes);
			ai.build(req);
			ai = null;
			
			//add the record to our 'counter' table.
			String series = (req.getAttribute("series") != null) ? (String)req.getAttribute("series") : "100MSGS";
			String cdId = (String)req.getAttribute(SubmittalAction.CONTACT_SUBMITTAL_ID);
			incrementCounter(cdId, series);
			
			
		} else {
			//send a message back that the user is already enrolled
			String msg = "The email address " + req.getParameter("pfl_EMAIL_ADDRESS_TXT") + " is already enrolled.";
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			super.sendRedirect(page.getFullPath(), msg, req);
		}
	}
	
	protected void loadReport(SMTServletRequest req, String contactActionId, String emailCampaignId) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		
		Map<String, UserDataVO> profiles = new HashMap<>();
		List<ReportVO> data = new LinkedList<>();
		
		StringBuilder sql = new StringBuilder(1000);
		sql.append("select a.profile_id, a.DEALER_LOCATION_ID, cast(e.value_txt as text) as home_nm, ");
		sql.append("cast(f.value_txt as text) as gifter_nm, ");
		sql.append("MIN(c.CREATE_DT) as first_dt, MAX(c.CREATE_DT) as last_dt, ");
		sql.append("COUNT(c.CAMPAIGN_LOG_ID) as email_cnt, b.ALLOW_COMM_FLG, ");
		sql.append("a.contact_submittal_id, wc.record_no, a.create_dt ");
		sql.append("from CONTACT_SUBMITTAL a ");
		sql.append("left outer join CONTACT_DATA e on a.CONTACT_SUBMITTAL_ID=e.CONTACT_SUBMITTAL_ID and e.CONTACT_FIELD_ID='c0a80237c670c7f0abba7364a8fc9a85' "); //funeralHomeName
		sql.append("left outer join CONTACT_DATA f on a.CONTACT_SUBMITTAL_ID=f.CONTACT_SUBMITTAL_ID and f.CONTACT_FIELD_ID='c0a80237c670f730b45753522de808b' "); //giftGiverName
		sql.append("inner join ORG_PROFILE_COMM b on a.PROFILE_ID=b.PROFILE_ID and b.ORGANIZATION_ID=? ");
		sql.append("left outer join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("WILLOWGREEN_COUNTER wc on a.CONTACT_SUBMITTAL_ID=wc.CONTACT_SUBMITTAL_ID ");
		sql.append("left outer join EMAIL_CAMPAIGN_LOG c on a.PROFILE_ID=c.PROFILE_ID and campaign_instance_id in (select campaign_instance_id from email_campaign_instance where EMAIL_CAMPAIGN_ID=?) ");
		sql.append("where a.ACTION_ID=? "); //this is actually actionGroupId, on the data level
		if (role.getRoleLevel() < SecurityController.ADMIN_ROLE_LEVEL)
			sql.append("and a.DEALER_LOCATION_ID=? ");
		sql.append("group by a.PROFILE_ID, a.DEALER_LOCATION_ID, a.contact_submittal_id, a.create_dt, b.ALLOW_COMM_FLG, cast(e.value_txt as text), cast(f.value_txt as text), record_no, a.create_dt ");
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
		int activeCnt = 0;
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		try {
			profiles = pm.searchProfileMap(dbConn, new ArrayList<String>(profiles.keySet()));
			for (ReportVO rpt : data) {
				rpt.setUser(profiles.get(rpt.getProfileId()));
				rpt.setSubmitter(profiles.get(rpt.getDealerLocationId()));
				
				//determine if the user is current getting emails
				if (rpt.getEmailCnt() < 100 && rpt.getAllowCommFlg() == 1)
					activeCnt++;
			}
			
		} catch (DatabaseException de) {
			log.error("could not retrieve profiles", de);
		} finally {
			pm = null;
		}
		
		req.setAttribute("activeNo", activeCnt);
		req.setAttribute("enrolledNo", data.size());
		super.putModuleData(data);
	}
	
	
	/**
	 * checks to see if the user has already recieved all of the emails in the series.
	 * 
	 * @param emailAddress
	 * @return
	 */
	protected boolean isEnrolled(String emailAddress, String emailCampaignId, String contactActionId) {
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

		//they have to still be enrolled in the contact_submittal table; this is where Jim deletes them from
		String sql2 = "select contact_submittal_id from contact_submittal where profile_id=? and action_id=?";
		log.debug(sql2 + " " + profileId + " " + contactActionId);
		try (PreparedStatement ps = dbConn.prepareStatement(sql2)) {
			ps.setString(1, profileId);
			ps.setString(2, contactActionId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) isEnrolled = true;
			
		} catch (SQLException sqle) {
			log.error("could not check contact_submittal table", sqle);
		}
		if (!isEnrolled) return isEnrolled;
		
		StringBuilder sql = new StringBuilder(400);
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
				isEnrolled = (rs.getInt(1) < rs.getInt(2) && rs.getInt(1) > 0);
				//log.debug("rcvd=" + rs.getInt(1) + " series=" + rs.getInt(2));
			}
		} catch (SQLException sqle) {
			log.error("could not lookup email count", sqle);
		}
		
		return isEnrolled;
	}
	
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	protected void incrementCounter(String csId, String series) {
		StringBuilder sql = new StringBuilder(100);
		sql.append("select max(record_no)+1 from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("WILLOWGREEN_COUNTER where series_txt=?");
		log.debug(sql + "|" + series);
		int recordNo = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, series);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) recordNo = rs.getInt(1);
		} catch (SQLException sqle) {
			log.error("could not increment counter", sqle);
		}
		
		if (recordNo == 0) recordNo = 1;
		
		sql = new StringBuilder(200);
		sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("WILLOWGREEN_COUNTER (counter_id, series_txt, contact_submittal_id, ");
		sql.append("record_no, create_dt) values (?,?,?,?,?)");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, series);
			ps.setString(3, csId);
			ps.setInt(4, recordNo);
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("could not increment counter", sqle);
		}
	}
	
	protected void convert(SMTServletRequest req) throws ActionException {
		//stub to be overloaded in DailyCaregiversEmailWrapper
	}
}
