package com.depuysynthes.scripts;

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
 * <b>Description: Implements the HTTP verb-based calls to showpad's API.  GET POST PUT DELETE, etc.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Nov 4, 2015
 ****************************************************************************/
public class ShowpadApiUtil {

	private OAuth2Token oauthUtil;


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
	public String executeGet(String url) throws IOException {
		return executeGet(new GenericUrl(url)).parseAsString();
	}

	/**
	 * performs an HTTP GET to the remote system with the OAUTH header (token)
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public HttpResponse executeGet(GenericUrl url) throws IOException {
		Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(oauthUtil.getToken().getAccessToken());
		HttpRequestFactory requestFactory = OAuth2Token.transport.createRequestFactory(credential);
		return requestFactory.buildGetRequest(url).execute();
	}


	/**
	 * performs an HTTP POST to the remote system with the OAUTH header (token)
	 * @param url
	 * @param content
	 * @return
	 * @throws IOException
	 */
	public String executePost(String url, Map<String,String> params) throws IOException {
		Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(oauthUtil.getToken().getAccessToken());
		HttpRequestFactory requestFactory = OAuth2Token.transport.createRequestFactory(credential);

		HttpContent content = new UrlEncodedContent(params);
		return requestFactory.buildPostRequest(new GenericUrl(url), content).execute().parseAsString();
	}


	/**
	 * pushes a file via a multi-part HTTP POST request
	 * @param url
	 * @param params
	 * @param f
	 * @return
	 * @throws IOException
	 */
	public String executePostFile(String url, Map<String, String> params, File f, String linkHeader) throws IOException {
		Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(oauthUtil.getToken().getAccessToken());
		HttpRequestFactory requestFactory = OAuth2Token.transport.createRequestFactory(credential);

		FileType ft = new FileType();
		FileContent fileContent = new FileContent(ft.getMimeType(f.getName()), f);
		GenericUrl gUrl = new GenericUrl(url);

		// Add parameters
		MultipartContent content = new MultipartContent();
		content.setMediaType(new HttpMediaType("multipart/form-data").setParameter("boundary", "__END_OF_PART__"));
		for (String k : params.keySet()) {
			MultipartContent.Part part = new MultipartContent.Part(new ByteArrayContent(null, params.get(k).getBytes()));
			part.setHeaders(new HttpHeaders().set("Content-Disposition", String.format("form-data; name=\"%s\"", k)));
			content.addPart(part);
		}

		// Add file
		MultipartContent.Part part = new MultipartContent.Part(fileContent);
		part.setHeaders(new HttpHeaders().set("Content-Disposition", String.format("form-data; name=\"file\"; filename=\"%s\"", f.getName())));
		content.addPart(part);

		//add the Link header
		HttpHeaders linkHeaders = new HttpHeaders();
		if (linkHeader != null && linkHeader.length() > 0)
			linkHeaders.set("Link", linkHeader);

		return  requestFactory.buildPostRequest(gUrl, content).setHeaders(linkHeaders).execute().parseAsString();
	}


	/**
	 * simplified/overloaded method to obfuscate the GenericUrl object from SMT code.
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public String executeDelete(String url) throws IOException {
		return executeDelete(new GenericUrl(url)).parseAsString();
	}


	/**
	 * performs an HTTP DELETE to the remote system with the OAUTH header (token)
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public HttpResponse executeDelete(GenericUrl url) throws IOException {
		Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(oauthUtil.getToken().getAccessToken());
		HttpRequestFactory requestFactory = OAuth2Token.transport.createRequestFactory(credential);
		return requestFactory.buildDeleteRequest(url).execute();
	}
}
