package com.mindbody.vo.clients;

import com.mindbody.MindbodyClientApi.ClientDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

import opennlp.tools.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> MindBodyGetClientVisitsConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> TODO
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 13, 2017
 ****************************************************************************/
public class MindBodyGetClientVisitsConfig extends MindBodyClientConfig {

	private boolean unpaidsOnly;

	/**
	 * @param type
	 * @param source
	 * @param user
	 */
	public MindBodyGetClientVisitsConfig(MindBodyCredentialVO source, MindBodyCredentialVO user) {
		super(ClientDocumentType.GET_CLIENT_VISITS, source, user);
	}

	/**
	 * @return the unpaidsOnly
	 */
	public boolean isUnpaidsOnly() {
		return unpaidsOnly;
	}

	/**
	 * @param unpaidsOnly the unpaidsOnly to set.
	 */
	public void setUnpaidsOnly(boolean unpaidsOnly) {
		this.unpaidsOnly = unpaidsOnly;
	}

	@Override
	public boolean isValid() {
		return super.isValid() && !StringUtil.isEmpty(getClientId());
	}
}
