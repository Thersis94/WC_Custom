package com.mindbody.vo.clients;

import java.util.Date;

import com.mindbody.MindbodyClientApi.ClientDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodyGetClientAccountBalancesConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages GetClientAccountBalances Endpoint unique Configuration.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 13, 2017
 ****************************************************************************/
public class MindBodyGetClientAccountBalancesConfig extends MindBodyClientConfig {

	private Date balanceDate;
	private Long classId;

	/**
	 * @param type
	 * @param source
	 * @param user
	 */
	public MindBodyGetClientAccountBalancesConfig(MindBodyCredentialVO source, MindBodyCredentialVO user) {
		super(ClientDocumentType.GET_CLIENT_ACCOUNT_BALANCES, source, user);
	}

	/**
	 * @return the balanceDate
	 */
	public Date getBalanceDate() {
		return balanceDate;
	}

	/**
	 * @return the classId
	 */
	public Long getClassId() {
		return classId;
	}

	/**
	 * @param balanceDate the balanceDate to set.
	 */
	public void setBalanceDate(Date balanceDate) {
		this.balanceDate = balanceDate;
	}

	/**
	 * @param classId the classId to set.
	 */
	public void setClassId(Long classId) {
		this.classId = classId;
	}
}