package com.ram.action.event;

// JDK 1.7.x
import java.util.LinkedHashMap;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionControllerFactoryImpl;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.exception.ApplicationException;
import com.siliconmtn.action.ActionRequest;
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
	 * Possible transaction types. These are placed into proper order of execution
	 * which is why this is a linked hashmap
	 */
	private final Map<String, String> transactionType = new LinkedHashMap<String, String>(){
		private static final long serialVersionUID = 1l;
		{
			put("event_group", "com.ram.action.event.InventoryEventGroupAction");
			put("event", "com.ram.action.event.InventoryEventAction");
			put("event_return", "com.ram.action.event.InventoryEventReturnAction");
			put("event_customer", "com.ram.action.event.InventoryEventCustomerAction");
			put("event_auditor", "com.ram.action.event.InventoryEventAuditorAction");
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
	public void retrieve(ActionRequest req) throws ActionException {
		String transType = StringUtil.checkVal(req.getParameter("transType"), DEFAULT_ACTION);
		ActionInitVO ai = new ActionInitVO(transactionType.get(transType));
		log.debug("retrieving " + transType);
		try {
			ActionInterface sai = new ActionControllerFactoryImpl().getActionInstance(ai);
			sai.setAttributes(getAttributes());
			sai.setDBConnection(getDBConnection());
			sai.retrieve(req);
			
		} catch (ApplicationException e) {
			throw new ActionException("Unable to retrieve data for " + transType, e);
		}

	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ActionInterface sai;
		try {
			for (String key : transactionType.keySet()) {
				log.debug("building " + key);
				ActionInitVO ai = new ActionInitVO(transactionType.get(key));
				sai = new ActionControllerFactoryImpl().getActionInstance(ai);
				sai.setAttributes(getAttributes());
				sai.setDBConnection(getDBConnection());
				sai.update(req);
			}

		} catch (ApplicationException e) {
			throw new ActionException("Unable to update/build event data", e);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
}
