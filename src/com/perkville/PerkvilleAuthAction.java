package com.perkville;

import com.google.api.client.auth.oauth2.Credential;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> PerkvilleAuthAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages HTTP Redirects for Perkville User Auth.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 28, 2017
 ****************************************************************************/
public class PerkvilleAuthAction extends PerkvilleAction {

	public PerkvilleAuthAction() {
		super();
	}

	public PerkvilleAuthAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void retrieve(ActionRequest req) {
		SiteVO site = ((SiteVO)req.getAttribute(Constants.SITE_DATA));
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);

		PerkvilleApi api = new PerkvilleApi(dbConn, site, user, req.getParameter("code"));

		//Attempt to get Token
		Credential c = api.getToken().getToken();
		if(c == null && (req.hasParameter("initPerkville") || req.hasParameter("code"))) {
			processOAuthCall(api, req);
		} else if(c != null) {
			c = api.refresh();
			setUserAuth(req, c);
			setModuleData(api.getPerks());
			req.getSession().setAttribute("perkvilleAccessToken", c.getAccessToken());
		}
	}

	/**
	 * Method manages the HTTP OAuth Handshakes necessary for retrieving
	 * a token.
	 * @param api
	 * @param req
	 */
	private void processOAuthCall(PerkvilleApi api, ActionRequest req) {
		PerkvilleOAuth2Token t = api.getToken();
		Credential c = null;
		PageVO p = ((PageVO)req.getAttribute(Constants.PAGE_DATA));
		try {

			/*
			 * Step 1.
			 * Initiate request out to Perkville for a temporary Access Code.
			 */
			if (!req.hasParameter("code")) {
				sendRedirect(t.buildAuthReqUrl(), "", req);
			}

			/*
			 * Step 2.
			 * With temporary Access Code, initiate Bearer Request for
			 * permanent Access Token.
			 */
			else if(req.hasParameter("code")) {

				//Initiate Bearer Token Request.
				c = t.getToken(req.getParameter("code"));

				// If not null, we have a Bearer Token.  Update and redirect.
				if(c != null) {

					setUserAuth(req, c);

					//Redirect User back to profile where they initiated the OAuth call.
					sendRedirect(p.getAliasName(), "", req);
				}
			}
		} catch(Exception e) {
			log.error("Problem navigating Perkville OAuth.", e);
		}
	}
}