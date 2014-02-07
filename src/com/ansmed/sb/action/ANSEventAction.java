package com.ansmed.sb.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import com.ansmed.sb.security.ANSRoleFilter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.event.EventFacadeAction;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/*****************************************************************************
<p><b>Title</b>: ANSEventAction.java</p>
<p>Description: <b/></p>
<p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author James Camire
@version 1.0
@since Mar 28, 2009
Last Updated:
 ***************************************************************************/
public class ANSEventAction extends SBActionAdapter {

	/**
	 * 
	 */
	public ANSEventAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public ANSEventAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
    public void build(SMTServletRequest req) throws ActionException {
		String msg = "You have sucessfully updated the event status";
		/*
		 * THIS CODE WAS LEFT BECAUSE SOMEDAY ANS WILL PROBABLY WANT TO APPROVE EVENTS *AND* EVENT TYPES.
		 * 
		StringBuilder sb = new StringBuilder();
		sb.append("update xr_event_signup set event_status_id = ? ");
		sb.append("where event_signup_id = ? ");
    	log.debug("ANS Event Status update SQL: " + sb);
    	
    	Boolean isDeleteStatus = Boolean.FALSE;
    	PreparedStatement ps = null;
    	try {
    		ps = dbConn.prepareStatement(sb.toString());
    		
			Enumeration<?> names = req.getParameterNames();
			while (names.hasMoreElements()) {
				String key = (String) names.nextElement();
				if (key.indexOf("eventStatusId_") > -1) {
					String val = req.getParameter(key);
					Integer status =  Convert.formatInteger(val.substring(0, val.indexOf("|")));
					log.debug(EventFacadeAction.STATUS_DELETED + "|" + status);
					if (status.equals(EventFacadeAction.STATUS_DELETED)) {
						isDeleteStatus = Boolean.TRUE;
						log.debug("Is delete true");
					}
					
					String id = val.substring(val.indexOf("|") + 1);
					log.debug(status + "|" + id);
					ps.setInt(1, status);
					ps.setString(2, id);
					ps.addBatch();
				}
			}
			
			// Update the batch
			ps.executeBatch();
			
			// Delete any records if necessary
			if (isDeleteStatus) {
				log.debug("Deleting ...");
				SMTActionInterface sai = new EventApprovalAction(this.actionInit);
				sai.setDBConnection(dbConn);
				sai.delete(req);
			}
    	} catch(SQLException sqle) {
    		log.error("Unable to update list of ANS event users", sqle);
    		msg = "Unable to update the event status.  Please contact your administrator";
    	} finally {
    		try {
    			ps.close();
    		} catch(Exception e) {}
    	}
    	*/
    	//process the eventType stuff
    	this.saveEventTypeSignups(req);
    	
    	// Redirect the user back to the original page
    	String url = req.getRequestURI() + "?msg=" + msg;
    	req.setAttribute(Constants.REDIRECT_URL, url);
    	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    }
	
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
    public void retrieve(SMTServletRequest req) throws ActionException {
		// Handle the admin searches
		String sbActionId = StringUtil.checkVal(req.getParameter(SB_ACTION_ID));
		if (sbActionId.length() > 0) {
			super.retrieve(req);
			return;
		}
		/*  
		 * THIS CODE WAS LEFT BECAUSE SOMEDAY ANS WILL PROBABLY WANT TO APPROVE EVENTS *AND* EVENT TYPES.
		 * 
    	ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
    	String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
    	ANSRoleFilter filter = new ANSRoleFilter();
    	SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
    	
    	// Build the SQL statement using the role filter in the where clause
    	StringBuilder sql = new StringBuilder();
    	sql.append("select d.first_nm + ' ' + d.last_nm as rep_nm, a.first_nm, ");
    	sql.append("a.last_nm, c.event_entry_id, event_status_id, event_nm, ");
    	sql.append("start_dt, a.surgeon_id, d.sales_rep_id, event_signup_id, ");
    	sql.append("a.profile_id , wait_list_no from ").append(schema).append("ans_surgeon a ");
    	sql.append("inner join xr_event_signup b on a.profile_id = b.profile_id ");
    	sql.append("inner join event_entry c on b.event_entry_id = c.event_entry_id ");
    	sql.append("inner join ").append(schema).append("ans_sales_rep d ");
    	sql.append("on a.sales_rep_id = d.sales_rep_id ");
    	sql.append("where approval_required_flg = 1 and c.start_dt >= ? ");
    	sql.append("and event_status_id < ");
    	sql.append(EventFacadeAction.STATUS_APPROVED).append(" ");
    	sql.append(filter.getSearchFilter(role, "d"));
    	sql.append(" order by c.start_dt ");
    	
    	log.debug("ANS Event SQL: " + sql);
    	PreparedStatement ps = null;
    	List<ANSEventVO> data = new ArrayList<ANSEventVO>();
    	try {
    		ps = dbConn.prepareStatement(sql.toString());
    		ps.setDate(1, Convert.formatSQLDate(new Date()));
    		ResultSet rs = ps.executeQuery();
    		while (rs.next()) {
    			data.add(new ANSEventVO(rs));
    			log.debug(rs.getString("event_signup_id"));
    		}
    	} catch(SQLException sqle) {
    		log.error("Unable to retrieve list of ANS event users", sqle);
    	} finally {
    		try {
    			ps.close();
    		} catch(Exception e) {}
    	}
    	*/
    	//load the eventType signups pending approval
    	this.loadEventTypeSignups(req);
    	
    	//mod.setActionData(data);
    	//mod.setDataSize(data.size());
	}
	
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#update(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
    public void update(SMTServletRequest req) throws ActionException {
		super.update(req);
		StringBuilder url = new StringBuilder("/");
		url.append(attributes.get(Constants.CONTEXT_NAME)).append("/admintool");
		url.append("?actionId=SB_MODULE&moduleTypeId=ANS_EVENT&organizationId=");
		url.append(req.getParameter("organizationId")).append("&msg=");
		url.append(req.getAttribute("MESSAGE"));
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
		log.debug("ANS Events Redirect URL: " + url);
	}
	
	private void loadEventTypeSignups(SMTServletRequest req) {
    	final String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
    	ANSRoleFilter filter = new ANSRoleFilter();
    	SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
    	
		StringBuffer sql = new StringBuffer();
		sql.append("select d.first_nm + ' ' + d.last_nm as rep_nm, b.event_type_id, ");
    	sql.append("a.first_nm + ' ' + a.last_nm as surgeon_nm, c.type_nm, ");
    	sql.append("a.surgeon_id, b.event_type_approval_id, b.create_dt, b.event_status_id ");
    	sql.append("from ").append(schema).append("ans_surgeon a ");
    	sql.append("inner join ").append(schema).append("ans_event_type_approval b ");
    	sql.append("on a.surgeon_id = b.surgeon_id ");
    	sql.append("inner join event_type c on b.event_type_id = c.event_type_id ");
    	sql.append("inner join ").append(schema).append("ans_sales_rep d ");
    	sql.append("on a.sales_rep_id = d.sales_rep_id ");
    	sql.append("where b.event_status_id=? " );
    	sql.append(filter.getSearchFilter(role, "d"));
    	sql.append("order by b.create_dt");
		PreparedStatement ps = null;
		List<ANSEventVO> data = new ArrayList<ANSEventVO>();
    	try {
    		ps = dbConn.prepareStatement(sql.toString());
    		ps.setString(1, Integer.valueOf(EventFacadeAction.STATUS_PENDING).toString());
    		ResultSet rs = ps.executeQuery();
    		ANSEventVO vo = null;
    		while (rs.next()) {
    			vo = new ANSEventVO();
    			vo.setStatusId(Convert.formatInteger(rs.getString("event_status_id")));
    			vo.setEventSignupId(rs.getString("event_type_approval_id"));
    			vo.setEventId(rs.getString("event_type_id"));
    			vo.setSalesRepName(rs.getString("rep_nm"));
    			vo.setSurgeonName(rs.getString("surgeon_nm"));
    			vo.setEventName(rs.getString("type_nm"));
    			vo.setEventDate(rs.getDate("create_dt"));
    			vo.setSurgeonId(rs.getString("surgeon_id"));
    			data.add(vo);
    			log.debug(rs.getString("event_type_approval_id"));
    		}
		} catch (SQLException sqle) {
			log.error("error loading eventType signups", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		req.setAttribute("eventTypeSignups", data);
	}
	
	
	public void saveEventTypeSignups(SMTServletRequest req) throws ActionException {
		final String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(schema).append("ans_event_type_approval ");
		sb.append("set event_status_id = ?, update_dt = ?, process_dt = ? ");
		sb.append("where event_type_approval_id = ? ");
    	log.debug("ANS Event Status update SQL: " + sb);
    	
    	PreparedStatement ps = null;
    	List<String> purgeRsvps = new ArrayList<String>();
    	try {
    		ps = dbConn.prepareStatement(sb.toString());
    		
			Enumeration<?> names = req.getParameterNames();
			while (names.hasMoreElements()) {
				String key = (String) names.nextElement();
				if (key.indexOf("eventTypeStatusId_") > -1) {
					String val = req.getParameter(key);
					String status =  val.substring(0, val.indexOf("|"));
					String id = val.substring(val.indexOf("|") + 1);
					log.debug(status + "|" + id);
					
					//if not approved we need to cascade this status change to the event RSVP too.
					/* Dave - commented out, but saving in case we need to replace STATUS_APPROVED with the prev line.
					//if (Convert.formatInteger(status).intValue() != EventFacadeAction.STATUS_PENDING_PREV_ATT.intValue())
					*/
					if (Convert.formatInteger(status).intValue() != EventFacadeAction.STATUS_APPROVED)
						purgeRsvps.add(id);
					
					ps.setString(1, status);
					ps.setTimestamp(2, Convert.getCurrentTimestamp());
					// set process_dt to null upon update so that automated event email notification
					// 'sees' this update and processes it.
					ps.setString(3, null);
					ps.setString(4, id);
					ps.addBatch();
				}
			}
			
			// Update the batch
			ps.executeBatch();
			
    	} catch(SQLException sqle) {
    		log.error("Unable to update list of ANS eventType approvals", sqle);
    	} finally {
    		try {
    			ps.close();
    		} catch(Exception e) {}
    	}
    	
    	//Delete any event signup records if necessary
    	//the primary key on these records is the SAME primary key that was inserted on the 
    	// ans_event_type_approval table.  See PhysicianEventAction-->build-->sharedPkId
		if (purgeRsvps.size() > 0) {
			log.debug("Deleting event signups...");
			StringBuffer sql = new StringBuffer();
			sql.append("delete from xr_event_signup where event_signup_id in (''");
			for (int x=0; x < purgeRsvps.size(); x++) {
				sql.append(",?");
			}
			sql.append(")");
			log.debug("SQL=" + sql);
			try { 
				ps = dbConn.prepareStatement(sql.toString());
				for (int x=0; x < purgeRsvps.size(); x++) {
					ps.setString(x+1, purgeRsvps.get(x));
				}
				ps.executeUpdate();
			} catch (SQLException sqle) {
				
			} finally {
				try { ps.close(); } catch (Exception e) {}
			}
		}
	}
}
