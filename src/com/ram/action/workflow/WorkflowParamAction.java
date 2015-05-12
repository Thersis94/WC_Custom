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
import com.ram.workflow.data.WorkflowConfigTypeVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
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
		List<WorkflowConfigParamVO> data = listWorkflowParams(req.getParameter("workflowModuleConfigId"));

		//Get List of Workflow Config Types if this is an edit.
		if(req.hasParameter("workflowModuleConfigId")) {
			List<WorkflowConfigTypeVO> configTypes = listWorkflowConfigTypes(req);
			req.setAttribute("configTypes", configTypes);
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
	 * Helper that loads up the WorkflowConfigTypeAction for retrieving all
	 * workflow config types.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	public List<WorkflowConfigTypeVO> listWorkflowConfigTypes(SMTServletRequest req) throws ActionException {
		WorkflowConfigTypeAction wcta = new WorkflowConfigTypeAction(this.actionInit);
		wcta.setAttributes(attributes);
		wcta.setDBConnection(dbConn);
		wcta.list(req);
		ModuleVO mod = (ModuleVO)req.getAttribute(AdminConstants.ADMIN_MODULE_DATA);
		return (List<WorkflowConfigTypeVO>)mod.getActionData();
	}

	/**
	 * Helper method that loads up all the Workflow Config Params for a given
	 * workflowModule.  Can optionally pass a workflowModuleConfigId to return
	 * specifics for a single config param.
	 * @param parameter
	 * @return
	 */
	public List<WorkflowConfigParamVO> listWorkflowParams(String workflowModuleConfigId) {
		List<WorkflowConfigParamVO> params = new ArrayList<WorkflowConfigParamVO>();
		boolean hasConfigId = StringUtil.checkVal(workflowModuleConfigId).length() > 0;

		try(PreparedStatement ps = dbConn.prepareStatement(getConfigParamSql(hasConfigId))) {
			if(hasConfigId) {
				ps.setString(1, workflowModuleConfigId);
			}
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
	private String getConfigParamSql(boolean hasConfigId) {
		String alias = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(260);

		sql.append("select * from ").append(alias);
		sql.append("RAM_WORKFLOW_MODULE_CONFIG a inner join ");
		sql.append(alias).append("RAM_CONFIG_TYPE b ");
		sql.append("on a.CONFIG_TYPE_CD = b.CONFIG_TYPE_CD ");
		if(hasConfigId) {
			sql.append("where a.WORKFLOW_MODULE_CONFIG_ID = ? ");
		}
		sql.append("order by b.CONFIG_TYPE_NM desc");

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
