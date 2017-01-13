package com.depuysynthes.security;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.siliconmtn.security.UserDataVO;

/****************************************************************************
 * <b>Title</b>: JnJIdentityProviderWithAllResponses.java<p/>
 * <b>Description: enhances the superclass by added ALL SSO response values to the 
 * UserDataVO; not just a specific few.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 29, 2015
 ****************************************************************************/
public class JnJIdentityProviderWithAllResponses extends JnJIdentityParser {

	protected static Logger log; 
	
	public JnJIdentityProviderWithAllResponses() {
		super();
		log = Logger.getLogger(getClass());
	}
	
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.security.saml.SSOParserInterface#mapUserData(java.util.Map)
	 */
	@Override
	public UserDataVO mapUserData(Map<String, List<String>> ssoData) {
		UserDataVO user = super.mapUserData(ssoData);
		
		//remove map values already extracted by the superclass
		ssoData.remove("mail");
		ssoData.remove("givenName");
		ssoData.remove("sn");
		ssoData.remove("co");
		ssoData.remove("employeeType");
		ssoData.remove("title");
		ssoData.remove("wwid");
		
		//iterate the remaining SSO values and set them as attributes on the VO
		for (String key : ssoData.keySet()) {
			String value = getStringValue(key, ssoData);
			if (value == null || value.length() == 0) continue;
			user.addAttribute(key, value);
			log.debug("SSO Response Value: " + key + "=" + value);
		}
		
		return user;
	}

}
