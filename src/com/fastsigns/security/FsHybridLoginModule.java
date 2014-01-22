package com.fastsigns.security;

import java.util.Map;

import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.AbstractLoginModule;
import com.siliconmtn.security.AbstractRoleModule;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.EmailAddressNotFoundException;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.common.constants.Constants;

public class FsHybridLoginModule extends AbstractLoginModule {

	private AbstractLoginModule alm = null;
	
	public FsHybridLoginModule() {
		super();
		//super.setUserProfile(true);
	}

    /**
     * @param arg0
     */
    public FsHybridLoginModule(Map<String, Object> arg0) {
        super(arg0);
        initAlm();
    }
	
	private void initAlm() {
		try {
			SMTServletRequest req = (SMTServletRequest)this.initVals.get(AbstractRoleModule.HTTP_REQUEST);
			if(req.hasParameter("type") && req.getParameter("type").equals("ecomm")){
				alm = new FsKeystoneLoginModule(initVals);
				super.setUserProfile(true);
				req.getSession().setAttribute(Constants.USER_DATA, null);
			} else {
				alm = new FsDBLoginModule(initVals);
			}	
		} catch(Exception e) {
			log.debug(e);
		}
	}

	@Override
	public String authenticate(String loginName, String password)
			throws AuthenticationException {
		return alm.authenticate(loginName, password);
	}

	@Override
	public UserDataVO authenticateUser(String userName, String password)
			throws AuthenticationException {
		return alm.authenticateUser(userName, password);
	}

	@Override
	public String retrievePassword(String emailAddress)
			throws EmailAddressNotFoundException {
		return alm.retrievePassword(emailAddress);
	}

	@Override
	public Long retrievePasswordAge(String authenticationId) {
		return alm.retrievePasswordAge(authenticationId);
	}

	@Override
	public String manageUser(String authId, String emailAddress,
			String password, Integer pwdResetFlag) throws InvalidDataException {
		return alm.manageUser(authId, emailAddress, password, pwdResetFlag);
	}

	@Override
	public boolean deleteUser(String authId) {
		return alm.deleteUser(authId);
	}

	@Override
	public String retrieveAuthenticationId(String userName) {
		return alm.retrieveAuthenticationId(userName);
	}

	@Override
	public String retrieveAuthIdByCookie(String profileId) {
		return alm.retrieveAuthIdByCookie(profileId);
	}

	@Override
	public boolean resetPassword(String pwd, UserDataVO user) {
		AbstractLoginModule klm = new FsKeystoneLoginModule();
		klm.init(initVals);
		AbstractLoginModule flm = new FsDBLoginModule();
		flm.init(initVals);
		boolean k = false, f = false;
		UserDataVO kUser = null;
		
		try {
			try {
				k = klm.resetPassword(pwd, user);
			} catch(Exception e) {
				k = false;
				log.debug("Could not log into Keystone.", e);
			}
			try {
				f = flm.resetPassword(pwd, user);
			} catch(Exception e) {
				f = false;
				log.debug("Could not log into Fastsigns.", e);
			}
			SMTServletRequest req = (SMTServletRequest)this.initVals.get(AbstractRoleModule.HTTP_REQUEST);
			String type = (String) req.getSession().getAttribute("isEcommType");
			if(k && type != null && type.equals("ecomm")){
				req.setParameter("type", "ecomm");
				kUser = klm.retrieveUserData(user.getEmailAddress(), pwd);
				req.getSession().setAttribute(Constants.USER_DATA, kUser);
			}
		} catch(Exception e) {
			log.debug("Could not reset password");
			return f;
		}
		return (k || f);
	}
	
	@Override
	public void setInitVals(Map<String, Object> arg0) {
		super.setInitVals(arg0);
		initAlm();
	}
	
	@Override
	public void init(Map<String, Object> initVals) {
		super.init(initVals);
		initAlm();
	}

	@Override
	public UserDataVO retrieveUserData(String encProfileId) 
	throws AuthenticationException {
		return alm.retrieveUserData(encProfileId);
	}
	
	public UserDataVO retrieveUserData(String loginName, String password) throws AuthenticationException {
		return alm.retrieveUserData(loginName, password);
	}
	
	@Override
	public Integer retrievePasswordResetFlag(String authId) 
	throws AuthenticationException {
		return alm.retrievePasswordResetFlag(authId);
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
	 * @param userProfile the userProfile to set
	 */
	public void setUserProfile(Boolean userprofile) {
		this.userProfile = userprofile;
	}

}
