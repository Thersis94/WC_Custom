package com.depuysynthes.ifu;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;

public class TechniqueGuideVO extends SBActionAdapter {

	
	public void retrieve(SMTServletRequest req) throws ActionException {
		/**
		 * Get all the ifu documents for the selected approval type
		 */
	}
	
	public void delete(SMTServletRequest req) throws ActionException {
		/**
		 * Complete the delete started in the IFUDocument tool
		 */
	}
	
	public void update(SMTServletRequest req) throws ActionException {
		/**
		 * Complete the update started in the IFUDocument tool
		 */
	}
	
	public void build(SMTServletRequest req) throws ActionException {
		/**
		 * Handle approval rejection
		 */
	}

}
