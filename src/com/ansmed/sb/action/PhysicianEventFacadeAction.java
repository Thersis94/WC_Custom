package com.ansmed.sb.action;

// JDK 1.6
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.http.HttpSession;

// SB 2.0
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.event.EventFacadeAction;
import com.smt.sitebuilder.common.constants.Constants;

//SMT Base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/*****************************************************************************
<p><b>Title</b>: PhysicianEventFacadeAction.java</p>
<p>Description: <b/></p>
<p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author David Bargerhuff
@version 1.0
@since Oct 14, 2009
Last Updated:
 ***************************************************************************/
public class PhysicianEventFacadeAction extends SimpleActionAdapter {

	public static final String PHYS_QUAL_DATA = "physQualData";
	public static final String PHYS_QUAL_DATA_VO = "physQualDataVo";
	public static final String PHYS_ALT_QUAL_DATA = "physAltQualData";
	
	/**
	 * 
	 */
	public PhysicianEventFacadeAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public PhysicianEventFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
    public void build(SMTServletRequest req) throws ActionException {
		log.debug("Starting PhysicianEventFacadeAction build action");
		String action = StringUtil.checkVal(req.getParameter("tabAction"));

		// Build the base redir url
		StringBuffer url = new StringBuffer();
		url.append(req.getRequestURI()).append("?");
		url.append("fullName=").append(StringUtil.checkVal(req.getParameter("fullName")));
		url.append("&profileId=").append(req.getParameter("profileId"));
		url.append("&surgeonId=").append(req.getParameter("surgeonId"));
		
		// Execute the action
    	SMTActionInterface sai = null;
		
		if (action.equalsIgnoreCase("insertAlt")) {
			// insert the new or updated alternate qualifying data.
			sai = new PhysicianAltQualDataAction(actionInit);
			sai.setDBConnection(dbConn);
			sai.setAttributes(attributes);
			sai.build(req);
			
    	}  else if (action.equalsIgnoreCase("updatePlan")) {
			// update the plan scs start date and/or specialty data.
			sai = new PhysicianQualDataAction(actionInit);
			sai.setDBConnection(dbConn);
			sai.setAttributes(attributes);
			sai.build(req);
			
    	} else if (action.equalsIgnoreCase("eventQualify")) {
    		sai = new PhysicianEventAction(actionInit);
    		sai.setDBConnection(dbConn);
    		sai.setAttributes(attributes);
    		sai.build(req);
    		
    		// ...so we return to the same view
    		url.append("&tabAction=eventQualify");
    		
    	} 
		
		// Redirect
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
		
    }
	
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
    public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("Starting PhysEventFacadeAction retrieve...");
		
		HttpSession ses = (HttpSession) req.getSession();
    	PhysQualDataVO vo = (PhysQualDataVO) ses.getAttribute(PHYS_QUAL_DATA);
    	
    	String action = StringUtil.checkVal(req.getParameter("tabAction"));
    	
		SMTActionInterface sai = null;
		
		if (action.equalsIgnoreCase("editAltData")) {
			log.debug("action = editAltData");
			// retrieve the alternate qualifying data for editing
			sai = new PhysicianAltQualDataAction(actionInit);
			sai.setDBConnection(dbConn);
			sai.setAttributes(attributes);
			sai.retrieve(req);
			vo = (PhysQualDataVO)req.getAttribute(PHYS_QUAL_DATA_VO);
			req.setAttribute(PHYS_ALT_QUAL_DATA, vo);
			
		} else if (action.equalsIgnoreCase("eventQualify")) {
			log.debug("action = eventQualify");
			sai = new PhysicianEventAction(actionInit);
			sai.setDBConnection(dbConn);
			sai.setAttributes(attributes);
			sai.retrieve(req);
			
		} else {
						
			/*
			 * Per Ryan C. 10-23-2009
			 * If TM navs away from Events tab and returns, TM should be taken
			 * to the Events qualification/nomination view using the qual data
			 * source type (plan data or alternate data) that the TM used most 
			 * recently for the physician.  Added code to check for the most recent
			 * nomination pending approval and to grab the qual data source type
			 * used for that nomination. If the TM is changing sources, then we
			 * override this behavior.
			 */
			
			boolean changeQualSrc = Convert.formatBoolean(StringUtil.checkVal(req.getParameter("changeQualSrc")));
			log.debug("changeQualSrc: " + changeQualSrc);
			
			if (changeQualSrc) {
				log.debug("Changing qual src.");
				// retrieve both sets of qualifying data for the event_index view.
				// retrieve qualifying data
				sai = new PhysicianQualDataAction(actionInit);
				sai.setDBConnection(dbConn);
				sai.setAttributes(attributes);
				sai.retrieve(req);
				vo = (PhysQualDataVO)req.getAttribute(PHYS_QUAL_DATA_VO);
				ses.setAttribute(PHYS_QUAL_DATA, vo);
				// remove the attribute from req because attribute name
				// will be used by the action returning 'alt' qual data.
				req.removeAttribute(PHYS_QUAL_DATA_VO);
				
				// retrieve alternate qualifying data
				sai = null;
				sai = new PhysicianAltQualDataAction(actionInit);
				sai.setDBConnection(dbConn);
				sai.setAttributes(attributes);
				sai.retrieve(req);
				vo = (PhysQualDataVO)req.getAttribute(PHYS_QUAL_DATA_VO);
				ses.setAttribute(PHYS_ALT_QUAL_DATA, vo);
				req.removeAttribute(PHYS_QUAL_DATA_VO);
				
			} else {
				log.debug("Not changing qual src.  Attempting to retrieve most recent src used.");
				// Check to see if there is a recent nomination pending and retrieve
				// the qualification source type (planData or altData).
				String qualSource = retrieveMostRecentQualSource(req);
				
				// If there is a recent nomination, set the appropriate flags 
				// and set the tabAction to 'eventQualify' so that we go straight
				// to the event qualification view.
				if(qualSource != null && qualSource.length() > 0) {
					log.debug("Qual source is: " + qualSource);
					// reset the 'useAltData' attribute to drive qual data retrieval
					// when PhysicianEventAction is called.
					if (qualSource.equalsIgnoreCase("altData")) {
						req.setAttribute("useAltQualData", "true");
					}
					
					req.setParameter("tabRedirect", "eventQualify");
					
					// retrieve events
					sai = new PhysicianEventAction(actionInit);
					sai.setDBConnection(dbConn);
					sai.setAttributes(attributes);
					sai.retrieve(req);
				} else {
					log.debug("Couldn't find most recent src, using default action.");
					// retrieve both sets of qualifying data for the event_index view.
					// retrieve qualifying data
					sai = new PhysicianQualDataAction(actionInit);
					sai.setDBConnection(dbConn);
					sai.setAttributes(attributes);
					sai.retrieve(req);
					vo = (PhysQualDataVO)req.getAttribute(PHYS_QUAL_DATA_VO);
					ses.setAttribute(PHYS_QUAL_DATA, vo);
					// remove the attribute from req because attribute name
					// will be used by the action returning 'alt' qual data.
					req.removeAttribute(PHYS_QUAL_DATA_VO);
					
					// retrieve alternate qualifying data
					sai = null;
					sai = new PhysicianAltQualDataAction(actionInit);
					sai.setDBConnection(dbConn);
					sai.setAttributes(attributes);
					sai.retrieve(req);
					vo = (PhysQualDataVO)req.getAttribute(PHYS_QUAL_DATA_VO);
					ses.setAttribute(PHYS_ALT_QUAL_DATA, vo);
					req.removeAttribute(PHYS_QUAL_DATA_VO);
				}
			}
		}
	}
	
	/* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
    public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	/**
	 * Returns a String designating the data source used for the most recent
	 * nomination operation performed for this physician.
	 * @param req
	 * @return
	 */
	private String retrieveMostRecentQualSource(SMTServletRequest req) {
		
		final String schema = (String) this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		
		StringBuffer sql = new StringBuffer();
		
		sql.append("select event_qual_src, create_dt from ").append(schema);
		sql.append("ans_event_type_approval where surgeon_id = ? ");
		sql.append("and event_status_id = ? ");
		sql.append("order by create_dt desc ");
		
		log.debug("most recent qual src sql: " + sql.toString() + " | " + surgeonId);
		
		PreparedStatement ps = null;
		String dataType = null;
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, surgeonId);
			ps.setInt(2, Convert.formatInteger(EventFacadeAction.STATUS_PENDING));
			
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				dataType = StringUtil.checkVal(rs.getString(1));
			}
			
		} catch(SQLException sqle) {
			log.error("Error retrieving 'previous operation' data type for physician.", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		return dataType;
	}

}
