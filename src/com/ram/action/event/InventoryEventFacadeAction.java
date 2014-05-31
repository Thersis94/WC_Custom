package com.ram.action.event;

// JDK 1.7.x
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.action.ActionControllerFactoryImpl;
// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.ApplicationException;
import com.siliconmtn.http.SMTServletRequest;

import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: InventoryEventFacadeAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b>Facade action to manage the various Event actions
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since May 31, 2014<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class InventoryEventFacadeAction extends SBActionAdapter {
	
	/**
	 * If not transaction type is passed, use the default action
	 */
	public static final String DEFAULT_ACTION = "event";
	
	/**
	 * Possible transaction types
	 */
	private static final Map<String, String> transactionType = new HashMap<String, String>(){
		private static final long serialVersionUID = 1l;
		{
			put("event", "com.ram.action.event.InventoryEventAction");
			put("event_return", "com.ram.action.event.InventoryEventReturnAction");
			put("event_group", "com.ram.action.event.InventoryEventGroupAction");
			put("event_customer", "com.ram.action.event.InventoryEventCustomerAction");
		}
	};
	
	/**
	 * 
	 */
	public InventoryEventFacadeAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public InventoryEventFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		String transType = StringUtil.checkVal(req.getParameter("transType"), DEFAULT_ACTION);
		ActionInitVO ai = new ActionInitVO(transactionType.get(transType));
		ActionControllerFactoryImpl factory = new ActionControllerFactoryImpl();
		
		try {
			SMTActionInterface sai = factory.getInstance(ai);
			sai.setAttributes(getAttributes());
			sai.setDBConnection(getDBConnection());
			sai.retrieve(req);
			
		} catch (ApplicationException e) {
			throw new ActionException("Unable to retrieve data for" + transType, e);
		}

	}

}
