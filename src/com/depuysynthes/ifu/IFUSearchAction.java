package com.depuysynthes.ifu;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;

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
