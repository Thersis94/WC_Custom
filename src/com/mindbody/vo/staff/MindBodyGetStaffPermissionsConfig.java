package com.mindbody.vo.staff;

import com.mindbody.MindBodyStaffApi.StaffDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodyGetStaffPermissionsConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages GetStaffPermissions Endpoint unique Configuration.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 13, 2017
 ****************************************************************************/
public class MindBodyGetStaffPermissionsConfig extends MindBodyStaffConfig {

	/**
	 * @param type
	 * @param sourceCredentials
	 * @param userCredentials
	 */
	public MindBodyGetStaffPermissionsConfig( MindBodyCredentialVO sourceCredentials, MindBodyCredentialVO userCredentials) {
		super(StaffDocumentType.GET_STAFF_PERMISSIONS, sourceCredentials, userCredentials);
	}

	@Override
	public boolean isValid() {
		return super.isValid() && !getStaffIds().isEmpty();
	}

	/**
	 * @return
	 */
	public long getStaffId() {
		return getStaffIds().get(0);
	}
}