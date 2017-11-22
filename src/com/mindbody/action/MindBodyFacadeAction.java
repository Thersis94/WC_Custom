package com.mindbody.action;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title:</b> MindBodyFacadeAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Acts as Gateway for all MindBody Interactions.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 20, 2017
 ****************************************************************************/
public class MindBodyFacadeAction extends SBActionAdapter {

	/**
	 * 
	 */
	public MindBodyFacadeAction() {
	}


	/**
	 * @param actionInit
	 */
	public MindBodyFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	public void retrieve(ActionRequest req) throws ActionException {
		
	}

	public void build(ActionRequest req) throws ActionException {
		
	}
}