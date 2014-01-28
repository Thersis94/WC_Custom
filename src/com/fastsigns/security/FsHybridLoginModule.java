package com.fastsigns.security;

import java.util.Map;

import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.security.AbstractLoginModule;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.EmailAddressNotFoundException;
import com.siliconmtn.security.UserDataVO;

/***************************************************************************
* <b>Title</b>: FsHybridLoginModule.java<p/>
* <b>Description: </b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2014<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author James McKain
* @version 1.0
* @since Jan 28, 2014
***************************************************************************/
public class FsHybridLoginModule extends AbstractLoginModule {

	/*
	 * enum constants for the different login modules we facade
	 */
	public enum LoginModule {
		Keystone, WebCrescendo
	}

	public FsHybridLoginModule() {
		super();
		super.setUserProfile(Boolean.TRUE);
	}
	
	/**
	 * @param arg0
	 */
	public FsHybridLoginModule(Map<String, Object> initVals) {
		super(initVals);
		super.setUserProfile(Boolean.TRUE);
	}

	
	/**
	 * static class loader method, allows methods to easily switch between 
	 * WC logins and Keystone logins.
	 * @param lm
	 * @return
	 */
	private AbstractLoginModule loadModule(LoginModule lm) {
		switch (lm) {
			case WebCrescendo: 
					return new FsDBLoginModule(initVals);
					
			case Keystone:
			default: 
				return new FsKeystoneLoginModule(initVals);
		}
	}
	
	@Override
	public String authenticate(String loginName, String password)
			throws AuthenticationException {
		try {
			//first try a keystone login
			return loadModule(LoginModule.Keystone).authenticate(loginName, password);
		} catch (AuthenticationException ae) {
			//if login fails, fallback to WC login
			return loadModule(LoginModule.WebCrescendo).authenticate(loginName, password);
		}
	}

	@Override
	public UserDataVO authenticateUser(String userName, String password)
			throws AuthenticationException {
		try {
			//first try a keystone login
			return loadModule(LoginModule.Keystone).authenticateUser(userName, password);
		} catch (AuthenticationException ae) {
			//if login fails, fallback to WC login
			return loadModule(LoginModule.WebCrescendo).authenticateUser(userName, password);
		}
	}

	@Override
	public String retrievePassword(String emailAddress)
			throws EmailAddressNotFoundException {
		try {
			//first try a keystone login
			return loadModule(LoginModule.Keystone).retrievePassword(emailAddress);
		} catch (EmailAddressNotFoundException ae) {
			//if login fails, fallback to WC login
			return loadModule(LoginModule.WebCrescendo).retrievePassword(emailAddress);
		}
	}

	@Override
	public Long retrievePasswordAge(String authenticationId) {
		//first try a keystone login
		Long age = loadModule(LoginModule.Keystone).retrievePasswordAge(authenticationId);
		
		//if login fails, fallback to WC login
		if (age == null) 
			age = loadModule(LoginModule.WebCrescendo).retrievePasswordAge(authenticationId);
		
		return age;
	}

	@Override
	public String manageUser(String authId, String emailAddress,
			String password, Integer pwdResetFlag)
			throws InvalidDataException {
		try {
			//first try a keystone login
			return loadModule(LoginModule.Keystone).manageUser(authId, emailAddress, password, pwdResetFlag);
		} catch (InvalidDataException ae) {
			//if login fails, fallback to WC login
			return loadModule(LoginModule.WebCrescendo).manageUser(authId, emailAddress, password, pwdResetFlag);
		}
	}

	@Override
	public boolean deleteUser(String authId) {
		//first try a keystone login
		boolean success =  loadModule(LoginModule.Keystone).deleteUser(authId);
		
		//if login fails, fallback to WC login
		if (!success)
			success = loadModule(LoginModule.WebCrescendo).deleteUser(authId);

		return success;
	}

	@Override
	public String retrieveAuthenticationId(String userName) {
		//first try a keystone login
		String id =  loadModule(LoginModule.Keystone).retrieveAuthenticationId(userName);
		
		//if login fails, fallback to WC login
		if (id == null)
			id = loadModule(LoginModule.WebCrescendo).retrieveAuthenticationId(userName);

		return id;
	}

	@Override
	public String retrieveAuthIdByCookie(String profileId) {
		//first try a keystone login
		String id =  loadModule(LoginModule.Keystone).retrieveAuthIdByCookie(profileId);
		
		//if login fails, fallback to WC login
		if (id == null)
			id = loadModule(LoginModule.WebCrescendo).retrieveAuthIdByCookie(profileId);

		return id;
	}

	@Override
	public boolean resetPassword(String pwd, UserDataVO user) {
		//first try a keystone login
		boolean success =  loadModule(LoginModule.Keystone).resetPassword(pwd, user);
		
		//if login fails, fallback to WC login
		if (!success)
			success = loadModule(LoginModule.WebCrescendo).resetPassword(pwd, user);

		return success;
	}

	@Override
	public void setInitVals(Map<String, Object> arg0) {
		super.setInitVals(arg0);
	}

	@Override
	public void init(Map<String, Object> initVals) {
		super.init(initVals);
	}

	@Override
	public UserDataVO retrieveUserData(String encProfileId)
			throws AuthenticationException {
		UserDataVO vo = null;
		try {
			//first try a keystone login
			vo =  loadModule(LoginModule.Keystone).retrieveUserData(encProfileId);
		
		} catch (AuthenticationException e) {
			//if login fails, fallback to WC login
			vo = loadModule(LoginModule.WebCrescendo).retrieveUserData(encProfileId);
		}
		return vo;
	}

	public UserDataVO retrieveUserData(String loginName, String password)
			throws AuthenticationException {
		UserDataVO vo = null;
		try {
			//first try a keystone login
			vo =  loadModule(LoginModule.Keystone).retrieveUserData(loginName, password);
		
		} catch (AuthenticationException e) {
			//if login fails, fallback to WC login
			vo = loadModule(LoginModule.WebCrescendo).retrieveUserData(loginName, password);
		}
		return vo;
	}

	@Override
	public Integer retrievePasswordResetFlag(String authId)
			throws AuthenticationException {
		Integer val = null;
		try {
			//first try a keystone login
			val =  loadModule(LoginModule.Keystone).retrievePasswordResetFlag(authId);
		
		} catch (AuthenticationException e) {
			//if login fails, fallback to WC login
			val= loadModule(LoginModule.WebCrescendo).retrievePasswordResetFlag(authId);
		}
		return val;
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public UserDataVO getUserData() {
		return userData;
	}

	public void setUserData(UserDataVO userData) {
		this.userData = userData;
	}

	public Map<String, Object> getInitVals() {
		return initVals;
	}

	public void addAttribute(String key, Object value) {
		if (initVals != null) {
			initVals.put(key, value);
		}
	}

	/**
	 * @return the userProfile
	 */
	public Boolean hasUserProfile() {
		return userProfile;
	}

	/**
	 * @param userProfile
	 *             the userProfile to set
	 */
	public void setUserProfile(Boolean userprofile) {
		this.userProfile = userprofile;
	}

}
