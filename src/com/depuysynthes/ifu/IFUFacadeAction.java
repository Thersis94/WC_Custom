package com.depuysynthes.ifu;

import java.sql.SQLException;

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
	public final String techniqueAction = "technique";
	public final String instanceAction = "instance";
	public final String ifuAction = "ifu";
	public final String searchAction = "search";
	
	
	public void list(SMTServletRequest req) throws ActionException {
		SMTActionInterface sai = getAction(req.getParameter(AdminConstants.FACADE_TYPE));
		if (sai != null) {
			sai.list(req);
		} else {
			log.debug("Invalid action passed to facade: " + req.getParameter(AdminConstants.FACADE_TYPE));
			super.retrieve(req);
		}
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		IFUDisplayAction sa = new IFUDisplayAction();
		sa.setDBConnection(dbConn);
		sa.retrieve(req);
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
		SMTActionInterface sai = getAction(req.getParameter(AdminConstants.FACADE_TYPE));
		if (sai != null) {
			sai.update(req);
		} else {
			log.debug("Invalid action passed to facade: " + req.getParameter(AdminConstants.FACADE_TYPE));
			super.update(req);
		}
	}
	
	/**
	 * Determine which action should handle the request
	 * @param actionType
	 * @return
	 */
	private SMTActionInterface getAction(String actionType) {
		SMTActionInterface ai = null;
		switch(StringUtil.checkVal(actionType)) {
			case ifuAction:
				ai = new IFUAction(actionInit);
			case instanceAction:
				ai =  new IFUInstanceAction(actionInit);
			case techniqueAction:
				ai = new IFUTechniqueAction(actionInit);
		}
		
		if (ai != null) {
			ai.setAttributes(attributes);
			ai.setDBConnection(dbConn);
		}
		return ai;
	}

	public void copy(SMTServletRequest req) throws ActionException {
	    	Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		
		try {
			dbConn.setAutoCommit(false);
			
			super.copy(req);
			getAction(this.ifuAction).copy(req);
			getAction(this.instanceAction).copy(req);
			
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
}
