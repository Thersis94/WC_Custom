package com.depuysynthes.srt.vo;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title:</b> SRTFileVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> TODO
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Mar 1, 2018
 ****************************************************************************/
@Table(name="SRT_FILE")
public class SRTFileVO extends BeanDataVO {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private String fileId;
	private String requestId;
	private String masterRecordId;
	private String rosterId;
	private String projectId;
	private String fileName;
	private String filePathText;
	private Date createDt;
	
	public SRTFileVO() {
		super();
	}

	public SRTFileVO(ActionRequest req) {
		populateData(req);
	}

	public SRTFileVO(ResultSet rs) {
		populateData(rs);
	}

	/**
	 * @return the fileId
	 */
	@Column(name="FILE_ID", isPrimaryKey=true)
	public String getFileId() {
		return fileId;
	}

	/**
	 * @return the requestId
	 */
	@Column(name="REQUEST_ID")
	public String getRequestId() {
		return requestId;
	}

	/**
	 * @return the masterRecordId
	 */
	@Column(name="MASTER_RECORD_ID")
	public String getMasterRecordId() {
		return masterRecordId;
	}

	/**
	 * @return the rosterId
	 */
	@Column(name="ROSTER_ID")
	public String getRosterId() {
		return rosterId;
	}

	/**
	 * @return the projectId
	 */
	@Column(name="PROJECT_ID")
	public String getProjectId() {
		return projectId;
	}

	/**
	 * @return the fileNm
	 */
	@Column(name="FILE_NM")
	public String getFileName() {
		return fileName;
	}

	/**
	 * @return the filePath
	 */
	@Column(name="FILE_PATH")
	public String getFilePathText() {
		return filePathText;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @param fileId the fileId to set.
	 */
	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	/**
	 * @param requestId the requestId to set.
	 */
	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	/**
	 * @param masterRecordId the masterRecordId to set.
	 */
	public void setMasterRecordId(String masterRecordId) {
		this.masterRecordId = masterRecordId;
	}

	/**
	 * @param rosterId the rosterId to set.
	 */
	public void setRosterId(String rosterId) {
		this.rosterId = rosterId;
	}

	/**
	 * @param projectId the projectId to set.
	 */
	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	/**
	 * @param fileNm the fileNm to set.
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * @param filePath the filePath to set.
	 */
	public void setFilePathText(String filePathText) {
		this.filePathText = filePathText;
	}

	/**
	 * @param createDt the createDt to set.
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}
}