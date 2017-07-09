package com.ram.action.or.vo;

import java.io.Serializable;
import java.util.Date;

import com.ram.action.or.RAMCaseManager;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title:</b> RAMCaseKit.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Class manages RAMCaseKit Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jun 30, 2017
 ****************************************************************************/
@Table(name="RAM_CASE_KIT")
public class RAMCaseKitVO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String caseKitId;
	private String locationItemMasterId;
	private String caseId;
	private Date createDt;
	private Date updateDt;
	private int productId;
	private String productNm;
	private String serialNoTxt;
	
		

	public RAMCaseKitVO() {
		
	}

	public RAMCaseKitVO(ActionRequest req) {
		this();
		setData(req);
	}

	public void setData(ActionRequest req) {
		caseKitId = req.getParameter("caseKitId");
		locationItemMasterId = req.getParameter("locationItemMasterId");
		caseId = req.getParameter(RAMCaseManager.RAM_CASE_ID);
	}

	/**
	 * @return the caseKitId
	 */
	@Column(name="case_kit_id", isPrimaryKey=true)
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
	 * @return the itemMasterId
	 */
	@Column(name="location_item_master_id")
	public String getLocationItemMasterId() {
		return locationItemMasterId;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @return the updateDt
	 */
	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDt() {
		return updateDt;
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
	 * @param itemMasterId the itemMasterId to set.
	 */
	public void setLocationItemMasterId(String locationItemMasterId) {
		this.locationItemMasterId = locationItemMasterId;
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
	 * @return the productId
	 */
	@Column(name="product_id", isReadOnly=true)
	public int getProductId() {
		return productId;
	}

	/**
	 * @param productId the productId to set
	 */
	public void setProductId(int productId) {
		this.productId = productId;
	}

	/**
	 * @return the productNm
	 */
	@Column(name="product_nm", isReadOnly=true)
	public String getProductNm() {
		return productNm;
	}

	/**
	 * @param productNm the productNm to set
	 */
	public void setProductNm(String productNm) {
		this.productNm = productNm;
	}

	/**
	 * @return the serialNoTxt
	 */
	@Column(name="serial_no_txt", isReadOnly=true)
	public String getSerialNoTxt() {
		return serialNoTxt;
	}

	/**
	 * @param serialNoTxt the serialNoTxt to set
	 */
	public void setSerialNoTxt(String serialNoTxt) {
		this.serialNoTxt = serialNoTxt;
	}
}