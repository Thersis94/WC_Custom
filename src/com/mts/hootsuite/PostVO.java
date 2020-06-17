package com.mts.hootsuite;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.log4j.Logger;

/****************************************************************************
 * <b>Title</b>: ArticleVO.java <b>Project</b>: Hootsuite <b>Description: </b>
 * VO for holding information required to create a new post for an article
 * <b>Copyright:</b> Copyright (c) 2020 <b>Company:</b> Silicon Mountain
 * Technologies
 * 
 * @author justinjeffrey
 * @version 3.0
 * @since May 28, 2020
 * @updates:
 ****************************************************************************/
public class PostVO {

	static Logger log = Logger.getLogger(Process.class.getName());
	
	String messageText;
	Date postDate; // We need to add 1 day to this at some point
	List<String> mediaId = new ArrayList<>();
	String mimeType;
	String mediaLocation;

	public PostVO() {
		messageText = "Java test message in PostVO";
		mediaLocation = "/home/justinjeffrey/Downloads/demoImg.jpeg";
		mimeType = "image/jpeg";
		postDate = new Date();
	}

	/**
	 * @return the messageText
	 */
	public String getMessageText() {
		return messageText;
	}

	/**
	 * @param messageText the messageText to set
	 */
	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}

	/**
	 * @return the postDate as a ISO formatted String
	 */
	public String getPostDate() {

		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
		df.setTimeZone(tz);
		String dateAsISO = df.format(postDate);

		return dateAsISO;
	}

	/**
	 * Set the Post Date value.
	 * @param postDayIncrement int that determines the number of days in the future the post will be scheduled.
	 */
	public void setPostDate(int postDayIncrement) {
		Calendar c = Calendar.getInstance();

		try {
			// Setting the date to the given date
			c.setTime(new Date());
		} catch (Exception e) {
			log.info(e);
		}
		
		c.add(Calendar.DAY_OF_MONTH, postDayIncrement);

		this.postDate = c.getTime();
	}

	/**
	 * @return the mediaId
	 */
	public List<String> getMediaIds() {
		return mediaId;
	}

	/**
	 * @param mediaId the mediaId to set
	 */
	public void setMediaIds(List<String> mediaId) {
		this.mediaId = mediaId;
	}
	
	/**
	 *  Add a single media id to the MediaIds List
	 * @param mediaId
	 */
	public void addMediaId(String mediaId) {
		this.mediaId.add(mediaId);
	}

	/**
	 * @return the mimeType
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * @param mimeType the mimeType to set
	 */
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	/**
	 * @return the mediaLocation
	 */
	public String getMediaLocation() {
		return mediaLocation;
	}

	/**
	 * @param mediaLocation the mediaLocation to set
	 */
	public void setMediaLocation(String mediaLocation) {
		this.mediaLocation = mediaLocation;
	}

	/**
	 * @return the log
	 */
	public static Logger getLog() {
		return log;
	}

	/**
	 * @param log the log to set
	 */
	public static void setLog(Logger log) {
		PostVO.log = log;
	}

	/**
	 * @return the mediaId
	 */
	public List<String> getMediaId() {
		return mediaId;
	}

	/**
	 * @param mediaId the mediaId to set
	 */
	public void setMediaId(List<String> mediaId) {
		this.mediaId = mediaId;
	}

	/**
	 * @param postDate the postDate to set
	 */
	public void setPostDate(Date postDate) {
		this.postDate = postDate;
	}

}
