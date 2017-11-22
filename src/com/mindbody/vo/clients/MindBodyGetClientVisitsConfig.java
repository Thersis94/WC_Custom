package com.mindbody.vo.clients;

import com.mindbody.MindBodyClientApi.ClientDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> MindBodyGetClientVisitsConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages GetClientVisits Endpoint unique Configuration.
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
	public MindBodyGetClientVisitsConfig(MindBodyCredentialVO source) {
		super(ClientDocumentType.GET_CLIENT_VISITS, source, null);
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
