package com.ram.action;

// JDK 1.8
import java.util.HashMap;
import java.util.Map;

// WC Custom Libs
import com.ram.action.products.ProductCartFacadeAction;
import com.ram.action.util.LookupAction;
//SMT base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC core
import com.smt.sitebuilder.action.SimpleActionAdapter;

//WC Email Campaigns
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: PortalControllerAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Controller for the RAM portal (AE, OR, SPD, etc).  
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * @author Tim Johnson
 * @version 1.0
 * @since June 28, 2017
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

	public PortalControllerAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public PortalControllerAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Populates the action map. This will make our map live once in the JVM.
	 */
	static {
		ACTIONS = new HashMap<>(1);
		ACTIONS.put("RAM_OR", ProductCartFacadeAction.class);
		ACTIONS.put("UTIL", LookupAction.class);
		ACTIONS.put("SPD", SPDAction.class);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		// Pass to superclass for widget registration.
		super.retrieve(req);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		String actionType = req.getParameter(ACTION_TYPE);
		String msg;
		
		try {
			loadAction(actionType).build(req);
			msg = (String) getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		} catch (ActionException ae) {
			log.error("Could not execute action type: " + actionType, ae.getCause());
			msg = (String) getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		}

		// Only proceed to redirect if it not a json request (?json=true)
		if (Convert.formatBoolean(req.getParameter("json")))
			return;

		// Setup the redirect. Build a URL for 'this' page if a child action didn't build one of it's own.
		// NOTE: The controller should (and does) control the redirect. It also sets 'msg' properly if the child action pukes.
		String redirUrl = (String)req.getAttribute(Constants.REDIRECT_URL);
		if (StringUtil.isEmpty(redirUrl)) {
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			StringBuilder url = new StringBuilder(200);
			url.append(page.getFullPath());
			if (!StringUtil.isEmpty(actionType)) url.append("?actionType=").append(actionType);
			redirUrl = url.toString();
		}
		sendRedirect(redirUrl, msg, req);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
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
