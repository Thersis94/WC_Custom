package com.restpeer.action.admin;

import com.siliconmtn.action.ActionException;
// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: AttributeWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the attributes / products being sold
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 13, 2019
 * @updates:
 ****************************************************************************/

public class AttributeWidget extends SBActionAdapter {

	/**
	 * Json key to access this action
	 */
	public static final String AJAX_KEY = "attribute";
	
	/**
	 * 
	 */
	public AttributeWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public AttributeWidget(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		
	}
}

