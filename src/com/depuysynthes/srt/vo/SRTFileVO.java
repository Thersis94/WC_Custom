package com.depuysynthes.srt.vo;

import java.sql.ResultSet;
import java.util.Date;

import com.depuysynthes.srt.util.SRTUtil;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.user.HumanNameIntfc;

/****************************************************************************
 * <b>Title:</b> SRTFileVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Stores SRT File Relationship Data.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Mar 1, 2018
 ****************************************************************************/
@Table(name="DPY_SYN_SRT_FILE")
public class SRTFileVO extends BeanDataVO implements HumanNameIntfc {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private String fileId;
	private String profileDocumentId;
	private String requestId;
	private String masterRecordId;
	private String rosterId;
	private String projectId;
	private String fileName;
	private String filePathText;
	private Date createDt;
	private String firstName;
	private String lastName;
	
	public SRTFileVO() {
		super();
	}

	public SRTFileVO(ActionRequest req) {
		populateData(req);

		//File RosterId should always be the user that's performing the upload.
		rosterId = SRTUtil.getRoster(req).getRosterId();
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
	 * @return the profileDocumentId
	 */
	@Column(name="PROFILE_DOCUMENT_ID")
	public String getProfileDocumentId() {
		return profileDocumentId;
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
	 * @param profileDocumentId the profileDocumentId to set.
	 */
	public void setProfileDocumentId(String profileDocumentId) {
		this.profileDocumentId = profileDocumentId;
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

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.user.HumanNameIntfc#getFirstName()
	 */
	@Override
	@Column(name="FIRST_NM")
	public String getFirstName() {
		return firstName;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.user.HumanNameIntfc#getLastName()
	 */
	@Override
	@Column(name="LAST_NM")
	public String getLastName() {
		return lastName;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.user.HumanNameIntfc#setFirstName(java.lang.String)
	 */
	@Override
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.user.HumanNameIntfc#setLastName(java.lang.String)
	 */
	@Override
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
}