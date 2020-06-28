package com.mts.hootsuite;

import java.io.ByteArrayInputStream;
// JDK 11
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// Apache Logger for detailed logging utilities
import org.apache.log4j.Logger;

// Gson for parsing json data
import com.google.gson.Gson;

// Local Libs
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

	}

	/**
	 * Public main for interfacing with the command line
	 * @param success
	 * @param msg
	 * @param socialId
	 * @param post
	 * @param postContent
	 * @param media
	 */
	public void post(boolean success, StringBuilder msg, String socialId, PostVO post, String postContent, boolean media) {
		post.setPostTime(1);// Replace this with the 11am scheduler
		try {
			if(media) {
				uploadHootsuiteMedia(success, msg, post);
				schedulePost(success, msg, socialId, post, postContent);
			} else
				schedulePost(success, msg, socialId, post, postContent);
		} catch (Exception e) {
			msg.append("Failure: ").append(e.getLocalizedMessage());
			success = false;
		}
	}

	/**
	 * Requests a new set of tokens from the Hootsuite Api refresh token endpoint
	 * @param success
	 * @param msg
	 * @param client
	 * @return
	 * @throws IOException
	 */
	public String refreshToken(boolean success, StringBuilder msg, HootsuiteClientVO client) throws IOException {

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

		checkRefreshTokenResponse(success, msg, response);

		return response.getRefresh_token();
	}

	/**
	 * Checks if the API response is successful and either logs an error or updates
	 * token values.
	 * @param success
	 * @param msg
	 * @param response
	 */
	private void checkRefreshTokenResponse(boolean success, StringBuilder msg, TokenResponseVO response) {
		if (response.getAccess_token() != null) {
			token = response.getAccess_token();
		} else {
			// Set schedule job success to false and append the completion message to include the error
			success = false;
			msg.append("Failure: ").append("Refresh Token Failed : " + response.getError() + "|" + response.getError_description() + "|" + response.getError_hint() + "|" + response.getStatus_code());
		}
	}

	/**
	 * Adds required parameters for the Hootsuite refresh end point
	 * @param parameters
	 * @param client
	 */
	private void addRefreshTokenParameters(Map<String, Object> parameters, HootsuiteClientVO client) {
		parameters.put("grant_type", "refresh_token");
		parameters.put("refresh_token", client.getRefreshToken());
	}

	/**
	 * Adds required headers for the Hootsuite Token refresh end point.
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
	 * @param success
	 * @param msg
	 * @return
	 * @throws IOException
	 */
	public HashMap<String, String> getSocialProfiles(boolean success, StringBuilder msg) throws IOException {
		
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
			// Set schedule job success to false and append the completion message to include the error
			success = false;
			msg.append("Failure: ").append("Refresh Token Failed : " + response.getError() + "|" + response.getError_description());
		}
		
		return socialProfiles;
	}

	/**
	 * Schedules a social media post using the hootsuite api
	 * @param success
	 * @param msg
	 * @param socialId
	 * @param post
	 * @param postContent
	 * @throws IOException
	 */
	private void schedulePost(boolean success, StringBuilder msg, String socialId, PostVO post, String postContent) throws IOException {

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
				"application/json;charset=utf-8", HttpConnectionType.POST));

		SchedulePostResponseVO response = gson.fromJson(StandardCharsets.UTF_8.decode(in).toString(),
				SchedulePostResponseVO.class);
		
		if(response.getErrors().size()>0) {
			// Set schedule job success to false and append the completion message to include the error
			success = false;
			msg.append("Failure: ").append("Refresh Token Failed : " + response.getErrors().toString() + "|" + response.getErrorMessage().toString());
		}

	}

	/**
	 * Formats the mediaIds into an array of maps
	 * @param mediaList
	 * @param socialIds
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
		message.setText(messageText);
		message.setMedia(mediaList);
	}

	/**
	 * getMediaUploadLink will request a link to the Hootsuite AWS file server that
	 * can be used in conjunction with upload image to create a media link for new
	 * message uploads
	 * @param success
	 * @param msg
	 * @param post
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void uploadHootsuiteMedia(boolean success, StringBuilder msg, PostVO post) throws IOException, InterruptedException {

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
			uploadMediaToAWS(success, msg, response, mlr, post.getMediaLocation());
			post.addMediaId(response.getId());
		} else {
			// Set schedule job success to false and append the completion message to include the error
			success = false;
			msg.append("Failure: ").append("Refresh Token Failed : " + response.getError() + "|" + response.getError_description());
		}

		waitForSuccessfulUpload(success, msg, response);
	}

	/**
	 * Loops the retrieveMediaUploadStatus until the media has been successfully
	 * uploaded to the AWS server
	 * @param success
	 * @param msg
	 * @param response
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void waitForSuccessfulUpload(boolean success, StringBuilder msg, MediaLinkResponseVO response) throws IOException, InterruptedException {

		int timeOut = 0;
		while (!retrieveMediaUploadStatus(response.getId())) {
			timeOut++;
			if (timeOut > 10) {
				msg.append("Failure: ").append("Media failed to upload.");
				success = false;
				break;
			}
			Thread.sleep(1000);
			log.debug("Waiting for upload to complete.");
		}

		if (retrieveMediaUploadStatus(response.getId())) {
			log.debug("Media successfully uploaded.");
		} else {
			// Set schedule job success to false and append the completion message to include the error
			success = false;
			msg.append("Failure: ").append("Media failed to upload.");
		}
	}

	/**
	 * uploadImage will upload a image to the hootsuite AWS file server. this upload
	 * returns a link that can be used when posting message to attach an image to
	 * that message.
	 * @param success
	 * @param msg
	 * @param response
	 * @param mlr
	 * @param path
	 * @throws IOException
	 */
	private void uploadMediaToAWS(boolean success, StringBuilder msg, MediaLinkResponseVO response, MediaLinkRequestVO mlr, String path)
			throws IOException {

		String errorMessage = "";

		SMTHttpConnectionManager cm = new SMTHttpConnectionManager();
		
		URL url = new URL(path);
		URLConnection conn = (URLConnection) url.openConnection();
		
		InputStream is = new ByteArrayInputStream(new byte[] { 0, 1, 2 });
		
		is = conn.getInputStream();
		 
	    byte[] bytesArr = is.readAllBytes();
		
		ByteBuffer in = ByteBuffer
				.wrap(cm.sendBinaryData(response.getUploadUrl(), bytesArr, mlr.getMimeType(), HttpConnectionType.PUT));

		errorMessage = StandardCharsets.UTF_8.decode(in).toString();

		if (errorMessage.length() > 0) {
			// Set schedule job success to false and append the completion message to include the error
			success = false;
			msg.append("Failure: ").append("Refresh Token Failed : " + response.getError() + "|" + response.getError_description());
		}
	}

	/**
	 * Checks the upload status of a media file to the Hootsuite/Amazon AWS server
	 * @param mediaId
	 * @return
	 * @throws IOException
	 */
	private boolean retrieveMediaUploadStatus(String mediaId) throws IOException {

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
}