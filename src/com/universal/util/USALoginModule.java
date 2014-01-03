package com.universal.util;

// JDK 1.6.x
import java.util.Map;


// Dom4j
import org.dom4j.Element;


// SMT Base Libs
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.AbstractLoginModule;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.EmailAddressNotFoundException;
import com.siliconmtn.security.UserDataVO;

// WC libs
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: USALoginModule.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Authenticates the user against a web service call
 * to the USA Signals site
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author billy
 * @version 1.0
 * @since Jan 26, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class USALoginModule extends AbstractLoginModule {

	/**
	 * 
	 */
	public USALoginModule() {
		this.setUserProfile(true);
	}

	/**
	 * @param initVals
	 */
	public USALoginModule(Map<String, Object> initVals) {
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
		String catalogSiteId = this.retrieveLoginSiteId();
		Element userElem = null;
		try {
			userElem = wsa.authenticateMember(loginName, password, catalogSiteId);
			user = wsa.parseUserData(userElem);
		} catch (Exception e) {
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
	
	/**
	 * Retrieves the login site ID which will be used by the WebServiceAction to determine
	 * which site to authenticate against.
	 * @return
	 */
	private String retrieveLoginSiteId() {
		String catalogSiteId = null;
		SMTServletRequest req = (SMTServletRequest) initVals.get(GlobalConfig.HTTP_REQUEST);
		if (req != null) {
			SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
			catalogSiteId = site.getSiteId();
		}
		log.debug("login module catalogSiteId: " + catalogSiteId);
		return catalogSiteId;
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
