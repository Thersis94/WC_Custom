package com.mindbody.vo.clients;

import com.mindbody.MindbodyClientApi.ClientDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> MindBodySendUserNewPasswordConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> TODO
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 14, 2017
 ****************************************************************************/
public class MindBodySendUserNewPasswordConfig extends MindBodyClientConfig {

	private String userEmail;
	private String userFirstName;
	private String userLastName;

	/**
	 * @param type
	 * @param source
	 * @param user
	 */
	public MindBodySendUserNewPasswordConfig(MindBodyCredentialVO source, MindBodyCredentialVO user) {
		super(ClientDocumentType.SEND_USER_NEW_PASSWORD, source, user);
	}


	/**
	 * @return the userEmail
	 */
	public String getUserEmail() {
		return userEmail;
	}

	/**
	 * @return the userFirstName
	 */
	public String getUserFirstName() {
		return userFirstName;
	}

	/**
	 * @return the userLastName
	 */
	public String getUserLastName() {
		return userLastName;
	}

	/**
	 * @param userEmail the userEmail to set.
	 */
	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	/**
	 * @param userFirstName the userFirstName to set.
	 */
	public void setUserFirstName(String userFirstName) {
		this.userFirstName = userFirstName;
	}

	/**
	 * @param userLastName the userLastName to set.
	 */
	public void setUserLastName(String userLastName) {
		this.userLastName = userLastName;
	}

	public boolean isValid() {
		return super.isValid() 
				&& !StringUtil.isEmpty(userEmail)
				&& !StringUtil.isEmpty(userFirstName)
				&& !StringUtil.isEmpty(userLastName);
	}
}
