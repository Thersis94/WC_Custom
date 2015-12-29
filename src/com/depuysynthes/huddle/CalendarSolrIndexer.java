package com.depuysynthes.huddle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.depuysynthesinst.events.CourseCalendarSolrIndexer;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
import com.smt.sitebuilder.common.constants.Constants;

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
	
	/**
	 * @param config
	 */
	public CalendarSolrIndexer(Properties config) {
		super(config);
		super.organizationId = "DPY_SYN_HUDDLE";
	}

	
	/**
	 * loads approved Events portlets that are attached to site pages,
	 * for all orgs in this WC instance. 
	 * @param orgId
	 * @param conn
	 * @return Map<pageUrl, BlogGroupVO>
	 */
	@Override
	protected List<EventEntryVO> loadEvents(Connection conn) {
		String sql = buildQuery();
		log.debug(sql);

		List<EventEntryVO> data = new ArrayList<EventEntryVO>();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, organizationId);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String url = rs.getString(2) + "/" + config.getProperty(Constants.QS_PATH);
				//ensure pages on subsites are aliased properly.
				String subSiteAlias = StringUtil.checkVal(rs.getString(1));
				if (subSiteAlias.length() > 0) url = "/" + subSiteAlias + url;
				
				//nursing events only appear on the nursing calendar page
//				if ("nurse-education".equals(subSiteAlias) && !"NURSE".equals(rs.getString(3)))
//					continue;
//				//vet events only appear on the vet calendar page
//				else if ("veterinary".equals(subSiteAlias) && !"VET".equals(rs.getString(3)))
//					continue;
//				//future leader events only appear on the future leaders calendar page
//				else if ("futureleaders".equals(subSiteAlias) && !"FUTURE".equals(rs.getString(3)))
//					continue;
//				else if ("".equals(subSiteAlias) && !"SURGEON".equals(rs.getString(3)))
//					continue;
				
				EventEntryVO vo = new EventEntryVO(rs);
				vo.setEventUrl(url + vo.getActionId());
				vo.setOrganizationId(organizationId);
				
				log.info("loaded " + vo.getEventTypeCd() + " - " + vo.getEventName());
				data.add(vo);
			}

		} catch(Exception e) {
			log.error("Unable to retrieve course calendar events", e);
		}

		log.info("loaded " + data.size() + " events");
		return data;
	}
}