package com.depuysynthes.ifu;

import java.sql.SQLException;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
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
			super.list(req);
		}
		super.retrieve(req);
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		SMTActionInterface sai = getAction(req.getParameter(AdminConstants.FACADE_TYPE));
		if (sai != null) {
			sai.retrieve(req);
		} else {
			throw new ActionException("Invalid action passed to facade: " + req.getParameter(AdminConstants.FACADE_TYPE));
		}
	}
	
	public void delete(SMTServletRequest req) throws ActionException {
		SMTActionInterface sai = getAction(req.getParameter(AdminConstants.FACADE_TYPE));
		if (sai != null) {
			sai.delete(req);
		} else {
			throw new ActionException("Invalid action passed to facade: " + req.getParameter(AdminConstants.FACADE_TYPE));
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
		switch(actionType) {
			case ifuAction:
				return new IFUAction(actionInit, dbConn);
			case instanceAction:
				return new IFUInstanceAction(actionInit, dbConn);
			case techniqueAction:
				return new IFUTechniqueAction(actionInit, dbConn);
		}
		return null;
	}

	public void copy(SMTServletRequest req) throws ActionException {
	    	Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		
		try{
			dbConn.setAutoCommit(false);
			
			super.copy(req);

			new IFUAction(actionInit, dbConn).copy(req);
			new IFUInstanceAction(actionInit, dbConn).copy(req);
			
			dbConn.commit();
			dbConn.setAutoCommit(true);
			
		} catch(Exception e) {
			try {
				dbConn.rollback();
			} catch (SQLException sqle) {
				log.error("A Problem Occured During Rollback.", sqle);
			}
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			throw new ActionException(e);
		}
		sbUtil.moduleRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}
}
