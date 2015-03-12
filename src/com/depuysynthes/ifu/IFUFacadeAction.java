package com.depuysynthes.ifu;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.AdminConstants;

/****************************************************************************
 * <b>Title</b>: IFUInstanceFacadeAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: Works as the starting point for all IFU actions.  Requests
 * are used to create the appropriate action and are then handed off to that action.
 * </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since March 10, 2015<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class IFUFacadeAction extends SimpleActionAdapter {

	public static final String MEDIABIN_PATH = "/json?amid=MEDIA_BIN_AJAX&mbid=";
	public static final String BINARY_PATH = "/binary/org/DPY_SYN_EMEA/ifu_documents/";
	public static final String orgPath = "/org/DPY_SYN_EMEA/ifu_documents/";

	/**
	 * ActionType - supported behaviors of this facade
	 **/
	private enum ActionType {
		technique, instance, ifu, display; 
	}

	public void list(SMTServletRequest req) throws ActionException {
		SMTActionInterface sai;
		if (req.hasParameter("dataMod")) {
			sai = getAction(StringUtil.checkVal(req.getParameter(AdminConstants.FACADE_TYPE), "ifu"));
			sai.list(req);
		} else {
			//list the portlet instances in the admintool
			super.retrieve(req);
		}
	}

	public void retrieve(SMTServletRequest req) throws ActionException {
		getAction(ActionType.display).retrieve(req);
	}

	public void delete(SMTServletRequest req) throws ActionException {
		SMTActionInterface sai = getAction(req.getParameter(AdminConstants.FACADE_TYPE));
		if (sai != null) {
			sai.delete(req);
		} else {
			//delete the Portlet instance
			super.delete(req);
		}
	}

	public void update(SMTServletRequest req) throws ActionException {
		for (String key : req.getParameterMap().keySet())
			log.debug(key+"|"+req.getParameter(key));
		SMTActionInterface sai = getAction(req.getParameter(AdminConstants.FACADE_TYPE));
		if (sai != null) {
			sai.update(req);
		} else {
			//update the portlet config
			super.update(req);
		}
	}


	/**
	 * Determine which action should handle the request based on a String value
	 * @param actionType
	 * @return
	 */
	private SMTActionInterface getAction(String actionType) {
		log.debug("Here we go " + actionType);
		ActionType at = null;
		try {
			at = ActionType.valueOf(actionType);
		} catch (Exception e) {
			log.warn("Not a valid action type");
			return null;
		}
		return getAction(at);
	}


	/**
	 * Determine which action should handle the request based on an enum token
	 * @param actionType
	 * @return
	 */
	private SMTActionInterface getAction(ActionType type) {
		SMTActionInterface ai = null;
		log.debug("Loading action " + type);
		switch(type) {
			case ifu:
				ai = new IFUAction(actionInit);
				break;
			case instance:
				ai =  new IFUInstanceAction(actionInit);
				break;
			case technique:
				ai = new IFUTechniqueAction(actionInit);
				break;
			case display:
				ai = new IFUDisplayAction(actionInit);
				break;
		}

		if (ai != null) {
			ai.setAttributes(attributes);
			ai.setDBConnection(dbConn);
		}
		return ai;
	}

	//TODO this is wrong - copying the portlet should not clone all the data.
	/**
	 * 
	public void copy(SMTServletRequest req) throws ActionException {
		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);

		try {
			dbConn.setAutoCommit(false);

			super.copy(req);
			getAction(ActionType.ifu).copy(req);
			getAction(ActionType.instance).copy(req);

			dbConn.commit();

		} catch(Exception e) {
			try {
				dbConn.rollback();
			} catch (SQLException sqle) {
				log.error("A Problem Occured During Rollback.", sqle);
			}
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			throw new ActionException(e);
		} finally {
			try {
				dbConn.setAutoCommit(true);
			} catch (Exception e) {}
		}
		super.moduleRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}
	 */
}
