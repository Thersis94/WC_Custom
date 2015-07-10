package com.depuysynthesinst.assg;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.depuysynthesinst.DSIUserDataVO.RegField;
import com.depuysynthesinst.assg.ResidentVO.ResidentGrouping;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
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
				mod.setActionData(searchResidents(req.getParameter("search")));
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
				if (StringUtil.isValidEmail(req.getParameter("email")))
					findAndAddResident(req.getParameter("email"), req);
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
		
		//invoke build(), which will add and invite this Resident
		req.setParameter("reqType", "add");
		req.setParameter("profileId", user.getProfileId());
		this.build(req);
		
		// Setup the redirect.
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder url = new StringBuilder();
		url.append(page.getRequestURI()).append("?view=").append(req.getParameter("view")); //display admin menus
		if (req.hasParameter("pg")) url.append("&pg=").append(req.getParameter("pg")); //admin page (include)
		url.append("&msg=Invitation+sent+to+").append(user.getEmailAddress());
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
	private List<UserDataVO> searchResidents(String searchKywd) throws ActionException {
		List<UserDataVO> users = new ArrayList<>();
		
		//TODO
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
		sql.append("where rd.profile_id=?");
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
		sql.append("select r.resident_id, r.profile_id, regd.value_txt as pgy_id, r.consent_dt, ");
		sql.append("isnull(r.update_dt, r.create_dt) as update_dt ");
		sql.append("from ").append(customDb).append("DPY_SYN_INST_RESIDENT r ");
		sql.append("inner join ").append(customDb).append("DPY_SYN_INST_RES_DIR rd on r.res_dir_id=rd.res_dir_id ");
		sql.append("left outer join register_submittal rs on r.profile_id=rs.profile_id and rs.site_id=? ");  //the user's registration on the site
		sql.append("left outer join register_data regd on rs.register_submittal_id=regd.register_submittal_id and regd.register_field_id=? "); //the user's PGY value
		sql.append("where rd.profile_id=?");
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
	protected void loadAssgResidents(AssignmentVO assg) {
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		List<String> profileIds = new ArrayList<>(50);
		StringBuilder sql = new StringBuilder(200);
		sql.append("select r.resident_id, r.profile_id, r.consent_dt, isnull(r.update_dt, r.create_dt) as update_dt "); //update_dt will tell us we haven't touched the record in 10 days and they still haven't consented.
		sql.append("from ").append(customDb).append("DPY_SYN_INST_RESIDENT r ");
		sql.append("inner join ").append(customDb).append("DPY_SYN_INST_RES_ASSG ra on r.resident_id=ra.resident_id ");
		sql.append("left outer join ").append(customDb).append("DPY_SYN_INST_RES_ASSG_ASSET raa on ra.res_assg_id=raa.res_assg_id ");
		sql.append("where ra.assg_id=? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, assg.getAssgId());
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
	
	

	
	/**
	 * Invite
	 * @param resident
	 * @throws ActionException
	 */
	private void invite(ResidentVO resident, SiteVO site) throws ActionException {
		//mark DB record as updated, so we know when the inquiry was sent.
		StringBuilder sql = new StringBuilder(100);
		sql.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_INST_RESIDENT set update_dt=? where resident_id=?");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setTimestamp(1, Convert.getCurrentTimestamp());
			ps.setString(2, resident.getResidentId());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("could not detach Director from Resident", sqle);
		}
		
		//send the email invite asking the Resident to acknowledge their Director
		try {
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipient(resident.getProfile().getEmailAddress());
			mail.setSubject("Invitation from DSI");
			mail.setFrom(site.getMainEmail());
//			mail.setHtmlBody(msg.toString());

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
		} catch (InvalidDataException ide) {
			log.error("could not send invite email", ide);
		}
	}
	
	
	/**
	 * Detaches a Resident from the superintendency of 'this' Director.  
	 * Does not actually delete the Resident.
	 * @param resident
	 * @throws ActionException
	 */
	private void delete(ResidentVO resident) throws ActionException {
		StringBuilder sql = new StringBuilder(100);
		sql.append("update from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_INST_RESIDENT set res_assg_id=null, update_dt=? where resident_id=?");
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
		sql.append("select 1 from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_INST_RESIDENT where profile_id=? and res_dir_id=?");
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, resident.getProfileId());
			ps.setInt(2, resident.getResidentDirectorId());
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				throw new ActionException("resident already enrolled");
		} catch (SQLException sqle) {
			log.error("could not verify duplicate resident", sqle);
		}

		//insert the RESIDENT table
		if (resident.getResidentId() == null)
			resident.setResidentId(new UUIDGenerator().getUUID());
			
		sql = new StringBuilder(150);
		sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_INST_RESIDENT (RES_DIR_ID, PROFILE_ID, ");
		sql.append("CREATE_DT, RESIDENT_ID) values (?,?,?,?)");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, resident.getResidentDirectorId());
			ps.setString(2, resident.getProfileId());
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.setString(4, resident.getResidentId());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("could not add new Resident", sqle);
		}
	}
}