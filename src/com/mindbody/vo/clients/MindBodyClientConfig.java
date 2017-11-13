package com.mindbody.vo.clients;

import com.mindbody.MindbodyClientApi.ClientDocumentType;
import com.mindbody.vo.MindBodyConfig;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodyClientVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Common Configuration for Client API Calls.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 6, 2017
 ****************************************************************************/
public class MindBodyClientConfig extends MindBodyConfig {

	private ClientDocumentType type;

	/**
	 * @param sourceName
	 * @param sourceKey
	 * @param siteIds
	 */
	public MindBodyClientConfig(ClientDocumentType type, MindBodyCredentialVO source, MindBodyCredentialVO user) {
		super(source, user);
		this.type = type;
	}

	/**
	 * @return the type
	 */
	public ClientDocumentType getType() {
		return type;
	}

	/**
	 * @param type the type to set.
	 */
	public void setType(ClientDocumentType type) {
		this.type = type;
	}

}