package com.mts.hootsuite;
/****************************************************************************
 * <b>Title</b>: ApproveMessageVO.java
 * <b>Project</b>: Hootsuite
 * <b>Description: </b> VO for the body of a Approve Message Request
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author justinjeffrey
 * @version 3.0
 * @since May 22, 2020
 * @updates:
 ****************************************************************************/
public class ApproveMessageVO {
	private int sequenceNumber;

	/**
	 * @return the sequenceNumber
	 */
	public int getSequenceNumber() {
		return sequenceNumber;
	}

	/**
	 * @param sequenceNumber the sequenceNumber to set
	 */
	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}
	
}
