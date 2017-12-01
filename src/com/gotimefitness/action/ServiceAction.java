package com.gotimefitness.action;

import com.mindbody.action.MindBodyServicesAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> ServiceAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Action Manages Service Related Code for Go Time Fitness.
 * Class extends basic Service functionality by adding tie in to Shopping Cart.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 28, 2017
 ****************************************************************************/
public class ServiceAction extends MindBodyServicesAction {

	public ServiceAction() {
		super();
	}

	public ServiceAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void build(ActionRequest req)  throws ActionException {
		//Add a Service to Cart.
		ShoppingCartAction sca = new ShoppingCartAction();
		sca.setAttributes(getAttributes());
		sca.addToCart(req, req.getParameter(MB_SERVICE_ID), req.getParameter(MB_SERVICE_NM), req.getParameter(MB_SERVICE_DESC), req.getIntegerParameter(MB_SERVICE_QTY), req.getDoubleParameter(MB_SERVICE_PRICE));

		//Redirect user if present.
		if(req.hasParameter(Constants.REDIRECT_URL)) {
			sendRedirect(req.getParameter(Constants.REDIRECT_URL), "Service added to Cart.", req);
		}
	}
}
