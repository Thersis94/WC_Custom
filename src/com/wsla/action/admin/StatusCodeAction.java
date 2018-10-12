package com.wsla.action.admin;

import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.ticket.StatusCodeVO;

/****************************************************************************
 * <b>Title</b>: StatusCodeAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the listing of status codes and the management 
 * of role assignments
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 11, 2018
 * @updates:
 ****************************************************************************/

public class StatusCodeAction extends SBActionAdapter {
	/**
	 * Ajax key to call this class
	 */
	public static final String AJAX_KEY = "statusCodeList";
	
	/**
	 * 
	 */
	public StatusCodeAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public StatusCodeAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (! req.hasParameter("json")) return;
		setModuleData(getStatusCodes());
	}
	
	/**
	 * Gets all of the status codes
	 * @return
	 */
	public List<StatusCodeVO> getStatusCodes() {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from ").append(getCustomSchema()).append("wsla_ticket_status a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("role b on a.role_id = b.role_id ");
		sql.append("order by status_nm");
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), null, new StatusCodeVO());
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.info("Building ...");
		
		StatusCodeVO status = new StatusCodeVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.update(status);
		} catch (InvalidDataException | DatabaseException e) {
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
}

