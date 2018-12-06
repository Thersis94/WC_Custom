package com.wsla.data.ticket;

/****************************************************************************
 * <b>Title</b>: DispositionCode.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Codes and bundle keys for the ticket dispositions.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Dec 3, 2018
 * @updates:
 ****************************************************************************/

public enum DispositionCode {
	
	NONREPAIRABLE(StatusCode.UNREPAIRABLE, LedgerSummary.DIAGNOSTIC_COMPLETED),
	REPAIRABLE(StatusCode.REPAIRABLE, LedgerSummary.DIAGNOSTIC_COMPLETED),
	REPAIRED(StatusCode.CAS_REPAIR_COMPLETE, LedgerSummary.REPAIR_STATUS_CHANGED);
	
	private StatusCode status;
	private LedgerSummary ledgerSummary;
	
	DispositionCode(StatusCode status, LedgerSummary ledgerSummary) {
		this.status = status;
		this.ledgerSummary = ledgerSummary;
	}
	
	public StatusCode getStatus() {
		return status;
	}
	
	public String getLedgerSummary() {
		return ledgerSummary.summary;
	}
}

