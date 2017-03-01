package com.biomed.smarttrak;

import java.lang.reflect.Constructor;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: FinancialDashAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Feb 06, 2017
 ****************************************************************************/

public class FinancialDashAction extends SBActionAdapter {
	
	private static final String FD = "fd";
	
	private enum FdActionType {
		fd("com.biomed.smarttrak.FinancialDashBaseAction"),
		fdOverlay("com.biomed.smarttrak.FinancialDashScenarioOverlayAction"),
		fdHierarchy("com.biomed.smarttrak.admin.FinancialDashHierarchyAction"),
		fdScenario("com.biomed.smarttrak.FinancialDashScenarioAction");
		
		private String klass;
		
		FdActionType(String klass) {
			this.klass = klass;
		}
		
		public String getKlass() {
			return klass;
		}
	}
	
	public FinancialDashAction() {
		super();
	}

	public FinancialDashAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		super.retrieve(req);
		
		ActionInterface ai = getAction(req);
		ai.retrieve(req);
	}
	
	@Override
	public void build(ActionRequest req) throws ActionException {
		super.build(req);
		
		ActionInterface ai = getAction(req);
		ai.build(req);
	}
	
	/**
	 * Gets the appropriate action based on the passed data.
	 * 
	 * Base: Standard display of data.
	 * Overlay: Display of data with scenario data used in place of standard data (where applicable).
	 * 
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private ActionInterface getAction(ActionRequest req) throws ActionException {
		String scenarioId = StringUtil.checkVal(req.getParameter("scenarioId"));
		String actionType = StringUtil.checkVal(req.getParameter("actionType"), FD);
		
		// Determine the request type
		FdActionType action;
		if (scenarioId.length() > 0 && FD.equals(actionType)) {
			action = FdActionType.fdOverlay;
		} else {
			action = FdActionType.valueOf(actionType);
		}
		
		log.debug("Starting FD Action: " + action.getKlass());

		// Forward to the appropriate action
		ActionInterface ai;
		try {
			Class<?> klass = Class.forName(action.getKlass());
			Constructor<?> constructor = klass.getConstructor(ActionInitVO.class);
			ai = (ActionInterface) constructor.newInstance(this.actionInit);
		} catch (Exception e) {
			throw new ActionException("Could not instantiate FD class.", e);
		}

		// Set the appropriate attributes for the action
		ai.setAttributes(this.attributes);
		ai.setDBConnection(dbConn);
		
		// In case default was used
		req.setParameter("actionType", actionType);

		return ai;
	}
}
