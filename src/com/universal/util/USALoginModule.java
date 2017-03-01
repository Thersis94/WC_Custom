package com.universal.util;

// JDK 1.6.x
import java.util.Map;
import java.util.Date;

// Dom4j
import org.dom4j.Element;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.security.AbstractLoginModule;
import com.siliconmtn.security.AuthenticationException;
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

	public USALoginModule() {
		super();
	}

	public USALoginModule(Map<String, Object> initVals) {
		super(initVals);
	}

	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#retrieveUserData(java.lang.String, java.lang.String)
	 */
	@Override
	public UserDataVO authenticateUser(String loginName, String password) 
			throws AuthenticationException {
		WebServiceAction wsa = new WebServiceAction(null);
		wsa.setAttributes(getAttributes());
		UserDataVO user = null;
		String siteId = getSiteId();
		Element userElem = null;
		try {
			userElem = wsa.authenticateMember(loginName, password, siteId);
			user = wsa.parseUserData(userElem);
		} catch (Exception e) {
			user = new UserDataVO();
			user.setAuthenticationId(loginName);
		}
		return user;
	}


	/**
	 * Retrieves the login site ID which will be used by the WebServiceAction to determine
	 * which site to authenticate against.
	 * @return
	 */
	private String getSiteId() {
		ActionRequest req = (ActionRequest) getAttribute(GlobalConfig.ACTION_REQUEST);
		if (req != null) {
			SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
			return site.getSiteId();
		} else {
			return null;
		}
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#retrievePasswordAge(java.lang.String)
	 */
	@Override
	public Long retrievePasswordAge(String authenticationId) {
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
	 * @see com.siliconmtn.security.AbstractLoginModule#resetPassword(java.lang.String,java.lang.String)
	 */
	@Override
	public boolean resetPassword(String pwd, UserDataVO user) {
		//not implemented
		return false;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#authenticateUser(java.lang.String)
	 */
	@Override
	public UserDataVO authenticateUser(String encProfileId) throws AuthenticationException {
		//not implemented
		return null;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#saveAuthRecord(java.lang.String, java.lang.String, java.lang.String, java.lang.Integer)
	 */
	@Override
	public String saveAuthRecord(String authId, String username, String password, Integer pwdResetFlag)
			throws InvalidDataException {
		//not implemented
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