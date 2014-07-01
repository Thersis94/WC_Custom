package com.ansmed.sb.patienttracker;

// JDK 1.6
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// SMT base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

// Sitebuilder II libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.tracker.vo.AssignmentVO;
import com.smt.sitebuilder.action.tracker.vo.PatientInteractionVO;
import com.smt.sitebuilder.action.tracker.TrackerDataContainer;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
* <b>Title</b>SJMTrackerDashboardAction.java<p/>
* <b>Description: Retrieves dashboard metrics for admins and assignees(Ambassadors)</b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2011<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Mar 31, 2011
* <b>Changes: </b>
****************************************************************************/
public class TrackerDashboardAction extends SBActionAdapter {

	public TrackerDashboardAction() {
		super();
	}

	public TrackerDashboardAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("SJMTrackerDashboardAction retrieve...");
		// retrieve role
		boolean admin = false;
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		if (role != null) {
			admin = role.getRoleLevel() == SecurityController.ADMIN_ROLE_LEVEL ? true : false;
		}
		TrackerDataContainer data = new TrackerDataContainer();
		Map<String, Object> dataMap = new HashMap<String, Object>();
		SMTActionInterface sai = null;
		
		if (admin) {
			// sort the assignments by assign date if we are not already doing a view sort, etc.
			// check viewSort, sortField, if nothing sort by assign date.
			String sortField = StringUtil.checkVal(req.getParameter("sortField"));
			String viewSort = StringUtil.checkVal(req.getParameter("viewSort"));
			log.debug("sortField/viewSort: " + sortField + "/" + viewSort);
			if ((sortField.length() == 0 || sortField.equals("none")) && viewSort.length() == 0) {
				req.setParameter("sortField", "assignDate");
				req.setParameter("sortType", "desc");
			}
		}
		
		// retrieve assignment metrics - active/completed/closed
		sai = new SJMAssignmentFacade(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.retrieve(req);
		ModuleVO mod = (ModuleVO) req.getAttribute(Constants.MODULE_DATA);
		data = (TrackerDataContainer)mod.getActionData();
		countAssignments(req, data.getAssignments(), dataMap, admin);
		
		if (admin) {
			// retrieve interaction metrics for completed needing review
			this.countCompletedAssignmentsNeedingReview(dataMap);
			
		} else {
			// check subType to see if we need to retrieve amb data
			String subType = StringUtil.checkVal(req.getParameter("subType"));
			if (subType.equals("assignee")) {
				sai = new SJMAssigneeManager(actionInit);
				sai.setDBConnection(dbConn);
				sai.setAttributes(attributes);
				sai.retrieve(req);
				TrackerDataContainer ambData =  new TrackerDataContainer();
				mod = (ModuleVO) req.getAttribute(Constants.MODULE_DATA);
				if (mod == null) log.debug("mod is null");
				else ambData = (TrackerDataContainer) mod.getActionData();
				data.setAssignees(ambData.getAssignees());
				data.setForm(ambData.getForm());
			}
		}
		// put the metrics map on the container
		data.setDataMap(dataMap);
		log.debug("dataMap.size: " + dataMap.size());
		mod = (ModuleVO) req.getAttribute(Constants.MODULE_DATA);
		mod.setActionData(data);
		req.setAttribute(Constants.MODULE_DATA, mod);
	}
		
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("SJMTrackerDashboardAction build...");
		this.retrieve(req);
	}
	
	/**
	 * Counts assignments based on certan status values
	 * @param req
	 * @param assignments
	 * @param dataMap
	 * @param isAdmin
	 */
	private void countAssignments(SMTServletRequest req, List<AssignmentVO> assignments, 
			Map<String, Object> dataMap, boolean isAdmin) {
		log.debug("counting assignments...");
		int totalAssignments = 0;
		int pending = 0;
		int inProgress = 0;
		int assist = 0;
		int reassign = 0;
		int needName = 0;
		int complaint = 0;
		int completed = 0;
		int ambPending = 0;
		int ambInProgress = 0;
		int ambAssist = 0;
		int ambComplaint = 0;
		int ambCompleted = 0;
		int ambTotalAssignments = 0;
		if (assignments != null) {
			//log.debug("assignments list size for counting is: " + assignments.size());
			// loop assignments and count
			for (AssignmentVO avo : assignments) {
				Integer statusId = avo.getAssignmentStatusId();
				if (isAdmin) { // this branch gathers the overall count
					switch (statusId) {
						case 10: // 'pending'
							pending++;
							break;
						case 30: // in progress
							inProgress++;
							break;
						case 40: // requests for assistance
							assist++;
							break;
						case 50: // requests for reassignment
							reassign++;
							break;
						case 60: // complaint
							complaint ++;
							break;
						case 70: // 'need Dr's name'
							needName++;
							break;
						case 100: // complete
							completed++;
							break;
						default:
							break;
					} // switch
				}
					
				// now count this assignees assignments
				UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
				if (avo.getAssignee().getProfileId().equals(user.getProfileId())) {
					switch (statusId) {
					case 10: // 'pending'
						ambPending++;
						break;
					case 30: // in progress
						ambInProgress++;
						break;
					case 40: // requests for assistance
						ambAssist++;
						break;
					case 60: // complaint
						ambComplaint++;
						break;
					case 100: //completed - used for admin's personal counter only
						ambCompleted++;
						break;
					default:
						break;
				}
			}
		}
		
		totalAssignments = pending + inProgress + assist + complaint + needName + completed;
		ambTotalAssignments = ambPending + ambInProgress + ambAssist + ambComplaint;
		}
		//log.debug("pending/inProgress/assist: " + pending + "/" + inProgress + "/" + assist);
		//log.debug("reassign/complaint/completed: " + reassign + "/" + complaint + "/" + completed);
		// add counts to map
		if (isAdmin) {
			dataMap.put("pending", pending);
			dataMap.put("inProgress", inProgress);
			dataMap.put("assist", assist);
			dataMap.put("reassign", reassign);
			dataMap.put("complaint", complaint);
			dataMap.put("needName", needName);
			dataMap.put("completed", completed);
			dataMap.put("totalAssignments", totalAssignments);
		}
		
		dataMap.put("ambPending", ambPending);
		dataMap.put("ambInProgress", ambInProgress);
		dataMap.put("ambAssist", ambAssist);
		dataMap.put("ambCompleted", ambCompleted);
		dataMap.put("ambTotalAssignments", ambTotalAssignments);
	}
	
	/**
	 * Counts interactions that have not yet been reviewed
	 * @param assignments
	 * @param dataMap
	 */
	@SuppressWarnings("unused")
	private void countInteractions(List<PatientInteractionVO> interactions, Map<String, Object> dataMap) {
		int reviewNeeded = 0;
		Map<String, String> reviewMap = new HashMap<String, String>();
		for (PatientInteractionVO p : interactions) {
			if (! p.isReviewed()) {
				if (reviewMap.get(p.getAssignmentId()) == null) {
					reviewNeeded++;
					reviewMap.put(p.getAssignmentId(), "Yes");
				}
			}
		}
		dataMap.put("reviewNeeded", new Integer(reviewNeeded));
		dataMap.put("reviewMap", reviewMap);
	}

	/**
	 * Counts the number of assignments that are completed and that are
	 * in a 'review needed' status.  Places the count on the dataMap and places a map of 
	 * assignment IDs on the dataMap, both for use by the JSTL view.
	 * @param dataMap
	 */
	private void countCompletedAssignmentsNeedingReview(Map<String,Object> dataMap) {
		// count 'review needed' only for completed assignments needing review
		StringBuffer sql = new StringBuffer();
		sql.append("select distinct b.PT_ASSIGNMENT_ID, b.REVIEW_FLG from PT_ASSIGNMENT a ");  
		sql.append("inner join PT_INTERACTION b on a.PT_ASSIGNMENT_ID = b.PT_ASSIGNMENT_ID ");
		sql.append("where a.ASSIGNMENT_STATUS_ID = ? and b.REVIEW_FLG = 0 ");
		log.debug("Dashboard review count SQL: " + sql.toString());
		Map<String, String> reviewMap = new HashMap<String, String>();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, SJMTrackerConstants.STATUS_COMPLETED);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				reviewMap.put(rs.getString("pt_assignment_id"), "Yes");
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving 'review needed' count in admin dashboard, ", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) { log.error("Error closing prepared statement, ", e); }
		}
		dataMap.put("reviewMap", reviewMap);
		dataMap.put("reviewNeeded", new Integer(reviewMap.size()));
	}
}
