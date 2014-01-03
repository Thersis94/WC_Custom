package com.sjm.corp.mobile.collection;

import com.smt.sitebuilder.action.SBModuleVO;


/****************************************************************************
 * <b>Title</b>: PatientsVO.java<p/>
 * <b>Description: Object that handles the data collected from SJM related to patients and stores it temporarily(until we put it in the db at the end)</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since June 21, 2012
 ****************************************************************************/

public class PatientsVO extends SBModuleVO{
	private static final long serialVersionUID = 1L;
	private int primaryCareRef;
	private int orthopedicRef;
	private int podiatristRef;
	private int chiropractorRef;
	private int physicalTherepistRef;
	private int otherRef;
	private String otherRefName;
	private String patientId;
	
	public PatientsVO(){
		super();
	}

	public int getPrimaryCareRef() {
		return primaryCareRef;
	}

	public void setPrimaryCareRef(int primaryCareRef) {
		this.primaryCareRef = primaryCareRef;
	}

	public int getOrthopedicRef() {
		return orthopedicRef;
	}

	public void setOrthopedicRef(int orthopedicRef) {
		this.orthopedicRef = orthopedicRef;
	}

	public int getPodiatristRef() {
		return podiatristRef;
	}

	public void setPodiatristRef(int podiatristRef) {
		this.podiatristRef = podiatristRef;
	}

	public int getChiropractorRef() {
		return chiropractorRef;
	}

	public void setChiropractorRef(int chiropractorRef) {
		this.chiropractorRef = chiropractorRef;
	}

	public int getPhysicalTherepistRef() {
		return physicalTherepistRef;
	}

	public void setPhysicalTherepistRef(int physicalTherepistRef) {
		this.physicalTherepistRef = physicalTherepistRef;
	}

	public int getOtherRef() {
		return otherRef;
	}

	public void setOtherRef(int otherRef) {
		this.otherRef = otherRef;
	}

	public String getOtherRefName() {
		return otherRefName;
	}

	public void setOtherRefName(String otherRefName) {
		this.otherRefName = otherRefName;
	}

	public String getPatientId() {
		return patientId;
	}

	public void setPatientId(String patientId) {
		this.patientId = patientId;
	}

}