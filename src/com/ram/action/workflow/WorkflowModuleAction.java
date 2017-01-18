/**
 * 
 */
package com.ram.action.workflow;

//JDK 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

//RAMDataFeed


//SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

import com.siliconmtn.workflow.data.WorkflowConfigParamVO;
import com.siliconmtn.workflow.data.WorkflowModuleVO;
//WebCrescendo 2.0
import com.smt.sitebuilder.common.ModuleVO;
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
	public void delete(ActionRequest req) throws ActionException {
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
	public void list(ActionRequest req) throws ActionException {

		//Get List of Workflow Module VOs
		List<WorkflowModuleVO> data = listWorkflowModules(req.getParameter("workflowModuleId"));

		//Set List of Workflow Modules on ModuleData.
		this.putModuleData(data, data.size(), true);
	}

	@Override
	public void update(ActionRequest req) throws ActionException {

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
	protected String buildRedirectSupplement(ActionRequest req) {
		return "&callType=module&bType=listModules";
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("WorkflowModuleAction retrieve...");
		List<WorkflowModuleVO> data = null;

		/*
		 * Check if this is a DropDown request or not.  DropDown returns all
		 * workflow modules.
		 */
		if(req.hasParameter("isDropDown")) {
			data = getUnfilteredWorkflowModules();
		}
		ModuleVO modVo = (ModuleVO) attributes.get(Constants.MODULE_DATA);

		//Only go after record count if we are doing a list.
        modVo.setDataSize(getRecordCount());
        modVo.setActionData(data);
        this.setAttribute(Constants.MODULE_DATA, modVo);
	}

	/**
	 * Helper method that returns the complete list of RAM_WORKFLOW_MODULES for a
	 * dropdown menu sorted by module_nm.
	 * @return
	 */
	private List<WorkflowModuleVO> getUnfilteredWorkflowModules() {
		List<WorkflowModuleVO> data = new ArrayList<>();
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);

		//Build SQL Query
		StringBuilder sql = new StringBuilder();
		sql.append("select wm.WORKFLOW_MODULE_ID, wm.MODULE_NM, wm.MODULE_DESC, wm.ACTIVE_FLG, ");
		sql.append("ct.CONFIG_TYPE_CD, ct.CONFIG_TYPE_NM, ct.CONFIG_TYPE_DESC, ");
		sql.append("wmc.WORKFLOW_MODULE_CONFIG_ID, wmc.IS_REQUIRED_FLG, wmc.IS_MODULE_FLG ");
		sql.append("from ").append(schema);
		sql.append("RAM_WORKFLOW_MODULE wm left join ").append(schema);
		sql.append("RAM_WORKFLOW_MODULE_CONFIG wmc on wm.WORKFLOW_MODULE_ID = wmc.WORKFLOW_MODULE_ID ");
		sql.append("left join ").append(schema);
		sql.append("RAM_CONFIG_TYPE ct on wmc.CONFIG_TYPE_CD = ct.CONFIG_TYPE_CD ");
		sql.append("order by MODULE_NM ");
		
		log.debug("WorkflowModuleAction getUnfilteredWorkflowModules SQL: " + sql.toString());

		//Query for Results and build WorkflowModuleVO Object.
		String currentModuleId = "";
		WorkflowModuleVO wm = null;
		WorkflowConfigParamVO wcp = null;
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				//Add the WorkflowModuleVO for the row to the Workflow Module List.
				if (!rs.getString("WORKFLOW_MODULE_ID").equals(currentModuleId)) {
					if (wm != null) data.add(wm);
					currentModuleId = rs.getString("WORKFLOW_MODULE_ID");
					wm = new WorkflowModuleVO(rs);
					wcp = new WorkflowConfigParamVO(rs);
					wm.addModuleConfig(wcp);
				} else {
					wcp = new WorkflowConfigParamVO(rs);
					wm.addModuleConfig(wcp);
				}
			}
			
			//Add the last remaining WorkflowModuleVO
			data.add(wm);
		} catch (SQLException e) {
			log.error(e);
		}

		//Return WorkflowVO Data.
		return data;
	}
	
	/**
	 * Gets the count of the records
	 * @return
	 * @throws SQLException
	 */
	protected int getRecordCount() {

		StringBuilder sql = new StringBuilder(80);
		sql.append("select count(WORKFLOW_ID) from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("RAM_WORKFLOW");

		int cnt = 0;
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString());) {

		//Get the count off the first row.
		ResultSet rs = ps.executeQuery();
		if(rs.next())
			cnt = rs.getInt(1);
		} catch(SQLException sqle) {
			log.error("Error retrieving workflow Count", sqle);
		}

		log.debug("Count: " + cnt);
		return cnt;		
	}

	/**
	 * Helper method that manages inserting and updating a Workflow Module XR
	 * Record in the Database.
	 * @param vo
	 * @throws SQLException
	 */
	public void modifyModuleXr(WorkflowModuleVO vo) throws SQLException {
		StringBuilder sql = new StringBuilder(200);
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		boolean isInsert = false;

		//Build appropriate SQL Statement.
		if (vo.getWorkflowModuleXRId().startsWith("ext")) {
			// is an insert
			sql.append("insert into ").append(schema);
			sql.append("RAM_WORKFLOW_MODULE_XR ");
			sql.append("(WORKFLOW_ID, WORKFLOW_MODULE_ID, MODULE_ORDER_NO, ");
			sql.append("CONTINUE_ON_ERROR_FLG, CREATE_DT, WORKFLOW_MODULE_XR_ID) ");
			sql.append("values (?,?,?,?,?,?)");

			isInsert = true;
			vo.setWorkflowModuleXRId(new UUIDGenerator().getUUID());
		} else {
			// is an update
			sql.append("update ").append(schema);
			sql.append("RAM_WORKFLOW_MODULE_XR ");
			sql.append("set WORKFLOW_ID = ?, WORKFLOW_MODULE_ID = ?, ");
			sql.append("MODULE_ORDER_NO = ?, CONTINUE_ON_ERROR_FLG = ? ");
			sql.append("WHERE WORKFLOW_MODULE_XR_ID = ?");
		}

		log.debug(sql + "|" + vo.isContinueOnError());
		
		//Exequte Query
		int i = 1;
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			//set insert/update params.
			ps.setString(i++, vo.getWorkflowId());
			ps.setString(i++, vo.getWorkflowModuleId());
			ps.setInt(i++, vo.getOrderNo());
			ps.setBoolean(i++, vo.isContinueOnError());
			if (isInsert) ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, vo.getWorkflowModuleXRId());

			ps.executeUpdate();
		} catch(Exception e) {
			log.error("Problem occured while inserting/updating a Workflow Module XR Record", e);
		}
	}
}
