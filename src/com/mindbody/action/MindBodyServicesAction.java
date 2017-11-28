package com.mindbody.action;

import java.util.Map;

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

	public void retrieve(ActionRequest req) throws ActionException {
		Map<String, String> config = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getSiteConfig();
		putModuleData(getServices(config, req));
	}

	public void build(ActionRequest req) throws ActionException {
		//Add a Service to Cart.
	}
}