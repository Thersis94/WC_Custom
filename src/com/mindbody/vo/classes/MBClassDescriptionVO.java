package com.mindbody.vo.classes;

import java.util.Date;

import com.mindbodyonline.clients.api._0_5_1.ActionCode;

/****************************************************************************
 * <b>Title:</b> MBClassDescriptionVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manage Mindbody ClassDescription Data 
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 20, 2017
 ****************************************************************************/
public class MBClassDescriptionVO {
	private ActionCode.Enum action;
	private boolean active;
	private String description;
	private String imageUrl;
	private int id;
	private String name;
	private String preReq;
	private String notes;
	private Date lastUpdated;
	private MBProgramVO program;
	private MBSessionTypeVO sessionType;
	private MBLevelVO level;

	public MBClassDescriptionVO() {
		//Public Constructor
	}

	/**
	 * @return the action
	 */
	public ActionCode.Enum getAction() {
		return action;
	}

	/**
	 * @return the active
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return the imageUrl
	 */
	public String getImageUrl() {
		return imageUrl;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the preReq
	 */
	public String getPreReq() {
		return preReq;
	}

	/**
	 * @return the notes
	 */
	public String getNotes() {
		return notes;
	}

	/**
	 * @return the lastUpdated
	 */
	public Date getLastUpdated() {
		return lastUpdated;
	}

	/**
	 * @return the program
	 */
	public MBProgramVO getProgram() {
		return program;
	}

	/**
	 * @return the sessionType
	 */
	public MBSessionTypeVO getSessionType() {
		return sessionType;
	}

	/**
	 * @return the level
	 */
	public MBLevelVO getLevel() {
		return level;
	}

	/**
	 * @param action the action to set.
	 */
	public void setAction(ActionCode.Enum action) {
		this.action = action;
	}

	/**
	 * @param active the active to set.
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * @param description the description to set.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param imageUrl the imageUrl to set.
	 */
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	/**
	 * @param id the id to set.
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @param name the name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param preReq the preReq to set.
	 */
	public void setPreReq(String preReq) {
		this.preReq = preReq;
	}

	/**
	 * @param notes the notes to set.
	 */
	public void setNotes(String notes) {
		this.notes = notes;
	}

	/**
	 * @param lastUpdated the lastUpdated to set.
	 */
	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	/**
	 * @param program the program to set.
	 */
	public void setProgram(MBProgramVO program) {
		this.program = program;
	}

	/**
	 * @param sessionType the sessionType to set.
	 */
	public void setSessionType(MBSessionTypeVO sessionType) {
		this.sessionType = sessionType;
	}

	/**
	 * @param level the level to set.
	 */
	public void setLevel(MBLevelVO level) {
		this.level = level;
	}
}