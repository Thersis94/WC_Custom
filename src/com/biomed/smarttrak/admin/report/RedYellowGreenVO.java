package com.biomed.smarttrak.admin.report;

import java.util.List;

import com.biomed.smarttrak.vo.UserVO;

/****************************************************************************
 * <b>Title:</b> RedGreenAccountVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> VO for managing data in Red Yellow Green Report Data
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jul 30, 2019
 ****************************************************************************/
public class RedYellowGreenVO {

	private AccountUsersVO acct;
	private double noActivityCnt;
	private double redActivityCnt;
	private double yellowActivityCnt;
	private double greenActivityCnt;

	public RedYellowGreenVO(AccountUsersVO acct) {
		this.acct = acct;
		List<UserVO> users = acct.getUsers();
		for(UserVO u : users) {
			String c = u.getLoginLegendColorText();

			switch(c) {
				default:
				case "None":
					noActivityCnt++;
					break;
				case "Green":
					greenActivityCnt++;
					break;
				case "Yellow":
					yellowActivityCnt++;
					break;
				case "Red":
					redActivityCnt++;
					break;
			}
		}
	}


	/**
	 * @return the acct
	 */
	public AccountUsersVO getAcct() {
		return acct;
	}


	/**
	 * @return the noActivityCnt
	 */
	public double getNoActivityCnt() {
		return noActivityCnt;
	}


	/**
	 * @return the redActivityCnt
	 */
	public double getRedActivityCnt() {
		return redActivityCnt;
	}


	/**
	 * @return the yellowActivityCnt
	 */
	public double getYellowActivityCnt() {
		return yellowActivityCnt;
	}


	/**
	 * @return the greenActivityCnt
	 */
	public double getGreenActivityCnt() {
		return greenActivityCnt;
	}


	/**
	 * @param acct the acct to set.
	 */
	public void setAcct(AccountUsersVO acct) {
		this.acct = acct;
	}


	/**
	 * @param noActivityCnt the noActivityCnt to set.
	 */
	public void setNoActivityCnt(int noActivityCnt) {
		this.noActivityCnt = noActivityCnt;
	}


	/**
	 * @param redActivityCnt the redActivityCnt to set.
	 */
	public void setRedActivityCnt(int redActivityCnt) {
		this.redActivityCnt = redActivityCnt;
	}


	/**
	 * @param yellowActivityCnt the yellowActivityCnt to set.
	 */
	public void setYellowActivityCnt(int yellowActivityCnt) {
		this.yellowActivityCnt = yellowActivityCnt;
	}


	/**
	 * @param greenActivityCnt the greenActivityCnt to set.
	 */
	public void setGreenActivityCnt(int greenActivityCnt) {
		this.greenActivityCnt = greenActivityCnt;
	}
}