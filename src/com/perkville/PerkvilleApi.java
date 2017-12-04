package com.perkville;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.google.api.client.auth.oauth2.Credential;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.perkville.vo.ConnectionVO;
import com.perkville.vo.PerkVO;
import com.perkville.vo.PerkvilleVO;
import com.perkville.vo.UserVO;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.security.BaseOAuth2Token.Config;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.json.GSONDateDeserializer;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.security.oauth.OAuth2TokenViaDB;


/****************************************************************************
 * <b>Title:</b> PerkvilleAPI.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages Interactions on the Perkville API.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 30, 2017
 ****************************************************************************/
public class PerkvilleApi {
	private Logger log;
	private PerkvilleOAuth2Token authToken;

	//Perkville Api URL
	private static final String API_URL = "https://api.perkville.com";

	//Map or available Perkville Endpoints.
	protected static final Map<String, String> ENDPOINTS;

	//Holds name for finding AccessToken on Session.
	public static final String ACCESS_TOKEN = "perkvilleAccessToken";

	private boolean isValidToken;
	//Perkville OAuth Scopes.
	private enum Scope {
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

	//TODO - Find a way to codify this config outside of an enum.
	private enum PVSiteConfig {
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

	/**
	 * populates the action map when the static constructor is called.  This will make our map live once in the JVM
	 */
	static {
		ENDPOINTS = new HashMap<>();
		ENDPOINTS.put("connections", "/v2/connections/");
		ENDPOINTS.put("businesses", "/v2/businesses/");
		ENDPOINTS.put("transactions", "/v2/transactions/");
		ENDPOINTS.put("perks", "/v2/perks/");	
	}

	/**
	 * Default Constructor that builds the authToken object and attempts
	 * to validate it.
	 *
	 * @param conn
	 * @param site
	 * @param user
	 * @param code
	 */
	public PerkvilleApi(Connection conn, SiteVO site, UserDataVO user, String code) {
		this.authToken = buildToken(site, user, conn, code);
		this.log = Logger.getLogger(getClass());
		this.isValidToken = isValidToken();
	}

	/**
	 * Return PerkvilleOAuth2Token.
	 * @return
	 */
	public PerkvilleOAuth2Token getAuthToken() {
		return authToken;
	}

	/**
	 * Perform lookup against Perkville for available Perks.
	 * @return
	 */
	public PerkvilleVO<PerkVO> getPerks() {
		PerkvilleVO<PerkVO> perks = new PerkvilleVO<>();
		if(isValidToken) {
			String perkData = performCall(buildUrl(ENDPOINTS.get("perks")));
			perks = convertData(perkData, new TypeToken<PerkvilleVO<PerkVO>>() {}.getType());
		}

		return perks;
	}

	/**
	 * Deserializes jsonData down to an Object of given Type.
	 * @param perkData
	 * @param perkVO
	 * @return
	 */
	private <T extends Object> T convertData(String jsonData, Type type) {
		GSONDateDeserializer dateParser = new GSONDateDeserializer(Arrays.asList(Convert.DATE_DASH_PATTERN, Convert.DATE_TIME_DASH_PATTERN));
		Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, dateParser).create();
		return gson.fromJson(jsonData, type);
	}

	/**
	 * Perform lookup against Perkville for a users points at a given location.
	 * @param emailAddress
	 * @return
	 */
	public int getPoints(String locationId) {
		int points = 0;

		if(isValidToken) {
			PerkvilleVO<ConnectionVO> conData = performConnectionLookup(null);
			if(conData != null && conData.getObjects() != null && !conData.getObjects().isEmpty()) {
				points = getLocationPoints(locationId, conData);
			}
		}

		return points;
	}

	/**
	 * Process Connections and return the points.
	 * @param locationId
	 * @param conData
	 * @return
	 */
	private int getLocationPoints(String locationId, PerkvilleVO<ConnectionVO> conData) {
		int points = 0;
		if(!StringUtil.isEmpty(locationId)) {
			for(ConnectionVO c : conData.getObjects()) {
				if(c.getHomeLocation().equals(locationId)) {
					points = c.getPointBalance();
				}
			}
		} else {
			points = conData.getObjects().get(0).getPointBalance();
		}

		return points;
	}

	/**
	 * Search for Connections with
	 * @param user_id
	 * @return
	 */
	public PerkvilleVO<ConnectionVO> performConnectionLookup(String connectionId) {
		PerkvilleVO<ConnectionVO> conData = new PerkvilleVO<>();
		if(isValidToken) {
			String connData = performCall(buildUrl(ENDPOINTS.get("connections"), connectionId)); 
			conData = convertData(connData, new TypeToken<PerkvilleVO<ConnectionVO>>() {}.getType());
		}
		return conData;
	}

	/**
	 * Search for Perkville User with given emailAddress.
	 * @param emailAddress
	 * @param paramMap 
	 * @return
	 */
	public PerkvilleVO<UserVO> performUserLookup(String emailAddress) {
		PerkvilleVO<UserVO> users = new PerkvilleVO<>();

		if(isValidToken) {
			Map<String, String> paramMap = new HashMap<>();
			if(!StringUtil.isEmpty(emailAddress)) {
				paramMap.put("emails__email", emailAddress);
			}

			String userData = performCall(buildUrl(ENDPOINTS.get("users"), paramMap));
			users = convertData(userData, new TypeToken<PerkvilleVO<UserVO>>() {}.getType());
		}
		return users;
	}

	/**
	 * Helper method for endpoints that don't require a search param.
	 * @param endpoint
	 * @return
	 */
	private String buildUrl(String endpoint) {
		return buildUrl(endpoint, null, null);
	}

	/**
	 * Helper method for the buildUrl Method that performs a Search.
	 * @param endpoint
	 * @param paramMap
	 * @return
	 */
	private String buildUrl(String endpoint, Map<String, String> paramMap) {
		return buildUrl(endpoint, null, paramMap);
	}

	/**
	 * Helper method for the buildUrl method that performs a lookup with givenId.
	 * @param endpoint
	 * @param endpointId
	 * @return
	 */
	private String buildUrl(String endpoint, String endpointId) {
		return buildUrl(endpoint, endpointId, null);
	}

	/**
	 * Manages building the URL for Perkville Endpoints.
	 * @param paramMap 
	 * @param string
	 * @return
	 */
	private String buildUrl(String endpoint, String endpointId, Map<String, String> paramMap) {
		StringBuilder url = new StringBuilder();
		url.append(API_URL).append(endpoint);

		//Set the EndpointId if given.
		if(!StringUtil.isEmpty(endpointId)) {
			url.append(endpointId).append("/");
		}

		//Add JSON Format param.
		url.append("?format=json");

		//Add Additional params if given.
		if(paramMap != null) {
			for(Entry<String, String> e : paramMap.entrySet()) {
				url.append(e.getKey()).append("=").append(e.getValue());
			}
		}

		return url.toString();
	}

	/**
	 * Perform the actual call to Perkville.
	 * @param url
	 * @return
	 */
	protected String performCall(String url) {
		SMTHttpConnectionManager c = new SMTHttpConnectionManager();

		/*
		 * Verify that we have a valid token for making the call.  An invalid
		 * access Token will cause problems.
		 */
		if(isValidToken) {
			//Add Header for the Authorization Bearer Token.
			String accessToken = authToken.getToken().getAccessToken();
			c.addRequestHeader("Authorization", "Bearer " + accessToken);

			//Add Header for User Agent.  Required to get through CloudFlare
			c.addRequestHeader("User-Agent", "WebCrescendoConnectionManager/3.3");

			//Perform actual Call.
			byte[] response = null;
			try {
				response = c.retrieveData(url);
			} catch (IOException e) {
				log.error("Error Connecting to Perkville Endpoint.", e);
			}

			//Return Response as String.
			return new String(response);
		} else {
			return "";
		}
	}

	/**
	 * Refresh the Token.
	 * @return
	 */
	public Credential refresh() {
		Credential c = authToken.getToken();
		log.debug("refreshing access token: " + c.getAccessToken());
		try {
			return authToken.refreshToken(c);
		} catch (IOException e) {
			log.error("Error Processing Code", e);
		}

		return c;
	}

	/**
	 * Helper method builds the PerkvilleOAuth2Token
	 * @param site
	 * @param user
	 * @param conn
	 * @param code
	 * @return
	 */
	private PerkvilleOAuth2Token buildToken(SiteVO site, UserDataVO user, Connection conn, String code) {
		Map<String, String> siteConfig = site.getSiteConfig();

		//Initialize Token.
		try {
			return new PerkvilleOAuth2Token(buildConfigMap(siteConfig, user), buildScopes(siteConfig), buildOAuthAttributes(conn, code, siteConfig));
		} catch (IOException e) {
			log.error("Error Processing Code", e);
		}

		return null;
	}

	/**
	 * Build the Attributes map for the PerkvilleOAuth2Token.
	 * @param req 
	 * @param siteConfig
	 * @return
	 */
	private Map<String, Object> buildOAuthAttributes(Connection conn, String code, Map<String, String> siteConfig) {
		Map<String, Object> attr = new HashMap<>();
		attr.put(OAuth2TokenViaDB.DB_CONN_KEY, conn);
		attr.put(PerkvilleOAuth2Token.RESPONSE_TYPE_PARAM, siteConfig.get(PVSiteConfig.PV_AUTH_CODE_RESPONSE.name()));
		attr.put(PerkvilleOAuth2Token.CODE_PARAM, code);
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
			String [] strArr = StringUtil.checkVal(siteConfig.get(PVSiteConfig.PV_SCOPE.name())).split(",");
			for(String s : strArr) {
				scopes.add(Scope.valueOf(s).name());
			}
		} else {
			scopes.add(Scope.PUBLIC.name());
			scopes.add(Scope.USER_CUSTOMER_INFO.name());
		}

		return scopes;
	}

	/**
	 * Build the Config map for the PerkvilleOAuth2Token
	 *
	 * TODO - All Calls required an authenticated user.  If the intent is
	 * to have some areas of Perkville retrieved on a public user then we
	 * need a set of bot Credentials in the system and need to override
	 * missing userId with the bot id.
	 *
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

	/**
	 * Validate that we have a proper Access Token for API Calls.
	 * @return
	 */
	private boolean isValidToken() {
		boolean isValid = false;

		//Get Credentials off authToken.
		Credential c = authToken.getToken();

		//Verify Credentials are useable.
		if(c != null && !StringUtil.isEmpty(c.getAccessToken())) {
			isValid = true;
		}

		return isValid;
	}
}