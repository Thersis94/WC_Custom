package com.mindbody.action;

import java.util.Map;

import com.mindbody.MindBodySaleApi;
import com.mindbody.MindBodySaleApi.SaleDocumentType;
import com.mindbody.util.MindBodyUtil;
import com.mindbody.vo.MindBodyResponseVO;
import com.mindbody.vo.sales.MindBodyCheckoutShoppingCartConfig;
import com.mindbody.vo.sales.MindBodyGetAcceptedCardTypeConfig;
import com.mindbody.vo.sales.MindBodyGetServicesConfig;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> MindBodySaleAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Action for building Sale related requests for the
 * MindBody Sale Apis.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 20, 2017
 ****************************************************************************/
public class MindBodySaleAction extends SimpleActionAdapter {

	/**
	 * 
	 */
	public MindBodySaleAction() {
		super();
	}


	/**
	 * @param actionInit
	 */
	public MindBodySaleAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SaleDocumentType t = getDocumentType(req.getParameter("callType"));
		Map<String, String> config = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getSiteConfig();

		switch(t) {
			case GET_ACCEPTED_CARD_TYPE:
				super.putModuleData(getAcceptedCardTypes(config));
				break;
			case GET_SERVICES:
				super.putModuleData(getServices(config, req));
				break;
			default:
				throw new ActionException("Endpoint Not Supported.");			
		}
	}

	/**
	 * Retrieves Services available from the MindBody Interface.
	 *
	 * @param config
	 * @param req
	 * @return
	 */
	public MindBodyResponseVO getServices(Map<String, String> config, ActionRequest req) {
		MindBodyGetServicesConfig conf = new MindBodyGetServicesConfig(MindBodyUtil.buildSourceCredentials(config));

		//Filter by classId if present
		if(req.hasParameter(MindBodyScheduleAction.MB_CLASS_ID)) {
			conf.setClassId(req.getIntegerParameter(MindBodyScheduleAction.MB_CLASS_ID));
		}

		//Filter by classScheduleId if present.
		if(req.hasParameter(MindBodyScheduleAction.MB_CLASS_SCHEDULE_ID)) {
			conf.setClassScheduleId(req.getIntegerParameter(MindBodyScheduleAction.MB_CLASS_SCHEDULE_ID));
		}

		return new MindBodySaleApi().getAllDocuments(conf);
	}


	/**
	 * Retrieve accepted card types from MindBody.  Results are a String list.
	 * @param config
	 * @return
	 */
	public MindBodyResponseVO getAcceptedCardTypes(Map<String, String> config) {
		MindBodyGetAcceptedCardTypeConfig conf = new MindBodyGetAcceptedCardTypeConfig(MindBodyUtil.buildSourceCredentials(config));
		return new MindBodySaleApi().getAllDocuments(conf);
	}


	@Override
	public void build(ActionRequest req) throws ActionException {
		SaleDocumentType t = getDocumentType(req.getParameter("callType"));
		Map<String, String> config = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getSiteConfig();

		switch(t) {
			case CHECKOUT_SHOPPING_CART:
				MindBodyResponseVO resp = checkoutCart(config, req);
				log.debug(resp);

				//Do we need to dress up the data?
				
				//Build Redirect

				break;
			default:
				throw new ActionException("CallType Not Supported.");
			
		}
	}
	
	/**
	 * @param req
	 * @return 
	 */
	private MindBodyResponseVO checkoutCart(Map<String, String> config, ActionRequest req) {
		MindBodyCheckoutShoppingCartConfig conf = new MindBodyCheckoutShoppingCartConfig(MindBodyUtil.buildSourceCredentials(config), MindBodyUtil.buildStaffCredentials(config));
		//Configure CheckoutshoppingCart config as necessary off Request.

		return new MindBodySaleApi().getAllDocuments(conf);
	}


	/**
	 * @param parameter
	 * @return
	 * @throws ActionException 
	 */
	private SaleDocumentType getDocumentType(String callType) throws ActionException {
		try {
			return SaleDocumentType.valueOf(callType);
		} catch(Exception e) {
			throw new ActionException("Given callType is invalid for this request: " + callType);
		}
	}
}