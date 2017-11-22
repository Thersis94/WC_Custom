package com.mindbody.vo.clients;

import com.mindbody.MindBodyClientApi.ClientDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodyGetClientReferralTypesConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages GetClientReferralTypes Endpoint unique Configuration.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 20, 2017
 ****************************************************************************/
public class MindBodyGetClientReferralTypesConfig extends MindBodyClientConfig {

	/**
	 * @param type
	 * @param source
	 * @param user
	 */
	public MindBodyGetClientReferralTypesConfig(MindBodyCredentialVO source) {
		super(ClientDocumentType.GET_CLIENT_REFERRAL_TYPES, source, null);
	}
}