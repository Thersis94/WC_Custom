package com.mindbody.vo.staff;

import java.util.ArrayList;
import java.util.List;

import com.mindbody.MindBodyStaffApi.StaffDocumentType;
import com.mindbody.vo.MindBodyConfig;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodyStaffVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Common Configuration for Class API Calls.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 6, 2017
 ****************************************************************************/
public abstract class MindBodyStaffConfig extends MindBodyConfig {

	private StaffDocumentType type;
	private List<Long> staffIds;

	/**
	 * @param sourceName
	 * @param sourceKey
	 * @param siteIds
	 */
	public MindBodyStaffConfig(StaffDocumentType type, MindBodyCredentialVO sourceCredentials, MindBodyCredentialVO userCredentials) {
		super(sourceCredentials, userCredentials);
		this.type = type;
		this.staffIds = new ArrayList<>();
	}

	/**
	 * @return the type
	 */
	public StaffDocumentType getType() {
		return type;
	}

	/**
	 * @param type the type to set.
	 */
	public void setType(StaffDocumentType type) {
		this.type = type;
	}

	/**
	 * @return the staffIds
	 */
	public List<Long> getStaffIds() {
		return staffIds;
	}

	/**
	 * @param staffIds the staffIds to set.
	 */
	public void setStaffIds(List<Long> staffIds) {
		this.staffIds = staffIds;
	}
}