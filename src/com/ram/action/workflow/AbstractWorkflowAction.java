/**
 * 
 */
package com.ram.action.workflow;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.workflow.data.BaseDBReqVO;
import com.smt.sitebuilder.action.AbstractBaseAction;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: AbstractWorkflowAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Action manages similarities between the Workflow Actions.
 * Utilizes the DBProcessor for managing db interactions and controls building of
 * the redirects.
 * <b>Copyright:</b> Copyright (c) 2015
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author raptor
 * @version 1.0
 * @since May 8, 2015
 *        <b>Changes: </b>
 *        TODO - Workflow Has been migrated to the core.  Determine if ram should
 *        maintain the public facing workflow Management or use admin side. 
 ****************************************************************************/
public abstract class AbstractWorkflowAction extends AbstractBaseAction {

	/**
	 * 
	 */
	public AbstractWorkflowAction() {
	}

	/**
	 * 
	 */
	public AbstractWorkflowAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		//Not Implemented
	}

	@Override
	public void copy(ActionRequest req) throws ActionException {
		//Not Implemented
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		//Not Implemented
	}

	protected boolean deleteObject(BaseDBReqVO o) {
		DBProcessor db = new DBProcessor(dbConn, (String) attributes.get(Constants.CUSTOM_DB_SCHEMA));
		try {
			return Convert.formatBoolean(db.delete(o));
		} catch (InvalidDataException e) {
			log.error(e);
		} catch (DatabaseException e) {
			log.error(e);
		}
		return false;
	}

	/**
	 * Helper method for inserting/updating Workflow Module Data to the database.
	 * If we perform an insert, the given vo will have its primary key updated
	 * to reflect it's new status.
	 * @param wfmv
	 */
	protected void saveObject(BaseDBReqVO obj, boolean isInsert) throws Exception {
		DBProcessor dbp = null;

		//Save the VO via DBProcessor utility.
		try {
			dbp = new DBProcessor(dbConn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
			if(isInsert) {
				dbp.insert(obj);
			} else {
				dbp.update(obj);
			}
		} catch(Exception e) {
			throw e;
		}
	}
	
	protected boolean isInUse(String id) {
		boolean inUse = false;

		//Query Db and look for anywhere that the given configTypeCd is in Use.
		try (PreparedStatement ps = dbConn.prepareStatement(getInUseSql())) {
			ps.setString(1, id);
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
	
	protected void setRedirect(ActionRequest req, String msg) {
		//Build Redirect
		StringBuilder pg = new StringBuilder(125);
		pg.append("/").append(getAttribute(Constants.CONTEXT_NAME));
		pg.append(getAttribute(AdminConstants.ADMIN_TOOL_PATH));
		pg.append("?dataMod=true").append("&actionId=").append(req.getParameter("actionId"));
		pg.append("&organizationId=").append(req.getParameter("organizationId"));
		pg.append(buildRedirectSupplement(req));
		if(msg != null) {
			pg.append("&msg=").append(msg);
		}

		//Set Redirect on request
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, pg.toString());
	}

	protected abstract String buildRedirectSupplement(ActionRequest req);
	protected abstract String getInUseSql();
}
