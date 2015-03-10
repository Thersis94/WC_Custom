package com.depuysynthes.ifu;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: IFUSearchAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: Searches the database for all items pertaining to the given 
 * search parameters and creates a list of IFU documents from those results.
 * If the language being searched does not have a complete list of IFUs then 
 * any missing documents will be loaded from the default langiage/</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since March 10, 2015<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class IFUSearchAction extends SBActionAdapter {

	public void retrieve(SMTServletRequest req) throws ActionException {		
		// Get the default language
		// build the retrieve sql 
		// Execute the retrieve sql with any search parameter gotten from the request object
		
		// Build a list of container vos, before adding a document we will check if another document of that
		// name is already in the list and skip it if already there
		
		// put the map on the request object
	}
	
	private String buildRetrieveSQL(SMTServletRequest req){
		/**
		 * Creates a query that will get all documents for the selected language and, 
		 * if the default language is not selected, the default language as well
		 */
		return "";
	}
}
