package com.wsla.data.ticket;

/****************************************************************************
 * <b>Title</b>: LedgerSummary.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> List of standard summary elements to be added to the ledger.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 29, 2018
 * @updates:
 ****************************************************************************/

public enum LedgerSummary {
	
	CALL_RECVD ("A call was received"),
	CALL_FINISHED ("Finished user call.  All data processed"),
	ASSET_LOADED ("User loaded an asset to the service order"),
	SCHEDULE_TRANSFER_COMPLETE ("Equipment was picked up or dropped off for the service order");
	RAN_DIAGNOSTIC ("A diagnostic was performed"),
	ACTIVITY_ADDED ("An activity was added to the ticket"),
	ASSET_LOADED ("User loaded an asset to the service order");
	
	public final String summary;
	LedgerSummary(String summary) { this.summary = summary; }
}

