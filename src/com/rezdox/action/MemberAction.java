package com.rezdox.action;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rezdox.action.RezDoxUtils.Product;
//WC Custom
import com.rezdox.data.MemberFormProcessor;
import com.rezdox.vo.MemberVO;
import com.rezdox.vo.MembershipVO;
import com.rezdox.vo.PromotionVO;
import com.siliconmtn.action.ActionControllerFactoryImpl;
//SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.http.CookieUtil;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.io.mail.EmailRecipientVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
//WC Core
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.form.FormAction;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerUtil;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.security.UserLogin;
import com.smt.sitebuilder.util.CampaignMessageSender;

/****************************************************************************
 * <b>Title</b>: MemberAction.java<p/>
 * <b>Description: New user registration and logged-in user profile management/settings.
 * Facades account creation, residence creation & related workflows.</b>
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Apr 16, 2018
 ****************************************************************************/
public class MemberAction extends SimpleActionAdapter {

	public static final String REQ_MEMBER_ID = "memberId";
	public static final String REQ_PROFILE_ID = "profileId";

	public MemberAction() {
		super();
	}


	/**
	 * @param actionInit
	 */
	public MemberAction(ActionInitVO actionInit) {
		super(actionInit);
	}


	/**
	 * @param dbConnection
	 * @param attributes
	 */
	public MemberAction(SMTDBConnection dbConnection, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConnection);
		setAttributes(attributes);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String formId = RezDoxUtils.getFormId(getAttributes());
		log.debug("Retrieving Member Form: " + formId);

		// Get the form
		DataManagerUtil util = new DataManagerUtil(getAttributes(), getDBConnection());
		DataContainer dc = util.loadForm(formId, req, MemberFormProcessor.class);

		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		mod.setAttribute(FormAction.FORM_DATA, dc);
		mod.setActionData(dc.getForm());
	}


	/**
	 * overloaded for backwards compatability
	 * @param memberId
	 * @return
	 */
	public MemberVO retrieveMemberData(String memberId) {
		return retrieveMemberData(memberId, null);
	}


	/**
	 * Retrieves member settings data for the specified memberId or profileId
	 * Overloaded to support the login module
	 * @param memberId
	 * @param profileId
	 * @return
	 */
	public MemberVO retrieveMemberData(String memberId, String profileId) {
		String sql;
		List<Object> params;
		if (!StringUtil.isEmpty(profileId)) {
			sql = StringUtil.join(DBUtil.SELECT_FROM_STAR, getCustomSchema(), "rezdox_member where profile_id=?");
			params = Arrays.asList(profileId);
		} else {
			sql = StringUtil.join(DBUtil.SELECT_FROM_STAR, getCustomSchema(), "rezdox_member where member_id=?");
			params = Arrays.asList(memberId);
		}

		DBProcessor dbp = new DBProcessor(getDBConnection());
		List<MemberVO> data = dbp.executeSelect(sql, params, new MemberVO());
		return !data.isEmpty() ? data.get(0) : new MemberVO();
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		SMTSession session = req.getSession();
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		MemberVO member = (MemberVO) session.getAttribute(Constants.USER_DATA);

		//support deleting a member entirely
		if (req.hasParameter("deleteUser")) {
			req.setParameter(REQ_MEMBER_ID, member.getMemberId());
			saveMember(req, true);
			CookieUtil.remove(req, Constants.USER_COOKIE);
			session.invalidate();
			return;
		}

		//capture if this is a new user signing up for the first time.
		boolean isNewUser = member == null || StringUtil.isEmpty(member.getProfileId());

		//set orgProfileComm=1 for all users - they can only opt out from email campaigns
		req.setParameter("allowCommunication", "1");
		req.setParameter("countryCode", "US"); //for good measure - until they add it to the UI

		//save the FormBuilder piece - this will save through to ProfileManager for us
		saveForm(req);

		//save the custom table - REZDOX_MEMBER
		member = saveMember(req, false);

		//overseed the MemberVO with the profile data collected by the form processor
		member.setData(req, true);

		//save auth record
		saveAuthRecord(member);

		//if new user - create a login account & log the user in for the first time
		if (isNewUser) {
			createProfileRole(member, site);
			performLogin(member, site, req);
			setupMembership(member, req);
			connectionToRezdox(member, req);
		}

		//put the member VO onto their session, refreshing any data that's already there.
		session.setAttribute(Constants.USER_DATA, member);
	}


	/**
	 * create or update the authentication record
	 * @param user
	 * @throws ActionException
	 */
	private void saveAuthRecord(UserDataVO user) throws ActionException {
		UserLogin login = new UserLogin(getDBConnection(), getAttributes());
		String newAuthId = null;
		try {
			if (StringUtil.isEmpty(user.getAuthenticationId()))
				user.setAuthenticationId(login.checkAuth(user.getEmailAddress()));

			newAuthId = login.saveAuthRecord(user.getAuthenticationId(), user.getEmailAddress(), user.getPassword(), 0);
			log.debug(String.format("authIds: %s || %s", user.getAuthenticationId(), newAuthId));
		} catch (DatabaseException e) {
			throw new ActionException("could not create auth record", e);
		}

		//coming off registration, update the user's profile with the new authId.  This is out of place, but because of the FormProcessor creating the profile
		if (newAuthId != user.getAuthenticationId()) { //when this is commonly true user.getAuthenticationId()==nulll
			user.setAuthenticationId(newAuthId);
			String sql = "update profile set authentication_id=? where profile_id=?";
			try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
				ps.setString(1, newAuthId);
				ps.setString(2,  user.getProfileId());
				ps.executeUpdate();
			} catch (SQLException sqle) {
				log.error("could not save authId to profile", sqle);
			}
		}
	}


	/**
	 * create the profile role record so the user can login
	 * @param user
	 * @param site
	 * @throws ActionException
	 */
	private void createProfileRole(UserDataVO user, SiteVO site) throws ActionException {
		ProfileRoleManager prm = new ProfileRoleManager();
		try {
			if (!prm.roleExists(user.getProfileId(), site.getSiteId(), null, getDBConnection())) {
				SBUserRole role = new SBUserRole(site.getSiteId());
				role.setProfileId(user.getProfileId());
				role.setRoleId(Integer.toString(SecurityController.PUBLIC_REGISTERED_LEVEL));
				role.setStatusId(SecurityController.STATUS_ACTIVE);
				prm.addRole(role, getDBConnection());
			}
		} catch (Exception e) {
			throw new ActionException("could not create profile role", e);
		}
	}


	/**
	 * perform initial login for this new user
	 * @param user
	 * @param site
	 * @param req
	 * @throws ActionException
	 */
	private void performLogin(UserDataVO user, SiteVO site, ActionRequest req) throws ActionException {
		try {
			SecurityController sc = new SecurityController(site.getLoginModules(), site.getRoleModule(), getAttributes());
			sc.authorizeUser(user, req, site, dbConn);
		} catch (Exception e) {
			throw new ActionException("could not auto-login user after registration", e);
		}
	}


	/**
	 * Saves a member form builder form
	 */
	protected void saveForm(ActionRequest req) {
		String formId = RezDoxUtils.getFormId(getAttributes());

		// Place ActionInit on the Attributes map for the Data Save Handler.
		setAttribute(Constants.ACTION_DATA, actionInit);

		// Call DataManagerUtil to save the form.
		new DataManagerUtil(getAttributes(), getDBConnection()).saveForm(formId, req, MemberFormProcessor.class);
	}


	/**
	 * Saves the MemberVO to the DB - called after MemberFormProcessor, which moves data off the form
	 * and onto the request object.
	 * @param req
	 * @throws DatabaseException
	 * @throws InvalidDataException
	 */
	public MemberVO saveMember(ActionRequest req, boolean isDelete) throws ActionException {
		MemberVO vo = new MemberVO(req);

		// Save the member's updated settings
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (isDelete) {
				dbp.delete(vo);
			} else {
				dbp.save(vo);
			}
		} catch(Exception e) {
			throw new ActionException("could not save member", e);
		}
		return vo;
	}


	/**
	 * Create a list of members in the system, agnostic of login ability or status.
	 * used for the residence transfer UI.  Exclude the user ('self')
	 * Called from ResidenceAction
	 * @param req
	 * @return
	 */
	public List<MemberVO> listMembers(ActionRequest req) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(150);
		sql.append("select member_id, profile_id, first_nm, last_nm, email_address_txt ");
		sql.append("from ").append(schema).append("REZDOX_MEMBER ");
		sql.append("where member_id != ? and profile_id is not null and email_address_txt is not null ");
		sql.append("order by last_nm, first_nm");

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), Arrays.asList(RezDoxUtils.getMemberId(req)), new MemberVO());
	}


	/**
	 * Handles the required steps for setting up a new member
	 * @param user
	 * @param req
	 * @throws ActionException
	 */
	private void setupMembership(MemberVO member, ActionRequest req) throws ActionException {
		// Get default member subscription... the only default after 6/1/2019 is HomeOwner.
		// Free business and residence subscriptions are added by member selection after signing up.
		MembershipAction ma = new MembershipAction(dbConn, attributes);
		MembershipVO membership = ma.retrieveDefaultMembership(req, Product.BUSINESS.name());

		// Get the "Free" promotion used when signing up
		PromotionAction pa = new PromotionAction(dbConn, attributes);
		PromotionVO promotion = pa.retrieveFreePromotion();

		// Give the member their free subscription
		SubscriptionAction sa = new SubscriptionAction(dbConn, attributes);
		sa.addSubscription(member, membership, promotion);

		//apply the default reward give to all new users at first login
		RewardsAction ra = new RewardsAction(getDBConnection(), getAttributes());
		ra.applyReward(RezDoxUtils.NEW_REGISTRANT_REWARD, member.getMemberId(), req);

		//send welcome email
		sendWelcomeEmail(member);
	}


	/**
	 * Leverages Email Campaigns to trigger a welcome email to the new member
	 * @param member
	 */
	private void sendWelcomeEmail(MemberVO member) {
		// Put the mail merge data onto the map
		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put("firstName", member.getFirstName());
		dataMap.put("emailAddress", member.getEmailAddress());

		// Add the recipient
		List<EmailRecipientVO> rcpts = new ArrayList<>();
		rcpts.add(new EmailRecipientVO(member.getProfileId(), member.getEmailAddress(), EmailRecipientVO.TO));

		// Send the email
		CampaignMessageSender util = new CampaignMessageSender(getAttributes());
		util.sendMessage(dataMap, rcpts, RezDoxUtils.EmailSlug.WELCOME.name());
	}


	/**
	 * Create a connection between this user and the Rezdox business, so the user has an initial connection.
	 * Do not send notif emails for this one.
	 * Note: if the user later decides to create a business, a second connection will be created between Rezdox and their business.
	 * Business users see business connections, so having both won't display a duplicate.
	 * @param member
	 * @param req
	 * @throws ActionException 
	 */
	private void connectionToRezdox(MemberVO member, ActionRequest req) throws ActionException {
		ConnectionAction ca = ActionControllerFactoryImpl.loadAction(ConnectionAction.class, this, true);
		req.setParameter("senderBusinessId", RezDoxUtils.REZDOX_BUSINESS_ID);
		req.setParameter("recipientMemberId", member.getMemberId());
		req.setParameter("approvedFlag", "1");
		req.setParameter("skipEmails", "1");
		ca.build(req);
	}
}
