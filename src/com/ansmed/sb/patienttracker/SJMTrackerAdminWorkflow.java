package com.ansmed.sb.patienttracker;

// JDK 1.6
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// SMT Base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

// Sitebuilder II libs
import com.smt.sitebuilder.action.tracker.TrackerAction;
import com.smt.sitebuilder.action.tracker.TrackerDataContainer;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
* <b>Title</b>SJMTrackerAdminWorkflow.java<p/>
* <b>Description: </b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2011<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Mar 31, 2011
* <b>Changes: </b>
****************************************************************************/
public class SJMTrackerAdminWorkflow extends TrackerAction {
	
	public static final String RESPONSE_MSG = "responseMessage";
	
	public SJMTrackerAdminWorkflow() {
	}
	
	public SJMTrackerAdminWorkflow(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("SJMTrackerAdminWorkflow retrieve...");
		String actionType = req.getParameter("actionType");
		String subType = req.getParameter("subType");
		log.debug("actionType: " + actionType);
		log.debug("subType: " + subType);
		// set up paging
		if (actionType != null && !actionType.equals("assignment") && 
				StringUtil.checkVal(req.getParameter("page")).length() == 0) {
			req.setParameter("page", "1");
		}
		// look-up assignee_id if this is not an admin
		checkUserRole(req);
		
		SMTActionInterface sai = null;
		if (actionType == null || actionType.length() == 0) {
			// retrieve dashboard metrics
			sai = new TrackerDashboardAction(actionInit);
		} else if (actionType.equalsIgnoreCase("patient")) {
			// retrieve the patient(s)
			sai = new SJMPatientManager(actionInit);
		} else if (actionType.equalsIgnoreCase("assignee")) {
			// retrieve the assignee(s)
			sai = new SJMAssigneeManager(actionInit);
		} else if (actionType.equalsIgnoreCase("assignment")) {
			// retrieve the assignment(s)
			sai = new SJMAssignmentFacade(actionInit);
		} else if (actionType.equalsIgnoreCase("reassign")) {
			// retrieve the reassignment view
			sai = new SJMAssignmentFacade(actionInit);
		} else if (actionType.equalsIgnoreCase("interaction")) {
			// retrieve the interaction(s)
			sai = new SJMPatientInteractionManager(actionInit);
		} else if (actionType.equalsIgnoreCase("adhoc")) { // PHASE 3 #3007
			sai = new AdhocFacadeAction(actionInit);
		} else {
			// non-zero length actionType, but not a valid value
			sai = new TrackerDashboardAction(actionInit);
		}
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.retrieve(req);
		
		ModuleVO mod = (ModuleVO)getAttribute(Constants.MODULE_DATA);
		TrackerDataContainer tdc = (TrackerDataContainer) mod.getActionData();
		if (tdc == null) tdc = new TrackerDataContainer();
		if (StringUtil.checkVal(actionType).length() > 0) tdc.setForm(retrieveForm(req,actionType));
		mod.setActionData(tdc);
		req.setAttribute(Constants.MODULE_DATA, mod);
	}
	
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("SJMTrackerAdminWorkflow build...");
		SMTActionInterface sai = null;
		String actionType = req.getParameter("actionType");
		if (actionType == null) {
			sai = new TrackerDashboardAction(actionInit);
		} else if (actionType.equalsIgnoreCase("patient")) {
			sai = new SJMPatientManager(actionInit);
		} else if (actionType.equalsIgnoreCase("assignee")) {
			sai = new SJMAssigneeManager(actionInit);
		} else if (actionType.equalsIgnoreCase("assignment")) {
			sai = new SJMAssignmentFacade(actionInit);
		} else if (actionType.equalsIgnoreCase("reassign")) {
			sai = new SJMAssignmentFacade(actionInit);
		} else if (actionType.equalsIgnoreCase("interaction")) {
			sai = new SJMPatientInteractionManager(actionInit);
		} else if (actionType.equalsIgnoreCase("adhoc")) {
			sai = new AdhocFacadeAction(actionInit);
		}
		log.debug("actionType: " + actionType);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.build(req);
	}
	
	/**
	 * Checks role.  If less than admin, sets assignee ID on session so that we are
	 * able to limit what a non-admin can query for.
	 * @param req
	 * @throws ActionException
	 */
	private void checkUserRole(SMTServletRequest req) throws ActionException {
		log.debug("checking user role...");
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		if (role.getRoleLevel() < SecurityController.ADMIN_ROLE_LEVEL) {
			this.checkAmbassadorProfile(req, user.getProfileId());
		}
	}
	
	private void checkAmbassadorProfile(SMTServletRequest req, String profileId) {
		log.debug("checking ambassador profile using profileId: " + profileId);
		StringBuffer sql = new StringBuffer();
		sql.append("select assignee_id, assignee_profile_id from pt_assignee where assignee_profile_id = ?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, profileId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				req.setParameter("assigneeId", rs.getString("assignee_id"));
				req.setParameter("assigneeProfileId", rs.getString("assignee_profile_id"));
				//req.getSession().setAttribute(SJMTrackerConstants.TRACKER_ASSIGNEE_ID, rs.getString("assignee_id"));
				log.debug("non-admin amb profile/assignee IDs: " + req.getParameter("assigneeProfileId") + "/" + req.getParameter("assigneeId"));
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving assignee profile ID, ", sqle);
		}
	}
	
}
