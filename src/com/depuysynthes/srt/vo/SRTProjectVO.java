package com.depuysynthes.srt.vo;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.depuysynthes.srt.vo.SRTProjectMilestoneVO.MilestoneTypeId;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.workflow.milestones.MilestoneIntfc;

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
public class SRTProjectVO extends BeanDataVO implements MilestoneIntfc<SRTProjectMilestoneVO> {

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
	private String projectTypeTxt;
	private String priority;
	private String hospitalPONo;
	private String specialInstructions;
	private List<SRTNoteVO> notes;
	private String projectStatus;
	private String projectStatusTxt;
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
	private Date surgDt;
	private Date deliveryDt;

	//Helper Values.  Not on DB Record
	private String engineerNm;
	private String designerNm;
	private String qualityEngineerNm;
	private String requestorNm;
	private String buyerNm;
	private String surgeonNm;
	private int total;
	private String distributorship;
	private String supplierNm;

	//Stores if there is a lock.
	private boolean lockStatus;
	private String lockedById;

	private List<SRTMasterRecordVO> masterRecords;
	private SRTRequestVO request;
	private Map<String, SRTProjectMilestoneVO> milestones;
	private Map<String, Date> ledgerDates;

	public SRTProjectVO() {
		notes = new ArrayList<>();
		masterRecords = new ArrayList<>();
		milestones = new LinkedHashMap<>();
		ledgerDates = new LinkedHashMap<>();
	}

	public SRTProjectVO(ActionRequest req) {
		this();
		populateData(req);
	}

	public SRTProjectVO(ResultSet rs) {
		this();
		populateData(rs);
	}

	/**
	 * Copy Constructor takes SRTProjectVO and clones relevant data off
	 * for an admin copy request.
	 * @param srtProjectVO
	 */
	public SRTProjectVO(SRTProjectVO project) {
		this();

		if(project != null) {
			this.setOpCoId(project.getOpCoId());
			this.setProjectName(project.getProjectName());
			this.setProjectType(project.getProjectType());
			this.setSpecialInstructions(project.getSpecialInstructions());
			this.setSrtContact(project.getSrtContact());
			this.setProjectStatus("UNASSIGNED");
			this.setProjectType("NEW");
			this.setCreateDt(Convert.getCurrentTimestamp());
			for(SRTMasterRecordVO mr : project.getMasterRecords()) {
				this.addMasterRecord(new SRTMasterRecordVO(mr.getMasterRecordId()));
			}
		}
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
	 * @return the projectTypeTxt
	 */
	@Column(name="PROJ_TYPE_TXT", isReadOnly=true)
	public String getProjectTypeTxt() {
		return projectTypeTxt;
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
	 * @return the projectStatusTxt
	 */
	@Column(name="proj_stat_txt", isReadOnly=true)
	public String getProjectStatusTxt() {
		return projectStatusTxt;
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
	 * @return the surgeonNm
	 */
	@Column(name="SURGEON_NM", isReadOnly=true)
	public String getSurgeonNm() {
		return StringUtil.checkVal(surgeonNm);
	}

	/**
	 * @return the makeFromScratch
	 */
	public boolean isMakeFromScratch() {
		return makeFromScratch;
	}

	/**
	 * @return the makeFromScratch
	 */
	@Column(name="MAKE_FROM_SCRATCH")
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
	 * @return the buyerNm
	 */
	@Column(name="BUYER_NM")
	public String getBuyerNm() {
		return buyerNm;
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
	 * @return the supplierNm
	 */
	@Column(name="SUPPLIER_NM")
	public String getSupplierNm() {
		return supplierNm;
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
	 * @return the surgDt
	 */
	@Column(name="SURG_DT")
	public Date getSurgDt() {
		return surgDt;
	}

	/**
	 * @return the deliveryDt
	 */
	@Column(name="DELIVERY_DT")
	public Date getDeliveryDt() {
		return deliveryDt;
	}

	/**
	 * @return the total
	 */
	@Column(name="TOTAL", isReadOnly=true)
	public int getTotal() {
		return total;
	}

	/**
	 * @param total the total to set.
	 */
	public void setTotal(int total) {
		this.total = total;
	}

	/**
	 * @return the territory
	 */
	@Column(name="distributorship", isReadOnly=true)
	public String getDistributorship() {
		return distributorship;
	}

	/**
	 * @param territory the territory to set.
	 */
	public void setDistributorship(String distributorship) {
		this.distributorship = distributorship;
	}

	/**
	 * @return the lockStatus
	 */
	@Column(name="LOCK_STATUS", isReadOnly=true)
	public boolean getLockStatus() {
		return lockStatus;
	}

	/**
	 * @return the lockedById
	 */
	@Column(name="LOCKED_BY_ID", isReadOnly=true)
	public String getLockedById() {
		return lockedById;
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
	 * @return the ledgerDates
	 */
	public Map<String, Date> getLedgerDates() {
		return ledgerDates;
	}

	/**
	 * Retrieve latest status off the stack.
	 * @return
	 */
	public String getStatus() {
		if(milestones == null || milestones.size() == 0) {
			return "";
		} else {
			List<SRTProjectMilestoneVO> fMilestones = new ArrayList<>(milestones.values()).stream().filter(m -> MilestoneTypeId.STATUS.equals(m.getMilestoneTypeId())).collect(Collectors.toList());
			return fMilestones.get(fMilestones.size() - 1).getMilestoneId();
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
	 * @param projectTypeTxt the projectTypeTxt to set.
	 */
	public void setProjectTypeTxt(String projectTypeTxt) {
		this.projectTypeTxt = projectTypeTxt;
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
	 * @param projectStatusTxt the projectStatusTxt to set.
	 */
	public void setProjectStatusTxt(String projectStatusTxt) {
		this.projectStatusTxt = projectStatusTxt;
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
	 * @param surgeonNm the surgeonNm to set.
	 */
	public void setSurgeonNm(String surgeonNm) {
		this.surgeonNm = surgeonNm;
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
	 * @param buyerNm the buyerNm to set.
	 */
	public void setBuyerNm(String buyerNm) {
		this.buyerNm = StringUtil.checkVal(buyerNm, null);
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
	 * @param supplierNm the supplierNm to set.
	 */
	public void setSupplierNm(String supplierNm) {
		this.supplierNm = supplierNm;
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
	 * @param surgDt the surgDt to set.
	 */
	public void setSurgDt(Date surgDt) {
		this.surgDt = surgDt;
	}

	/**
	 * @param deliveryDt the deliveryDt to set.
	 */
	public void setDeliveryDt(Date deliveryDt) {
		this.deliveryDt = deliveryDt;
	}

	/**
	 * @param lockStatus the lockStatus to set.
	 */
	public void setLockStatus(boolean lockStatus) {
		this.lockStatus = lockStatus;
	}

	/**
	 * @param lockedById the lockedById to set.
	 */
	public void setLockedById(String lockedById) {
		this.lockedById = lockedById;
	}

	/**
	 * @param masterRecords the masterRecords to set.
	 */
	public void setMasterRecords(List<SRTMasterRecordVO> masterRecords) {
		this.masterRecords = masterRecords;
	}

	/**
	 * @param masterRecord the masterRecords to add.
	 */
	@BeanSubElement
	public void addMasterRecord(SRTMasterRecordVO masterRecord) {
		if(masterRecord != null && !StringUtil.isEmpty(masterRecord.getMasterRecordId())) {
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
	 * @param ledgerDates the ledgerDates to set.
	 */
	public void setLedgerDates(Map<String, Date> ledgerDates) {
		this.ledgerDates = ledgerDates;
	}

	/**
	 * Add a Milestone Record to the Milestones Map.
	 * @param milestone - the milestone to be added
	 */
	@BeanSubElement
	@Override
	public void addMilestone(SRTProjectMilestoneVO milestone) {
		if(milestone != null && !StringUtil.isEmpty(milestone.getMilestoneId())) {
			milestone.setProjectId(projectId);
			milestones.put(milestone.getMilestoneId(), milestone);
		}
	}

	/**
	 * Adds a milestone using id and date.
	 * @param milestoneId
	 * @param milestoneDt
	 */
	public void addMilestone(String milestoneId, Date milestoneDt) {
		if(!StringUtil.isEmpty(milestoneId)) {
			SRTProjectMilestoneVO milestone = new SRTProjectMilestoneVO(milestoneId, projectId);
			if(milestoneDt != null) {
				milestone.setMilestoneDt(milestoneDt);
			}
			milestones.put(milestoneId, milestone);
		}
	}

	/**
	 * Add a LedgerDate to the ledgers Map.
	 * @param ledgerType - The ledgerType to be added
	 * @param ledgerDate - The ledgerDate to be added
	 */
	public void addLedgerDate(String ledgerType, Date ledgerDate) {
		if(!StringUtil.isEmpty(ledgerType) && ledgerDate != null) {
			if("SURG_DT".equals(ledgerType)) {
				this.surgDt = ledgerDate;
			} else if("ORIG_MFG_DEL_DT".equals(ledgerType)) {
				this.deliveryDt = ledgerDate;
			} else if("REV_MFG_DEL_DT".equals(ledgerType)) {
				this.deliveryDt = ledgerDate;
			}
			ledgerDates.put(ledgerType, ledgerDate);
		}
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.workflow.milestones.MilestoneIntfc#removeMilestone(java.lang.String)
	 */
	@Override
	public void removeMilestone(String milestoneId) {
		milestones.remove(milestoneId);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.workflow.milestones.MilestoneIntfc#getData()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <C extends Comparable<C>> C getFieldValue(String fieldName) {
		C data = null;
		if(ledgerDates.containsKey(fieldName)) {
			data = (C) ledgerDates.get(fieldName);
		} else {
			data = MilestoneIntfc.super.getFieldValue(fieldName);
		}
		return data;
	}
}