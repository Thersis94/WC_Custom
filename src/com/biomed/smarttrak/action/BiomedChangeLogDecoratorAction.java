/**
 *
 */
package com.biomed.smarttrak.action;

import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.util.BiomedChangeLogUtil;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.changelog.ChangeLogIntfc;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

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

		//Call Retrieve on the Record.
		sai.retrieve(req);

		//Get the Original Record.
		ChangeLogIntfc original = getChangeLogItem(((ModuleVO)attributes.get(Constants.MODULE_DATA)).getActionData());

		//Call Actual Build to update the Record.
		sai.build(req);

		//Call Retrieve second time to get the new Record.
		sai.retrieve(req);

		//Get the Diff Record.
		ChangeLogIntfc diff = getChangeLogItem(((ModuleVO)attributes.get(Constants.MODULE_DATA)).getActionData());

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
			new BiomedChangeLogUtil(dbConn, attributes).createChangeLog(req, original, diff);
		}
	}

	/**
	 * Helper method that transposes various possible actionData types to the
	 * proper ChangeLogIntfc type.
	 * @param actionData
	 * @return
	 */
	private ChangeLogIntfc getChangeLogItem(Object actionData) {
		ChangeLogIntfc cli = null;
		Object ele = actionData;
		if(actionData instanceof List && !((List<?>) actionData).isEmpty()) {
			ele = ((List<?>)actionData).get(0);
		} else if(actionData instanceof Map && !((Map<?, ?>) actionData).isEmpty()) {
			ele = ((Map<?, ?>)actionData).values().iterator().next();
		}

		//Assume we have a singular object now and attempt to cast to ChangeLogIntfc.
		if(ele instanceof ChangeLogIntfc) {
			cli = (ChangeLogIntfc) ele;
		}

		//retrun cli.
		return cli;
	}
}