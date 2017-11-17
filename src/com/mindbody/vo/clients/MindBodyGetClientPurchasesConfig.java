package com.mindbody.vo.clients;

import com.mindbody.MindBodyClientApi.ClientDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> MindBodyGetClientPurchasesConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages GetClientPurchases Endpoint unique Configuration.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 13, 2017
 ****************************************************************************/
public class MindBodyGetClientPurchasesConfig extends MindBodyClientConfig {

	private Integer saleId;

	/**
	 * @param type
	 * @param source
	 * @param user
	 */
	public MindBodyGetClientPurchasesConfig(MindBodyCredentialVO source, MindBodyCredentialVO user) {
		super(ClientDocumentType.GET_CLIENT_PURCHASES, source, user);
	}

	/**
	 * @return the saleId
	 */
	public Integer getSaleId() {
		return saleId;
	}

	/**
	 * @param saleId the saleId to set.
	 */
	public void setSaleId(Integer saleId) {
		this.saleId = saleId;
	}

	@Override
	public boolean isValid() {
		return super.isValid() && !StringUtil.isEmpty(getClientId());
	}
}