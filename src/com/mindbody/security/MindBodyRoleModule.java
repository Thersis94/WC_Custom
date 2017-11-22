package com.mindbody.security;

import java.util.Map;

import com.smt.sitebuilder.security.DBRoleModule;

/****************************************************************************
 * <b>Title:</b> MindBodyRoleModule.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Role Module for MindBody Powered Sites.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 16, 2017
 ****************************************************************************/
public class MindBodyRoleModule extends DBRoleModule {

	/**
	 * 
	 */
	public MindBodyRoleModule() {
		super();
	}


	/**
	 * @param init
	 */
	public MindBodyRoleModule(Map<String, Object> init) {
		super(init);
	}

}
