package com.rezdox.action;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.rezdox.data.MemberFormProcessor;
import com.rezdox.vo.MemberVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.form.FormAction;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerUtil;
import com.smt.sitebuilder.data.vo.FormVO;

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
		
		ModuleVO mod = (ModuleVO) getAttribute(Contants.MODULE_DATA);
		mod.setAttribute(FormAction.FORM_DATA, dc);
		mod.setActionData(dc.getForm());
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
		UserDataVO user = (UserDataVO) session.getAttribute(Constants.USER_DATA);

		//capture if this is a new user signing up for the first time.
		boolean isNewUser = user == null || StringUtil.isEmpty(user.getProfileId());

		//save the FormBuilder piece - this will save through to ProfileManager for us automatically
		//TODO make sure profile manager is called along the way, but AFTER the form processor
		saveForm(req);

		//save the custom table - REZDOX_MEMBER
		user = saveMember(req);
		
		//TODO user here will be missing all their profile data

		//save auth record
		//TODO check if this is done upstream
		saveAuthRecord(user);

		//if new user - create a login account & log the user in for the first time
		if (isNewUser) {
			createProfileRole(user, site);
			performLogin(user, site, req);
			setupMembership(user, req);
		}

		//put the member VO onto their session, refreshing any data that's already there.
		req.getSession().setAttribute(Constants.USER_DATA, user);

		//new users get welcomed with a dashboard redirect - setup by the login module already.
		String redirPg = isNewUser ? RezDoxUtils.SUBSCRIPTION_UPGRADE_PATH : RezDoxUtils.PROFILE_PATH;
		sendRedirect(redirPg, null, req);
	}


	/**
	 * TODO create or update the authentication record
	 **/
	private void saveAuthRecord(UserDataVO user) {
		UserLogin login = new UserLogin(getDBConnection(), getAttributes());
		login.saveAuthRecord(user.getAuthenticationId(), user.getEmailAddress(), user.getPassword(), 0);
	}


	/**
	 * TODO create the profile role record so the user can login
	 **/
	private void createProfileRole(UserDataVO user, SiteVO site) throws ActionException {
		ProfileRoleManager prm = new ProfileRoleManager();
	//	try {
			if (!prm.roleExists(user.getProfileId(), site.getSiteId(), null, getDBConnection())) {
				SBUserRole role = new SBUserRole(site.getSiteId());
				role.setProfileId(user.getProfileId());
				role.setRoleId(Integer.toString(SecurityController.PUBLIC_REGISTERED_LEVEL));
				role.setStatusId(SecurityController.STATUS_ACTIVE);
				prm.addRole(role, getDBConnection());
			}
	//	} catch (Exception e) {
	//		throw new ActionException("could not createa profile role", e);
	//	}
	}


	/**
	 * TODO perform initial login for this new user
	 **/
	private void performLogin(UserDataVO user, SiteVO site, ActionRequest req) {
	//	try {
			SecurityController sc = new SecurityController(site.getLoginModule(), site.getRoleModule(), getAttributes());
			sc.authorizeUser(user, req, site, dbConn);
	//	} catch (Exception e) {
	//		throw new ActionException("could not auto-login user after registration", e);
	//	}
	}


	/**
	 * Saves a member form builder form
	 */
	protected void saveForm(ActionRequest req) {
		String formId = RezDoxUtils.getFormId(getAttributes());

		// Place ActionInit on the Attributes map for the Data Save Handler.
		setAttribute(Constants.ACTION_DATA, actionInit);

		// Call DataManagerUtil to save the form.
		new DataManagerUtil(attributes, dbConn).saveForm(formId, req, MemberFormProcessor.class);
	}


	/**
	 * Saves the MemberVO to the DB - called after MemberFormProcessor, which moves data off the form
	 * and onto the request object.
	 * @param req
	 * @throws DatabaseException
	 * @throws InvalidDataException
	 */
	public MemberVO saveMember(ActionRequest req) throws ActionException {
		MemberVO vo = new MemberVO(req);

		// Save the member's updated settings
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			dbp.save(vo);
		} catch(Exception e) {
			throw new ActionException(e);
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
		// Get default member subscription... the only default right now is "100 Connections".
		// Free business and residence subscriptions are added by member selection after signing up.
		MembershipAction ma = new MembershipAction(dbConn, attributes);
		MembershipVO membership = ma.retrieveDefaultMembership(Group.CO);

		// Get the "Free" promotion used when signing up
		PromotionAction pa = new PromotionAction(dbConn, attributes);
		PromotionVO promotion = pa.retrieveFreePromotion();

		// Give the member their free subscription
		SubscriptionAction sa = new SubscriptionAction(dbConn, attributes);
		sa.addSubscription(member, membership, promotion);

		//apply the default reward give to all new users at first login
		RewardsAction ra = new RewardsAction(getDBConnection(), getAttributes());
		ra.applyReward(RezDoxUtils.NEW_REGISTRANT_REWARD, member.getMemberId());
	}
}
