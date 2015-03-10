package com.depuysynthes.ifu;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.FacadeActionAdapter;

public class IFUInstanceFacadAction extends FacadeActionAdapter {
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		// Get the IFU instance id from the request object
		
		// build the retrieve query to get the current ifu instance from
		// the database as well as all technique guides that belong to it
		
		// Run the query and build the document with a list of technique guides.
		
		// put the document on the request object.
	}
	
	public void list(SMTServletRequest req) throws ActionException {
		// Get the IFU instance id from the request object
		
		// build the retrieve query to get the current ifu container as well
		// as all of its instances
		
		// Run the query and build the container with a map of instances.
		
		// put the container on the request object.
	}
	
	public void delete(SMTServletRequest req) throws ActionException {
		// Get the current instance's id
		
		// Build the delete query.  Approval is handled higher up so we don't need to do any extra checks here
		
		// Execute the query and return
	}

	public void update(SMTServletRequest req) throws ActionException {
		// build the documentvo from the request object
		
		// build the update query with the build query function
		
		// Set the values on the query with information on the vo
		
		//Execute the query and return
	}
	
	private String buildUpdateSql() {
		//Build the update sql for
		return "";
	}
		  
}
