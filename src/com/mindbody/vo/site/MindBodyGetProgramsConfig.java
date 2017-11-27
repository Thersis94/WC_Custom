package com.mindbody.vo.site;

import com.mindbody.MindBodySiteApi.SiteDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;
import com.mindbodyonline.clients.api._0_5_1.ScheduleType;

/****************************************************************************
 * <b>Title:</b> MindBodyGetProgramsConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages MindBody GetPrograms Endpoint config.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 26, 2017
 ****************************************************************************/
public class MindBodyGetProgramsConfig extends MindBodySiteConfig {

	private ScheduleType scheduleType;
	private boolean onlineOnly;
	/**
	 * @param type
	 * @param source
	 * @param user
	 */
	public MindBodyGetProgramsConfig(MindBodyCredentialVO source) {
		super(SiteDocumentType.GET_PROGRAMS, source, null);
	}
	/**
	 * @return the scheduleType
	 */
	public ScheduleType getScheduleType() {
		return scheduleType;
	}
	/**
	 * @return the onlineOnly
	 */
	public boolean isOnlineOnly() {
		return onlineOnly;
	}
	/**
	 * @param scheduleType the scheduleType to set.
	 */
	public void setScheduleType(ScheduleType scheduleType) {
		this.scheduleType = scheduleType;
	}
	/**
	 * @param onlineOnly the onlineOnly to set.
	 */
	public void setOnlineOnly(boolean onlineOnly) {
		this.onlineOnly = onlineOnly;
	}

	
}
