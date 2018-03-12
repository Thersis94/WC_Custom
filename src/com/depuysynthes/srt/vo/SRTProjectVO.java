package com.depuysynthes.srt.vo;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> SRTProjectVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Stores Project Data
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 5, 2018
 ****************************************************************************/
@Table(name="DPY_SYN_SRT_PROJECT")
public class SRTProjectVO extends BeanDataVO {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private String projectId;
	private String requestId;
	private String opCoId;
	private String coProjectId;
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

	//Helper Values.  Not on DB Record
	private String engineerNm;
	private String designerNm;
	private String qualityEngineerNm;
	private String requestorNm;

	private List<SRTMasterRecordVO> masterRecords;
	private SRTRequestVO request;
	private Map<String, SRTProjectMilestoneVO> milestones;

	public SRTProjectVO() {
		notes = new ArrayList<>();
		masterRecords = new ArrayList<>();
		milestones = new LinkedHashMap<>();
	}

	public SRTProjectVO(ActionRequest req) {
		populateData(req);
	}

	public SRTProjectVO(ResultSet rs) {
		populateData(rs);
	}

	/**
	 * @return the projectId
	 */
	@Column(name="PROJECT_ID", isPrimaryKey=true)
	public String getProjectId() {
		return projectId;
	}

	/**
	 * @return the opCoId
	 */
	@Column(name="OP_CO_ID")
	public String getOpCoId() {
		return opCoId;
	}

	/**
	 * @return the coProjectId
	 */
	@Column(name="CO_PROJECT_ID")
	public String getCoProjectId() {
		return coProjectId;
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
	@Column(name="PROJ_TYPE_ID")
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
	@Column(name="proj_stat_id")
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
	 * @return the engineerNm
	 */
	@Column(name="ENGINEER_NM", isReadOnly=true) 
	public String getEngineerNm() {
		return engineerNm;
	}

	/**
	 * @return the designerId
	 */
	@Column(name="DESIGNER_ID")
	public String getDesignerId() {
		return designerId;
	}

	/**
	 * @return the designerNm
	 */
	@Column(name="DESIGNER_NM", isReadOnly=true) 
	public String getDesignerNm() {
		return designerNm;
	}

	/**
	 * @return the qualityEngineerId
	 */
	@Column(name="QUALITY_ENGINEER_ID")
	public String getQualityEngineerId() {
		return qualityEngineerId;
	}

	/**
	 * @return the designerNm
	 */
	@Column(name="QUALITY_ENGINEER_NM", isReadOnly=true) 
	public String getQualityEngineerNm() {
		return qualityEngineerNm;
	}

	/**
	 * @return the makeFromScratch
	 */
	@Column(name="MAKE_FROM_SCRATCH_NO")
	public boolean isMakeFromScratch() {
		return makeFromScratch;
	}

	/**
	 * @return the makeFromScratch
	 */
	@Column(name="MAKE_FROM_SCRATCH_NO")
	public int getMakeFromScratchFlg() {
		return Convert.formatInteger(makeFromScratch);
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
	public boolean isProjectHold() {
		return projectHold;
	}

	/**
	 * @return the projectHold
	 */
	@Column(name="PROJECT_HOLD_FLG")
	public int getProjectHoldFlg() {
		return Convert.formatInteger(projectHold);
	}

	/**
	 * @return the projectCancelled
	 */
	public boolean isProjectCancelled() {
		return projectCancelled;
	}

	/**
	 * @return the projectCancelled
	 */
	@Column(name="PROJECT_CANCELLED_FLG")
	public int getProjectCancelledFlg() {
		return Convert.formatInteger(projectCancelled);
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
	public List<SRTMasterRecordVO> getMasterRecords() {
		return masterRecords;
	}

	/**
	 * @return the reqVO
	 */
	public SRTRequestVO getRequest() {
		return request;
	}

	/**
	 * @return the designerNm
	 */
	@Column(name="REQUESTOR_NM", isReadOnly=true) 
	public String getRequestorNm() {
		return requestorNm;
	}

	/**
	 * @return the SRT Request ID
	 */
	@Column(name="REQUEST_ID", isInsertOnly=true)
	public String getRequestId() {
		return requestId;
	}

	/**
	 * @return the milestones
	 */
	public Map<String, SRTProjectMilestoneVO> getMilestones() {
		return milestones;
	}

	/**
	 * Retrieve latest status off the stack.
	 * @return
	 */
	public String getStatus() {
		if(milestones == null || milestones.size() == 0) {
			return "";
		} else {
			List<SRTProjectMilestoneVO> m = new ArrayList<>(milestones.values());
			return m.get(m.size() - 1).getMilestoneId();
		}
	}

	/**
	 * @param milestoneId the id of the Milestone to retrieve.
	 * @return
	 */
	public SRTProjectMilestoneVO getMilestone(String milestoneId) {
		return milestones.get(milestoneId);
	}

	/**
	 * @param projectId the projectId to set.
	 */
	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	/**
	 * @param opCoId the opCoId to set.
	 */
	public void setOpCoId(String opCoId) {
		this.opCoId = opCoId;
	}

	/**
	 * @param coProjectId the coProjectId to set.
	 */
	public void setCoProjectId(String coProjectId) {
		this.coProjectId = coProjectId;
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
	 * @param actualRoi the actualRow to set.
	 */
	public void setActualRoiDbl(double actualRoi) {
		this.actualRoi = BigDecimal.valueOf(actualRoi);
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
		this.engineerId = StringUtil.checkVal(engineerId, null);
	}

	/**
	 * @param engineerNm the engineerNm to set.
	 */
	public void setEngineerNm(String engineerNm) {
		this.engineerNm = engineerNm;
	}

	/**
	 * @param designerId the designerId to set.
	 */
	public void setDesignerId(String designerId) {
		this.designerId = StringUtil.checkVal(designerId, null);
	}

	/**
	 * @param designerNm the designerNm to set.
	 */
	public void setDesignerNm(String designerNm) {
		this.designerNm = designerNm;
	}

	/**
	 * @param qualityEngineerId the qualityEngineerId to set.
	 */
	public void setQualityEngineerId(String qualityEngineerId) {
		this.qualityEngineerId = StringUtil.checkVal(qualityEngineerId, null);
	}

	/**
	 * @param qualityEngineerNm the qualityEngineerNm to set.
	 */
	public void setQualityEngineerNm(String qualityEngineerNm) {
		this.qualityEngineerNm = qualityEngineerNm;
	}

	/**
	 * @param makeFromScratch the makeFromScratch to set.
	 */
	public void setMakeFromScratch(boolean makeFromScratch) {
		this.makeFromScratch = makeFromScratch;
	}

	/**
	 * @param makeFromScratch the makeFromScratch to set.
	 */
	public void setMakeFromScratchFlg(int makeFromScratch) {
		this.makeFromScratch = Convert.formatBoolean(makeFromScratch);
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
		this.buyerId = StringUtil.checkVal(buyerId, null);
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
	 * @param projectHold the projectHold to set.
	 */
	public void setProjectHoldFlg(int projectHold) {
		this.projectHold = Convert.formatBoolean(projectHold);
	}

	/**
	 * @param projectCancelled the projectCancelled to set.
	 */
	public void setProjectCancelled(boolean projectCancelled) {
		this.projectCancelled = projectCancelled;
	}

	/**
	 * @param projectCancelled the projectCancelled to set.
	 */
	public void setProjectCancelledFlg(int projectCancelled) {
		this.projectCancelled = Convert.formatBoolean(projectCancelled);
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
	 * @param masterRecords the masterRecords to set.
	 */
	public void setMasterRecord(List<SRTMasterRecordVO> masterRecords) {
		this.masterRecords = masterRecords;
	}

	/**
	 * @param masterRecord the masterRecords to add.
	 */
	@BeanSubElement
	public void addMasterRecord(SRTMasterRecordVO masterRecord) {
		if(masterRecord != null && !StringUtil.isEmpty(masterRecord.getPartNo())) {
			masterRecords.add(masterRecord);
		}
	}

	/**
	 * @param reqVO the request to set.
	 */
	@BeanSubElement
	public void setRequest(SRTRequestVO request) {
		this.request = request;
	}

	/**
	 * @param requestId the requestId to set.
	 */
	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	/**
	 * @param requestorNm the requestorNm to set.
	 */
	public void setRequestorNm(String requestorNm) {
		this.requestorNm = requestorNm;
	}

	/**
	 * @param milestones the milestones to set.
	 */
	public void setMilestones(Map<String, SRTProjectMilestoneVO> milestones) {
		this.milestones = milestones;
	}

	/**
	 * @param milestoneId - the milestoneId to be added
	 * @param milestoneDt - the date the milstone was achieved.
	 */
	@BeanSubElement
	public void addMilestone(SRTProjectMilestoneVO milestone) {
		if(milestone != null && !StringUtil.isEmpty(milestone.getMilestoneId())) {
			milestones.put(milestone.getMilestoneId(), milestone);
		}
	}
}