package com.depuysynthes.ifu;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;

public class IFUSearchAction extends SBActionAdapter {

	public void retrieve(SMTServletRequest req) throws ActionException {		
		// TODO Get documents from the database that match the current search terms
		/**
		 * determine the default language if we are not using that right now
		 * build query
		 * get results
		 * parse results into a map of IFUContainers
		 */
		
	}
	
	private String buildRetrieveSQL(SMTServletRequest req){
		/**
		 * Creates a query that will get all documents for the selected language and, 
		 * if the default language is not selected, the default language as well
		 */
		return "";
	}
}
