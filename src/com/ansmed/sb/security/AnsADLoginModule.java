package com.ansmed.sb.security;

// JDK 1.5.0
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.exception.NotAuthorizedException;
import com.siliconmtn.security.AbstractLoginModule;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.LDAPAuth;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

// SB Libs
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.common.constants.ErrorCodes;
import com.smt.sitebuilder.security.UserLogin;

/*****************************************************************************
 <p><b>Title</b>: AnsADLoginModule.java</p>
 <p><b>Description: </b>Implements user login for all site activities.  This 
 class is called via a factory pattern.  It is identified in the config file.</p>
 <p> Uses the SB data base to retrieve authentication info</p>
 <p>Authenticates against the SB profile first and if not found, uses Active
 Directory to find the user</p>
 <p>Copyright: Copyright (c) 2000 - 2005 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 2.0
 @since Jun 3, 2006
 Code Updates
 James Camire, Jun 3, 2006 - Creating Initial Class File
 ***************************************************************************/

public class AnsADLoginModule extends AbstractLoginModule {

	private final String SJM_AD_HOST = "SJM_AD_HOST_";
	private final String SJM_LOGIN_DOMAIN = "SJM_LOGIN_DOMAIN_";
	private final String SJM_LDAP_BASE = "SJM_LDAP_BASE_";
	private final int LDAP_DOMAIN_LIMIT = 3;
	private int domainCount = 0;
	private Map<String, String> LDAPMap = null;

	public AnsADLoginModule() {
		super();
	}

	/**
	 * @param arg0
	 */
	public AnsADLoginModule(Map<String, Object> arg0) {
		super(arg0);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#retrieveUserData(java.lang.String, java.lang.String)
	 */
	@Override 
	public UserDataVO authenticateUser(String user, String pwd) 
			throws AuthenticationException {
		log.info("Starting user login: " + user + "/" + pwd);
		UserDataVO profile = new UserDataVO();
		if (user == null || pwd == null) throw new AuthenticationException(ErrorCodes.ERR_INVALID_LOGIN);        
		// Get the database Connection
		Connection dbConn = (Connection)getAttribute(GlobalConfig.KEY_DB_CONN);
		// If user name is the email address, check sb profile, otherwise, use AD to authenticate
		if(user.indexOf("@") > 0) {
			String encKey = (String)getAttribute(Constants.ENCRYPT_KEY);
			UserLogin ul = new UserLogin(dbConn, encKey);

			// Connect to the EJB and check the user Credentials
			UserDataVO authData = null;
			try {
				authData = ul.checkExistingCredentials(user, pwd);
			} catch (NotAuthorizedException nae) {
				throw new AuthenticationException(ErrorCodes.ERR_INVALID_LOGIN);
			}

			//NOTE: authData cannot be null at this location
			log.debug("authData password history: " + authData.getPasswordHistory());
			// set the authenticationId for logging, etc.
			profile.setAuthenticationId(authData.getAuthenticationId());

			if (authData.isAuthenticated()) {
				// user exists and is authenticated, retrieve profile
				log.debug("user has passed preliminary authentication (i.e. is authenticated)...");
				ProfileManager pm = ProfileManagerFactory.getInstance(getAttributes());
				try {
					profile = pm.getProfile(profile.getAuthenticationId(), dbConn, ProfileManager.AUTH_ID_LOOKUP, null);
				} catch(Exception e) {
					log.debug("Unable to retrieve profile: " + e.getMessage());
				}
				profile.setAuthenticated(authData.isAuthenticated());
				profile.setPasswordResetFlag(authData.getPasswordResetFlag());
				profile.setPasswordHistory(authData.getPasswordHistory());
				log.debug("user's password history is: " + profile.getPasswordHistory());
			}

		} else {
			// use LDAP authentication
			String host = null;
			String domain = null;
			String base = null;
			// load the LDAP properties map with host, domain, and base properties
			this.loadLDAPProperties();
			log.debug("found " + domainCount + " LDAP domains");

			// Loop the number of domains and attempt to authenticate
			int failCount = 0;
			for (int i = 1; i <= domainCount; i++) {
				host = (String) getAttribute(SJM_AD_HOST + i);
				domain = (String) getAttribute(SJM_LOGIN_DOMAIN + i);
				// if user supplied a domain, let's use that
				if (user.indexOf("/") > -1) {
					domain = user.substring(0,user.indexOf("/"));
					user = user.substring(user.indexOf("/") + 1);
				} else if (user.indexOf("\\") > -1) {
					domain = user.substring(0,user.indexOf("\\"));
					user = user.substring(user.indexOf("\\") + 1);
				}
				base = (String) getAttribute(SJM_LDAP_BASE + i);

				log.debug("attempting auth to host | domain | base: " + host + " | " + domain + " | " + base);
				LDAPAuth ldap = new LDAPAuth(host, user, pwd, domain);
				// Authenticate against the AD controller
				try {
					profile = ldap.search(base);
					profile.setAuthenticationId(user);
					log.debug("Profile from LDAP auth: " + profile);

					// check sales rep table to further populate profile
					log.debug("checking sales rep table for more profile info");
					profile = this.checkRepTable(profile, user, dbConn);
					profile.setAuthenticated(true);
					// since we have successfully authenticated, break out of loop
					log.debug("successful auth, exiting loop");
					break;
				} catch (Exception e) {
					failCount++;
					log.debug("auth failed, failCount is: " + failCount);
					profile.setAuthenticationId(user);
				}
			}
		}
		// Return the user data
		return profile;
	}

	/**
	 * Manages the user information for the authentication system
	 */
	@Override
	public String saveAuthRecord(String authId, String userName, String password, Integer resetFlag)  
			throws InvalidDataException {
		if (userName == null || userName.length() == 0) throw new InvalidDataException();
		if (password == null || password.length() == 0) throw new InvalidDataException();
		String auth = null;

		// Get the database Connection
		Connection dbConn = (Connection)getAttribute(GlobalConfig.KEY_DB_CONN);
		String encKey = (String)getAttribute(Constants.ENCRYPT_KEY);
		UserLogin ul = new UserLogin(dbConn, encKey);

		try {
			auth = ul.modifyUser(authId, userName, password, resetFlag);
		} catch (DatabaseException de) {
			log.error("Error modifying user's authentication record, ", de);
		}

		return auth;
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#retrievePasswordAge(java.lang.String)
	 */
	@Override
	public Long retrievePasswordAge(String authenticationId) {
		// Get the database Connection
		Connection dbConn = (Connection)getAttribute(GlobalConfig.KEY_DB_CONN);
		String encKey = (String)getAttribute(Constants.ENCRYPT_KEY);
		UserLogin ul = new UserLogin(dbConn, encKey);
		return ul.getPasswordAge(authenticationId);
	}

	/**
	 * Deletes a user from the auth system
	 */
	@Override
	public boolean deleteUser(String authId) {
		// Get the database Connection
		Connection dbConn = (Connection)getAttribute(GlobalConfig.KEY_DB_CONN);
		String encKey = (String)getAttribute(Constants.ENCRYPT_KEY);
		UserLogin ul = new UserLogin(dbConn, encKey);
		return ul.deleteUser(authId);
	}

	@Override
	public String retrieveAuthenticationId(String userName) {
		// Get the database Connection
		Connection dbConn = (Connection)getAttribute(GlobalConfig.KEY_DB_CONN);
		String encKey = (String)getAttribute(Constants.ENCRYPT_KEY);
		UserLogin ul = new UserLogin(dbConn, encKey);
		return ul.checkAuth(userName);
	}

	/**
	 * 
	 * @param user
	 * @return
	 */
	private UserDataVO checkRepTable(UserDataVO profile, String login, Connection dbConn) {
		log.debug("Starting sales rep lookup");
		UserDataVO user = profile;
		String schema = (String)getAttribute("customDbSchema");
		StringBuilder sb = new StringBuilder(100);
		sb.append("select * from ").append(schema).append("ans_sales_rep ");
		sb.append("where ans_login_id = ?");
		try (PreparedStatement ps = dbConn.prepareStatement(sb.toString())) {
			ps.setString(1, login);
			log.debug("Checking sales rep table: " + sb + "|" + login);

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				user.setFirstName(rs.getString("first_nm"));
				user.setLastName(rs.getString("last_nm"));
				user.setProfileId(rs.getString("profile_id"));
				user.setEmailAddress(rs.getString("email_address_txt"));
				user.addPhone(new PhoneVO(rs.getString("phone_number_txt")));
			}
			log.debug("User data after sales rep lookup:" + user);
		} catch(SQLException sqle) {
			log.error("error retrieving rep info", sqle);
		}

		return user;
	}

	/**
	 * Loads the list of AD domains to use for attempting authentication.
	 */
	private void loadLDAPProperties() {
		log.debug("loading LDAP domain properties");
		this.LDAPMap = new HashMap<>();
		String pKey = null;
		String pVal = null;
		for (int i = 1; i <= LDAP_DOMAIN_LIMIT; i++) {
			pKey = SJM_AD_HOST + i;
			pVal = StringUtil.checkVal(getAttribute(pKey));
			if (pVal.length() > 0) {
				LDAPMap.put(pKey, (String)getAttribute(pKey));
			} else {
				// break out of the loop
				break;
			}
			pKey = SJM_LOGIN_DOMAIN + i;
			if (getAttribute(pKey) != null) LDAPMap.put(pKey, (String)getAttribute(pKey));
			pKey = SJM_LDAP_BASE + i;
			if (getAttribute(pKey) != null) LDAPMap.put(pKey, (String)getAttribute(pKey));
			domainCount++;
		}
	}

	@Override
	public boolean resetPassword(String pwd, UserDataVO user) {
		Connection dbConn = (Connection)getAttribute(GlobalConfig.KEY_DB_CONN);
		String encKey = (String)getAttribute(Constants.ENCRYPT_KEY);

		// update user's password.
		StringBuilder sql = new StringBuilder(100);
		sql.append("update authentication set password_txt = ?, ");
		sql.append("password_reset_flg = ? where authentication_id = ?");
		log.debug("Password reset SQL: " + sql.toString() + " | " + 0 + " | " + user.getAuthenticationId());

		// get the valid newPassword
		String encPwd = (String)getAttribute(Constants.ENCRYPT_KEY);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			// encrypt the new password before storing it.
			StringEncrypter se = new StringEncrypter(encKey);
			encPwd = se.encrypt(pwd);

			ps.setString(1, encPwd);
			ps.setInt(2,0);
			ps.setString(3, user.getAuthenticationId());

			int count = ps.executeUpdate();
			if (count < 1) throw new SQLException();
			return true;
		} catch (SQLException e) {
			log.error("Error occurred resetting user password. ", e);
			return false;
		} catch (EncryptionException ee) {
			log.error("Error encrypting user password. ", ee);
			return false;
		} catch(Exception e){
			log.error("Something happened", e);
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#authenticateUser(java.lang.String)
	 */
	@Override
	public UserDataVO authenticateUser(String encProfileId) throws AuthenticationException {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#recordLogin(java.lang.String, java.lang.String, java.util.Date)
	 */
	@Override
	public void recordLogin(UserDataVO user, String siteId, String userAgent, String ipAddr, Date d) {
		// not implemented
	}
}