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
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.AdminConstants;
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
public class WorkflowModuleAction extends SBActionAdapter {

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
	public void build(SMTServletRequest req) throws ActionException {
		//Not Implemented
	}

	@Override
	public void copy(SMTServletRequest req) throws ActionException {
		//Not Implemented
	}

	@Override
	public void delete(SMTServletRequest req) throws ActionException {

		//Get the workflowModuleId off the request.
		String workflowModuleId = req.getParameter("workflowModuleId");

		//Check that it's not null.
		if(StringUtil.checkVal(workflowModuleId).length() > 0) {
			deleteWorkflowModule(workflowModuleId);
		}
	}

	@Override
	public void list(SMTServletRequest req) throws ActionException {

		//Get List of Workflow Module VOs
		List<WorkflowModuleVO> data = listWorkflowModules(req.getParameter("workflowModuleId"));

		//Set List of Workflow Modules on ModuleData.
		this.putModuleData(data, data.size(), true);
	}

	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		list(req);
	}

	@Override
	public void update(SMTServletRequest req) throws ActionException {

		//Get WorkflowModuleVO off the request.
		WorkflowModuleVO wfmv = new WorkflowModuleVO(req);

		//Update the Database with informationContained within.
		try {
			updateWorkflowModule(wfmv);
		} catch(Exception e) {
			log.error("Problem occured while inserting/updating a WorkflowModule Record", e);
		}

		StringBuilder pg = new StringBuilder(125);
		pg.append("/").append(attributes.get(Constants.CONTEXT_NAME)).append("/");
		pg.append(getAttribute(AdminConstants.ADMIN_TOOL_PATH));
		pg.append("?dataMod=true&callType=module&bType=listWorkflows");
		pg.append("&actionId=").append(req.getParameter("actionId"));
		pg.append("&organizationId=").append(req.getParameter("organizationId"));
		
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, pg.toString());
	}

	/**
	 * Helper method for deleting a Workflow Module.
	 * @param workflowModuleId - The Id of the Module to be deleted.
	 */
	public void deleteWorkflowModule(String workflowModuleId) {
		StringBuilder sql = new StringBuilder(100);
		sql.append("delete from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_WORKFLOW_MODULE where WORKFLOW_MODULE_ID = ?");
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, workflowModuleId);
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error(e);
		}
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
	 * Helper method for inserting/updating Workflow Module Data to the database.
	 * If we perform an insert, the given vo will have its primary key updated
	 * to reflect it's new status.
	 * @param wfmv
	 */
	public void updateWorkflowModule(WorkflowModuleVO wfmv) throws Exception {
		DBProcessor dbp = null;
		String generated = null;

		//Save the VO via DBProcessor utility.
		try {
			dbp = new DBProcessor(dbConn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
			dbp.save(wfmv);

			//Check if there is a Generated Key
			generated = dbp.getGeneratedPKId();
		} catch(Exception e) {
			throw e;
		} finally {

			//If this was an insert and we have a generated key, set it.
			if(StringUtil.checkVal(generated).length() > 0) {
				wfmv.setWorkflowModuleId(generated);
			}
		}
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

	/**
	 * Helper method that sets Retrieves data off the SMTServletRequest object
	 * and returns a WorkflowModuleVO.  There is an issue with using the vo
	 * setData(SMTServletRequest req) methods on the vo between WC Projects and
	 * the RAMDataFeed Project resulting in a LinkageError.  No other actions for
	 * ram use them so must be known but undocumented.
	 * @param req
	 */
	public WorkflowModuleVO buildWorkflowModuleVO(SMTServletRequest req) {
		WorkflowModuleVO module = new WorkflowModuleVO();
		module.setWorkflowModuleId(req.getParameter("workflowModuleId"));
		module.setModuleNm(req.getParameter("moduleNm"));
		module.setModuleDesc(req.getParameter("moduleDesc"));
		module.setClassNm(req.getParameter("classNm"));
		module.setActiveFlg(Convert.formatBoolean(req.getParameter("activeFlg")));

		return module;
	}
}
