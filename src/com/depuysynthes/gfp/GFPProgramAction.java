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
		
		// Check if we are dealing with dashboard request
		
		// if so and we have a program id we get the one program we need
		// otherwise get all programs
		
		// if we are not dealing with a dashboard request check for a user id
		
		// if we get one get the program associated with that user.
		
	}
	
	
	/**
	 * Get the program associate with the supplied id
	 * @param id the id of the program we are looking for
	 * @param isUser determines the starting point of the search
	 */
	private void getProgram (String id, boolean isUser) {
		// Get the supplied program
		
		// Select query for program table joined on workshop, resource, and category table
		// with a where clause specifying the program we want.
		// If isUser is true a join on the user table will added in and the 
		// where clause will deal with the user id instead of the program id
	}
	
	
	/**
	 * Gets all the programs in the database
	 */
	private void getAllPrograms() {
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
		// and call the associated update method
	}
	
	
	/**
	 * Build and run an update/insert query for a program
	 * @param req
	 */
	private void updateProgram (SMTServletRequest req) {
		// Update/Insert query for the program table
	}
	
	
	/**
	 * Build and run an update/insert query for a workshop
	 * @param req
	 * @throws ActionException
	 */
	private void updateWorkshop (SMTServletRequest req) throws ActionException {
		// Update/Insert query for the workshop table
		
	}
	

	/**
	 * Build and run an update/insert query for a resource as well as 
	 * create the xr table record if an insert is being run
	 * @param req
	 * @throws ActionException
	 */
	private void updateResource (SMTServletRequest req) throws ActionException {
		// Determine if we are adding the resource to a program or to a workshop
		
		// Update/Insert query for the resource table
		
		// If it was an insert create an insert query for the
		// appropriate table (workshop|program)
	}
	
}
