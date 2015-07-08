package com.depuysynthes.gfp;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: GFPUserAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Handle the creation and handling of GFP roles for users
 * and handles the assignment of those users to hospitals.
 * <b>Copyright:</b> Copyright (c) 2015
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Eric Damschroder
 * @version 1.0
 * @since July 6, 2015
 ****************************************************************************/

public class GFPUserAction extends SBActionAdapter {
	
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		// Check the request object for a user's id
		// if one is provided we limit the search query to that id
		// otherwise we get all users
		
		// Select query from user table, joined with hospital and program tables
		// to get names of hospital and enrolled program
		
		// If we were given a user id we also need to get the full list of programs
		// so that all options are available for assigning programs to that user.
	}
	
	
	private void getPrograms(SMTServletRequest req) throws ActionException {
		// Create a GFPProgramAction and get a list of all programs
		// and put that on the request object for use when assigning a user
		// a program
	}
	

	public void delete(SMTServletRequest req) throws ActionException {
		// Get the user's profile id from the request object
		// and delete the record with the supplied id.
		
		// Delete query for user table
	}
	

	public void update(SMTServletRequest req) throws ActionException {
		// Get user information from request object and update the user table
		// This will only affect the custom user table, the profile table will
		// remain untouched.
		
		// Insert/Update query for user table
	}
	
}
