/**
 *
 */
package com.biomed.smarttrak.action;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.changelog.ChangeLogIntfc;
import com.smt.sitebuilder.changelog.ChangeLogUtil;

/****************************************************************************
 * <b>Title</b>: BiomedChangeLogDecoratorAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Decorator that ensures Build calls create Approval and
 * ChangeLog Records Correctly.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author raptor
 * @version 1.0
 * @since Mar 3, 2017
 ****************************************************************************/
public class BiomedChangeLogDecoratorAction extends SBActionAdapter {

	/**
	 * Member variable for the class to be decorated.  Usually the action being 
	 * called from the Module Controller
	 */
	private ActionInterface sai;
	public BiomedChangeLogDecoratorAction(ActionInterface sai) {
		super();

		// Assign the action and update this object with the DB connection and attributes
		this.sai = sai;
		this.setAttributes(sai.getAttributes());
		this.setDBConnection(sai.getDBConnection());
	}

	public void build(ActionRequest req) throws ActionException {

		//Get the Original Record off Request via ChangeLogUtil.
		 ChangeLogIntfc original = ChangeLogUtil.getChangeLog(req, true);

		//Call Actual Build to update the Record.
		sai.build(req);

		//Get the Diff Record off Request via ChangeLogUtil.
		ChangeLogIntfc diff = ChangeLogUtil.getChangeLog(req, false);

		//Quick fail if diff is null.  Means we don't have a ChangeLogIntfc.
		if(diff == null) {
			return;
		}

		//Default to Non-Zero Diff value.
		int dNo = -1;

		//Neither is null, then check for equality.  No Update.
		if(original != null) {
			dNo = original.getDiffText().compareTo(diff.getDiffText());
		}

		//If Not Equal, create a ChangeLog Record.
		if(dNo != 0) {
			log.debug("Diff Found.  Forwarding to ChangeLogUtil.");
			new ChangeLogUtil(dbConn, attributes).createChangeLog(req);
		}

		/*
		 * After we finish writing ChangeLog info.  Flush out the ChangeLog data
		 * on Request.
		 */
		ChangeLogUtil.cleanupChangeLog(req);
	}
}