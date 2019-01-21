package com.biomed.smarttrak.security;

import java.util.List;
import java.util.Map;

import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.security.saml.AbstractSSOParser;

/****************************************************************************
 * <b>Title</b>: OneLoginIdentityParser.java<p/>
 * <b>Description: Parses OneLogin Identity Provider Saml user Response.</b>
 * <b>Copyright:</b> Copyright (c) 2019<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Dec 29, 2015
 ****************************************************************************/
public class OneLoginIdentityParser extends AbstractSSOParser { 
	
	public OneLoginIdentityParser() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.saml.SSOParserInterface#mapUserData(java.util.Map)
	 */
	@Override
	public UserDataVO mapUserData(Map<String, List<String>> ssoData) {
		UserDataVO user = new UserDataVO();

		// Assign the email address
		user.setEmailAddress(this.getStringValue("User.email", ssoData));

		// Set the user's First Name
		user.setFirstName(this.getStringValue("User.FirstName", ssoData));

		// Set the user's Last Name
		user.setLastName(this.getStringValue("User.LastName", ssoData));

		return user;
	}
}