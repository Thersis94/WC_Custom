package com.depuysynthes.ifu;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
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
	public static final String ORG_PATH = "/org/DPY_SYN_EMEA/ifu_documents/";
	/* 2020-05-12: DBargerhuff: This is a temp accommodation until DS EMEA is ready to fully 
	 * embrace the changes wrought in DSEMEA-67. */
	public static final String PROD_IFU_SITE_URL = "https://ifu.depuysynthes.com";

	/**
	 * ActionType - supported behaviors of this facade
	 **/
	private enum ActionType {
		technique, instance, ifu; 
	}

	public IFUFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	public IFUFacadeAction() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		if (req.hasParameter("dataMod")) {
			ActionInterface sai = getAction(StringUtil.checkVal(req.getParameter(AdminConstants.FACADE_TYPE), "ifu"));
			if (sai != null) sai.list(req);
		} else {
			//list the portlet instances in the admintool
			super.retrieve(req);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SimpleActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		ActionInterface sai = getAction(req.getParameter(AdminConstants.FACADE_TYPE));
		if (sai != null) {
			sai.delete(req);
		} else {
			//delete the Portlet instance
			super.delete(req);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SimpleActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		ActionInterface sai = getAction(req.getParameter(AdminConstants.FACADE_TYPE));
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
	private ActionInterface getAction(String actionType) {
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
	private ActionInterface getAction(ActionType type) {
		ActionInterface ai = null;
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
		}

		if (ai != null) {
			ai.setAttributes(attributes);
			ai.setDBConnection(dbConn);
		}
		return ai;
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#copy(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void copy(ActionRequest req) throws ActionException {
		ActionInterface sai = getAction(ActionType.ifu);
		if (sai != null) sai.copy(req);
	}
}
