package com.depuysynthes.srt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;

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
public class SRTRequestVO {

	public enum ReasonForRequest {
		CONVERT_NEW("Convert New Business"), 
		MAINTAIN_EXISTING("Maintain Existing Business"), 
		PREVENT_LOSS("Prevent Loss");

		private String text;
		private ReasonForRequest(String text) {this.text = text;}
		public String getText() {return text;}
	}

	public enum ChargeTo {
		TERRITORY("Territory"),
		HOSPITAL_PO("Hospital P.O."),
		OTHER("Other");
		private String text;
		private ChargeTo(String text) {this.text = text;}
		public String getText() {return text;}
	}
	private String requestId;
	private SRTRosterVO requestor;
	private String hospitalName;
	private String surgeonFirstName;
	private String surgeonLastName;
	private String description;
	private String reqTerritoryId;
	private List<GenericVO> fileUploads;
	private List<SRTNoteVO> srtNotes;
	private BigDecimal estimatedRoi;
	private int qtyNo;
	private ReasonForRequest reason;
	private ChargeTo chargeTo;
	private Date createDt;
	private Date updateDt;

	public SRTRequestVO() {
		fileUploads = new ArrayList<>();
		srtNotes = new ArrayList<>();
	}

	/**
	 * @return the requestId
	 */
	@Column(name="REQUEST_ID", isPrimaryKey=true)
	public String getRequestId() {
		return requestId;
	}

	/**
	 * @return the requestor
	 */
	public SRTRosterVO getRequestor() {
		return requestor;
	}

	/**
	 * 
	 * @return the requstor's rosterId
	 */
	public String getRosterId() {
		return requestor!= null ? requestor.getRosterId() : "";
	}

	/**
	 * @return the hospitalName
	 */
	@Column(name="HOSPITAL_NM")
	public String getHospitalName() {
		return hospitalName;
	}

	/**
	 * @return the surgeonFirstName
	 */
	@Column(name="SURGEON_FIRST_NM")
	public String getSurgeonFirstName() {
		return surgeonFirstName;
	}

	/**
	 * @return the surgeonLastName
	 */
	@Column(name="SURGEON_LAST_NM")
	public String getSurgeonLastName() {
		return surgeonLastName;
	}

	/**
	 * @return the description
	 */
	@Column(name="REQ_DESC")
	public String getDescription() {
		return description;
	}

	/**
	 * @return the reqTerritoryId
	 */
	@Column(name="REQ_TERRITORY_ID")
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
	@Column(name="REASON_FOR_REQ")
	public ReasonForRequest getReason() {
		return reason;
	}

	/**
	 * @return the chargeTo
	 */
	@Column(name="CHARGE_TO")
	public ChargeTo getChargeTo() {
		return chargeTo;
	}

	/**
	 * @param requestor the requestor to set.
	 */
	public void setRequestor(SRTRosterVO requestor) {
		this.requestor = requestor;
	}

	/**
	 * @param hospitalName the hospitalName to set.
	 */
	public void setHospitalName(String hospitalName) {
		this.hospitalName = hospitalName;
	}

	/**
	 * @param surgeonFirstName the surgeonFirstName to set.
	 */
	public void setSurgeonFirstName(String surgeonFirstName) {
		this.surgeonFirstName = surgeonFirstName;
	}

	/**
	 * @param surgeonLastName the surgeonLastName to set.
	 */
	public void setSurgeonLastName(String surgeonLastName) {
		this.surgeonLastName = surgeonLastName;
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
	@BeanSubElement
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
	@BeanSubElement
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
	public void setReason(ReasonForRequest reason) {
		this.reason = reason;
	}

	/**
	 * @param chargeTo the chargeTo to set.
	 */
	public void setChargeTo(ChargeTo chargeTo) {
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
}