package com.depuysynthes.huddle;


import javax.servlet.http.Cookie;

import com.depuysynthesinst.events.CourseCalendar;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.event.EventFacadeAction;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: EventCalendar.java<p/>
 * <b>Description: 
 * Admin side: imports data from an Excel file into the Event's portlet defined by the administrator.
 * Public side: loads all the events tied to the porlet and filters them by Anatomy type.
 * </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 28, 2015
 ****************************************************************************/
public class EventCalendarAction extends CourseCalendar {

	public EventCalendarAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public EventCalendarAction(ActionInitVO arg0) {
		super(arg0);
	}


	/**
	 * retrieves a list of Events tied to this porlet.  Filters the list to the passed anatomy, if present. 
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);

		//hook for event signup; these would come from an email and the user must login first,
		//so we needed to keep the URLs short and redirect-able.
		if (req.hasParameter("reqParam_2") && "ADD".equalsIgnoreCase(req.getParameter("reqParam_1"))) {
			UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
			if (user != null) {
				req.setParameter("userSignup", "true");
				req.setParameter("profileId", user.getProfileId());
				req.setParameter("rsvpCodeText", req.getParameter("reqParam_2"));
				build(req);
			}
			return;
		}

		Cookie rppCook = req.getCookie(HuddleUtils.RPP_COOKIE);
		if (rppCook != null)
			req.setParameter("rpp", rppCook.getValue());
		
		req.setParameter("fmid",mod.getPageModuleId());
		//NOTE: page & start get picked up by SolrActionVO automatically, because we set "fmid"

		//call SolrAction 
		actionInit.setActionId((String)mod.getAttribute(SBModuleVO.ATTRIBUTE_2));
		SolrAction sa = new SolrAction(actionInit);
		sa.setAttributes(attributes);
		sa.setDBConnection(dbConn);
		sa.retrieve(req);

		//get the response object back from SolrAction
		mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		SolrResponseVO solrResp = (SolrResponseVO) mod.getActionData();
		log.error("**************** SOLR COUNT: " + solrResp.getTotalResponses());
		
		req.setParameter("fmid","");
	}



	/**
	 * Build gets called for creating iCal files (downloads) of the passed eventEntryId
	 */
	public void build(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);

		//for event RSVP signups
		if (req.hasParameter("userSignup")) {
			req.setAttribute(EventFacadeAction.STATUS_OVERRIDE, EventFacadeAction.STATUS_APPROVED);
		} else if (req.hasParameter("rptType")) { //iCal downloads - url depends on which page of the site they're on
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			if (page.getAliasName().contains("profile")) { //calendar pages are OK with the default, skip this block
				SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
				String url = site.getFullSiteAlias() + page.getFullPath() + "?pmid=" + mod.getPageModuleId() + "&eventEntryId=";
				req.setAttribute(EventFacadeAction.STATUS_OVERRIDE, url);
			}
		}


		actionInit.setActionId((String)mod.getAttribute(SBModuleVO.ATTRIBUTE_1));
		mod.setActionId(actionInit.getActionId());
		EventFacadeAction efa = new EventFacadeAction(actionInit);
		efa.setAttributes(attributes);
		efa.setDBConnection(dbConn);
		efa.build(req);
	}

}
