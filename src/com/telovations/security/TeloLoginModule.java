package com.telovations.security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.security.AbstractLoginModule;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.EmailAddressNotFoundException;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.UserDataVO;
import com.telovations.action.WebServiceAction;

/****************************************************************************
 * <b>Title</b>: TeloLoginModule.java <p/>
 * <b>Project</b>: WC_MISC <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jul 15, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class TeloLoginModule extends AbstractLoginModule {
	
	/**
	 * 
	 */
	public TeloLoginModule() {
		this.setUserProfile(true);
	}

	/**
	 * @param initVals
	 */
	public TeloLoginModule(Map<String, Object> initVals) {
		super(initVals);
		this.setUserProfile(true);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#retrieveUserData(java.lang.String, java.lang.String)
	 */
	public UserDataVO retrieveUserData(String loginName, String password) 
			throws AuthenticationException {
		String url = WebServiceAction.BASE_URL;
		url += loginName + "/profile";
		UserDataVO user = new UserDataVO();
		
		try {
			byte[] b = this.getProfileInfo(loginName, password, url);
			if (b == null) throw new AuthenticationException("Invalid User name/password");
			user = this.parseProfile(new String(b), password);
		} catch(Exception e) {
			user.setAuthenticationId(loginName);
		}
		
		return user;
	}
	
	
	/**
	 * Converts the XML into a USer Data VO object
	 * @param xml
	 * @return
	 * @throws DocumentException 
	 */
	protected UserDataVO parseProfile(String xmlData, String pwd) throws DocumentException {
		Document doc = null;
		Element e = null;
		UserDataVO user = new UserDataVO();
		log.debug("XML Data: " + xmlData);
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(xmlData.toString().getBytes("UTF-8"));
			SAXReader reader = new SAXReader();
			doc = reader.read(bais);
			Element root = doc.getRootElement();
			e = root.element("details");
		} catch (Exception ex) {
			throw new DocumentException(ex);
		}
		
		user.setFirstName(e.elementText("firstName"));
		user.setLastName(e.elementText("lastName"));
		user.addPhone(new PhoneVO("WORK", e.elementText("phoneNumber"), "US"));
		user.setEmailAddress(e.elementText("emailAddress"));
		user.setProfileId(e.elementText("userId"));
		user.setAliasName(e.elementText("groupId"));
		user.setAuthenticationId(user.getProfileId());
		if (user.getProfileId() != null) user.setAuthenticated(true);
		user.setPassword(pwd);
		
		log.debug("User Data: " + user);
		
		return user;
	}
	
	/**
	 * Calls the Telovations web service to get the user profile
	 * @param user
	 * @param pass
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public byte[] getProfileInfo(String user, String pass, String url) throws IOException {
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		byte[] data = conn.basicAuthLogin(user, pass, url, null);
		
		return data;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#authenticate(java.lang.String, java.lang.String)
	 */
	@Override
	public String authenticate(String loginName, String password)
	throws AuthenticationException {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#authenticateUser(java.lang.String, java.lang.String)
	 */
	@Override
	public UserDataVO authenticateUser(String loginName, String password)
	throws AuthenticationException {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#retrievePassword(java.lang.String)
	 */
	@Override
	public String retrievePassword(String emailAddress)
	throws EmailAddressNotFoundException {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#retrievePasswordAge(java.lang.String)
	 */
	@Override
	public Long retrievePasswordAge(String authenticationId) {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#manageUser(java.lang.String, java.lang.String, java.lang.String, java.lang.Integer)
	 */
	@Override
	public String manageUser(String authId, String emailAddress, String password, Integer pwdResetFlag) 
	throws InvalidDataException {
		return null;
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
	 * @see com.siliconmtn.security.AbstractLoginModule#retrieveAuthIdByCookie(java.lang.String)
	 */
	@Override
	public String retrieveAuthIdByCookie(String profileId) {
		return null;
	}

	@Override
	public boolean resetPassword(String pwd, UserDataVO user) {
		return false;
	}

	@Override
	public UserDataVO retrieveUserData(String encProfileId)
			throws AuthenticationException {
		return null;
	}

}
