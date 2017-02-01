package com.biomed.smarttrak.vo;

/*****************************************************************************
 <p><b>Title</b>: SmartTRAKTeamVO.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author groot
 @version 1.0
 @since Jan 31, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class SmartTRAKTeamVO {

	private String teamId;
	private String accountId;
	private String teamName;
	private int defaultFlag;
	private String ownerProfileId;
	
	/**
	* Constructor
	*/
	public SmartTRAKTeamVO() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return the teamId
	 */
	public String getTeamId() {
		return teamId;
	}

	/**
	 * @param teamId the teamId to set
	 */
	public void setTeamId(String teamId) {
		this.teamId = teamId;
	}

	/**
	 * @return the accountId
	 */
	public String getAccountId() {
		return accountId;
	}

	/**
	 * @param accountId the accountId to set
	 */
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	/**
	 * @return the teamName
	 */
	public String getTeamName() {
		return teamName;
	}

	/**
	 * @param teamName the teamName to set
	 */
	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}

	/**
	 * @return the defaultFlag
	 */
	public int getDefaultFlag() {
		return defaultFlag;
	}

	/**
	 * @param defaultFlag the defaultFlag to set
	 */
	public void setDefaultFlag(int defaultFlag) {
		this.defaultFlag = defaultFlag;
	}

	/**
	 * @return the ownerProfileId
	 */
	public String getOwnerProfileId() {
		return ownerProfileId;
	}

	/**
	 * @param ownerProfileId the ownerProfileId to set
	 */
	public void setOwnerProfileId(String ownerProfileId) {
		this.ownerProfileId = ownerProfileId;
	}
	
}
