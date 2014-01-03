package com.sas.util;

// JDK 1.6.x
import java.util.Map;


// SMT Base Libs
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.security.AbstractLoginModule;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.EmailAddressNotFoundException;
import com.siliconmtn.security.UserDataVO;

/****************************************************************************
 * <b>Title</b>: SASLoginModule.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Authenticates the user against a web service call
 * to the stacks and stacks site
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Aug 12, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class SASLoginModule extends AbstractLoginModule {

	/**
	 * 
	 */
	public SASLoginModule() {
		this.setUserProfile(true);
	}

	/**
	 * @param initVals
	 */
	public SASLoginModule(Map<String, Object> initVals) {
		super(initVals);
		this.setUserProfile(true);
	}

	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#retrieveUserData(java.lang.String, java.lang.String)
	 */
	public UserDataVO retrieveUserData(String loginName, String password) 
	throws AuthenticationException {
		WebServiceAction wsa = new WebServiceAction(null);
		wsa.setAttributes(initVals);
		UserDataVO user = null;
		try {
			user = wsa.authenticateMember(loginName, password);
		} catch (Exception e) {
			// set login name as authenticationId for logging purposes.
			user = new UserDataVO();
			user.setAuthenticationId(loginName);
		}		
		return user;
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
