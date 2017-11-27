package com.mindbody;

import java.rmi.RemoteException;

import org.apache.axis2.AxisFault;

import com.mindbody.util.MindBodyUtil;
import com.mindbody.vo.MindBodyResponseVO;
import com.mindbody.vo.sales.MindBodyCheckoutShoppingCartConfig;
import com.mindbody.vo.sales.MindBodyGetAcceptedCardTypeConfig;
import com.mindbody.vo.sales.MindBodyGetContractsConfig;
import com.mindbody.vo.sales.MindBodyGetServicesConfig;
import com.mindbody.vo.sales.MindBodySalesConfig;
import com.mindbodyonline.clients.api._0_5_1.CheckoutShoppingCartDocument;
import com.mindbodyonline.clients.api._0_5_1.CheckoutShoppingCartRequest;
import com.mindbodyonline.clients.api._0_5_1.CheckoutShoppingCartResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.CheckoutShoppingCartResult;
import com.mindbodyonline.clients.api._0_5_1.Contract;
import com.mindbodyonline.clients.api._0_5_1.GetAcceptedCardTypeDocument;
import com.mindbodyonline.clients.api._0_5_1.GetAcceptedCardTypeRequest;
import com.mindbodyonline.clients.api._0_5_1.GetAcceptedCardTypeResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetAcceptedCardTypeResult;
import com.mindbodyonline.clients.api._0_5_1.GetContractsDocument;
import com.mindbodyonline.clients.api._0_5_1.GetContractsRequest;
import com.mindbodyonline.clients.api._0_5_1.GetContractsResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetContractsResult;
import com.mindbodyonline.clients.api._0_5_1.GetServicesDocument;
import com.mindbodyonline.clients.api._0_5_1.GetServicesRequest;
import com.mindbodyonline.clients.api._0_5_1.GetServicesResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetServicesResult;
//Mind Body Sale API Jar
import com.mindbodyonline.clients.api._0_5_1.Sale_x0020_ServiceStub;
import com.mindbodyonline.clients.api._0_5_1.Service;
//Base Libs
import com.siliconmtn.common.http.HttpStatus;
import com.siliconmtn.util.StringUtil;

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
		MindBodyResponseVO resp = null;

		switch(config.getType()) {
			case CHECKOUT_SHOPPING_CART:
				resp = checkoutShoppingCart((MindBodyCheckoutShoppingCartConfig) config);
				break;
			case GET_ACCEPTED_CARD_TYPE:
				resp = getAcceptedCardType((MindBodyGetAcceptedCardTypeConfig) config);
				break;
			case GET_CONTRACTS:
				resp = getContracts((MindBodyGetContractsConfig) config);
				break;
			case GET_SERVICES:
				resp = getServices((MindBodyGetServicesConfig) config);
				break;
			default:
				log.error("Endpoint Not Supported.");
				resp = buildErrorResponse(HttpStatus.CD_501_NOT_IMPLEMENTED, "Endpoint Not Supported");
				break;
		}
		return resp;
	}

	/**
	 * This is not thoroughly Tested.
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO checkoutShoppingCart(MindBodyCheckoutShoppingCartConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		CheckoutShoppingCartRequest req = CheckoutShoppingCartRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureCheckoutChoppingCartRequest(req, config);

		CheckoutShoppingCartDocument doc = CheckoutShoppingCartDocument.Factory.newInstance();
		doc.addNewCheckoutShoppingCart().setRequest(req);

		Sale_x0020_ServiceStub client = getConfiguredStub();
		CheckoutShoppingCartResponseDocument res = client.checkoutShoppingCart(doc);
		CheckoutShoppingCartResult r = res.getCheckoutShoppingCartResponse().getCheckoutShoppingCartResult();
		resp.populateResponseFields(r);
		log.info(r);
		if(resp.isValid()) {
			resp.addResults(MindBodyUtil.convertShoppingCart(r.getShoppingCart()));
		}

		return resp;
	}

	/**
	 * @param req
	 * @param config
	 */
	private void configureCheckoutChoppingCartRequest(CheckoutShoppingCartRequest req, MindBodyCheckoutShoppingCartConfig config) {
		req.setClientID(config.getClientId());
		req.setCartItems(MindBodyUtil.buildArrayOfCartItem(config.getCartItems()));
		req.setPayments(MindBodyUtil.buildArrayOfPayments(config.getPayments()));

		if(!StringUtil.isEmpty(config.getCartId())) {
			req.setCartID(config.getCartId());
		}

		if(config.isInStore()) {
			req.setInStore(config.isInStore());
		}

		if(!StringUtil.isEmpty(config.getPromotionCode())) {
			req.setPromotionCode(config.getPromotionCode());
		}
	}

	/**
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO getAcceptedCardType(MindBodyGetAcceptedCardTypeConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetAcceptedCardTypeRequest req = GetAcceptedCardTypeRequest.Factory.newInstance();
		prepareRequest(req, config);

		GetAcceptedCardTypeDocument doc = GetAcceptedCardTypeDocument.Factory.newInstance();
		doc.addNewGetAcceptedCardType().setRequest(req);

		GetAcceptedCardTypeResponseDocument res = getConfiguredStub().getAcceptedCardType(doc);
		GetAcceptedCardTypeResult r = res.getGetAcceptedCardTypeResponse().getGetAcceptedCardTypeResult();
		resp.populateResponseFields(r);
		log.info(r);
		if(resp.isValid()) {
			for(String t : r.getCardTypes().getStringArray()) {
				resp.addResults(t);
			}
		}

		return resp;
	}

	/**
	 * TODO - Add ConvertContract to MindBodyUtil.
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO getContracts(MindBodyGetContractsConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetContractsRequest req = GetContractsRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureContractRequest(req, config);

		GetContractsDocument doc = GetContractsDocument.Factory.newInstance();
		doc.addNewGetContracts().setRequest(req);

		GetContractsResponseDocument res = getConfiguredStub().getContracts(doc);
		GetContractsResult r = res.getGetContractsResponse().getGetContractsResult();
		resp.populateResponseFields(r);
		log.info(r);
		if(resp.isValid()) {
			for(Contract c : r.getContracts().getContractArray()) {
				resp.addResults(c);
			}
		}

		return resp;
	}

	/**
	 * @param req
	 * @param config
	 */
	private void configureContractRequest(GetContractsRequest req, MindBodyGetContractsConfig config) {
		if(!config.getContractIds().isEmpty()) {
			req.setContractIDs(MindBodyUtil.buildArrayOfInt(config.getContractIds()));
		}

		req.setLocationID(config.getLocationId());

		if(config.isSoldOnline()) {
			req.setSoldOnline(config.isSoldOnline());
		}
	}

	/**
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO getServices(MindBodyGetServicesConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetServicesRequest req = GetServicesRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureServiceRequest(req, config);

		GetServicesDocument doc = GetServicesDocument.Factory.newInstance();
		doc.addNewGetServices().setRequest(req);

		GetServicesResponseDocument res = getConfiguredStub().getServices(doc);
		GetServicesResult r = res.getGetServicesResponse().getGetServicesResult();
		resp.populateResponseFields(r);
		log.info(r);
		if(resp.isValid()) {
			for(Service s : r.getServices().getServiceArray()) {
				resp.addResults(MindBodyUtil.convertService(s));
			}
		}

		return resp;
	}

	/**
	 * @param req
	 * @param config
	 */
	private void configureServiceRequest(GetServicesRequest req, MindBodyGetServicesConfig config) {
		if(!config.getProgramIds().isEmpty()) {
			req.setProgramIDs(MindBodyUtil.buildArrayOfInt(config.getProgramIds()));
		}
		if(!config.getSessionTypeIds().isEmpty()) {
			req.setSessionTypeIDs(MindBodyUtil.buildArrayOfInt(config.getSessionTypeIds()));
		}
		if(!config.getServiceIds().isEmpty()) {
			req.setServiceIDs(MindBodyUtil.buildArrayOfString(config.getServiceIds()));
		}
		if(config.getClassId() != null) {
			req.setClassID(config.getClassId());
		}
		if(config.getClassScheduleId() != null) {
			req.setClassScheduleID(config.getClassScheduleId());
		}
		if(config.isSellOnline()) {
			req.setSellOnline(config.isSellOnline());
		}
		if(config.getLocationId() != null) {
			req.setLocationID(config.getLocationId());
		}
		if(config.isHideRelatedPrograms()) {
			req.setHideRelatedPrograms(config.isHideRelatedPrograms());
		}
		if(config.getStaffId() != null) {
			req.setStaffID(config.getStaffId());
		}
	}
}