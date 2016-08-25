package com.depuysynthesinst.events;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;

import com.depuysynthes.lucene.MediaBinSolrIndex.MediaBinField;
import com.depuysynthesinst.lms.FutureLeaderACGME;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SMTAbstractIndex;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: CourseCalendarSolrIndexer.java<p/>
 * <b>Description: adds the courses from the EVENT_ENTRY table to the Solr index.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Sep 10, 2014
 ****************************************************************************/
public class CourseCalendarSolrIndexer extends SMTAbstractIndex {
	
	/**
	 * Index type for this index.  This value is stored in the INDEX_TYPE field
	 */
	protected String INDEX_TYPE = "COURSE_CAL";
	protected String organizationId = "DPY_SYN_INST";

	/**
	 * @param config
	 */
	public CourseCalendarSolrIndexer(Properties config) {
		super(config);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#addIndexItems(org.apache.solr.client.solrj.SolrClient)
	 */
	@Override
	public void addIndexItems(SolrClient server) {
		log.info("Indexing Course Calendar Portlets");
		List<EventEntryVO> data = loadEvents(dbConn, null);
		indexEvents(server, data);
	}
	
	
	public void indexCertainItems(Set<String> eventEntryIds) {
		SolrClient server = makeServer();
		log.info("Indexing Certain Course Calendar Portlets");
		
		//if we're doing a full rebuild, make sure we purge what's in there
		if (eventEntryIds == null) {
			try {
				purgeIndexItems(server);
			} catch (IOException e) {
				log.error("could not purge index", e);
			}
		}
		
		List<EventEntryVO> data = loadEvents(dbConn, eventEntryIds);
		indexEvents(server, data);
		
		try {
			server.commit(false, false, true); //commit, but don't wait for Solr to acknowledge
		} catch (Exception e) {
			log.error("could not commit to Solr", e);
		}
	}
	

	protected void indexEvents(SolrClient server, List<EventEntryVO> data) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		List<SolrInputDocument> docs = new ArrayList<>(data.size());
		
		for (EventEntryVO vo : data) {
			SolrInputDocument doc = new SolrInputDocument();
			try {
				doc.setField(SearchDocumentHandler.INDEX_TYPE, INDEX_TYPE);
				doc.setField(SearchDocumentHandler.ORGANIZATION, vo.getOrganizationId());
				doc.setField(SearchDocumentHandler.LANGUAGE, "en");
				doc.setField(SearchDocumentHandler.ROLE, SecurityController.PUBLIC_ROLE_LEVEL);
				doc.setField(SearchDocumentHandler.DOCUMENT_URL, vo.getEventUrl());
				doc.setField(SearchDocumentHandler.DOCUMENT_ID, vo.getActionId());
				doc.setField(SearchDocumentHandler.TITLE, vo.getEventName());
				doc.setField(SearchDocumentHandler.SUMMARY, buildSummary(vo));
				doc.setField(SearchDocumentHandler.AUTHOR, vo.getContactName());
				doc.setField(SearchDocumentHandler.UPDATE_DATE, df.format(vo.getLastUpdate()));
				doc.setField(SearchDocumentHandler.START_DATE + "_dt", df.format(vo.getStartDate()));
				doc.setField(SearchDocumentHandler.END_DATE + "_dt", df.format(vo.getEndDate()));
				doc.setField(SearchDocumentHandler.CONTENTS, StringUtil.getToString(vo));
				doc.setField(SearchDocumentHandler.MODULE_TYPE, "EVENT");
				doc.setField("opco_s", vo.getOpcoName());
				doc.setField(MediaBinField.AssetType.getField(), "Course");
				doc.setField(MediaBinField.AssetDesc.getField(), "Course"); //displays on the gallery view
				doc.setField("duration_i", vo.getDuration()); //this is an int, not a String like MediaBin uses
				doc.setField("eventType_s", StringUtil.checkVal(vo.getEventTypeCd()).toLowerCase());
				
				for (String s : StringUtil.checkVal(vo.getServiceText()).split(","))
					doc.addField(SearchDocumentHandler.HIERARCHY, s.trim());
				
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
	 * builds a summary of the Event using city & state.  fallback to full description
	 * @param vo
	 * @return
	 */
	protected String buildSummary(EventEntryVO vo) {
		String val = StringUtil.checkVal(vo.getCityName());
		if (val.length() > 0 && vo.getStateCode() != null && vo.getStateCode().length() > 0) val += ", ";
		val+= vo.getStateCode();
		
		return val;
	}
	
	
	/**
	 * loads approved Events portlets that are attached to site pages,
	 * for all orgs in this WC instance. 
	 * @param orgId
	 * @param conn
	 * @return Map<pageUrl, BlogGroupVO>
	 */
	protected List<EventEntryVO> loadEvents(Connection conn, Set<String> eventIds) {
		String sql = buildQuery("COURSE_CAL", eventIds);
		log.debug(sql);

		List<EventEntryVO> data = new ArrayList<EventEntryVO>();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, organizationId);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			if (eventIds != null) {
				int x = 3;
				for (String eventId : eventIds)
					ps.setString(x++, eventId);
			}
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String url = rs.getString(2) + "/" + config.getProperty(Constants.QS_PATH);
				//ensure pages on subsites are aliased properly.
				String subSiteAlias = StringUtil.checkVal(rs.getString(1));
				if (subSiteAlias.length() > 0) url = "/" + subSiteAlias + url;
				
				//nursing events only appear on the nursing calendar page
				if ("nurse-education".equals(subSiteAlias) && !"NURSE".equals(rs.getString(3)))
					continue;
				//vet events only appear on the vet calendar page
				else if ("veterinary".equals(subSiteAlias) && !"VET".equals(rs.getString(3)))
					continue;
				//future leader events only appear on the future leaders calendar page
				else if ("futureleaders".equals(subSiteAlias) && !"FUTURE".equals(rs.getString(3)))
					continue;
				else if ("".equals(subSiteAlias) && !"SURGEON".equals(rs.getString(3)))
					continue;
				else if ("outpatient-education".equals(subSiteAlias) && !"OUTPATIENT".equals(rs.getString(3)))
					continue;
				
				EventEntryVO vo = new EventEntryVO(rs);
				vo.setEventUrl(url + vo.getActionId());
				vo.setOrganizationId(organizationId);
				
				//for vet, we need to align the hierarchies so they match the anatomy pages, which are 2nd level.
				if ("VET".equals(vo.getEventTypeCd())) {
					vo.setServiceText("Vet/Small Animal,Vet/Large Animal");
				} else if ("FUTURE".equals(vo.getEventTypeCd()) && vo.getServiceText() != null) {
					String[] svcs = vo.getServiceText().split(",");
					if (svcs != null && svcs.length > 0) {
						StringBuilder services = new StringBuilder();
						for (String s : svcs) {
							if (services.length() > 0) services.append(",");
							services.append(FutureLeaderACGME.getHierarchyFromCode(s));
						}
						log.debug(services);
						vo.setServiceText(services.toString());
					}
				}
				
				log.info("loaded " + vo.getEventTypeCd() + " - " + vo.getEventName());
				data.add(vo);
			}

		} catch(Exception e) {
			log.error("Unable to retrieve course calendar events", e);
		}

		log.info("loaded " + data.size() + " events");
		return data;
	}

	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTAbstractIndex#getIndexType()
	 */
	@Override
	public String getIndexType() {
		return INDEX_TYPE;
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#purgeIndexItems(org.apache.solr.client.solrj.SolrClient)
	 */
	@Override
	public void purgeIndexItems(SolrClient server) throws IOException {
		try {
			server.deleteByQuery(SearchDocumentHandler.INDEX_TYPE + ":" + getIndexType() + " AND " + SearchDocumentHandler.ORGANIZATION + ":" + organizationId);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
	
	
	/**
	 * returns the event lookup query used to load indexable events
	 * @return
	 */
	protected String buildQuery(String moduleTypeId, Set<String> eventIds) {
		StringBuilder sql = new StringBuilder();
		sql.append("select s.alias_path_nm, c.full_path_txt, et.type_nm, ee.* ");
		sql.append("from event_entry ee ");
		sql.append("inner join event_type et on ee.event_type_id=et.event_type_id ");
		sql.append("inner join event_group eg on et.action_id=eg.action_id ");
		sql.append("inner join sb_action a on eg.action_id=a.attrib1_txt ");
		sql.append("inner join page_module b on a.action_id=b.action_id ");
		sql.append("inner join page_module_role pmr on pmr.page_module_id=b.page_module_id and pmr.role_id='0' ");  //only public portlets
		sql.append("inner join page c on c.page_id=b.page_id ");
		sql.append("inner join page_role pr on pr.page_id=c.page_id and pr.role_id='0' "); //only public pages
		sql.append("inner join site s on c.site_id=s.site_id ");
		sql.append("inner join module_display md on b.module_display_id=md.module_display_id ");
		sql.append("where s.ORGANIZATION_ID=? and ee.start_dt > ? ");
		sql.append("and (a.pending_sync_flg is null or a.pending_sync_flg=0) ");  //portlet not pending
		sql.append("and (c.pending_sync_flg is null or c.pending_sync_flg=0) "); //page not pending
		sql.append("and a.module_type_id='").append(moduleTypeId);
		sql.append("' and md.indexable_flg=1 "); //only include pages that contain Views that are considered indexable.
		
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
