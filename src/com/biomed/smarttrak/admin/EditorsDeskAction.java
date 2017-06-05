/**
 *
 */
package com.biomed.smarttrak.admin;

import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.util.BiomedChangeLogUtil;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.approval.ApprovalVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: EditorsDeskAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Action Manages Retrieving latest Changes to various parts
 * of the system.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Feb 28, 2017
 ****************************************************************************/
public class EditorsDeskAction extends SBActionAdapter {

	public EditorsDeskAction() {
		super();
	}

	public EditorsDeskAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	public void retrieve(ActionRequest req) throws ActionException {
		List<Object> changeLogs = new BiomedChangeLogUtil(dbConn, attributes).loadRecords(null, req.getParameter("changeLogId"), req.getParameter("organizationId"), true);

		putModuleData(changeLogs);
	}

	public void build(ActionRequest req) throws ActionException {
		String wcSyncId = req.getParameter("wcSyncId");
		String profileId = ((UserDataVO)req.getSession().getAttribute(Constants.USER_DATA)).getProfileId();

		try {
			updateApprovalStatus(wcSyncId, profileId);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Problem updating Approval Record.", e);
		}
	}

	/**
	 * @param clId
	 * @param profileId
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	private void updateApprovalStatus(String wcSyncId, String profileId) throws InvalidDataException, DatabaseException {
		List<String> fields = new ArrayList<>();
		fields.add("disposition_by_id");
		fields.add("wc_sync_id");
		ApprovalVO app = new ApprovalVO();
		app.setWcSyncId(wcSyncId);
		app.setApproverId(profileId);
		new DBProcessor(dbConn).executeSqlUpdate(getApprovalUpdateSql(), app, fields);
	}

	/**
	 * @return
	 */
	private String getApprovalUpdateSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append("update WC_SYNC set disposition_by_id = ? where wc_sync_id = ?");
		return sql.toString();
	}
}