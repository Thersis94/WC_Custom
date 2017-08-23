package com.biomed.smarttrak.fd;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;

import com.smt.sitebuilder.action.SBActionAdapter;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.security.SecurityController;

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
	public static final String DASH_TYPE = "dashType";

	// Default financial dashboard type
	protected DashType dashType = DashType.COMMON;

	/**
	 * Values for whether the dashboard is being viewed from admin or public
	 */
	public enum DashType {ADMIN, COMMON}

	/**
	 * Map of financial dash classes
	 */
	public static final Map<String, String> FD_ACTIONS;
	
	static {
		Map<String, String> fdActions = new HashMap<>();
		fdActions.put("fd", "com.biomed.smarttrak.fd.FinancialDashBaseAction");
		fdActions.put("fdOverlay", "com.biomed.smarttrak.fd.FinancialDashScenarioOverlayAction");
		fdActions.put("fdHierarchy", "com.biomed.smarttrak.admin.FinancialDashHierarchyAction");
		fdActions.put("fdScenario", "com.biomed.smarttrak.fd.FinancialDashScenarioAction");
		fdActions.put("fdFootnote", "com.biomed.smarttrak.fd.FinancialDashFootnoteAction");

		FD_ACTIONS = Collections.unmodifiableMap(fdActions);
	}

	public FinancialDashAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public FinancialDashAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SecurityController.isFdAuth(req);
		getAction(req).retrieve(req);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		getAction(req).build(req);
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
		String actionType = StringUtil.checkVal(req.getParameter(AdminControllerAction.ACTION_TYPE), FD);

		// Set the dashboard type
		req.setAttribute(DASH_TYPE, dashType);

		// Determine the request type
		String action;
		if (scenarioId.length() > 0 && FD.equals(actionType)) {
			action = FD_ACTIONS.get("fdOverlay");
		} else {
			action = FD_ACTIONS.get(actionType);
		}

		log.debug("Starting FD Action: " + action);

		// Forward to the appropriate action
		ActionInterface ai;
		try {
			Class<?> klass = Class.forName(action);
			Constructor<?> constructor = klass.getConstructor(ActionInitVO.class);
			ai = (ActionInterface) constructor.newInstance(this.actionInit);
		} catch (Exception e) {
			throw new ActionException("Could not instantiate FD class.", e);
		}

		// Set the appropriate attributes for the action
		ai.setAttributes(this.attributes);
		ai.setDBConnection(dbConn);

		// In case default was used
		req.setParameter(AdminControllerAction.ACTION_TYPE, actionType);

		return ai;
	}
}