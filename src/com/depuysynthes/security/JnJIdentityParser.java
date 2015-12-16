package com.depuysynthes.security;

// JDK 1.7.x
import java.util.List;
import java.util.Map;


// SMT Base Libs
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.security.saml.AbstractSSOParser;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: JnJIdentityParser.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Put Something Here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Mar 3, 2015<p/>
 * @updates:
 ****************************************************************************/
public class JnJIdentityParser extends AbstractSSOParser {

	/**
	 * 
	 */
	public JnJIdentityParser() {
		
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.saml.SSOParserInterface#mapUserData(java.util.Map)
	 */
	@Override
	public UserDataVO mapUserData(Map<String, List<String>> ssoData) {
		UserDataVO user = new UserDataVO();
		
		// Assign the email address
		user.setEmailAddress(this.getStringValue("mail", ssoData));
		
		// Set the user's First Name
		user.setFirstName(this.getStringValue("givenName", ssoData));
		
		// Set the user's Last Name
		user.setLastName(this.getStringValue("sn", ssoData));
		
		// country
		user.setCountryCode(this.getStringValue("co", ssoData));
		
		// employeeType
		user.addAttribute("employeeType", this.getStringValue("employeeType", ssoData));
		
		// title
		String title = StringUtil.checkVal(this.getStringValue("title", ssoData), null);
		if (title != null && ! title.equalsIgnoreCase("notitle")) {
			user.addAttribute("title", this.getStringValue("title", ssoData));
		}
		
		// add world-wide ID (wwid)
		user.addAttribute("wwid", this.getStringValue("wwid", ssoData));
		
		return user;
	}

}
