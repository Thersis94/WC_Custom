package com.perkville;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.siliconmtn.http.parser.StringEncoder;
import com.smt.sitebuilder.security.oauth.OAuth2TokenViaDB;

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
public class PerkvilleOAuth2Token extends OAuth2TokenViaDB {

	public static final String CLIENT_ID_PARAM = "client_id";
	public static final String REDIRECT_URL_PARAM = "redirect_uri";
	public static final String RESPONSE_TYPE_PARAM = "response_type";
	public static final String SCOPE_PARAM = "scope";
	public static final String STATE_PARAM = "state";
	public static final String GRANT_TYPE_PARAM = "grant_type";
	public static final String CODE_PARAM = "code";

	public static final String PERKVILLE_KEYSTORE = "perkville";

	public PerkvilleOAuth2Token(Map<Config, String> config, List<String> scopes, Map<String, Object> attributes) throws IOException {
		super(config, scopes, attributes);
	}

	/**
	 * Builds appropriate Auth URL for Perkville.
	 * @return the Auth Request Url.
	 */
	@Override
	public String buildAuthReqUrl() {
		GenericUrl url = new GenericUrl(config.get(Config.AUTH_SERVER_URL));
	    url.set(CLIENT_ID_PARAM, config.get(Config.API_KEY));
	    url.set(RESPONSE_TYPE_PARAM, attributes.get(RESPONSE_TYPE_PARAM));
	    url.set(SCOPE_PARAM, buildScopeString());
	    url.set(STATE_PARAM, attributes.get(STATE_PARAM));

		//GenericUrl is encoding the CallbackUrl in a way that Perkville doesn't recognize so append it after the fact.
	    StringBuilder u = new StringBuilder(150);
	    u.append(url.build()).append("&").append(REDIRECT_URL_PARAM).append("=").append(StringEncoder.urlEncode(config.get(Config.TOKEN_CALLBACK_URL)));
		log.info(u);

	    return u.toString();
	}

	/**
	 * Build the Token Request Url.
	 */
	@Override
	public String buildTokenReqUrl() {
		GenericUrl url = new GenericUrl(config.get(Config.TOKEN_SERVER_URL));
		url.set(CLIENT_ID_PARAM, config.get(Config.API_KEY));
		url.set(CODE_PARAM, attributes.get(CODE_PARAM));
		url.set(GRANT_TYPE_PARAM, config.get(Config.GRANT_TYPE));
		url.set(STATE_PARAM, attributes.get(STATE_PARAM));

		//GenericUrl is encoding the CallbackUrl in a way that Perkville doesn't recognize so append it after the fact.
		StringBuilder u = new StringBuilder(150);
	    u.append(url.build()).append("&").append(REDIRECT_URL_PARAM).append("=").append(StringEncoder.urlEncode(config.get(Config.TOKEN_CALLBACK_URL)));
		log.info(u);

	    return u.toString();
	}

	/**
	 * Build the Scope String according to how Perkville wants it.
	 * @return
	 */
	private String buildScopeString() {
		StringBuilder scopeStr = new StringBuilder(scopes.size() * 15);
		for(String s: scopes) {
			scopeStr.append(s).append(" ");
		}

		return scopeStr.toString().trim();
	}

	/**
	 * Build the TokenResponse Object with the provided code.
	 * @param code - Response from the OAuth Endpoint.
	 * @return
	 * @throws IOException
	 */
	public TokenResponse getTokenResponse(String code) throws IOException {
		AuthorizationCodeTokenRequest tokReq = flow.newTokenRequest(code)
		.setRedirectUri(config.get(Config.TOKEN_CALLBACK_URL))
		.setScopes(scopes);
		tokReq.setClientAuthentication(new BasicAuthentication(config.get(Config.API_KEY), config.get(Config.API_SECRET)));

		return tokReq.execute();
	}

	/**
	 * Build the TokenRequestUrl with the required Params.
	 * @param code - Auth code retrieved from AuthRequest Response.
	 * @return
	 */
	public String buildTokenRequestUrl(String code) {
		GenericUrl url = new GenericUrl(config.get(Config.TOKEN_SERVER_URL));
		url.set(CLIENT_ID_PARAM, config.get(Config.API_KEY));
		url.set(CODE_PARAM, code);
		url.set(GRANT_TYPE_PARAM, attributes.get(GRANT_TYPE_PARAM));
		url.set(REDIRECT_URL_PARAM, Config.TOKEN_CALLBACK_URL);
		url.set(STATE_PARAM, attributes.get(STATE_PARAM));

		return url.build();
	}
}