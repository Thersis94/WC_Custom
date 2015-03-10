package com.depuysynthes.ifu;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.FacadeActionAdapter;

public class IFUInstanceFacadAction extends FacadeActionAdapter {
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		// TODO get the selected document
		/**
		 * get the IFU instance id from the request object
		 * get all related training documents and IFU documents for that id
		 * put it on the request object
		 */
	}
	
	public void list(SMTServletRequest req) throws ActionException {
		// TODO get all the documents for all languages from the database
		/**
		 * Get all documents for all languages and put them into a document container
		 * we don't care about the technique guides at this level
		 */
	}
	
	public void delete(SMTServletRequest req) throws ActionException {
		// TODO delete the document
		/**
		 * Since approval is handled at the higher levels we don't need to worry about whether or not
		 * we are dealing with an in progress document or an approved document and can just remove 
		 * the instance from the database without worry
		 */
	}

	public void update(SMTServletRequest req) throws ActionException {
		// TODO update the current IFU document
		/**
		 * Update the document in question.
		 * In the future this will include versioning whenever
		 * the document that is being used is updated
		 **/
	}
	
	private void versionDocument(IFUDocumentVO doc) {
		/**
		 * If this function is called we want to update the version status of the
		 * document based on DePuy's versioning system.
		 */
	}
	
	private String buildUpdateSql() {
		//Build the update sql for
		return "";
	}
		  
}
