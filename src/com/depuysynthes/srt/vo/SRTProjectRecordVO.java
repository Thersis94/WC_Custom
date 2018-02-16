package com.depuysynthes.srt.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> SRTProjectRecordVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Stores ProjectRecordVO
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 5, 2018
 ****************************************************************************/
public class SRTProjectRecordVO {

	private String projectId;
	private String projectName;
	private String projectType;
	private String priority;
	private String hospitalPONo;
	private String specialInstructions;
	private List<SRTNoteVO> notes;
	private String projectStatus;
	private BigDecimal actualRoi;
	private String srtContact;
	private String engineerId;
	private String designerId;
	private String qualityEngineerId;
	private boolean makeFromScratch;
	private String funcCheckOrderNo;
	private String makeFromOrderNo;
	private String buyerId;
	private String mfgPOToVendor;
	private String supplierId;
	private boolean projectHold;
	private boolean projectCancelled;
	private String warehouseTrackingNo;
	private String mfgDtChangeReason;
	private String warehouseSalesOrderNo;
	private Date createDt;
	private Date updateDt;

	private SRTMasterRecordVO masterRecord;
	private SRTRequestVO reqVO;
	private Map<String, Date> milestones;

	public SRTProjectRecordVO() {
		notes = new ArrayList<>();
		milestones = new HashMap<>();
	}

	/**
	 * @return the projectId
	 */
	@Column(name="PROJECT_ID", isPrimaryKey=true)
	public String getProjectId() {
		return projectId;
	}

	
	/**
	 * @return the projectName
	 */
	@Column(name="PROJECT_NAME")
	public String getProjectName() {
		return projectName;
	}

	/**
	 * @return the projectType
	 */
	@Column(name="PROJECT_TYPE_ID")
	public String getProjectType() {
		return projectType;
	}

	/**
	 * @return the priority
	 */
	@Column(name="PRIORITY_ID")
	public String getPriority() {
		return priority;
	}

	/**
	 * @return the hospitalPONo
	 */
	@Column(name="HOSPITAL_PO")
	public String getHospitalPONo() {
		return hospitalPONo;
	}

	/**
	 * @return the specialInstructions
	 */
	@Column(name="SPECIAL_INSTRUCTIONS")
	public String getSpecialInstructions() {
		return specialInstructions;
	}

	/**
	 * @return the notes
	 */
	public List<SRTNoteVO> getNotes() {
		return notes;
	}

	/**
	 * @return the projectStatus
	 */
	@Column(name="PROJECT_STATUS_ID")
	public String getProjectStatus() {
		return projectStatus;
	}

	/**
	 * @return the actualRoi
	 */
	public BigDecimal getActualRoi() {
		return actualRoi;
	}

	@Column(name="ACTUAL_ROI")
	public double getActualRoiDbl() {
		return actualRoi != null ? actualRoi.doubleValue() : BigDecimal.ZERO.doubleValue();
	}

	/**
	 * @return the srtContact
	 */
	@Column(name="SRT_CONTACT")
	public String getSrtContact() {
		return srtContact;
	}

	/**
	 * @return the engineerId
	 */
	@Column(name="ENGINEER_ID")
	public String getEngineerId() {
		return engineerId;
	}

	/**
	 * @return the designerId
	 */
	@Column(name="DESIGNER_ID")
	public String getDesignerId() {
		return designerId;
	}

	/**
	 * @return the qualityEngineerId
	 */
	@Column(name="QUALITY_ENGINEER_ID")
	public String getQualityEngineerId() {
		return qualityEngineerId;
	}

	/**
	 * @return the makeFromScratch
	 */
	@Column(name="MAKE_FROM_SCRATCH_NO")
	public boolean isMakeFromScratch() {
		return makeFromScratch;
	}

	/**
	 * @return the funcCheckOrderNo
	 */
	@Column(name="FUNCT_CHECK_ORDER_NO")
	public String getFuncCheckOrderNo() {
		return funcCheckOrderNo;
	}

	/**
	 * @return the makeFromOrderNo
	 */
	@Column(name="MAKE_FROM_ORDER_NO")
	public String getMakeFromOrderNo() {
		return makeFromOrderNo;
	}

	/**
	 * @return the buyerId
	 */
	@Column(name="BUYER_ID")
	public String getBuyerId() {
		return buyerId;
	}

	/**
	 * @return the mfgPOToVendor
	 */
	@Column(name="MFG_PO_TO_VENDOR")
	public String getMfgPOToVendor() {
		return mfgPOToVendor;
	}

	/**
	 * @return the supplierId
	 */
	@Column(name="SUPPLIER_ID")
	public String getSupplierId() {
		return supplierId;
	}

	/**
	 * @return the projectHold
	 */
	@Column(name="PROJECT_HOLD_FLG")
	public boolean isProjectHold() {
		return projectHold;
	}

	/**
	 * @return the projectCancelled
	 */
	@Column(name="PROJECT_CANCELLED_FLG")
	public boolean isProjectCancelled() {
		return projectCancelled;
	}

	/**
	 * @return the warehouseTrackingNo
	 */
	@Column(name="WAREHOUSE_TRACKING_NO")
	public String getWarehouseTrackingNo() {
		return warehouseTrackingNo;
	}

	/**
	 * @return the mfgDtChangeReason
	 */
	@Column(name="MFG_DT_CHG_REASON_ID")
	public String getMfgDtChangeReason() {
		return mfgDtChangeReason;
	}

	/**
	 * @return the warehouseSalesOrderNo
	 */
	@Column(name="WAREHOUSE_SALES_NO")
	public String getWarehouseSalesOrderNo() {
		return warehouseSalesOrderNo;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="CREATE_DT", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @return the updateDt
	 */
	@Column(name="UPDATE_DT", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDt() {
		return updateDt;
	}

	/**
	 * @return the masterRecord
	 */
	public SRTMasterRecordVO getMasterRecord() {
		return masterRecord;
	}

	/**
	 * @return the masterRecord Id
	 */
	@Column(name="MASTER_RECORD_ID")
	public String getMasterRecordId() {
		return masterRecord != null ? masterRecord.getMasterRecordId() : "";
	}

	/**
	 * @return the reqVO
	 */
	public SRTRequestVO getReqVO() {
		return reqVO;
	}

	/**
	 * @return the SRT Request ID
	 */
	@Column(name="REQUEST_ID")
	public String getRequestId() {
		return reqVO != null ? reqVO.getRequestId() : "";
	}
	/**
	 * @return the milestones
	 */
	public Map<String, Date> getMilestones() {
		return milestones;
	}

	/**
	 * @param milestoneId the id of the Milestone to retrieve.
	 * @return
	 */
	public Date getMilestone(String milestoneId) {
		return milestones.get(milestoneId);
	}

	/**
	 * @param projectId the projectId to set.
	 */
	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	/**
	 * @param projectName the projectName to set.
	 */
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	/**
	 * @param projectType the projectType to set.
	 */
	public void setProjectType(String projectType) {
		this.projectType = projectType;
	}

	/**
	 * @param priority the priority to set.
	 */
	public void setPriority(String priority) {
		this.priority = priority;
	}

	/**
	 * @param hospitalPONo the hospitalPONo to set.
	 */
	public void setHospitalPONo(String hospitalPONo) {
		this.hospitalPONo = hospitalPONo;
	}

	/**
	 * @param specialInstructions the specialInstructions to set.
	 */
	public void setSpecialInstructions(String specialInstructions) {
		this.specialInstructions = specialInstructions;
	}

	/**
	 * @param notes the notes to set.
	 */
	public void setNotes(List<SRTNoteVO> notes) {
		this.notes = notes;
	}

	/**
	 * @param note the note to add.
	 */
	@BeanSubElement
	public void addNote(SRTNoteVO note) {
		if(note != null) {
			notes.add(note);
		}
	}

	/**
	 * @param projectStatus the projectStatus to set.
	 */
	public void setProjectStatus(String projectStatus) {
		this.projectStatus = projectStatus;
	}

	/**
	 * @param actualRoi the actualRoi to set.
	 */
	public void setActualRoi(BigDecimal actualRoi) {
		this.actualRoi = actualRoi;
	}

	/**
	 * @param srtContact the srtContact to set.
	 */
	public void setSrtContact(String srtContact) {
		this.srtContact = srtContact;
	}

	/**
	 * @param engineerId the engineerId to set.
	 */
	public void setEngineerId(String engineerId) {
		this.engineerId = engineerId;
	}

	/**
	 * @param designerId the designerId to set.
	 */
	public void setDesignerId(String designerId) {
		this.designerId = designerId;
	}

	/**
	 * @param qualityEngineerId the qualityEngineerId to set.
	 */
	public void setQualityEngineerId(String qualityEngineerId) {
		this.qualityEngineerId = qualityEngineerId;
	}

	/**
	 * @param makeFromScratch the makeFromScratch to set.
	 */
	public void setMakeFromScratch(boolean makeFromScratch) {
		this.makeFromScratch = makeFromScratch;
	}

	/**
	 * @param funcCheckOrderNo the funcCheckOrderNo to set.
	 */
	public void setFuncCheckOrderNo(String funcCheckOrderNo) {
		this.funcCheckOrderNo = funcCheckOrderNo;
	}

	/**
	 * @param makeFromOrderNo the makeFromOrderNo to set.
	 */
	public void setMakeFromOrderNo(String makeFromOrderNo) {
		this.makeFromOrderNo = makeFromOrderNo;
	}

	/**
	 * @param buyerId the buyerId to set.
	 */
	public void setBuyerId(String buyerId) {
		this.buyerId = buyerId;
	}

	/**
	 * @param mfgPOToVendor the mfgPOToVendor to set.
	 */
	public void setMfgPOToVendor(String mfgPOToVendor) {
		this.mfgPOToVendor = mfgPOToVendor;
	}

	/**
	 * @param supplierId the supplierId to set.
	 */
	public void setSupplierId(String supplierId) {
		this.supplierId = supplierId;
	}

	/**
	 * @param projectHold the projectHold to set.
	 */
	public void setProjectHold(boolean projectHold) {
		this.projectHold = projectHold;
	}

	/**
	 * @param projectCancelled the projectCancelled to set.
	 */
	public void setProjectCancelled(boolean projectCancelled) {
		this.projectCancelled = projectCancelled;
	}

	/**
	 * @param warehouseTrackingNo the warehouseTrackingNo to set.
	 */
	public void setWarehouseTrackingNo(String warehouseTrackingNo) {
		this.warehouseTrackingNo = warehouseTrackingNo;
	}

	/**
	 * @param mfgDtChangeReason the mfgDtChangeReason to set.
	 */
	public void setMfgDtChangeReason(String mfgDtChangeReason) {
		this.mfgDtChangeReason = mfgDtChangeReason;
	}

	/**
	 * @param warehouseSalesOrderNo the warehouseSalesOrderNo to set.
	 */
	public void setWarehouseSalesOrderNo(String warehouseSalesOrderNo) {
		this.warehouseSalesOrderNo = warehouseSalesOrderNo;
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
	 * @param masterRecord the masterRecord to set.
	 */
	@BeanSubElement
	public void setMasterRecord(SRTMasterRecordVO masterRecord) {
		this.masterRecord = masterRecord;
	}

	/**
	 * @param reqVO the reqVO to set.
	 */
	@BeanSubElement
	public void setReqVO(SRTRequestVO reqVO) {
		this.reqVO = reqVO;
	}

	/**
	 * @param milestones the milestones to set.
	 */
	public void setMilestones(Map<String, Date> milestones) {
		this.milestones = milestones;
	}

	/**
	 * @param milestoneId - the milestoneId to be added
	 * @param milestoneDt - the date the milstone was achieved.
	 */
	public void addMilestone(String milestoneId, Date milestoneDt) {
		if(!StringUtil.isEmpty(milestoneId)) {
			milestones.put(milestoneId, milestoneDt);
		}
	}
}