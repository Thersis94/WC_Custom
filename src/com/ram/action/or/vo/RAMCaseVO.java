package com.ram.action.or.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ram.action.or.vo.RAMCaseItemVO.RAMCaseType;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

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
	private String surgeonId;
	private String salesRepId;
	private String hospitalRepId;
	private Date createDt;
	private Date updateDt;
	private Date surgeryDt;
	private Date spdDt;
	private List<RAMSignatureVO> signatures;
	private Map<RAMCaseType, Map<String, RAMCaseItemVO>> items;
	private Map<String, RAMCaseKitVO> kits;
	private RAMCaseStatus caseStatus;

	public RAMCaseVO() {
		this.signatures = new ArrayList<>();
		this.kits = new HashMap<>();
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
	 * @return the surgeonId
	 */
	@Column(name="surgeon_id")
	public String getSurgeonId() {
		return surgeonId;
	}

	/**
	 * @return the salesRepId
	 */
	@Column(name="sales_rep_id")
	public String getSalesRepId() {
		return salesRepId;
	}

	/**
	 * @return the hospitalRepId
	 */
	@Column(name="hospital_rep_id")
	public String getHospitalRepId() {
		return hospitalRepId;
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
	public List<RAMSignatureVO> getSignatures() {
		return signatures;
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
	 * @param surgeonId the surgeonId to set.
	 */
	public void setSurgeonId(String surgeonId) {
		this.surgeonId = surgeonId;
	}

	/**
	 * @param salesRepId the salesRepId to set.
	 */
	public void setSalesRepId(String salesRepId) {
		this.salesRepId = salesRepId;
	}

	/**
	 * @param hospitalRepId the hospitalRepId to set.
	 */
	public void setHospitalRepId(String hospitalRepId) {
		this.hospitalRepId = hospitalRepId;
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
	public void setSignatures(List<RAMSignatureVO> signatures) {
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

	@BeanSubElement
	public void addSignature(RAMSignatureVO signature) {
		if(signature != null)
			signatures.add(signature);
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

			if(imap.containsKey(item.getProductId())) {
				imap.get(item.getProductId()).addQty(item.getQtyNo());
			} else {
				imap.put(item.getProductId(), item);
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