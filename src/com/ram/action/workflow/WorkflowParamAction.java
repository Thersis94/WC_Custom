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
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
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
public class WorkflowParamAction extends SBActionAdapter {

	/**
	 * 
	 */
	public WorkflowParamAction() {
	}

	public WorkflowParamAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	@Override
	public void build(SMTServletRequest req) throws ActionException {
		//Not Implemented
	}

	@Override
	public void copy(SMTServletRequest req) throws ActionException {
		//Not Implemented
	}

	@Override
	public void delete(SMTServletRequest req) throws ActionException {
		super.delete(req);
	}

	@Override
	public void list(SMTServletRequest req) throws ActionException {

		//Get List of WorkflowModule Config VOs
		List<WorkflowConfigParamVO> data = listWorkflowParams(req.getParameter("workflowModuleConfigId"));

		//Get List of Workflow Config Types
		List<WorkflowConfigTypeVO> configTypes = listWorkflowConfigTypes();
		req.setAttribute("configTypes", configTypes);

		//Set List of Workflow Modules on ModuleData.
		this.putModuleData(data, data.size(), true);
	}

	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		list(req);
	}

	@Override
	public void update(SMTServletRequest req) throws ActionException {
		super.update(req);
	}

	/**
	 * Helper 
	 * @return
	 */
	public List<WorkflowConfigTypeVO> listWorkflowConfigTypes() {
		List<WorkflowConfigTypeVO> types = new ArrayList<WorkflowConfigTypeVO>();

		StringBuilder sql = new StringBuilder(100);
		sql.append("select * from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_CONFIG_TYPE order by CONFIG_TYPE_NM desc");

		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				types.add(new WorkflowConfigTypeVO(rs));
			}
		} catch (SQLException e) {
			log.error(e);
		}
		return types;
	}

	/**
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
}
