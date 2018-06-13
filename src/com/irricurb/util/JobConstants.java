package com.irricurb.util;

/****************************************************************************
 * <b>Title</b>: JobConstants.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Set of constants matching to the keys in the IC_JOB_ATTRIBUTE
 * table.  This file will be static only, with 2 sections, one section for admin 
 * constants (those constants utilized by the job scheduler) and those constants
 * that may be utilized inside a scheduled job 
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jun 4, 2018
 * @updates:
 ****************************************************************************/

public class JobConstants {

	/**
	 * 
	 */
	private JobConstants() {
		super();
	}

	/**** Admin Constants **/
	
	public static final String JOB_NAME = "JOB_NAME";
	public static final String JOB_GROUP = "JOB_GROUP";
	public static final String TRIGGER_NAME = "TRIGGER_NAME";
	public static final String TRIGGER_GROUP = "TRIGGER_GROUP";
	public static final String INTERVAL = "INTERVAL";
	public static final String DURATION_COUNT = "DURATION_COUNT";
	public static final String SEND_PORTAL = "SEND_PORTAL";
	
	/**** Job Constants **/
	public static final String SOIL_MOISTURE_LEVEL = "SOIL_MOISTURE_LEVEL";
	public static final String LIGHT_LEVEL = "LIGHT_LEVEL";
}

