package com.depuysynthes.huddle;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO.AuthenticationType;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: LoginAction.java<p/>
 * <b>Description: This action is tied to the Layout, at the public role level only.
 * If this module executes that means the user is not logged in; immediately initialte a login
 * request, which is a redirect to SSO.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 20, 2016
 ****************************************************************************/
public class LoginAction extends com.smt.sitebuilder.action.user.LoginAction {

	public LoginAction() {
		super();
	}
	
	public LoginAction(ActionInitVO init) {
		super(init);
	}
	
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		//do not redirect logOff requests
		if (req.hasParameter("logOff")) return;
		
		//capture the destnUrl before we add more params to the request
		if (!req.hasParameter(Constants.SSO_SAML_RESPONSE)) {
			//initiate login, which is a redirect to SSO
			
			//bring the user back to this page, query string and all
			//log.debug("URL=" + req.getRequestURL() + " URI=" + req.getRequestURI());
			//log.debug("QS=" + req.getQueryString());
			String url = req.getRequestURL().toString();
			
			//remove the context path from the URL so we don't get an unnecessary double-redirect from Apache when we go back to the page.
			url = StringUtil.replace(url, "/" + getAttribute(Constants.CONTEXT_NAME), "" + getAttribute(Constants.CONTEXT_PATH));
			
			//append the query string, but not for /qs/ URL structures
			if (req.getQueryString() != null && SMTServletRequest.DIRECTORY_TYPE != req.getReqType()) url += "?" + req.getQueryString();
			
			//set the redirectUrl
			req.setParameter(DESTN_URL, url);
			
			req.setParameter(Constants.SSO_INITIATE, "true");
			req.setParameter(GlobalConfig.KEY_AUTH_TYPE_OTHER, AuthenticationType.SAML.name());
		}
		
		build(req);
	}
}