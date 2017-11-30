package com.perkville;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.api.client.auth.oauth2.Credential;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.BaseOAuth2Token.Config;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.oauth.OAuth2TokenViaDB;

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

	public enum Scope {
		PUBLIC,						//Grants read-only access to public information
		USER_CUSTOMER_INFO,			//Grants read-only access to user's contact information, business connections, and vouchers. Grants ability to mark a voucher as "Used".
		USER_REDEEM,					//Grants ability to redeem a user's earned points
		USER_REFERRAL,				//Grants ability to create a referral to a users connected businesses
		ADMIN_CUSTOMER_INFO,			//Grants read-only access to a user's customer's contact information and business connections.
		ADMIN_CUSTOMER_REDEEM,		//Grants read access to a user's customer's vouchers. Grants ability to mark customer's vouchers as "Used". Grants ability to redeem points on behalf of customers.
		ADMIN_CUSTOMER_GRANT_POINTS,	//Grants ability to grant points to a user's customers and invite new customers to Perkville.
		ADMIN_CUSTOMER_REFERRAL,		//Grants ability to create a referral on behalf of two users.
		ADMIN_PERK,					//Grants ability to create and edit perks within a business' rewards program.
		ADMIN_LOCATION				//Grants ability to create and edit a business's locations.
	}

	public enum PVSiteConfig {
		PV_API_KEY,
		PV_API_SECRET,
		PV_TOKEN_CALLBACK_URL,
		PV_TOKEN_SERVER_URL,
		PV_AUTH_SERVER_URL,
		PV_KEYSTORE,
		PV_STATE,
		PV_SCOPE,
		PV_AUTH_CODE_RESPONSE,
		PV_GRANT_TYPE_CODE
	}

	public PerkvilleAuthAction() {
		super();
	}

	public PerkvilleAuthAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void retrieve(ActionRequest req) {
		if(req.hasParameter("initPerkville") || req.hasParameter("code")) {
			processOAuthCall(req);
		}
	}

	private void processOAuthCall(ActionRequest req) {
		Map<String, String> siteConfig = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getSiteConfig();
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		Credential c = null;

		try {

			//Initialize Token.
			PerkvilleOAuth2Token token = new PerkvilleOAuth2Token(buildConfigMap(siteConfig, user), buildScopes(siteConfig), buildOAuthAttributes(req, siteConfig));

			//Attempt to get Token
			c = token.getToken();

			//If Null and on Step 1, redirect User to to Perkville Auth url 
			if (c == null && !req.hasParameter("code")) {
				sendRedirect(token.buildAuthReqUrl(), "", req);
			}

			//If Null and on Step 2, get the code param off the req and request Token.
			else if(c == null && req.hasParameter("code")) {
				// receive authorization code and exchange it for an access token
				c = token.getToken(req.getParameter("code"));

				//Testing hook for perkville binding successfully.
				if(c != null) {
					req.setAttribute("perkvilleLoaded", true);
				}
			}

			//Refresh Token.
			else {
				log.debug("refreshing access token" + req.getParameter("access_token"));
				//c = token.getFlow().createAndStoreCredential(token.requestRefreshToken(c), config.get(Config.PV_CLIENT_ID));
			}

		} catch(Exception e) {
			log.error("Problem navigating Perkville OAuth.", e);
		}
	}

	/**
	 * Build the Attributes map for the PerkvilleOAuth2Token.
	 * @param req 
	 * @param siteConfig
	 * @return
	 */
	private Map<String, Object> buildOAuthAttributes(ActionRequest req, Map<String, String> siteConfig) {
		Map<String, Object> attr = new HashMap<>();
		attr.put(OAuth2TokenViaDB.DB_CONN_KEY, dbConn);
		attr.put(PerkvilleOAuth2Token.RESPONSE_TYPE_PARAM, siteConfig.get(PVSiteConfig.PV_AUTH_CODE_RESPONSE.name()));
		attr.put(PerkvilleOAuth2Token.CODE_PARAM, req.getParameter(PerkvilleOAuth2Token.CODE_PARAM));
		return attr;
	}

	/**
	 * Build List of Scopes for PerkvilleOAuth2Token.
	 * @param siteConfig
	 * @return
	 */
	private List<String> buildScopes(Map<String, String> siteConfig) {
		List<String> scopes = new ArrayList<>();
		if(siteConfig.containsKey(PVSiteConfig.PV_SCOPE.name())) {
			String [] strArr = StringUtil.checkVal(siteConfig.get(PVSiteConfig.PV_SCOPE.name())).split("\\+");
			for(String s : strArr) {
				scopes.add(Scope.valueOf(s).name());
			}
		} else {
			scopes.add(Scope.PUBLIC.name());
		}

		return scopes;
	}

	/**
	 * Build the Config map for the PerkvilleOAuth2Token
	 * @param req
	 * @return
	 */
	private Map<Config, String> buildConfigMap(Map<String, String> siteConfig, UserDataVO user) {

		Map<Config, String> configMap = new EnumMap<>(Config.class);
		configMap.put(Config.USER_ID, user.getProfileId());
		configMap.put(Config.API_KEY, siteConfig.get(PVSiteConfig.PV_API_KEY.name()));
		configMap.put(Config.API_SECRET, siteConfig.get(PVSiteConfig.PV_API_SECRET.name())); 
		configMap.put(Config.TOKEN_CALLBACK_URL, siteConfig.get(PVSiteConfig.PV_TOKEN_CALLBACK_URL.name()));
		configMap.put(Config.TOKEN_SERVER_URL, siteConfig.get(PVSiteConfig.PV_TOKEN_SERVER_URL.name()));
		configMap.put(Config.AUTH_SERVER_URL, siteConfig.get(PVSiteConfig.PV_AUTH_SERVER_URL.name()));
		configMap.put(Config.KEYSTORE, PerkvilleOAuth2Token.PERKVILLE_KEYSTORE);
		configMap.put(Config.GRANT_TYPE, siteConfig.get(PVSiteConfig.PV_GRANT_TYPE_CODE.name()));
		return configMap;
	}
}
