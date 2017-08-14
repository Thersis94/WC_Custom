package com.biomed.smarttrak.admin;

//Java 8
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.user.HumanNameIntfc;
import com.siliconmtn.util.user.LastNameComparator;

// WebCrescendo
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.registration.RegistrationAction;
import com.smt.sitebuilder.action.registration.ResponseLoader;
import com.smt.sitebuilder.action.registration.SubmittalAction;
import com.smt.sitebuilder.action.registration.SubmittalDataVO;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.security.UserLogin;
import com.smt.sitebuilder.security.WCUtil;

//WC_Custom
import com.biomed.smarttrak.vo.UserVO;
import com.biomed.smarttrak.vo.UserVO.RegistrationMap;
import com.biomed.smarttrak.action.AdminControllerAction;

/*****************************************************************************
 <p><b>Title</b>: AccountUserAction.java</p>
 <p><b>Description: Ties Users to Accounts for Smartrak. Supports creating new users and managing 
 			core user profile data (incl core registration data)</b></p>
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Feb 2, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class AccountUserAction extends SBActionAdapter {

	protected static final String ACCOUNT_ID = AccountAction.ACCOUNT_ID; //req param
	protected static final String USER_ID = "userId"; //req param

	public AccountUserAction() {
		super();
	}

	public AccountUserAction(ActionInitVO actionInit) {
		super(actionInit);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		//if loginAs was passed, log-out the admin and log-in the desired user.  Login occurs after redirect, we're just doing the prep
		if (req.hasParameter("loginAs")) {
			loginAsUser(req);
			return;
		}

		List<Object> users = loadAccountUsers(req, null);
		//if we're not the edit page we can return the list, which should only have one record on it.
		//for the list page we want to return a Map of lists, breaking out the users by Division (for display)
		if (req.hasParameter(USER_ID) || req.hasParameter("view")) { //'view' is for JSON list loaders
			putModuleData(users);
		} else {
			GenericVO vo = sortRecords(users);
			summateLoginActivity(vo, req);
			putModuleData(vo);
		}
	}


	/**
	 * Take the active user accounts (from sortRecords) and bucketize the users by login activity, according to time-table (legend)
	 * @param req
	 */
	@SuppressWarnings("unchecked")
	private void summateLoginActivity(GenericVO data, ActionRequest req) {
		Map<Integer, Integer> counts = new HashMap<>();
		counts.put(Integer.valueOf(0), Integer.valueOf(0));
		counts.put(Integer.valueOf(30), Integer.valueOf(0));
		counts.put(Integer.valueOf(60), Integer.valueOf(0));
		counts.put(Integer.valueOf(90), Integer.valueOf(0));
		
		Map<String, List<UserVO>> active = (Map<String, List<UserVO>>) data.getKey();
		for (Map.Entry<String, List<UserVO>> entry : active.entrySet()) {
			for (UserVO user : entry.getValue()) {
				Integer age = user.getLoginAge();
				counts.put(age, 1+counts.get(age));
			}
		}
		//help with debugging
		if (log.isDebugEnabled()) {
			for (Map.Entry<Integer, Integer> entry : counts.entrySet())
				log.debug(entry.getValue() + " users in " + entry.getKey());
		}
		req.setAttribute("activtyMap", counts);
	}


	/**
	 * sorts the List<UserVO> into buckets by Divsion - only used for display in the Manage tool.
	 * @param users
	 * @return
	 */
	private GenericVO sortRecords(List<Object> users) {
		final String noDivision = "No Division Specified";
		Map<Integer, String> divisions = loadDivisionList();
		Map<String, List<UserVO>> active = new LinkedHashMap<>();
		Map<String, List<UserVO>> inactive = new LinkedHashMap<>();
		
		//seed the data map using the sequence defined in the database
		for (Map.Entry<Integer, String> entry : divisions.entrySet()) {
			active.put(entry.getValue(), new ArrayList<>());
			inactive.put(entry.getValue(), new ArrayList<>());
		}
		
		//add a catch-all for users w/o a Division
		active.put(noDivision, new ArrayList<>());
		inactive.put(noDivision, new ArrayList<>());
		
		//put each user into their respective display bucket
		for (Object obj : users) {
			UserVO user = (UserVO) obj;
			String divNm = divisions.get(Convert.formatInteger(user.getPrimaryDivision()));
			//log.debug("div=" + divNm + " id=" + user.getPrimaryDivision())
			if (StringUtil.isEmpty(divNm)) divNm = noDivision;
			if ("I".equals(user.getStatusCode())) {
				inactive.get(divNm).add(user);
			} else {
				active.get(divNm).add(user);
			}
		}
		
		//remove any emtpy entries from the map - Java 8
		active.entrySet().removeIf(e-> e.getValue().isEmpty());
		inactive.entrySet().removeIf(e-> e.getValue().isEmpty());
		
		//help with debugging
		if (log.isDebugEnabled()) {
			for (Map.Entry<String, List<UserVO>> entry : active.entrySet())
				log.debug(entry.getValue().size() + " users in " + entry.getKey());

			for (Map.Entry<String, List<UserVO>> entry : inactive.entrySet())
				log.debug(entry.getValue().size() + " inactive users in " + entry.getKey());
		}
		

		return new GenericVO(active, inactive);
	}
	

	/**
	 * loads the list of Divisions from the DB.  Easier and cleaner to isolate that try to include on the main query.  Also
	 * allows us to control sort order, and rolls with any changes that get made to the data over time.
	 * @return
	 */
	private Map<Integer, String> loadDivisionList() {
		Map<Integer, String> data = new LinkedHashMap<>();
		String sql = "select option_desc, option_value_txt from register_field_option where register_field_id=? order by order_no, option_desc";
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, RegistrationMap.DIVISIONS.getFieldId());
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				data.put(rs.getInt(2), rs.getString(1));
			
		} catch (SQLException sqle) {
			log.error("could not load Division list", sqle);
		}
		
		return data;
	}
	

	/**
	 * Flushes the users session, then logs them in as the passed profileId, then redirects to the homepage
	 * @param req
	 */
	protected void loginAsUser(ActionRequest req) {
		try {
			WCUtil.logout(req.getSession());
			SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
			String encKey = (String)getAttribute(Constants.ENCRYPT_KEY);
			StringEncrypter se = new StringEncrypter(encKey);
			String encProfileId = StringEncoder.urlEncode(se.encrypt(req.getParameter("loginAs")));

			SecurityController sc = new SecurityController(site.getLoginModule(), site.getRoleModule(), getAttributes());
			sc.loadUserFromCookie(encProfileId, encKey, dbConn, req, site);

			sendRedirect("/","", req);
		} catch (Exception e) {
			log.error("could not prepare for automatic user login", e);
		}
	}


	/**
	 * loads the list of users tied to this account.
	 * This data populates the bootstrap table on the list page
	 * Can pass an optional profileId in order to retrieve via profileId rather
	 * than USER_ID.
	 * @param req
	 * @param schema
	 * @param profileId
	 * @return
	 * @throws ActionException
	 */
	public List<Object> loadAccountUsers(ActionRequest req, String profileId) throws ActionException {
		AccountAction.loadAccount(req, dbConn, getAttributes());
		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
		String accountId = req.hasParameter(ACCOUNT_ID) ? req.getParameter(ACCOUNT_ID) : null;
		String userId = req.hasParameter(USER_ID) ? req.getParameter(USER_ID) : null;
		boolean loadProfileData = false;

		//Build Sql
		String sql = formatRetrieveQuery(schema, userId, profileId, req.hasParameter("sendNow")); //for sendNow (Manage Updates) we only want active user accounts

		//Build Params
		List<Object> params = new ArrayList<>();
		params.add(AdminControllerAction.PUBLIC_SITE_ID);
		params.add(AdminControllerAction.PUBLIC_SITE_ID);
		params.add(UserVO.RegistrationMap.TITLE.getFieldId());
		params.add(UserVO.RegistrationMap.NOTES.getFieldId());
		params.add(UserVO.RegistrationMap.DIVISIONS.getFieldId());
		if (StringUtil.isEmpty(profileId)) {
			params.add(accountId);
			if (userId != null) {
				params.add(userId);
				loadProfileData = true;
			}
		} else {
			params.add(profileId);
			loadProfileData = true;
		}

		//Call Execute the Query with given params.
		List<Object> users = executeUserQuery(sql, params);

		//get more information about this one user, so we can display the edit screen.
		//If this is an ADD, we don't need the additional lookups
		if (loadProfileData)
			loadRegistration(req, users);

		return users;
	}

	/**
	 * Helper method that manages talking to DBProcessor and decrypting UserData.
	 * @param req
	 * @param sql
	 * @param params
	 * @param loadProfileData
	 * @return
	 * @throws ActionException
	 */
	protected List<Object> executeUserQuery(String sql, List<Object> params) {
		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);

		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object>  data = db.executeSelect(sql, params, new UserVO());
		log.debug("loaded " + data.size() + " users");

		//decrypt the owner profiles
		decryptNames(data);

		return data;
	}

	/**
	 * loads everything we need to know about a single user so we can edit their registration, profile, or user database records.
	 * Also loads ACLs so we can manage their permissions.
	 * @param userId
	 * @param schema
	 * @param userObj
	 * @throws ActionException 
	 */
	protected void loadRegistration(ActionRequest req, List<Object> users) throws ActionException {
		//load the registration form
		ActionInitVO actionInit = new ActionInitVO();
		actionInit.setActionGroupId(AdminControllerAction.REGISTRATION_GRP_ID);
		RegistrationAction sa = new RegistrationAction(actionInit);
		sa.setDBConnection(dbConn);
		sa.setAttributes(getAttributes());
		sa.retrieve(req);
		req.setAttribute("registrationForm", ((ModuleVO)sa.getAttribute(Constants.MODULE_DATA)).getActionData());

		//fail-fast if there's no user to load responses for, or too many users
		if (users == null || users.isEmpty() || users.size() != 1)
			return;

		//load the user's registration responses - these will go into the UserDataVO->attributes map
		UserVO user = (UserVO) users.get(0);
		ResponseLoader rl = new ResponseLoader();
		rl.setDbConn(dbConn);
		rl.loadRegistrationResponses(user, AdminControllerAction.PUBLIC_SITE_ID);

		//load the user's profile data
		callProfileManager(user, req, false);

		users.set(0, user); //make sure the record gets back on the list, though it probably does by reference anyways
	}


	/**
	 * Calls ProfileManager for both read and write transaction for core user data
	 * @param user
	 * @param req
	 * @throws ActionException 
	 */
	protected void callProfileManager(UserVO user, ActionRequest req, boolean isSave) {
		//load the users profile data
		ProfileManager pm = ProfileManagerFactory.getInstance(getAttributes());
		try {
			if (isSave) {
				pm.updateProfile(user, dbConn);
			} else {
				SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
				UserDataVO vo = pm.getProfile(user.getProfileId(), dbConn, ProfileManager.PROFILE_ID_LOOKUP, site.getOrganizationId());
				user.setData(vo.getDataMap());
			}
		} catch (com.siliconmtn.exception.DatabaseException de) { //this is a different DatabaseException than DBProcessor
			log.error("could not load user profile", de);
		}
	}

	/**
	 * loop and decrypt owner names, which came from the profile table
	 * @param accounts
	 */
	@SuppressWarnings("unchecked")
	protected void decryptNames(List<Object> data) {
		LastNameComparator c = new LastNameComparator();
		c.decryptNames((List<? extends HumanNameIntfc>)(List<?>)data, (String)getAttribute(Constants.ENCRYPT_KEY));
		Collections.sort(data, c);
	}


	/**
	 * gives the user a dummy randomly-generated password and creates the authentication record.
	 * puts authenticationId onto the userVo for storage in the profile table.
	 * @param user
	 */
	protected void saveAuthRecord(UserVO user) throws ActionException {
		//create a random password if this is a new account and a password was not provided - this is for security reasons
		if (StringUtil.isEmpty(user.getPassword()) && StringUtil.isEmpty(user.getAuthenticationId()))
			user.setPassword(RandomAlphaNumeric.generateRandom(8));

		UserLogin ul = new UserLogin(dbConn, (String)getAttribute(Constants.ENCRYPT_KEY));
		//save the record.  Flag it for password reset immediately.
		try {
			String authId = ul.checkAuth(user.getEmailAddress());
			//if the user had an auth record already then don't change their password or flag them for reset
			String pswd = !StringUtil.isEmpty(user.getPassword()) ? user.getPassword() : UserLogin.DUMMY_PSWD;
			authId = ul.modifyUser(authId, user.getEmailAddress(), pswd, StringUtil.isEmpty(authId) ? 1 : 0);
			user.setAuthenticationId(authId);
		} catch (com.siliconmtn.exception.DatabaseException e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Formats the account retrieval query.
	 * @return
	 * TODO getting duplicate rows from AL table when device/browser change.
	 */
	protected String formatRetrieveQuery(String schema, String userId, String profileId, boolean activeOnly) {
		StringBuilder sql = new StringBuilder(300);
		sql.append("select u.account_id, u.profile_id, u.user_id, u.register_submittal_id, u.status_cd, u.acct_owner_flg, ");
		sql.append("u.expiration_dt, p.first_nm, p.last_nm, p.email_address_txt, max(al.login_dt) as login_dt, ");
		sql.append("al.oper_sys_txt, al.browser_txt, u.fd_auth_flg, u.ga_auth_flg, u.mkt_auth_flg, ");
		sql.append("title.value_txt as title_txt, notes.value_txt as notes_txt, division.value_txt as division_txt ");
		sql.append("from ").append(schema).append("biomedgps_user u ");
		sql.append("left join profile p on u.profile_id=p.profile_id ");
		sql.append("left join authentication_log al on p.authentication_id=al.authentication_id and al.site_id=? and al.status_cd=1 ");
		sql.append("left join register_submittal rs on p.profile_id=rs.profile_id and rs.site_id=? ");
		sql.append("left join register_data title on rs.register_submittal_id=title.register_submittal_id and title.register_field_id=? ");
		sql.append("left join register_data notes on rs.register_submittal_id=notes.register_submittal_id and notes.register_field_id=? ");
		sql.append("left join register_data division on rs.register_submittal_id=division.register_submittal_id and division.register_field_id=? ");
		if (StringUtil.isEmpty(profileId)) {
			sql.append("where u.account_id=? ");
			if (userId != null) sql.append("and u.user_id=? ");
		} else {
			sql.append("and p.profile_id=? ");
		}
		//only load active user accounts - used by Manage Updates 'send now' tool so we're not emailing users who can't login to ST.
		if (activeOnly)
			sql.append("and u.status_cd != 'I' and (u.expiration_dt is null or u.expiration_dt > CURRENT_DATE) ");
		
		sql.append("group by u.account_id, u.profile_id, u.user_id, u.register_submittal_id, u.status_cd, ");
		sql.append("u.expiration_dt, p.first_nm, p.last_nm, p.email_address_txt, al.oper_sys_txt, al.browser_txt, ");
		sql.append("title_txt, notes_txt, division_txt ");

		log.debug(sql);
		return sql.toString();
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		//ajax hook for quick-saving notes:
		if (req.hasParameter("saveNote")) {
			saveNote(req);
			return;
		}
		
		UserVO user = new UserVO(req);

		//save auth
		saveAuthRecord(user);

		//save their WC profile
		callProfileManager(user, req, true);

		//check & create profile_role if needed
		saveProfileRole(user, false);

		//save their registration data
		saveRegistrationData(req, user);

		//save their UserVO (smarttrak user table)
		saveRecord(user, false);

		setupRedirect(req);
	}


	/**
	 * Handles the onBlur ajax call to quick-save the user's notes textarea
	 * @param req
	 */
	private void saveNote(ActionRequest req) {
		req.setParameter("formFields", new String[]{ RegistrationMap.NOTES.getFieldId() } , Boolean.TRUE);
		
		//build a list of values to insert based on the ones we're going to delete
		List<SubmittalDataVO> regData = new ArrayList<>();
		SubmittalDataVO vo = new SubmittalDataVO(null); //encryption key=null, we don't need it.
		vo.setRegisterFieldId(RegistrationMap.NOTES.getFieldId());
		vo.setUserValue(req.getParameter("noteText"));
		regData.add(vo);
		
		SubmittalAction sa = new SubmittalAction();
		sa.setAttributes(getAttributes());
		sa.setDBConnection(getDBConnection());
		sa.updateRegisterData(req, null, req.getParameter("rsid"), regData, false);
		
	}

	/**
	 * checks and creates the profile_role record tied to the public site, if necessary
	 * @param user
	 * @throws ActionException
	 */
	protected void saveProfileRole(UserVO user, boolean isDelete) throws ActionException {
		ProfileRoleManager prm = new ProfileRoleManager();
		SBUserRole role = new SBUserRole(AdminControllerAction.PUBLIC_SITE_ID);
		role.setStatusId(SecurityController.STATUS_ACTIVE);
		role.setProfileId(user.getProfileId());

		try {
			//find any existing role - will either be deleted or updated
			role.setProfileRoleId(prm.checkRole(user.getProfileId(), role.getSiteId(), null, null, dbConn));

			if (isDelete) {
				prm.removeRole(role.getProfileRoleId(), dbConn);
				return;
			}
			//determine the role id to set
			setRoleId(role, user);
			
			prm.addRole(role, dbConn);

		} catch (com.siliconmtn.exception.DatabaseException e) {
			throw new ActionException(e);
		}
	}

	/**
	 * Helper method that determines the role id to set for the UserRole
	 * @param role
	 * @param user
	 */
	protected void setRoleId(SBUserRole role, UserVO user){
		//determine the role id
		if (UserVO.Status.EUREPORTS.getCode().equals(user.getStatusCode())) {
			role.setRoleId(AdminControllerAction.EUREPORT_ROLE_ID);
		} else if(UserVO.Status.COMPUPDATES.getCode().equals(user.getStatusCode())
				|| UserVO.Status.UPDATES.getCode().equals(user.getStatusCode())){
					role.setRoleId(AdminControllerAction.UPDATES_ROLE_ID);
		} else if (UserVO.Status.STAFF.getCode().equals(user.getStatusCode())) {
			role.setRoleId(AdminControllerAction.STAFF_ROLE_ID);
		} else {
			role.setRoleId(Integer.toString(SecurityController.PUBLIC_REGISTERED_LEVEL));
		}
	}

	/**
	 * Transposes the registration fields off the request into a list of Fields, and passes them to the 
	 * Registration action to be saved.
	 * @param req
	 * @param user
	 * @throws ActionException
	 */
	protected void saveRegistrationData(ActionRequest req, UserVO user) throws ActionException {
		SubmittalAction sa = new SubmittalAction();
		sa.setAttributes(getAttributes());
		sa.setDBConnection(dbConn);
		Set<String> formFields = new HashSet<>(30);

		//possibly create a new registration record.  This would be for the 'add user' scenario
		if (StringUtil.isEmpty(user.getRegisterSubmittalId())) {
			req.setParameter(SBActionAdapter.SB_ACTION_ID, AdminControllerAction.REGISTRATION_GRP_ID);
			user.setRegisterSubmittalId(sa.insertRegisterSubmittal(req, user.getProfileId(), AdminControllerAction.PUBLIC_SITE_ID));
		}

		//build a list of values to insert based on the ones we're going to delete
		List<SubmittalDataVO> regData = new ArrayList<>();
		SubmittalDataVO vo;
		for (RegistrationMap field : UserVO.RegistrationMap.values()) {
			formFields.add(field.getFieldId());
			String[] values = req.getParameterValues(field.getReqParam());
			if (values == null) continue; //we're still going to flush the old data, but have nothing to save in it's place

			for (String val : values) {
				vo = new SubmittalDataVO(null);
				vo.setRegisterFieldId(field.getFieldId());
				vo.setUserValue(val);
				regData.add(vo);
			}
		}

		//put the fields we're going to be saving onto the request - Registration won't save what we can't prove we're passing
		req.setParameter("formFields", formFields.toArray(new String[formFields.size()]) , Boolean.TRUE);
		sa.updateRegisterData(req, user, user.getRegisterSubmittalId(), regData, false); //false = do not reload responses onto session
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		UserVO user = new UserVO(req);
		saveRecord(user, true); //deletes them from Smartrak, but not from the WC core
		saveProfileRole(user, true); //revoke website access
		setupRedirect(req);
	}


	/**
	 * builds the redirect URL that takes us back to the list of teams page.
	 * @param req
	 */
	protected void setupRedirect(ActionRequest req) {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder url = new StringBuilder(200);
		url.append(page.getFullPath());
		url.append("?actionType=").append(req.getParameter("actionType"));
		url.append("&accountId=").append(req.getParameter("accountId"));
		url.append("&accountName=").append(AdminControllerAction.urlEncode(req.getParameter("accountName")));
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}


	/**
	 * reusable internal method for invoking DBProcessor
	 * @param req
	 * @param isDelete
	 * @throws ActionException
	 */
	protected void saveRecord(UserVO user, boolean isDelete) throws ActionException {
		DBProcessor db = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		try {
			if (isDelete) {
				db.delete(user);
			} else {
				db.save(user);
			}
		} catch (InvalidDataException | DatabaseException e) {
			throw new ActionException(e);
		}
	}
}