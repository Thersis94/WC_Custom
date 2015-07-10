package com.depuysynthes.gfp;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.FacadeActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: GFPFacadeAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Handles properly directing users to the proper information
 * that they have access to and give admins access to the update and delete functions
 * <b>Copyright:</b> Copyright (c) 2015
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Eric Damschroder
 * @version 1.0
 * @since July 6, 2015
 ****************************************************************************/

public class GFPFacadeAction extends FacadeActionAdapter {
	
	public enum GFPLevel{
		PROGRAM,	WORKSHOP,	RESOURCE;
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		SMTActionInterface sai = null;
		// Determine if we are working with a user or programs
		if (req.hasParameter("editUser")) {
			sai = new GFPUserAction();
		} else {
			sai = new GFPProgramAction();
		}
		
		sai.setActionInit(actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		sai.retrieve(req);
	}
	

	public void build(SMTServletRequest req) throws ActionException {
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		// If we are not dealing with an admin level user we ignore the request.
		if (role.getRoleLevel() < 100) return;
		
		SMTActionInterface sai = null;
		// Determine if we are working with a user or programs
		if (req.hasParameter("editUser")) {
			sai = new GFPUserAction();
		} else {
			sai = new GFPProgramAction();
		}
		sai.setActionInit(actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		
		String actionType = req.getParameter("actionType");
		
		// If our supplied actionType is neither an update nor a delete
		// we can't trust that it has enough information to do anything
		// and simply don't do anything with the request.
		if ("update".equals(actionType)) {
			sai.update(req);
		} else if ("delete".equals(actionType)) {
			sai.delete(req);
		}
	}
}
