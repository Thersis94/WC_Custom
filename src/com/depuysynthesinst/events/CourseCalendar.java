package com.depuysynthesinst.events;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import com.depuysynthesinst.lms.FutureLeaderACGME;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.siliconmtn.util.parser.AnnotationParser;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.event.EventEntryAction;
import com.smt.sitebuilder.action.event.EventFacadeAction;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
import com.smt.sitebuilder.action.event.vo.EventGroupVO;
import com.smt.sitebuilder.action.event.vo.EventTypeVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: CourseCalendar.java<p/>
 * <b>Description: 
 * Admin side: imports data from an Excel file into the Event's portlet defined by the administrator.
 * Public side: loads all the events tied to the porlet and filters them by Anatomy type.
 * </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Aug 25, 2014
 ****************************************************************************/
public class CourseCalendar extends SimpleActionAdapter {

	public CourseCalendar() {
		super();
	}

	public CourseCalendar(ActionInitVO arg0) {
		super(arg0);
	}
	
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}

	@Override
	public void update(SMTServletRequest req) throws ActionException {
		if (!Convert.formatBoolean(req.getParameter("batchOnly")))
			super.update(req);
		
		if (req.getFile("xlsFile") != null || req.getFile("batchFile") != null)
			processUpload(req);
		
	}
	
	
	/**
	 * processes the file upload and imports each row as a new event to add to the 
	 * desired event calendar. 
	 * @param req
	 * @throws ActionException
	 */
	private void processUpload(SMTServletRequest req) throws ActionException {
		AnnotationParser parser;
		FilePartDataBean fpdb = req.getFile("xlsFile");
		if (fpdb == null) fpdb = req.getFile("batchFile");
		try {
			parser = new AnnotationParser(CourseCalendarVO.class, fpdb.getExtension());
		} catch(InvalidDataException e) {
			throw new ActionException("could not load import file", e);
		}
		try {
			//Gets the xls file from the request object, and passes it to the parser.
			//Parser then returns the list of populated beans
			Map<Class<?>, Collection<Object>> beans = parser.parseFile(fpdb, true);
			
			 UUIDGenerator uuid = new UUIDGenerator();
			ArrayList<Object> beanList = new ArrayList<>(beans.get(CourseCalendarVO.class));
			Set<String> eventIds = new HashSet<>(beanList.size());
			
			//Disable the db autocommit for the insert batch
			dbConn.setAutoCommit(false);
			
			EventEntryAction eventAction = new EventEntryAction();
			eventAction.setDBConnection(dbConn);
			
			for (Object o : beanList) {
				//set the eventTypeId for each
				CourseCalendarVO vo = (CourseCalendarVO) o;
				vo.setActionId(uuid.getUUID());
				eventIds.add(vo.getActionId());
				vo.setEventTypeId(req.getParameter("eventTypeId"));
				vo.setStatusFlg(EventFacadeAction.STATUS_APPROVED);
			}
			eventAction.importBeans(beanList, req.getParameter("attrib1Text"));
			
			//commit only after the entire import succeeds
			dbConn.commit();
			
			//push the new assets to Solr
			pushToSolr(eventIds);
			
		} catch (InvalidDataException | SQLException e) {
			log.error("could not process DSI calendar import", e);
		} finally {
			try {
				//restore autocommit state
				dbConn.setAutoCommit(true);
			} catch (SQLException e) {}
		}
	}
	
	/**
	 * retrieves a list of Events tied to this porlet.  Filters the list to the passed anatomy, if present. 
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);

		String anatomy = null;
		boolean showFilters = (page.getAliasName().equals("calendar")); //not needed on these pages/views;
		
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
		
		//if not on the calendar page, we'll need to filter the events by anatomy
		if (! page.getAliasName().equals("calendar") && ! page.getAliasName().equals("profile")) {
			SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
			anatomy = getAnatomyFromAlias(page, site);
			req.setParameter(EventEntryAction.REQ_SERVICE_OPT, anatomy);
		}
		
		Calendar cal = Calendar.getInstance();
		if (page.getAliasName().equals("profile"))
			cal.add(Calendar.DATE, -90);
		req.setParameter(EventEntryAction.REQ_START_DT, Convert.formatDate(cal.getTime(), Convert.DATE_SLASH_PATTERN));
		
		//load the Events
		actionInit.setActionId((String)mod.getAttribute(SBModuleVO.ATTRIBUTE_1));
		mod.setActionId(actionInit.getActionId());
		EventFacadeAction efa = new EventFacadeAction(actionInit);
		efa.setAttributes(attributes);
		efa.setDBConnection(dbConn);
		efa.retrieve(req);
		mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		EventGroupVO vo = (EventGroupVO) mod.getActionData();
		
		//prepare facets/filters
		if (showFilters) {
			if (!req.hasParameter("location"))
				prepareSpecialtyFacets(req, vo);
			
			req.setValidateInput(false);
			filterDataBySpecialty(req, vo);
			filterDataByLocation(req, vo);
			req.setValidateInput(true);
			
			if (req.hasParameter("location"))
				prepareSpecialtyFacets(req, vo);
			
			//locations listed are limited to only those containing events (after specialty filter is applied)
			prepareLocationFacets(req, vo);
			
			super.putModuleData(vo);
		}
	}
	
	/**
	 * prepare search filters to present to the user based on the data we're displaying
	 * @param req
	 * @param grpVo
	 */
	private void prepareSpecialtyFacets(SMTServletRequest req, EventGroupVO grpVo) {
		//one for specialties, put on the request by Type
		for (EventTypeVO typeVo : grpVo.getTypes().values()) {
			Map<String, Integer> specialties = new TreeMap<String, Integer>();
			boolean isFutureLdrs = "FUTURE".equals(typeVo.getTypeName());
			for (EventEntryVO vo : typeVo.getEvents()) {
				String specs = StringUtil.checkVal(vo.getServiceText(), "Other");
				for (String spec : specs.split(",")) {
					spec = StringUtil.checkVal(spec).trim();
					if (isFutureLdrs) spec = StringUtil.checkVal(FutureLeaderACGME.getNameFromCode(spec), spec);
					if (specialties.containsKey(spec)) {
						specialties.put(spec, specialties.get(spec)+1);
					} else {
						specialties.put(spec, 1);
					}
				}
			}
			log.debug("loaded " + specialties.size() + " specialty filters");
			req.setAttribute("facet_spec_" + typeVo.getTypeName(), specialties);
		}
	}
	
	private void prepareLocationFacets(SMTServletRequest req, EventGroupVO grpVo) {
		//one for Location (city & state), put on the request by Type
		for (EventTypeVO typeVo : grpVo.getTypes().values()) {
			Map<String, Integer> locations = new TreeMap<String, Integer>();
			for (EventEntryVO vo : typeVo.getEvents()) {
				String state = StringUtil.checkVal(vo.getStateCode());
				String locn = StringUtil.checkVal(vo.getCityName());
				if (state.length() > 0) locn += ", " + state;
				
				if (locn.length() == 2) locn = "Other";
				if (locations.containsKey(locn)) {
					locations.put(locn, locations.get(locn)+1);
				} else {
					locations.put(locn, 1);
				}
			}
			log.debug("loaded " + locations.size() + " location filters facet_locn_" + typeVo.getTypeName());
			req.setAttribute("facet_locn_" + typeVo.getTypeName(), locations);
		}
	}
	
	
	/**
	 * filter the list of events being returned to the browser to only those matching 
	 * certain locations.  A "location" here is a String: "city, st"
	 * @param req
	 * @param vo
	 */
	private void filterDataByLocation(SMTServletRequest req, EventGroupVO grpVo) {
		if (!req.hasParameter("location")) return;
		List<String> filters = Arrays.asList(req.getParameter("location").split("~"));
		if (filters == null || filters.size() == 0) return;
		
		for (EventTypeVO typeVo : grpVo.getTypes().values()) {
			List<EventEntryVO> data = new ArrayList<EventEntryVO>();
			for (EventEntryVO vo : typeVo.getEvents()) {
				//check each event and only include those matching our filters
				String locn = StringUtil.checkVal(vo.getCityName()) + ", " + StringUtil.checkVal(vo.getStateCode());
				if (filters.contains(locn))
					data.add(vo);
			}
			log.debug("removed " + (typeVo.getEvents().size() - data.size()) + " events by location, now " + data.size());
			typeVo.setEvents(data);
		}
	}
	
	/**
	 * filter the list of events being returned to the browser to only those matching 
	* certain specialties
	 * @param req
	 * @param vo
	 */
	private void filterDataBySpecialty(SMTServletRequest req, EventGroupVO grpVo) {
		if (!req.hasParameter("specialty")) return;
		List<String> filters = Arrays.asList(req.getParameter("specialty").split("~"));
		if (filters == null || filters.size() == 0) return;
		//log.debug(filters);
		
		for (EventTypeVO typeVo : grpVo.getTypes().values()) {
			List<EventEntryVO> data = new ArrayList<EventEntryVO>();
			boolean isFutureLdrs = "FUTURE".equals(typeVo.getTypeName());
			for (EventEntryVO vo : typeVo.getEvents()) {
				boolean addIt = false;
				//check each event and only include those matching our filters
				String spec = StringUtil.checkVal(vo.getServiceText());
				if (spec == null || spec.length() == 0) spec = "Other";
				outer:
				for (String s : spec.split(",")) {
					if (isFutureLdrs) s = StringUtil.checkVal(FutureLeaderACGME.getNameFromCode(s), s);
					log.debug("spec=" + s);
					for (String f : filters) {
						if (s.contains(f)) {
							addIt = true;
							break outer;
						}
					}
				}
				if (!addIt) continue;
				data.add(vo);
				//log.debug("added " + vo.getServiceText());
			}
			log.debug("removed " + (typeVo.getEvents().size() - data.size()) + " events by specialty, now " + data.size());
			typeVo.setEvents(data);
		}
	}
	
	/**
	 * cast the URL alias to a anotomical section (as used in the Events lists)
	 * most of these align, but a couple needed massaging.
	 * @param alias
	 * @return
	 */
	private String getAnatomyFromAlias(PageVO page, SiteVO site) {
		//on the main site we don't filter
		if (site.getAliasPathName() == null && page.isDefaultPage()) return "";
		String alias = page.getAliasName().toLowerCase();
		
		if ("veterinary".equals(site.getAliasPathName())) return "Vet"; //vet section
		else if ("outpatient-education".equals(site.getAliasPathName())) return "Outpatient Education"; //Outpatient Ed. section
		else if ("bundled-payments".equals(site.getAliasPathName())) return "Bundled Payments"; // Bundled Payments section
		else if ("nurse-education".equals(site.getAliasPathName())) return "Nurse Education"; //nursing section
		else if ("futureleaders".equals(site.getAliasPathName())) return FutureLeaderACGME.getCodeFromAlias(alias);
		else if (alias.equals("chest-wall")) return "Chest Wall";
		else if (alias.indexOf("-") > 0) return StringUtil.capitalizePhrase(alias.replace("-", " & ")); //Foot & Ankle, Hand & Wrist
		
		return StringUtil.capitalize(alias);
	}
	
	
	
	/**
	 * Build gets called for creating iCal files (downloads) of the passed eventEntryId
	 */
	@Override
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
	
	
	protected void pushToSolr(Set<String> eventIds) {
		Properties props = new Properties();
		props.putAll(getAttributes());
		CourseCalendarSolrIndexer indexer = new CourseCalendarSolrIndexer(props); 
		indexer.setDBConnection(dbConn);
		indexer.indexCertainItems(eventIds);
	}
}
