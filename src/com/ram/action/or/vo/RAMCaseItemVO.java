package com.ram.action.or.vo;

import java.util.Date;

import com.ram.action.or.RAMCaseManager;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;

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
@Table(name="ram_case_item")
public class RAMCaseItemVO {

	public enum RAMCaseType {}

	private String caseItemId;
	private String productId;
	private String caseKitId;
	private String caseId;
	private int qtyNo;
	private RAMCaseType caseType;
	private Date createDt;
	private Date updateDt;

	public RAMCaseItemVO() {
		
	}

	public RAMCaseItemVO(ActionRequest req) {
		this();
		setData(req);
	}

	private void setData(ActionRequest req) {
		caseItemId = req.getParameter("caseItemId");
		productId = req.getParameter("productId");
		caseKitId = req.getParameter("caseKitId");
		caseId = req.getParameter(RAMCaseManager.RAM_CASE_ID);
		qtyNo = Convert.formatInteger("qtyNo");
		setCaseTypeTxt(req.getParameter("caseTypeCd"));
	}

	/**
	 * @return the caseItemId
	 */
	@Column(name="case_item_id", isPrimaryKey=true)
	public String getCaseItemId() {
		return caseItemId;
	}

	/**
	 * @return the productId
	 */
	@Column(name="product_id")
	public String getProductId() {
		return productId;
	}

	/**
	 * @return the caseKitId
	 */
	@Column(name="case_kit_id")
	public String getCaseKitId() {
		return caseKitId;
	}

	/**
	 * @return the caseId
	 */
	@Column(name="case_id")
	public String getCaseId() {
		return caseId;
	}

	/**
	 * @return the qtyNo
	 */
	@Column(name="qty_no")
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
	 * @return the caseType
	 */
	@Column(name="case_type_cd")
	public String getCaseTypeTxt() {
		return caseType.toString();
	}

	/**
	 * @return the createDt
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @return the updateDt
	 */
	@Column(name="update_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDt() {
		return updateDt;
	}

	/**
	 * @param caseItemId the caseItemId to set.
	 */
	public void setCaseItemId(String caseItemId) {
		this.caseItemId = caseItemId;
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
	 * @param caseId the caseId to set.
	 */
	public void setCaseId(String caseId) {
		this.caseId = caseId;
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

	/**
	 * @param caseType the caseType to set.
	 */
	public void setCaseTypeTxt(String caseType) {
		try{
			this.caseType = RAMCaseType.valueOf(caseType);
		} catch(Exception e) {
			//Throw away exception.
		}
	}

	/**
	 * @param createDt the createDt to set.
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}

	/**
	 * @param updateDt the updateDt to set.
	 */
	public void setUpdateDt(Date updateDt) {
		this.updateDt = updateDt;
	}

	/**
	 * @param qtyNo2
	 */
	public void addQty(int qtyNo) {
		this.qtyNo += qtyNo;
	}
}