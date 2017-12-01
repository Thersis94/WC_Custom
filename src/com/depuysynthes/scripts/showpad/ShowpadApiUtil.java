package com.depuysynthes.scripts.showpad;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicIntegerArray;

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
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.security.OAuth2TokenViaCLI;
import com.siliconmtn.security.BaseOAuth2Token.Config;

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
	private static final int READ_TIMEOUT = 120000; //2 minutes
	private static final int WRITE_TIMEOUT = 120000; //2 minutes

	private static final int API_1HR_LIMIT = 3500; //stay below the 5k ceiling.  Leave a buffer of 100, because they may not count as precisely as us.

	protected static int lastMinute = 0;
	protected static AtomicIntegerArray minuteTotals = new AtomicIntegerArray(60);

	/**
	 * This class requires an OAUTH token in order to function
	 * @param oauthUtil
	 */
	public ShowpadApiUtil(OAuth2Token oauthUtil) {
		this.oauthUtil = oauthUtil;
	}


	/**
	 * reused method to load config and initialize the API util
	 * @param prefix
	 * @return
	 * @throws IOException
	 */
	public static ShowpadApiUtil makeInstance(Properties props, String pfx) throws IOException {
		String prefix = pfx == null ? "" : pfx;

		//setup API util for the target account
		EnumMap<Config, String> config = new EnumMap<>(Config.class);
		config.put(Config.USER_ID, props.getProperty(prefix + "showpadAcctName"));
		config.put(Config.API_KEY, props.getProperty(prefix + "showpadApiKey"));
		config.put(Config.API_SECRET, props.getProperty(prefix + "showpadApiSecret"));
		config.put(Config.TOKEN_CALLBACK_URL, props.getProperty(prefix + "showpadCallbackUrl"));
		config.put(Config.TOKEN_SERVER_URL, props.getProperty(prefix + "showpadTokenUrl"));
		config.put(Config.AUTH_SERVER_URL,  props.getProperty(prefix + "showpadAuthUrl"));
		config.put(Config.KEYSTORE, "showpad-" + StringUtil.removeNonAlphaNumeric(config.get(Config.API_KEY)));
		config.put(Config.GRANT_TYPE, "refresh_token");
		List<String> scopes = Arrays.asList(props.getProperty(prefix + "showpadScopes").split(","));
		return new ShowpadApiUtil(new OAuth2TokenViaCLI(config, scopes));
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
		checkResponseForQuota(resp);
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
		checkResponseForQuota(resp);
		return resp != null ? resp.parseAsString() : "";
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
			if (entry.getValue() == null) {
				log.error("empty content part " + entry.getKey());
				continue;
			}
			MultipartContent.Part part = new MultipartContent.Part(new ByteArrayContent(null, entry.getValue().getBytes()));
			part.setHeaders(new HttpHeaders().set("Content-Disposition", String.format("form-data; name=\"%s\"", entry.getKey())));
			content.addPart(part);
		}

		// Add file - if one was passed
		if (f != null) {
			FileType ft = new FileType();
			FileContent fileContent = new FileContent(ft.getMimeType(f.getName()), f);
			MultipartContent.Part part = new MultipartContent.Part(fileContent);
			part.setHeaders(new HttpHeaders().set("Content-Disposition", String.format("form-data; name=\"file\"; filename=\"%s\"", f.getName())));
			log.debug("uploading file named " + f.getName() + " size=" + f.length());
			content.addPart(part);
		}

		//add the Link header
		HttpHeaders linkHeaders = new HttpHeaders();
		if (linkHeader != null && !linkHeader.isEmpty())
			linkHeaders.set("Link", linkHeader);

		GenericUrl gUrl = new GenericUrl(url);
		HttpResponse resp = requestFactory.buildPostRequest(gUrl, content).setReadTimeout(WRITE_TIMEOUT).setHeaders(linkHeaders).execute();
		checkResponseForQuota(resp);
		return resp != null ? resp.parseAsString() : "";
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
		checkResponseForQuota(resp);
		return resp;
	}


	/**
	 * inspect the response code for 429, which is their QuotaLimitException.  We need to pause the script when that occurs.
	 * @param resp
	 */
	private void checkResponseForQuota(HttpResponse resp) {
		if (resp == null || 429 != resp.getStatusCode()) return;
		//if we get a 429, bad things are going on.  Showpad things we're over our limit even though our internal counter says otherwise.
		//Let's just sleep for 30mins and let that angry orge named Showpad cool down.
		sleepThread(30*60*1000);
	}

	/**
	 * increment a static counter and then test to see if we've hit the Showpad-defined 
	 * glass ceiling on API requests in a 24hr period. 
	 * @throws QuotaException
	 */
	protected static synchronized void checkRequestCount() {
		final int currentMinute = Calendar.getInstance().get(Calendar.MINUTE);

		//when the minute changes, set the counter for the NEW minute to zero, then begin incrementing it again
		if (currentMinute != lastMinute) {
			//set the NEW minute back to zero, from an hour ago.
			minuteTotals.set(currentMinute, 0);

			//flush all minutes between the last run and now...we may have been off doing non-Showpad things for 10mins.
			flushNPriorMinutes(currentMinute, lastMinute);

			lastMinute = currentMinute;
		}

		int count = minuteTotals.incrementAndGet(currentMinute);
		int total = 0;
		for (int x=minuteTotals.length(); x > 0; x--)
			total += minuteTotals.get(x-1);

		log.debug("QuotaTotal | minute: " + count + " hour: " + total);

		//we need to lock and pause the thread, until Showpad releases some quota to us
		//NOTE: if the script is running really fast you may burn through the quota in <1hr.  We may go through several 
		//sleep cylces before the counts begin to come down.  This is proper behavior.
		if (total >= API_1HR_LIMIT)
			sleepThread(5*60*1000);
	}


	/**
	 * @param i
	 */
	private static void sleepThread(int milliseconds) {
		log.info("Sleeping for " + milliseconds/60000 + " minutes, 60min Showpad quota reached");
		try {
			Thread.sleep(milliseconds);
		} catch (Exception e) {
			log.fatal("could not sleep thread", e);
		}
	}

	/**
	 * if there has been a multi-minute lapse since the counter incremented, go back through those
	 * minutes and reset them all to zero, because no activity was recorded during that time.
	 * @param currentMinute
	 * @param lastMinute
	 */
	private static void flushNPriorMinutes(int currentMinute, int lastMinute) {
		int diff = currentMinute-lastMinute;
		if (diff < 0) diff = 60 - Math.abs(diff); //think 9:10 - 8:55 
		if (diff < 2) return; //0=no change, 1=naturally resetting, no time jump

		//remember currentMinute was 5mins ago...roll forward from there
		for (int x=diff-1; x > 0; x--) {
			int idx = currentMinute-x;
			if (idx < 0) idx = 60-Math.abs(idx); //reset to the top of the hour
			minuteTotals.set(idx, 0);
			log.debug("reset count on minute " + idx  + " to " + minuteTotals.get(idx));
		}
	}
}