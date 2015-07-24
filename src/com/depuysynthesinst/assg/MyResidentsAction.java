package com.depuysynthesinst.assg;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.depuysynthesinst.DSIUserDataVO;
import com.depuysynthesinst.DSIUserDataVO.RegField;
import com.depuysynthesinst.assg.ResidentVO.ResidentGrouping;
import com.depuysynthesinst.emails.InviteResidentVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>: MyResidentsAction.java<p/>
 * <b>Description: manages all stand-alone Resident actions. - DSI My Assignments admin side.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 8, 2015
 ****************************************************************************/
public class MyResidentsAction extends SBActionAdapter {

	public static final int CONSENT_TIMEOUT  = 10; //days that must lapse before a new invite can be sent
	
	/**
	 * 
	 */
	public MyResidentsAction() {
	}

	/**
	 * @param actionInit
	 */
	public MyResidentsAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	
	/**
	* loads a list of Residents for the logged-in Director
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		
		//load the list of residents
		if (req.hasParameter("search")) {
			if (StringUtil.isValidEmail(req.getParameter("search"))) {
				//Find or create the WC profile, add them as a Resident, and invite them. 
				//This mostly runs through the build() method
				findAndAddResident(req.getParameter("search"), req);
			} else {
				//search residents matching keyword and return a list of matches to choose from
				int resDirId = Convert.formatInteger("" + req.getSession().getAttribute(AssignmentsFacadeAction.RES_DIR_ID));
				mod.setActionData(searchResidents(req.getParameter("search"), site, resDirId));
			}
		} else {
			mod.setActionData(loadResidentList(user.getProfileId(), site));
		}
	}
	
	
	/**
	 * handles all the 'write' transactions related to managing Residents
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {
		String reqType = StringUtil.checkVal(req.getParameter("reqType"), null);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		
		ResidentVO resident = new ResidentVO(req);
		switch (reqType) {
			case "reinvite":
				this.invite(resident, site);
				break;
			case "delete":
				this.delete(resident);
				break;
			case "add":
				this.addResident(resident);
				this.invite(resident, site);
				break;
			case "search":
				findAndAddResident(req.getParameter("email"), req);
				break;
			case "manageProctor":
				this.manageMyDirector(resident, req);
				break;
		}
	}
	
	
	/**
	 * finds/adds a WC user and then creates their Resident account, and invites them.
	 * @param email
	 * @param req
	 * @throws ActionException
	 */
	private void findAndAddResident(String email, SMTServletRequest req) throws ActionException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		ProfileManager pm = ProfileManagerFactory.getInstance(getAttributes());
		UserDataVO user = new UserDataVO();
		try {
			user.setEmailAddress(email);
			user.setProfileId(pm.checkProfile(user, dbConn));
			
			if (user.getProfileId() == null) //create a new profile if need be - the user will come back later and register on the website
				pm.updateProfile(user, dbConn);
		} catch (DatabaseException de) {
			log.error("could not find user for email: " + email, de);
		}

		String msg = "Invitation+sent+to+" + user.getEmailAddress();
		ResidentVO resident = new ResidentVO(req);
		resident.setProfileId(user.getProfileId());
		try {
			this.addResident(resident);
			this.invite(resident, site);
		} catch (ActionException ae) {
			msg = ae.getMessage();
		}
		
		
		// Setup the redirect.
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder url = new StringBuilder();
		url.append(page.getRequestURI()).append("?view=").append(req.getParameter("view")); //display admin menus
		if (req.hasParameter("pg")) url.append("&pg=").append(req.getParameter("pg")); //admin page (include)
		url.append("&msg=").append(msg);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}
	
	
	/**
	 * searches through all registered Residents via keyword.
	 * If keyword is a valid email address, 
	 * @param searchKywd
	 * @return
	 * @throws ActionException
	 */
	private List<UserDataVO> searchResidents(String searchKywd, SiteVO site, int resDirId) throws ActionException {
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		List<UserDataVO> users = new ArrayList<>();
		ProfileManager pm = ProfileManagerFactory.getInstance(getAttributes());
		UserDataVO vo;
		
		//find users with a matching name & are registered users as Residents or Chief Residents
		StringBuilder sql = new StringBuilder(400);
		sql.append("select p.profile_id, p.first_nm, p.last_nm, p.email_address_txt ");
		sql.append("from PROFILE p ");
		sql.append("inner join PROFILE_ROLE pr on p.profile_id=pr.profile_Id and pr.role_id=? and pr.site_id=? and pr.status_id=? ");
		sql.append("inner join register_submittal rs on p.profile_id=rs.profile_id and rs.site_id=? ");  //the user's registration on the site
		sql.append("inner join register_data regd on rs.register_submittal_id=regd.register_submittal_id and regd.register_field_id=? "); //the user's Profession value
		sql.append("left outer join ").append(customDb).append("DPY_SYN_INST_RESIDENT r on p.profile_id=r.profile_id and r.res_dir_id=? ");
		sql.append("where (regd.value_txt=? or regd.value_txt=?) and p.SEARCH_LAST_NM=? and r.resident_id is null");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, "" + SecurityController.PUBLIC_REGISTERED_LEVEL);
			ps.setString(2, ((site.getAliasPathParentId() != null) ? site.getAliasPathParentId() : site.getSiteId())); //use parent's siteId
			ps.setInt(3, SecurityController.STATUS_ACTIVE);
			ps.setString(4, ((site.getAliasPathParentId() != null) ? site.getAliasPathParentId() : site.getSiteId())); //use parent's siteId
			ps.setString(5, RegField.c0a80241b71c9d40a59dbd6f4b621260.toString()); //Profession register_field_id
			ps.setInt(6, resDirId); //do not include users who are already tied to this resident director
			ps.setString(7, "RESIDENT"); //profession value
			ps.setString(8, "CHIEF"); //profession value
			ps.setString(9, pm.getEncValue("SEARCH_LAST_NM", searchKywd.toUpperCase()));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				vo = new UserDataVO();
				vo.setProfileId(rs.getString(1));
				vo.setFirstName(pm.getStringValue("FIRST_NM", rs.getString(2)));
				vo.setLastName(pm.getStringValue("LAST_NM", rs.getString(3)));
				vo.setEmailAddress(pm.getStringValue("EMAIL_ADDRESS_TXT", rs.getString(4)));
				users.add(vo);
			}
		} catch (SQLException sqle) {
			log.error("could not load residents", sqle);
		}
		
		return users;
	}
	
	
	/**
	 * loads a count of residents attached to the given Resident Director
	 * @param directorProfileId
	 * @return
	 * 		**CALLED FROM MyAssignmentsAdminAction**
	 */
	protected int loadResidentCount(String directorProfileId) {
		StringBuilder sql = new StringBuilder(100);
		sql.append("select count(*) from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_INST_RESIDENT r ");
		sql.append("inner join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_INST_RES_DIR rd on r.res_dir_id=rd.res_dir_id ");
		sql.append("where rd.profile_id=? and r.active_flg=1");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, directorProfileId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) return rs.getInt(1);
		} catch (SQLException sqle) {
			log.error("could not delete user assignment asset completion tag", sqle);
		}
		return 0;
	}
	
	
	/**
	 * loads a list of residents associated to the given Director
	 * @param directorProfileId
	 * @return
	 * @throws ActionException
	 */
	private ResidentGrouping loadResidentList(String directorProfileId, SiteVO site) throws ActionException {
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		List<ResidentVO> data = new ArrayList<>();
		List<String> profileIds = new ArrayList<>(50);
		StringBuilder sql = new StringBuilder(200);
		sql.append("select distinct r.resident_id, r.profile_id, regd.value_txt as pgy_id, ");
		sql.append("r.consent_dt, r.invite_sent_dt ");
		sql.append("from ").append(customDb).append("DPY_SYN_INST_RESIDENT r ");
		sql.append("inner join ").append(customDb).append("DPY_SYN_INST_RES_DIR rd on r.res_dir_id=rd.res_dir_id ");
		sql.append("left outer join register_submittal rs on r.profile_id=rs.profile_id and rs.site_id=? ");  //the user's registration on the site
		sql.append("left outer join register_data regd on rs.register_submittal_id=regd.register_submittal_id and regd.register_field_id=? "); //the user's PGY value
		sql.append("where rd.profile_id=? and r.active_flg=1");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, ((site.getAliasPathParentId() != null) ? site.getAliasPathParentId() : site.getSiteId())); //use parent's siteId
			ps.setString(2, RegField.DSI_PGY.toString());
			ps.setString(3, directorProfileId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				data.add(new ResidentVO(rs));
				profileIds.add(rs.getString("profile_id"));
			}
		} catch (SQLException sqle) {
			log.error("could not load residents", sqle);
		}
		
		//no residents, no profile lookup needed.
		if (profileIds.size() == 0) return new ResidentVO().new ResidentGrouping(data);
		
		//load profiles for all these residents
		ProfileManager pm = ProfileManagerFactory.getInstance(getAttributes());
		try {
			Map<String, UserDataVO> profiles = pm.searchProfileMap(dbConn, profileIds);
			
			for (ResidentVO res : data)
				res.setProfile(profiles.get(res.getProfileId()));
			
		} catch (DatabaseException de) {
			log.error("could not load profiles for Residents", de);
		}
		
		return new ResidentVO().new ResidentGrouping(data);
	}
	
	
	/**
	 * loads all of the residents attached to an Assignment; inclusive of their PGY
	 * and %complete for the assignment.
	 * To get %complete we need to load the resident's completion of the assets.
	 * We already know how many assets are in the assignment.
	 * @param assg
	 * @return
	 * 		**CALLED FROM MyAssignmentsAdminAction**
	 */
	protected void loadAssgResidents(AssignmentVO assg, SiteVO site, boolean fullDetail, String residentId) {
		if (fullDetail) {
			loadAssgResidents(assg, site, residentId, false);
			return;
		}
		
		//load some simple counts - we don't need all the resident data, only stats
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(200);
		sql.append("select r.resident_id, sum(case raa.complete_dt when null then 0 else 1 end) ");
		sql.append("from ").append(customDb).append("DPY_SYN_INST_RESIDENT r ");
		sql.append("inner join ").append(customDb).append("DPY_SYN_INST_RES_ASSG ra on r.resident_id=ra.resident_id ");
		sql.append("left outer join ").append(customDb).append("DPY_SYN_INST_RES_ASSG_ASSET raa on ra.res_assg_id=raa.res_assg_id ");
		sql.append("where ra.assg_id=? and r.active_flg=1 ");
		sql.append("group by r.resident_id");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, assg.getAssgId());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				assg.addResident(new ResidentVO(rs));
				assg.addResidentStats(rs.getString(1), rs.getInt(2));
			}
		} catch (SQLException sqle) {
			log.error("could not load list of assignment's residents", sqle);
		}
		
	}
	
		/**
		 * helper to above, loads full detail for the residents
		 * @param assg
		 * @param site
		 */
	private void loadAssgResidents(AssignmentVO assg, SiteVO site, String residentId, boolean outerJoinAssg) {
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		List<String> profileIds = new ArrayList<>(50);
		StringBuilder sql = new StringBuilder(200);
		sql.append("select distinct r.resident_id, r.profile_id, r.consent_dt, r.invite_sent_dt, regd.value_txt as pgy_id, ra.res_assg_id ");
		sql.append("from ").append(customDb).append("DPY_SYN_INST_RESIDENT r ");
		if (outerJoinAssg) {
			sql.append("left outer join ").append(customDb).append("DPY_SYN_INST_RES_ASSG ra on r.resident_id=ra.resident_id and ra.assg_id=? ");
		} else {
			sql.append("inner join ").append(customDb).append("DPY_SYN_INST_RES_ASSG ra on r.resident_id=ra.resident_id and ra.assg_id=? ");
		}
		sql.append("left outer join ").append(customDb).append("DPY_SYN_INST_RES_ASSG_ASSET raa on ra.res_assg_id=raa.res_assg_id ");
		sql.append("left outer join register_submittal rs on r.profile_id=rs.profile_id and rs.site_id=? ");  //the user's registration on the site
		sql.append("left outer join register_data regd on rs.register_submittal_id=regd.register_submittal_id and regd.register_field_id=? "); //the user's PGY value
		sql.append("where r.res_dir_id=? and r.active_flg=1");
		if (residentId != null) sql.append("and r.resident_id=?");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, assg.getAssgId());
			ps.setString(2, ((site.getAliasPathParentId() != null) ? site.getAliasPathParentId() : site.getSiteId())); //use parent's siteId
			ps.setString(3, RegField.DSI_PGY.toString());
			ps.setInt(4, assg.getResDirId());
			if (residentId != null) ps.setString(5, residentId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				assg.addResident(new ResidentVO(rs));
				profileIds.add(rs.getString("profile_id"));
			}
			
		} catch (SQLException sqle) {
			log.error("could not load list of assignment's residents", sqle);
		}
		
		//no residents, no profile lookup needed.
		if (profileIds.size() == 0) return;
		
		//load profiles for all these residents
		ProfileManager pm = ProfileManagerFactory.getInstance(getAttributes());
		try {
			Map<String, UserDataVO> profiles = pm.searchProfileMap(dbConn, profileIds);
			
			for (ResidentVO res : assg.getResidents())
				res.setProfile(profiles.get(res.getProfileId()));

		} catch (DatabaseException de) {
			log.error("could not load profiles for Residents", de);
		}
	}
	
	
	protected void loadMyResidents(AssignmentVO assg, SiteVO site, String residentId) {
		loadAssgResidents(assg, site, residentId, true);
	}
	
	
	/**
	 * Invite
	 * @param resident
	 * @throws ActionException
	 */
	private void invite(ResidentVO resident, SiteVO site) throws ActionException {
		//mark DB record as updated, so we know when the inquiry was sent.
		StringBuilder sql = new StringBuilder(100);
		sql.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_INST_RESIDENT set invite_sent_dt=? where resident_id=?");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setTimestamp(1, Convert.getCurrentTimestamp());
			ps.setString(2, resident.getResidentId());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("could not detach Director from Resident", sqle);
		}
		
		//load a complete profile for the invitee so we can customize the email
		ProfileManager pm = ProfileManagerFactory.getInstance(getAttributes());
		try {
			resident.setProfile(pm.getProfile(resident.getProfileId(), dbConn, ProfileManager.PROFILE_ID_LOOKUP, site.getOrganizationId()));
		} catch (DatabaseException de) {
			log.error("could not load user profile", de);
		}
		
		UserDataVO resDir = getResDirProfile(resident.getResidentDirectorId(), site.getOrganizationId());
		
		//send the email invite asking the Resident to acknowledge their Director
		try {
			InviteResidentVO mail = new InviteResidentVO();
			mail.setFrom(site.getMainEmail());
			mail.addRecipient(resident.getProfile().getEmailAddress());
			mail.buildMessage(resident.getProfile(), resDir, site); //builds subject and message body automatically

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
		} catch (Exception ide) {
			log.error("could not send invite email", ide);
		}
	}
	
	
	/**
	 * gets the profileId for the given resident_director record, then does a 
	 * ProfileManager lookup for the UserDataVO of that person.
	 * @param resDirId
	 * @param orgId
	 * @return
	 */
	private UserDataVO getResDirProfile(int resDirId, String orgId) {
		StringBuilder sql = new StringBuilder(100);
		sql.append("select profile_id from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_INST_RES_DIR where RES_DIR_ID=?");
		log.debug(sql);
		
		String resDirProfileId = "";
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, resDirId);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				resDirProfileId = rs.getString(1);
			
		} catch (SQLException sqle) {
			log.error("could not load resDir profileId", sqle);
		}
		
		//load a complete profile for the resDir so we can customize the email
		ProfileManager pm = ProfileManagerFactory.getInstance(getAttributes());
		try {
			return pm.getProfile(resDirProfileId, dbConn, ProfileManager.PROFILE_ID_LOOKUP, orgId);
		} catch (DatabaseException de) {
			log.error("could not load user profile", de);
		}
		return new UserDataVO();
	}
	
	
	/**
	 * Detaches a Resident from the superintendency of 'this' Director.  
	 * Does not actually delete the Resident.
	 * @param resident
	 * @throws ActionException
	 */
	private void delete(ResidentVO resident) throws ActionException {
		StringBuilder sql = new StringBuilder(100);
		sql.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_INST_RESIDENT set active_flg=0, update_dt=? where resident_id=?");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setTimestamp(1, Convert.getCurrentTimestamp());
			ps.setString(2, resident.getResidentId());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("could not detach Director from Resident", sqle);
		}
	}
	
	
	/**
	 * writes an Resident to the database
	 * @param resident
	 * @throws ActionException
	 */
	private void addResident(ResidentVO resident) throws ActionException {
		//first make sure the resident isn't already on the roster
		StringBuilder sql = new StringBuilder(150);
		sql.append("select active_flg, resident_id from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_INST_RESIDENT where profile_id=? and res_dir_id=?");
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, resident.getProfileId());
			ps.setInt(2, resident.getResidentDirectorId());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				if (rs.getInt(1) == 1) {
					throw new ActionException("Resident already enrolled");
				} else {
					//re-activate the already-enrolled resident
					resident.setResidentId(rs.getString(2));
					reactivateResident(resident);
					return;
				}
			}
		} catch (SQLException sqle) {
			log.error("could not verify duplicate resident", sqle);
		}

		//insert the RESIDENT table
		if (resident.getResidentId() == null)
			resident.setResidentId(new UUIDGenerator().getUUID());
			
		sql = new StringBuilder(150);
		sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_INST_RESIDENT (RES_DIR_ID, PROFILE_ID, ACTIVE_FLG, ");
		sql.append("CREATE_DT, RESIDENT_ID) values (?,?,?,?,?)");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, resident.getResidentDirectorId());
			ps.setString(2, resident.getProfileId());
			ps.setInt(3, 1);
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			ps.setString(5, resident.getResidentId());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("could not add new Resident", sqle);
			throw new ActionException("Could not add Resident - user not found");
		}
	}
	
	
	/**
	 * re-enrolls a previously deleted Resident from a Director's superintendency
	 * @param resident
	 * @throws ActionException
	 */
	private void reactivateResident(ResidentVO resident) throws ActionException {
		StringBuilder sql = new StringBuilder(100);
		sql.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_INST_RESIDENT set active_flg=1, update_dt=? where resident_id=?");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setTimestamp(1, Convert.getCurrentTimestamp());
			ps.setString(2, resident.getResidentId());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			throw new ActionException("could not re-activate Resident");
		}
	}
	
	
	/**
	 * 
	 * @param resident
	 * @param req
	 * @throws ActionException
	 */
	private void manageMyDirector(ResidentVO resident, SMTServletRequest req) throws ActionException {
		StringBuilder sql = new StringBuilder(100);
		boolean isRevoke = Convert.formatBoolean(req.getParameter("revokeDirector"));
		if (isRevoke) {
			sql.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DPY_SYN_INST_RESIDENT set active_flg=0, update_dt=? where resident_id=?");
		} else {
			sql.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DPY_SYN_INST_RESIDENT set active_flg=1, consent_dt=? where resident_id=?");
		}
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setTimestamp(1, Convert.getCurrentTimestamp());
			ps.setString(2, resident.getResidentId());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("could not update MyDirector for Resident", sqle);
		}
		
		//if the user is accepting invitations, remove this one from their pending list
		DSIUserDataVO user = DSIUserDataVO.getInstance(req.getSession().getAttribute(Constants.USER_DATA));
		if (user != null && user.getPendingResDirs() != null) {
			user.getPendingResDirs().remove(resident.getResidentId());
			req.getSession().setAttribute(Constants.USER_DATA, user);
		}
		
	}
}