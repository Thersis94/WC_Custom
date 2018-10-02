package com.wsla.common;

/****************************************************************************
 * <b>Title</b>: WSLAConstants.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Commonly Used Constants for the WSLA Portal Project
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 18, 2018
 * @updates:
 ****************************************************************************/

public class WSLAConstants {

	/**
	 * 
	 */
	private WSLAConstants() {
		super();
	}

	/**
	 * All ticket data attributes will be prefixed with this value
	 */
	public static final String ATTRIBUTE_PREFIX = "attr_";
	
	/**
	 * Number of random chars for the ticket text 
	 */
	public static final int TICKET_RANDOM_CHARS = 8;
}

