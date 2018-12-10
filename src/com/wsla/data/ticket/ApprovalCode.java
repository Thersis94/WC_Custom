package com.wsla.data.ticket;

/****************************************************************************
 * <b>Title</b>: ApprovalCodes.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Approval codes for wsla assets
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Nov 27, 2018
 * @updates:
 ****************************************************************************/
public enum ApprovalCode {
	PENDING(false,-1), APPROVED(false, 0), REJECTED(true, 1);
	boolean rejected;
	int level;
	
	private ApprovalCode(boolean rejected, int level) {
        this.level = level;
        this.rejected = rejected;
    }
	
	public int getLevel() {
		return level;
	}
	
	public boolean isRejected(){
		return rejected;
	}
			
}
