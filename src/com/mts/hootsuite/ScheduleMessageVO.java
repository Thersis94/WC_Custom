package com.mts.hootsuite;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/****************************************************************************
 * <b>Title</b>: TwitterMessageVO.java
 * <b>Project</b>: Hootsuite
 * <b>Description: </b> VO for the Schedule Message request
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author justinjeffrey
 * @version 3.0
 * @since May 11, 2020
 * @updates:
 ****************************************************************************/
public class ScheduleMessageVO {

	private String text;
	private List<String> socialProfileIds = new ArrayList<>();
	private String scheduledSendTime;
	private List<Map<String, String>> media = new ArrayList<>();
	private List<Map<String, String>> mediaUrls = new ArrayList();
	
	/**
	 * @return the text
	 */
	public String getText() {
		return text;
	}
	/**
	 * @param text the text to set
	 */
	public void setText(String text) {
		this.text = text;
	}
	/**
	 * @return the socialProfiles
	 */
	public List<String> getSocialProfiles() {
		return socialProfileIds;
	}
	/**
	 * @param socialProfiles the socialProfiles to set
	 */
	public void setSocialProfiles(List<String> socialProfiles) {
		this.socialProfileIds = socialProfiles;
	}
	
	/**
	 * @return the media
	 */
	public List<Map<String, String>> getMedia() {
		return media;
	}
	/**
	 * @param mediaList the media to set
	 */
	public void setMedia(List<Map<String, String>> mediaList) {
		this.media = mediaList;
	}
	/**
	 * @return the socialProfileIds
	 */
	public List<String> getSocialProfileIds() {
		return socialProfileIds;
	}
	/**
	 * @param socialProfileIds the socialProfileIds to set
	 */
	public void setSocialProfileIds(List<String> socialProfileIds) {
		this.socialProfileIds = socialProfileIds;
	}
	/**
	 * @return the scheduledSendTime
	 */
	public String getScheduledSendTime() {
		return scheduledSendTime;
	}
	/**
	 * @param scheduledSendTime the scheduledSendTime to set
	 */
	public void setScheduledSendTime(String scheduledSendTime) {
		this.scheduledSendTime = scheduledSendTime;
	}
	/**
	 * @return the mediaUrls
	 */
	public List<Map<String, String>> getMediaUrls() {
		return mediaUrls;
	}
	/**
	 * @param mediaUrls the mediaUrls to set
	 */
	public void setMediaUrls(List<Map<String, String>> mediaUrls) {
		this.mediaUrls = mediaUrls;
	}
	
	
	
	
}
