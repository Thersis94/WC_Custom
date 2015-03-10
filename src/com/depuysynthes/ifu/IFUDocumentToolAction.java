package com.depuysynthes.ifu;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;

public class IFUDocumentToolAction extends SBActionAdapter {
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		// TODO get the selected document
		/**
		 * get the IFU id from the request object
		 * get all related training documents and IFU documents for that id
		 * put it on the request object
		 */
	}
	
	public void list(SMTServletRequest req) throws ActionException {
		// TODO get all the documents for all languages from the database
		/**
		 * Get all the records from the DEPUUY_IFU table
		 */
	}
	
	public void delete(SMTServletRequest req) throws ActionException {
		// TODO delete the document
		/**
		 * Check if the current item is able to just be deleted, is in progress or is being called by the approval action,
		 * or just needs to be slated for approval.
		 * If we are going to delete the item we remove it from the database
		 * If we are going to slate it for deletion we change its status and add the appropriate approval records
		 */
	}

	public void update(SMTServletRequest req) throws ActionException {
		// TODO Update the document
		/**
		 * Check if we are dealing with an already in progress item.
		 * If we are not we duplicate the record and commit the updates
		 * Otherwise we just commit the updates.
		 */
	}

}
