package com.biomed.smarttrak.admin.user;

// Java 7
import java.util.List;

// SMTBaseLibs
import com.siliconmtn.security.UserDataVO;

// WebCrescendo
import com.smt.sitebuilder.common.PageVO;

/*****************************************************************************
 <p><b>Title</b>: UserActivityVO.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author groot
 @version 1.0
 @since Jan 13, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserActivityVO {

	private String siteId;
	private String sessionId;
	private UserDataVO profile;
	private List<PageVO> pages;
	private long sessionLastAccessed;
	/**
	 * @return the siteId
	 */
	public String getSiteId() {
		return siteId;
	}
	/**
	 * @param siteId the siteId to set
	 */
	public void setSiteId(String siteId) {
		this.siteId = siteId;
	}
	/**
	 * @return the profile
	 */
	public UserDataVO getProfile() {
		return profile;
	}
	/**
	 * @param profile the profile to set
	 */
	public void setProfile(UserDataVO profile) {
		this.profile = profile;
	}
	/**
	 * @return the pages
	 */
	public List<PageVO> getPages() {
		return pages;
	}
	/**
	 * @param pages the pages to set
	 */
	public void setPages(List<PageVO> pages) {
		this.pages = pages;
	}
	/**
	 * @return the sessionId
	 */
	public String getSessionId() {
		return sessionId;
	}
	/**
	 * @param sessionId the sessionId to set
	 */
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	/**
	 * @return the sessionLastAccessed
	 */
	public long getSessionLastAccessed() {
		return sessionLastAccessed;
	}
	/**
	 * @param sessionLastAccessed the sessionLastAccessed to set
	 */
	public void setSessionLastAccessed(long sessionLastAccessed) {
		this.sessionLastAccessed = sessionLastAccessed;
	}
	
}
