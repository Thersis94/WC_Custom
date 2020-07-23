package com.mts.hootsuite;

//JDK 1.8.x
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//SMT Base Libs
import com.siliconmtn.data.parser.BeanDataVO;

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
public class ScheduleMessageVO extends BeanDataVO {

	// This has to be named 'text' Hootsuite throws a fit if it is named anything else
	private String text;
	private List<String> socialProfileIds = new ArrayList<>();
	private String scheduledSendTime;
	private List<Map<String, String>> media = new ArrayList<>();
	private List<Map<String, String>> mediaUrls = new ArrayList<>();
	
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
	/**
	 * @return the hootsuiteMessageText
	 */
	public String getHootsuiteMessageText() {
		return text;
	}
	/**
	 * @param hootsuiteMessageText the hootsuiteMessageText to set
	 */
	public void setHootsuiteMessageText(String hootsuiteMessageText) {
		this.text = hootsuiteMessageText;
	}
	
}
