package com.mindbody;

import java.rmi.RemoteException;
import java.util.List;

import org.apache.axis2.AxisFault;

import com.mindbody.vo.sales.MindBodySalesConfig;
import com.mindbodyonline.clients.api._0_5_1.Sale_x0020_ServiceStub;

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
	 * @see com.mindbody.MindBodyApiIntfc#getDocument(com.mindbody.MindBodyCallVO)
	 * TODO - Endpoints need Coded as Need Arises
	 */
	@Override
	public List<Object> getDocument(MindBodySalesConfig call) throws RemoteException {
		return null;
	}

}
