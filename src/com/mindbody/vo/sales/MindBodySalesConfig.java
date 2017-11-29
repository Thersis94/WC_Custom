package com.mindbody.vo.sales;

import com.mindbody.MindBodySaleApi.SaleDocumentType;
import com.mindbody.vo.MindBodyConfig;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodySalesVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Common Configuration for Sales API Calls.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 6, 2017
 ****************************************************************************/
public class MindBodySalesConfig extends MindBodyConfig {

	private SaleDocumentType type;

	/**
	 * @param sourceName
	 * @param sourceKey
	 * @param siteIds
	 */
	public MindBodySalesConfig(SaleDocumentType type, MindBodyCredentialVO source, MindBodyCredentialVO user) {
		super(source, user);
		this.type = type;
	}

	public SaleDocumentType getType() {
		return type;
	}
}