package com.fastsigns.security;

// JDK 1.6.x
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Connection;
import java.util.Date;
import java.util.Map;



// SMT Base Libs
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.AESEncryption;
import com.siliconmtn.security.AESKey;
import com.siliconmtn.security.AbstractLoginModule;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.EmailAddressNotFoundException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;

// Web Crescendo Libs
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.UserLogin;

/*****************************************************************************
 <p><b>Title</b>: FsKeystoneLoginModule.java</p>
 <p><b>Description: </b>
 	Logs the user into WC using Keystone for authentication and data retrieval.
 	
 <p>Copyright: Copyright (c) 2000 - 2012 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Dec 14, 2012
 ***************************************************************************/

public class FsHelpSiteLoginModule extends AbstractLoginModule {
	private final String loginApiKey = "";
	private final static String passPhrase = "ae790270-42d1-4663-adf8-46d5980c71c1";
	private final static String saltValue = "5cd444b2-48e6-4a0b-aa7f-766e8d6de614";
	private final static String initVector = "3144f1059fac47f5";
	private final String param1 = "sharedToken";
	//private final String param2 = "date";
	public static void main(String [] args) {
		AESKey key = new AESKey();
		key.setPassPhrase(passPhrase);
		key.setSaltValue(saltValue);
		key.setVector(initVector);
		try {
			String aes = AESEncryption.encryptString("sharedToken=EF3478A623204C86A6DF|param2=" + Convert.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss"), key, 256);
			System.out.println(aes);
			System.out.println(AESEncryption.decryptString(URLDecoder.decode("%2fPlau9uhZd12WvOwEc%2fpTWikWjXn094ERPpclnRB%2b4XYpNDibq5MAwUMD60Ey7LX1cqTaphXSbRZSc8meu7hoQ%3d%3d", "UTF-8"), key, 256));
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			System.out.println("password=pass&emailAddress=" + URLEncoder.encode(AESEncryption.encryptString("sharedToken=EF3478A623204C86A6DF|param2=" + Convert.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss"), key, 256), "UTF-8"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	//private final String param2 = "date";
    public FsHelpSiteLoginModule() {
        super();
        super.setUserProfile(true);
    }

    /**
     * @param arg0
     */
    public FsHelpSiteLoginModule(Map<String, Object> arg0) {
        super(arg0);
    }

    /* (non-Javadoc)
     * @see com.siliconmtn.security.AbstractLoginModule#authenticate(java.lang.String, java.lang.String)
     * "Login by cookie" method
     */
    @Override
    public UserDataVO retrieveUserData(String encProfileId) 
    throws AuthenticationException {
    	if(verifiedConnection()){
    		UserDataVO user = new UserDataVO();
    		user.setAuthenticationId("allowed");
    		user.setAuthenticated(true);
    		return user;
    	}
    	return null;
    }
    
    private boolean verifiedConnection() {
		try {
			log.debug("Verifying Login");
	    	SMTServletRequest req = (SMTServletRequest) this.getInitVals().get(GlobalConfig.HTTP_REQUEST);
	    	log.debug("Retrieved Request Object");
	        String p = req.getQueryString().replace("password=pass&emailAddress=", "").replace("%25", "%");
	        log.error("emailAddressStr = " + p);
			String output = null;
			
	    	AESKey key = new AESKey();
			key.setPassPhrase(passPhrase);
			key.setSaltValue(saltValue);
			key.setVector(initVector);
			String decodedVal = URLDecoder.decode(p, "UTF-8");
			log.error("URLdecoded String: " + decodedVal);
			output = AESEncryption.decryptString(decodedVal, key, 256);
			log.error("Decrypted value: " + output);
			String p1 = "";
			Date p2 = null;
			
			String [] o = output.split("\\|");
			for(int i = 0; i < o.length; i++){
				if(o[i].contains(param1)){
					p1 = o[i].split("=")[1];
				} else {
					String temp =  o[i].split("=")[1];
					p2 = Convert.formatDate("yyyy-MM-dd HH:mm:ss", temp);
				}
			}
			if(p2 == null)
				log.error("Problem Parsing Date");
			log.error(Convert.getCurrentTimestamp().getTime() - p2.getTime());
			if(Convert.getCurrentTimestamp().getTime() - p2.getTime() < Long.parseLong("60000") && p1.equals("EF3478A623204C86A6DF")){
				return true;
			} else if(Convert.getCurrentTimestamp().getTime() - p2.getTime() > Long.parseLong("60000")) {
				log.error("Problem with timestamp : " + p2.toString());
			} else if (!p1.equals("EF3478A623204C86A6DF")){
				log.error("Problem with sharedToken : " + p1);
			}
			//log.debug(p1);
		} catch (NullPointerException e){
			log.debug("User not authorized");
			return false;
		} catch (Exception e) {
			log.error(e);
			return false;
		}
		
		return false;
	}

	/* (non-Javadoc)
     * @see com.siliconmtn.security.AbstractLoginModule#authenticate(java.lang.String, java.lang.String)
     * "Login by form (username & password)" method.
     */
    @Override
    public UserDataVO retrieveUserData(String email, String pwd) 
    throws AuthenticationException {
       log.debug("In : public UserDataVO retrieveUserData(String email, String pwd){}");
       if(verifiedConnection()){
    	   UserDataVO user = new UserDataVO();
   			user.setAuthenticationId("allowed");
   			user.setAuthenticated(true);
   			return user;   		
   		}
        return null;
    }
    
   
    /**
     * Retrieves a password based upon the user name.
     */
    public String retrievePassword(String emailAddress)
    throws EmailAddressNotFoundException {
        return null;
    }
    
    public String retrieveAuthIdByCookie(String encProfileId) {
		log.debug("encProfileId=" + encProfileId);
        try {
			encProfileId = URLDecoder.decode(encProfileId, "UTF-8");
			StringEncrypter se = new StringEncrypter(loginApiKey);
			return se.decrypt(encProfileId);
		} catch (Exception e) {
			return null;
		}
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

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#deleteUser(java.lang.String)
	 */
	@Override
	public boolean deleteUser(String authId) {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#retrieveAuthenticationId(java.lang.String)
	 */
	@Override
	public String retrieveAuthenticationId(String userName) {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#authenticate(java.lang.String, java.lang.String)
	 */
	@Override
	public String authenticate(String loginName, String password)
	throws AuthenticationException {
		//this will throw Exceptions long before we risk a null pointer here...
		return this.retrieveUserData(loginName, password).getProfileId();
	}
	
    /* (non-Javadoc)
     * @see com.siliconmtn.security.AbstractLoginModule#authenticateUser(java.lang.String, java.lang.String)
     */
	@Override
	public UserDataVO authenticateUser(String userName, String password)
			throws AuthenticationException {
		return this.retrieveUserData(userName, password);
	}

	@Override
	public String manageUser(String authId, String emailAddress,
			String password, Integer pwdResetFlag) throws InvalidDataException {
		return null;
	}

	@Override
	public boolean resetPassword(String pwd, UserDataVO user) {
		return false;
	}

}
