package com.mts.hootsuite;

// JDK 11
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.catalina.tribes.util.Arrays;
// Apache Logger for detailed logging utilities
import org.apache.log4j.Logger;

// Gson for parsing json data
import com.google.gson.Gson;
import com.mts.hootsuite.AuthResponseVO;

// Local Libs
import com.mts.hootsuite.MediaLinkRequestVO;
import com.mts.hootsuite.MediaLinkResponseVO;
import com.mts.hootsuite.MediaUploadStatusResponseVO;
import com.mts.hootsuite.PostVO;
import com.mts.hootsuite.ScheduleMessageVO;
import com.mts.hootsuite.SchedulePostResponseVO;
import com.mts.hootsuite.SocialMediaProfilesVO;
import com.mts.hootsuite.TokenResponseVO;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.io.http.SMTHttpConnectionManager.HttpConnectionType;

/****************************************************************************
 * <b>Title</b>: HootsuiteTestRequests.java <b>Project</b>: Hootsuite
 * <b>Description: </b> Class for developing Hootsuite test requests
 * <b>Copyright:</b> Copyright (c) 2020 <b>Company:</b> Silicon Mountain
 * Technologies
 * 
 * @author justinjeffrey
 * @version 3.0
 * @since May 11, 2020
 * @updates:
 ****************************************************************************/
public class HootsuiteManager {

	static Logger log = Logger.getLogger(Process.class.getName());
	private String token;
	

	/**
	 * Public main for interfacing with the command line
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		HootsuiteManager hr = new HootsuiteManager();
		hr.execute();
	}
	
	public void execute() throws IOException {
		
//		log.info(shortenURL("https://www.mystrategist.com/medtech-strategist/article/navigating_beyond_covid-19_what_device_companies_can_do_to_survive_and_thrive_a_conversation_with_glenn_snyder_deloitte_consulting.html"));
		
	}
	
	public void post(String socialId, PostVO post, HootsuiteClientVO hc, String postContent, boolean media) throws IOException {

		post.setPostDate(1);// Replace this with the 11am scheduler
		postMessage(socialId, post, postContent, media);
		
	}

	/**
	 * Post a message with media using the hootsuite api.
	 * @param post VO containing post values (text, media ids, date to post)
	 * @param postContent 
	 * @param client VO containing client values (Social profiles ids)
	 */
	public void postMessage(String socialId, PostVO post, String postContent, boolean media) {
		try {
			if(media) {
				uploadHootsuiteMedia(post);
				schedulePost(socialId, post, postContent);
			} else
				schedulePost(socialId, post, postContent);
		} catch (Exception e) {
			log.info(e);
		}
	}
	
	private void getToken(String oauthCode) throws IOException {
		
		Gson gson = new Gson();
		Map<String, Object> parameters = new HashMap<>();
		
		HttpConnectionType post = HttpConnectionType.POST;
		
		SMTHttpConnectionManager cm = new SMTHttpConnectionManager();
		
		parameters.put("grant_type", "authorization_code");
		parameters.put("code", oauthCode);
		parameters.put("redirect_uri", "http://localhost:3000/callback");// Redirect URI that is set in hootsuite.
		
		cm.addRequestHeader("Authorization", "Basic YTYwZDA0MzItMzk5OS00YThkLTkxNDAtZjdhNDNmMzNjZjlmOlVac25hcW5mZVo5bA==");// Basic header. This can probably be assigned using the SMTHTTP utility
		
		// Send post request
		ByteBuffer in = ByteBuffer
				.wrap(cm.getRequestData("https://platform.hootsuite.com/oauth2/token", parameters, post));
		
		log.info(StandardCharsets.UTF_8.decode(in).toString());
		
	}

	/**
	 * Requests a new set of tokens from the Hootsuite Api refresh token endpoint
	 * 
	 * @throws IOException
	 */
	public String refreshToken(HootsuiteClientVO client) throws IOException {
		
		log.info("running refreshToken");

		Gson gson = new Gson();
		Map<String, Object> parameters = new HashMap<>();

		SMTHttpConnectionManager cm = new SMTHttpConnectionManager();

		addRefreshTokenHeaders(cm);

		addRefreshTokenParameters(parameters, client);

		HttpConnectionType post = HttpConnectionType.POST;

		// Send post request
		ByteBuffer in = ByteBuffer
				.wrap(cm.getRequestData("https://platform.hootsuite.com/oauth2/token", parameters, post));

		// Capture the response
		TokenResponseVO response = gson.fromJson(StandardCharsets.UTF_8.decode(in).toString(), TokenResponseVO.class);

		log.info(gson.toJson(response).toString()); // Remove when the tokens are stored to a database.

		checkRefreshTokenResponse(response);

		return response.getRefresh_token();
	}

	/**
	 * Checks if the API response is successful and either logs an error or updates
	 * token values.
	 * 
	 * @param response
	 */
	private void checkRefreshTokenResponse(TokenResponseVO response) {
		if (response.getAccess_token() != null) {
			token = response.getAccess_token();
		} else {
			// Log out the error response info
			log.info(response.getError());
			log.info(response.getError_description());
			log.info(response.getError_hint());
			log.info(response.getStatus_code());
		}
	}

	/**
	 * Adds required parameters for the Hootsuite refresh end point
	 * 
	 * @param parameters
	 * @param client 
	 */
	private void addRefreshTokenParameters(Map<String, Object> parameters, HootsuiteClientVO client) {
		log.info("adding to parameters " + client.getRefreshToken());
		parameters.put("grant_type", "refresh_token");
		parameters.put("refresh_token", client.getRefreshToken());
	}

	/**
	 * Adds required headers for the Hootsuite Token refresh end point.
	 * 
	 * @param cm
	 */
	private void addRefreshTokenHeaders(SMTHttpConnectionManager cm) {
		cm.addRequestHeader("Accept", "application/json;charset=utf-8");
		cm.addRequestHeader("Content-Type", "application/x-www-form-urlencoded");
		cm.addRequestHeader("Authorization",
				"Basic YTYwZDA0MzItMzk5OS00YThkLTkxNDAtZjdhNDNmMzNjZjlmOlVac25hcW5mZVo5bA==");
		cm.addRequestHeader("Accept-Encoding", "gzip, deflate, br");
	}

	/**
	 * Returns a map of all of the social media profile ids associated with the clients profile
	 * @return map of social ids
	 * @throws IOException
	 */
	public HashMap<String, String> getSocialProfiles() throws IOException {
		
		Gson gson = new Gson();

		Map<String, Object> parameters = new HashMap<>();

		SMTHttpConnectionManager cm = new SMTHttpConnectionManager();

		cm.addRequestHeader("Authorization", "Bearer " + token);
		cm.addRequestHeader("Content-Type", "type:application/json;charset=utf-8");

		HttpConnectionType get = HttpConnectionType.GET;

		// Send post request
		ByteBuffer in = ByteBuffer
				.wrap(cm.getRequestData("https://platform.hootsuite.com/v1/socialProfiles", parameters, get));

		SocialMediaProfilesVO response = gson.fromJson(StandardCharsets.UTF_8.decode(in).toString(),
				SocialMediaProfilesVO.class);
		HashMap<String, String> socialProfiles = response.getAllSocialIds();

		if(response.getError() != null) {
			log.info(response.getError() + " : " + response.getError_description());
		}
		
		return socialProfiles;
	}

//	/**
//	 * Checks to see if the token is expired and refreshes the token if it is.
//	 * 
//	 * @throws IOException
//	 */
//	private void checkToken() throws IOException {
//		Date now = new Date();
//		if (now.compareTo(tokenExperationDate) > 0) {
//			refreshToken();
//		}
//	}

	/**
	 * Schedules a social media post using the hootsuite api
	 * @param twitterId VO containing client values (Social profiles ids)
	 * @param post VO containing post values (text, media ids, date to post)
	 * @param postContent 
	 * @throws IOException
	 */
	private void schedulePost(String socialId, PostVO post, String postContent) throws IOException {

		List<String> socialIds = new ArrayList<>();
		socialIds.add(socialId);

		List<Map<String, String>> mediaList = new ArrayList<>();
		
		populateMediaList(mediaList, post.getMediaIds());

		Gson gson = new Gson();

		ScheduleMessageVO message = new ScheduleMessageVO();

		setMessageContent(message, post.getPostDate(), socialIds, postContent, mediaList);

		byte[] document = gson.toJson(message).getBytes();

		SMTHttpConnectionManager cm = new SMTHttpConnectionManager();
		cm.addRequestHeader("Authorization", "Bearer " + token);

		ByteBuffer in = ByteBuffer.wrap(cm.sendBinaryData("https://platform.hootsuite.com/v1/messages", document,
				"application/json", HttpConnectionType.POST));

		SchedulePostResponseVO response = gson.fromJson(StandardCharsets.UTF_8.decode(in).toString(),
				SchedulePostResponseVO.class);
		
		if(response.getErrors().size()>0) {
			log.info(response.getErrorMessage());
		}

	}

	/**
	 * Formats the mediaIds into an array of maps
	 * @param mediaList a list of maps containing the media ids that will be attached to the message body 
	 * @param mediaIds list of social media ids that will be added to the mediaList
	 */
	private void populateMediaList(List<Map<String, String>> mediaList, List<String> socialIds) {
		for(String id : socialIds) {
			Map<String, String> mediaIdMap = new HashMap<>();
			mediaIdMap.put("id", id);
			mediaList.add(mediaIdMap);
		}
	}

	/**
	 * Sets the parameters to the values of the ScheduleMessageVO
	 * 
	 * @param message
	 * @param scheduledSendTime
	 * @param socialIdList
	 * @param messageText
	 * @param mediaList
	 */
	private void setMessageContent(ScheduleMessageVO message, String scheduledSendTime, List<String> socialIdList,
			String messageText, List<Map<String, String>> mediaList) {
		message.setScheduledSendTime(scheduledSendTime);
		message.setSocialProfiles(socialIdList);
		message.setText(messageText);// This needs to be restricted to a length of 280 characters for twitter
		message.setMedia(mediaList);
	}

	/**
	 * getMediaUploadLink will request a link to the Hootsuite AWS file server that
	 * can be used in conjunction with upload image to create a media link for new
	 * message uploads
	 * @param post 
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void uploadHootsuiteMedia(PostVO post) throws IOException, InterruptedException {

//		checkToken();

		Gson gson = new Gson();

		SMTHttpConnectionManager cm = new SMTHttpConnectionManager();
		cm.addRequestHeader("Authorization", "Bearer " + token);

		MediaLinkRequestVO mlr = new MediaLinkRequestVO();
		mlr.setMimeType(post.getMimeType());

		byte[] document = gson.toJson(mlr).getBytes();

		ByteBuffer in = ByteBuffer.wrap(cm.sendBinaryData("https://platform.hootsuite.com/v1/media", document,
				"application/json", HttpConnectionType.POST));

		MediaLinkResponseVO response = gson.fromJson(StandardCharsets.UTF_8.decode(in).toString(),
				MediaLinkResponseVO.class);

		if (response.successfulRequest()) {
			uploadMediaToAWS(response, mlr, post.getMediaLocation());
			post.addMediaId(response.getId());
		} else {
			log.info("checkMediaLinkResponse returned false");
			log.info(response.getError() + " : " + response.getError_description());
		}

		waitForSuccessfulUpload(response);
	}

	/**
	 * Loops the retrieveMediaUploadStatus until the media has been successfully
	 * uploaded to the AWS server
	 * 
	 * @param response the response from hootsuite media link request
	 * @throws IOException
	 * @throws InterruptedException
	 */

	private void waitForSuccessfulUpload(MediaLinkResponseVO response) throws IOException, InterruptedException {

		// Find a better way to do this!
		int timeOut = 0;
		while (!retrieveMediaUploadStatus(response.getId())) {
			timeOut++;
			if (timeOut > 10) {
				log.info("Upload Failed");
				break;
			}
			Thread.sleep(1000);
			log.info("Waiting for upload to complete.");
		}

		if (retrieveMediaUploadStatus(response.getId())) {
			log.info("Media successfully uploaded.");
		} else {
			log.info("Media still uploading.");
		}
	}

	/**
	 * uploadImage will upload a image to the hootsuite AWS file server. this upload
	 * returns a link that can be used when posting message to attach an image to
	 * that message.
	 * 
	 * @param response
	 * @param mlr
	 * 
	 * @throws IOException
	 */
	private void uploadMediaToAWS(MediaLinkResponseVO response, MediaLinkRequestVO mlr, String path)
			throws IOException {

		String errorMessage = "";

		SMTHttpConnectionManager cm = new SMTHttpConnectionManager();

		// Using a file path for now. Later this will need to be changed to whatever
		// format Webcrescendo uses
		byte[] bytesArr = Files.readAllBytes(Paths.get(path));

		// Build the put request using the response url value
		ByteBuffer in = ByteBuffer
				.wrap(cm.sendBinaryData(response.getUploadUrl(), bytesArr, mlr.getMimeType(), HttpConnectionType.PUT));

		errorMessage = StandardCharsets.UTF_8.decode(in).toString();

		if (errorMessage.length() > 0) {
			log.info("uploadMedia response code: " + cm.getResponseCode());
			log.info("uploadMedia error message: " + errorMessage);
		}
	}

	/**
	 * Checks the upload status of a media file to the Hootsuite/Amazon AWS server
	 * 
	 * @return the boolean status of the upload
	 * @throws IOException
	 */
	private boolean retrieveMediaUploadStatus(String mediaId) throws IOException {

//		checkToken();

		Gson gson = new Gson();

		SMTHttpConnectionManager cm = new SMTHttpConnectionManager();
		cm.addRequestHeader("Authorization", "Bearer " + token);

		Map<String, Object> parameters = new HashMap<>();

		HttpConnectionType get = HttpConnectionType.GET;

		ByteBuffer in = ByteBuffer
				.wrap(cm.getRequestData("https://platform.hootsuite.com/v1/media/" + mediaId, parameters, get));

		MediaUploadStatusResponseVO response = gson.fromJson(StandardCharsets.UTF_8.decode(in).toString(),
				MediaUploadStatusResponseVO.class);

		if (response.getState() != null && response.getState().equalsIgnoreCase("READY"))
			return true;
		else
			return false;
	}
	
//	/**
//	 * Creates a shortened URL using the Bit.ly URL shortening API
//	 * @return a shortened version of the URL
//	 * @throws IOException 
//	 */
//	private String shortenURL(String longURL) throws IOException {
//		
//		String bitlyToken = "3bffc1465445d781e2ebd502f30c295d6f5c04a9";
//		String shortURL = "";
//		
//		Gson gson = new Gson();
//
//		SMTHttpConnectionManager cm = new SMTHttpConnectionManager();
//		cm.addRequestHeader("Authorization", "Bearer " + bitlyToken);
//		cm.addRequestHeader("Accept", "*/*");
//		
//		URLShortenerRequestVO sr = new URLShortenerRequestVO();
//		sr.setLong_url(longURL);
//
//		byte[] document = gson.toJson(sr).getBytes();
//		
//		String doc = new String(document);
//		log.info(doc);
//		
//		ByteBuffer in = ByteBuffer.wrap(cm.sendBinaryData("https://api-ssl.bitly.com/v4/shorten", document,
//				"application/json", HttpConnectionType.POST));
//		
//		URLShortenerResponseVO response = gson.fromJson(StandardCharsets.UTF_8.decode(in).toString(),
//				URLShortenerResponseVO.class);
//
//		if(response.getMessage().length() != 0) {
//			log.info(response.getMessage());
//			log.info(response.getErrors());
//			log.info(response.getMessage());
//			log.info(response.getResource());
//			return null;
//		} else
//			return response.getLink();		
//	}
}