/**
 *
 */
package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.biomed.smarttrak.util.BiomedChangeLogUtil;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.action.SBActionAdapter;
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
		List<Object> changeLogs = new BiomedChangeLogUtil(dbConn, attributes).loadRecords(null, null, req.getParameter("organizationId"), true);

		putModuleData(changeLogs);
	}

	public void build(ActionRequest req) throws ActionException {
		String clId = req.getParameter("changeLogId");
		String profileId = ((UserDataVO)req.getSession().getAttribute(Constants.USER_DATA)).getProfileId();

		updateApprovalStatus(clId, profileId);
	}

	/**
	 * @param clId
	 * @param profileId
	 */
	private void updateApprovalStatus(String clId, String profileId) {
		try(PreparedStatement ps = dbConn.prepareStatement(getApprovalUpdateSql())) {
			ps.setString(1, profileId);
			ps.setString(2, clId);
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Problem updating ChangeLog Approval Record.", e);
		}
	}

	/**
	 * @return
	 */
	private String getApprovalUpdateSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append("update WC_SYNC set disposition_by_id = ? where wc_sync_id in ");
		sql.append("(select wc_sync_id from change_log where change_log_id = ?)");
		return sql.toString();
	}
}