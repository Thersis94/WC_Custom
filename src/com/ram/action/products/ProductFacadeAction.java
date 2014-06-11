package com.ram.action.products;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: KitFacadeAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Facade Action for the RAM Kit DataTool.  This Action
 * looks at the stage variable on the requests and ensures that the proper 
 * action is called to process the data.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since May 20, 2014
 *        <p/>
 *        <b>Changes: </b>
 ***************************************************************************
 */
public class ProductFacadeAction extends SBActionAdapter {

	public static final String STEP_PARAM = "bType";
	public static enum KIT_STEP {PRODUCT, PRODUCTRECALL, KIT, KITLAYER, KITPRODUCT, KITCOORDINATE, CUSTOMER}
	/**
	 * Default Constructor
	 */
	public ProductFacadeAction() {
		super();
		
	}
	/**
	 * Generic Constructor with ActionInitVO data
	 * @param actionInit
	 */
	public ProductFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		String step = StringUtil.checkVal(req.getParameter(STEP_PARAM));
		getAction(step).build(req);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		String step = StringUtil.checkVal(req.getParameter(STEP_PARAM));
		getAction(step).retrieve(req);
	}
	
	/**
	 * Build the Action and set the necessary pieces on it before returning it.
	 * @param step
	 * @return
	 */
	public SMTActionInterface getAction(String step) {
		SMTActionInterface action = null;
		try {
			switch(KIT_STEP.valueOf(step)) {
				case KITLAYER:
					action = new KitLayerAction(actionInit);
					break;
				case KITPRODUCT:
					action = new KitLayerProductAction(actionInit);
					break;
				case KITCOORDINATE:
					action = new KitLayerCoordinateAction(actionInit);
					break;
				case KIT:
					action = new KitAction(actionInit);
					break;
				case PRODUCTRECALL:
					action = new ProductRecallAction(actionInit);
					break;
				case CUSTOMER:
					action = new CustomerProductAction(actionInit);
					break;
				case PRODUCT:
				default:
					action = new ProductAction(actionInit);
					break;
			}
		} catch(IllegalArgumentException iae) {
			action = new ProductAction(actionInit);
		}
		action.setDBConnection(dbConn);
		action.setAttributes(attributes);
		return action;
	}
	
}
