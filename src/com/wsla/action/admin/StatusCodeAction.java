package com.wsla.action.admin;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.ticket.StatusCodeVO;
import com.wsla.data.ticket.StatusNotificationVO;

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
	 * Helper constructor
	 * @param dbConn
	 * @param attributes
	 */
	public StatusCodeAction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		this.dbConn = dbConn;
		this.attributes = attributes;
	}

	/**
	 * @param actionInit
	 */
	public StatusCodeAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	public void deleteNotification(ActionRequest req) 
	throws InvalidDataException, DatabaseException {
		
		StatusNotificationVO statusNote = new StatusNotificationVO();
		statusNote.setStatusNotificationId(req.getParameter("statusNotificationId"));
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.delete(statusNote);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (! req.hasParameter("json")) return;
		
		if (req.hasParameter("statusCode")) 
			setModuleData(getNotifications(req.getParameter("statusCode")));
		else 
			setModuleData(getStatusCodes(null, null, null));
	}
	
	/**
	 * Gets the notifications for a given statusCode
	 * @param statusCode
	 * @return
	 */
	public List<StatusNotificationVO> getNotifications(String statusCode) {
		StringBuilder sql = new StringBuilder(256);
		sql.append("select * from ").append(getCustomSchema());
		sql.append("wsla_ticket_status_notification a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_ticket_status b on a.status_cd = b.status_cd ");
		sql.append("inner join email_campaign_instance d ");
		sql.append("on a.campaign_instance_id = d.campaign_instance_id ");
		sql.append("where a.status_cd = ? order by instance_nm ");
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), Arrays.asList(statusCode), new StatusNotificationVO());
	}
	
	/**
	 * Gets all of the status codes
	 * @return
	 */
	public List<StatusCodeVO> getStatusCodes(String roleId, Locale locale, String statusCode) {
		StringBuilder sql = new StringBuilder(128);
		List<Object> vals = new ArrayList<>();
		
		sql.append("select status_cd, active_flg,a.role_id, group_status_cd, authorized_role_txt, ");
		sql.append("billable_activity_cd, next_step_url, next_step_btn_key_cd, b.role_nm, ");
		if (locale == null) sql.append("status_nm ");
		else sql.append("case when value_txt is null then status_nm else value_txt end as status_nm ");
		sql.append("from ").append(getCustomSchema()).append("wsla_ticket_status a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("role b on a.role_id = b.role_id ");
		
		if (locale != null) {
			sql.append("left outer join resource_bundle_key c on a.status_cd = c.key_id ");
			sql.append("left outer join resource_bundle_data d on c.key_id = d.key_id ");
			sql.append("and language_cd = ? and country_cd = ? ");
			vals.add(locale.getLanguage());
			vals.add(locale.getCountry());
		}
		
		if (! StringUtil.isEmpty(roleId)) {
			sql.append("where a.role_id = ? ");
			vals.add(roleId);
		} else if (!StringUtil.isEmpty(statusCode)) {
			sql.append("where a.status_cd = ? ");
			vals.add(statusCode);
		}
		
		sql.append("order by status_nm");
		log.debug(sql);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), vals, new StatusCodeVO());
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("Building ...");
		
		try {
			if (req.hasParameter("notification")) {
				putModuleData(saveStatusNotification(req));
			} else if (req.hasParameter("statusNotificationId")) {
				deleteNotification(req);
			} else {
				putModuleData(saveStatus(req));
			}
		} catch (InvalidDataException | DatabaseException e) {
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * Saves the core status info
	 * @param req
	 * @return
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public StatusCodeVO saveStatus(ActionRequest req) 
	throws InvalidDataException, DatabaseException {
		StatusCodeVO status = new StatusCodeVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.update(status);
		
		return status;
	}
	
	/**
	 * Adds a notification type for the given status
	 * @param req
	 * @return
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public StatusNotificationVO saveStatusNotification(ActionRequest req) 
	throws InvalidDataException, DatabaseException {
		
		String delim = StringUtil.getDelimitedList(req.getParameterValues("roles"), false, ",");
		req.setParameter("roleId", delim, false);
		StatusNotificationVO ntfcn = new StatusNotificationVO(req);
		log.debug(ntfcn);
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.save(ntfcn);
		
		return ntfcn;
	}
}

