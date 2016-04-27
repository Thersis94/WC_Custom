package com.depuysynthes.huddle.solr;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.SolrInputDocument;

import com.depuysynthes.huddle.HuddleUtils;
import com.depuysynthesinst.events.CourseCalendarSolrIndexer;
import com.siliconmtn.common.html.state.USStateList;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: CalendarSolrIndexer.java<p/>
 * <b>Description: adds the courses from the EVENT_ENTRY table to the Solr index.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 28, 2015
 ****************************************************************************/
public class CalendarSolrIndexer extends CourseCalendarSolrIndexer {

	protected Map<Object, Object> states;
	private static final int MIN_ROLE_LVL = SecurityController.PUBLIC_REGISTERED_LEVEL;
	
	/**
	 * @param config
	 */
	public CalendarSolrIndexer(Properties config) {
		super(config);
		super.organizationId = "DPY_SYN_HUDDLE";

		states = new HashMap<>();
		//put together a state list for lookups from stateCode
		for(Map.Entry<Object, Object> entry : new USStateList().getStateList().entrySet())
		    states.put(entry.getValue(), entry.getKey());
	}
	
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTAbstractIndex#getIndexType()
	 */
	@Override
	public String getIndexType() {
		return HuddleUtils.IndexType.COURSE_CAL.toString();
	}
	
	
	/**
	 * push the list of pased VOs into Solr.
	 */
	protected void indexEvents(CloudSolrClient server, List<EventEntryVO> data) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		List<SolrInputDocument> docs = new ArrayList<>(data.size());
		
		for (EventEntryVO vo : data) {
			SolrInputDocument doc = new SolrInputDocument();
			try {
				doc.setField(SearchDocumentHandler.INDEX_TYPE, getIndexType());
				doc.setField(SearchDocumentHandler.ORGANIZATION, vo.getOrganizationId());
				doc.setField(SearchDocumentHandler.LANGUAGE, "en");
				doc.setField(SearchDocumentHandler.ROLE, MIN_ROLE_LVL);
				doc.setField(SearchDocumentHandler.DOCUMENT_URL, vo.getActionUrl());
				doc.setField(SearchDocumentHandler.DOCUMENT_ID, vo.getActionId());
				doc.setField(SearchDocumentHandler.TITLE, vo.getEventName());
				doc.setField(SearchDocumentHandler.SUMMARY, vo.getEventDesc());
				doc.setField(SearchDocumentHandler.AUTHOR, vo.getContactName());
				doc.setField(SearchDocumentHandler.UPDATE_DATE, df.format(vo.getLastUpdate()));
				doc.setField(SearchDocumentHandler.START_DATE + "_dt", df.format(vo.getStartDate()));
				doc.setField(SearchDocumentHandler.END_DATE + "_dt", df.format(vo.getEndDate()));
				doc.setField(SearchDocumentHandler.CONTENTS, StringUtil.getToString(vo));
				doc.setField(SearchDocumentHandler.MODULE_TYPE, "EVENT");
				doc.setField("eventType_s", StringUtil.checkVal(vo.getLocationDesc()));
				
				//add-ons for Huddle
				doc.setField("status_i", vo.getStatusFlg());
				doc.setField("externalUrl_s", vo.getEventFilePath()); //uploaded file or brochure
				doc.setField("eventUrl_s", vo.getEventUrl()); //cVent registration URL
				doc.setField(SearchDocumentHandler.AUTHOR + "Email_s", vo.getEmailAddress());
				doc.setField(SearchDocumentHandler.AUTHOR + "Phone_s", vo.getPhoneText());
				doc.setField(SearchDocumentHandler.START_DATE + "Year_i", Convert.formatDate(vo.getStartDate(), "yyyy"));
				doc.setField(SearchDocumentHandler.START_DATE + "Month_i", Convert.formatDate(vo.getStartDate(), "MM"));
				doc.setField(SearchDocumentHandler.END_DATE + "Year_i", Convert.formatDate(vo.getEndDate(), "yyyy"));
				doc.setField(SearchDocumentHandler.END_DATE + "Month_i", Convert.formatDate(vo.getEndDate(), "MM"));
				doc.setField(HuddleUtils.SOLR_OPCO_FIELD, StringUtil.checkVal(vo.getOpcoName()).split(", "));
				doc.setField(SearchDocumentHandler.CITY + "_s", vo.getCityName());
				doc.setField(SearchDocumentHandler.STATE + "_s", StringUtil.checkVal(states.get(vo.getStateCode())));
				doc.setField(SearchDocumentHandler.STATE, vo.getStateCode());
				doc.setField("shortDesc_s", vo.getShortDesc()); //intended audience
				//doc.setField("service_s", vo.getServiceText()); //anatomical focus
				doc.setField("objectives_s", vo.getObjectivesText());
				docs.add(doc);
			} catch (Exception e) {
				log.error("Unable to index course: " + StringUtil.getToString(vo), e);
			}
		}
		
		if (docs.size() > 0) {
			try {
				server.add(docs);
			} catch (Exception e) {
				log.error("could not add documents to Solr", e);
			}
		}
	}

	
	/**
	 * loads approved Events portlets that are attached to site pages,
	 * for all orgs in this WC instance. 
	 * @param orgId
	 * @param conn
	 * @return Map<pageUrl, BlogGroupVO>
	 */
	@Override
	protected List<EventEntryVO> loadEvents(Connection conn, Set<String> eventIds) {
		String sql = buildQuery("HUDDLE_COURSE_CAL", eventIds);
		log.debug(sql);

		List<EventEntryVO> data = new ArrayList<EventEntryVO>();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, organizationId);
			if (eventIds != null) {
				int x = 2;
				for (String eventId : eventIds)
					ps.setString(x++, eventId);
			}
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String url = rs.getString(2) + "/" + config.getProperty(Constants.QS_PATH);
				//ensure pages on subsites are aliased properly.
				String subSiteAlias = StringUtil.checkVal(rs.getString(1));
				if (subSiteAlias.length() > 0) url = "/" + subSiteAlias + url;
				
				EventEntryVO vo = new EventEntryVO(rs);
				vo.setActionUrl(url + vo.getActionId());
				vo.setOrganizationId(organizationId);
				
				log.info("loaded " + vo.getEventTypeCd() + " - " + vo.getEventName());
				data.add(vo);
			}

		} catch(Exception e) {
			log.error("Unable to retrieve events", e);
		}

		log.info("loaded " + data.size() + " events");
		return data;
	}
	

	
	
	/**
	 * returns the event lookup query used to load indexable events
	 * @return
	 */
	@Override
	protected String buildQuery(String moduleTypeId, Set<String> eventIds) {
		StringBuilder sql = new StringBuilder();
		sql.append("select s.alias_path_nm, c.full_path_txt, et.type_nm, ee.* ");
		sql.append("from event_entry ee ");
		sql.append("inner join event_type et on ee.event_type_id=et.event_type_id ");
		sql.append("inner join event_group eg on et.action_id=eg.action_id ");
		sql.append("inner join sb_action a on eg.action_id=a.action_id ");
		sql.append("inner join page_module b on a.action_id=b.action_id ");
		sql.append("inner join page_module_role pmr on pmr.page_module_id=b.page_module_id and pmr.role_id='").append(MIN_ROLE_LVL).append("' ");  //only public portlets
		sql.append("inner join page c on c.page_id=b.page_id ");
		sql.append("inner join page_role pr on pr.page_id=c.page_id and pr.role_id='").append(MIN_ROLE_LVL).append("' "); //only public pages
		sql.append("inner join site s on c.site_id=s.site_id ");
		sql.append("inner join module_display md on b.module_display_id=md.module_display_id ");
		sql.append("where s.ORGANIZATION_ID=? ");
		sql.append("and (a.pending_sync_flg is null or a.pending_sync_flg=0) ");  //portlet not pending
		sql.append("and (c.pending_sync_flg is null or c.pending_sync_flg=0) "); //page not pending
		sql.append("and a.module_type_id='").append(moduleTypeId);
		sql.append("' and md.indexable_flg=1 "); //only include pages that contain Views that are considered indexable.
		sql.append("and isnull(ee.end_dt, ee.start_dt) >= DATEADD(month, -6, CURRENT_TIMESTAMP) "); //only include -6mos of events - per business requirement

		//limit the results to the new events we're adding - this scenario is invoked by the real-time indexer
		if (eventIds != null) {
			sql.append("and ee.event_entry_id in (''");
			for (@SuppressWarnings("unused") String s : eventIds)
				sql.append(",?");
			sql.append(") ");
		}
		return sql.toString();
	}
}