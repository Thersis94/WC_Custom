package com.mindbody.vo.sales;

import java.util.List;

import com.mindbody.vo.MindBodyConfig;

/****************************************************************************
 * <b>Title:</b> MindBodySalesVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> TODO
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 6, 2017
 ****************************************************************************/
public class MindBodySalesConfig extends MindBodyConfig {

	/**
	 * @param sourceName
	 * @param sourceKey
	 * @param siteIds
	 */
	public MindBodySalesConfig(String sourceName, String sourceKey, List<Integer> siteIds) {
		super(sourceName, sourceKey, siteIds);
	}

}
