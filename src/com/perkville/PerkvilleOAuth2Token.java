package com.perkville;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.RefreshTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.siliconmtn.security.OAuth2Token;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> PerkvilleOAuth2Token.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Perkville OAuth Token.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 28, 2017
 ****************************************************************************/
public class PerkvilleOAuth2Token implements OAuth2Token {

	protected static Logger log;
	
	/**
	 * how close we're willing to get to an expired token before refreshing it
	 */
	protected static final int TIMEOUT_BUFFER_SECS = 30;

	public static final String CLIENT_ID_PARAM = "client_id";
	public static final String REDIRECT_URL_PARAM = "redirect_url";
	public static final String RESPONSE_TYPE_PARAM = "response_type";
	public static final String SCOPE_PARAM = "scope";
	public static final String STATE_PARAM = "state";
	public static final String GRANT_TYPE_PARAM = "grant_type";
	public static final String CODE_PARAM = "code";

	private Map<Config, String> config = null;

	/** 
	 * Change Scopes to match those you've authorized in your Showpad account for this user.
	 */
	private Collection<String> scopes = null;

	private AuthorizationCodeFlow flow = null;
	/**
	 * Global instance of the {@link DataStoreFactory}. The best practice is to make it a single
	 * globally shared instance across your application.
	 */
	private FileDataStoreFactory dataStoreFactory;

	private Credential token;
	public enum SCOPE {
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

	public enum Config {
		PV_CLIENT_ID,
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

	public PerkvilleOAuth2Token(Map<Config, String> config) throws IOException {
		if (config == null || config.size() != Config.values().length) {
			throw new IOException("Config missing or incomplete.  You must implement all values in the Param enum.");
		}
		log = Logger.getLogger(this.getClass());
		this.config = config;
		buildScopes(config.get(Config.PV_SCOPE));

		//TODO - How to store Credentials?
		File dataStoreDir = new File(System.getProperty("user.home"), ".store/" + config.get(Config.PV_KEYSTORE));
		dataStoreFactory = new FileDataStoreFactory(dataStoreDir);

		//load the token so we're ready to go.
		//We want to tell the invoking class early if this isn't going to work; they likely depend on it.
		token = obtainToken();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.OAuth2Token#getToken()
	 */
	@Override
	public Credential getToken() {
		//renew the token if it expires in the next 30 seconds; it may be worthless by the time we're ready to use it. -JM 02.16.16
		if (token == null || token.getExpiresInSeconds() == null || token.getExpiresInSeconds() <= TIMEOUT_BUFFER_SECS) {
			if (token != null) log.debug("token timeout in " + token.getExpiresInSeconds() + " seconds");
			try {
				token = obtainToken();
			} catch (Exception e) {
				log.error("could not obtain access token", e);
			}
		}
		return token;
	}

	private Credential obtainToken() throws IOException {
		AuthorizationCodeFlow.Builder builder = new AuthorizationCodeFlow.Builder(
				BearerToken.authorizationHeaderAccessMethod(),
				transport,
				jsonFactory,
				new GenericUrl(buildAuthRequestUrl()),
				new BasicAuthentication(config.get(Config.PV_API_KEY), config.get(Config.PV_API_SECRET)),
				config.get(Config.PV_CLIENT_ID),
				config.get(Config.PV_AUTH_SERVER_URL));
		builder.setScopes(scopes);
		builder.setDataStoreFactory(dataStoreFactory);

		flow = builder.build();
		return flow.loadCredential(config.get(Config.PV_CLIENT_ID));
	}

	/**
	 * creates and sends a RefreshTokenRequest to the OAUTH2 remote server.
	 * The returned TokenResponse must be captured by the Flow and persisted to 
	 * the keystore for next time.
	 * @param c
	 * @return
	 * @throws IOException
	 */
	public TokenResponse requestRefreshToken(Credential c) throws IOException {
		RefreshTokenRequest authReq = new RefreshTokenRequest(
				transport, 
				jsonFactory,
				new GenericUrl(config.get(Config.PV_TOKEN_SERVER_URL)), 
				c.getRefreshToken());
		authReq.setClientAuthentication(new BasicAuthentication(config.get(Config.PV_API_KEY), config.get(Config.PV_API_SECRET)));
		authReq.setGrantType(config.get(Config.PV_GRANT_TYPE_CODE));

		return authReq.execute();
	}

	/**
	 * client_id		Yes		The client id that was emailed to the developer contact upon client registration
	 * 
	 * redirect_uri	No		The redirect uri that you would like Perkville to redirect your user to once they
	 * 						have authorized your application to access their data on Perkville. Important:
	 * 						This MUST match one of the redirect uris that was provided upon client registration.
	 * 						If this querystring parameter is omitted, Perkville will default to the first redirect
	 * 						uri it has stored for the provided client id.
	 * 
	 * response_type	Yes		This must be "code" for the authorization code flow.
	 * 
	 * scope			No		The scopes that you would like to request access to on behalf of the user, separated by
	 * 						spaces. See below for a detailed explanation of the allowed scopes. If not provided,
	 * 						defaults to all of the scopes that your client application is allowed to request.
	 * 
	 * state			No		It is recommended that the client application populate this query parameter with a random
	 * 						hash. When Perkville redirects the user back to the client server, this state will be
	 * 						included in the redirect request query string so that the client servers may verify
	 * 						the authenticity of the request.
	 * 
	 * @return the Auth Request Url.
	 */
	public String buildAuthRequestUrl() {
		GenericUrl url = new GenericUrl(config.get(Config.PV_AUTH_SERVER_URL));
	    url.set(CLIENT_ID_PARAM, config.get(Config.PV_CLIENT_ID));
	    url.set(REDIRECT_URL_PARAM, config.get(Config.PV_TOKEN_CALLBACK_URL));
	    url.set(RESPONSE_TYPE_PARAM, config.get(Config.PV_AUTH_CODE_RESPONSE));
	    url.set(SCOPE_PARAM, config.get(Config.PV_SCOPE));
	    url.set(STATE_PARAM, config.get(Config.PV_STATE));

	    return url.build();
	}

	/**
	 * Build the TokenRequestUrl with the required Params.
	 * @param code - Auth code retrieved from AuthRequest Response.
	 * @return
	 */
	public String buildTokenRequestUrl(String code) {
		GenericUrl url = new GenericUrl(config.get(Config.PV_TOKEN_SERVER_URL));
		url.set(CLIENT_ID_PARAM, config.get(Config.PV_CLIENT_ID));
		url.set(CODE_PARAM, code);
		url.set(GRANT_TYPE_PARAM, config.get(Config.PV_GRANT_TYPE_CODE));
		url.set(REDIRECT_URL_PARAM, config.get(Config.PV_TOKEN_CALLBACK_URL));
		url.set(STATE_PARAM, config.get(Config.PV_STATE));

		return url.build();
	}

	public AuthorizationCodeFlow getFlow() {
		return flow;
	}

	/**
	 * Build List of Perkville Scopes we want for the user.
	 * @return
	 */
	public void buildScopes(String scopeConfig) {
		scopes = new ArrayList<>();
		if(!StringUtil.isEmpty(scopeConfig) && scopeConfig.indexOf('+') > -1) {
			String [] sArr = scopeConfig.split("\\+");
			for(String s : sArr) {
				try {
					scopes.add(SCOPE.valueOf(s).name());
				} catch(Exception e) {
					log.error("Scope " + s + " is not a valid Perkville Scope.");
				}
			}
		} else {
			scopes.add(SCOPE.PUBLIC.name());
			scopes.add(SCOPE.USER_CUSTOMER_INFO.name());
		}
	}

	/**
	 * @param parameter
	 * @return
	 * @throws IOException 
	 */
	public Credential processResponse(String code) throws IOException {
		TokenResponse response;
		response = getFlow().newTokenRequest(code)
				.setRedirectUri(config.get(Config.PV_TOKEN_CALLBACK_URL))
				.setScopes(scopes)
				.execute();

		// store credential and return it
		return getFlow().createAndStoreCredential(response, config.get(Config.PV_CLIENT_ID));
	}
}