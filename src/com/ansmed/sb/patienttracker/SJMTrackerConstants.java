package com.ansmed.sb.patienttracker;

import java.io.Serializable;

/****************************************************************************
 * <b>Title</b>: SJMTrackerConstants.java<p/>
 * <b>Description: Codes used throughout the application</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2005<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Feb 16, 2012
 ****************************************************************************/
public class SJMTrackerConstants implements Serializable {
	
	// private constructor to disallow instantiation
	private SJMTrackerConstants() {}
	
	private static final long serialVersionUID = -4095067457099719295L;
	
	public static final Integer STATUS_PENDING = 10;
	public static final Integer STATUS_ACCEPTED = 20;
	public static final Integer STATUS_IN_PROGRESS = 30;
	public static final Integer STATUS_REQUEST_ASSIST = 40;
	public static final Integer STATUS_REQUEST_REASSIGN = 50;
	public static final Integer STATUS_COMPLAINT = 60;
	public static final Integer STATUS_DR_NAME = 70;
	public static final Integer STATUS_COMPLETED = 100;
	public static final Integer STATUS_EXPIRED = 900;
	public static final Integer STATUS_REASSIGNED = 910;
	public static final Integer STATUS_CLOSED = 999;
	public static final Integer RESPONSE_DEFAULT = 1;
	public static final Integer RESPONSE_ACCEPTED = 110;
	public static final Integer RESPONSE_DECLINED = 120;
	public static final Integer RESPONSE_NONE_48 = 130;
	public static final Integer RESPONSE_NONE_96 = 140;
	public static final Integer RESPONSE_ADMIN_ASSIGNED = 150;
	public static final Integer RESPONSE_REASSIGNED = 160;
	public static final Integer RESPONSE_COMPLAINT = 170;
	public static final Integer RESPONSE_ADHOC = 180;
	public static final Integer EMAIL_TYPE_FOLLOW_UP = 1000;
	public static final Integer EMAIL_TYPE_NONE_48_NEXT_AMB = 1100;
	public static final Integer EMAIL_TYPE_REP = 1110;
	public static final Integer EMAIL_TYPE_ALTERNATE_ADDRESS = 1115;
	public static final Integer EMAIL_TYPE_TECH_SUPPORT = 1120;
	public static final Integer EMAIL_TYPE_STATUS_IN_PROGRESS_AFTER_ASSIST = 1125;
	public static final Integer EMAIL_TYPE_FIELD_SJM_MEMBER = 1130;
	public static final Integer EMAIL_TYPE_REP_FIELD_FORM_ASSIGN = 1135;
	public static final Integer EMAIL_TYPE_COPY_SJM_TEAM = 1140;
	public static final String TRACKER_ORG_ID = "SJM_AMBASSADORS";
	public static final String TRACKER_ASSIGNEE_ID = "trackerAssigneeId";
	public static final String DEFAULT_SJM_EMAIL_SUFFIX = "@sjm.com";
	public static final String NOTIFY_DATE_RESET_BY_USER = "USER_RESET";
	public static final String NOTIFY_DATE_RESET_BY_SYSTEM = "SYSTEM_RESET";
	
	// email constants used by TrackerMailFormatter class
	public static final String FROM_SENDER = "Assignment.Tracker@sjmneuro.com";
	public static final String AMB_ADMIN_EMAIL_ADDRESS = "LSterling@sjm.com";
	public static final String DEFAULT_EMAIL_TYPE_ALTERNATE_LINK = "https://webmail.sjm.com";
	public static final String ASSIGNMENT_RESPONSE_URL = "http://www.ambassadorcommunity.com/response?process=true&assignmentId=";
	public static final String BASE_TRACKER_URL = "http://www.ambassadorcommunity.com/admin";
	//public static final String ASSIGNMENT_RESPONSE_URL = "http://amb.sb.whiterabbit.com/response?process=true&assignmentId=";
	//public static final String BASE_TRACKER_URL = "http://amb.sb.whiterabbit.com/admin";
	//public static final String ASSIGNMENT_RESPONSE_URL = "http://amb.sbdev.siliconmtn.com/response?process=true&assignmentId=";
	//public static final String BASE_TRACKER_URL = "http://amb.sbdev.siliconmtn.com/admin";
	public static final String INTERACTION_SUMMARY_FIELD_ID = "c0a802376667f35a1ced22dabaacd93b";
	public static final String ASSIGNMENT_NOTES_FIELD_ID = "c0a80241a58b9b378623001ecb1cddf4";
	public static final String NOTIFY_NOTES_FIELD_ID = "c0a80241fbb9dab5895d7d534c79a56";
	
}
