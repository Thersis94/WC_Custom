package com.depuysynthes.srt.vo;

import java.sql.ResultSet;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.user.HumanNameIntfc;

/****************************************************************************
 * <b>Title:</b> SRTNoteVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Stores SRT Note Data.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 5, 2018
 ****************************************************************************/
@Table(name="SRT_NOTE")
public class SRTNoteVO extends BeanDataVO implements HumanNameIntfc {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private String noteId;
	private String projectId;
	private String rosterId;
	private String noteTxt;
	private String createDt;
	private String firstNm;
	private String lastNm;

	public SRTNoteVO() {
		super();
	}

	public SRTNoteVO(ActionRequest req) {
		this();
		super.populateData(req);
	}

	public SRTNoteVO(ResultSet rs) {
		this();
		super.populateData(rs);
	}

	/**
	 * @return the noteId
	 */
	@Column(name="NOTE_ID", isPrimaryKey=true)
	public String getNoteId() {
		return noteId;
	}

	/**
	 * @return the projectId
	 */
	@Column(name="PROJECT_ID")
	public String getProjectId() {
		return projectId;
	}

	/**
	 * @return
	 */
	@Column(name="ROSTER_ID")
	public String getRosterID() {
		return rosterId;
	}

	/**
	 * @return the noteTxt
	 */
	@Column(name="NOTE_TXT")
	public String getNoteTxt() {
		return noteTxt;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="CREATE_DT", isInsertOnly=true, isAutoGen=true)
	public String getCreateDt() {
		return createDt;
	}

	/**
	 * @param noteId the noteId to set.
	 */
	public void setNoteId(String noteId) {
		this.noteId = noteId;
	}

	/**
	 * @param projectId the projectId to set.
	 */
	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	/**
	 * @param rosterId the rosterId to set.
	 */
	public void setRosterId(String rosterId) {
		this.rosterId = rosterId;
	}

	/**
	 * @param noteTxt the noteTxt to set.
	 */
	public void setNoteTxt(String noteTxt) {
		this.noteTxt = noteTxt;
	}

	/**
	 * @param createDt the createDt to set.
	 */
	public void setCreateDt(String createDt) {
		this.createDt = createDt;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.user.HumanNameIntfc#getFirstName()
	 */
	@Override
	@Column(name="first_nm", isReadOnly=true)
	public String getFirstName() {
		return firstNm;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.user.HumanNameIntfc#getLastName()
	 */
	@Override
	@Column(name="last_nm", isReadOnly=true)
	public String getLastName() {
		return lastNm;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.user.HumanNameIntfc#setFirstName(java.lang.String)
	 */
	@Override
	public void setFirstName(String firstNm) {
		this.firstNm = firstNm;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.user.HumanNameIntfc#setLastName(java.lang.String)
	 */
	@Override
	public void setLastName(String lastNm) {
		this.lastNm = lastNm;
	}
}