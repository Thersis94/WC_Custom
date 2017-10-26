package com.biomed.smarttrak.action.rss.util;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.biomed.smarttrak.action.rss.RSSDataAction.ArticleStatus;
import com.biomed.smarttrak.action.rss.RSSFilterAction.FilterType;
import com.biomed.smarttrak.action.rss.vo.RSSArticleFilterVO;
import com.biomed.smarttrak.action.rss.vo.RSSArticleVO;
import com.biomed.smarttrak.action.rss.vo.RSSFeedGroupVO;
import com.biomed.smarttrak.action.rss.vo.RSSFilterVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> AbstractSmarttrakRSSFeed.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Abstract Smarttrak Feed Util.  Holds Common Code that
 * should be available to all feed processors.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.0
 * @since May 21, 2017
 ****************************************************************************/
public abstract class AbstractSmarttrakRSSFeed extends CommandLineUtil {

	public static final String REPLACE_SPAN = "replaceSpan";
	public static final String PUBMED_ARTICLE_URL = "pubmedArticleUrl";
	public static final String PUBMED_ENTITY_ID = "pubmedEntityId";
	public static final String IS_DEBUG = "isDebug";

	protected Map<FilterType, Map<String, List<RSSFilterVO>>> filters;
	protected List<RSSFeedGroupVO> groups;
	private UUIDGenerator uuid;


	/**
	 * @param args
	 */
	public AbstractSmarttrakRSSFeed(String[] args) {
		super(args);
		loadProperties("scripts/bmg_smarttrak/rss_config.properties");
		loadDBConnection(props);
		filters = new EnumMap<>(FilterType.class);
		groups = new ArrayList<>();
		uuid = new UUIDGenerator();
	}

	/**
	 * Helper method that builds the 
	 * @return
	 */
	protected String buildUpdateFeedStatusStatement() {
		StringBuilder sql = new StringBuilder(75);
		sql.append("update RSS_ENTITY set is_active = ? where rss_entity_id = ?");
		return sql.toString();
	}

	/**
	 * Helper method checks Database for existing Articles.
	 * @param article
	 * @return
	 */
	protected Map<String, Set<String>> getExistingArticles(List<String> articleIds, String entityId) {
		Map<String, Set<String>> ids = new HashMap<>();

		if(articleIds == null || articleIds.isEmpty()) {
			return ids;
		}

		try(PreparedStatement ps = dbConn.prepareStatement(getArticleExistsSql(articleIds.size()))) {
			int i = 1;
			ps.setString(i++, entityId);
			for(String aId : articleIds) {
				ps.setString(i++, aId);
			}

			ResultSet rs = ps.executeQuery();

			String currId = null;
			Set<String> afGroups = null;
			while(rs.next()) {
				if(!rs.getString("article_guid").equals(currId)) {
					if(currId != null) {
						ids.put(currId, afGroups);
					}

					afGroups = new HashSet<>();
					currId = rs.getString("article_guid");
				}

				afGroups.add(rs.getString("feed_group_id"));
			}

			//Set Last Group.
			ids.put(currId, afGroups);
		} catch (SQLException e) {
			log.error("Error Checking RSS Article Exists", e);
		}
		return ids;
	}

	/**
	 * Helper method builds the SQL for checking existing Articles.
	 * @return
	 */
	protected String getArticleExistsSql(int size) {
		StringBuilder sql = new StringBuilder(500 + size * 3);
		String schema = (String)props.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select article_guid, feed_group_id from ").append(schema);
		sql.append("BIOMEDGPS_RSS_ARTICLE a inner join ");
		sql.append(schema).append("biomedgps_rss_filtered_article fa ");
		sql.append("on fa.rss_article_id = a.rss_article_id ");
		sql.append("where rss_entity_id = ? and article_guid in (");
		DBUtil.preparedStatmentQuestion(size, sql);
		sql.append(") order by article_guid ");
		return sql.toString();
	}

	/**
	 * Method manages Saving a List of RSSArticles.
	 * @param article
	 */
	protected void storeArticles(RSSArticleVO a) {
		DBProcessor dbp = new DBProcessor(dbConn, props.getProperty(Constants.CUSTOM_DB_SCHEMA));
		try {
			a.setRssArticleId(uuid.getUUID());
			dbp.insert(a);
			String sql = getArticleFilterSql();
			dbp.executeBatch(sql, buildArticleFilterVals(a));
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Error Saving Articles", e);
		}
	}

	protected String getArticleFilterSql() {
		StringBuilder sql = new StringBuilder(400);
		sql.append("insert into ").append(props.getProperty(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_rss_filtered_article (rss_article_filter_id, ");
		sql.append("feed_group_id, article_status_cd, rss_article_id, filter_title_txt, ");
		sql.append("filter_article_txt, create_dt, match_no) values(?,?,?,?,?,?,?,?)");
		return sql.toString();
	}

	protected Map<String, List<Object>> buildArticleFilterVals(RSSArticleVO a) {
		Map<String, List<Object>> insertValues = new HashMap<>();
		int n = 0;
		int r = 0;
		int o = 0;

		for (RSSArticleFilterVO af : a.getFilterVOs().values()) {
			if(af.getArticleStatus().equals(ArticleStatus.N)) {
				n++;
			} else if (af.getArticleStatus().equals(ArticleStatus.R)) {
				r++;
			} else if (af.getArticleStatus().equals(ArticleStatus.O)) {
				o++;
			}

			String afId = uuid.getUUID();
			List<Object> insertData = new ArrayList<>();
			insertData.addAll(Arrays.asList(afId, af.getFeedGroupId(), af.getArticleStatus().name()));
			insertData.addAll(Arrays.asList(a.getRssArticleId(), StringUtil.checkVal(af.getFilterTitleTxt(), "Untitled")));
			insertData.addAll(Arrays.asList(StringUtil.checkVal(af.getFilterArticleTxt(), "No Article Available"), Convert.getCurrentTimestamp()));
			insertData.add(af.getMatchCount());
			insertValues.put(afId, insertData);
		}

		log.info("Number of New: " + n + "\nNumber of Rejected: " + r + "\nNumber of Omitted: " + o);

		return insertValues;
	}

	/**
	 * Helper method that manages the HTTP Connection to Pubmed Servers and
	 * converting the response back to a manageable format.
	 * @param url
	 * @param queryParams
	 * @return
	 */
	protected byte [] getDataViaHTTP(String url, Map<String, Object> queryParams) {
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		byte[] data = null;
		try {
			data = conn.retrieveDataViaPost(url, queryParams);

			//trap all errors generated by LL
			if (404 == conn.getResponseCode()) {
				return data;
			}

			if (200 != conn.getResponseCode()) {
				throw new IOException("Transaction Unsuccessful, code=" + conn.getResponseCode());
			}

		} catch (IOException e) {
			StringBuilder err = new StringBuilder(100);
			err.append("Could not retrieve Feed: ").append(url).append(", Connection Response: ").append(conn.getResponseCode());
			log.error(err.toString());
		}
		return data;
	}

	/**
	 * Retrieves all the Feeds for Biomedgps smarttrak.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected void loadFilters(String rssEntityId) {
		Map<String, List<RSSFilterVO>> omits = new HashMap<>();
		Map<String, List<RSSFilterVO>> reqs = new HashMap<>();
		DBProcessor dbp = new DBProcessor(dbConn, props.getProperty(Constants.CUSTOM_DB_SCHEMA));
		List<Object> vals = new ArrayList<>();
		if(!StringUtil.isEmpty(rssEntityId)) {
			vals.add(rssEntityId);
		}
		groups = (List<RSSFeedGroupVO>)(List<?>)dbp.executeSelect(getFiltersSql(!StringUtil.isEmpty(rssEntityId)), vals, new RSSFeedGroupVO());
		for(RSSFeedGroupVO g : groups) {
			List<RSSFilterVO> o = new ArrayList<>();
			List<RSSFilterVO> r = new ArrayList<>();
			for(RSSFilterVO f : g.getFilters()) {
				if(FilterType.O.name().equals(f.getTypeCd())) {
					o.add(f);
				} else if (FilterType.R.name().equals(f.getTypeCd())) {
					r.add(f);
				}
			}
			omits.put(g.getFeedGroupId(), o);
			reqs.put(g.getFeedGroupId(), r);
		}

		filters.put(FilterType.O, omits);
		filters.put(FilterType.R, reqs);
	}


	/**
	 * Builds Feeds Info Sql Query.
	 * @return
	 */
	protected String getFiltersSql(boolean hasRSSEntityId) {
		String schema = props.getProperty(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(450);
		sql.append("select g.feed_group_id, f.filter_expression, f.filter_type_cd ");
		sql.append("from ").append(schema).append("biomedgps_feed_group g ");
		sql.append("inner join ").append(schema).append("biomedgps_feed_filter_group_xr gxr ");
		sql.append("on g.feed_group_id = gxr.feed_group_id ");
		sql.append("inner join ").append(schema).append("biomedgps_rss_parser_filter f ");
		sql.append("on f.filter_id = gxr.filter_id ");
		if(hasRSSEntityId) {
			sql.append("inner join ").append(schema).append("biomedgps_feed_source_group_xr xr ");
			sql.append("on g.feed_group_id = xr.feed_group_id and xr.rss_entity_id = ? ");
		}
		sql.append("order by g.feed_group_id, f.filter_type_cd ");
		return sql.toString();
	}

	/**
	 * Apply Filters to the articles.  Depending on if it is a Required or Omit
	 * filter, perform desired workflow.
	 * @param article
	 * @param filters
	 */
	protected void applyFilter(RSSArticleVO article, String feedGroupId) {
		Map<String, List<RSSFilterVO>> oFilter = filters.get(FilterType.O);
		RSSArticleFilterVO af = new RSSArticleFilterVO(article, feedGroupId);

		if(oFilter != null && oFilter.containsKey(feedGroupId)) {
			List<RSSFilterVO> ofs = oFilter.get(feedGroupId);
			for(RSSFilterVO filter: ofs) {
				if(checkOmitMatch(af, filter)) {

					// Null out FullArticleTxt to lessen memory overhead
					af.setFullArticleTxt(null);
					article.addFilteredText(af);
					return;
				}
			}
		}

		/*
		 * If we haven't marked the article as omitted, check if we pass required
		 * filters.
		 */
		Map<String, List<RSSFilterVO>> rFilter = filters.get(FilterType.R);

		if(!ArticleStatus.R.equals(af.getArticleStatus()) && rFilter != null && rFilter.containsKey(feedGroupId)) {
			List<RSSFilterVO> rfs = rFilter.get(feedGroupId);
			for(RSSFilterVO filter: rfs) {
				if(checkReqMatch(af, filter)) {

					// Null out FullArticleTxt to lessen memory overhead
					af.setFullArticleTxt(null);
					article.addFilteredText(af);
					return;
				}
			}
		}

		article.addFilteredText(af);
	}


	/**
	 * Check if we have a match on an Omit Filter.  If so, set Status Code R for
	 * Rejected.
	 * @param article
	 * @param filter
	 */
	protected boolean checkOmitMatch(RSSArticleFilterVO af, RSSFilterVO filter) {
		boolean isMatch = checkMatch(af, filter);

		if(isMatch) {
			af.setArticleStatus(ArticleStatus.R);
		}

		return isMatch;
	}


	/**
	 * Check if we have a match on a Required filter.  If so and Status isn't
	 * Rejected, Set Status as New.
	 * @param article
	 * @param filter
	 */
	protected boolean checkReqMatch(RSSArticleFilterVO af, RSSFilterVO filter) {
		boolean isMatch = checkMatch(af, filter);

		if(isMatch && !ArticleStatus.R.equals(af.getArticleStatus())) {
			af.setArticleStatus(ArticleStatus.N);
		}

		return isMatch;
	}


	/**
	 * Perform Regex Search and Replacement on the article title and body text.
	 * @param article
	 * @param filter
	 * @return
	 */
	protected boolean checkMatch(RSSArticleFilterVO af, RSSFilterVO filter) {
		boolean isMatch = false;

		StringBuilder regex = new StringBuilder(filter.getFilterExpression().length() + 10);
		regex.append("(?i)(").append(filter.getFilterExpression()).append(")");

		af.setFilterArticleTxt(af.getArticleTxt().replaceAll(regex.toString(), props.getProperty(REPLACE_SPAN)));
		af.setFilterTitleTxt(af.getTitleTxt().replaceAll(regex.toString(), props.getProperty(REPLACE_SPAN)));

		//Build Matchers.
		if(af.getFilterArticleTxt().contains("<span class='hit'>")) {
			isMatch = true;
		} else if (!StringUtil.isEmpty(af.getFullArticleTxt())) {
			String filteredFull = af.getFullArticleTxt().replaceAll(regex.toString(), props.getProperty(REPLACE_SPAN));
			if(filteredFull.contains("<span class='hit'>")) {
				isMatch = true;
				af.setMatchCount(filteredFull.split("<span class='hit'>").length - 1);
			}
		}
		if(af.getFilterTitleTxt().contains("<span class='hit'>")) {
			isMatch = true;
		}

		return isMatch;
	}

	/**
	 * Filter out previously stored Articles.
	 * @param articles
	 * @param existsIds
	 * @return
	 */
	protected boolean articleExists(String articleGuid, String feedGroupId, Map<String, Set<String>> existsIds) {
		boolean exists = false;
		if(existsIds.containsKey(articleGuid)) {
			Set<String> grps = existsIds.get(articleGuid);
			if(grps.contains(feedGroupId)) {
				exists = true;
			}
		}
		return exists;
	}

}