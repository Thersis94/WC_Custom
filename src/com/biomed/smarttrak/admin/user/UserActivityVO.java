package com.biomed.smarttrak.admin.user;

//Java 7
import java.util.Calendar;
import java.util.Date;
import java.util.List;

// SMTBaseLibs
import com.siliconmtn.security.UserDataVO;

// WebCrescendo
import com.smt.sitebuilder.util.PageViewVO;

/*****************************************************************************
 <p><b>Title</b>: UserActivityVO.java</p>
 <p><b>Description: </b>Bean that contains a user's session activity and page activity
 log for a given time interval.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Jan 13, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserActivityVO {

	private static final int MILLIS_MINUTE = 1000 * 60;
	private static final int MILLIS_HOUR = MILLIS_MINUTE * 60; 
	private String siteId;
	private String sessionId;
	private UserDataVO profile;
	private List<PageViewVO> pageViews;
	private Date lastAccessTime;
	private long lastAccessHours;
	private long lastAccessMinutes;
	
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
		if (profile == null) profile = new UserDataVO();
		return profile;
	}
	/**
	 * @param profile the profile to set
	 */
	public void setProfile(UserDataVO profile) {
		this.profile = profile;
	}
	/**
	 * @return the pageViews
	 */
	public List<PageViewVO> getPageViews() {
		return pageViews;
	}
	/**
	 * @param pageViews the pageViews to set
	 */
	public void setPageViews(List<PageViewVO> pageViews) {
		this.pageViews = pageViews;
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
	 * @return the lastAccessTime
	 */
	public Date getLastAccessTime() {
		return lastAccessTime;
	}
	/**
	 * @param lastAccessTime the lastAccessTime to set
	 */
	public void setLastAccessTime(Date lastAccessTime) {
		this.lastAccessTime = lastAccessTime;
		if (lastAccessTime != null)
			formatLastAccessDisplayText();
	}
	/**
	 * @param lastAccessTime the lastAccessTime to set
	 */
	public void setLastAccessTime(long lastAccessTimeInMillis) {
		if (lastAccessTimeInMillis < 0)
			setLastAccessTime(null);
		else
			setLastAccessTime(new Date(lastAccessTimeInMillis));
	}
	/**
	 * @return the lastAccessTime
	 */
	public long getLastAccessTimeInMillis() {
		if (lastAccessTime == null) return -1;
		return lastAccessTime.getTime();
	}

	/**
	 * Calculate the last accessed time hours and minutes display values 
	 * for use by the JSTL view. 
	 */
	private void formatLastAccessDisplayText() {
		long now = Calendar.getInstance().getTimeInMillis();
		long diffTime = now - lastAccessTime.getTime();
		lastAccessHours = diffTime/MILLIS_HOUR;
		lastAccessMinutes = (diffTime%MILLIS_HOUR)/MILLIS_MINUTE;
	}
	/**
	 * Returns the number of hours ago that the user last generated activity
	 * @return
	 */
	public long getLastAccessDisplayHours() {
		return lastAccessHours;
	}
	/**
	 * Returns the number of minutes ago (within the last hour) that the user last generated activity.
	 * This is used in conjunctionn with lastAccessDisplayHours and represents the remainder of time
	 * left over after calculating the number of hours ago that the user last generated activity.
	 * @return
	 */
	public long getLastAccessDisplayMinutes() {
		return lastAccessMinutes;
	}
	/**
	 * Helper method for retrieving user's profile Id.
	 * @return
	 */
	public String getProfileId() {
		return getProfile().getProfileId();
	}
	/**
	 * Helper method for setting profile ID
	 * @param profileId
	 */
	public void setProfileId(String profileId) {
		getProfile().setProfileId(profileId);
	}
	/**
	 * Helper method for retrieving user's first name.
	 * @return
	 */
	public String getFirstName() {
		return getProfile().getFirstName();
	}
	/**
	 * Helper method for setting first name
	 * @param firstName
	 */
	public void setFirstName(String firstName) {
		getProfile().setFirstName(firstName);
	}
	/**
	 * Helper method for retrieving user's last name.
	 * @return
	 */
	public String getLastName() {
		return getProfile().getLastName();
	}
	/**
	 * Helper method for setting last name
	 * @param lastName
	 */
	public void setLastName(String lastName) {
		getProfile().setLastName(lastName);
	}
}
