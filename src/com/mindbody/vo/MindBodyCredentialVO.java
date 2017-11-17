package com.mindbody.vo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> MindBodyCredentialVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages MindBody Credential Information.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 11, 2017
 ****************************************************************************/
public class MindBodyCredentialVO {

	private String userName;
	private String password;
	private List<Integer> siteIds;
	/**
	 * 
	 */
	public MindBodyCredentialVO(String userName, String password, List<Integer> siteIds) {
		this.userName = userName;
		this.password = password;
		this.siteIds = siteIds;
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
	 * @return the siteIds
	 */
	public List<Integer> getSiteIds() {
		return siteIds;
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
	/**
	 * @param siteIds the siteIds to set.
	 */
	public void setSiteIds(List<Integer> siteIds) {
		this.siteIds = siteIds;
	}

	/**
	 * Validate the Credentials have all necessary parts.
	 * @return
	 */
	public boolean isValid() {
		return !StringUtil.isEmpty(userName) && !StringUtil.isEmpty(password) && !siteIds.isEmpty();
	}
}