package com.wsla.data.ticket;

/****************************************************************************
 * <b>Title</b>: TicketOriginCode.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> lists all the pathways that can generate a service order.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Feb 28, 2019
 * @updates:
 ****************************************************************************/
public enum TicketOriginCode {
	
	CALL("Call"), 
	EMAIL("Email"),
	SALESFORCE("Salesforce");
	
	public final String codeName;
	TicketOriginCode(String codeName) { this.codeName = codeName; }
	
}
