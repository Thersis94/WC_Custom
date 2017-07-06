package com.ram.action.or.vo;

import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.ram.action.or.RAMCaseManager;
import com.ram.action.or.vo.RAMCaseItemVO.RAMCaseType;
import com.ram.action.or.vo.RAMSignatureVO.SignatureType;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title:</b> RAMCaseVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Bean manages RAMCase Information.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jun 28, 2017
 ****************************************************************************/
@Table(name="RAM_CASE")
public class RAMCaseVO {

	public enum RAMCaseStatus {}

	private String caseId;
	private String hospitalId;
	private String operatingRoomId;
	private Date createDt;
	private Date updateDt;
	private Date surgeryDt;
	private Date spdDt;
	private Map<SignatureType, Map<String, RAMSignatureVO>> signatures;
	private Map<RAMCaseType, Map<String, RAMCaseItemVO>> items;
	private Map<String, RAMCaseKitVO> kits;
	private RAMCaseStatus caseStatus;

	public RAMCaseVO() {
		this.kits = new HashMap<>();
		this.signatures = new EnumMap<>(SignatureType.class);
		this.items = new EnumMap<>(RAMCaseType.class);
	}

	public RAMCaseVO(ActionRequest req) {
		this();
		setData(req);
	}

	/**
	 * Helper method that pulls data off the Request to populate the bean.
	 * @param req
	 */
	public void setData(ActionRequest req) {
		caseId = req.getParameter(RAMCaseManager.RAM_CASE_ID);
		hospitalId = req.getParameter("hospitalId");
		operatingRoomId = req.getParameter("operatingRoomId");
		surgeryDt = Convert.formatDate(req.getParameter("surgeryDt"));
		spdDt = Convert.formatDate(req.getParameter("spdDt"));
		setCaseStatusTxt(req.getParameter("caseStatus"));
	}

	/**
	 * @return the caseId
	 */
	@Column(name="case_id", isPrimaryKey=true)
	public String getCaseId() {
		return caseId;
	}

	/**
	 * @return the hospitalId
	 */
	@Column(name="hospital_id")
	public String getHospitalId() {
		return hospitalId;
	}

	/**
	 * @return the operatingRoomId
	 */
	@Column(name="or_id")
	public String getOperatingRoomId() {
		return operatingRoomId;
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
	 * @return the surgeryDt
	 */
	@Column(name="surgery_dt")
	public Date getSurgeryDt() {
		return surgeryDt;
	}

	/**
	 * @return the spdDt
	 */
	@Column(name="spd_dt")
	public Date getSpdDt() {
		return spdDt;
	}

	/**
	 * @return the signatures
	 */
	public Map<SignatureType, Map<String, RAMSignatureVO>> getSignatures() {
		return signatures;
	}

	/**
	 * @param st
	 * @return the Map of signatures for the given type.
	 */
	public Map<String, RAMSignatureVO> getSignatures(SignatureType st) {
		return signatures.get(st);
	}

	/**
	 * @return the kits
	 */
	public Map<String, RAMCaseKitVO> getKits() {
		return kits;
	}

	/**
	 * @return the items
	 */
	public Map<RAMCaseType, Map<String, RAMCaseItemVO>> getItems() {
		return items;
	}

	/**
	 * @return the caseStatus
	 */
	public RAMCaseStatus getCaseStatus() {
		return caseStatus;
	}

	/**
	 * @return the caseStatusTxt
	 */
	public String getCaseStatusTxt() {
		return caseStatus.toString();
	}

	/**
	 * @param caseId the caseId to set.
	 */
	public void setCaseId(String caseId) {
		this.caseId = caseId;
	}

	/**
	 * @param hospitalId the hospitalId to set.
	 */
	public void setHospitalId(String hospitalId) {
		this.hospitalId = hospitalId;
	}

	/**
	 * @param operatingRoomId the operatingRoomId to set.
	 */
	public void setOperatingRoomId(String operatingRoomId) {
		this.operatingRoomId = operatingRoomId;
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
	 * @param surgeryDt the surgeryDt to set.
	 */
	public void setSurgeryDt(Date surgeryDt) {
		this.surgeryDt = surgeryDt;
	}

	/**
	 * @param spdDt the spdDt to set.
	 */
	public void setSpdDt(Date spdDt) {
		this.spdDt = spdDt;
	}

	/**
	 * @param signatures the signatures to set.
	 */
	public void setSignatures(Map<SignatureType, Map<String, RAMSignatureVO>> signatures) {
		this.signatures = signatures;
	}

	/**
	 * @param kits the kits to set.
	 */
	public void setKits(Map<String, RAMCaseKitVO> kits) {
		this.kits = kits;
	}

	/**
	 * @param items the items to set.
	 */
	public void setItems(Map<RAMCaseType, Map<String, RAMCaseItemVO>> items) {
		this.items = items;
	}

	/**
	 * @param caseStatus the caseStatus to set.
	 */
	public void setCaseStatus(RAMCaseStatus caseStatus) {
		this.caseStatus = caseStatus;
	}

	/**
	 * @param caseStatus the caseStatusTxt to set.
	 */
	public void setCaseStatusTxt(String caseStatus) {
		try {
			this.caseStatus = RAMCaseStatus.valueOf(caseStatus);
		} catch(Exception e) {
			//Throw away exception.
		}
	}

	@BeanSubElement
	public void addSignature(RAMSignatureVO signature) {
		if(signature != null) {
			SignatureType st = signature.getSignatureType();
			Map<String, RAMSignatureVO> sigs = signatures.get(st);
			if(sigs == null) {
				sigs = new HashMap<>();
			}
			sigs.put(signature.getProfileId(), signature);
			signatures.put(st, sigs);
		}
	}

	@BeanSubElement
	public void addCaseKit(RAMCaseKitVO kit) {
		if(kit != null) {
			kits.put(kit.getCaseKitId(), kit);
		}
	}

	@BeanSubElement
	public void addItem(RAMCaseItemVO item) {
		if(item != null) {
			Map<String, RAMCaseItemVO> imap = items.get(item.getCaseType());
			if(imap == null) {
				imap = new HashMap<>();
			}

			if(imap.containsKey(item.getCaseItemId())) {
				imap.get(item.getCaseItemId()).addQty(item.getQtyNo());
			} else {
				imap.put(item.getCaseItemId(), item);
			}

			items.put(item.getCaseType(), imap);
		}
	}

	public void removeItem(RAMCaseItemVO item) {
		if(item != null && items.containsKey(item.getCaseType())) {
			items.get(item.getCaseType()).remove(item.getProductId());
		}
	}

	public void removeKit(RAMCaseKitVO kit) {
		if(kit != null && kits.containsKey(kit.getCaseKitId())) {
			kits.remove(kit.getCaseKitId());
		}
	}
}