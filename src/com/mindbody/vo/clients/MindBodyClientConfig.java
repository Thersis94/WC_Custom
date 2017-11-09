package com.mindbody.vo.clients;

import java.util.List;

import com.mindbody.vo.MindBodyConfig;

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

	/**
	 * @param sourceName
	 * @param sourceKey
	 * @param siteIds
	 */
	public MindBodyClientConfig(String sourceName, String sourceKey, List<Integer> siteIds) {
		super(sourceName, sourceKey, siteIds);
	}

}
