package com.ansmed.sb.action;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;

import com.siliconmtn.http.session.SMTSession;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.event.EventFacadeAction;
import com.smt.sitebuilder.action.event.UserEventAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
<p><b>Title</b>: PhysicianEventAction.java</p>
<p>Description: <b/></p>
<p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author James McKain
@version 1.0
@since Jun 22, 2009
Last Updated:
 ***************************************************************************/
public class PhysicianEventAction extends SimpleActionAdapter {

	public static final String PHYS_QUAL_DATA = "physQualData";
	public static final String PHYS_QUAL_DATA_VO = "physQualDataVo";
	
	/**
	 * 
	 */
	public PhysicianEventAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public PhysicianEventAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
    public void build(ActionRequest req) throws ActionException {
		log.debug("starting PhysicianEventAction build action");
		String action = StringUtil.checkVal(req.getParameter("action"));
		final String schema = (String) this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String msg = null;
		
		if (action.equalsIgnoreCase("nominate")) {
			
			//determine data source used for the nomination and set value.
			SMTSession ses = (SMTSession) req.getSession();
			boolean altData = ((PhysQualDataVO) ses.getAttribute(PHYS_QUAL_DATA)).getIsAltData();
			String source = "";
			if (altData) {
				source = "altData";
			} else {
				source = "planData";
			}
			
			StringBuffer sql = new StringBuffer();
			sql.append("insert into ").append(schema).append("ans_event_type_approval ");
			sql.append("(event_type_approval_id, surgeon_id, event_type_id, ");
			sql.append("event_status_id, event_qual_src, create_dt) ");
			sql.append("values (?,?,?,?,?,?)");
			PreparedStatement ps = null;
			String sharedPkId = new UUIDGenerator().getUUID();
	    	try {
	    		ps = dbConn.prepareStatement(sql.toString());
	    		ps.setString(1, sharedPkId);
	    		ps.setString(2, req.getParameter("surgeonId"));
	    		ps.setString(3, req.getParameter("eventTypeId"));
	    		ps.setInt(4, EventFacadeAction.STATUS_PENDING);
	    		ps.setString(5, source);
	    		ps.setTimestamp(6, Convert.getCurrentTimestamp());
    			ps.executeUpdate();
    			msg = "Your nomination was recorded successfully.";
    				
	    	} catch(SQLException sqle) {
	    		log.error("Unable to register eventType nomination", sqle);
	    		msg = "An error has occured, your transaction was not processed.";
	    	} finally {
	    		try { ps.close(); } catch(Exception e) {}
	    	}
	    	
	    	//pre-register the surgeon for an upcoming event
	    	if (StringUtil.checkVal(req.getParameter("eventEntryId")).length() > 0 && 
	    			StringUtil.checkVal(req.getParameter("profileId")).length() > 0) {
	    		
	    		//set the enrollment status because we already know they should be approved for this event
	    		req.setAttribute(EventFacadeAction.STATUS_OVERRIDE, EventFacadeAction.STATUS_PENDING);
	    		req.setParameter(EventFacadeAction.USER_SIGNUP, "true");
				
				//pass-through users signing up for individual events (not eventTypes)
				SMTActionInterface ai = new UserEventAction(actionInit);
				ai.setDBConnection(dbConn);
				ai.setAttributes(attributes);
				ai.build(req);
	    	}
			
		} else if (Convert.formatBoolean(req.getParameter("userSignup"))) {
			//delete any existing event enrollments if this is what the user agreed to
			if (Convert.formatBoolean(req.getParameter("delExisting"))) {
				log.debug("deleting existing event enrollments");
				PreparedStatement ps = null;
				StringBuffer sql = new StringBuffer();
				sql.append("delete from xr_event_signup ");
				sql.append("where profile_id=? and attendence_flg is null or attendence_flg=0");
				try {
					ps = dbConn.prepareStatement(sql.toString());
		    		ps.setString(1, req.getParameter("profileId"));
		    		ps.executeUpdate();
		    		
				} catch (SQLException sqle) {
					log.error("Unable to delete existing event enrollments", sqle);
		    	} finally {
		    		try { ps.close(); } catch(Exception e) {}
		    	}
			}

    		//set the enrollment status because we already know they should be approved for this event
    		req.setAttribute(EventFacadeAction.STATUS_OVERRIDE, EventFacadeAction.STATUS_APPROVED);
			
			//pass-through users signing up for individual events (not eventTypes)
			SMTActionInterface ai = new UserEventAction(actionInit);
			ai.setDBConnection(dbConn);
			ai.setAttributes(attributes);
			ai.build(req);
		}
    	
    	// Redirect the user back to the original page
		StringBuffer url = new StringBuffer();
		url.append(req.getRequestURI()).append("?");
		url.append("page=").append(StringUtil.checkVal(req.getParameter("page")));
		url.append("&order=").append(StringUtil.checkVal(req.getParameter("order")));
		url.append("&tmName=").append(StringUtil.checkVal(req.getParameter("tmName")));
		url.append("&lastName=").append(StringUtil.checkVal(req.getParameter("lastName")));
		url.append("&state=").append(StringUtil.checkVal(req.getParameter("state")));
		url.append("&zipCode=").append(StringUtil.checkVal(req.getParameter("zipCode")));
		url.append("&fullName=").append(StringUtil.checkVal(req.getParameter("fullName")));
		url.append("&order=").append(StringUtil.checkVal(req.getParameter("order")));
		url.append("&searchSubmitted=").append(StringUtil.checkVal(req.getParameter("searchSubmitted")));
		url.append("&surgeonId=").append(StringUtil.checkVal(req.getParameter("surgeonId")));
		url.append("&profileId=").append(StringUtil.checkVal(req.getParameter("profileId")));
		url.append("&rpp=").append(StringUtil.checkVal(req.getParameter("rpp")));
		url.append("&businessPlan=").append(StringUtil.checkVal(req.getParameter("businessPlan")));
		url.append("&fromTab=").append(StringUtil.checkVal(req.getParameter("fromTab")));
		url.append("&planTypeId=").append(StringUtil.checkVal(req.getParameter("planTypeId")));
		url.append("&tabAction=").append(StringUtil.checkVal(req.getParameter("eventQualify")));
		if (msg != null) url.append("&msg=").append(msg);
    	req.setAttribute(Constants.REDIRECT_URL, url.toString());
    	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    }
	
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
    public void retrieve(ActionRequest req) throws ActionException {
		log.debug("Starting PhysEventAction retrieve...");
		SMTSession ses = (SMTSession) req.getSession();
    	ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
    	PhysQualDataVO vo = (PhysQualDataVO) ses.getAttribute(PHYS_QUAL_DATA);
    	
    	// derived from req param
    	boolean useAltData = Convert.formatBoolean(StringUtil.checkVal(req.getParameter("useAltData")));
    	
    	// derived from req attribute
    	boolean useAltQualData = Convert.formatBoolean(StringUtil.checkVal(req.getAttribute("useAltQualData")));
    	
    	//load the qualData if we don't have it or if the surgeon changed
	    //if (vo != null || !vo.getSurgeonId().equals(req.getParameter("surgeonId"))) {
    	//THIS CONDITIONAL CHECK HAD TO BE REMOVED BECAUSE WE CAN'T TELL WHEN/IF THE
    	//SCS USAGE DATA OR SCS_START_DT FIELDS HAVE BEEN CHANGED ONCE THIS DATA IS 
    	//STORED IN THE SESSION - JM 07-22-09
			SMTActionInterface pqda = null;
			
			// If alternate qualifying data was requested, retrieve it.
			if (useAltData || useAltQualData || vo.getIsAltData()) {
				log.debug("Putting alternate qual data on session...");
				pqda = new PhysicianAltQualDataAction(actionInit);
				pqda.setDBConnection(dbConn);
				pqda.setAttributes(attributes);
				// If submitting the simpleData form, persist the data.
				// Otherwise just attempt to retrieve the data.
				pqda.retrieve(req);
				
			// Otherwise retrieve the business plan qualifying data.
			} else {
				log.debug("Putting business plan qual data on session...");
				pqda = new PhysicianQualDataAction(actionInit);
				pqda.setDBConnection(dbConn);
				pqda.setAttributes(attributes);
				pqda.retrieve(req);
			}
			
			vo = (PhysQualDataVO)req.getAttribute(PHYS_QUAL_DATA_VO);
			log.debug("Physician Name: " + vo.getSurgeonVO().getFirstName() + " " + vo.getSurgeonVO().getFirstName());
			log.debug("Physician profileId: " + vo.getSurgeonVO().getProfileId());
			ses.setAttribute(PHYS_QUAL_DATA, vo);
		//}
    	
    	//if we have the necessary qualData then load the events...
    	if (((PhysQualDataVO) ses.getAttribute(PHYS_QUAL_DATA)).isQualDataComplete()) {
    		log.debug("Qual data is complete...retrieving qualifying events and events attended.");
			String oldActionId = mod.getActionId();
			actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
			
			SMTActionInterface ai = new EventFacadeAction(actionInit);
			ai.setDBConnection(dbConn);
			ai.setAttributes(attributes);
			ai.retrieve(req);
			
			actionInit.setActionId(oldActionId);
			
			//Date+30 needed on JSP
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, 30);
			req.setAttribute("maxDt", cal.getTime());
			
			//Date+120 also needed on JSP to limit viewable events to 
			//a max of 4 months from today's date.
			cal.add(Calendar.DATE, 90);
			req.setAttribute("maxViewableDt", cal.getTime());
    	}
	}
	
	/* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
    public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	

}
