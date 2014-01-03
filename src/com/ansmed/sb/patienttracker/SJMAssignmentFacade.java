package com.ansmed.sb.patienttracker;

// JDK 1.6
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

//SMB Baselibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// SiteBuilder II libs
import com.smt.sitebuilder.action.tracker.vo.AssignmentVO;
import com.smt.sitebuilder.action.tracker.AssigneeManager;
import com.smt.sitebuilder.action.tracker.TrackerAction;

/****************************************************************************
* <b>Title</b>SJMAssigmentFacade.java<p/>
* <b>Description: </b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2012<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author David Bargerhuff
* @version 1.0
* @since Feb 16, 2012
* <b>Changes: </b>
****************************************************************************/
public class SJMAssignmentFacade extends TrackerAction {
	
	public SJMAssignmentFacade() {
		super();
	}

	public SJMAssignmentFacade(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("SJMAssignmentFacade retrieve...");
		SMTActionInterface sai = null;
		String actionType = StringUtil.checkVal(req.getParameter("actionType"));
		if (actionType.equals("reassign")) {
			sai = new SJMReassignmentManager(this.actionInit);
		} else {
			sai = new SJMAssignmentManager(this.actionInit);
		}
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		sai.retrieve(req);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("SJMAssignmentFacade build...");
		SMTActionInterface sai = null;
		String actionType = StringUtil.checkVal(req.getParameter("actionType"));
		if (actionType.equals("reassign")) {
			sai = new SJMReassignmentManager(this.actionInit);
		} else {
			// check for reassignment triggers
			AssignmentVO avo = new AssignmentVO();
			avo.setData(req);
			if (this.checkForReassignment(req, avo)) {
				sai = new SJMReassignmentManager(this.actionInit);
			} else {
				sai = new SJMAssignmentManager(this.actionInit);				
			}
		}
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		sai.build(req);	
	}
	
	/**
	 * Checks to see if this is an update and if this update requires a reassignment of the assignment.
	 * @param req
	 * @param avo
	 * @return
	 */
	private boolean checkForReassignment(SMTServletRequest req, AssignmentVO avo) {
		boolean isReassignment = false;
		// check for certain changes in status
		if (avo.getAssignmentId() != null) {
			if (! avo.getCurrentStatusId().equals(avo.getAssignmentStatusId())) {
				switch(avo.getAssignmentStatusId()) {
					case 60: // complaint
						// if this is an update to 'complaint' status, we only reassign
						// the assignment if the currently assigned ambassador isn't already an admin.
						if (! this.checkForAdminOwnerOfAssignment(req, avo)) {
							isReassignment = true;
						}
						break;
					case 50: // request reassignment
					case 900: // expired
						isReassignment = true;
						break;
					default:
						break;
				}
			} else if (Convert.formatBoolean(req.getParameter("productComplaint"))) {
				// this was flagged as product complaint via an interaction, if not already a complaint, then reassign
				if (! avo.getCurrentStatusId().equals(SJMTrackerConstants.STATUS_COMPLAINT)) {
					isReassignment = true;
				}
			}
		}
		log.debug("isReassignment: " + isReassignment);
		return isReassignment;
	}
	
	/**
	 * Checks to see if the owner of the assignment being updated to 'complaint' status is an admin.
	 * @param req
	 * @param avo
	 * @return
	 */
	private boolean checkForAdminOwnerOfAssignment(SMTServletRequest req, AssignmentVO avo) {
		log.debug("complaint detected, determining if updated assignment belongs to admin");
		boolean isAdminOwner = false;
		StringBuffer sql = new StringBuffer();
		sql.append("select a.assignee_type_id from pt_assignee a ");
		sql.append("inner join pt_assignment b on a.assignee_id = b.assignee_id ");
		sql.append("where b.assignee_id = ? and b.pt_assignment_id = ? ");
		log.debug("admin owner sql: " + sql.toString());
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareCall(sql.toString());
			ps.setString(1, avo.getAssigneeId());
			ps.setString(2, avo.getAssignmentId());
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				int type = rs.getInt("assignee_type_id");
				if (type >= AssigneeManager.MIN_ADMIN_TYPE_ID) {
					isAdminOwner = true;
					req.setAttribute("isAdminOwnedAssignment", new Boolean(isAdminOwner));
				}
			}
		} catch (SQLException sqle) {
			log.error("Error determining if assignee is an admin, ", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) { log.debug("Error closing PreparedStatement, ", e); }
		}
		return isAdminOwner;
	}
}
