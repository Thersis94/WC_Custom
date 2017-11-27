package com.mindbody.vo.site;

import com.mindbody.MindBodySiteApi.SiteDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodyGetSessionTypesConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages MindBody Get Session Type Endpoint Config.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 26, 2017
 ****************************************************************************/
public class MindBodyGetSessionTypesConfig extends MindBodySiteConfig {

	/**
	 * @param type
	 * @param source
	 * @param user
	 */
	public MindBodyGetSessionTypesConfig(MindBodyCredentialVO source) {
		super(SiteDocumentType.GET_SESSION_TYPES, source, null);
	}
}