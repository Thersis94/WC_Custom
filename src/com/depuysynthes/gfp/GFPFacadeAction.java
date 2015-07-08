package com.depuysynthes.gfp;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.FacadeActionAdapter;

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
	public void retrieve(SMTServletRequest req) throws ActionException {
		// Check if the current user has the gfp role,
		// whether they are a site admin, and
		// if we are dealing with a dashboard request
		
		// If the user does not have a GFP role they are not supposed to see
		// any of the logged in user information and we return here.
		
		// If we are dealing with an admin level user determine whether 
		// we are working with programs or users
		// Create the approprite action and call out to the created action
		
		// Else create a program action and retrieve the pertinent program
	}
	

	public void build(SMTServletRequest req) throws ActionException {
		// Check if the current user is a site admin that has the proper
		// roles assigned to them.  If not return now
		
		// Check if the user is editing a program or a user
		
		// Create the appropriate action.
		
		// Check if we are doing an update or delete
		
		// Call chosen function for created action
	}
}
