package com.depuysynthes.ifu;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.FacadeActionAdapter;

public class IFUTechniqueFacadeAction extends FacadeActionAdapter {
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		// TODO get the selected document
		/**
		 * Get the technique guide id from the request object
		 * get the selected technique guide from the database
		 */
	}
	
	public void list(SMTServletRequest req) throws ActionException {
		// TODO Get all the technique guides for the current IFU instance
		/**
		 * Get current IFU instance id from request object
		 * get all technique guides for this id from the database
		 */
	}
	
	public void delete(SMTServletRequest req) throws ActionException {
		// TODO delete the document
		/**
		 * Get the current technique guide id from the request object
		 * and delete that document.
		 * Since approval is handled on the ifu level we don't need to figure
		 * out whether or not we are allowed to just delete the document
		 */
	}

	public void update(SMTServletRequest req) throws ActionException {
		// TODO add the document to the database
		/**
		 * add or update a technique document to the database
		 * that references this IFU document
		 */
	}
	
	private String buildUpdateSql() {
		//Build the update sql for
		return "";
	}
}
