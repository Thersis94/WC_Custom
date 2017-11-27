package com.mindbody.vo.classes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.mindbody.MindBodyClassApi.ClassDocumentType;
import com.mindbody.vo.MindBodyConfig;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> ClassCallVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Common Configuration for Class API Calls.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 6, 2017
 ****************************************************************************/
public abstract class MindBodyClassConfig extends MindBodyConfig {

	private ClassDocumentType type;
	private List<String> clientIds;
	private List<Integer> classIds;
	private List<Integer> programIds;
	private List<Long> staffIds;
	private List<Integer> locationIds;
	private Date startDt;
	private Date endDt;


	/**
	 * 
	 */
	protected MindBodyClassConfig(ClassDocumentType type, MindBodyCredentialVO source, MindBodyCredentialVO user) {
		super(source, user);
		this.type = type;
		this.clientIds = new ArrayList<>();
		this.classIds = new ArrayList<>();
		this.programIds = new ArrayList<>();
		this.staffIds = new ArrayList<>();
		this.locationIds = new ArrayList<>();
	}


	/**
	 * @return the type
	 */
	public ClassDocumentType getType() {
		return type;
	}


	public List<String> getClientIds() {
		return clientIds;
	}


	public List<Integer> getClassIds() {
		return classIds;
	}


	public Date getStartDt() {
		return startDt;
	}


	public Date getEndDt() {
		return endDt;
	}


	public void setClientIds(List<String> clientIds) {
		this.clientIds = clientIds;
	}


	public void setClassIds(List<Integer> classIds) {
		this.classIds = classIds;
	}


	public void setStartDt(Date startDt) {
		this.startDt = startDt;
	}


	public void setEndDt(Date endDt) {
		this.endDt = endDt;
	}


	public void addClientId(String clientId) {
		this.clientIds.add(clientId);
	}


	public void addClassId(Integer classId) {
		this.classIds.add(classId);
	}


	/**
	 * @return the programIds
	 */
	public List<Integer> getProgramIds() {
		return programIds;
	}


	/**
	 * @return the staffIds
	 */
	public List<Long> getStaffIds() {
		return staffIds;
	}


	/**
	 * @return the locationIds
	 */
	public List<Integer> getLocationIds() {
		return locationIds;
	}


	/**
	 * @param programIds the programIds to set.
	 */
	public void setProgramIds(List<Integer> programIds) {
		this.programIds = programIds;
	}

	public void addProgramId(Integer programId) {
		this.programIds.add(programId);
	}

	/**
	 * @param staffIds the staffIds to set.
	 */
	public void setStaffIds(List<Long> staffIds) {
		this.staffIds = staffIds;
	}


	/**
	 * @param locationIds the locationIds to set.
	 */
	public void setLocationIds(List<Integer> locationIds) {
		this.locationIds = locationIds;
	}

}