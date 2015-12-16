package com.depuysynthes.security;

// Java 7
import java.sql.Connection;
import java.util.Map;

// SMTBaseLibs 2.0
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;

// WebCrescendo 2.0
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.common.constants.ErrorCodes;
import com.smt.sitebuilder.security.SAMLLoginModule;
import com.smt.sitebuilder.security.SSOProviderVO;

/****************************************************************************
 * <b>Title: </b>SAMLProxyLoginModule.java <p/>
 * <b>Project: </b>WC_Customo <p/> Custom login module to allow legacy Depuy
 * site(s) to utilize SSO-based authentication using SAML. The placeholder WC site
 * that proxies the SSO config for a legacy site must have the same domain name
 * as the legacy site. 
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2015<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since Dec 15, 2015<p/>
 *<b>Changes: </b>
 * Dec 15, 2015: David Bargerhuff: Created class.
 ****************************************************************************/
public class SAMLProxyLoginModule extends SAMLLoginModule {

	private final String REDIRECT_URI_SRT = "/srt/sso.asp";
	
	public SAMLProxyLoginModule() {
	}

	/**
	 * @param config
	 */
	public SAMLProxyLoginModule(Map<String, Object> config) {
		super(config);
	}
	
	/**
	 * Checks for SAML auth initiation or SAML response.  Defaults to WC login
	 * authentication.
	 */
	@Override
	public UserDataVO retrieveUserData(String user, String pwd) 
			throws AuthenticationException {

		SMTServletRequest req = (SMTServletRequest)initVals.get(GlobalConfig.HTTP_REQUEST);
		Connection conn = (Connection) initVals.get(GlobalConfig.KEY_DB_CONN);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);

		if (req.hasParameter("initiateSSO")) {
			// behave the same as superclass
			super.retrieveUserData(user, pwd);
			
		} else if (req.hasParameter("SAMLResponse")) {
			/* process the response, redirect tokens calling site.  
			 * WC does not consume this response. */
		
			// retrieve provider info
			SSOProviderVO provider = retrieveProviderData(req, conn, site);

			// parse response and  build UserDataVO from response
			UserDataVO baseUser = parseSSOResponse(req, site, provider);
			
			// build/set sso redirect using the alternate service endpoint URI.
			StringBuilder redir = new StringBuilder(40);
			redir.append(REDIRECT_URI_SRT);
			redir.append("?SAMLResponse=");
			if (baseUser.getAttribute("authToken") == null) {
				redir.append("invalid");
			} else {
				redir.append("valid");
				redir.append("&authToken=").append(baseUser.getAttribute("authToken"));
				redir.append("&authValue=").append(baseUser.getAttribute("authValue"));
			}
			log.debug("parsed proxied response, sso auth redir=" + redir);
			// throw exception so that the redirect we built will be executed
			throw new AuthenticationException(ErrorCodes.SSO_AUTH_REDIRECT);
		}
		
		return new UserDataVO();

	}	

}
