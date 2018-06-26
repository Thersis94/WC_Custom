package com.depuysynthes.srt.vo;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title:</b> SRTRequestVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Stores SRT Request Data.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 5, 2018
 ****************************************************************************/
@Table(name="DPY_SYN_SRT_REQUEST")
public class SRTRequestVO extends BeanDataVO {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private String requestId;
	private String rosterId;
	private SRTRosterVO requestor;
	private String hospitalName;
	private String surgeonNm;
	private String description;
	private String reqTerritoryId;
	private List<GenericVO> fileUploads;
	private List<SRTNoteVO> srtNotes;
	private BigDecimal estimatedRoi;
	private SRTRequestAddressVO requestAddress;
	private int qtyNo;
	private String reason;
	private String reasonTxt;
	private String chargeTo;
	private String opCoId;
	private Date createDt;
	private Date updateDt;

	//Helper Values.
	private String projectStatus;

	public SRTRequestVO() {
		super();
		fileUploads = new ArrayList<>();
		srtNotes = new ArrayList<>();
	}

	public SRTRequestVO(ActionRequest req) {
		this();
		super.populateData(req);
	}

	public SRTRequestVO(ResultSet rs) {
		this();
		super.populateData(rs);
	}

	/**
	 * @return the requestId
	 */
	@Column(name="REQUEST_ID", isPrimaryKey=true)
	public String getRequestId() {
		return requestId;
	}

	@Column(name="ROSTER_ID")
	public String getRosterId() {
		return rosterId;
	}

	/**
	 * @return the requestor
	 */
	public SRTRosterVO getRequestor() {
		return requestor;
	}

	/**
	 * @return helper method to get RequestorNm
	 */
	public String getRequstorNm() {
		return requestor != null ? requestor.getFullName() : "";
	}

	/**
	 * @return the hospitalName
	 */
	@Column(name="HOSPITAL_NM")
	public String getHospitalName() {
		return hospitalName;
	}

	/**
	 * @return the description
	 */
	@Column(name="REQUEST_DESC")
	public String getDescription() {
		return description;
	}

	/**
	 * @return the reqTerritoryId
	 */
	@Column(name="REQUEST_TERRITORY_ID")
	public String getReqTerritoryId() {
		return reqTerritoryId;
	}

	/**
	 * @return the fileUploads
	 */
	public List<GenericVO> getFileUploads() {
		return fileUploads;
	}

	/**
	 * @return the srtNotes
	 */
	public List<SRTNoteVO> getSrtNotes() {
		return srtNotes;
	}

	/**
	 * @return the estimatedRoi
	 */
	public BigDecimal getEstimatedRoi() {
		return estimatedRoi;
	}

	/**
	 * @return estimatedRoi in double format.
	 */
	@Column(name="ESTIMATED_ROI")
	public double getEstimatedRoiDbl() {
		return estimatedRoi != null ? estimatedRoi.doubleValue() : BigDecimal.ZERO.doubleValue();
	}

	/**
	 * @return the qtyNo
	 */
	@Column(name="QTY_NO")
	public int getQtyNo() {
		return qtyNo;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @return the updateDt
	 */
	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDt() {
		return updateDt;
	}

	/**
	 * @return the reason
	 */
	@Column(name="REASON_FOR_REQUEST")
	public String getReason() {
		return reason;
	}

	/**
	 * @return the reason Text
	 */
	@Column(name="REASON_FOR_REQUEST_TXT", isReadOnly=true)
	public String getReasonTxt() {
		return reasonTxt;
	}

	/**
	 * @return the chargeTo
	 */
	@Column(name="CHARGE_TO")
	public String getChargeTo() {
		return chargeTo;
	}

	/**
	 * @return the address
	 */
	public SRTRequestAddressVO getRequestAddress() {
		return requestAddress;
	}

	/**
	 * @return the opCoId
	 */
	@Column(name="OP_CO_ID")
	public String getOpCoId() {
		return opCoId;
	}

	/**
	 * @return the projectStatus
	 */
	@Column(name="PROJ_STAT_ID", isReadOnly=true)
	public String getProjectStatus() {
		return projectStatus;
	}

	/**
	 * @return the surgeonNm
	 */
	@Column(name="SURGEON_NM")
	public String getSurgeonNm() {
		return surgeonNm;
	}

	@BeanSubElement
	public void setRequestAddress(SRTRequestAddressVO address) {
		this.requestAddress = address;
	}

	/**
	 * @param requestor the requestor to set.
	 */
	@BeanSubElement
	public void setRequestor(SRTRosterVO requestor) {
		this.requestor = requestor;
	}

	/**
	 * @param requestId the requestId to set.
	 */
	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	/**
	 * @param rosterId the rosterId to set.
	 */
	public void setRosterId(String rosterId) {
		this.rosterId = rosterId;
	}

	/**
	 * @param hospitalName the hospitalName to set.
	 */
	public void setHospitalName(String hospitalName) {
		this.hospitalName = hospitalName;
	}

	/**
	 * @param description the description to set.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param reqTerritoryId the reqTerritoryId to set.
	 */
	public void setReqTerritoryId(String reqTerritoryId) {
		this.reqTerritoryId = reqTerritoryId;
	}

	/**
	 * @param fileUploads the fileUploads to set.
	 */
	public void setFileUploads(List<GenericVO> fileUploads) {
		this.fileUploads = fileUploads;
	}

	/**
	 * @param file the file to add.
	 */
	public void addFileUpload(GenericVO file) {
		if(file != null) {
			fileUploads.add(file);
		}
	}

	/**
	 * @param srtNotes the srtNotes to set.
	 */
	public void setSrtNotes(List<SRTNoteVO> srtNotes) {
		this.srtNotes = srtNotes;
	}

	/**
	 * @param note the srtNote to add.
	 */
	public void addSrtNote(SRTNoteVO note) {
		if(note != null) {
			srtNotes.add(note);
		}
	}

	/**
	 * @param estimatedRoi the estimatedRoi to set.
	 */
	public void setEstimatedRoi(BigDecimal estimatedRoi) {
		this.estimatedRoi = estimatedRoi;
	}

	/**
	 * @param estimatedRoi the estimatedRoi to set.
	 */
	public void setEstimatedRoiDbl(double estimatedRoi) {
		this.estimatedRoi = BigDecimal.valueOf(estimatedRoi);
	}

	/**
	 * @param qtyNo the qtyNo to set.
	 */
	public void setQtyNo(int qtyNo) {
		this.qtyNo = qtyNo;
	}

	/**
	 * @param reason the reason to set.
	 */
	public void setReason(String reason) {
		this.reason = reason;
	}

	/**
	 * @param reasonTxt the reasonTxt to set.
	 */
	public void setReasonTxt(String reasonTxt) {
		this.reasonTxt = reasonTxt;
	}

	/**
	 * @param chargeTo the chargeTo to set.
	 */
	public void setChargeTo(String chargeTo) {
		this.chargeTo = chargeTo;
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
	 * @param opCoId the opCoId to set.
	 */
	public void setOpCoId(String opCoId) {
		this.opCoId = opCoId;
	}

	public void setProjectStatus(String projectStatus) {
		this.projectStatus = projectStatus;
	}

	/**
	 * @param surgeonNm the surgeonNm to set.
	 */
	public void setSurgeonNm(String surgeonNm) {
		this.surgeonNm = surgeonNm;
	}
}