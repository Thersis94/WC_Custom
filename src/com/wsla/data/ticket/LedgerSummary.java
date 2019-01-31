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
	SCHEDULE_TRANSFER ("Equipment transfer was scheduled"),
	SCHEDULE_TRANSFER_COMPLETE ("Equipment was picked up or dropped off for the service order"),
	RAN_DIAGNOSTIC ("A diagnostic was performed"),
	DIAGNOSTIC_COMPLETED ("CAS completed the diagnostics"),
	CAS_REQUESTED_PARTS ("The CAS has requested parts for the equipment"),
	PARTS_REQUEST_REVIEWED ("WSLA has reviewed the parts request"),
	PARTS_REQUEST_REJECTED ("WSLA has rejected the parts request"),
	SHIPMENT_CREATED ("A parts shipment has been created"),
	SHIPMENT_RECEIVED ("The CAS has marked the shipment received"),
	VALID_SERIAL_SAVED("The user submitted a valid serial and it was saved to the ticket"),
	SERIAL_UPDATED("The serial number was updated on the ticket"),
	INVALID_SERIAL_SAVED("Invalid Serial number saved to the ticket"),
	SERIAL_APPROVED("The serial number was approved"),
	REPAIR_STATUS_CHANGED ("The repair status was changed"),
	DISPOSITION_CHANGED("The ticket's disposition was changed"),
	ACTIVITY_ADDED ("An activity was added to the ticket"),
	CAS_ASSIGNED ("A Service Center was assigned to the ticket"),
	TICKET_CLONED ("A closed ticket was cloned"),
	TICKET_CLOSED ("The ticket was closed"),
	REFUND_REJECTED ("Refund or replacement has been Rejected"),
	HARVEST_COMPETE ("Harvesting of this tv is complete set up for shipment of SN plate"),
	ASSET_LOADED ("User loaded an asset to the service order"),
	ASSET_REJECTED ("User assets were rejected"),
	ASSET_APPROVED ("User assets were approved"),
	REPAIR_TYPE ("Unit Repair Type");
	
	public final String summary;
	LedgerSummary(String summary) { this.summary = summary; }
}

