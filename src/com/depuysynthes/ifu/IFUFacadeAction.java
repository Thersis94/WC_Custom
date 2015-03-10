package com.depuysynthes.ifu;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.FacadeActionAdapter;

/****************************************************************************
 * <b>Title</b>: IFUInstanceFacadeAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: Works as the starting point for all IFU actions.  Requests
 * are used to create the approprite action and are then handed off to that action.
 * </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since March 10, 2015<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class IFUFacadeAction extends FacadeActionAdapter {
	
	public void list(SMTServletRequest req) throws ActionException {
		// Get the actual action we are calling from the request object
		
		// Build an instance of that action and pass along the request object.
	}

	
	public void retrieve(SMTServletRequest req) throws ActionException {
		// Get the actual action we are calling from the request object
		
		// Build an instance of that action and pass along the request object.
	}
	
	public void delete(SMTServletRequest req) throws ActionException {
		// Get the actual action we are calling from the request object
		
		// Build an instance of that action and pass along the request object.
	}

	public void copy(SMTServletRequest req) throws ActionException {
		// Get the id for the current IFU record.
		
		// Create a record duplicator for this, being sure to include all instances and their technique guides
		
		// Run the duplicator.
	}
}
