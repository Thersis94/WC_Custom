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
	 * All ticket data attributes will be prefixed with this value
	 */
	public static final String ATTRIBUTE_PREFIX = "attr_";

	/**
	 * Number of random chars for the ticket text 
	 */
	public static final int TICKET_RANDOM_CHARS = 8;

	/**
	 * WSLA Resource Bundle
	 */
	public static final String RESOURCE_BUNDLE = "com.smt.sitebuilder.resource.bundle.wsla.DatabaseList";
	
	/**
	 * WSLA Database Resource Bundle ID
	 */
	public static final String RESOURCE_BUNDLE_ID = "WSLA_BUNDLE";

	/**
	 * Workflow slug's to lookup a workflow
	 */
	public enum WorkflowSlug {
		WSLA_NOTIFICATION
	}
	
	/**
	 * "References" email header suffix for tickets, to denote where the
	 * reference came from in a chain of references. 
	 */
	public static final String TICKET_EMAIL_REFERENCE_SUFFIX = "@WSLA-Ticket";


	/**
	 * Loaded in the Shipment/Logistics JSPs as a hidden form field.  Fine as-is until the day 
	 * comes to expose 'source' as a selectable option - which is a phase-2 upgrade.
	 * TODO change this to the actual WSLA warehouse according to the scripted data import
	 */
	public static final String DEFAULT_SHIPPING_SRC = "b89e4d5a3e2c439f879a25aee66bedde";


	private WSLAConstants() {
		//don't need a constructor - static class
	}
}
