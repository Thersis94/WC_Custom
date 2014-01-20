package com.depuy.events_v2;

// JDK 1.7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// J2EE 1.4.0 Libs
import javax.servlet.http.HttpSession;

//wc-depuy libs
import com.depuy.events.vo.CoopAdVO;

// SMT BaseLibs
import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.depuy.events_v2.vo.PersonVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.security.UserDataVO;

// SB Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;


/****************************************************************************
 * <b>Title</b>: EventPostcardSelect.java<p/>
 * <b>Description: Manages EventActions for the SB Sites</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2005<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 1.0
 * @since Mar 15, 2006
 ****************************************************************************/
public class PostcardSelectV2 extends SBActionAdapter {
	
	public enum ReqType {
		//create-wizard screens
		getstarted, selectType, eventInfo, leads,
		//timeline screens
		summary, status, promote, manage, dayof, postseminar, 
		//list pages
		active /** implied default **/, completed, 
		//reports
		reportForm,  rsvpBreakdown
	}
	
	/*
	 * Enums for the different Comparator sorting order.
	 * these are intentionally lower-case to match the incoming query string parameter.
	 */
	public enum SortOrder {
		type		(TypeComparator.class),
		product	(ProductComparator.class),
		rsvp		(RSVPComparator.class), 
		date		(DateComparator.class), 
		status	(StatusComparator.class), 
		owner	(OwnerComparator.class);
		
		private Comparator<DePuyEventSeminarVO> comp  = null;
		public Comparator<DePuyEventSeminarVO> getComparator() { return comp; }
		private SortOrder(Class<? extends Comparator<DePuyEventSeminarVO>> comp) {
			try {
				this.comp = comp.newInstance();
			} catch (InstantiationException e) {
				log.error("could not load Comparator", e);
			} catch (IllegalAccessException e) {
				log.error("could not access Comparator", e);
			}
		}
	};
	
	public PostcardSelectV2() {
		super();
	}

	/**
	 * @param arg0
	 */
	public PostcardSelectV2(ActionInitVO arg0) {
		super(arg0);
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		HttpSession ses = req.getSession();
		String eventPostcardId = StringUtil.checkVal(req.getParameter("eventPostcardId"));
		ReqType reqType = null;
		try {
			reqType = ReqType.valueOf(req.getParameter("reqType"));
		} catch (Exception e) {
			//ignore these, default reqType (list active sems) will be used
		}
		
		UserDataVO user = (UserDataVO) ses.getAttribute(Constants.USER_DATA);
		SBUserRole roles = (SBUserRole) ses.getAttribute(Constants.ROLE_DATA);
		Integer roleId = (roles != null) ? roles.getRoleLevel() : SecurityController.PUBLIC_ROLE_LEVEL;
		String profileId = (roleId < SecurityController.ADMIN_ROLE_LEVEL) ? user.getProfileId() : null;
	
		Object data = null;
		try {
			if (eventPostcardId.length() > 0) {
				//load one postcard in it's entirety
				data = loadOneSeminar(eventPostcardId, actionInit.getActionId(), reqType, profileId);
				
			} else {
				//load the list of postcards (screen# 1)
				data = loadSeminarList(actionInit.getActionId(), reqType, profileId, req.getParameter("sortType"));
			}
			
		} catch (SQLException sqle) {
			log.error("could not retrieve seminar data", sqle);
		}
		
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		mod.setActionData(data);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 *
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		HttpSession ses = req.getSession();
		String eventPostcardId = StringUtil.checkVal(req.getParameter("eventPostcardId"));
		String reqType = StringUtil.checkVal(req.getParameter("reqType"));
		SBUserRole roles = (SBUserRole) ses.getAttribute(Constants.ROLE_DATA);
		Integer roleId = (roles != null) ? roles.getRoleLevel() : SecurityController.PUBLIC_ROLE_LEVEL;
		UserDataVO user = (UserDataVO) ses.getAttribute(Constants.USER_DATA);
		List<String> profileIds = new ArrayList<String>();
		boolean listCities = (reqType.equals("scopeLeads") || reqType.equals("leads"));
		Integer rptType = Convert.formatInteger(req.getParameter("rptType"));
		Date startDt = Convert.formatDate(req.getParameter("startDt"));
		Date endDt = Convert.formatDate(req.getParameter("endDt"));
		boolean showLeadsByCity = Convert.formatBoolean(req.getParameter("byCity"));
				
		//status: get current/upcoming events, or get expired events older than 1 day
		String status = "start_dt > getDate()-1 ";
		if (reqType.equals("showDels")) status = "start_dt < getDate()-1 ";
		else if (reqType.equals("scopeLeads")) status = "1=0 ";
		else if (eventPostcardId.length() > 0) status = "1=1 ";
		
		StringBuilder sql = new StringBuilder();
        sql.append("select a.event_postcard_id, a.profile_id, a.content_no, ");
        sql.append("a.status_flg, a.logo_img, a.logo_consent_txt, a.authorization_txt, ");
        sql.append("a.presenter_bio_txt, a.venue_txt, a.attrib_1_txt, a.attrib_2_txt, ");
        sql.append("a.attrib_3_txt, a.attrib_4_txt, a.presenter_opt_in_flg, ");
        sql.append("a.presenter_experience_txt, a.presenter_email_txt, a.presenter_address_txt, b.order_no, ");
        sql.append("c.event_entry_id, c.event_nm, c.location_desc, c.contact_nm, ");
        sql.append("cast(c.event_desc as varchar(500)) as event_desc, f.action_id, ");
        sql.append("c.short_desc, c.start_dt, c.end_dt, c.address_txt, c.status_flg as event_status, ");
        sql.append("c.address2_txt, c.city_nm, c.state_cd, c.zip_cd, c.rsvp_code_txt, ");
        sql.append("sum(e.guests_no) as rsvp_total, d.display_nm_flg, d.clinic_nm, ");
        sql.append("g.coop_ad_id, g.status_flg as coop_ad_status_flg, g.surgeon_status_flg, ");
        sql.append("g.surgeon_nm, g.surgeon_title_txt, g.surgeon_email_txt, g.surgeon_img_url, ");
        sql.append("g.clinic_nm, g.clinic_address_txt, g.clinic_phone_txt, g.clinic_hours_txt, g.surg_experience_txt, ");
        sql.append("c.event_type_id, d.feat_prod_flg, d.focus_mrkt_flg, d.display_srvc_flg, f.group_cd, d.attendance_cnt, ");
        sql.append("i.newspaper1_txt as 'radio_nm', i.newspaper1_phone_no as 'radio_ph', i.coop_ad_id as 'radio_id', ");
        sql.append("i.newspaper2_txt as 'radio_contact_nm', i.newspaper2_phone_no as 'radio_contact_email', ");
        sql.append("i.run_dates_txt as 'radio_deadline', c.file_path_txt, h.type_nm, h.desc_txt ");
        sql.append("from ");
        sql.append("event_postcard a inner join ");
        sql.append("event_postcard_assoc b on a.event_postcard_id=b.event_postcard_id ");
        sql.append("inner join event_entry c on b.event_entry_id=c.event_entry_id ");
        sql.append("inner join event_type h on c.event_type_id=h.event_type_id ");
        sql.append("inner join event_group f on h.action_id=f.action_id ");
        sql.append("inner join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
        sql.append("depuy_event_entry d on c.event_entry_id=d.event_entry_id ");
        sql.append("left join event_rsvp e on c.event_entry_id=e.event_entry_id and e.rsvp_status_flg=1 ");
        sql.append("left join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
        sql.append("DEPUY_EVENT_COOP_AD g on a.event_postcard_id=g.event_postcard_id and (g.ad_type_txt is null or g.ad_type_txt != 'radio') ");
        sql.append("left join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
        sql.append("DEPUY_EVENT_COOP_AD i on a.event_postcard_id=i.event_postcard_id and i.ad_type_txt = 'radio' ");
        sql.append("where c.action_id=? ");
        
        if (rptType == LeadsDataTool.EVENT_ROLLUP_REPORT || 
        		rptType == LeadsDataTool.RSVP_BREAKDOWN_REPORT) {  //for event rollup report
        	sql.append(" and c.start_dt >= ? and c.start_dt <= ? and a.status_flg is not null ");
        } else {
        	String types = "1,2";
        	if (reqType.equals("report") && (rptType.equals(LeadsDataTool.POSTCARD_SUMMARY_PULL) 
        			|| rptType.equals(LeadsDataTool.LEAD_AGING_REPORT))) 
        		types = "1,2,3";
        	
        	//exclude not-submitted or deleted events, but don't exclude canceled DURING the cancellation request, so we can send the email notifs.
        	sql.append("and (a.status_flg is null or a.status_flg in (").append(types).append(")) ");
        	sql.append("and ").append(status);
        }
		if (eventPostcardId.length() > 0) sql.append("and a.event_postcard_id=? ");
		
		if (roleId  == SecurityController.PUBLIC_REGISTERED_LEVEL) {
				sql.append("and a.profile_id=? "); //scope users to their submissions only
		} else if (roleId == SecurityController.PUBLIC_ROLE_LEVEL) {
			//public user, can only see one event, for one purpose (surgeon ad approval)
			sql.append("and a.event_postcard_id=? ");
		}
		
		if (eventEntryId.length() > 0) sql.append("and c.event_entry_id=? ");
		
		sql.append("group by a.event_postcard_id, a.profile_id, a.content_no, ");
        sql.append("a.status_flg, a.logo_img, a.logo_consent_txt, a.authorization_txt, ");
        sql.append("a.presenter_bio_txt, a.venue_txt, a.attrib_1_txt, a.attrib_2_txt, ");
        sql.append("a.attrib_3_txt, a.attrib_4_txt, a.presenter_opt_in_flg, ");
        sql.append("a.presenter_experience_txt, a.presenter_email_txt, a.presenter_address_txt, b.order_no, ");
        sql.append("c.event_entry_id, c.event_nm, c.location_desc, c.contact_nm, ");
        sql.append("cast(c.event_desc as varchar(500)), h.type_nm, h.desc_txt, f.group_cd, c.status_flg, ");
        sql.append("f.action_id, c.short_desc, c.start_dt, c.end_dt, c.address_txt, ");
        sql.append("c.address2_txt, c.city_nm, c.state_cd, c.zip_cd, c.rsvp_code_txt, ");
        sql.append("d.display_nm_flg, d.clinic_nm, g.coop_ad_id, g.status_flg, g.surgeon_status_flg, ");
        sql.append("g.surgeon_nm, g.surgeon_title_txt, g.surgeon_email_txt, g.surgeon_img_url, ");
        sql.append("g.clinic_nm, g.clinic_address_txt, g.clinic_phone_txt, g.clinic_hours_txt, g.surg_experience_txt, ");
        sql.append("c.event_type_id, d.feat_prod_flg, d.focus_mrkt_flg, i.newspaper1_txt, ");
        sql.append("i.newspaper1_phone_no, i.coop_ad_id, i.newspaper2_txt, i.newspaper2_phone_no, i.run_dates_txt, ");
        sql.append("c.file_path_txt, d.display_srvc_flg, d.attendance_cnt order by a.event_postcard_id desc");
		log.info("Event Postcard SQL: " + sql + " actionId=" + mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		log.debug("postcard=" + eventPostcardId + " event=" + eventEntryId);
		DePuyEventPostcardVO vo = new DePuyEventPostcardVO();
		PreparedStatement ps = null;
        List<DePuyEventPostcardVO> data = new ArrayList<DePuyEventPostcardVO>();
        List<DePuyEventEntryVO> eventList = new ArrayList<DePuyEventEntryVO>();
		try {
			int i=0;
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(++i, (String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
			if (rptType == LeadsDataTool.EVENT_ROLLUP_REPORT || 
	        		rptType == LeadsDataTool.RSVP_BREAKDOWN_REPORT) {  //for event rollup report
				ps.setDate(++i, Convert.formatSQLDate(startDt));
				ps.setDate(++i, Convert.formatSQLDate(endDt));
			}
			if (eventPostcardId.length() > 0) ps.setString(++i, eventPostcardId);
			if (roleId  == SecurityController.PUBLIC_REGISTERED_LEVEL) { 
				ps.setString(++i, user.getProfileId());
			}  else if (roleId == SecurityController.PUBLIC_ROLE_LEVEL) {
				//surgeon ad approval screen.  return no results if this condition is not meetable
				ps.setString(++i, eventPostcardId);
			}
			
			if (eventEntryId.length() > 0) ps.setString(++i, eventEntryId);
			
			ResultSet rs = ps.executeQuery();
            String lastPostcardId = "";
			while (rs.next()) {
				//this RS returns multiple rows for each postcard (for multiple events).
				if (!lastPostcardId.equals(rs.getString("event_postcard_id"))) {
					log.debug("lastPostcardId = " + lastPostcardId);
					if (!lastPostcardId.equals("")) {
						vo.setDePuyEvents(eventList);
						eventList = new ArrayList<DePuyEventEntryVO>();
						
						
						vo.setLeadSources(this.getEventLeadSources(vo.getEventPostcardId(),
							req.getParameter("state_cd"), site.getCountryCode(), listCities, 
							user, roleId, vo.getProductName(), showLeadsByCity));
						log.debug("done lead sources");

						data.add(vo);
		                vo = null;
					}
					
					lastPostcardId = rs.getString("event_postcard_id");
					vo = new DePuyEventPostcardVO();
					vo.setData(rs);
					vo.setLanguage((String)mod.getAttribute(ModuleVO.ATTRIBUTE_2));
					
					//get the coopAds for the RollUpReport
					if (rptType == LeadsDataTool.EVENT_ROLLUP_REPORT) { 
						CoopAdsAction caa = new CoopAdsAction(actionInit);
						caa.setDBConnection(dbConn);
						caa.setAttributes(attributes);
						vo.setCoopAd(caa.retrieve(rs.getString("coop_ad_id"), lastPostcardId));
					}
				}
				
				//add this owner to our list of Profiles to retrieve
				log.debug("add profileId to the list");
				if (!profileIds.contains(vo.getProfileId())) 
					profileIds.add(vo.getProfileId());

				//create the new event and add it to the list for this postcard
				log.debug("creating a new EventVO");
				DePuyEventEntryVO eventVo = new DePuyEventEntryVO(rs);
				eventVo.setStatusFlg(rs.getInt("event_status"));
				eventVo.setRsvpTotal(rs.getInt("rsvp_total"));
				eventVo.setEventGroupId(rs.getString("action_id"));
				eventVo.setAddtlPostcards(this.getAddtlPostcards(eventVo.getActionId()));
				eventList.add(eventVo);
				eventVo = null;
				log.debug("doneLoop");

			}
			
			//add the final postcard to the list
			if (profileIds.size() > 0) {
				vo.setDePuyEvents(eventList);
				vo.setLeadSources(this.getEventLeadSources(vo.getEventPostcardId(),
					req.getParameter("state_cd"), site.getCountryCode(), listCities, 
					user, roleId, vo.getProductName(), showLeadsByCity));
				
				log.debug("done lead sources");
				data.add(vo);
			}
			
		} catch (SQLException sqle) {
			log.error("EventPostcardException", sqle);
			throw new ActionException("Error Getting Event Action: " + sqle.getMessage());
		} finally {
        	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
		}
		log.debug("found " + data.size() + " postcards");
		
		
		//get profiles for all the owners!
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		Map<String, UserDataVO> users = new HashMap<String, UserDataVO>();
		try {
			users = pm.searchProfileMap(dbConn, profileIds);
		} catch (Exception e) {
			log.error("error retrieving Profiles", e);
		}
		pm = null;
		
		//attached the owner's Profile to each postcardVO and 
		// re-sort the elements by earliest event date (for display)
		Iterator<DePuyEventPostcardVO> iter = data.iterator();
		DePuyEventPostcardVO pc = null;
		TreeMap<Date,DePuyEventPostcardVO> sorted = new TreeMap<Date,DePuyEventPostcardVO>(new PostcardComparator());
		while (iter.hasNext()) {
			pc = iter.next();
			try {
				pc.setOwner(users.get(pc.getProfileId()));
			} catch (Exception e) {
				log.error("could not find owner for postcard=" + pc.getEventPostcardId());
			}
			
			Date key = pc.getEarliestEventDate();
			if (sorted.containsKey(key)) {  //this is a bug-fix for when multiple events share a common date
				Long l = key.getTime() + sorted.size();
				key.setTime(l);
			}
			sorted.put(key, pc);
			pc = null;
		}
		log.debug("sortedSize=" + sorted.size());
		//replace the master List with the updated one (containing owner profiles)
		data.clear();
		data.addAll(sorted.values());
		
		
		//SCOPE LEADS ADD-IN.  All of the above code/query is skipped because RS.size=0
		if (reqType.equals("scopeLeads")) {
			vo.setLeadSources(this.getEventLeadSources(vo.getEventPostcardId(),
					req.getParameter("state_cd"), site.getCountryCode(), false, 
					user, roleId, req.getParameter("groupCd"), showLeadsByCity));
			data.add(vo);
		
		//get coopAds if requested
		} else if (reqType.equals("coopAdsForm") || reqType.equals("coopAdsReview") 
				|| rptType == LeadsDataTool.POSTCARD_SUMMARY_PULL) {
			SMTActionInterface sai = new CoopAdsAction(actionInit);
			sai.setAttributes(attributes);
			sai.setDBConnection(dbConn);
			sai.retrieve(req);
		}
		
		//Add the data to the request object for processing by Facade
		log.debug("data size=" + data.size());
		req.setAttribute(PostcardSelectV2.RETR_EVENTS, data);
		
		//call reporting/data-tools if requested
		if (reqType.equals("report"))
			this.formatReport(req);
	}



	
	private List<DePuyEventLeadSourceVO> getEventLeadSources(String eventPostcardId, 
				String stateCd, String countryCd, boolean listCities, UserDataVO user, Integer roleId,
				String productNm, boolean listByCity) {
		List<DePuyEventLeadSourceVO> data = new ArrayList<DePuyEventLeadSourceVO>();
		
		StringBuilder sql = new StringBuilder();
		sql.append("select sum(est_leads_no), b.state_nm, a.state_cd from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("depuy_event_leads_datasource a inner join ");
		sql.append("state b on a.state_cd=b.state_cd and b.country_cd=? ");
		sql.append("where a.event_postcard_id=? ");
		if (stateCd != null) sql.append("and a.state_cd=? ");
		sql.append("group by state_nm, a.state_cd order by b.state_nm");
		log.info("EventLeadSource SQL: " + sql);
				
		//edt used to pull the cities list/checkboxes for leads in the given state
		LeadsDataTool edt = new LeadsDataTool(actionInit);
		edt.setAttributes(this.attributes);
		edt.setDBConnection(dbConn);
		
		DePuyEventLeadSourceVO vo = null;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, countryCd);
			ps.setString(2, eventPostcardId);
			if (stateCd != null) ps.setString(3, stateCd);
			ResultSet rs = ps.executeQuery();
            
			while (rs.next()) {
				vo = new DePuyEventLeadSourceVO();
				vo.setLeadsCnt(rs.getInt(1));
				vo.setStateName(rs.getString(2));
				vo.setProductName(productNm);
				vo.setStateCode(rs.getString(3));
				
				//for display layout; we grab all the cities and display a grid w/checkboxes
				if (listCities) {
					vo.setLeadCities(edt.scopeLeads(eventPostcardId, stateCd, productNm, user, roleId, listByCity));
					vo.setLeadZips(edt.getSelected(eventPostcardId, productNm, stateCd, "zip"));
				}
				
                data.add(vo);
                vo = null;
			}
			
			//if no leads are saved still get a list of cities for this state to display (for the 'add' page)
			if (data.size() == 0 && stateCd != null) {
				log.debug("getting default list for " + stateCd);
				vo = new DePuyEventLeadSourceVO();
				vo.setStateCode(stateCd);
				vo.setStateName(stateCd);
				vo.setProductName(productNm);
				vo.setLeadCities(edt.scopeLeads(null, stateCd, productNm, user, roleId, listByCity));
				
				//lookup state name
				PreparedStatement ps2 = null;
				try {
					sql = new StringBuilder("select state_nm from state where state_cd=? and country_cd=?");
					ps2 = dbConn.prepareStatement(sql.toString());
					ps2.setString(1, stateCd);
					ps2.setString(2, countryCd);
					ResultSet rs2 = ps2.executeQuery();
					if (rs2.next()) 
						vo.setStateName(rs2.getString(1));
					
				} catch (Exception e) {
					log.error("couldn't query state named " + stateCd, e);
				} finally {
					if (ps2 != null) {
			        	try {
			        		ps2.close();
			        	} catch(Exception e) {}
		        	}
				}
				
				data.add(vo);
				
			}
			
		} catch (SQLException sqle) {
			log.error("getEventLeadSources, eventPostcardId=" + eventPostcardId, sqle);
		} finally {
        	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
		}
		
		log.debug("finished getEventLeadSources, size=" + data.size());
		return data;
	}


	
	private void formatReport(SMTServletRequest req) throws ActionException {
		log.debug("starting postcard Reporting");
		AbstractSBReportVO rpt = null;
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		List<?> data = (List<?>)req.getAttribute(PostcardSelectV2.RETR_EVENTS);
		Integer rptType = Convert.formatInteger(req.getParameter("rptType"));
		DePuyEventPostcardVO vo = new DePuyEventPostcardVO();
		
		//generate the leads data
		boolean pullLeads = (rptType == LeadsDataTool.MAILING_LIST_PULL || 
							 rptType == LeadsDataTool.MAILING_LIST_BY_DATE_PULL || 
							 rptType == LeadsDataTool.EMAIL_ADDRESS_PULL ||
							 rptType == LeadsDataTool.LEAD_AGING_REPORT);
		LeadsDataTool ldt = new LeadsDataTool(actionInit);
		ldt.setDBConnection(dbConn);
		ldt.setAttributes(this.attributes);
		
		try {
			vo = (DePuyEventPostcardVO) data.get(0);
			if (pullLeads) vo.setLeadsData(ldt.pullLeads(vo.getEventPostcardId(), vo.getProductName(), rptType, req.getParameter("startDt")));
		} catch (Exception e) {
			//log.error("selecting leads data", e);
		}
		
		
		if (rptType == LeadsDataTool.MAILING_LIST_PULL || 
				rptType == LeadsDataTool.MAILING_LIST_BY_DATE_PULL) {
			rpt = new EventPostalLeadsReportVO();
			rpt.setData(vo);
			
		} else if (rptType == LeadsDataTool.EMAIL_ADDRESS_PULL) {
			rpt = new EventEmailListReportVO();
			rpt.setData(vo);
			
		} else if (rptType == LeadsDataTool.POSTCARD_SUMMARY_PULL) {
			StringBuilder basePath = new StringBuilder();
			basePath.append("http://").append(site.getSiteAlias()).append("/");
			basePath.append(StringUtil.replace((String)getAttribute("binaryDirectory"),"/",""));
			basePath.append(getAttribute("orgAlias")).append(site.getOrganizationId());
			basePath.append("/").append(site.getSiteId());
			
			if (StringUtil.checkVal(vo.getLogoImage()).length() > 0)
				vo.setLogoImage(basePath.toString() + "/logos/" + vo.getLogoImage());

			if (StringUtil.checkVal(vo.getAuthorizationText()).length() > 0)
				vo.setAuthorizationText(basePath.toString() + "/auth/" + vo.getAuthorizationText());
			
			if (StringUtil.checkVal(vo.getPresenterBioText()).length() > 0)
				vo.setPresenterBioText(basePath.toString() + "/auth/" + vo.getPresenterBioText());
			
			if (StringUtil.checkVal(vo.getPcAttribute4()).length() > 0)
				vo.setPcAttribute4(basePath.toString() + "/auth/" + vo.getPcAttribute4());
			
			if (req.getAttribute(CoopAdsAction.RETR_ADS) != null) {
				CoopAdVO ad = (CoopAdVO) req.getAttribute(CoopAdsAction.RETR_ADS);
				if (StringUtil.checkVal(ad.getAdFileUrl()).length() > 0)
			    	ad.setAdFileUrl(basePath.toString() + "/ads/" + ad.getAdFileUrl());
				if (StringUtil.checkVal(ad.getSurgeonImageUrl()).length() > 0)
			    	ad.setSurgeonImageUrl(basePath.toString() + "/ads/" + ad.getSurgeonImageUrl());
				vo.setCoopAd(ad);
			}
			rpt = new PostcardSummaryReportVO();
			rpt.setData(vo);
			
		} else if (rptType == LeadsDataTool.LOCATOR_REPORT) {
			rpt = new LocatorReportVO();
			DePuyEventEntryVO event = (DePuyEventEntryVO) vo.getEventById(req.getParameter("eventEntryId"));
			event.setAttribute("groupCd", vo.getGroupCd());
			event.setAttribute("radius", req.getParameter("radius"));
			rpt.setData(event);
			
		} else if (rptType == LeadsDataTool.RSVP_SUMMARY_REPORT) {
			rpt = new RsvpSummaryReportVO();
			Iterator<?> iter = data.iterator();
			DePuyEventPostcardVO pCard = null;
			DePuyEventEntryVO event = null;
			List<DePuyEventEntryVO> eventsList = new ArrayList<DePuyEventEntryVO>();
			EventRSVPAction rsvpAction = new EventRSVPAction(this.actionInit);
			rsvpAction.setAttributes(this.attributes);
			rsvpAction.setDBConnection(dbConn);
			while (iter.hasNext()) {
				//call the RSVP action for summations by event
				pCard = (DePuyEventPostcardVO) iter.next();
				List<DePuyEventEntryVO> events = pCard.getDePuyEvents();
				Iterator<DePuyEventEntryVO> eIter = events.iterator();
				
				//for each event (on the postcard)
				while (eIter.hasNext()) {
					event = eIter.next();
					event.setContactName(pCard.getOwner().getFirstName() + " " + pCard.getOwner().getLastName());
					event.setRsvpSummary(rsvpAction.rsvpSummary(event.getActionId()));
					eventsList.add(event);
				}
			}
			rpt.setData(eventsList);
			
		} else if (rptType == LeadsDataTool.RSVP_BREAKDOWN_REPORT) {
			log.debug("starting rsvp breakdown");
			rpt = new RsvpBreakdownReportVO();
			Iterator<?> iter = data.iterator();
			DePuyEventPostcardVO pCard = null;
			DePuyEventEntryVO event = null;
			List<DePuyEventEntryVO> eventsList = new ArrayList<DePuyEventEntryVO>();
			EventRSVPAction rsvpAction = new EventRSVPAction(this.actionInit);
			rsvpAction.setAttributes(this.attributes);
			rsvpAction.setDBConnection(dbConn);
			while (iter.hasNext()) {
				//call the RSVP action for breakdowns by event
				pCard = (DePuyEventPostcardVO) iter.next();
				List<DePuyEventEntryVO> events = pCard.getDePuyEvents();
				Iterator<DePuyEventEntryVO> eIter = events.iterator();
				
				//for each event (on the postcard)
				while (eIter.hasNext()) {
					event = eIter.next();
					event.setContactName(pCard.getOwner().getFirstName() + " " + pCard.getOwner().getLastName());
					event.setRsvpSummary(rsvpAction.rsvpBreakdown(event.getActionId()));
					eventsList.add(event);
				}
			}
			rpt.setData(eventsList);
			
		} else if (rptType == LeadsDataTool.EVENT_ROLLUP_REPORT) {
			rpt = new EventRollupReportVO();
			rpt.setData(data);
			
		} else if (rptType == LeadsDataTool.LEAD_AGING_REPORT) {
			rpt = new LeadAgingReportVO();
			rpt.setData(vo);
			
		}
		log.debug("finished postcardReportVO");
		
		//notify site admin of this pull for reports that impact security
		if (rptType != LeadsDataTool.LOCATOR_REPORT && 
				rptType != LeadsDataTool.EVENT_ROLLUP_REPORT && 
				rptType != LeadsDataTool.RSVP_SUMMARY_REPORT &&
				rptType != LeadsDataTool.RSVP_BREAKDOWN_REPORT &&
				rptType != LeadsDataTool.POSTCARD_SUMMARY_PULL &&
				rptType != LeadsDataTool.LEAD_AGING_REPORT) {
			AbstractPostcardEmailer epe = AbstractPostcardEmailer.newInstance(vo.getGroupCd(), attributes, dbConn);
			epe.notifyAdminOfListPull(vo, user, rptType, site);
		}
		
		//put the report on the request object for PageBuilderServlet to stream back
		if (rpt != null) {
			req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
			req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
		}
	}
	*/
	
	
	/**
	 * runs a simple PIVOT query to colate the data server-side instead of WC-side.
	 * This query only grabs the data used to display the Active and Completed Seminars pages.
	 * @param profileId
	 * @param string 
	 * @return
	 * @throws SQLException
	 */
	private List<DePuyEventSeminarVO> loadSeminarList(String actionGroupId, ReqType reqType, String profileId, String sortType) throws SQLException {
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		Set<String> profileIds = new HashSet<String>();
		List<DePuyEventSeminarVO> data = new ArrayList<DePuyEventSeminarVO>();
//		Integer rptType = Convert.formatInteger(req.getParameter("rptType"), 0);
		
		StringBuilder sql = new StringBuilder();
		sql.append("select distinct event_entry_id, RSVP_CODE_TXT, start_dt, type_nm, profile_id, ");
		sql.append("surgeon_nm, event_nm, city_nm, state_cd, status_flg, event_postcard_id, ");
		sql.append("rsvp_no, [4] as 'hip', [5] as 'knee', [6] as 'shoulder' ");  //in a PIVOT, we're turning the data (values) into column headings.  hence the square brackets.
		//sql.append(" ");
		sql.append("from (select e.event_entry_id, e.RSVP_CODE_TXT, e.start_dt, et.type_nm, ep.event_postcard_id, ");
		sql.append("ep.PROFILE_ID, s.surgeon_nm, e.event_nm, e.city_nm, e.state_cd, ");
		sql.append("ep.status_flg, lxr.JOINT_ID, COUNT(rsvp.EVENT_RSVP_ID) as 'rsvp_no' ");
		sql.append("from EVENT_ENTRY e ");
		sql.append("inner join EVENT_TYPE et on e.EVENT_TYPE_ID=et.EVENT_TYPE_ID ");
		sql.append("inner join EVENT_GROUP eg on et.ACTION_ID=eg.ACTION_ID ");
		sql.append("inner join SB_ACTION sb on eg.ACTION_ID=sb.ACTION_ID ");
		sql.append("inner join EVENT_POSTCARD_ASSOC epa on e.EVENT_ENTRY_ID=epa.EVENT_ENTRY_ID ");
		sql.append("inner join EVENT_POSTCARD ep on epa.EVENT_POSTCARD_ID=ep.EVENT_POSTCARD_ID ");
		sql.append("left outer join EVENT_RSVP rsvp on e.EVENT_ENTRY_ID=rsvp.EVENT_ENTRY_ID ");
		sql.append("left outer join ").append(customDb).append("DEPUY_EVENT_SURGEON s on ep.EVENT_POSTCARD_ID=s.EVENT_POSTCARD_ID ");
		sql.append("left outer join ").append(customDb).append("DEPUY_EVENT_SPECIALTY_XR lxr on ep.EVENT_POSTCARD_ID=lxr.EVENT_POSTCARD_ID ");
		//--conditionally grab only the events this non-admin is affiliated with -- 
		if (profileId != null) {
			sql.append("left outer join ").append(customDb).append("DEPUY_EVENT_PERSON_XR pxr on ep.EVENT_POSTCARD_ID=pxr.EVENT_POSTCARD_ID ");
			sql.append("where (pxr.PROFILE_ID=? or ep.PROFILE_ID=?) and sb.action_group_id=? ");
		} else {
			sql.append("where sb.action_group_id=? and ep.status_flg != 0");  //exclude any that haven't been submitted yet.
		}
		if (ReqType.completed == reqType) {
			sql.append("and e.start_dt < getDate()-2");
		} else {
			sql.append("and e.start_dt >= getDate()-2");
		}
		sql.append("group by e.event_entry_id, ep.event_postcard_id, e.RSVP_CODE_TXT, e.start_dt, et.type_nm, ep.PROFILE_ID, ");
		sql.append("e.event_nm, s.surgeon_nm, e.city_nm, e.state_cd, ep.status_flg, lxr.JOINT_ID ");
		sql.append(") baseQry ");
		sql.append("pivot (count(joint_id) for joint_id in ([4],[5],[6])) as pvtQry "); //PIVOT is an implicit group-by
		log.debug(sql);
//		 if (rptType == LeadsDataTool.EVENT_ROLLUP_REPORT || 
//		        		rptType == LeadsDataTool.RSVP_BREAKDOWN_REPORT) {  //for event rollup report
//		        	sql.append(" and c.start_dt >= ? and c.start_dt <= ? and a.status_flg is not null ");
//		        } else {
//		        	String types = "1,2";
//		        	if (reqType.equals("report") && (rptType.equals(LeadsDataTool.POSTCARD_SUMMARY_PULL) 
//		        			|| rptType.equals(LeadsDataTool.LEAD_AGING_REPORT))) 
//		        		types = "1,2,3";
//		        	
//		        	//exclude not-submitted or deleted events, but don't exclude canceled DURING the cancellation request, so we can send the email notifs.
//		        	sql.append("and (a.status_flg is null or a.status_flg in (").append(types).append(")) ");
//		        	sql.append("and ").append(status);
//		        }
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			if (profileId != null) {
				ps.setString(1, profileId);
				ps.setString(2, profileId);
				ps.setString(3, actionGroupId);
			} else {
				ps.setString(1, actionGroupId);
			}
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				//set aside profileIds for the event owners, these will need to be retrieved from ProfileManager
				profileIds.add(rs.getString("profile_id"));
				data.add(new DePuyEventSeminarVO().populateFromListRS(rs));
			}
		} finally { 
			try { ps.close(); } catch (Exception e) { }
		}
		
		//retrieve the profiles for the names we need to display
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		Map<String, UserDataVO> profiles = null;
		try {
			profiles = pm.searchProfileMap(dbConn, new ArrayList<String>(profileIds));
			
			//loop the data and attach the profiles where they belong
			for (DePuyEventSeminarVO vo : data) {
				if (profiles.containsKey(vo.getProfileId()))
						vo.setOwner(profiles.get(vo.getProfileId()));
			}
			
		} catch (DatabaseException e) {
			//this is not fatal, so we're not going to throw it.  Only one data element will be missing from the page.
			log.error("could not load user profiles " + e.getMessage());
		} catch (NullPointerException npe) {
			log.error("could not attach user profiles " + npe.getMessage());
		}
		
		//If we have a sortType, ensure we sort the data.
		if(sortType != null && sortType.length() > 0 && data.size() > 1)
			data = sortData(data, sortType);
		
		log.debug("loaded " + data.size() + " Seminars");
		return data;
	}
	
	
	/**
	 * Sort Method that switches on a enum of Types for the different Comparators.
	 * @param data
	 * @param sortType
	 * @return
	 */
	private List<DePuyEventSeminarVO> sortData(List<DePuyEventSeminarVO> data, String sortType) {
		if (sortType == null || sortType.length() == 0) sortType = "rsvp";
		try {
			Collections.sort(data, SortOrder.valueOf(sortType).getComparator());
		//} catch (IllegalArgumentException iae) {
			//malformed enum passed, default to rsvp (highest->lowest)
		//	Collections.sort(data, SortOrder.rsvp.getComparator());
		} catch (Exception e) {
			log.debug("could not sort Seminars", e);
		}
		log.debug("data sorted");
		return data;
	}

	/**
	 * loads a single postcard, all of it!
	 * @param eventPostcardId
	 * @param actionGroupId
	 * @param reqType
	 * @param profileId
	 * @return
	 * @throws SQLException
	 */
	private DePuyEventSeminarVO loadOneSeminar(String eventPostcardId, String actionGroupId, 
			ReqType reqType, String profileId) throws SQLException {
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		Set<String> profileIds = new HashSet<String>();
		DePuyEventSeminarVO vo = null;
		
		StringBuilder sql = new StringBuilder();
		sql.append("select *, ep.profile_id as owner_profile_id, pxr.profile_id as person_profile_id, ep.status_flg as pc_status_flg  ");
		sql.append("from EVENT_ENTRY e ");
		sql.append("inner join EVENT_TYPE et on e.EVENT_TYPE_ID=et.EVENT_TYPE_ID ");
		sql.append("inner join EVENT_GROUP eg on et.ACTION_ID=eg.ACTION_ID ");
		sql.append("inner join SB_ACTION sb on eg.ACTION_ID=sb.ACTION_ID ");
		sql.append("inner join EVENT_POSTCARD_ASSOC epa on e.EVENT_ENTRY_ID=epa.EVENT_ENTRY_ID ");
		sql.append("inner join EVENT_POSTCARD ep on epa.EVENT_POSTCARD_ID=ep.EVENT_POSTCARD_ID ");
		sql.append("left outer join ").append(customDb).append("DEPUY_EVENT_SURGEON s on ep.EVENT_POSTCARD_ID=s.EVENT_POSTCARD_ID ");
		sql.append("left outer join ").append(customDb).append("DEPUY_EVENT_SPECIALTY_XR lxr on ep.EVENT_POSTCARD_ID=lxr.EVENT_POSTCARD_ID ");
		sql.append("left outer join ").append(customDb).append("DEPUY_EVENT_PERSON_XR pxr on ep.EVENT_POSTCARD_ID=pxr.EVENT_POSTCARD_ID ");
		sql.append("where sb.action_group_id=? and ep.event_postcard_id=? ");
		//--conditionally grab only the events this non-admin is affiliated with -- 
		if (profileId != null) {
			sql.append("and (pxr.PROFILE_ID=? or ep.PROFILE_ID=?) ");
		}
		log.debug(sql + "|" + actionGroupId + "|" + eventPostcardId + "|" + profileId);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, actionGroupId);
			ps.setString(2, eventPostcardId);
			if (profileId != null) {
				ps.setString(3, profileId);
				ps.setString(4, profileId);
			}
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (vo == null) {
					vo = new DePuyEventSeminarVO(rs);
					vo.setProfileId(rs.getString("owner_profile_id"));
					profileIds.add(vo.getProfileId());
				}
				//REP & TGM will need to be retrieved from ProfileManager
				PersonVO p = new PersonVO(rs.getString("postcard_role_cd"), rs.getString("person_profile_id"));
				vo.addPerson(p);
				profileIds.add(rs.getString("person_profile_id"));
				
				//add the joint, duplicates are handled by the Set
				vo.addJoint(rs.getString("joint_id"));
				
			}
		} finally { 
			try { ps.close(); } catch (Exception e) { }
		}
		
		//retrieve the profiles for the names we need to display
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		Map<String, UserDataVO> profiles = null;
		try {
			profiles = pm.searchProfileMap(dbConn, new ArrayList<String>(profileIds));
			
			//profile for the owner
			if (profiles.containsKey(vo.getProfileId()))
				vo.setOwner(profiles.get(vo.getProfileId()));
			
			//assign profiles to the TGMs and REPs
			for (PersonVO p : vo.getPeople()) {
				if (profiles.containsKey(p.getProfileId())) {
					p.setData(profiles.get(p.getProfileId()).getDataMap());
				}
			}
			
		} catch (DatabaseException e) {
			//this is not fatal, so we're not going to throw it.  Only one data element will be missing from the page.
			log.error("could not load user profiles " + e.getMessage());
		} catch (NullPointerException npe) {
			log.error("could not attach user profiles " + npe.getMessage());
		}
		
		//load CoopAds (newspaper & radio)
		if (loadCoopAds(reqType)) {
			try {
				loadCoopAds(vo);
			} catch (ActionException e) {
				log.error("could not load CoopAds for seminar " + eventPostcardId, e);
			}
		}

		return vo;
	}
	
	/**
	 * helper to isolate the logic of whether or not we need to load the Ads for the given request
	 * @param reqType
	 * @return
	 */
	private boolean loadCoopAds(ReqType reqType) {
		return (ReqType.reportForm != reqType);
	}
	
	private void loadCoopAds(DePuyEventSeminarVO vo) throws ActionException {
		CoopAdsActionV2 caa = new CoopAdsActionV2(actionInit);
		caa.setAttributes(attributes);
		caa.setDBConnection(dbConn);
		List<CoopAdVO> ads = caa.retrieve(null, vo.getEventPostcardId());
		for (CoopAdVO ad : ads) {
			if ("radio".equalsIgnoreCase(ad.getAdType())) {
				vo.setRadioAd(ad);
			} else {
				vo.setNewspaperAd(ad);
			}
		}
		return;
	}
	
	/*
	 * simple Date Comparator that returns the INVERSE of natural date ordering
	 * postcards are listed latest->earliest (not naturally, earliest->latest)
	 */
	public class PostcardComparator implements Comparator<Date> {
		public int compare(Date o1, Date o2) {
			if (o1.before(o2)) return 1;
			else if (o1.after(o2)) return -1;
			else if (o1.equals(o2)) return 0;
			else return -1;
		}
		public int compare(String o1, String o2) {
			return -1;
		}
		
	}
	
	/*
	 * Simple String Comparator that sorts based on the Seminars Joint Label
	 */
	public class ProductComparator implements Comparator<DePuyEventSeminarVO> {

		@Override
		public int compare(DePuyEventSeminarVO o1, DePuyEventSeminarVO o2) {
			log.debug("comparing by product");
			return o1.getJointLabel().compareTo(o2.getJointLabel());
		}
		
	}
	
	/*
	 * Simple String Comparator that sorts based on the Events Joint RSVP Code
	 */
	public class RSVPComparator implements Comparator<DePuyEventSeminarVO> {

		@Override
		public int compare(DePuyEventSeminarVO o1, DePuyEventSeminarVO o2) {
			log.debug("comparing rsvp codes");
			return o1.getRSVPCodes().compareTo(o2.getRSVPCodes());
		}
		
	}
	
	/*
	 * Simple Date Comparator that sorts based on the Events Start Date
	 */
	public class DateComparator implements Comparator<DePuyEventSeminarVO> {

		@Override
		public int compare(DePuyEventSeminarVO o1, DePuyEventSeminarVO o2) {
			Date d1 = o1.getEvents().get(0).getStartDate();
			Date d2 = o2.getEvents().get(0).getStartDate();
			if (d1.before(d2)) return 1;
			else if (d1.after(d2)) return -1;
			else if (d1.equals(d2)) return 0;
			else return -1;
		}
		
	}
	
	/*
	 * Simple String Comparator that sorts based on the Events Type Code
	 */
	public class TypeComparator implements Comparator<DePuyEventSeminarVO> {

		@Override
		public int compare(DePuyEventSeminarVO o1, DePuyEventSeminarVO o2) {
			return o1.getEvents().get(0).getEventTypeCd().compareTo(o2.getEvents().get(0).getEventTypeCd());

		}
		
	}
	
	/*
	 * Simple String Comparator that sorts based on the Events Status Flag
	 */
	public class StatusComparator implements Comparator<DePuyEventSeminarVO> {

		@Override
		public int compare(DePuyEventSeminarVO o1, DePuyEventSeminarVO o2) {
			int s1 = o1.getEvents().get(0).getStatusFlg();
			int s2 = o2.getEvents().get(0).getStatusFlg();
			if (s1 > s2) return 1;
			else if (s1 < s2) return -1;
			else if (s1 ==s2) return 0;
			else return -1;
		}
		
	}
	
	/*
	 * Simple String Comparator that sorts based on the Seminars Owners Full Name
	 */
	public class OwnerComparator implements Comparator<DePuyEventSeminarVO> {

		@Override
		public int compare(DePuyEventSeminarVO o1, DePuyEventSeminarVO o2) {
			return o1.getOwner().getFullName().compareTo(o2.getOwner().getFullName());

		}
		
	}
}
