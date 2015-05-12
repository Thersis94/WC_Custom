/**
 * 
 */
package com.ram.action.workflow;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.ram.workflow.data.WorkflowModuleVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: WorkflowModuleAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Action Manages Workflow Modules.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Apr 29, 2015
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class WorkflowModuleAction extends AbstractWorkflowAction {

	/**
	 * 
	 */
	public WorkflowModuleAction() {
		super();
	}

	public WorkflowModuleAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	@Override
	public void delete(SMTServletRequest req) throws ActionException {
		//Get the workflowModuleId off the request.
		WorkflowModuleVO wfmv = new WorkflowModuleVO(req);
		String msg = "Workflow Module deleted Successfully";

		//Check that we have the necessary Parameters on the Request.
		boolean canDelete = StringUtil.checkVal(wfmv.getWorkflowModuleId()).length() > 0;
		boolean inUse = isInUse(wfmv.getWorkflowModuleId());
		boolean success = false;
		/*
		 * Verify that both necessary parameters are given and there are no
		 * instances where the given configTypeCd is in Use.
		 */
		if(canDelete && !inUse) {
			success = deleteObject(wfmv);
		}

		if(!success && inUse) {
			msg = "Could not delete.  Workflow Module " + wfmv.getWorkflowModuleId() + " is in Use.";
		} else if(!canDelete){
			msg = "Could not delete.  Missing required param on request.";
		}

		setRedirect(req, msg);

	}

	@Override
	public void list(SMTServletRequest req) throws ActionException {

		//Get List of Workflow Module VOs
		List<WorkflowModuleVO> data = listWorkflowModules(req.getParameter("workflowModuleId"));

		//Set List of Workflow Modules on ModuleData.
		this.putModuleData(data, data.size(), true);
	}

	@Override
	public void update(SMTServletRequest req) throws ActionException {

		boolean success = true;
		String msg = null;

		//Get WorkflowModuleVO off the request.
		WorkflowModuleVO wfmv = new WorkflowModuleVO(req);
		boolean isInsert = Convert.formatBoolean(req.getParameter("isInsert"));

		//Update the Database with information contained within.
		try {
			saveObject(wfmv, isInsert);
		} catch(Exception e) {
			log.error("Problem occured while inserting/updating a WorkflowModule Record", e);
			success = false;
		}

		//Update Msg
		if(!success && isInsert) {
			msg = "There was a problem creating the WorkflowModule.";
		} else if (!success) {
			msg = "There was a problem updating the WorkflowModule.";
		}

		//Set Redirect
		setRedirect(req, msg);
	}

	/**
	 * Helper method for generating a list of WorkflowModuleVOs
	 * @return - List of WorkflowModuleVOs returned by query.
	 */
	public List<WorkflowModuleVO> listWorkflowModules(String workflowModuleId) {
		List<WorkflowModuleVO> data = new ArrayList<WorkflowModuleVO>();

		//Check if this is we have a workflowModuleId
		boolean hasModuleId = StringUtil.checkVal(workflowModuleId).length() > 0;

		//Query Db for all WorkflowModules.
		try (PreparedStatement ps = dbConn.prepareStatement(getWorkflowModuleListSql(hasModuleId))) {
			if(hasModuleId) {
				ps.setString(1, workflowModuleId);
			}
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				data.add(new WorkflowModuleVO(rs));
			}
		} catch (SQLException e) {
			log.error(e);
		}

		//Return data.
		return data;
	}

	/**
	 * Helper method for retrieving the WorkflowModule List Query.
	 * @return
	 */
	public String getWorkflowModuleListSql(boolean hasModuleId) {
		StringBuilder sql = new StringBuilder(140);
		sql.append("select * from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_WORKFLOW_MODULE ");
		if(hasModuleId) {
			sql.append("where WORKFLOW_MODULE_ID = ? ");
		}
		sql.append("order by MODULE_NM desc");

		return sql.toString();
	}

	/* (non-Javadoc)
	 * @see com.ram.action.workflow.AbstractWorkflowAction#getInUseSql()
	 */
	@Override
	protected String getInUseSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_WORKFLOW_MODULE_XR where WORKFLOW_MODULE_ID = ?");
		return sql.toString();
	}

	/* (non-Javadoc)
	 * @see com.ram.action.workflow.AbstractWorkflowAction#buildRedirectSupplement(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	protected String buildRedirectSupplement(SMTServletRequest req) {
		return "&callType=module&bType=listModules";
	}

}
