package com.biomed.smarttrak.action.rss;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.biomed.smarttrak.action.rss.RSSDataAction.ArticleStatus;
import com.biomed.smarttrak.action.rss.vo.RSSFilterVO;
import com.biomed.smarttrak.action.rss.vo.SmarttrakRssEntityVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.session.SMTCookie;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.rss.RSSEntityVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> NewsroomConsoleAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Action manages data interactions for the Newsroom Console
 * Screen.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author raptor
 * @version 3.0
 * @since May 24, 2017
 ****************************************************************************/
public class NewsroomConsoleAction extends NewsroomAction {

	public static final String FEED_GROUP_ID = "feedGroupId";

	public NewsroomConsoleAction() {
		super();
	}

	public NewsroomConsoleAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if(req.hasParameter("loadFeeds")) {
			loadFeeds(req.getParameter(FEED_GROUP_ID));
		} else if(req.hasParameter("omitFilters")) {
			this.putModuleData(loadNonGroupedFilters(req));
		} else {
			req.setAttribute("filters", loadNonGroupedFilters(req));
			req.setAttribute("sources", loadSources(req));
			req.setAttribute("segments", loadSegmentGroupArticles(req));
		}
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		if (!req.hasParameter(FEED_GROUP_ID)) return;

		if(req.hasParameter("filterId")) {
			addFilterGroupXR(req.getParameter(FEED_GROUP_ID), req.getParameter("filterId"));
		} else if (req.hasParameter("sourceId")) {
			addSourceGroupXR(req.getParameter(FEED_GROUP_ID), req.getParameter("sourceId"));
		}
	}


	/**
	 * Add the supplied source to the current feed group
	 * @param feedGroupId
	 * @param sourceId
	 * @throws ActionException
	 */
	private void addSourceGroupXR(String feedGroupId, String sourceId) throws ActionException {
		StringBuilder sql = new StringBuilder(125);
		sql.append("insert into ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("biomedgps_feed_source_group_xr ");
		sql.append("(source_group_id, feed_group_id, rss_entity_id, create_dt)");
		sql.append("values(?,?,?,?)");
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, feedGroupId);
			ps.setString(3, sourceId);
			ps.setTimestamp(4, Convert.getCurrentTimestamp());

			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}

	/**
	 * @param parameter
	 * @param parameter2
	 */
	private void addFilterGroupXR(String feedGroupId, String filterId) {
		RSSFilterAction rfa = new RSSFilterAction(this.actionInit);
		rfa.setAttributes(attributes);
		rfa.setDBConnection(dbConn);
		rfa.saveFilterGroupXRVals(filterId, feedGroupId);
	}

	@Override
	public void delete(ActionRequest req) throws ActionException {
		if(req.hasParameter("delFilterGroupXr")) {
			deleteGroupFilterXr(req.getParameter("pkId"));
		} else if (req.hasParameter("rssEntityId")) {
			deleteEntityGroupXr(req.getParameter(FEED_GROUP_ID), req.getParameter("rssEntityId"));
		}
	}

	/**
	 * @param parameter
	 * @param parameter2
	 */
	private void deleteEntityGroupXr(String feedGroupId, String rssEntityId) {
		StringBuilder sql = new StringBuilder(150);
		sql.append("delete from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_feed_source_group_xr where feed_group_id = ? and rss_entity_id = ?");

		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, feedGroupId);
			ps.setString(2, rssEntityId);
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Error Deleting Feed Group Xr", e);
		}
	}

	/**
	 * @param parameter
	 * @param parameter2
	 */
	private void deleteGroupFilterXr(String filterGroupXrId) {
		StringBuilder sql = new StringBuilder(150);
		sql.append("delete from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_feed_filter_group_xr where feed_filter_group_xr_id = ?");

		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, filterGroupXrId);
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Error Deleting Filter Group Xr", e);
		}
	}


	/**
	 * Load sources not already assigned to the current feed group
	 * @param req
	 * @return
	 */
	private List<RSSEntityVO> loadSources(ActionRequest req) {
		SMTCookie c = req.getCookie("ACTIVE_FEED_GROUP");
		if(c != null) {
			String feedGroupId = c.getValue();
			List<Object> vals = new ArrayList<>();
			vals.add(feedGroupId);

			DBProcessor dbp = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
			return dbp.executeSelect(buildSourceRetrieve(), vals, new RSSEntityVO());
		}
		return Collections.emptyList();
	}


	/**
	 * Build the sql to retrieve no assigned sources
	 */
	private String buildSourceRetrieve() {
		StringBuilder sql = new StringBuilder(350);
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select * from ").append(schema).append("biomedgps_rss_entity br ");
		sql.append("left join rss_entity r on r.rss_entity_id = br.rss_entity_id ");
		sql.append("where br.rss_entity_id not in (select rss_entity_id from ");
		sql.append(schema).append("biomedgps_feed_source_group_xr xr ");
		sql.append("where xr.feed_group_id = ?) ");
		sql.append("order by rss_feed_nm");
		return sql.toString();
	}

	/**
	 * @param req
	 */
	protected List<RSSFilterVO> loadNonGroupedFilters(ActionRequest req) {
		SMTCookie c = req.getCookie("ACTIVE_FEED_GROUP");
		if(c != null) {
			String feedGroupId = c.getValue();
			List<Object> vals = new ArrayList<>();
			vals.add(feedGroupId);

			DBProcessor dbp = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
			return dbp.executeSelect(buildNonGroupedFilterRetrieve(), vals, new RSSFilterVO());
		}
		return Collections.emptyList();
	}

	/**
	 * @param feedGroupId
	 * @return
	 */
	private String buildNonGroupedFilterRetrieve() {
		StringBuilder sql = new StringBuilder(350);
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select * from ").append(schema).append("BIOMEDGPS_RSS_PARSER_FILTER f ");
		sql.append("where f.filter_id not in (select filter_id from ");
		sql.append(schema).append("biomedgps_feed_filter_group_xr xr ");
		sql.append("where xr.feed_group_id = ?) ");
		sql.append("order by filter_nm");
		return sql.toString();
	}

	/**
	 * Method loads Feeds table in the Console view.
	 * @param req
	 */
	private void loadFeeds(String feedGroupId) {
		List<Object> vals = new ArrayList<>();
		vals.add(feedGroupId);
		vals.add(ArticleStatus.O.toString());
		vals.add(feedGroupId);
		DBProcessor dbp = new DBProcessor(dbConn);
		this.putModuleData(dbp.executeSelect(loadFeedsSql(), vals, new SmarttrakRssEntityVO()));
	}

	/**
	 * Method builds feeds sql.
	 * @return
	 */
	private String loadFeedsSql() {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(825);

		sql.append("select distinct on (re.rss_entity_id) re.rss_entity_id, ");
		sql.append("rss_url, rss_feed_nm, re.use_filters_no, ar.last_checked_dt, ar.title_txt ");
		sql.append("from rss_entity re ");
		sql.append("inner join ").append(schema).append("biomedgps_feed_source_group_xr xr ");
		sql.append("on re.rss_entity_id = xr.rss_entity_id ");
		sql.append("left outer join ( ");
		sql.append("select fa.create_dt as last_checked_dt, a.title_txt, a.rss_entity_id, ");
		sql.append("fa.feed_group_id, fa.article_status_cd ");
		sql.append("from ").append(schema).append("biomedgps_rss_filtered_article fa ");
		sql.append("inner join ").append(schema).append("biomedgps_rss_article a ");
		sql.append("on a.rss_article_id = fa.rss_article_id ");
		sql.append("where fa.feed_group_id = ? ");
		sql.append("order by a.rss_entity_id, fa.create_dt desc ");
		sql.append(") as ar ");
		sql.append("on ar.rss_entity_id = re.rss_entity_id ");
		sql.append("and xr.feed_group_id = ar.feed_group_id and ar.article_status_cd != ? ");
		sql.append("where xr.feed_group_id = ? ");
		sql.append("order by re.rss_entity_id");

		return sql.toString();
	}
}