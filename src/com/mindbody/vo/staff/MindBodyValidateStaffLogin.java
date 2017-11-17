package com.mindbody.vo.staff;

import com.mindbody.MindBodyStaffApi.StaffDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> MindBodyValidateStaffLogin.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages ValidateStaffLogin Endpoint unique Configuration.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 13, 2017
 ****************************************************************************/
public class MindBodyValidateStaffLogin extends MindBodyStaffConfig {

	private String userName;
	private String password;

	/**
	 * @param type
	 * @param sourceCredentials
	 * @param userCredentials
	 */
	public MindBodyValidateStaffLogin(MindBodyCredentialVO sourceCredentials, MindBodyCredentialVO userCredentials) {
		super(StaffDocumentType.VALIDATE_STAFF_LOGIN, sourceCredentials, userCredentials);
	}

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param userName the userName to set.
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @param password the password to set.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public boolean isValid() {
		return super.isValid() && !StringUtil.isEmpty(userName) && !StringUtil.isEmpty(password);
	}
}
