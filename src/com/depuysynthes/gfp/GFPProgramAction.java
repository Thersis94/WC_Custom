package com.depuysynthes.gfp;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: GFPProgramAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Action for use in handling the display, management, and
 * assignment of GFP programs
 * <b>Copyright:</b> Copyright (c) 2015
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Eric Damschroder
 * @version 1.0
 * @since July 6, 2015
 ****************************************************************************/

public class GFPProgramAction extends SBActionAdapter {

	public void retrieve(SMTServletRequest req) throws ActionException {
		// Check the request object for either a user id or a program id.
		// If we have either of them we only need to get one program
		// In the situation of a user id we get the program related to them
		// In the situation of a program id we need only get that program
		// If neither id is present we get all available programs
	}
	
	
	private void getProgram (String id, boolean isUser) {
		// Get the supplied program
		
		// Select query for program table joined on workshop, resource, and category table
		// with a where clause specifying the program we want.
		// If isUser is true a join on the user table will added in and the 
		// where clause will deal with the user id instead of the program id
	}
	
	
	private void getAllProgram() {
		// Get all programs from the database
		
		// Select query for the program table.
		// Workshop and resource tables are not needed here since we are 
		// only dealing with the top level in this situation.
	}
	

	public void delete(SMTServletRequest req) throws ActionException {
		// Only called from the dashboard
		// Determine the level of the delete (resource, workshop, program)
		// and remove the associated item.
	}

	
	public void update(SMTServletRequest req) throws ActionException {
		// Only called from the dashboard
		// Determine the level of the update (resource, workshop, program)
		// and update the associated item.
	}
	
	
	private void updateProgram (SMTServletRequest req) {
		// Update/Insert query for the program table
	}
	
	
	private void updateWorkshop (SMTServletRequest req) throws ActionException {
		// Update/Insert query for the workshop table
		
	}
	
	
	private void updateResource (SMTServletRequest req) throws ActionException {
		// Determine if we are adding the resource to a program or to a workshop
		
		// Update/Insert query for the resource table
		
		// If it was an insert create an insert query for the
		// appropriate table (workshop|program)
	}
	
}
