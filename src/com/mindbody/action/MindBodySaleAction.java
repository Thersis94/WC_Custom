package com.mindbody.action;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SimpleActionAdapter;

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
		
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		
	}
}