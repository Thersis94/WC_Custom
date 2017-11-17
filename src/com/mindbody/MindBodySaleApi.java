package com.mindbody;

import java.rmi.RemoteException;

import org.apache.axis2.AxisFault;

import com.mindbody.vo.MindBodyResponseVO;
import com.mindbody.vo.sales.MindBodySalesConfig;

//Mind Body Sale API Jar
import com.mindbodyonline.clients.api._0_5_1.Sale_x0020_ServiceStub;

//Base Libs
import com.siliconmtn.common.http.HttpStatus;

/****************************************************************************
 * <b>Title:</b> MindBodySaleApi.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manage Mind Body Sales Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 3, 2017
 ****************************************************************************/
public class MindBodySaleApi extends AbstractMindBodyApi<Sale_x0020_ServiceStub, MindBodySalesConfig> {

	public enum SaleDocumentType {
		GET_ACCEPTED_CARD_TYPE,
		CHECKOUT_SHOPPING_CART,
		GET_SALES,
		GET_SERVICES,
		UPDATE_SERVICES,
		GET_PACKAGES,
		GET_PRODUCTS,
		GET_CONTRACTS,
		PURCHASE_CONTRACTS,
		UPDATE_PRODUCTS,
		REDEEM_SPA_FINDER_WELLNESS_CARD,
		GET_CUSTOM_PAYMENT_METHODS,
		UPDATE_SALE_DATE,
		RETURN_SALE
	}
	/**
	 * 
	 */
	public MindBodySaleApi() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.mindbody.MindBodyApiIntfc#getStub()
	 */
	@Override
	public Sale_x0020_ServiceStub getStub() throws AxisFault {
		return new Sale_x0020_ServiceStub();
	}

	/* (non-Javadoc)
	 * @see com.mindbody.AbstractMindBodyApi#processRequest(com.mindbody.vo.MindBodyConfig)
	 */
	@Override
	protected MindBodyResponseVO processRequest(MindBodySalesConfig config) throws RemoteException {
		return buildErrorResponse(HttpStatus.CD_501_NOT_IMPLEMENTED, "Endpoint Not Supported");
	}
}