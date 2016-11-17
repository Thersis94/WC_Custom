/**
 * 
 */
package com.ram.action.workflow;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.workflow.data.WorkflowConfigTypeVO;
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
public class WorkflowConfigTypeAction extends AbstractWorkflowAction {

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
	public void delete(SMTServletRequest req) throws ActionException {
		//Get the configTypeCd off the request.
		WorkflowConfigTypeVO ctvo = new WorkflowConfigTypeVO(req);
		String msg = "Config Type deleted Successfully";

		//Check that we have the necessary Parameters on the Request.
		boolean canDelete = StringUtil.checkVal(ctvo.getConfigTypeCd()).length() > 0;
		boolean inUse = isInUse(ctvo.getConfigTypeCd());
		boolean success = false;

		/*
		 * Verify that both necessary parameters are given and there are no
		 * instances where the given configTypeCd is in Use.
		 */
		if(canDelete && !inUse) {
			success = deleteObject(ctvo);
		}

		//Updated Success Message if necessary.
		if(!success && inUse) {
			msg = "Could not delete.  Config Type " + ctvo.getConfigTypeCd() + " is in Use.";
		} else if(!canDelete){
			msg = "Could not delete.  Missing required param on request.";
		}

		//Send Redirect
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
	public void update(SMTServletRequest req) throws ActionException {

		boolean success = true;
		String msg = null;

		//Get the Workflow ConfigType off the request.
		WorkflowConfigTypeVO ctvo = new WorkflowConfigTypeVO(req);

		//Update the Database with the information contained within.
		try {
			saveObject(ctvo, true);
		} catch(Exception e) {
			log.error("Problem occurred while inserting/updating a WorkflowConfigType Record", e);
			success = false;
		}

		if(!success) {
			msg = "There was a problem creating the ConfigType.";
		}
		setRedirect(req, msg);
	}

	public List<WorkflowConfigTypeVO> listConfigTypes(String configTypeCd) {
		List<WorkflowConfigTypeVO> data = new ArrayList<WorkflowConfigTypeVO>();

		//Check we have a configTypeCd
		boolean hasConfigTypeCd = StringUtil.checkVal(configTypeCd).length() > 0;

		//Query Db for ConfigTypes
		try (PreparedStatement ps = dbConn.prepareStatement(getConfigTypeListSql(hasConfigTypeCd))) {
			log.debug(getConfigTypeListSql(hasConfigTypeCd));
			if(hasConfigTypeCd) {
				ps.setString(1, configTypeCd);
			}
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				WorkflowConfigTypeVO ctvo = new WorkflowConfigTypeVO(rs);
				data.add(ctvo);
			}
		} catch (SQLException e) {
			log.error(e);
		}

		return data;
	}

	/**
	 * @return
	 */
	protected String getInUseSql() {
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

	/* (non-Javadoc)
	 * @see com.ram.action.workflow.AbstractWorkflowAction#buildRedirectSupplement(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	protected String buildRedirectSupplement(SMTServletRequest req) {
		return "&callType=config&bType=listConfigTypes";
	}

}
