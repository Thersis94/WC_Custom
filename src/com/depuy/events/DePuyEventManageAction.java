package com.depuy.events;

// DePuy SB Libs
import com.depuy.events.PostcardInsert;
import com.depuy.events.PostcardSelect;
import com.depuy.events.vo.report.SigninReportVO;

// SMT Base libs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;

// SB Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.event.EventGroupAction;
import com.smt.sitebuilder.action.event.EventRSVPAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: DePuyEventManageAction.java</p>
 <p>public interface that supports the DePuy patient activation website...AKA events management</p>
 <p>Copyright: Copyright (c) 2000 - 2007 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 2.0
 @since Nov 19, 2007
 ***************************************************************************/

public class DePuyEventManageAction extends SimpleActionAdapter {

    /**
     * 
     */
    public DePuyEventManageAction() {
        super();
    }

    /**
     * @param arg0
     */
    public DePuyEventManageAction(ActionInitVO arg0) {
        super(arg0);
    }

    
    public void list(SMTServletRequest req) throws ActionException {
    	super.retrieve(req);    	
    }

    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
     */
    @Override
    public void build(SMTServletRequest req) throws ActionException {
    	log.debug("DePuyEventManageAction:build() starting...");
    	String ft = StringUtil.checkVal(req.getParameter(AdminConstants.FACADE_TYPE));
    	SMTActionInterface ee = null;
    	String oldInitId = actionInit.getActionId();
    	ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
    	actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
    	
    	if (ft.equals("rsvp")) {
    		EventRSVPAction er = new EventRSVPAction(this.actionInit);
	    	er.setAttributes(this.attributes);
	    	er.setDBConnection(dbConn);
	    	er.updateRSVP(req);
	    	er = null;
	    	
	    	//set redirect page (used for public site redirects only)
			StringBuilder redirectPg = new StringBuilder();
			redirectPg.append(req.getRequestURI()).append("?facadeType=rsvp&reqType=rsvpList");
			redirectPg.append("&eventEntryId=").append(req.getParameter("eventEntryId"));
			redirectPg.append("&eventDt=").append(req.getParameter("eventDt"));
			redirectPg.append("&eventCode=").append(req.getParameter("eventCode"));
			redirectPg.append("&eventType=").append(req.getParameter("eventType"));
			redirectPg.append("&eventNm=").append(req.getParameter("eventNm"));
			redirectPg.append("&msg=").append(req.getAttribute("message"));
			log.debug("nextPage=" + redirectPg);
			
			req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
			req.setAttribute(Constants.REDIRECT_URL, redirectPg.toString());
			
    	} else {
    		ee = new PostcardInsert(this.actionInit);
	    	ee.setAttributes(this.attributes);
	    	ee.setDBConnection(dbConn);
	    	ee.build(req);
    	}
    	log.debug("build complete");

    	// Setup the redirect.
    	actionInit.setActionId(oldInitId);
    }

    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
    public void retrieve(SMTServletRequest req) throws ActionException {
		String ft = req.getParameter(AdminConstants.FACADE_TYPE);
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		String facadeActionId = actionInit.getActionId();
		actionInit.setActionId((String) mod.getAttribute(ModuleVO.ATTRIBUTE_1));

		// Retrieve the postcard Data
		SMTActionInterface eg = null;
		Object data = null;

		if ("rsvp".equals(ft)) {
			eg = new EventRSVPAction(this.actionInit);
			eg.setAttributes(this.attributes);
			eg.setDBConnection(dbConn);
			eg.retrieve(req);
			log.info("Retrieved Event RSVP Data");
			data = req.getAttribute(EventRSVPAction.RETR_EVENTS);

			if (req.hasParameter("signin")) {
				// generate a sign-in sheet using the data returned from the
				// RSVPAction
				SigninReportVO rpt = new SigninReportVO(req);
				rpt.setData(data);
				req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
			}
		} else {
			// Retrieve the Group Data
			eg = new EventGroupAction(this.actionInit);
			eg.setAttributes(this.attributes);
			eg.setDBConnection(dbConn);
			eg.retrieve(req);
			log.info("Retrieved Group Data");

			// retrieve event/postcard data
			eg = new PostcardSelect(this.actionInit);
			eg.setAttributes(this.attributes);
			eg.setDBConnection(dbConn);
			eg.retrieve(req);
			log.info("Retrieved Postcard Data ");
			data = req.getAttribute(PostcardSelect.RETR_EVENTS);
		}

		// Store the retrieved data in the ModuleVO.actionData and replace
		// into the Map
		super.putModuleData(data);
		actionInit.setActionId(facadeActionId);
    }
}
