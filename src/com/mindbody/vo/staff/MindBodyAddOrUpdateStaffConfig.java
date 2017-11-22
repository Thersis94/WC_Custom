package com.mindbody.vo.staff;

import java.util.List;

import com.mindbody.MindBodyStaffApi.StaffDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;
import com.mindbodyonline.clients.api._0_5_1.Staff;

/****************************************************************************
 * <b>Title:</b> MindBodyAddOrUpdateStaffConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages AddOrUpdateStaff Endpoint unique Configuration.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 13, 2017
 ****************************************************************************/
public class MindBodyAddOrUpdateStaffConfig extends MindBodyStaffConfig {

	public static final String UPDATE_ACTION = "Update";
	public static final String ADD_NEW_ACTION = "AddNew";

	private boolean test;
	private List<Staff> staff;
	private String updateAction;

	/**
	 * @param type
	 * @param sourceCredentials
	 * @param userCredentials
	 */
	public MindBodyAddOrUpdateStaffConfig(MindBodyCredentialVO sourceCredentials, MindBodyCredentialVO userCredentials, boolean isUpdate) {
		super(StaffDocumentType.ADD_OR_UPDATE_STAFF, sourceCredentials, userCredentials);

		if(isUpdate) {
			updateAction = UPDATE_ACTION;
		} else {
			updateAction = ADD_NEW_ACTION;
		}
	}

	/**
	 * @return the test
	 */
	public boolean isTest() {
		return test;
	}

	/**
	 * @return the staff
	 */
	public List<Staff> getStaff() {
		return staff;
	}

	/**
	 * @param test the test to set.
	 */
	public void setTest(boolean test) {
		this.test = test;
	}

	/**
	 * @param staff the staff to set.
	 */
	public void setStaff(List<Staff> staff) {
		this.staff = staff;
	}

	/**
	 * @return
	 */
	public String getUpdateAction() {
		return updateAction;
	}
}