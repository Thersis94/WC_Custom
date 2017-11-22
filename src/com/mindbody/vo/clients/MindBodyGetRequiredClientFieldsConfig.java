package com.mindbody.vo.clients;

import com.mindbody.MindBodyClientApi.ClientDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodyGetRequiredClientFieldsConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages GetRequiredClientFields Endpoint unique Configuration.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 14, 2017
 ****************************************************************************/
public class MindBodyGetRequiredClientFieldsConfig extends MindBodyClientConfig {

	/**
	 * @param type
	 * @param source
	 * @param user
	 */
	public MindBodyGetRequiredClientFieldsConfig(MindBodyCredentialVO source) {
		super(ClientDocumentType.GET_REQUIRED_CLIENT_FIELDS, source, null);
	}
}