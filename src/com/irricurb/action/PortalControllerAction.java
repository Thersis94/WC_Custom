package com.irricurb.action;

import java.util.HashMap;
import java.util.Map;

import com.irricurb.action.project.ProjectFacadeAction;
import com.irricurb.util.LookupAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title</b>: PortalController.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Controller class that directs requests according to req params
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Dec 11, 2017
 * @updates:
 ****************************************************************************/
public class PortalControllerAction extends SimpleActionAdapter {
	
	/*
	 * Request param to determine action to execute
	 */
	public static final String ACTION_TYPE = "actionType";

	/*
	 * Where users get redirected when they're not authorized
	 */
	public static final String PUBLIC_401_PG = "/portal";
	
	/*
	 * Actions this Controller can execute
	 */
	protected static final Map<String, Class<? extends ActionInterface>> ACTIONS;
	
	/**
	 * Populates the action map. This will make our map live once in the JVM.
	 */
	static {
		ACTIONS = new HashMap<>(1);
		ACTIONS.put("PROJECT", ProjectFacadeAction.class);
		ACTIONS.put("UTIL", LookupAction.class);
		
	}
	
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("portal control retrieve action type " + req.getParameter(ACTION_TYPE));
		if (req.hasParameter(ACTION_TYPE)) {
			loadAction(req.getParameter(ACTION_TYPE)).retrieve(req);
		} else {
			//go to view, display the content from the WYSWIYG in /admintool
			super.retrieve(req);
		}
	}
	
	/**
	 * Based on the passed actionType, instantiate the appropriate class and return.
	 * 
	 * @param actionType
	 * @return
	 * @throws ActionException
	 */
	protected ActionInterface loadAction(String actionType) throws ActionException {
		Class<?> c = ACTIONS.get(actionType);
		if (c == null) 
			throw new ActionException("Unknown action type: " + actionType);

		// Instantiate the action & return it - pass attributes & dbConn
		try {
			ActionInterface action = (ActionInterface) c.newInstance();
			action.setDBConnection(dbConn);
			action.setAttributes(getAttributes());
			return action;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ActionException("Problem instantiating action type: " + actionType);
		}
	}
}
