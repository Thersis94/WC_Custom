package com.wsla.data.ticket;

/****************************************************************************
 * <b>Title</b>: StatusCode.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> List of available status codes and their descriptions
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 13, 2018
 * @updates:
 ****************************************************************************/

public enum StatusCode {
	OPENED ("Ticket Opened", "", "", false),
	USER_CALL_DATA_INCOMPLETE ("User Call Data Incomplete", "", "", false),
	USER_DATA_COMPLETE ("User Data Complete", "", "", false),
	PROBLEM_RESOLVED ("Problem Resolved", "", "", false),
	PROBLEM_UNRESOLVED ("Problem Unresolved", "", "", false),
	CAS_ASSIGNED ("CAS Assigned", "", "", false),
	USER_DATA_INCOMPLETE ("User Ticket Incomplete Data", "", "", false),
	USER_DATA_APPROVAL_PENDING ("Approval Pending", "", "", false),
	UNSUPPORTED_PRODUCT ("Unsupported Product", "", "", false),
	UNLISTED_SERIAL_NO ("Unlisted Serial Number", "", "", false),
	DECLINED_SERIAL_NO ("Declined Serial Number", "", "", false),
	PENDING_PICKUP ("Pickup Pending", "", "", false),
	PICKUP_COMPLETE ("Pickup Complete", "", "", false),
	REPAIRABLE ("Repairable", "", "", false),
	UNREPAIRABLE ("Unrepairable", "", "", false),
	CAS_PARTS_REQUESTED ("CAS Parts Requested", "", "", false),
	CAS_PARTS_ORDERED ("CAS Ordered PARTS", "", "", false),
	PARTS_SHIPPED_CAS ("Parts Shipped to CAS", "", "", false),
	PARTS_RCVD_CAS ("Parts Received by CAS", "", "", false),
	CAS_IN_REPAIR ("In Repair by CAS", "", "", false),
	DELIVERY_SCHEDULED ("Delivery Scheduled", "", "", false),
	DELIVERY_COMPLETE ("Delivery Complete", "", "", false),
	PARTS_OUT_STOCK ("Parts Out of Stock", "", "", false),
	OEM_PARTS_ORDERED ("Parts Backordered with OEM", "", "", false),
	OEM_PARTS_RCVD ("OEM Parts Received", "", "", false),
	OEM_PARTS_SHIPPED ("Parts Shipped", "", "", false),
	REPLACEMENT_REQUEST ("Replacement Requested", "", "", false),
	REFUND_REQUEST ("Refund Requested", "", "", false),
	REFUND_DENIED ("Refund Denied", "", "", false),
	REFUND_APPROVED ("Refund Approved", "", "", false),
	REPLACEMENT_CONFIRMED ("Replacement Confirmed", "", "", false),
	REPLACEMENT_UNAVAILABLE ("Replacement Unavailable", "", "", false),
	RETURN_RECVD("Return Received", "", "", false),
	RPLC_DELIVERY_SCHED ("Replacement Delivery Scheduled", "", "", false),
	RPLC_DELIVEY_RCVD ("Replacment Delivery Received", "", "", false),
	CREDIT_MEMO_STORE ("Credit Memo - Store", "", "", false),
	CREDIT_MEMO_OEM ("Credit Memo - OEM", "", "", false),
	CREDIT_MEMO_WSLA ("Credit Memo - WSLA", "", "", false),
	REFUND_COMPLETE ("Refund Complete", "", "", false),
	DEFECTIVE_SHIPPED ("Defective Shipped", "", "", false),
	DEFECTIVE_RCVD ("Defective Received", "", "", false),
	HARVEST_REQ ("Harvest Requested", "", "", false),
	HARVEST_DENIED ("Harvest Denied", "", "", false),
	HARVEST_DENIED_SHIPPED ("Harvest Denied Shipped", "", "", false),
	HARVEST_APPROVED ("Harvest Approved", "", "", false),
	HARVEST_COMPLETE("Harvest Complete", "", "", false),
	HARVEST_RETURN("Return to OEM", "", "", false),
	HARVEST_REPAIR("Repair Product", "", "", false),
	CLOSED ("Ticket Closed", "", "", false);
	
	public final String codeName;
	public final String notificationWorklow;
	public final String nextStepUrl;
	public final boolean nextStepNeedsReload;
	
	StatusCode(String codeName, String notificationWorklow, String nextStepUrl, boolean needsReload) {
		this.codeName = codeName;
		this.notificationWorklow = notificationWorklow;
		this.nextStepUrl = nextStepUrl;
		this.nextStepNeedsReload = needsReload;
	}
}

