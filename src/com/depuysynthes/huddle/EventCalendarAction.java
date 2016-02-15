package com.depuysynthes.huddle;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.http.Cookie;

import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.common.SolrDocument;

import com.depuysynthes.huddle.solr.CalendarSolrIndexer;
import com.depuysynthesinst.events.CourseCalendar;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.mail.CalendarEventMessageVO;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.io.mail.CalendarEventMessageVO.Method;
import com.siliconmtn.io.mail.MessageVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.event.EventFacadeAction;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.admin.action.SBModuleAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.MessageSender;
import com.smt.sitebuilder.util.solr.SolrActionUtil;

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

	public EventCalendarAction(ActionInitVO arg0) {
		super(arg0);
	}

	@Override
	public void update(SMTServletRequest req) throws ActionException {
		if (!req.hasParameter("cPage")) {
			if (Convert.formatBoolean(req.hasParameter("isBatch"))) {
				req.setParameter("batchOnly", "true");
			}

			req.setParameter("attrib1Text",  req.getParameter("sbActionId"), true);
			super.update(req);
			
			if (!Convert.formatBoolean(req.hasParameter("isBatch"))) {
				req.setParameter("eventBypass", "true");
				SMTActionInterface sai = new EventFacadeAction(actionInit);
				sai.setDBConnection(dbConn);
				sai.setAttributes(attributes);
				sai.update(req);
			}

			if (Convert.formatBoolean(req.hasParameter("isBatch"))) {
				req.setParameter("manMod", "true", true);
				super.adminRedirect(req, attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE), (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH), req.getParameter(SBModuleAction.SB_ACTION_ID));
				//append to the above-created redirectURL
				StringBuilder redirect = new StringBuilder((String)req.getAttribute(Constants.REDIRECT_URL));
				redirect.append("&cPage=facade&facadeType=true");
				redirect.append("&actionName=").append(req.getParameter("actionName"));
			     req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
			     req.setAttribute(Constants.REDIRECT_URL, redirect.toString());
			}
		} else {
			updateEvent(req);
		}
	}
	
	
	/**
	 * Update the event item specified by the request object and make any 
	 * required changes to solr.
	 * @param req
	 * @throws ActionException
	 */
	private void updateEvent(SMTServletRequest req) throws ActionException {

		SMTActionInterface sai = new EventFacadeAction(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.update(req);
		
		int status = Convert.formatInteger(req.getParameter("statusFlg"));
		if (req.hasParameter("eventEntryId") && (
				status == EventFacadeAction.STATUS_APPROVED || 
				status == EventFacadeAction.STATUS_WAITLIST ||
				status == EventFacadeAction.STATUS_CANCELLED)) //we use cancelled as 'registration is closed'.  The event is still active 
		{
			
			// Only add approved events to solr.
			Set<String> events = new HashSet<>();
			events.add(req.getParameter("eventEntryId"));
			pushToSolr(events);
		} else if (req.hasParameter("eventEntryId")) {
			// This event is not approved so delete it from solr
			SolrActionUtil util = new SolrActionUtil(getAttributes());
			util.removeDocument(req.getParameter("eventEntryId"));
		}
	}

	@Override
	public void list(SMTServletRequest req) throws ActionException {
		super.list(req);
		
		if (req.hasParameter("facadeType")) {
			listEvent(req);
		}
	}
	

	/**
	 * Get any data from the event tables that is needed for the facade action
	 * @param req
	 * @throws ActionException
	 */
	private void listEvent(SMTServletRequest req) throws ActionException {
		SMTActionInterface sai = new EventFacadeAction(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.list(req);
		
		ModuleVO mod = (ModuleVO) getAttribute(AdminConstants.ADMIN_MODULE_DATA);
		
		// If the module data is still in the action data and the request is
		// for a facade page remove the actiond data
		if (mod.getActionData() instanceof SBModuleVO && Convert.formatBoolean(req.getParameter("facadeType")))
			mod.setActionData(null);
	}

	/**
	 * retrieves a list of Events tied to this porlet.  Filters the list to the passed anatomy, if present. 
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);

		Cookie rppCook = req.getCookie(HuddleUtils.RPP_COOKIE);
		if (rppCook != null)
			req.setParameter("rpp", rppCook.getValue());

		req.setParameter("fmid",mod.getPageModuleId());
		//NOTE: page & start get picked up by SolrActionVO automatically, because we set "fmid"
		
		//determine whether we're displaying the archives or not.  Set a startDate range query accordingly
		 //do not apply if we're looking at a specific event
		if (!req.hasParameter("reqParam_1")) {
			String dateFilter = "";
			if (req.hasParameter("archive")) {
				req.setParameter("sortField", "endDate_dt");
				req.setParameter("sortDirection", ORDER.desc.toString());
				dateFilter = "endDate_dt:[NOW-6MONTHS TO NOW]";
			} else {
				dateFilter = "endDate_dt:[NOW TO *]";
			}
			String[] fqs = req.getParameterValues("fq");
			if (fqs == null) fqs = new String[0];
			List<String> data = new ArrayList<>(Arrays.asList(fqs));
			data.add(dateFilter);
			req.setParameter("fq", data.toArray(new String[data.size()]), true);
		}

		//call SolrAction 
		actionInit.setActionId((String)mod.getAttribute(SBModuleVO.ATTRIBUTE_2));
		SolrAction sa = new SolrAction(actionInit);
		sa.setAttributes(attributes);
		sa.setDBConnection(dbConn);
		sa.retrieve(req);

		req.setParameter("fmid","");
	}


	/**
	 * Build gets called for creating iCal files (downloads) of the passed eventEntryId
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);

		if (req.hasParameter("rptType")) { //iCal downloads - url depends on which page of the site they're on
			actionInit.setActionId((String)mod.getAttribute(SBModuleVO.ATTRIBUTE_1));
			mod.setActionId(actionInit.getActionId());
			EventFacadeAction efa = new EventFacadeAction(actionInit);
			efa.setAttributes(attributes);
			efa.setDBConnection(dbConn);
			efa.build(req);

		} else if (req.hasParameter("remindMe")) {
			retrieve(req); //get this (single) event, from Solr
			mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
			SolrResponseVO solrResp = (SolrResponseVO) mod.getActionData();
			
			//send an email containing a calendar event - to be added to the recipient's Calendar (Outlook or other)
			if (solrResp != null && solrResp.getTotalResponses() == 1) {
				SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
				PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
				UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
				MessageVO msg = makeCalendarEvent(solrResp.getResultDocuments().get(0), site, page, user);
				new MessageSender(getAttributes(), dbConn).sendMessage(msg);
			} else {
				throw new ActionException("event not found");
			}
		}
	}


	private EmailMessageVO makeCalendarEvent(SolrDocument sd, SiteVO site, PageVO page, UserDataVO user) 
			throws ActionException {
		CalendarEventMessageVO msg = new CalendarEventMessageVO();
		try {
			Date startDt = (Date)sd.get("startDate_dt");
			if (startDt == null) throw new ActionException("no startDate");
			
			String url = site.getFullSiteAlias() +page.getFullPath() + "/" + getAttribute(Constants.QS_PATH) + sd.get("documentId");
			
			// Convert Solr's GMT to Local Time
			String dateFormat = "yyyy/MM/dd HH:mm:ss";
			SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			startDt = Convert.formatDate(dateFormat, sdf.format(startDt));
			//use a calendar obj to determine when registration opens
			Calendar cal = Calendar.getInstance();
			cal.setTime(startDt);
			cal.add(Calendar.DATE, HuddleUtils.EVENT_REGISTRATION_OPENS);
			
			msg.setFrom(site.getMainEmail());
			msg.setSubject("DSHuddle - Register for " + sd.get("title"));
			msg.addRecipients(user.getEmailAddress());
			msg.setTitle("DSHuddle - Registration opens for " + sd.get("title"));
			msg.setMethod(Method.REQUEST);
			msg.setDescription(getBodyMessage(false, sd, cal, url));
			msg.setHtmlDescription(getBodyMessage(true, sd, cal, url));
			msg.setStartDate(cal.getTime());
			msg.setEndDate(startDt);
			//msg.setLocation(url);
		} catch (Exception e) {
			throw new ActionException("could not build calendar event", e);
		}
		return msg;
	}
	
	private String getBodyMessage(boolean inHtml, SolrDocument sd, Calendar cal, String url) {
		String delim = (inHtml) ? "<br/>" : "\n";
		StringBuilder sb =new StringBuilder(150);
		sb.append("Registration opens for ").append(sd.get("title"));
		sb.append(" on ").append(Convert.formatDate(cal.getTime(), Convert.DATE_LONG));
		sb.append(delim).append(delim).append("Visit DSHuddle.com to register:").append(delim);
		sb.append(url);
		
		return sb.toString();
	}
	
	@Override
	protected void pushToSolr(Set<String> eventIds) {
		Properties props = new Properties();
		props.putAll(getAttributes());
		CalendarSolrIndexer indexer = new CalendarSolrIndexer(props);
		indexer.setDBConnection(dbConn);
		indexer.indexCertainItems(eventIds);
	}
	
	
	@Override
	public void delete(SMTServletRequest req) throws ActionException {
		if (!req.hasParameter("cPage")) {
			super.delete(req);
			pushToSolr(null);
		} else {
			deleteEvent(req);
		}
	}

	
	/**
	 * Prepare an event facade action to handle deleting an event item
	 * and remove any associated documents from solr
	 * @param req
	 * @throws ActionException
	 */
	private void deleteEvent(SMTServletRequest req) throws ActionException {
		SMTActionInterface sai = new EventFacadeAction(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.delete(req);
		
		if (req.hasParameter("eventEntryId")) {
			SolrActionUtil util = new SolrActionUtil(getAttributes());
			util.removeDocument(req.getParameter("eventEntryId"));
		} else if (req.hasParameter("eventTypeId")) {
			// If we deleted an entire group rebuild the index to be sure that
			// the index is up to date with the current events.
			pushToSolr(null);
		}
	}
}
