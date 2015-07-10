package com.depuysynthesinst;

import java.util.Map;

import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.security.SAMLLoginModule;

/****************************************************************************
 * <b>Title</b>: DSILoginModule.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 9, 2015
 ****************************************************************************/
public class DSILoginModule extends SAMLLoginModule {

	/**
	 * 
	 */
	public DSILoginModule() {
	}

	/**
	 * @param config
	 */
	public DSILoginModule(Map<String, Object> config) {
		super(config);
	}

	@Override
	public UserDataVO retrieveUserData(String user, String pwd)
			throws AuthenticationException {
		return new DSIUserDataVO(super.retrieveUserData(user, pwd));
	}

}
