/**
 * 
 */
package com.ram.action.workflow;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.ram.workflow.data.WorkflowConfigTypeVO;
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
 * <b>Title</b>: WorkflowConfigTypeAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> TODO
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since May 6, 2015
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class WorkflowConfigTypeAction extends SBActionAdapter {

	/**
	 * 
	 */
	public WorkflowConfigTypeAction() {
	}

	/**
	 * @param actionInit
	 */
	public WorkflowConfigTypeAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	@Override
	public void build(SMTServletRequest req) throws ActionException {
		update(req);
	}

	@Override
	public void copy(SMTServletRequest req) throws ActionException {
		
	}

	@Override
	public void delete(SMTServletRequest req) throws ActionException {
		//Get the workflowModuleId off the request.
		String configTypeCd = req.getParameter("configTypeCd");
		String msg = "Config Type deleted Successfully";
		//Check that we have the necessary Parameters on the Request.
		boolean canDelete = StringUtil.checkVal(configTypeCd).length() > 0;
		boolean inUse = isInUse(configTypeCd);
		boolean success = false;
		/*
		 * Verify that both necessary parameters are given and there are no
		 * instances where the given configTypeCd is in Use.
		 */
		if(canDelete && !inUse) {
			success = deleteConfigType(configTypeCd);
		}

		if(!success && inUse) {
			msg = "Could not delete.  Config Type " + configTypeCd + " is in Use.";
		} else if(!canDelete){
			msg = "Could not delete.  Missing required param on request.";
		}
		
		setRedirect(req, msg);
	}

	@Override
	public void list(SMTServletRequest req) throws ActionException {

		//Get List of Workflow ConfigType VOs
		List<WorkflowConfigTypeVO> data = listConfigTypes(req.getParameter("configTypeCd"));

		//Set List of Workflow ConfigTypes on ModuleData
		this.putModuleData(data, data.size(), true);
	}

	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		list(req);
	}

	@Override
	public void update(SMTServletRequest req) throws ActionException {

		boolean success = true;
		String msg = null;

		//Get the Workflow ConfigType off the request.
		WorkflowConfigTypeVO ctvo = new WorkflowConfigTypeVO(req);

		//Update the Database with the information contained within.
		try {
			updateWorkflowConfigType(ctvo);
		} catch(Exception e) {
			log.error("Problem occurred while inserting/updating a WorkflowConfigType Record", e);
			success = false;
		}

		if(!success) {
			msg = "There was a problem creating the ConfigType.";
		}
		setRedirect(req, msg);
	}

	public boolean deleteConfigType(String configTypeCd) {
		boolean deleteSuccess = true;
		StringBuilder sql = new StringBuilder();
		sql.append("delete from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_CONFIG_TYPE where CONFIG_TYPE_CD = ?");

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, configTypeCd);
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error(e);
			deleteSuccess = false;
		}

		return deleteSuccess;
	}

	public boolean isInUse(String configTypeCd) {
		boolean inUse = false;

		//Query Db and look for anywhere that the given configTypeCd is in Use.
		try (PreparedStatement ps = dbConn.prepareStatement(getInUseSql())) {
			ps.setString(1, configTypeCd);
			ResultSet rs = ps.executeQuery();

			//If there are any records, it's in use so update inUse.
			if(rs.next()) {
				inUse = true;
			}
		} catch (SQLException e) {
			log.error(e);
		}

		//Return inUse
		return inUse;
	}

	public List<WorkflowConfigTypeVO> listConfigTypes(String configTypeCd) {
		List<WorkflowConfigTypeVO> data = new ArrayList<WorkflowConfigTypeVO>();

		//Check we have a configTypeCd
		boolean hasConfigTypeCd = StringUtil.checkVal(configTypeCd).length() > 0;

		//Query Db for ConfigTypes
		try (PreparedStatement ps = dbConn.prepareStatement(getConfigTypeListSql(hasConfigTypeCd))) {
			if(hasConfigTypeCd) {
				ps.setString(1, configTypeCd);
			}
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				data.add(new WorkflowConfigTypeVO(rs));
			}
		} catch (SQLException e) {
			log.error(e);
		}

		return data;
	}

	public void updateWorkflowConfigType(WorkflowConfigTypeVO ctvo) throws Exception {
		DBProcessor dbp = null;

		//Insert the VO via DBProcessor utility.
		try {
			dbp = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
			ctvo.setCreateDt(Convert.getCurrentTimestamp());
			dbp.insert(ctvo);
		} catch(Exception e) {
			throw e;
		}
	}

	/**
	 * @return
	 */
	private String getInUseSql() {
		StringBuilder sql = new StringBuilder(100);
		sql.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_WORKFLOW_MODULE_CONFIG where CONFIG_TYPE_CD = ?");
		return sql.toString();
	}

	/**
	 * @param hasConfigTypeCd
	 * @return
	 */
	private String getConfigTypeListSql(boolean hasConfigTypeCd) {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_CONFIG_TYPE ");

		//Append where if hasConfigTypeCd
		if(hasConfigTypeCd) {
			sql.append("where CONFIG_TYPE_CD = ? ");
		}
		sql.append("order by CONFIG_TYPE_CD");
		return sql.toString();
	}

	private void setRedirect(SMTServletRequest req, String msg) {
		//Build Redirect
		StringBuilder pg = new StringBuilder(125);
		pg.append("/").append(getAttribute(Constants.CONTEXT_NAME));
		pg.append(getAttribute(AdminConstants.ADMIN_TOOL_PATH));
		pg.append("?dataMod=true&callType=config&bType=listConfigTypes");
		pg.append("&actionId=").append(req.getParameter("actionId"));
		pg.append("&organizationId=").append(req.getParameter("organizationId"));
		if(msg != null) {
			pg.append("&msg=").append(msg);
		}

		//Set Redirect on request
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, pg.toString());
	}
}
