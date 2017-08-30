package com.ram.action.products;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;

import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title</b>ProductCartFacadeAction.java<p/>
 * <b>Description: Handles case search and cart functionality for the 
 * ram site.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since June 30, 2017
 * <b>Changes: </b>
 ****************************************************************************/

public class ProductCartFacadeAction extends SimpleActionAdapter {

	public ProductCartFacadeAction() {
		super();
	}

	/**
	 * @param avo
	 */
	public ProductCartFacadeAction(ActionInitVO avo) {
		super(avo);
	}

	/**
	 * Returns the appropriate action
	 * 
	 * @return
	 */
	public ActionInterface getAction(ActionRequest req) {
		ActionInterface action = null;
		String step = StringUtil.checkVal(req.getParameter("step"));
		
		if (StringUtil.isEmpty(step)) {
			action = new CaseSearchAction(actionInit);
		} else {
			action = new ProductCartAction(actionInit);
		}

		action.setDBConnection(dbConn);
		action.setAttributes(attributes);

		return action;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		getAction(req).build(req);
	}
	
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		getAction(req).retrieve(req);
	}
}
