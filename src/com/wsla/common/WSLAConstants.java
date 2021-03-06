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
	 * the id for the main warehouse for parts, shipping and inv
	 */
	public static final String MAIN_WAREHOUSE_ID = "WSLA_020";
			
	/**
	 * Number of random chars for the ticket text 
	 */
	public static final int TICKET_RANDOM_CHARS = 8;

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
	 * Defines the roles used by this site
	 */
	public enum WSLARole {
		PUBLIC("0", "Public"),
		REGISTERED("10", "RegisteredUser"),
		ADMIN("100", "Site Administrator"),
		WSLA_CALL_CENTER("WSLA_CALL_CENTER", "Call Center Rep"),
		WSLA_CUSTOMER_SVC("WSLA_CUSTOMER_SVC", "Customer Service"),
		WSLA_END_CUSTOMER("WSLA_END_CUSTOMER", "WSLA End Customer"),
		WSLA_EXECUTIVE("WSLA_EXECUTIVE", "WSLA Executive"),
		WSLA_OEM("WSLA_OEM", "Manufacturer"),
		WSLA_PROSPECT("WSLA_PROSPECT", "Prospect"),
		WSLA_RETAILER("WSLA_RETAILER", "Retailer"),
		WSLA_SERVICE_CENTER("WSLA_SERVICE_CENTER", "WSLA Service Center"),
		WSLA_WAREHOUSE("WSLA_WAREHOUSE", "WSLA Warehouse");

		private String roleId;
		private String roleName;

		WSLARole(String roleId, String roleName) {
			this.roleId = roleId;
			this.roleName = roleName;
		}
		public String getRoleId() {
			return roleId;
		}
		public String getRoleName() {
			return roleName;
		}
	}
	/**
	 * Warranty id used for all retail owned equipment over all of wsla
	 */
	public static final String RETAIL_WARRANTY = "RETAILER_OWNED_DEVICE";

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
	public static final String DEFAULT_SHIPPING_SRC = "WSLA_020";

	/**
	 * The provider ID for the WSLA CAS
	 */
	public static final String WSLA_CAS_ID = "WSLA_ID";
	
	/**
	 * Constant to define the value that will be entered by a support rep if the user does
	 * not have an email address.  This is to ensure reps are entering a value for the
	 * email address field
	 */
	public static final String NO_EMAIL_ADDRESS = "none@wsla.com";

	/**
	 * Constant to define the value that will be entered by a support rep if the user does
	 * not have the product serial number handy.  This is to ensure reps are entering a 
	 * value for the serial number field
	 */
	public static final String NO_SERIAL_NUMBER = "NOSN";

	/**
	 * Key used to identify the service order
	 */
	public static final String TICKET_ID = "ticketId";

	/**
	 * Key used to identify the service order number
	 */
	public static final String TICKET_NO = "ticketIdText";
	
	/**
	 * key used to identify a flagtoggle request
	 */
	public static final String FLAG_TOGGLE = "flagToggle";

	/**
	 * 
	 */
	private WSLAConstants() {
		//don't need a constructor - static class
	}
}
