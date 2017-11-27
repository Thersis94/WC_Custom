package com.mindbody.vo.site;

import com.mindbody.MindBodySiteApi.SiteDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodyGetRelationshipsConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages MindBody Get Relationships Endpoint Config.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 26, 2017
 ****************************************************************************/
public class MindBodyGetRelationshipsConfig extends MindBodySiteConfig {

	/**
	 * @param type
	 * @param source
	 * @param user
	 */
	public MindBodyGetRelationshipsConfig(MindBodyCredentialVO source) {
		super(SiteDocumentType.GET_RELATIONSHIPS, source, null);
	}
}