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
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
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
public class GriefEmailWrapper extends EmailWrapper {

	public GriefEmailWrapper() {
		super();
	}

	public GriefEmailWrapper(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.willowgreen.action.EmailWrapper#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		req.setAttribute("series", "GHP");
		super.build(req);
	}

	/**
	 * for GHP, we do not run this check for duplicate enrollments.
	 */
	protected boolean isEnrolled(String emailAddress, String emailCampaignId) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see com.willowgreen.action.EmailWrapper#loadReport(com.siliconmtn.action.ActionRequest, java.lang.String, java.lang.String)
	 */
	@Override
	protected void loadReport(ActionRequest req, String contactActionId, String emailCampaignId) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);

		Map<String, UserDataVO> profiles = new HashMap<>();
		List<ReportVO> data = new LinkedList<>();

		StringBuilder sql = new StringBuilder(1000);
		sql.append("select a.profile_id, a.DEALER_LOCATION_ID, cast(e.value_txt as text) as home_nm, ");
		sql.append("cast(f.value_txt as text) as deceased_nm, cast(g.value_txt as text) as deceased_dt, ");
		sql.append("cast(h.value_txt as text) as relationship, a.contact_submittal_id, ");
		sql.append("a.create_dt as first_dt, MAX(c.CREATE_DT) as last_dt, cast(i.value_txt as text) as gifter_nm,");
		sql.append("COUNT(c.CAMPAIGN_LOG_ID) as email_cnt, b.ALLOW_COMM_FLG, wc.record_no ");
		sql.append("from CONTACT_SUBMITTAL a ");
		sql.append("inner join CONTACT_DATA f on a.CONTACT_SUBMITTAL_ID=f.CONTACT_SUBMITTAL_ID and f.CONTACT_FIELD_ID='c0a802375319b3d0302c74e29ae6d39b' "); //deceasedNm
		sql.append("inner join CONTACT_DATA g on a.CONTACT_SUBMITTAL_ID=g.CONTACT_SUBMITTAL_ID and g.CONTACT_FIELD_ID='c0a80237531a65d43affa50416c0d30a' "); //deceasedDt
		sql.append("inner join CONTACT_DATA h on a.CONTACT_SUBMITTAL_ID=h.CONTACT_SUBMITTAL_ID and h.CONTACT_FIELD_ID='c0a80237531a24551878bce08195a3ff' "); //relationship
		sql.append("left outer join CONTACT_DATA e on a.CONTACT_SUBMITTAL_ID=e.CONTACT_SUBMITTAL_ID and e.CONTACT_FIELD_ID='c0a802411e3054ad416c9a2fe6b86c4b' "); //funeralHomeName (org/hospice name)
		sql.append("left outer join CONTACT_DATA i on a.CONTACT_SUBMITTAL_ID=i.CONTACT_SUBMITTAL_ID and i.CONTACT_FIELD_ID='c0a802378ce466298419508db034e2c' "); //giftee
		sql.append("inner join ORG_PROFILE_COMM b on a.PROFILE_ID=b.PROFILE_ID and b.ORGANIZATION_ID=? ");
		sql.append("left outer join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("WILLOWGREEN_COUNTER wc on a.CONTACT_SUBMITTAL_ID=wc.CONTACT_SUBMITTAL_ID ");
		sql.append("left outer join EMAIL_CAMPAIGN_LOG c on a.PROFILE_ID=c.PROFILE_ID and campaign_instance_id in (select campaign_instance_id from email_campaign_instance where EMAIL_CAMPAIGN_ID=?) ");
		sql.append("inner join SB_ACTION sa on a.action_id=sa.action_group_id ");
		sql.append("where a.ACTION_ID=? and sa.pending_sync_flg=0 ");
		if (role.getRoleLevel() < SecurityController.ADMIN_ROLE_LEVEL)
			sql.append("and a.DEALER_LOCATION_ID=? ");
		sql.append("group by a.PROFILE_ID, a.DEALER_LOCATION_ID, a.contact_submittal_id, a.create_dt, b.ALLOW_COMM_FLG, cast(e.value_txt as text), cast(f.value_txt as text), cast(g.value_txt as text), cast(h.value_txt as text), cast(i.value_txt as text),record_no ");
		sql.append("order by record_no desc");
		log.debug(sql + " " + contactActionId + " " + emailCampaignId + " " + site.getOrganizationId());

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, site.getOrganizationId());
			ps.setString(2, emailCampaignId);
			ps.setString(3, contactActionId);
			if (role.getRoleLevel() < SecurityController.ADMIN_ROLE_LEVEL)
				ps.setString(4, role.getProfileId());

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				UserDataVO user = new UserDataVO();
				user.setProfileId(rs.getString(1));
				profiles.put(user.getProfileId(), user);

				if (rs.getString(2) != null) {
					UserDataVO user2 = new UserDataVO();
					user2.setProfileId(rs.getString(2));
					profiles.put(user2.getProfileId(), user2);
				}

				data.add(new ReportVO(rs));
			}

		} catch (SQLException sqle) {
			log.error("could not load report", sqle);
		}

		//put a profile to all these peeps!
		int activeCnt = 0;
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		try {
			pm.populateRecords(dbConn, new ArrayList<>(profiles.values()));

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

}
