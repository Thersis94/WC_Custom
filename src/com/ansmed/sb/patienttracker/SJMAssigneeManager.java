package com.ansmed.sb.patienttracker;

// JDK 6
import java.util.Collections;
import java.util.List;

// SB_ANS_Medical libs
import com.ansmed.sb.patienttracker.comparator.AssigneeAvailabilityComparator;

// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;

// SiteBuilder II libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.tracker.AssigneeManager;
import com.smt.sitebuilder.action.tracker.TrackerAction;
import com.smt.sitebuilder.action.tracker.TrackerDataContainer;
import com.smt.sitebuilder.action.tracker.vo.AssigneeVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
* <b>Title</b>SJMAssigneeManager.java<p/>
* <b>Description: Wrapper class to implement custom SJM patient tracker behavior.</b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2011<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Dec 01, 2011
* <b>Changes: </b>
****************************************************************************/
public class SJMAssigneeManager extends SBActionAdapter {
	
	private static final String DEFAULT_NEW_AMB_PASSWORD = "newambassador";
	
	public SJMAssigneeManager() {
		super();
	}

	public SJMAssigneeManager(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("SJMAssigneeManager retrieve...");

		SMTActionInterface sai = null;
		// retrieve assignees
		sai = new AssigneeManager(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.retrieve(req);
		
		// check to see if we are sorting by a custom value
		String sortField = StringUtil.checkVal(req.getParameter("sortField"));
		if (sortField.equalsIgnoreCase("dayVal")) {
			ModuleVO mod = (ModuleVO) req.getAttribute(Constants.MODULE_DATA);
			TrackerDataContainer tdc = (TrackerDataContainer) mod.getActionData();
			this.sortByDayValue(req, tdc.getAssignees());
			mod.setActionData(tdc);
			req.setAttribute(Constants.ACTION_DATA, mod);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("SJMAssigneeManager build...");
		
		boolean isUpdate = false;
		if (StringUtil.checkVal(req.getParameter("assigneeId")).length() > 0) {
			isUpdate = true;
		}
		log.debug("isUpdate? " + isUpdate);
		
		// if we're adding a new ambassador, assign a default password
		if (StringUtil.checkVal(req.getParameter("actionType")).equalsIgnoreCase("assignee") && 
				StringUtil.checkVal(req.getParameter("assigneeId")).length() == 0) {
			req.setParameter("assigneePassword", DEFAULT_NEW_AMB_PASSWORD);
		}
		
		// insert/update the assignment
		SMTActionInterface sai = null;
		sai = new AssigneeManager(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.build(req);
    	
		this.processRedirect(req, isUpdate);
	}
	
	/**
	 * Builds redirect
	 * @param req
	 * @param isUpdate
	 */
	private void processRedirect(SMTServletRequest req, boolean isUpdate) {
    	SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		if (role != null) {
	    	StringBuffer url = new StringBuffer();
	    	PageVO page = (PageVO)req.getAttribute(Constants.PAGE_DATA);
	    	url.append(StringUtil.checkVal(attributes.get("contextPath")));
	    	url.append(page.getFullPath());

			if (role.getRoleLevel() < AssigneeManager.MIN_ADMIN_TYPE_ID) {
				// non-admin redirect to dashboard/profile view
		    	url.append("?subType=assignee");
			} else {
				// build admin redirect
				url.append("?actionType=assignee");
				if (isUpdate) {
					url.append("&assigneeId=").append(req.getParameter("assigneeId"));
					url.append("&formSubmittalId=").append(req.getParameter("formSubmittalId"));
				}
	    	}
	    	
			String redirectMsg = this.processRedirectMessage(req, role, isUpdate);
	    	url.append("&organizationId=").append(req.getParameter("organizationId"));
	    	if (redirectMsg != null && redirectMsg.length() > 0) {
	    		url.append("&msg=").append(redirectMsg);
	    	}
			
	    	log.debug("SJMAssigneeManager redirect URL: " + url);
	    	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
	    	req.setAttribute(Constants.REDIRECT_URL, url.toString());
		}
	}
	
	/**
	 * Builds a friendly redirect message based upon the build message, the role, and whether the build
	 * operation was an insert or an update.
	 * @param req
	 * @param role
	 * @param isUpdate
	 * @return
	 */
	private String processRedirectMessage(SMTServletRequest req, SBUserRole role, boolean isUpdate) {
		StringBuffer buildMsg = new StringBuffer();
		StringBuffer redirectMsg = new StringBuffer();
		if (req.getAttribute(TrackerAction.TRACKER_BUILD_MSG) != null) {
			buildMsg = (StringBuffer) req.getAttribute(TrackerAction.TRACKER_BUILD_MSG);	
		}
		
		if (buildMsg.indexOf("Exception") > -1) {
			String excType = buildMsg.toString();
			if (excType.equals("AssigneeDuplicateMailException")) {
				redirectMsg.append("Duplicate email address found:  The ");
				if (isUpdate) redirectMsg.append("updated ");
				redirectMsg.append("email address you specified is already in use.");
			} else {
				redirectMsg.append("An error occurred and the system was unable to ");
				
				if (isUpdate) {
					redirectMsg.append("update ");
				} else {
					redirectMsg.append("create ");
				}
				
				if (role.getRoleLevel() < AssigneeManager.MIN_ADMIN_TYPE_ID) {
					redirectMsg.append("your ");
				} else {
					redirectMsg.append("the ambassador's ");
				}
				
				redirectMsg.append("profile. (Code: ");
				if (excType.equals("AssigneeException")) {
					redirectMsg.append("System Error)");
				} else if (excType.equals("AssigneeAuthRecordException")) {
					redirectMsg.append("Authentication)");
				} else if (excType.equals("AssigneeRoleException")) {
					redirectMsg.append("Role Assignment)");
				} else if (excType.equals("AssigneeBaseRecordException")) {
					redirectMsg.append("Base Record)");
				} else {
					redirectMsg.append("System Error)");
				}
			}
		} else {
			redirectMsg.append("You have successfully ");
			if (isUpdate) {
				redirectMsg.append("updated ");
			} else {
				redirectMsg.append("created ");
			}

			if (role.getRoleLevel() < AssigneeManager.MIN_ADMIN_TYPE_ID) {
				redirectMsg.append("your ");
			} else {
				redirectMsg.append("the ambassador's ");	
			}
			redirectMsg.append("profile.");
		}
		return redirectMsg.toString();
	}
	
	/**
	 * 
	 * @param req
	 * @param sortField
	 */
	private void sortByDayValue(SMTServletRequest req, List<AssigneeVO> ambs) {
		String day = StringUtil.checkVal(req.getParameter("day"));
		if (day.length() == 0) return;
		log.debug("sorting by day: " + day);
		// unpack the returned list
		if (ambs.size() > 0) {
			AssigneeAvailabilityComparator aac = new AssigneeAvailabilityComparator();
			aac.setSortValue(day);
			aac.setSortType(req.getParameter("sortType"));
			Collections.sort(ambs, aac);
		}
	}
}
