package com.mindbody.vo.clients;

import com.mindbody.MindbodyClientApi.ClientDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

import opennlp.tools.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> MindBodyValidateLoginConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages ValidateLoginConfig Endpoint unique Configuration.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 14, 2017
 ****************************************************************************/
public class MindBodyValidateLoginConfig extends MindBodyClientConfig {

	private String userName;
	private String password;

	/**
	 * @param type
	 * @param source
	 * @param user
	 */
	public MindBodyValidateLoginConfig(MindBodyCredentialVO source, MindBodyCredentialVO user) {
		super(ClientDocumentType.VALIDATE_LOGIN, source, user);
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