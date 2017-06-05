package com.biomed.smarttrak.action.rss.util;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.biomed.smarttrak.action.rss.RSSDataAction.ArticleStatus;
import com.biomed.smarttrak.action.rss.RSSFilterAction.FilterType;
import com.biomed.smarttrak.action.rss.vo.RSSArticleVO;
import com.biomed.smarttrak.action.rss.vo.RSSFeedGroupVO;
import com.biomed.smarttrak.action.rss.vo.RSSFilterVO;
import com.biomed.smarttrak.action.rss.vo.SmarttrakRssEntityVO;
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
	/**
	 * @param args
	 */
	public AbstractSmarttrakRSSFeed(String[] args) {
		super(args);
		loadProperties("scripts/bmg_smarttrak/rss_config.properties");
		loadDBConnection(props);
	}

	/**
	 * Update the RSS Feed Status Code.
	 * @param f
	 */
	protected void updateFeed(SmarttrakRssEntityVO f) {
		try(PreparedStatement ps = dbConn.prepareStatement(buildUpdateFeedStatusStatement())) {
			ps.setInt(1, 0);
			ps.setString(2, f.getRssEntityId());
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}
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
	protected Set<String> getExistingArticles(List<String> articleIds, String entityId) {
		Set<String> ids = new HashSet<>();

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

			while(rs.next()) {
				ids.add(rs.getString("article_guid"));
			}
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
		StringBuilder sql = new StringBuilder(150 + size * 3);
		sql.append("select article_guid from ").append(props.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_RSS_ARTICLE where rss_entity_id = ? and article_guid in (");
		DBUtil.preparedStatmentQuestion(size, sql);
		sql.append(") ");
		log.info(sql.toString());
		return sql.toString();
	}

	/**
	 * Method manages Saving a List of RSSArticles.
	 * @param article
	 */
	protected void storeArticles(List<RSSArticleVO> articles) {
		try {
			new DBProcessor(dbConn, props.getProperty(Constants.CUSTOM_DB_SCHEMA)).executeBatch(getArticleInsertSql(), buildBatchVals(articles));
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Error Processing Code", e);
		}
	}


	/**
	 * Method Manages building the Smarttrak RSS Article Batch Save Statement.
	 * @return
	 */
	protected String getArticleInsertSql() {
		StringBuilder sql = new StringBuilder(410);
		sql.append("insert into ").append(props.getProperty(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_rss_article (rss_article_id, article_status_cd, ");
		sql.append("feed_group_id, rss_entity_id, article_guid, article_txt, ");
		sql.append("filter_article_txt, title_txt, filter_title_txt, article_url, ");
		sql.append("publish_dt, create_dt, publication_nm) values (?,?,?,?,?,?,?,?,?,?,?,?,?)");
		log.debug(sql.toString());
		return sql.toString();
	}

	/**
	 * Method manages building Map of RSSArticle values used in Batch Save Statement.
	 * @param articles
	 * @return
	 */
	protected Map<String, List<Object>> buildBatchVals(List<RSSArticleVO> articles) {
		Map<String, List<Object>> insertValues = new HashMap<>();

		UUIDGenerator uuid = new UUIDGenerator();
		for (RSSArticleVO a : articles) {
			String xrId = uuid.getUUID();
			List<Object> insertData = new ArrayList<>();
			insertData.addAll(Arrays.asList(xrId, a.getArticleStatusCd(), a.getFeedGroupId(), a.getRssEntityId()));
			insertData.addAll(Arrays.asList(a.getArticleGuid(), a.getArticleTxt(), a.getFilterArticleTxt(), a.getTitleTxt()));
			insertData.addAll(Arrays.asList(a.getFilterTitleTxt(), a.getArticleUrl(), a.getPublishDt(), Convert.getCurrentTimestamp()));
			insertData.addAll(Arrays.asList(a.getPublicationName()));
			insertValues.put(xrId, insertData);
		}

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
		log.info("retrieving " + url);
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
			log.error("Error Processing Code", e);
		}
		return data;
	}

	/**
	 * Retrieves all the Feeds for Biomedgps smarttrak.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, RSSFeedGroupVO> loadFilters(String rssEntityId) {
		Map<String, RSSFeedGroupVO> groupMap = new HashMap<>();
		DBProcessor dbp = new DBProcessor(dbConn, props.getProperty(Constants.CUSTOM_DB_SCHEMA));
		List<Object> vals = new ArrayList<>();
		if(!StringUtil.isEmpty(rssEntityId)) {
			vals.add(rssEntityId);
		}
		List<RSSFeedGroupVO> groups = (List<RSSFeedGroupVO>)(List<?>)dbp.executeSelect(getFiltersSql(!StringUtil.isEmpty(rssEntityId)), vals, new RSSFeedGroupVO());
		for(RSSFeedGroupVO g : groups) {
			groupMap.put(g.getFeedGroupId(), g);
		}

		return groupMap;
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
	protected void matchArticle(RSSArticleVO article, List<RSSFilterVO> filters) {
		for(RSSFilterVO filter: filters) {
			if(FilterType.O.name().equals(filter.getTypeCd())) {
				checkOmitMatch(article, filter);
			} else if(FilterType.R.name().equals(filter.getTypeCd())) {
				checkReqMatch(article, filter);
			}
		}
	}


	/**
	 * Check if we have a match on an Omit Filter.  If so, set Status Code R for
	 * Rejected.
	 * @param article
	 * @param filter
	 */
	protected void checkOmitMatch(RSSArticleVO article, RSSFilterVO filter) {
		boolean isMatch = checkMatch(article, filter);

		if(isMatch) {
			article.setArticleStatusCd(ArticleStatus.R.name());
		}
	}


	/**
	 * Check if we have a match on a Required filter.  If so and Status isn't
	 * Rejected, Set Status as New.
	 * @param article
	 * @param filter
	 */
	protected void checkReqMatch(RSSArticleVO article, RSSFilterVO filter) {
		boolean isMatch = checkMatch(article, filter);

		if(isMatch && !ArticleStatus.R.name().equals(article.getArticleStatusCd())) {
			article.setArticleStatusCd(ArticleStatus.N.name());
		}
	}


	/**
	 * Perform Regex Search and Replacement on the article title and body text.
	 * @param article
	 * @param filter
	 * @return
	 */
	protected boolean checkMatch(RSSArticleVO article, RSSFilterVO filter) {
		boolean isMatch = false;

		article.setFilterArticleTxt(article.getArticleTxt().replaceAll(filter.getFilterExpression(), props.getProperty(REPLACE_SPAN)));
		article.setFilterTitleTxt(article.getTitleTxt().replaceAll(filter.getFilterExpression(), props.getProperty(REPLACE_SPAN)));

		//Build Matchers.
		if(article.getFilterArticleTxt().contains("<span class='hit'>")) {
			isMatch = true;
		}
		if(article.getFilterTitleTxt().contains("<span class='hit'>")) {
			isMatch = true;
		}

		return isMatch;
	}
}