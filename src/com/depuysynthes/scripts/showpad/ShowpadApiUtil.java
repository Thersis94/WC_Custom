package com.depuysynthes.scripts.showpad;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

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

	protected static Logger log = Logger.getLogger(ShowpadApiUtil.class);

	private OAuth2Token oauthUtil;
	private static final int READ_TIMEOUT = 60000; //1 minute
	private static final int WRITE_TIMEOUT = 120000; //2 minutes

	private static final int API_1HR_LIMIT = 9900; //stay below the 10k ceiling.  Leave a buffer of 100, because they may not count as precisely as us.

	protected static int lastMinute = 0;
	protected static AtomicInteger[] minuteTotals = new AtomicInteger[60];

	/**
	 * This class requires an OAUTH token in order to function
	 * @param oauthUtil
	 */
	public ShowpadApiUtil(OAuth2Token oauthUtil) {
		this.oauthUtil = oauthUtil;
		//init the counter array objects
		for (int x=0; x < 60; x++)
			minuteTotals[x] = new AtomicInteger();
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
		checkRequestCount();
		Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(oauthUtil.getToken().getAccessToken());
		HttpRequestFactory requestFactory = OAuth2Token.transport.createRequestFactory(credential);

		HttpResponse resp = requestFactory.buildGetRequest(url).setReadTimeout(READ_TIMEOUT).execute();
		return resp;
	}


	/**
	 * performs an HTTP POST to the remote system with the OAUTH header (token)
	 * @param url
	 * @param content
	 * @return
	 * @throws IOException
	 */
	public String executePost(String url, Map<String,String> params) throws IOException {
		checkRequestCount();
		Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(oauthUtil.getToken().getAccessToken());
		HttpRequestFactory requestFactory = OAuth2Token.transport.createRequestFactory(credential);
		HttpContent content = new UrlEncodedContent(params);

		HttpResponse resp = requestFactory.buildPostRequest(new GenericUrl(url), content).setReadTimeout(WRITE_TIMEOUT).execute();
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
	public String executePostFile(String url, Map<String, String> params, File f, String linkHeader) throws IOException {
		checkRequestCount();
		Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(oauthUtil.getToken().getAccessToken());
		HttpRequestFactory requestFactory = OAuth2Token.transport.createRequestFactory(credential);

		// Add parameters
		MultipartContent content = new MultipartContent();
		content.setMediaType(new HttpMediaType("multipart/form-data").setParameter("boundary", "__END_OF_PART__"));
		for (Map.Entry<String, String> entry : params.entrySet()) {
			MultipartContent.Part part = new MultipartContent.Part(new ByteArrayContent(null, entry.getValue().getBytes()));
			part.setHeaders(new HttpHeaders().set("Content-Disposition", String.format("form-data; name=\"%s\"", entry.getKey())));
			content.addPart(part);
		}

		// Add file - if one was passed
		if (f != null) {
			FileType ft = new FileType();
			FileContent fileContent = new FileContent(ft.getMimeType(f.getName()), f);
			String fileName = params.containsKey("name") ? params.get("name") : f.getName();
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
		return resp.parseAsString();
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
		checkRequestCount();
		Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(oauthUtil.getToken().getAccessToken());
		HttpRequestFactory requestFactory = OAuth2Token.transport.createRequestFactory(credential);

		HttpResponse resp = requestFactory.buildDeleteRequest(url).setReadTimeout(WRITE_TIMEOUT).execute();
		return resp;
	}


	/**
	 * increment a static counter and then test to see if we've hit the Showpad-defined 
	 * glass ceiling on API requests in a 24hr period. 
	 * @throws QuotaException
	 */
	protected static synchronized void checkRequestCount() {
		int currentMinute = Calendar.getInstance().get(Calendar.MINUTE);

		//when the minute changes, set the counter for the NEW minute to zero, then begin incrementing it again
		if (currentMinute != lastMinute) {
			//flush all minutes between the last run and now...we may have been off doing non-Showpad things for 15mins.
			int diff = currentMinute-lastMinute;
			if (diff < 0) diff = 60 - Math.abs(diff); //think 9:10 - 8:55 
			for (int x=diff; x > 0; x--) {
				int idx = currentMinute-x;
				if (idx < 0) idx = 60-idx; //reset to the top of the hour
				minuteTotals[idx].set(0); //remember currentMinute was 5mins ago...add forward from there
				log.debug("reset count on minute " + idx  + " to " + minuteTotals[idx].get());
			}
			
			
			lastMinute = currentMinute;
		}
		int count = minuteTotals[currentMinute].getAndIncrement();

		int total = 0;
		for (int x=minuteTotals.length; x > 0; x--)
			total += minuteTotals[x-1].get();

		log.debug("QuotaTotal | minute: " + count + " hour: " + total);

		//we need to lock and pause the thread, until Showpad releases some quota to us
		//NOTE: if the script is running really fast you may burn through the quota in <1hr.  We may go through several 
		//sleep cylces before the counts begin to come down.  This is proper behavior.
		if (total >= API_1HR_LIMIT) {
			log.info("Sleeping for 5 minutes, 60min Showpad quota reached");
			try {
				Thread.sleep(5*60*1000); //sleep 5mins
			} catch (Exception e) {
				log.fatal("could not sleep thread", e);
			}
		}
	}
}