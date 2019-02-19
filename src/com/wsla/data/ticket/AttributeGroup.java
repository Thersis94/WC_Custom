package com.wsla.data.ticket;

/****************************************************************************
 * <b>Title</b>: AttributeGroup.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Values and codes for the group of wsla attributes on a ticket.
 * Used to select data for the view
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 17, 2018
 * @updates:
 ****************************************************************************/

public enum AttributeGroup {
	
	OVERVIEW("Overview"),
	SERVICE_CENTER("Service Center"),
	END_USER("End User"),
	DIAGNOSTICS("Diagnostics"),
	REFUND_REPLACE("Refund / Replace"),
	TIME_LINE("Timeline");
	
	public final String groupName;
	AttributeGroup(String groupName) { this.groupName = groupName; }
}

