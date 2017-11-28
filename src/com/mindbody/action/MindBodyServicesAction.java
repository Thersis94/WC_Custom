package com.mindbody.action;

import java.util.Map;

import com.gotimefitness.action.ShoppingCartAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> MindBodyServicesAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manage MindBody Services Interactions.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 27, 2017
 ****************************************************************************/
public class MindBodyServicesAction extends MindBodySaleAction {

	public static final String MB_SERVICE_ID = "mbServiceId";
	public static final String MB_SERVICE_NM = "mbServiceNm";
	public static final String MB_SERVICE_DESC = "mbServiceDesc";
	public static final String MB_SERVICE_QTY = "mbServiceQty";
	public static final String MB_SERVICE_PRICE = "mbServicePrice";

	/**
	 * 
	 */
	public MindBodyServicesAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public MindBodyServicesAction(ActionInitVO arg0) {
		super(arg0);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		Map<String, String> config = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getSiteConfig();
		putModuleData(getServices(config, req));
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		//Add a Service to Cart.
		ShoppingCartAction sca = new ShoppingCartAction();
		sca.setAttributes(getAttributes());
		sca.addToCart(req, req.getParameter(MB_SERVICE_ID), req.getParameter(MB_SERVICE_NM), req.getParameter(MB_SERVICE_DESC), req.getIntegerParameter(MB_SERVICE_QTY), req.getDoubleParameter(MB_SERVICE_PRICE));

		if(req.hasParameter(Constants.REDIRECT_URL)) {
			sendRedirect(req.getParameter(Constants.REDIRECT_URL), "Service added to Cart.", req);
		}
	}
}