package com.biomed.smarttrak.security;

// Java 8
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

//SMTBaseLibs
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.exception.NotAuthorizedException;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.DjangoPasswordHasher;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.SHAEncrypt;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

//WebCrescendo libs
import com.smt.sitebuilder.common.constants.ErrorCodes;
import com.smt.sitebuilder.security.DBLoginModule;
import com.smt.sitebuilder.security.UserLogin;
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
		UserLogin ul = new UserLogin(dbConn, getAttributes());
		UserDataVO authUser = ul.getAuthRecord(null, username);
		authUser.setEmailAddress(username);

		//getAuthRecord never returns null.  Test the VO for authenticationId, throw if not found
		if (StringUtil.isEmpty(authUser.getAuthenticationId()))
			throw new AuthenticationException(ErrorCodes.ERR_INVALID_LOGIN);

		authUser = testUserPassword(ul, authUser, password); //this will throw if the password can't be verified

		// Return user authented user, after loading their Smarttrak data
		UserVO user = loadSmarttrakUser(loadUserData(null, authUser.getAuthenticationId()));

		//Check if we got the userId.  If not, then user passed WC Security but not Smarttrak Security.
		if(StringUtil.isEmpty(user.getUserId())) {
			throw new AuthenticationException(ErrorCodes.ERR_NOT_AUTHORIZED);
		}

		return user;
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
	 */
	public UserVO loadSmarttrakUser(UserDataVO userData) throws AuthenticationException {
		UserVO stUser = new UserVO();
		stUser.setData(userData.getDataMap());
		stUser.setAttributes(userData.getAttributes());
		stUser.setAuthenticated(userData.isAuthenticated());
		loadCustomData(stUser);

		return stUser;
	}


	/**
	 * Retrieves base SmartTRAK-specific user data for a user based on 
	 * the user ID field or the profile ID field populated
	 * WC profile ID and populates the ST user bean with that data.
	 * @param conn
	 * @param user
	 * @return
	 */
	private void loadCustomData(UserVO user) throws AuthenticationException {
		Connection dbConn = (Connection)getAttribute(GlobalConfig.KEY_DB_CONN);
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);

		// use profile ID as that is all we have at the moment.
		StringBuilder sql = new StringBuilder(1100);
		sql.append("select u.user_id, u.account_id, u.register_submittal_id, u.fd_auth_flg, u.ga_auth_flg, u.update_dt, u.create_dt, ");
		sql.append("u.acct_owner_flg, coalesce(u.expiration_dt, a.expiration_dt) as expiration_dt, u.status_cd, u.active_flg, a.type_id, ");
		sql.append("t.team_id, t.account_id, t.team_nm, t.default_flg, t.private_flg, a.account_nm, p.profile_id as source_id, p.email_address_txt as source_email, ");
		sql.append("u.expiration_dt as user_expiration, a.expiration_dt as account_expiration, a.status_no ");
		sql.append("from ").append(schema).append("biomedgps_user u ");
		sql.append("left outer join ").append(schema).append("biomedgps_user_team_xr xr on u.user_id=xr.user_id ");
		sql.append("left outer join ").append(schema).append("biomedgps_team t on xr.team_id=t.team_id ");
		sql.append("inner join ").append(schema).append("biomedgps_account a on u.account_id=a.account_id ");
		sql.append("left outer join register_data rd on rd.register_submittal_id = u.register_submittal_id and register_field_id = ? ");
		sql.append("left outer join profile p on p.profile_id = rd.value_txt ");
		sql.append("where u.profile_id=? ");
		sql.append("order by t.team_nm"); //active > 0 includes Active and Demo.
		log.debug(sql + user.getProfileId());

		int iter = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, UserVO.RegistrationMap.SOURCE.getFieldId());
			ps.setString(2, user.getProfileId());
			ResultSet rs = ps.executeQuery();
			StringEncrypter se = new StringEncrypter((String)getAttribute(Constants.ENCRYPT_KEY));
			while (rs.next()) {
				if (iter == 0) { //first run, get the fields from the _user table.  They're the same each time by.
					user.setUserId(rs.getString("user_id"));
					user.setAccountId(rs.getString("account_id"));
					user.setRegisterSubmittalId(rs.getString("register_submittal_id"));
					user.setFdAuthFlg(rs.getInt("fd_auth_flg"));
					user.setGaAuthFlg(rs.getInt("ga_auth_flg"));
					user.setAcctOwnerFlg(rs.getInt("acct_owner_flg"));
					user.setExpirationDate(rs.getDate("expiration_dt")); //used by the role module to block access to the site
					user.setLicenseType(rs.getString("status_cd"));
					user.setStatusFlg(rs.getInt("active_flg"));
					user.setAccountName(rs.getString("account_nm"));
					user.setSourceId(rs.getString("source_id"));
					user.setSourceEmail(decrypt(se, rs.getString("source_email")));
					user.setCreateDate(rs.getDate("create_dt"));
					user.setUpdateDate(rs.getDate("update_dt"));
					
					checkAuthentication(rs);

					// Account Type - used by the role module to restrict users to Updates Only (role) - just pass the "4" along to it.
					String type = rs.getString("type_id");
					if ("4".equals(type))
						user.setLicenseType(type);

					iter = 1;
				}
				user.addTeam(new TeamVO(rs));
			}
		} catch (SQLException sqle) {
			log.error("could not query Smarttrak user/teams", sqle);
		} catch (EncryptionException e) {
			log.error("Failed to create String encrypter", e);
		}

		if (log.isDebugEnabled() && user.getTeams() != null)
			log.debug("loaded " + user.getTeams().size() + " teams for " + user.getEmailAddress());
	}

	
	/**
	 * Check potential auth disconnects and throw the proper error code for each
	 * @param rs
	 * @throws AuthenticationException
	 */
	private void checkAuthentication(ResultSet rs) throws AuthenticationException {
		Date now = new Date();
		try {
			Date userExp = rs.getTimestamp("user_expiration");
			if (userExp != null && now.after(userExp)) throw new AuthenticationException(ErrorCodes.CUSTOM_ERROR_MSG + "userExpired");
			if (rs.getInt("active_flg") < 1) throw new AuthenticationException(ErrorCodes.CUSTOM_ERROR_MSG + "userInvalid");
			Date accountExp = rs.getTimestamp("account_expiration");
			if (accountExp != null && now.after(accountExp)) throw new AuthenticationException(ErrorCodes.CUSTOM_ERROR_MSG + "accountExpired");
			if (!"A".equals(rs.getString("status_no"))) throw new AuthenticationException(ErrorCodes.CUSTOM_ERROR_MSG + "accountInvalid");
		} catch (SQLException e) {
			log.error(e);
			throw new AuthenticationException(ErrorCodes.CUSTOM_ERROR_MSG + "otherLoginException");
		}
	}

	/**
	 * Decrypt the supplied string
	 * @param se
	 * @param string
	 * @return
	 */
	private String decrypt(StringEncrypter se, String string) {
		if (StringUtil.isEmpty(string)) return null;
		try {
			return se.decrypt(string);
		} catch (Exception e) {
			return string;
		}
	}


	/**
	 * tests the password obtained from the visitor against the auth record's password using
	 * a waterfall of encryption schemes.
	 * @param authUser
	 * @param proclaimedPassword
	 * @throws AuthenticationException
	 */
	protected UserDataVO testUserPassword(UserLogin ul, UserDataVO authUser, String allegedPswd) 
			throws AuthenticationException {
		//test the password against the legacy/Django scheme.  If it matches we're done
		DjangoPasswordHasher hasher = new DjangoPasswordHasher();
		if (hasher.checkPassword(allegedPswd, authUser.getPassword()))
			return authUser;

		//test the password against SHA1 - the "legacy-legacy" Smarttrak scheme!  if it matches we're done
		SHAEncrypt sha = new SHAEncrypt();
		try {
			if (sha.encrypt(allegedPswd).equals(authUser.getPassword()))
				return authUser;
		} catch (Exception e) {
			log.warn("password is not SHA1, or didn't match the stored value", e);
		}

		//finally, test the password using the WC core.  Any password created or changed after 7/1/2017 will fall into this scenario.
		try {
			return ul.checkExistingCredentials(authUser.getEmailAddress(), allegedPswd);
		} catch (NotAuthorizedException e) {
			throw new AuthenticationException(ErrorCodes.ERR_INVALID_LOGIN, e);
		}		
	}
}