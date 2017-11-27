package com.mindbody.action;

import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

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
public class MindBodyFacadeAction extends SimpleActionAdapter {
	public static final String ACTION_TYPE = "actionType"; //reqParam this class executes around
	protected static final Map<String, Class<? extends ActionInterface>> ACTIONS;

	/**
	 * populates the action map when the static constructor is called.  This will make our map live once in the JVM
	 */
	static {
		ACTIONS = new HashMap<>(35);
		ACTIONS.put("class", MindBodyClassAction.class);
		ACTIONS.put("client", MindBodyClientAction.class);
		ACTIONS.put("sale", MindBodySaleAction.class);
		ACTIONS.put("schedule", MindBodyScheduleAction.class);
	}
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

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		loadAction(req.getParameter(ACTION_TYPE)).retrieve(req);

	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		String actionType = req.getParameter(ACTION_TYPE);
		String msg;
		try {
			ActionInterface action = loadAction(actionType);

			//allow either deletes or saves (build) to be called directly from the controller
			if (AdminConstants.REQ_DELETE.equals(req.getParameter("actionPerform"))) {
				action.delete(req);
			} else if(AdminConstants.REQ_COPY.equals(req.getParameter("actionPerform"))){
				action.copy(req);
			}else {
				action.build(req);
			}
			msg = (String) getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);

		} catch (ActionException ae) {
			log.error("could not execute " + actionType, ae.getCause());
			msg = (String) getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		}

		// Only proceed to redirect if it not a json request (?json=true)
		if (Convert.formatBoolean(req.getParameter("json")))
			return;

		//setup the redirect.  Build a URL for 'this' page if a child action didn't build one of it's own.
		//NOTE: the controller should (and does) control the redirect.  It also sets 'msg' properly if the child action pukes.
		String redirUrl = (String)req.getAttribute(Constants.REDIRECT_URL);
		if (StringUtil.isEmpty(redirUrl)) {
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			redirUrl = page.getFullPath();
		}
		sendRedirect(redirUrl, msg, req);
	}

	/**
	 * Based on passed cPage, instantiate the appropriate class and return.
	 * @param cPage
	 * @return
	 * @throws ActionException
	 */
	protected ActionInterface loadAction(String actionType) throws ActionException {
		Class<?> c = ACTIONS.get(actionType);
		if (c == null) 
			throw new ActionException("unknown action type:" + actionType);

		//instantiate the action & return it - pass attributes & dbConn
		try {
			ActionInterface action = (ActionInterface) c.newInstance();
			action.setActionInit(actionInit);
			action.setDBConnection(dbConn);
			action.setAttributes(getAttributes());
			action.setActionInit(actionInit);
			return action;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ActionException("Problem Instantiating type: " + actionType);
		}
	}
}