package com.depuy.events_v2;

// DePuy SB Libs
import java.util.Calendar;

import com.depuy.events.vo.report.SigninReportVO;
import com.depuy.events_v2.ReportBuilder.ReportType;
// SMT Base libs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
// SB Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.event.EventGroupAction;
import com.smt.sitebuilder.action.event.EventRSVPAction;
import com.smt.sitebuilder.action.event.vo.EventRsvpVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 * <p>
 * <b>Title</b>: DePuyEventManageAction.java
 * </p>
 * <p>
 * public interface that supports the DePuy patient activation website...AKA
 * events management
 * </p>
 * <p>
 * Copyright: Copyright (c) 2000 - 2013 SMT, All Rights Reserved
 * </p>
 * <p>
 * Company: Silicon Mountain Technologies
 * </p>
 * 
 * @author James McKain
 * @version 1.0
 * @since Nov 19, 2013
 ***************************************************************************/

public class DePuyEventManageActionV2 extends SimpleActionAdapter {

	/**
	 * 
	 */
	public DePuyEventManageActionV2() {
		super();
	}

	/**
	 * @param arg0
	 */
	public DePuyEventManageActionV2(ActionInitVO arg0) {
		super(arg0);
	}

	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.
	 * http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("DePuyEventManageAction:build() starting...");
		String ft = StringUtil.checkVal(req.getParameter(AdminConstants.FACADE_TYPE));
		SMTActionInterface ee = null;
		String oldInitId = actionInit.getActionId();
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		actionInit.setActionId((String) mod.getAttribute(ModuleVO.ATTRIBUTE_1));

		switch (ft) {
			case "rsvp":
				EventRSVPAction er = new EventRSVPAction(this.actionInit);
				er.setAttributes(this.attributes);
				er.setDBConnection(dbConn);
				
				EventRsvpVO vo = new EventRsvpVO(req);

				Calendar reminderDt = Calendar.getInstance();
				reminderDt.setTime(Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("eventDt")));
				reminderDt.add(Calendar.DATE, -7); // set reminder for 7 days before the event
				vo.setReminderDt(reminderDt.getTime());
				
				er.updateRSVP(vo);
				er = null;

				// set redirect page (used for public site redirects only)
				StringBuilder redirectPg = new StringBuilder();
				redirectPg.append(req.getRequestURI()).append(
						"?facadeType=rsvp&reqType=closeModal");
				redirectPg.append("&printerFriendlyTheme=true&hidePf=true");
				redirectPg.append("&msg=").append(req.getAttribute("message"));
				log.debug("nextPage=" + redirectPg);

				req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
				req.setAttribute(Constants.REDIRECT_URL, redirectPg.toString());

				break;

			case "report": 
				ReportType type = null;
				try {
					type = ReportType.valueOf(req.getParameter("rptType"));
				} catch (Exception e) {
					throw new ActionException("unknown report type", e);
				}
				
				// retrieve event/postcard first
				if (type.requiresSeminar()) { //we don't need seminar data for certain reports
					ee = new PostcardSelectV2(this.actionInit);
					ee.setAttributes(this.attributes);
					ee.setDBConnection(dbConn);
					ee.retrieve(req);
					log.debug("Retrieved Postcard Data ");
				}

				//the Object returned here (getActionData) could be a List<VO>, or a single VO.  Let the ReportBuilder worry about it!
				mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);

				ReportBuilder rb = new ReportBuilder(this.actionInit);
				rb.setAttributes(attributes);
				rb.setDBConnection(dbConn);
				rb.generateReport(req, mod.getActionData());

				break;
			case "customReport":
				log.info("Managing custom report");
				ee = new CustomReportAction(this.actionInit);
				ee.setAttributes(this.attributes);
				ee.setDBConnection(dbConn);
				ee.build(req);
				break;
			case "delete":
				ee = new PostcardDeleteV2( this.actionInit );
				ee.setAttributes( this.attributes );
				ee.setDBConnection(dbConn);
				ee.build(req);
				break;

			case "rsvpFileImport":
				//For batch uploading
				EventRSVPAction era = new EventRSVPAction(this.actionInit);
				era.setAttributes( this.attributes );
				era.setDBConnection(dbConn);
				era.importFile(req);
				era = null;
				break;
				
			default:
				ee = new PostcardInsertV2(this.actionInit);
				ee.setAttributes(this.attributes);
				ee.setDBConnection(dbConn);
				ee.build(req);
				break;
		}
		log.debug("build complete");

		// Setup the redirect.
		actionInit.setActionId(oldInitId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn
	 * .http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		String oldInitId = actionInit.getActionId();
		actionInit.setActionId((String) mod.getAttribute(ModuleVO.ATTRIBUTE_1));

		// Retrieve the postcard Data
		SMTActionInterface eg = null;
		Object data = null;

		if (req.hasParameter(AdminConstants.FACADE_TYPE)) {
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
			eg = new PostcardSelectV2(this.actionInit);
			eg.setAttributes(this.attributes);
			eg.setDBConnection(dbConn);
			eg.retrieve(req);
			log.info("Retrieved Postcard Data ");
			data = ((ModuleVO) attributes.get(Constants.MODULE_DATA)).getActionData();
		}

		// Store the retrieved data in the ModuleVO.actionData and replace
		// into the Map
		super.putModuleData(data);
		actionInit.setActionId(oldInitId);
	}
}
