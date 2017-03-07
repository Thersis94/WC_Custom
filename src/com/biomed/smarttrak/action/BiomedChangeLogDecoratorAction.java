/**
 *
 */
package com.biomed.smarttrak.action;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.MethodUtils;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.approval.ApprovalController;
import com.smt.sitebuilder.approval.ApprovalController.ModuleType;
import com.smt.sitebuilder.approval.ApprovalController.SyncStatus;
import com.smt.sitebuilder.approval.ApprovalException;
import com.smt.sitebuilder.approval.ApprovalVO;
import com.smt.sitebuilder.changelog.ChangeLogIntfc;
import com.smt.sitebuilder.changelog.ChangeLogUtil;
import com.smt.sitebuilder.changelog.ChangeLogVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: BiomedChangeLogDecoratorAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Decorator that ensures Build calls create Approval and
 * ChangeLog Records Correctly.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
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

	@Override
	public void build(ActionRequest req) throws ActionException {

		//Attempt to get Original Data.
		ChangeLogIntfc original = getChangeLogIntfc(req, true);

		//Call Actual Build to update the Record.
		sai.build(req);

		//Attempt to get Differential Data.
		ChangeLogIntfc diff = getChangeLogIntfc(req, false);

		//Quick fail if diff is null.  Means we don't have a ChangeLogIntfc.
		if(diff == null) {
			return;
		}

		//Default to Non-Zero Diff value.
		int dNo = -1;
		String origTxt = null, diffTxt = diff.getDiffText();

		//Neither is null, then check for equality.  No Update.
		if(original != null) {
			origTxt = original.getDiffText();
			dNo = diffTxt.compareTo(origTxt);
		}

		//If Not Equal, create a ChangeLog Record.
		if(dNo != 0) {
			log.debug("Diff Found.  Forwarding to ChangeLogUtil.");
			ApprovalVO app = getApprovalRecord(req, diff, original);
			String typeCd = (String)req.getSession().getAttribute(Constants.CHANGELOG_DIFF_TYPE_CD);
			ChangeLogVO clv = new ChangeLogVO(app.getWcSyncId(), typeCd, origTxt, diffTxt);
			new ChangeLogUtil(dbConn, attributes).saveChangeLog(clv);
		}

		/*
		 * After we finish writing ChangeLog info.  Flush out the ChangeLog data
		 * on Request.
		 */
		ChangeLogUtil.cleanupChangeLog(req);
	}

	/**
	 * Call out to ApprovalController and request an ApprovalVO.  Set additional
	 * data on it 
	 * @param req
	 * @param diff
	 * @param original
	 * @return
	 * @throws ApprovalException 
	 */
	public ApprovalVO getApprovalRecord(ActionRequest req, ChangeLogIntfc diff, ChangeLogIntfc original) throws ActionException {
		//Build an approvalVO.
		ApprovalVO app = new ApprovalVO(req, StringUtil.checkVal(MethodUtils.getPrimaryId(diff)), StringUtil.checkVal(MethodUtils.getPrimaryId(original)), ModuleType.Portlet, SyncStatus.Approved);
		app.setItemName(diff.getItemName());
		app.setItemDesc(diff.getItemDesc());

		try {

			//Save the ApprovalVO
			new ApprovalController(dbConn, attributes).createEntry(app);
		} catch(ApprovalException ae) {
			throw new ActionException("Problem Saving Approval Record.", ae);
		}
		return app;
	}

	/**
	 * Helper method that manages getting the ChangeLogIntfc for ChangeLog Processing.
	 * @param req
	 * @param isOriginal
	 * @return
	 * @throws ActionException 
	 */
	public ChangeLogIntfc getChangeLogIntfc(ActionRequest req, boolean isOriginal) throws ActionException {

		/*
		 * Look for ChangeLog Data on Session.  Inexpensive operation.
		 */
		ChangeLogIntfc cli = ChangeLogUtil.getChangeLog(req, isOriginal);

		/*
		 * If cli is null, try getting via retrieve.  More expensive.
		 */
		if(cli == null) {
			//Call Retrieve
			sai.retrieve(req);

			//Look for Data on Module Data.
			cli = ChangeLogUtil.getChangeLog(attributes);
		}

		return cli;
	}
}