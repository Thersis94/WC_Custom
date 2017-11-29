package com.perkville;

import java.util.EnumMap;
import java.util.Map;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.perkville.PerkvilleOAuth2Token.Config;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
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
public class PerkvilleAuthAction extends SimpleActionAdapter {

	public PerkvilleAuthAction() {
		super();
	}

	public PerkvilleAuthAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void retrieve(ActionRequest req) {

		//Build map of config information.
		Map<Config, String> config = buildConfigMap(req);
		Credential c = null;

		try {

			//Initialize Token.
			PerkvilleOAuth2Token token = new PerkvilleOAuth2Token(config);

			//Attempt to get Token
			c = token.getToken();

			//If Null and on Step 1, redirect User to to Perkville Auth url 
			if (c == null && !req.hasParameter("code")) {
				log.debug("creating access token");
				//we need to pause here and prompt the user to go get us an access token.  An interactive web browser is required to do this.
				AuthorizationCodeRequestUrl u = token.getFlow().newAuthorizationUrl();
				u.setRedirectUri(config.get(Config.PV_TOKEN_CALLBACK_URL));
				sendRedirect(u.toURL().toString(), "", req);
			}

			//If Null and on Step 2, get the code param off the req and request Token.
			else if(c == null && req.hasParameter("code")) {
				// receive authorization code and exchange it for an access token
				c = token.processResponse(req.getParameter("code"));
			}

			//Refresh Token.
			else {
				log.debug("refreshing access token");
				c = token.getFlow().createAndStoreCredential(token.requestRefreshToken(c), config.get(Config.PV_CLIENT_ID));
			}

		} catch(Exception e) {
			log.error("Problem navigating Perkville OAuth.", e);
		}

		//If we have a Credential Object, Persist the Credentials Access Token.
		if(c != null) {
			persistCredentials(c.getAccessToken());
		}
	}

	/**
	 * Store Access Token long term.  Probably in Database.
	 * @param c
	 */
	private void persistCredentials(String token) {
		log.debug("HAVE TOKEN: " + token);
		//TODO - Discuss how/where to store Access Token.
	}

	/**
	 * @param req
	 * @return
	 */
	private Map<Config, String> buildConfigMap(ActionRequest req) {
		Map<String, String> siteConfig = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getSiteConfig();
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);

		Map<Config, String> configMap = new EnumMap<>(Config.class);
		configMap.put(Config.PV_CLIENT_ID, user.getProfileId());
		configMap.put(Config.PV_API_KEY, siteConfig.get(Config.PV_API_KEY.name()));
		configMap.put(Config.PV_API_SECRET, siteConfig.get(Config.PV_API_SECRET.name())); 
		configMap.put(Config.PV_TOKEN_CALLBACK_URL, siteConfig.get(Config.PV_TOKEN_CALLBACK_URL.name()));
		configMap.put(Config.PV_TOKEN_SERVER_URL, siteConfig.get(Config.PV_TOKEN_SERVER_URL.name()));
		configMap.put(Config.PV_AUTH_SERVER_URL, siteConfig.get(Config.PV_AUTH_SERVER_URL.name()));
		configMap.put(Config.PV_KEYSTORE, "perkville-" + StringUtil.removeNonAlphaNumeric(siteConfig.get(Config.PV_API_KEY.name())));
		configMap.put(Config.PV_STATE, siteConfig.get(Config.PV_STATE.name()));
		configMap.put(Config.PV_SCOPE, siteConfig.get(Config.PV_SCOPE.name()));
		configMap.put(Config.PV_AUTH_CODE_RESPONSE, siteConfig.get(Config.PV_AUTH_CODE_RESPONSE.name()));
		configMap.put(Config.PV_GRANT_TYPE_CODE, siteConfig.get(Config.PV_GRANT_TYPE_CODE.name()));
		return configMap;
	}
}
