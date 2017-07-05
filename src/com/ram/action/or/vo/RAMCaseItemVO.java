package com.ram.action.or.vo;

import com.siliconmtn.action.ActionRequest;

/****************************************************************************
 * <b>Title:</b> RamCaseItemVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> VO Manages RAM Case Item Information.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jun 28, 2017
 ****************************************************************************/
public class RAMCaseItemVO {

	public enum RAMCaseType {}

	private String productId;
	private String caseKitId;
	private int qtyNo;
	private RAMCaseType caseType;

	public RAMCaseItemVO() {
		
	}

	public RAMCaseItemVO(ActionRequest req) {
		this();
		setData(req);
	}

	private void setData(ActionRequest req) {
		
	}

	/**
	 * @return the productId
	 */
	public String getProductId() {
		return productId;
	}

	/**
	 * @return the caseKitId
	 */
	public String getCaseKitId() {
		return caseKitId;
	}

	/**
	 * @return the qtyNo
	 */
	public int getQtyNo() {
		return qtyNo;
	}

	/**
	 * @return the caseType
	 */
	public RAMCaseType getCaseType() {
		return caseType;
	}

	/**
	 * @param productId the productId to set.
	 */
	public void setProductId(String productId) {
		this.productId = productId;
	}

	/**
	 * @param caseKitId the caseKitId to set.
	 */
	public void setCaseKitId(String caseKitId) {
		this.caseKitId = caseKitId;
	}

	/**
	 * @param qtyNo the qtyNo to set.
	 */
	public void setQtyNo(int qtyNo) {
		this.qtyNo = qtyNo;
	}

	/**
	 * @param caseType the caseType to set.
	 */
	public void setCaseType(RAMCaseType caseType) {
		this.caseType = caseType;
	}

	public void addQty(int qtyNo) {
		this.qtyNo += qtyNo;
	}
}