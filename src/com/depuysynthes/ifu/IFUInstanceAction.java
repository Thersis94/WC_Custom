package com.depuysynthes.ifu;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: IFUDocumentToolAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: Top level action for handling the IFU documents.  This handles
 * all the most broad metadata information pertaining to the documents and leaves
 * handling the actual instances of the documents and their associated technique
 * guides to the appropriate actions.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since March 10, 2015<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class IFUInstanceAction extends SBActionAdapter {
	
	public IFUInstanceAction() {
		super();
	}
	
	public IFUInstanceAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	public IFUInstanceAction(ActionInitVO actionInit, SMTDBConnection conn) {
		super(actionInit);
		super.setDBConnection(conn);
	}

	public void retrieve(SMTServletRequest req) throws ActionException {
		// Get ifu id from the request object
		
		// Build the sql query to get the ifu from the database
		// Instances and technique guides are not needed at this level
		
		// build an IFU container from the resultset and put it on the request object
	}
	
	public void list(SMTServletRequest req) throws ActionException {
		// Build the sql query to get all the ifus from the database
		// Create a list of IFU containers from the result set and put it on the request object
	}
	
	public void delete(SMTServletRequest req) throws ActionException {
		// Check the approval status of the item we are going to delete
		// if this is approved then we need to submit this for deletion via the appropriate function
		
		// Build the sql query to delete this ifu
		
		// execute the query and return
	}

	public void update(SMTServletRequest req) throws ActionException {
		this.update(new IFUDocumentVO(req));
	}
	
	public void update(IFUDocumentVO vo) throws ActionException {
		// Check the current approval status of the record we are updating.
		// if we are trying to update an approved item we copy it and update its status
		
		// Check if we are doing an update that calls for a version change, if so call the versionDocument function
		
		// build the update sql 
		
		//get the ifu contianer from the request object
		
		// fill out the update query with the contianer
		
		// run the query
	}
	
	private void versionDocument(IFUDocumentVO doc) {
		/**
		 * If this function is called we want to update the version status of the
		 * document based on DePuy's versioning system.
		 */
	}
	
	private void updateApprovalStatus(SMTServletRequest req) throws ActionException {
		// Update the current records approval status
		// Call the method to create the neccesary approval records
	}
	
	private void updateApprovalRecords(SMTServletRequest req) throws ActionException {
		// create the needed wc_sync records 
		// update the approval status of this record
	}
	
	private String buildUpdateSql() {
		//Build the update sql for the update
		return "";
	}
	
	public void copy(SMTServletRequest req) throws ActionException {
		// get the current ifu id
		// set up the record duplicator to copy the current record as well as its children and grandchildren
	}

}
