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
	OPENED ("Ticket Opened"),
	EXISTING_TICKET ("Existing Ticket"),
	EXPIRED_WARRANTY ("Expired Warranty"),
	USER_CALL_DATA_INCOMPLETE ("User Call Data Incomplete"),
	USER_DATA_COMPLETE ("User Data Complete"),
	PROBLEM_RESOLVED ("Problem Resolved"),
	PROBLEM_UNRESOLVED ("Problem Unresolved"),
	CAS_ASSIGNED ("CAS Assigned"),
	USER_DATA_INCOMPLETE ("User Ticket Incomplete Data"),
	USER_DATA_APPROVAL_PENDING ("Approval Pending"),
	UNSUPPORTED_PRODUCT ("Unsupported Product"),
	UNLISTED_SERIAL_NO ("Unlisted Serial Number"),
	MISSING_SERIAL_NO ("Serial Number Not Provided"),
	DECLINED_SERIAL_NO ("Declined Serial Number"),
	PENDING_PICKUP ("Pickup Pending"),
	PICKUP_COMPLETE ("Pickup Complete"),
	CAS_IN_DIAG ("CAS Diagnotics"),
	REPAIRABLE ("Repairable"),
	UNREPAIRABLE ("Unrepairable"),
	CAS_PARTS_REQUESTED ("CAS Parts Requested"),
	CAS_PARTS_ORDERED ("CAS Ordered PARTS"),
	PARTS_SHIPPED_CAS ("Parts Shipped to CAS"),
	PARTS_RCVD_CAS ("Parts Received by CAS"),
	CAS_IN_REPAIR ("In Repair by CAS"),
	CAS_REPAIR_COMPLETE ("CAS Repair Complete"),
	PENDING_UNIT_RETURN ("Pending Equipment Return"),
	DELIVERY_SCHEDULED ("Delivery Scheduled"),
	DELIVERY_COMPLETE ("Delivery Complete"),
	PARTS_OUT_STOCK ("Parts Out of Stock"),
	OEM_PARTS_ORDERED ("Parts Backordered with OEM"),
	OEM_PARTS_RCVD ("OEM Parts Received"),
	OEM_PARTS_SHIPPED ("Parts Shipped"),
	REPLACEMENT_REQUEST ("Replacement Requested"),
	REFUND_REQUEST ("Refund Requested"),
	REFUND_DENIED ("Refund Denied"),
	REFUND_APPROVED ("Refund Approved"),
	REPLACEMENT_CONFIRMED ("Replacement Confirmed"),
	REPLACEMENT_UNAVAILABLE ("Replacement Unavailable"),
	RETURN_RECVD("Return Received"),
	RPLC_DELIVERY_SCHED ("Replacement Delivery Scheduled"),
	RPLC_DELIVEY_RCVD ("Replacment Delivery Received"),
	CREDIT_MEMO_STORE ("Credit Memo - Store"),
	CREDIT_MEMO_OEM ("Credit Memo - OEM"),
	CREDIT_MEMO_WSLA ("Credit Memo - WSLA"),
	REFUND_COMPLETE ("Refund Complete"),
	DEFECTIVE_SHIPPED ("Defective Shipped"),
	DEFECTIVE_RCVD ("Defective Received"),
	DISPOSE_UNIT ("Unit disposed of"),
	HARVEST_REQ ("Harvest Requested"),
	HARVEST_DENIED ("Harvest Denied"),
	HARVEST_DENIED_SHIPPED ("Harvest Denied Shipped"),
	HARVEST_APPROVED ("Harvest Approved"),
	HARVEST_COMPLETE("Harvest Complete"),
	HARVEST_RETURN("Return to OEM"),
	HARVEST_REPAIR("Repair Product"),
	PRODUCT_DECOMMISSIONED("Product already decommissioned"),
	SERVICE_ORDER_NUMBER_LOOKUP("Service Order Number Look Up"),
	CLOSED ("Ticket Closed");
	
	public final String codeName;
	StatusCode(String codeName) { this.codeName = codeName; }

}

