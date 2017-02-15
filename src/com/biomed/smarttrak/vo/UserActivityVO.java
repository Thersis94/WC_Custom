package com.biomed.smarttrak.vo;

//Java 8
import java.util.Calendar;
import java.util.Date;

// WebCrescendo
import com.smt.sitebuilder.util.UserPageViewVO;

/*****************************************************************************
 <p><b>Title</b>: UserActivityVO.java</p>
 <p><b>Description: </b>Bean that extends UserPageViewVO and adds certain time-based
 fields required for displaying user activity.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Jan 13, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserActivityVO extends UserPageViewVO {

	private static final int MILLIS_MINUTE = 1000 * 60;
	private static final int MILLIS_HOUR = MILLIS_MINUTE * 60; 
	private Date lastAccessTime;
	private long lastAccessHours;
	private long lastAccessMinutes;
	
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
	 * @param lastAccessTimeinMillis the lastAccessTime to set
	 */
	public void setLastAccessTime(long lastAccessTimeInMillis) {
		if (lastAccessTimeInMillis < 0) {
			setLastAccessTime(null);
		} else {
			setLastAccessTime(new Date(lastAccessTimeInMillis));
		}
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
	public long getLastAccessHours() {
		return lastAccessHours;
	}
	/**
	 * Returns the number of minutes ago (within the last hour) that the user last generated activity.
	 * This is used in conjunction with lastAccessDisplayHours and represents the remainder of time
	 * left over after calculating the number of hours ago that the user last generated activity.
	 * @return
	 */
	public long getLastAccessMinutes() {
		return lastAccessMinutes;
	}
}