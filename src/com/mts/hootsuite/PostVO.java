package com.mts.hootsuite;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import jdk.internal.jline.internal.Log;

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
	
	private String title;
	private String author;
	private String description;
	private String link;
	private Date postDate; // We need to add 1 day to this at some point
	private List<String> mediaId = new ArrayList<>();
	private String mimeType = "";
	private String mediaLocation;
	private String shortURL;
	private String postContent;

	public PostVO() {
		mimeType = "image/jpeg";
		postDate = new Date();
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
			System.out.println(e);
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
	 * @throws IOException 
	 */
	public String getMimeType() throws IOException {
		
		Path mediaFileLocation = Paths.get(mediaLocation);
		
		// If mime type is not set
		if(mimeType.equals("")) {
			// Get and set the mimeType
			mimeType = Files.probeContentType(mediaFileLocation);
		}
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
		
		return mediaLocation.toString();
	}

	/**
	 * @param mediaLocation the mediaLocation to set
	 */
	public void setMediaLocation(String mediaLocation) {
		this.mediaLocation = mediaLocation;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the author
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * @param author the author to set
	 */
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Returns a post formatted to the twitter standard
	 * @return
	 */
	public String getTwitterFormattedString() {
		
		String message;
		
		int maxDescLength = 280 - (title.length() + shortURL.length());
		
		String twitterDesc;
		
		if(description.length()-4 > maxDescLength) {
			twitterDesc = description.substring(0, maxDescLength-5);
			message = title + " " + twitterDesc + "... " + shortURL;
		} else {
			twitterDesc = description;
			message = title + " " + twitterDesc + " " + shortURL;
		}
		return message;
	}
	
	/**
	 * Returns a standard formatted message for Hootsuite
	 * @return
	 */
	public String getStandardFormattedString() {
		
		String message;
		
		int maxDescLength = 700 - (title.length() +  + shortURL.length() + author.length());
		
		String standardDesc;
		
		if(description.length()-4 > maxDescLength) {
			standardDesc = description.substring(0, maxDescLength-5);
			message = standardDesc + "... " + "\n" + title + ", " + author + "\n" + shortURL;
		} else {
			standardDesc = description;
			message = standardDesc + "\n" + title + ", " + author + "\n" + shortURL;
		}
		return message;
		
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the link
	 */
	public String getLink() {
		return link;
	}

	/**
	 * @param link the link to set
	 */
	public void setLink(String link) {
		this.link = link;
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

	/**
	 * @return the shortURL
	 */
	public String getShortURL() {
		return shortURL;
	}

	/**
	 * @param shortURL the shortURL to set
	 */
	public void setShortURL(String shortURL) {
		this.shortURL = shortURL;
	}

	/**
	 * @return the postContent
	 */
	public String getPostContent() {
		return postContent;
	}

	/**
	 * @param postContent the postContent to set
	 */
	public void setPostContent(String postContent) {
		this.postContent = postContent;
	}
	
	// Adds hashtags to the description String
	public void addHashTags(List<String> categories) {
		String descriptionWithHashtags = description;
		
//		List<Integer> indexes = new ArrayList<>();
//		
//		for(String category : categories) {
//			indexes.add(description.toLowerCase().indexOf(category.toLowerCase()));
//		}
//		
//		for(Integer index : indexes) {
//			if(index != -1)
//				descriptionWithHashtags = descriptionWithHashtags.substring(0, index) + "#" + descriptionWithHashtags.substring(index);
//		}
		description = descriptionWithHashtags;
	}

	

	

}
