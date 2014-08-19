package com.fastsigns.security;

// JDK 1.6.x
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;




// SMT Base Libs
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.AESEncryption;
import com.siliconmtn.security.AESKey;
import com.siliconmtn.security.AbstractLoginModule;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.EmailAddressNotFoundException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/*****************************************************************************
 * <b>Title</b>: FsHelpSiteLoginModule.java</p>
 * <b>Description: </b> Logs the user into WC using credentials passed from support.fastsigns.com
 * <p>Copyright: Copyright (c) 2000 - 2014 SMT, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * 
 * @author James McKain
 * @version 1.0
 * @since Feb 13, 2014
 ***************************************************************************/

public class FsHelpSiteLoginModule extends AbstractLoginModule {
	
	private final static String SHARED_SECRET = "EF3478A623204C86A6DF";
	private final static String TOKEN_NM = "sharedToken";
	private final AESKey  aesKey;
	private final long DATE_THRES_MILLIS = 60000;

	public FsHelpSiteLoginModule() {
		super();
		super.setUserProfile(true);

		//these hard-coded values were predefined in conjuction with FS.  Do not change!
		aesKey = new AESKey();
		aesKey.setPassPhrase("ae790270-42d1-4663-adf8-46d5980c71c1");
		aesKey.setSaltValue("5cd444b2-48e6-4a0b-aa7f-766e8d6de614");
		aesKey.setVector("3144f1059fac47f5");
	}

	/**
	 * @param arg0
	 */
	public FsHelpSiteLoginModule(Map<String, Object> arg0) {
		this();
		super.setInitVals(arg0);
	}
	
	/**
	 * main method, for testing purposes only.
	 * @param args
	 */
	public static void main(String[] args) {
		FsHelpSiteLoginModule lm = new FsHelpSiteLoginModule();
		org.apache.log4j.PropertyConfigurator.configure("/data/log4j.properties");
		String[] tokenArr = new String[2];
		try {
			Calendar dt = Calendar.getInstance();
			dt.add(Calendar.HOUR_OF_DAY, 5); //offset from Indiana to GMT
			
			String phonyPayload = AESEncryption.encryptString(TOKEN_NM + "=" + SHARED_SECRET + "|date=" + Convert.formatDate(dt.getTime(), Convert.DATE_TIME_DASH_PATTERN), lm.aesKey, 256);
			phonyPayload = StringEncoder.urlEncode(phonyPayload);
			phonyPayload = "password=pass&emailAddress=" + phonyPayload;
			System.out.println("usable auth token: " + phonyPayload);
			tokenArr = lm.dissectTokenFromString(phonyPayload);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			boolean passed = lm.verifiedConnection(tokenArr);
			System.out.println("Login passed?" + passed);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.siliconmtn.security.AbstractLoginModule#authenticate(java.lang.String
	 * , java.lang.String) "Login by cookie" method
	 */
	@Override
	public UserDataVO retrieveUserData(String encProfileId) throws AuthenticationException {
		throw new AuthenticationException("not supported");
	}

	private boolean verifiedConnection(String[] tokenArr) throws AuthenticationException {
		log.debug("Verifying Login");
		String passPhrase = "";
		Date peerDate = null;

		//quick sanity check, we can't proceed successfully without 2 args
		if (tokenArr == null || tokenArr.length < 2) 
			throw new AuthenticationException("missing tokens");
		
		for (int i = 0; i < tokenArr.length; i++) {
			if (tokenArr[i] == null) continue;
			log.debug("arr[x]: " + tokenArr[i]);
			
			if (tokenArr[i].indexOf(TOKEN_NM) > -1) {
				passPhrase = tokenArr[i].split("=")[1];
				log.debug("found passPhrase: " + passPhrase);
			} else {
				String temp = tokenArr[i].split("=")[1]; //"date="
				try {
					//convert the date from UTC time zone, to our time zone
					SimpleDateFormat isoFormat = new SimpleDateFormat(Convert.DATE_TIME_DASH_PATTERN);
					isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
					peerDate = isoFormat.parse(temp);
					log.debug("peerDate: " + peerDate);
				} catch (Exception e) { 
					e.printStackTrace();
				}
			}
		}
		
		//fail-fast if we're missing critical info 
		if (peerDate == null || passPhrase == null) {
			log.error("tokenArr=" + StringUtil.getToString(tokenArr, false, false, ","));
			throw new AuthenticationException("missing payload data");
		}
		
		//test the secret first; this is easier than a Date comparison.
		if (!passPhrase.equals(SHARED_SECRET)) {
			log.error("shared secrets do not match, user provided: " + passPhrase);
			throw new AuthenticationException("secrets don't match");
		}
		
		//if the passed time is more than a minute old, fail the login 
		if (peerDate.getTime() < (System.currentTimeMillis() - DATE_THRES_MILLIS)) {
			log.error("login expired by date, user said: " + peerDate.getTime() + ", we're at: " + (System.currentTimeMillis()-DATE_THRES_MILLIS));
			throw new AuthenticationException("date mismatch");
		}
		
		//all tests passed, the user is logged in
		return true;
	}
	
	
	/**
	 * takes the incoming string and goes 4 things:
	 * 1) isolate it from other query string params
	 * 2) URLDecode it
	 * 3) Decrypt it (AES)
	 * 4) Split the decrypted token into a String[] of passed args
	 * @param token
	 * @return String[]
	 */
	private String[] dissectTokenFromString(String token) {
		String[] tokenArr = new String[2];
		
		//retrieve the token from the query string - we must parse it out because of possible ampersands in the token
		token = token.replaceAll("&?password=pass&?", "");
		log.debug("token with no password= " + token);
		token = token.replace("emailAddress=", "");
		log.debug("token ready for decoding " + token);
		
		try {
			token = URLDecoder.decode(token, "UTF-8");
			log.debug("URLdecoded String: " + token);
		} catch (Exception e) {
			//EncodingNotSupportedException
			log.warn("could not decode token", e);
		}
		
		
		try {
			log.debug("token=" + token);
			log.debug(aesKey.getPassPhrase());
			token = AESEncryption.decryptString(token, aesKey, 256);
			log.debug("Decrypted value: " + token);
		} catch (Exception e) {
			//token not decryptable
			log.error("passed token not decryptable", e);
			return tokenArr;
		}
		
		//if we've made it this far, split the token into separate arguments 
		tokenArr = token.split("\\|");
		
		return tokenArr;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.siliconmtn.security.AbstractLoginModule#authenticate(java.lang.String
	 * , java.lang.String) "Login by form (username & password)" method.
	 */
	@Override
	public UserDataVO retrieveUserData(String email, String pwd)
			throws AuthenticationException {
		SMTServletRequest req = (SMTServletRequest) this.getInitVals().get(GlobalConfig.HTTP_REQUEST);
		String[] tokenArr = dissectTokenFromString(req.getQueryString());
		
		if (verifiedConnection(tokenArr)) {
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
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.siliconmtn.security.AbstractLoginModule#retrievePasswordAge(java
	 * .lang.String)
	 */
	@Override
	public Long retrievePasswordAge(String authenticationId) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.siliconmtn.security.AbstractLoginModule#deleteUser(java.lang.String)
	 */
	@Override
	public boolean deleteUser(String authId) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.siliconmtn.security.AbstractLoginModule#retrieveAuthenticationId
	 * (java.lang.String)
	 */
	@Override
	public String retrieveAuthenticationId(String userName) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.siliconmtn.security.AbstractLoginModule#authenticate(java.lang.String
	 * , java.lang.String)
	 */
	@Override
	public String authenticate(String loginName, String password)
			throws AuthenticationException {
		throw new AuthenticationException("not suported");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.siliconmtn.security.AbstractLoginModule#authenticateUser(java.lang
	 * .String, java.lang.String)
	 */
	@Override
	public UserDataVO authenticateUser(String userName, String password)
			throws AuthenticationException {
		throw new AuthenticationException("not suported");
	}

	@Override
	public String manageUser(String authId, String emailAddress,
			String password, Integer pwdResetFlag)
			throws InvalidDataException {
		return null;
	}

	@Override
	public boolean resetPassword(String pwd, UserDataVO user) {
		return false;
	}

}
