package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
//Java 8
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.action.SmarttrakRegistrationAction;
import com.biomed.smarttrak.action.UpdatesEditionAction;
import com.biomed.smarttrak.admin.report.AccountUsersVO;
import com.biomed.smarttrak.util.SmarttrakTree;
//WC_Custom
import com.biomed.smarttrak.vo.UserVO;
import com.biomed.smarttrak.vo.UserVO.RegistrationMap;
import com.biomed.smarttrak.vo.UserVO.Status;
import com.siliconmtn.action.ActionControllerFactoryImpl;
// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.ApplicationException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.sb.email.util.EmailCampaignBuilderUtil;
import com.siliconmtn.sb.email.vo.EmailRecipientVO;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
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
import com.smt.sitebuilder.security.PasswordComplexityFactory;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.security.UserLogin;
import com.smt.sitebuilder.security.WCUtil;

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

	protected static final String CFG_WELCOME_EML = "smarttrakWelcomeEmailInstanceId";
	protected static final String CFG_PSWD_RESET_EML = "smarttrakPasswordResetEmailInstanceId";
	protected static final String ACCOUNT_ID = AccountAction.ACCOUNT_ID; //req param
	protected static final String USER_ID = "userId"; //req param
	protected static final String PROFILE_ID = UpdatesEditionAction.PROFILE_ID;
	private static final String WELCOME_STANDARD_SLUG = "BIOMED_WELCOME";
	private static final String WELCOME_UPDATES_SLUG = "BIOMED_UPDATES_WELCOME";
	

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
		} else if (req.hasParameter("checkUserByEmail")) {
			checkForExistingUser(req);
			return;
		} else if (req.hasParameter("sendWelcomeEmail")) {
			sendWelcomeEmail(req);
			return;
		} else if (req.hasParameter("sendPasswordEmail")) {
			sendPasswordEmail(req);
			return;
		}

		List<UserVO> users = loadAccountUsers(req, req.getParameter(PROFILE_ID));
		//if we're not the edit page we can return the list, which should only have one record on it.
		//for the list page we want to return a Map of lists, breaking out the users by Division (for display)
		if (req.hasParameter(USER_ID) || req.hasParameter("view")) { //'view' is for JSON list loaders
			putModuleData(users);

			//load User Market Preferences
			SmarttrakRegistrationAction sra = ActionControllerFactoryImpl.loadAction(SmarttrakRegistrationAction.class, this);
			sra.loadMarketsInformation(req);
		} else {
			GenericVO vo = sortRecords(users);
			summateLoginActivity(vo, req);
			summateStatus(vo, req);
			prepAddModal(vo, req);
			putModuleData(vo);
		}
	}


	/**
	 * Triggers the email campaign email for Welcome Msg. to be sent to the user
	 * A password reset URL is conditionally supported (when needed)
	 * @param req
	 */
	private void sendWelcomeEmail(ActionRequest req) {
		//build the emailConfig
		UserVO u = new UserVO(req);
		Map<String, Object> config = new HashMap<>();
		config.put("firstName", u.getFirstName());
		config.put("lastName", u.getLastName());
		config.put("emailAddress", u.getEmailAddress());
		config.put("createDt", Convert.getCurrentTimestamp());
		config.put(PROFILE_ID, u.getProfileId());
		config.put(USER_ID, u.getUserId());
		config.put(ACCOUNT_ID, u.getAccountId());
		if (Convert.formatBoolean(req.getParameter("passwordReset")))
			config.put("passwordResetKey", makeResetKey(req, u.getEmailAddress()));

		String campInstId = WELCOME_STANDARD_SLUG;
		if ("T".equals(u.getLicenseType()) || "U".equals(u.getLicenseType()))
			campInstId = WELCOME_UPDATES_SLUG;
		
		List<EmailRecipientVO> recipients = new ArrayList<>(2);
		recipients.add(new EmailRecipientVO(u.getProfileId(), (String)config.get("emailAddress"), EmailRecipientVO.TO));
		String sourceId = req.getParameter("sourceId");
		String sourceEmail = addSourceEmail(sourceId);
		if (sourceEmail.length() > 0)
			recipients.add(new EmailRecipientVO(sourceId, sourceEmail, EmailRecipientVO.BCC));

		//perform the email send
		EmailCampaignBuilderUtil ecbu = new EmailCampaignBuilderUtil(dbConn, attributes);
		ecbu.sendMessage(config, recipients, campInstId);
	}


	/**
	 * Triggers the email campaign email for Password Reset to be sent to the user.
	 * @param req
	 */
	private void sendPasswordEmail(ActionRequest req) {
		//build the emailConfig
		UserVO u = new UserVO(req);
		Map<String, Object> config = new HashMap<>();
		config.put("firstName", u.getFirstName());
		config.put("lastName", u.getLastName());
		config.put("emailAddress", u.getEmailAddress());
		config.put("createDt", Convert.getCurrentTimestamp());
		config.put(PROFILE_ID, u.getProfileId());
		config.put(USER_ID, u.getUserId());
		config.put(ACCOUNT_ID, u.getAccountId());
		config.put("passwordResetKey", makeResetKey(req, u.getEmailAddress()));

		String campInstId = StringUtil.checkVal((String) getAttribute(CFG_PSWD_RESET_EML));
		List<EmailRecipientVO> recipients = new ArrayList<>(2);
		recipients.add(new EmailRecipientVO(u.getProfileId(), (String)config.get("emailAddress"), EmailRecipientVO.TO));
		String sourceId = req.getParameter("sourceId");
		String sourceEmail = addSourceEmail(sourceId);
		if (sourceEmail.length() > 0)
			recipients.add(new EmailRecipientVO(sourceId, sourceEmail, EmailRecipientVO.BCC));

		//perform the email send
		EmailCampaignBuilderUtil ecbu = new EmailCampaignBuilderUtil(dbConn, attributes);
		ecbu.sendMessage(config, recipients, campInstId);
	}
	
	
	/**
	 * Get the email address associated with the supplied source.
	 * @param sourceId
	 * @return
	 */
	private String addSourceEmail(String sourceId) {
		String sql = "select email_address_txt from profile where profile_id = ? ";
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, sourceId);
			
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				StringEncrypter se = new StringEncrypter((String) attributes.get(Constants.ENCRYPT_KEY));
				String email = StringUtil.checkVal(rs.getString("email_address_txt"));
				return se.decrypt(email);
			}
			
		} catch (Exception e) {
			log.warn("Failed to get source email with profile id " + sourceId);
		}
		return "";
	}


	/**
	 * generates a password reset URL for the user.
	 * @param req
	 * @return
	 */
	private String makeResetKey(ActionRequest req, String email) {
		long oneWeekMillis = 604800000;
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		try {
			return StringEncoder.urlEncode(PasswordComplexityFactory
					.getInstance(site.getPasswordModule(), getAttributes())
					.generateResetToken(site.getSiteAlias(), email, oneWeekMillis));
		} catch (ApplicationException e) {
			log.error("unable to generate password reset token", e);
			return "";
		}
	}

	/**
	 * uses email address to see if a user is already in ST, or already in WC, before being added.
	 * @param req
	 */
	private void checkForExistingUser(ActionRequest req) {
		String encKey = (String)getAttribute(Constants.ENCRYPT_KEY);
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);

		String email = req.getParameter("checkUserByEmail").toUpperCase();
		StringBuilder sql = new StringBuilder(150);
		sql.append("select a.user_id, b.profile_id, a.account_id from ").append(schema).append("biomedgps_user a ");
		sql.append("right outer join profile b on a.profile_id=b.profile_id ");
		sql.append("where b.search_email_txt=?");
		log.debug(sql + " " + email);

		Map<String, String> data = new HashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			StringEncrypter se = new StringEncrypter(encKey);
			ps.setString(1, se.encrypt(email));
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				data.put(USER_ID, rs.getString(1));
				data.put(PROFILE_ID, rs.getString(2));
				data.put(ACCOUNT_ID, rs.getString(3));
			}
		} catch (Exception e) {
			log.error("could not search existing users by email", e);
		}

		putModuleData(data);
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
				//exclude open seats
				if (user.getStatusFlg() != Status.OPEN.getCode()) {
					Integer age = user.getLoginAge();
					counts.put(age, 1+counts.get(age));
				}
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
	 * Take the active user accounts (from sortRecords) and bucketize the users by login activity, according to time-table (legend)
	 * @param req
	 */
	@SuppressWarnings("unchecked")
	private void summateStatus(GenericVO data, ActionRequest req) {

		Map<String, List<UserVO>> users = (Map<String, List<UserVO>>) data.getKey();

		Map<String, Integer> licenses = new LinkedHashMap<>();
		for (Map.Entry<String, List<UserVO>> entry : users.entrySet()) {
			for (UserVO user : entry.getValue()) {
				Integer sts = user.getStatusFlg();
				if (sts == UserVO.Status.ACTIVE.getCode()) {
					incrementActive(licenses, user.getLicenseType());
				}else if (sts == UserVO.Status.OPEN.getCode()) {
					incrementStatus(licenses, "O");
				}
			}
		}
		req.setAttribute("statusMap", licenses);
	}
	
	
	/**
	 * Builds a Map used to offer options in the Add User modal - for resuing open seats by Division
	 * @param vo
	 * @param req
	 */
	@SuppressWarnings("unchecked")
	private void prepAddModal(GenericVO vo, ActionRequest req) {
		Map<String, String> data = new HashMap<>();

		Map<String, List<UserVO>> users = (Map<String, List<UserVO>>) vo.getKey();
		for (Map.Entry<String, List<UserVO>> entry : users.entrySet()) {
			for (UserVO user : entry.getValue()) {
				//capture the first Open Seat in each of the Divisions
				if (Status.OPEN.getCode() == user.getStatusFlg() && data.get(entry.getKey()) == null)
					data.put(entry.getKey(), user.getUserId());
			}
		}
		req.setAttribute("addModalSpecs", data);
	}


	/**
	 * sorts the List<UserVO> into buckets by Divsion - only used for display in the Manage tool.
	 * @param users
	 * @return
	 */
	private GenericVO sortRecords(List<UserVO> users) {
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
		for (UserVO user : users) {
			String divNm = divisions.get(Convert.formatInteger(user.getPrimaryDivision()));
			//log.debug("div=" + divNm + " id=" + user.getPrimaryDivision())
			if (StringUtil.isEmpty(divNm)) divNm = noDivision;
			if (Status.INACTIVE.getCode() == user.getStatusFlg()) {
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

			SecurityController sc = new SecurityController(site.getLoginModules(), site.getRoleModule(), getAttributes());
			sc.processCookieLogin(encProfileId, dbConn, req, site);

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
	public List<UserVO> loadAccountUsers(ActionRequest req, String profileId) throws ActionException {
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
		List<UserVO> users = executeUserQuery(sql, params);

		//get more information about this one user, so we can display the edit screen.
		//If this is an ADD, we don't need the additional lookups
		if (loadProfileData) {
			loadRegistration(req, users);
			loadMarkets(users);
		}

		return users;
	}

	/**
	 * Load all markets that the user is not supposed to see in the updates emails
	 * @param users
	 * @throws ActionException
	 */
	private void loadMarkets(List<UserVO> users) throws ActionException {
		if (users == null || users.isEmpty() || users.size() != 1)
			return;
		
		UserVO user = users.get(0);
		StringBuilder sql = new StringBuilder(100);
		sql.append("select section_id from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_user_updates_skip where user_id = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, user.getUserId());
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) 
				user.addSkippedMarket(rs.getString("section_id"));
		} catch (SQLException e) {
			throw new ActionException(e);
		}
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
	protected List<UserVO> executeUserQuery(String sql, List<Object> params) {
		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
		log.debug(params);
		DBProcessor db = new DBProcessor(dbConn, schema);
		List<UserVO>  data = db.executeSelect(sql, params, new UserVO());
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
	protected void loadRegistration(ActionRequest req, List<UserVO> users) throws ActionException {
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
		UserVO user = users.get(0);
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
		ProfileManager pm = ProfileManagerFactory.getInstance(getAttributes());
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		try {
			if (isSave) {
				pm.updateProfile(user, dbConn);
				pm.assignCommunicationFlg(site.getOrganizationId(), user.getProfileId(), user.getAllowCommunication(), dbConn);
			} else {
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
	protected void decryptNames(List<? extends HumanNameIntfc> data) {
		LastNameComparator c = new LastNameComparator();
		c.decryptNames(data, (String)getAttribute(Constants.ENCRYPT_KEY));
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

		UserLogin ul = new UserLogin(dbConn, getAttributes());
		//save the record.  Flag it for password reset immediately.
		try {
			String authId = ul.checkAuth(user.getEmailAddress());
			//if the user had an auth record already then don't change their password or flag them for reset
			String pswd = !StringUtil.isEmpty(user.getPassword()) ? user.getPassword() : UserLogin.DUMMY_PSWD;
			authId = ul.saveAuthRecord(authId, user.getEmailAddress(), pswd, StringUtil.isEmpty(authId) ? 1 : 0);
			user.setAuthenticationId(authId);
		} catch (com.siliconmtn.exception.DatabaseException e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Formats the account retrieval query.
	 * @return
	 */
	protected String formatRetrieveQuery(String schema, String userId, String profileId, boolean activeOnly) {
		StringBuilder sql = new StringBuilder(300);
		sql.append("select u.account_id, u.profile_id, u.user_id, u.register_submittal_id, u.status_cd, u.acct_owner_flg, ");
		sql.append("u.expiration_dt, p.first_nm, p.last_nm, p.email_address_txt, max(al.login_dt) as login_dt, ");
		sql.append("u.fd_auth_flg, u.ga_auth_flg, u.mkt_auth_flg, u.active_flg, u.create_dt, ");
		sql.append("title.value_txt as title_txt, notes.value_txt as notes_txt, division.value_txt as division_txt ");
		sql.append("from profile p ");
		sql.append("left join ").append(schema).append("biomedgps_user u on u.profile_id=p.profile_id ");
		sql.append("left join authentication_log al on p.authentication_id=al.authentication_id and al.site_id=? and al.status_cd=1 ");
		sql.append("left join register_submittal rs on p.profile_id=rs.profile_id and rs.site_id=? ");
		sql.append("left join register_data title on rs.register_submittal_id=title.register_submittal_id and title.register_field_id=? ");
		sql.append("left join register_data notes on rs.register_submittal_id=notes.register_submittal_id and notes.register_field_id=? ");
		sql.append("left join register_data division on rs.register_submittal_id=division.register_submittal_id and division.register_field_id=? ");
		sql.append("where 1=1 ");
		if (StringUtil.isEmpty(profileId)) {
			sql.append("and u.account_id=? ");
			if (userId != null) sql.append("and u.user_id=? ");
		} else {
			sql.append("and p.profile_id=? ");
		}
		//only load active user accounts - used by Manage Updates 'send now' tool so we're not emailing users who can't login to ST.
		if (activeOnly)
			sql.append("and u.active_flg=1 and (u.expiration_dt is null or u.expiration_dt > CURRENT_DATE) ");

		sql.append("group by u.account_id, u.profile_id, u.user_id, u.register_submittal_id, u.status_cd, ");
		sql.append("u.expiration_dt, u.active_flg, u.create_dt, p.first_nm, p.last_nm, p.email_address_txt, ");
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
		} else if (req.hasParameter("saveStatus")) {
			saveStatus(req);
			return;
		}
		
		//insert fake first/last names if we're CREATING an open seat - these values are not disclosed in the UI but we need the profile to exist
		if (!req.hasParameter("profileId") && UserVO.Status.OPEN.getCode() == Convert.formatInteger(req.getParameter("statusFlg")).intValue()) {
			req.setParameter("firstName", "SMARTTRAK_" + RandomAlphaNumeric.generateRandom(10));
			req.setParameter("lastName", "OPENSEAT_PLACEHOLDER");
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
		
		// If this is a new user add them to the default account.
		if (StringUtil.isEmpty(req.getParameter(USER_ID)))
			addToDefaultTeam(user);
		
		addSkippedMarkets(user);

		setupRedirect(req);
	}


	/**
	 * Add all markets that should be skipped in the updates emails.
	 * @param user
	 * @throws ActionException
	 */
	public void addSkippedMarkets(UserVO user) throws ActionException {
		deleteOldSkips(user.getUserId());
		
		StringBuilder sql = new StringBuilder(120);
		sql.append("insert into ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_user_updates_skip (user_updates_skip_id, user_id, section_id, create_dt)");
		sql.append("values(?,?,?,?)");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			UUIDGenerator gen = new UUIDGenerator();
			for (String skip : user.getSkippedMarkets()) {
				ps.setString(1, gen.getUUID());
				ps.setString(2, user.getUserId());
				ps.setString(3, skip);
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			
			ps.executeBatch();
			
		} catch(SQLException e) {
			throw new ActionException(e);
		}
	}

	/**
	 * Flush out the current list of skips to make room for the new ones.
	 * @param userId
	 * @throws ActionException
	 */
	private void deleteOldSkips(String userId) throws ActionException {
		StringBuilder sql = new StringBuilder(100);
		sql.append("delete from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_user_updates_skip where user_id = ? ");
		

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, userId);
			
			ps.executeUpdate();
		} catch(SQLException e) {
			throw new ActionException(e);
		}
	}

	/**
	 * Add the user to the default team of thier account
	 * @param user
	 * @throws ActionException
	 */
	private void addToDefaultTeam(UserVO user) throws ActionException {
		StringBuilder sql = new StringBuilder(200);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("insert into ").append(customDb).append("biomedgps_user_team_xr ");
		sql.append("(user_team_xr_id, team_id, user_id, create_dt) ");
		sql.append("select ?, team_id, ?, CURRENT_TIMESTAMP from ");
		sql.append(customDb).append("biomedgps_team where ");
		sql.append("account_id = ? and default_flg = 1 ");
		log.debug(sql+"|"+user.getUserId()+"|"+user.getAccountId());
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, user.getUserId());
			ps.setString(3, user.getAccountId());
			
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
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
	 * Handles the onClick ajax call to quick-save the user's status (change / toggle)
	 * @param req
	 */
	private void saveStatus(ActionRequest req) {
		StringBuilder sql = new StringBuilder(150);
		sql.append("update ").append((String) getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_user set active_flg=?, update_dt=CURRENT_TIMESTAMP where user_id=?");
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, Convert.formatInteger(req.getParameter("activeFlg")));
			ps.setString(2, req.getParameter(USER_ID));
			ps.executeUpdate();

		} catch (SQLException sqle) {
			log.error("could not update user status", sqle);
		}
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
		if (UserVO.LicenseType.EUREPORTS.getCode().equals(user.getLicenseType())) {
			role.setRoleId(AdminControllerAction.EUREPORT_ROLE_ID);
		} else if(UserVO.LicenseType.COMPUPDATES.getCode().equals(user.getLicenseType())
				|| UserVO.LicenseType.UPDATES.getCode().equals(user.getLicenseType())){
			role.setRoleId(AdminControllerAction.UPDATES_ROLE_ID);
		} else if (UserVO.LicenseType.STAFF.getCode().equals(user.getLicenseType())) {
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
		url.append("&accountId=").append(req.getParameter(ACCOUNT_ID));
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
	
	
	/**
	 * Load all accounts and thier seats
	 * @return
	 * @throws ActionException
	 */
	public Map<AccountUsersVO, Map<String, Integer>> loadAccountCounts(ActionRequest req) throws ActionException {
		String sql = getCountSQL();
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ResultSet rs = ps.executeQuery();
			return parseCountResults(rs, req);
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}
	
	
	/**
	 * Parse the results into a map of accounts and thier seat counts
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private Map<AccountUsersVO, Map<String, Integer>> parseCountResults(ResultSet rs, ActionRequest req) throws SQLException {
		Map<AccountUsersVO, Map<String, Integer>> results = new LinkedHashMap<>();
		AccountUsersVO account = new AccountUsersVO();
		account.setAccountId("");
		Map<String, Integer> counts = Collections.emptyMap();
		DBProcessor db = new DBProcessor(dbConn);
		
		AccountPermissionAction apa = new AccountPermissionAction(actionInit);
		apa.setDBConnection(dbConn);
		apa.setAttributes(attributes);
		
		while (rs.next()) {
			if (!account.getAccountId().equals(rs.getString("account_id"))) {
				addAccount(account, counts, results, apa, req);
				account = new AccountUsersVO();
				db.executePopulate(account, rs);
				counts = new HashMap<>();
			}
			
			incrementActive(counts, rs.getString("status_cd"), true, rs.getInt("active_flg"));
		}
		addAccount(account, counts, results, apa, req);
		
		return results;
	}
	
	
	/**
	 * Overloaded version of the increment status that can be used by summateStatus
	 * @param licenses
	 * @param licenseType
	 */
	protected void incrementActive(Map<String, Integer> licenses, String licenseType) {
		incrementActive(licenses, licenseType, false, 0);
	}
	
	
	/**
	 * Increment the count provided the user has the appropriate license type
	 * @param licenses
	 * @param licenseType
	 * @param i 
	 */
	private void incrementActive(Map<String, Integer> licenses, String licenseType, boolean isReport, int status) {
		if (StringUtil.isEmpty(licenseType)) return;
		String activeType = "";
		if (isReport)
			activeType = status == 1? "A":"O";
		switch (licenseType) {
		case "A":
		case "E":
		case "C":
		case "U":
		case "K":
			incrementStatus(licenses, activeType+licenseType);
			break;
		default:
			// Skip everything else
		}
	}

	/**
	 * Ensure that a status count is present and increment it.
	 * @param status
	 * @param section
	 * @param status2 
	 */
	private void incrementStatus(Map<String, Integer> licenses, String section) {
		if (!licenses.containsKey(section)) 
			licenses.put(section, 0);
		licenses.put(section, licenses.get(section)+1);
	}
	
	
	
	/**
	 * Add the account to the results map.
	 * @param account
	 * @param counts
	 * @param results
	 * @param apa 
	 * @param req 
	 */
	private void addAccount(AccountUsersVO account, Map<String, Integer> counts,
			Map<AccountUsersVO, Map<String, Integer>> results, AccountPermissionAction apa, ActionRequest req) {
		if ("".equals(account.getAccountId())) return;
		req.setParameter("accountId", account.getAccountId());
		try {
			apa.retrieve(req);
		} catch (Exception e) {
			log.error("Could not retrieve account permissions for account ID: " + account.getAccountId());
		}
		// try to get to it
		ModuleVO mod = (ModuleVO)getAttribute(Constants.MODULE_DATA);
		if (mod != null) {
			SmarttrakTree t = (SmarttrakTree)mod.getActionData();
			account.setPermissions(t);
		}
		
		results.put(account, counts);
	}

	/**
	 * Get the sql for listing active users and open seats for all accounts
	 * @return
	 */
	private String getCountSQL() {
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(250);
		sql.append("select a.account_id, a.account_nm, u.active_flg, u.status_cd, a.start_dt, a.expiration_dt, a.classification_id ");
		sql.append(DBUtil.FROM_CLAUSE).append(customDb).append("biomedgps_account a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(customDb).append("biomedgps_user u ");
		sql.append("on a.account_id = u.account_id and u.active_flg != 0 ");
		sql.append("where a.status_no = 'A' ");
		sql.append("order by a.account_nm asc ");
		log.debug(sql);
		return sql.toString();
	}
}