package com.biomed.smarttrak.fd;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.FacadeActionAdapter;

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
public class FinancialDashAction extends FacadeActionAdapter {

	private static final String FD = "fd";
	public static final String DASH_TYPE = "dashType";

	// Default financial dashboard type
	protected DashType dashType = DashType.COMMON;

	/**
	 * Values for whether the dashboard is being viewed from admin or public
	 */
	public enum DashType { 
		ADMIN, COMMON
	}

	public FinancialDashAction() {
		super();
		initMap();
	}

	/**
	 * @param actionInit
	 */
	public FinancialDashAction(ActionInitVO actionInit) {
		super(actionInit);
		initMap();
	}

	/**
	 * populates the classLoader map on the FacadeActionAdapter
	 */
	private void initMap() {
		actionMap.put("fd", com.biomed.smarttrak.fd.FinancialDashBaseAction.class);
		actionMap.put("fdOverlay", com.biomed.smarttrak.fd.FinancialDashScenarioOverlayAction.class);
		actionMap.put("fdHierarchy", com.biomed.smarttrak.admin.FinancialDashHierarchyAction.class);
		actionMap.put("fdScenario", com.biomed.smarttrak.fd.FinancialDashScenarioAction.class);
		actionMap.put("fdFootnote", com.biomed.smarttrak.fd.FinancialDashFootnoteAction.class);
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
		String actionType = StringUtil.checkVal(req.getParameter(AdminControllerAction.ACTION_TYPE), FD);

		// Set the dashboard type
		req.setAttribute(DASH_TYPE, dashType);

		// Determine the request type
		if (req.hasParameter("scenarioId") && FD.equals(actionType))
			actionType = "fdOverlay";


		// In case default was used
		req.setParameter(AdminControllerAction.ACTION_TYPE, actionType);

		log.debug("Loading FD Action: " + actionType);
		return super.loadAction(actionType);
	}
}