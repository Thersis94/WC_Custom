package com.mindbody.vo.clients;

import com.mindbody.MindbodyClientApi.ClientDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodyGetCustomClientFieldsConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> TODO
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 13, 2017
 ****************************************************************************/
public class MindBodyGetCustomClientFieldsConfig extends MindBodyClientConfig {

	/**
	 * @param type
	 * @param source
	 * @param user
	 */
	public MindBodyGetCustomClientFieldsConfig(MindBodyCredentialVO source, MindBodyCredentialVO user) {
		super(ClientDocumentType.GET_CUSTOM_CLIENT_FIELDS, source, user);
	}

}
