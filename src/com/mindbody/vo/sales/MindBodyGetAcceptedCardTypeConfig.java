package com.mindbody.vo.sales;

import com.mindbody.MindBodySaleApi.SaleDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodyGetAcceptedCardTypeConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages Config for GetAcceptedCardType Endpoint
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 26, 2017
 ****************************************************************************/
public class MindBodyGetAcceptedCardTypeConfig extends MindBodySalesConfig {
	/**
	 * @param type
	 * @param source
	 * @param user
	 */
	public MindBodyGetAcceptedCardTypeConfig(MindBodyCredentialVO source) {
		super(SaleDocumentType.GET_ACCEPTED_CARD_TYPE, source, null);
	}
}