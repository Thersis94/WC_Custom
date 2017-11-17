package com.mindbody.vo.clients;

import com.mindbody.MindBodyClientApi.ClientDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodyAddArrivalConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages AddArrival Endpoint unique Configuration.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 13, 2017
 ****************************************************************************/
public class MindBodyAddArrivalConfig extends MindBodyClientConfig {

	/**
	 * @param type
	 * @param source
	 * @param user
	 */
	public MindBodyAddArrivalConfig(MindBodyCredentialVO source, MindBodyCredentialVO user) {
		super(ClientDocumentType.ADD_ARRIVAL, source, user);
	}

	@Override
	public boolean isValid() {
		return super.isValid() && !getClientIds().isEmpty();
	}

	/**
	 * @return
	 */
	public String getClientId() {
		return getClientIds().get(0);
	}

	public Integer getLocationId() {
		return getLocationIds().get(0);
	}
}