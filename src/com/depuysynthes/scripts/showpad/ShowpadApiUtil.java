package com.depuysynthes.scripts.showpad;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpMediaType;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.MultipartContent;
import com.google.api.client.http.UrlEncodedContent;
import com.siliconmtn.common.FileType;
import com.siliconmtn.security.OAuth2Token;

/****************************************************************************
 * <b>Title</b>: ShowpadApiUtil.java<p/>
 * <b>Description:</b> Implements the HTTP verb-based calls to showpad's API.  GET POST PUT DELETE, etc.
 * Works with the "v3" version of the showpad API. 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Nov 4, 2015
 ****************************************************************************/
public class ShowpadApiUtil {

	private OAuth2Token oauthUtil;
	private final int READ_TIMEOUT = 60000; //1 minute
	private final int WRITE_TIMEOUT = 120000; //2 minutes

	private static int requestCount = 0;
	private static final int API_LIMIT = 49900; //stay below the 50k ceiling

	/**
	 * This class requires an OAUTH token in order to function
	 * @param oauthUtil
	 */
	public ShowpadApiUtil(OAuth2Token oauthUtil) {
		this.oauthUtil = oauthUtil;
	}

	/**
	 * simplified/overloaded method to obfuscate the GenericUrl object from SMT code.
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public String executeGet(String url) throws IOException, QuotaException {
		return executeGet(new GenericUrl(url)).parseAsString();
	}

	/**
	 * performs an HTTP GET to the remote system with the OAUTH header (token)
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public HttpResponse executeGet(GenericUrl url) throws IOException, QuotaException {
		checkRequestCount();
		Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(oauthUtil.getToken().getAccessToken());
		HttpRequestFactory requestFactory = OAuth2Token.transport.createRequestFactory(credential);
		
		HttpResponse resp = requestFactory.buildGetRequest(url).setReadTimeout(READ_TIMEOUT).execute();
		if ("429".equals(resp.getStatusCode())) throw new QuotaException("rate limit exceeded");
		return resp;
	}


	/**
	 * performs an HTTP POST to the remote system with the OAUTH header (token)
	 * @param url
	 * @param content
	 * @return
	 * @throws IOException
	 */
	public String executePost(String url, Map<String,String> params) throws IOException, QuotaException {
		checkRequestCount();
		Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(oauthUtil.getToken().getAccessToken());
		HttpRequestFactory requestFactory = OAuth2Token.transport.createRequestFactory(credential);
		HttpContent content = new UrlEncodedContent(params);
		
		HttpResponse resp = requestFactory.buildPostRequest(new GenericUrl(url), content).setReadTimeout(WRITE_TIMEOUT).execute();
		if ("429".equals(resp.getStatusCode())) throw new QuotaException("rate limit exceeded");
		return resp.parseAsString();
	}


	/**
	 * pushes a file via a multi-part HTTP POST request
	 * @param url
	 * @param params
	 * @param f
	 * @return
	 * @throws IOException
	 */
	public String executePostFile(String url, Map<String, String> params, File f, String linkHeader) throws IOException, QuotaException {
		checkRequestCount();
		Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(oauthUtil.getToken().getAccessToken());
		HttpRequestFactory requestFactory = OAuth2Token.transport.createRequestFactory(credential);

		// Add parameters
		MultipartContent content = new MultipartContent();
		content.setMediaType(new HttpMediaType("multipart/form-data").setParameter("boundary", "__END_OF_PART__"));
		for (String k : params.keySet()) {
			MultipartContent.Part part = new MultipartContent.Part(new ByteArrayContent(null, params.get(k).getBytes()));
			part.setHeaders(new HttpHeaders().set("Content-Disposition", String.format("form-data; name=\"%s\"", k)));
			content.addPart(part);
		}
		
		// Add file - if one was passed
		if (f != null) {
			FileType ft = new FileType();
			FileContent fileContent = new FileContent(ft.getMimeType(f.getName()), f);
			String fileName = (params.containsKey("name") ? params.get("name") : f.getName());
			MultipartContent.Part part = new MultipartContent.Part(fileContent);
			part.setHeaders(new HttpHeaders().set("Content-Disposition", String.format("form-data; name=\"file\"; filename=\"%s\"", fileName)));
			content.addPart(part);
		}

		//add the Link header
		HttpHeaders linkHeaders = new HttpHeaders();
		if (linkHeader != null && linkHeader.length() > 0)
			linkHeaders.set("Link", linkHeader);

		GenericUrl gUrl = new GenericUrl(url);
		HttpResponse resp = requestFactory.buildPostRequest(gUrl, content).setReadTimeout(WRITE_TIMEOUT).setHeaders(linkHeaders).execute();
		if ("429".equals(resp.getStatusCode())) throw new QuotaException("rate limit exceeded");
		return resp.parseAsString();
	}


	/**
	 * simplified/overloaded method to obfuscate the GenericUrl object from SMT code.
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public String executeDelete(String url) throws IOException, QuotaException {
		return executeDelete(new GenericUrl(url)).parseAsString();
	}


	/**
	 * performs an HTTP DELETE to the remote system with the OAUTH header (token)
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public HttpResponse executeDelete(GenericUrl url) throws IOException, QuotaException {
		checkRequestCount();
		Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(oauthUtil.getToken().getAccessToken());
		HttpRequestFactory requestFactory = OAuth2Token.transport.createRequestFactory(credential);
		
		HttpResponse resp = requestFactory.buildDeleteRequest(url).setReadTimeout(WRITE_TIMEOUT).execute();
		if ("429".equals(resp.getStatusCode())) throw new QuotaException("rate limit exceeded");
		return resp;
	}
	
	
	/**
	 * increment a static counter and then test to see if we've hit the Showpad-defined 
	 * glass ceiling on API requests in a 24hr period. 
	 * @throws QuotaException
	 */
	private void checkRequestCount() throws QuotaException {
		++requestCount;
		if (requestCount >= API_LIMIT) throw new QuotaException("showpad API limit reached, too many requests: " + requestCount);
	}
}

/**
 * **************************************************************************
 * <b>Title</b>: ShowpadApiUtil.java<p/>
 * <b>Description: Used to tell invoking classes that we've reached our threshold at Showpad
 * and are unable to process any more requests.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 15, 2016
 ***************************************************************************
 */
class QuotaException extends Exception {
	private static final long serialVersionUID = -4186031278333010611L;

	public QuotaException(String reason) {
		super(reason);
	}
}
