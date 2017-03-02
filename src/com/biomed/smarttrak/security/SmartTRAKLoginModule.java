package com.biomed.smarttrak.security;

// Java 8
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

//SMTBaseLibs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.DjangoPasswordHasher;
import com.siliconmtn.security.SHAEncrypt;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.user.LoginAction;
import com.smt.sitebuilder.common.constants.Constants;

//WebCrescendo libs
import com.smt.sitebuilder.common.constants.ErrorCodes;
import com.smt.sitebuilder.security.DBLoginModule;
import com.smt.sitebuilder.security.UserLogin;
import com.biomed.smarttrak.action.AdminControllerAction.Section;

//WC_Custom libs
import com.biomed.smarttrak.vo.TeamVO;
import com.biomed.smarttrak.vo.UserVO;

/*****************************************************************************
 <p><b>Title</b>: SmartTRAKLoginModule</p>
 <p><b>Description: </b>Custom login module for SmartTRAK user login.  Authenticates 
 user against WebCrescendo core, then retrieves SmartTRAK user data and teams 
 membership.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author David Bargerhuff
 @version 1.0
 @since Jan 03, 2017
 <b>Changes:</b>
 ***************************************************************************/
public class SmartTRAKLoginModule extends DBLoginModule {

	public SmartTRAKLoginModule() {
		super();
	}

	public SmartTRAKLoginModule(Map<String, Object> config) {
		super(config);
	}


	/**
	 * test Smartrak passwords against 2 encryption schemes, pbkdf2_sha256 with fallback to SHA1
	 */
	@Override
	public UserDataVO authenticateUser(String username, String password) throws AuthenticationException {
		if (StringUtil.isEmpty(username) || StringUtil.isEmpty(password))
			throw new AuthenticationException(ErrorCodes.ERR_INVALID_LOGIN);
		log.debug("Starting authenticateUser: " + username + "/" + password);

		// call UserLogin to load the authentication record
		Connection dbConn = (Connection)getAttribute(GlobalConfig.KEY_DB_CONN);
		String encKey = (String)getAttribute(Constants.ENCRYPT_KEY);
		UserLogin ul = new UserLogin(dbConn, encKey);
		UserDataVO authUser = ul.getAuthRecord(null, username);

		//getAuthRecord never returns null.  Test the VO for authenticationId, throw if not found
		if (StringUtil.isEmpty(authUser.getAuthenticationId()))
			throw new AuthenticationException(ErrorCodes.ERR_INVALID_LOGIN);

		testUserPassword(authUser, password); //this will throw if the password can't be verified

		// Return user authented user, after loading their Smarttrak data
		return loadSmarttrakUser(loadUserData(null, authUser.getAuthenticationId()));
	}


	/**
	 * Retrieves user data based on the encrypted profile ID passed in.
	 * Called when the user goes through automatic login using a saved cookie
	 * @param encProfileId
	 */
	@Override
	public UserDataVO authenticateUser(String encProfileId) throws AuthenticationException {
		UserDataVO user = super.authenticateUser(encProfileId);
		//Return user authented user, after loading their Smarttrak data
		return loadSmarttrakUser(user);
	}


	/**
	 * since we authenticated the user, take their UserDataVO and enhance it into a SmarttrakUserVO.
	 * Load their Smarttrak User account and their list of Teams.
	 * @param user
	 * @return
	 * @throws AuthenticationException
	 */
	public UserVO loadSmarttrakUser(UserDataVO userData) {
		UserVO stUser = new UserVO();
		stUser.setData(userData.getDataMap());
		stUser.setAttributes(userData.getAttributes());
		stUser.setAuthenticated(userData.isAuthenticated());
		loadCustomData(stUser);

		//if status is EU Reports, redirect them to the markets page
		if ("M".equals(stUser.getStatusCode())) {
			ActionRequest req = (ActionRequest) getAttribute(GlobalConfig.ACTION_REQUEST);
			req.getSession().setAttribute(LoginAction.DESTN_URL, Section.MARKET.getPageURL());
		}
		return stUser;
	}


	/**
	 * Retrieves base SmartTRAK-specific user data for a user based on 
	 * the user ID field or the profile ID field populated
	 * WC profile ID and populates the ST user bean with that data.
	 * @param conn
	 * @param user
	 * @return
	 * @throws AuthenticationException
	 */
	private void loadCustomData(UserVO user) {
		Connection dbConn = (Connection)getAttribute(GlobalConfig.KEY_DB_CONN);
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);

		// use profile ID as that is all we have at the moment.
		StringBuilder sql = new StringBuilder(200);
		sql.append("select u.user_id, u.account_id, u.register_submittal_id, u.fd_auth_flg, u.ga_auth_flg, u.mkt_auth_flg, ");
		sql.append("u.acct_owner_flg, coalesce(u.expiration_dt, a.expiration_dt) as expiration_dt, u.status_cd, ");
		sql.append("t.team_id, t.account_id, t.team_nm, t.default_flg, t.private_flg ");
		sql.append("from ").append(schema).append("biomedgps_user u ");
		sql.append("left outer join ").append(schema).append("biomedgps_user_team_xr xr on u.user_id=xr.user_id ");
		sql.append("inner join ").append(schema).append("biomedgps_team t on xr.team_id=t.team_id ");
		sql.append("inner join ").append(schema).append("biomedgps_account a on u.account_id=a.account_id ");
		sql.append("where u.account_id=t.account_id and u.profile_id=? order by t.team_nm");
		log.debug(sql + user.getProfileId());

		int iter = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, user.getProfileId());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (iter == 0) { //first run, get the fields from the _user table.  They're the same each time by.
					user.setUserId(rs.getString("user_id"));
					user.setAccountId(rs.getString("account_id"));
					user.setRegisterSubmittalId(rs.getString("register_submittal_id"));
					user.setFdAuthFlg(rs.getInt("fd_auth_flg"));
					user.setGaAuthFlg(rs.getInt("ga_auth_flg"));
					user.setMktAuthFlg(rs.getInt("mkt_auth_flg"));
					user.setAcctOwnerFlg(rs.getInt("acct_owner_flg"));
					user.setExpirationDate(rs.getDate("expiration_dt")); //used by the role module to block access to the site
					user.setStatusCode(rs.getString("status_cd"));
					iter = 1;
				}
				user.addTeam(new TeamVO(rs));
			}
		} catch (SQLException sqle) {
			log.error("could not query Smarttrak user/teams", sqle);
		}

		if (log.isDebugEnabled() && user.getTeams() != null)
			log.debug("loaded " + user.getTeams().size() + " teams for " + user.getEmailAddress());
	}


	/**
	 * tests the password obtained from the visitor against the auth record's password using
	 * a waterfall of encryption schemes.
	 * @param authUser
	 * @param proclaimedPassword
	 * @throws AuthenticationException
	 */
	protected void testUserPassword(UserDataVO authUser, String allegedPswd) throws AuthenticationException {
		//test the password against the Django scheme.  If it matches we're done
		DjangoPasswordHasher hasher = new DjangoPasswordHasher();
		if (hasher.checkPassword(allegedPswd, authUser.getPassword()))
			return;

		//test the password against SHA1 - the legacy Smarttrak scheme.  if it matches we're done
		SHAEncrypt sha = new SHAEncrypt();
		try {
			if (sha.encrypt(allegedPswd).equals(authUser.getPassword()))
				return;
		} catch (Exception e) {
			log.warn("password is not SHA1, or didn't match the stored value", e);
		}

		//finally, test the password against SMT's 3DES encryption scheme
		StringEncrypter se;
		try {
			se = new StringEncrypter((String)getAttribute(Constants.ENCRYPT_KEY));
			if (se.encrypt(allegedPswd).equals(authUser.getPassword()))
				return;
		} catch (Exception e) {
			log.warn("password is not 3DES, or didn't match the stored value", e);
		}

		throw new AuthenticationException(ErrorCodes.ERR_INVALID_LOGIN);
	}


	/**
	 * saves the auth record to the database.  Encrypts the user's password with the custom scheme first.
	 * Does not currently preserve password history, but could be added off of this method.  Database field would need to be lengthened.
	 */
	@Override
	public String saveAuthRecord(String authId, String userName, String password, Integer resetFlag) 
			throws InvalidDataException {
		if (StringUtil.isEmpty(userName) || StringUtil.isEmpty(password)) 
			throw new InvalidDataException();

		// Get the database Connection
		Connection dbConn = (Connection)getAttribute(GlobalConfig.KEY_DB_CONN);
		String encKey = (String)getAttribute(Constants.ENCRYPT_KEY);
		UserLogin ul = new UserLogin(dbConn, encKey);

		try {
			return ul.modifyUser(authId, userName, password, resetFlag);
		} catch (DatabaseException de) {
			throw new InvalidDataException(de);
		}
	}
}