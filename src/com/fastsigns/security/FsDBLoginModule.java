package com.fastsigns.security;

// JDK 1.6
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


// SMTBaseLibs 2.0
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.exception.NotAuthorizedException;
import com.siliconmtn.security.AbstractLoginModule;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.EmailAddressNotFoundException;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.SHAEncrypt;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.RandomAlphaNumeric;

//WebCrescendo 2.0
import com.smt.sitebuilder.action.registration.ResponseLoader;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.common.constants.ErrorCodes;
import com.smt.sitebuilder.security.UserLogin;

/*****************************************************************************
 <p><b>Title</b>: FsDBLoginModule.java</p>
 <p><b>Description: </b>Implements user login for all site activities.  This 
 class is called via a factory pattern.  It is identified in the config file.</p>
 <p> Uses the SB data base to retrieve authentication info
 <p>Copyright: Copyright (c) 2000 - 2010 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Dave Bargerhuff
 @version 1.0
 @since Dec 14, 2010
 Code Updates:
 Dec 14, 2010: Dave Bargerhuff - Creating Initial Class File
 May 20, 2013: Dave Bargerhuff - made preliminary updates to reflecting changes in login processing
 Jun 03, 2013: Dave Bargerhuff - further updates to login processing.
 ***************************************************************************/

public class FsDBLoginModule extends AbstractLoginModule {
	
	public static final int TEMP_PASSWORD_LENGTH = 8;

    /**
     * 
     */
    public FsDBLoginModule() {
        super();
    }

    /**
     * @param arg0
     */
    public FsDBLoginModule(Map<String, Object> arg0) {
        super(arg0);
    }

    /* (non-Javadoc)
     * @see com.siliconmtn.security.AbstractLoginModule#authenticate(java.lang.String, java.lang.String)
     */
    @Deprecated
    @Override
    public String authenticate(String user, String pwd) 
    		throws AuthenticationException {
        log.debug("Starting user login: " + user + "/" + pwd);
        if (user == null || pwd == null) throw new AuthenticationException("No login info");
        
        // Get the database Connection
        Connection dbConn = (Connection)initVals.get(GlobalConfig.KEY_DB_CONN);
        String encKey = (String)initVals.get(Constants.ENCRYPT_KEY);
        UserLogin ul = new UserLogin(dbConn, encKey);
        
        String authId = null;
        
        log.debug("user/pwd: " + user + " / " + pwd);
        // check credentials using standard pwd encryption
        try {
            authId = ul.checkCredentials(user, pwd);
        } catch (NotAuthorizedException e) {
            throw new AuthenticationException(ErrorCodes.ERR_INVALID_LOGIN, e);
        }
        
        // if standard auth failed try auth using pwd encrypted in SHA-1
        if (authId == null || authId.length() == 0) {
        	log.debug("std auth failed, attempting auth using SHA-1 pwd");
        	
        	SHAEncrypt sha = new SHAEncrypt();
        	String shaPwd = null;
        	try {
        		shaPwd = sha.encrypt(pwd);
        		log.debug("SHA-1 encrypted password: " + shaPwd);
        	} catch (Exception e) {
        		log.error("Error encrypting user pwd using SHA-1.");
        		throw new AuthenticationException(ErrorCodes.ERR_INVALID_LOGIN, e);
        	}
        	
        	try {
        		authId = ul.checkEncryptedCredentials(user, shaPwd);
        	} catch (NotAuthorizedException nae) {
        		throw new AuthenticationException(ErrorCodes.ERR_INVALID_LOGIN, nae);
        	}
        	
    		// if successful auth using SHA-1, update user's auth password using standard encryption
        	if (authId != null && authId.length() > 0) {
        		try {
        			this.manageUser(authId, user, pwd, 0);
        		} catch (InvalidDataException ide) {
        			log.error("Error updating user's auth credentials to use standard pwd encryption." ,ide);
        		}
        	} else {
                throw new AuthenticationException(ErrorCodes.ERR_INVALID_LOGIN);
        	}
        }

        // Return the user's auth Id
        return authId;
    }
    
    /* (non-Javadoc)
     * @see com.siliconmtn.security.AbstractLoginModule#authenticateUser(java.lang.String, java.lang.String)
     */
	@Override
	public UserDataVO authenticateUser(String user, String pwd)
			throws AuthenticationException {
		log.debug("Starting user login via authenticateUser: " + user + "/" + pwd);
		//UserDataVO authUser = new UserDataVO();
		//authUser.setAuthenticationId(this.authenticate(user, pwd));
		
        if (user == null || pwd == null)
            throw new AuthenticationException("No login information was supplied for authentication.");
        
        // Get the database Connection
        Connection dbConn = (Connection)initVals.get(GlobalConfig.KEY_DB_CONN);
        String encKey = (String)initVals.get(Constants.ENCRYPT_KEY);
        UserLogin ul = new UserLogin(dbConn, encKey);
        
        /*
         * Check user credentials and return a UserDataVO (vo).  
         * The vo is empty (not null) if the user does not exist and is populated with authentication 
         * data if the user does exist.  If the vo is not empty, a flag is set signifying 
         * whether or not the user was authenticated.
         */
       	UserDataVO authUser = ul.checkExistingCredentials(user, pwd);   
       	
        if (authUser == null) {
			// specified user does not exist in the authentication table.
			throw new AuthenticationException(ErrorCodes.ERR_INVALID_LOGIN);	
        }
        
		// If user exists but standard auth pwd failed, try auth using pwd
		// encrypted in SHA-1
		if (!authUser.isAuthenticated()) {
			log.debug("std auth failed, attempting auth using SHA-1 pwd");

			SHAEncrypt sha = new SHAEncrypt();
			String shaPwd = null;
			try {
				shaPwd = sha.encrypt(pwd);
				log.debug("SHA-1 encrypted password: " + shaPwd);
			} catch (Exception e) {
				log.error("Error encrypting user pwd using SHA-1, " + e);
			}

			String authId = null;
			if (shaPwd != null) {
				try {
					authId = ul.checkEncryptedCredentials(user, shaPwd);
				} catch (NotAuthorizedException nae) {
					log.error("Error checking encrypted credentials during user login, ", nae);
				}
			}

			// if successful auth using SHA-1 pwd, user is authenticated so
			// update user's auth password using standard encryption.
			if (authId != null) {
				authUser.setAuthenticated(true);
				try {
					this.manageUser(authId, user, pwd, 0);
				} catch (InvalidDataException ide) {
					log.error("Error updating user's auth credentials to use standard password encryption.", ide);
				}
				
			} else {
				//authId is null
				throw new AuthenticationException(ErrorCodes.ERR_INVALID_LOGIN);
			}
		}

		// Return user authentication data
		return authUser;
	}
	
    
    /**
     * Manages the user information for the authentication system
     */
    public String manageUser(String authId, String userName, String password, Integer resetFlag)  
    throws InvalidDataException {
        if (userName == null || userName.length() == 0 || password == null || password.length() == 0)
        	throw new InvalidDataException();
        
        String auth = null;
        
        // Get the database Connection
        Connection dbConn = (Connection)initVals.get(GlobalConfig.KEY_DB_CONN);
        String encKey = (String)initVals.get(Constants.ENCRYPT_KEY);
        UserLogin ul = new UserLogin(dbConn, encKey);
        
        try {
            auth = ul.modifyUser(authId, userName, password, resetFlag);
        } catch (DatabaseException de) {
            log.error("Unable to manage user, ", de);
        }
        
        return auth;
    }
    
    /**
     * Retrieves a password based upon the user name.
     */
    public String retrievePassword(String emailAddress)
    throws EmailAddressNotFoundException {
        // Get the database Connection
        Connection dbConn = (Connection)initVals.get(GlobalConfig.KEY_DB_CONN);
        String encKey = (String)initVals.get(Constants.ENCRYPT_KEY);
        UserLogin ul = new UserLogin(dbConn, encKey);
        
        //get the raw encrypted password
        String pwd = ul.getPassword(emailAddress, false);
        
        // if pwd is not null we need to check pwd to see if it is using our encryption
        if (pwd != null) pwd = this.checkEncryptedPassword(emailAddress, pwd, encKey);
        
        return pwd;
    }
    
    /* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#retrievePasswordAge(java.lang.String)
	 */
	@Override
	public Long retrievePasswordAge(String authenticationId) {
        // Get the database Connection
        Connection dbConn = (Connection)initVals.get(GlobalConfig.KEY_DB_CONN);
        String encKey = (String)initVals.get(Constants.ENCRYPT_KEY);
        UserLogin ul = new UserLogin(dbConn, encKey);
        return ul.getPasswordAge(authenticationId);      
    }
    
    /**
     * Checks encrypted password to determine if the password uses SMT encryption.  If not,
     * a temporary password is generated, the user's password is updated with the temporary
     * password, and the password reset flag is set to force a password change at next
     * login.  The temporary password is returned to the user.
     * @param emailAddress
     * @param pwd
     * @param encKey
     * @return
     */
    public String checkEncryptedPassword(String emailAddress, String pwd, String encKey) {
    	log.debug("checking encrypted password");    	
    	String decPwd = null;
    	try {
    		StringEncrypter se = new StringEncrypter(encKey);
    		decPwd = se.decrypt(pwd);
    	} catch (EncryptionException ee) {
    		log.debug("Failed to decrypt user password, generating temp password, forcing reset.");
    		// set 'decrypted' password to null to force a reset
    	}
    	
    	// if we couldn't decrypt the password, we are going to assume that it is
    	// encrypted with our encryption.
    	if (decPwd == null) {
    		//get user's authId
    		String authId = this.retrieveAuthenticationId(emailAddress);
    		if (authId != null) {
        		//generate a tmp password
    			decPwd = RandomAlphaNumeric.generateRandom(TEMP_PASSWORD_LENGTH, true);
        		// update user's existing password, set password reset flag
        		try {
        			this.manageUser(authId, emailAddress, decPwd, new Integer(1));
        		} catch (InvalidDataException ide) {
        			decPwd = null;
        			log.error("Error managing user's temporary password, ", ide);
        		}
    		}
    	}
    	return decPwd;
    }
    
    /**
     * Retrieves a password reset flag value based upon the user name.
     */
    public Integer retrievePasswordResetFlag(String emailAddress) {
        // Get the database Connection
        Connection dbConn = (Connection)initVals.get(GlobalConfig.KEY_DB_CONN);
        String encKey = (String)initVals.get(Constants.ENCRYPT_KEY);
        UserLogin ul = new UserLogin(dbConn, encKey);
        
        Integer pwdResetFlg = ul.getPasswordResetFlag(emailAddress);
        
        return pwdResetFlg;
    }
    
    /**
     * Deletes a user from the auth system
     */
    public boolean deleteUser(String authId) {
        // Get the database Connection
        Connection dbConn = (Connection)initVals.get(GlobalConfig.KEY_DB_CONN);
        String encKey = (String)initVals.get(Constants.ENCRYPT_KEY);
        UserLogin ul = new UserLogin(dbConn, encKey);
        
        boolean success = ul.deleteUser(authId);
        
        return success;
    }

	@Override
	public String retrieveAuthenticationId(String userName) {
        // Get the database Connection
        Connection dbConn = (Connection)initVals.get(GlobalConfig.KEY_DB_CONN);
        String encKey = (String)initVals.get(Constants.ENCRYPT_KEY);
        UserLogin ul = new UserLogin(dbConn, encKey);
        return ul.checkAuth(userName);
	}
	
	@Override
    public String retrieveAuthIdByCookie(String profileId) {
        // Get the database Connection
        Connection dbConn = (Connection)initVals.get(GlobalConfig.KEY_DB_CONN);
        String encKey = (String)initVals.get(Constants.ENCRYPT_KEY);
        UserLogin ul = new UserLogin(dbConn, encKey);
        return ul.getAuthFromCookie(profileId);
    }

	@Override
	public boolean resetPassword(String pwd, UserDataVO user) {

		Connection dbConn = (Connection)initVals.get(GlobalConfig.KEY_DB_CONN);
        String encKey = (String)initVals.get(Constants.ENCRYPT_KEY);
        		
		// update user's password.
		StringBuffer sql = new StringBuffer();
		sql.append("update authentication set password_txt = ?, ");
		sql.append("password_reset_flg = ? where authentication_id = ?");
		
		log.debug("Password reset SQL: " + sql.toString() + " | " + 0 + " | " + user.getAuthenticationId());
		
		PreparedStatement ps = null;
		
		// get the valid newPassword
		
		String encPwd = (String)initVals.get(Constants.ENCRYPT_KEY);
		
		try {
			// encrypt the new password before storing it.
			StringEncrypter se = new StringEncrypter(encKey);
			encPwd = se.encrypt(pwd);
			
            ps = dbConn.prepareStatement(sql.toString());
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
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {
					log.error("Error closing PreparedStatement. ",e);
				}
			}
		}
	}

	/**
	 * this method is build specifically to support FsHybridLoginModule, which supports
	 * independant profile management (to Keystone).  Since that module advertises
	 * "I'll get you use the user's profile", we must do the same (even though we're within 
	 * WC and don't have to)
	 */
	@Override
	public UserDataVO retrieveUserData(String loginName, String password)
			throws AuthenticationException {
		
		UserDataVO authData = this.authenticateUser(loginName, password);
		UserDataVO newUser = null;
		
		if (authData.isAuthenticated()) {
			//Retrieve the full profile by authentication ID lookup
			ProfileManager pm = ProfileManagerFactory.getInstance(initVals);
			try {
				Connection dbConn = (Connection)initVals.get(GlobalConfig.KEY_DB_CONN);
				newUser = pm.getProfile(authData.getAuthenticationId(), dbConn, ProfileManager.AUTH_ID_LOOKUP, null);
				newUser.setAuthenticated(authData.isAuthenticated());
				newUser.setAuthenticationLogId(authData.getAuthenticationLogId());
				newUser.setPasswordResetFlag(authData.getPasswordResetFlag());
				
				if (authData.getPasswordResetFlag() == 1)
					newUser.setPasswordHistory(authData.getPasswordHistory());

				log.debug("user password history: " + newUser.getPasswordHistory());
			} catch(Exception e) {
				log.debug("Unable to retrieve profile: " + e.getMessage());
			}
			
		}
		return newUser;
	}

	
	@Override
	public UserDataVO retrieveUserData(String encProfileId)
			throws AuthenticationException {
		log.debug("Retrieving user data via encrypted profileId");
		UserDataVO user = null;
		String decProfileId = null;
		String encKey = (String)initVals.get(Constants.ENCRYPT_KEY);
		try {
			encProfileId = URLDecoder.decode(encProfileId, "UTF-8");
			StringEncrypter se = new StringEncrypter(encKey);
			decProfileId = se.decrypt(encProfileId);
		} catch (Exception e) {
			log.error("Error decrypting an encrypted profile ID, ", e);
			return user;
		}

		ProfileManager pm = ProfileManagerFactory.getInstance(this.initVals);
		Connection dbConn = (Connection)initVals.get(GlobalConfig.KEY_DB_CONN);
		try {
			List<String> profileIds = new ArrayList<String>();
			profileIds.add(decProfileId);
			List<UserDataVO> profiles = pm.searchProfile(dbConn, profileIds);
			if (! profiles.isEmpty()) {
				user = profiles.get(0);
				user.setPasswordResetFlag(retrievePasswordResetFlag(user.getEmailAddress()));
			}
		} catch(Exception e) {
			log.error("Unable to retrieve profile, ", e);
		}

		if (user == null) return user;
		//  retrieve user's registration data
		String siteId = (String) initVals.get(Constants.SITE_ALIAS_PATH);

		//leverage the registration response loader to update the UserDataVO with submitted registration data.
		ResponseLoader rl = new ResponseLoader();
		rl.setDbConn(dbConn);
		rl.loadRegistrationResponses(user, siteId);

		return user;
	}
}
