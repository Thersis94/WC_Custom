package com.mindbody.vo.site;

import com.mindbody.MindBodySiteApi.SiteDocumentType;
import com.mindbody.vo.MindBodyConfig;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodySiteConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Common Configuration for Site API Calls.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 26, 2017
 ****************************************************************************/
public class MindBodySiteConfig extends MindBodyConfig {

	private SiteDocumentType type;
	/**
	 * @param source
	 * @param user
	 */
	public MindBodySiteConfig(SiteDocumentType type, MindBodyCredentialVO source, MindBodyCredentialVO user) {
		super(source, user);
		this.type = type;
	}

	/**
	 * @return
	 */
	public SiteDocumentType getType() {
		return type;
	}	
}
