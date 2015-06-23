/**
 * 
 */
package com.ram.action.workflow;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.ram.workflow.data.WorkflowConfigParamVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: WorkflowParamAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Action manages RAM Workflow Params.
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
public class WorkflowParamAction extends AbstractWorkflowAction {

	/**
	 * 
	 */
	public WorkflowParamAction() {
	}

	public WorkflowParamAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	@Override
	public void delete(SMTServletRequest req) throws ActionException {
		//Get the configTypeCd off the request.
		WorkflowConfigParamVO wcp = new WorkflowConfigParamVO(req);
		String msg = "Workflow Module Param deleted Successfully";

		//Check that we have the necessary Parameters on the Request.
		boolean canDelete = StringUtil.checkVal(wcp.getWorkflowModuleConfigId()).length() > 0;
		boolean inUse = isInUse(wcp.getWorkflowModuleConfigId());
		boolean success = false;

		/*
		 * Verify that both necessary parameters are given and there are no
		 * instances where the given workflowModuleConfigId is in Use.
		 */
		if(canDelete && !inUse) {
			success = deleteObject(wcp);
		}

		//Updated Success Message if necessary.
		if(!success && inUse) {
			msg = "Could not delete.  Module Param " + wcp.getConfigTypeNm() + " is in Use.";
		} else if(!canDelete){
			msg = "Could not delete.  Missing required param on request.";
		}

		//Send Redirect
		setRedirect(req, msg);
	}

	@Override
	public void list(SMTServletRequest req) throws ActionException {

		//Get List of WorkflowModule Config VOs
		List<WorkflowConfigParamVO> data = listWorkflowParams(req.getParameter("workflowModuleId"));

		//Get the WorkflowModuleConfigId off the request.
		String confId = req.getParameter("workflowModuleConfigId");

		/*
		 * If the WorkflowModuleConfigId is present, set the matching VO on the
		 * req.
		 */
		if(StringUtil.checkVal(confId).length() > 0) {
			for(WorkflowConfigParamVO cp : data) {
				log.debug(cp.getWorkflowModuleConfigId());
				if(confId.equals(cp.getWorkflowModuleConfigId())) {
					req.setAttribute("sel", cp);
					break;
				}
			}
		}
		//Set List of Workflow Modules on ModuleData.
		this.putModuleData(data, data.size(), true);
	}

	@Override
	public void update(SMTServletRequest req) throws ActionException {

		boolean success = true;
		String msg = null;

		//Get the Workflow ConfigType off the request.
		WorkflowConfigParamVO wcp = new WorkflowConfigParamVO(req);
		boolean isInsert = Convert.formatBoolean(req.getParameter("isInsert"));

		//Update the Database with the information contained within.
		try {
			saveObject(wcp, isInsert);
		} catch(Exception e) {
			log.error("Problem occurred while inserting/updating a WorkflowConfigParam Record", e);
			success = false;
		}

		if(!success) {
			msg = "There was a problem creating the Workflow Config Param.";
		}
		setRedirect(req, msg);
	}

	/**
	 * Helper method that loads up all the Workflow Config Params for a given
	 * workflowModule.  Can optionally pass a workflowModuleConfigId to return
	 * specifics for a single config param.
	 * @param parameter
	 * @return
	 */
	public List<WorkflowConfigParamVO> listWorkflowParams(String workflowModuleId) {
		List<WorkflowConfigParamVO> params = new ArrayList<WorkflowConfigParamVO>();

		try(PreparedStatement ps = dbConn.prepareStatement(getConfigParamSql())) {
			log.debug("Query: " + getConfigParamSql());
			ps.setString(1, workflowModuleId);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				params.add(new WorkflowConfigParamVO(rs));
			}
		} catch (SQLException e) {
			log.error(e);
		}
		return params;
	}

	/**
	 * Helper method that returns the sql query needed to retrieve all the config
	 * Params.
	 * @param hasConfigId
	 * @return
	 */
	private String getConfigParamSql() {
		String alias = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(260);

		sql.append("select * from ").append(alias);
		sql.append("RAM_CONFIG_TYPE a left outer join ");
		sql.append(alias).append("RAM_WORKFLOW_MODULE_CONFIG b ");
		sql.append("on a.CONFIG_TYPE_CD = b.CONFIG_TYPE_CD ");
		sql.append("and b.WORKFLOW_MODULE_ID = ? ");
		sql.append("order by a.CONFIG_TYPE_NM");

		return sql.toString();
	}

	/* (non-Javadoc)
	 * @see com.ram.action.workflow.AbstractWorkflowAction#buildRedirectSupplement(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	protected String buildRedirectSupplement(SMTServletRequest req) {
		StringBuilder sb = new StringBuilder(95);
		sb.append("&callType=param&bType=listModuleParams&workflowModuleId=");
		sb.append(req.getParameter("workflowModuleId"));
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see com.ram.action.workflow.AbstractWorkflowAction#getInUseSql()
	 */
	@Override
	protected String getInUseSql() {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(125);
		sql.append("select * from ").append(schema).append("RAM_WORKFLOW_MODULE_CONFIG_XR ");
		sql.append("where WORKFLOW_MODULE_CONFIG_ID = ?");
		return sql.toString();
	}
}
