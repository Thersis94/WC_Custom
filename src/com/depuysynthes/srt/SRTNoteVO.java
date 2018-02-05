package com.depuysynthes.srt;

import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

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
public class SRTNoteVO {

	private String noteId;
	private String projectId;
	private String noteTxt;
	private String createDt;

	public SRTNoteVO() {
		//Default Constructor
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
}