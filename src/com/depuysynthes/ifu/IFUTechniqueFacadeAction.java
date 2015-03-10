package com.depuysynthes.ifu;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.FacadeActionAdapter;

/****************************************************************************
 * <b>Title</b>: IFUTechniqueFacadeAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: Handles information and metadata specific to the technique
 * guides of a particular instance of an IFU.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since March 10, 2015<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class IFUTechniqueFacadeAction extends FacadeActionAdapter {
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		// Get document id from the request object
		
		// Build the query to get the document and all its technique guides from the database
		
		// Build a documentvo and associated technique guide vos from the result set
		
		//place the document vo on the request object
	}
	
	public void list(SMTServletRequest req) throws ActionException {
		// Get current IFU id
		
		// Build the search query to get all instances of this IFU document
		// Ignore all technique guides at this step
		
		// Build a list of document vos from the result set.
		
		//place the list on the request object.
	}
	
	public void delete(SMTServletRequest req) throws ActionException {
		// Get the current instance id from the request object
		
		// Build the delete query.  We dont need to bother with approval since it 
		// is handled at a higher level.
		
		// execute the delete and return
	}

	public void update(SMTServletRequest req) throws ActionException {
		// Build a document vo from the request object
		
		// build the update sql.  Approval is habdled higher up so we 
		// don't need to check for that
		
		// Set the values from the vo
		
		// execute and return.
	}
	
	private String buildUpdateSql() {
		// Find out whether we are updating or inserting
		// Build query based on previous response
		return "";
	}
}
