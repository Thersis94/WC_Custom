/**
 *
 */
package com.biomed.smarttrak.action;

import com.biomed.smarttrak.action.AdminControllerAction.Section;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
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
 * @author Billy Larsen
 * @version 1.0
 * @since Mar 3, 2017
 ****************************************************************************/
public class BiomedChangeLogDecoratorAction extends SBActionAdapter {

	/**
	 * EditPath manages ActionType, PKId Name and Public Url path for ChangeLog
	 * Elements.
	 */
	public enum EditPath {
			MARKET("marketAdmin", "marketId", Section.MARKET.getURLToken()),
			PRODUCT("productAdmin", "productId", Section.PRODUCT.getURLToken()),
			COMPANY("companyAdmin", "companyId", Section.COMPANY.getURLToken()),
			INSIGHT("insights", "insightId", Section.INSIGHT.getURLToken()),
			UPDATE("updates", "updateId", "");
		private String actionType;
		private String publicUrl;
		private String pkIdName;

		EditPath(String actionType, String pkIdName, String publicUrl){
			this.actionType = actionType;
			this.pkIdName = pkIdName;
			this.publicUrl = publicUrl;
		}

		public String getActionType() {
			return actionType;
		}

		public String getPkIdName() {
			return pkIdName;
		}

		public String getPublicUrl() {
			return publicUrl;
		}
	}
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
	public void retrieve(ActionRequest req) throws ActionException {
		sai.retrieve(req);
	}

	@Override
	public void build(ActionRequest req) throws ActionException {

		//Check if ActionType is configured for Change Logs.
		boolean useChangeLog = checkUseChangeLog(req);

		/*
		 * If Code is ChangeLoggable, go through ChangeLogAPI, else call build.
		 * Problems were arising in Data Actions (FD/Grids) where retrieve was
		 * polluting Build Return Data.
		 */
		if(useChangeLog) {
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
			String origTxt = null;
			String diffTxt = diff.getDiffText();

			//Neither is null, then check for equality.  No Update.
			if(original != null) {
				origTxt = original.getDiffText();
				dNo = diffTxt.compareTo(origTxt);
			}

			//If Not Equal, create a ChangeLog Record.
			if(dNo != 0) {
				log.debug("Diff Found.  Forwarding to ChangeLogUtil.");

				EditPath e = getEditPath(req.getParameter("actionType"));
				ApprovalVO app = buildApprovalRecord(req, diff, original, e);
				ChangeLogVO clv = new ChangeLogVO(app.getWcSyncId(), origTxt, diffTxt, e.name());
				ChangeLogUtil util = new ChangeLogUtil(dbConn, attributes);
				try {//update any old records	
					util.updateApprovalStatus(app);
				} catch (Exception ex) {
					log.error("Error attempting to update approval record: " + ex);
				}
				util.saveChangeLog(clv);
			
			}

			/*
			 * After we finish writing ChangeLog info.  Flush out the ChangeLog data
			 * on Request.
			 */
			ChangeLogUtil.cleanupChangeLog(req);
		} else {
			sai.build(req);
		}
	}

	/**
	 * Determine whether or not a change log should be used
	 * @param req
	 * @return
	 */
	private boolean checkUseChangeLog(ActionRequest req) {
		if (Convert.formatBoolean(req.getParameter("bypassChangelog")))
			return false;
		
		for(EditPath e : EditPath.values()) {
			if(e.getActionType().equals(req.getParameter("actionType"))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param parameter
	 * @return
	 */
	protected EditPath getEditPath(String actionType) {
		for(EditPath e : EditPath.values()) {
			if(e.getActionType().equals(actionType)) {
				return e;
			}
		}

		return null;
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
	public ApprovalVO buildApprovalRecord(ActionRequest req, ChangeLogIntfc diff, ChangeLogIntfc original, EditPath e) throws ActionException {
		//Build an approvalVO.
		ApprovalVO app = new ApprovalVO(req,
										StringUtil.checkVal(MethodUtils.getPrimaryId(diff)),
										StringUtil.checkVal(MethodUtils.getPrimaryId(original)),
										ModuleType.Portlet,
										SyncStatus.Approved);
		app.setItemName(diff.getItemName());
		app.setItemDesc(diff.getItemDesc());
		app.setRequestTypeId(e.name());

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
		ChangeLogIntfc cli = ChangeLogUtil.getChangeLogSessData(req, isOriginal);

		/*
		 * If cli is null, try getting via retrieve.  More expensive.
		 */
		if(cli == null) {
			//Call Retrieve
			sai.retrieve(req);

			//Look for Data on Module Data.
			cli = ChangeLogUtil.getChangeLog((ModuleVO)getAttribute(Constants.MODULE_DATA));
		}

		return cli;
	}
}