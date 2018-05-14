package com.irricurb.util;

/****************************************************************************
 * <b>Title</b>: ICConstants.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Constants for the IC Application
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 27, 2018
 * @updates:
 ****************************************************************************/

public class ICConstants {
	
	/**
	 * 
	 */
	private ICConstants() {
		super();
	}
	
	/**
	 * Key in the SC to store information for the project on the controller
	 */
	public static final String IC_PROJECT = "IC_PROJECT";
	
	/**
	 * Key value for the encryption key
	 */
	public static final String IC_ENCRYPT_KEY = "icEncryptKey";

	/**
	 * Key in SC to retrieve the url of the portal
	 */
	public static final String PORTAL_URL_KEY = "portalUrl";
	
	/**
	 * Key in SC to retrieve the security key for the app
	 */
	public static final String SECURITY_KEY = "securityKey";
	
	/**
	 * Key in SC to store the projectId
	 */
	public static final String PROJECT_ID = "projectId";

	/**
	 * Key in SC to store the projectLocationId
	 */
	public static final String PROJECT_LOCATION_ID = "projectLocationId";
	
	
	public static final String CONTROLLER_COMMAND_SERVLET = "controllerCommandServlet";
	
	// Error Response Section
	
	/**
	 * Sets the error code and message for the unauthorized request error
	 */
	public static final String SECURITY_ERROR_RESPONSE = "IC-ERROR-01: Unauthorized Request, Invalid Security Key";
	
	/**
	 * If the request parameters are not correctly passed, return the error message below 
	 */
	public static final String REQUEST_ERROR_RESPONSE = "IC-ERROR-02:you must pass data for the type and json";
	
	/**
	 *  JSON error codes
	 */
	public static final String ERR_JSON_ACTION = "jsonActionError";
}

