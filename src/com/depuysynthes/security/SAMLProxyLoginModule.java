package com.depuysynthes.security;

// Java 7
import java.sql.Connection;
import java.util.Calendar;
import java.util.Map;



// SMTBaseLibs 2.0
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

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

			/* parse response and build UserDataVO from response
			 * We try/catch to ensure that we capture any parsing exceptions
			 * so that we can send these back in a custom manner to the legacy
			 * calling site.  */
			UserDataVO baseUser = null;
			try {
				baseUser = parseSSOResponse(req, site, provider);
			} catch (AuthenticationException ae) {
				log.error("Intercepted the parent SSO response parsing exception.");
			}
			
			// build/set sso redirect using the alternate service endpoint URI.
			StringBuilder redir = new StringBuilder(40);
			redir.append(REDIRECT_URI_SRT);
			redir.append("?SAMLResponse=");
			if (baseUser == null || 
					StringUtil.checkVal(baseUser.getAttribute("wwid"),null) == null) {
				redir.append("invalid");
			} else {
				redir.append("valid");
				redir.append("&authToken=").append("wwid");
				redir.append("&authValue=").append(baseUser.getAttribute("wwid"));
				redir.append("&ts=").append(Calendar.getInstance().getTimeInMillis());
			}
			log.debug("parsed proxied response, sso auth redir=" + redir);
			// set redir, throw exception so redirect will be processed
			req.setAttribute(Constants.SSO_AUTH_REDIRECT, redir.toString());
			throw new AuthenticationException(ErrorCodes.SSO_AUTH_REDIRECT);
		} else {
			// if neither case, throw 'invalid login'
			throw new AuthenticationException(ErrorCodes.ERR_INVALID_LOGIN);
		}
		
		return null;

	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractLoginModule#initiateLogin()
	 */
	@Override
	public boolean canInitiateLogin(SMTServletRequest req) throws AuthenticationException {
		//only initiate logic if the session is new.
		//This traps an infinite redirect loop where something goes wrong on WC 
		//but the user successfully authenticates to SSO. (go there, come back, fail, redir to homepage, go there, come back, fail, ...con't.)
		if (!req.getSession().isNew())
			return false;

		//set a parameter to invoke SSO and leverage the superclass implementation
		req.setParameter("initiateSSO", "true");
		return super.canInitiateLogin(req);
	}

}
