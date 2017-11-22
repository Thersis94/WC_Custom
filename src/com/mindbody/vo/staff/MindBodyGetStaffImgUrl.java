package com.mindbody.vo.staff;

import com.mindbody.MindBodyStaffApi.StaffDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodyGetStaffImgUrl.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages GetStaffImgUrl Endpoint unique Configuration.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 13, 2017
 ****************************************************************************/
public class MindBodyGetStaffImgUrl extends MindBodyStaffConfig {

	/**
	 * @param type
	 * @param sourceCredentials
	 * @param userCredentials
	 */
	public MindBodyGetStaffImgUrl(MindBodyCredentialVO sourceCredentials, MindBodyCredentialVO userCredentials) {
		super(StaffDocumentType.GET_STAFF_IMG_URL, sourceCredentials, userCredentials);
	}

	public Long getStaffId() {
		return super.getStaffIds().get(0);
	}

	@Override
	public boolean isValid() {
		return super.isValid() && getStaffId() != null;
	}
}