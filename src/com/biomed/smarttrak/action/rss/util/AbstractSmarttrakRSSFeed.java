package com.biomed.smarttrak.action.rss.util;

import java.io.IOException;
import java.sql.Connection;
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
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import com.biomed.smarttrak.action.rss.RSSDataAction.ArticleStatus;
import com.biomed.smarttrak.action.rss.RSSFilterAction.FilterType;
import com.biomed.smarttrak.action.rss.vo.RSSArticleFilterVO;
import com.biomed.smarttrak.action.rss.vo.RSSArticleVO;
import com.biomed.smarttrak.action.rss.vo.RSSFeedGroupVO;
import com.biomed.smarttrak.action.rss.vo.RSSFilterVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SMTIndexIntfc;

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
public abstract class AbstractSmarttrakRSSFeed {

	protected static final String SPAN_CLASS_HIT = "<span class='hit'>";
	protected static final String UPDATE_RSS_SQL = "update RSS_ENTITY set is_active = ? where rss_entity_id = ?";
	protected static final String REPLACE_SPAN = "replaceSpan";
	protected static final String PUBMED_ARTICLE_URL = "pubmedArticleUrl";
	protected static final String PUBMED_ENTITY_ID = "pubmedEntityId";
	protected static final String IS_DEBUG = "isDebug";

	protected Logger log;
	protected Properties props;
	protected Connection dbConn;
	protected String customDb;
	protected String replaceSpanText;
	protected String mockUserAgent;
	protected Map<FilterType, Map<String, List<RSSFilterVO>>> filters;
	protected List<RSSFeedGroupVO> groups;
	protected UUIDGenerator uuid;
	protected String feedName;

	private String  storeArticleQuery;
	private Map<String, Long> accessTimes;
	private static final long LAG_TIME_MS = 2000;

	private SMTIndexIntfc index;

	/**
	 * @param index 
	 * @param args
	 */
	public AbstractSmarttrakRSSFeed(Connection dbConn, Properties props, SMTIndexIntfc index) {
		this.dbConn = dbConn;
		this.props = props;
		this.index = index;
		customDb = props.getProperty(Constants.CUSTOM_DB_SCHEMA);
		replaceSpanText = props.getProperty(REPLACE_SPAN);
		mockUserAgent = props.getProperty("mockUserAgent");
		log = Logger.getLogger(getClass());
		filters = new EnumMap<>(FilterType.class);
		groups = new ArrayList<>();
		uuid = new UUIDGenerator();
		accessTimes = new HashMap<>(5000);
		prepQueries();
	}

	private void prepQueries() {
		StringBuilder sql = new StringBuilder(250);
		sql.append("insert into ").append(customDb);
		sql.append("biomedgps_rss_filtered_article (rss_article_filter_id, ");
		sql.append("feed_group_id, article_status_cd, rss_article_id, filter_title_txt, ");
		sql.append("filter_article_txt, create_dt, match_no) values (?,?,?,?,?,?,?,?)");

		storeArticleQuery = sql.toString();
	}
	/**
	 * Abstract run method to be implemented by concrete subclasses.
	 */
	public abstract void run();


	protected String buildUpdateFeedStatusStatement() {
		return UPDATE_RSS_SQL;
	}


	/**
	 * Retrieves a set of articleIds from the DB based on rssEntityId and article_guid values.
	 * @param article
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, GenericVO> getExistingArticles(List<String> articleGuids, String rssEntityId) {
		Map<String, GenericVO> data = new HashMap<>();
		if (articleGuids == null || articleGuids.isEmpty()) return data;

		int i = 0;
		String currArticleGuid = null;
		GenericVO info = null;
		Set<String> feedGroupIds = null;
		long start = System.currentTimeMillis();
		log.info(getArticleExistsSql(articleGuids.size()));
		try (PreparedStatement ps = dbConn.prepareStatement(getArticleExistsSql(articleGuids.size()))) {
			ps.setString(++i, rssEntityId);
			for (String id : articleGuids)
				ps.setString(++i, id);

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				currArticleGuid = rs.getString("value");
				info = data.get(currArticleGuid);

				if (info == null) {
					feedGroupIds = new HashSet<>();
					info = new GenericVO(rs.getString("key"), feedGroupIds);
				} else {
					feedGroupIds = (Set<String>) info.getValue();
				}

				feedGroupIds.add(rs.getString("feed_group_id"));
				data.put(currArticleGuid, info);
			}
		} catch (SQLException e) {
			log.error("error checking if RSS article exists", e);
		}
		log.info("read took " + (System.currentTimeMillis()-start) + "ms");
		return data;
	}


	/**
	 * Helper method builds the SQL for checking existing Articles.
	 * @return
	 */
	protected String getArticleExistsSql(int size) {
		StringBuilder sql = new StringBuilder(500);
		sql.append("select a.rss_article_id as key, a.article_guid as value, fa.feed_group_id from ").append(customDb);
		sql.append("BIOMEDGPS_RSS_ARTICLE a ");
		sql.append(DBUtil.INNER_JOIN).append(customDb).append("biomedgps_rss_filtered_article fa on a.rss_article_id=fa.rss_article_id ");
		sql.append("where a.rss_entity_id=? and a.article_guid in (");
		DBUtil.preparedStatmentQuestion(size, sql);
		sql.append(") order by a.article_guid");
		return sql.toString();
	}

	/**
	 * Method manages Saving a List of RSSArticles.
	 * @param article
	 */
	protected void storeArticle(RSSArticleVO article) {
		long start = System.currentTimeMillis();
		try {
			/*
			 * This check is verifying the article is unique in the system.
			 * Currently is preventing duplicates from aggregate feeds showing up
			 * as newer articles.
			 */
			if (!articleExists(article)) {
				DBProcessor dbp = new DBProcessor(dbConn, customDb);

				dbp.insert(article);
				log.debug("created rss_article " + article.getArticleGuid());

				// Save a list of filtered matches tied to the article
				// only if the article is new. If we already saved this article
				// it is not breaking news and doesn't to be filtered and put into feeds.
				dbp.executeBatch(storeArticleQuery, buildArticleFilterVals(article));
				log.info("write took " + (System.currentTimeMillis()-start) + "ms");

			} else {
				log.info("Article Already Exists: " + article.getRssArticleId());
			}

		} catch (InvalidDataException | DatabaseException e) {
			log.error("Error Saving Articles", e);
		}
	}


	/**
	 * quick DB query to see if the article already exists - so we don't create a dupliate.
	 * @param articleGuid
	 * @param dbp
	 * @return
	 */
	private boolean articleExists(RSSArticleVO article) {
		StringBuilder sql = new StringBuilder(100);
		sql.append("select rss_article_id from ").append(customDb).append("biomedgps_rss_article where article_guid=?");

		long start = System.currentTimeMillis();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, article.getArticleGuid());
			ResultSet rs = ps.executeQuery();
			if  (rs.next()) {
				//update the ID by erference so it can be used downstream
				article.setRssArticleId(rs.getString(1));
				return true;
			}

		} catch (SQLException sqle) {
			log.error("could not check for existing article", sqle);
		}
		//set a new ID and return false so it gets inserted
		article.setRssArticleId(uuid.getUUID());
		log.info("read article took " + (System.currentTimeMillis()-start) + "ms");
		return false;
	}


	protected Map<String, List<Object>> buildArticleFilterVals(RSSArticleVO a) {
		Map<String, List<Object>> insertValues = new HashMap<>();
		int n = 0;
		int r = 0;
		int o = 0;

		for (RSSArticleFilterVO af : a.getFilterVOs().values()) {
			if (ArticleStatus.N.equals(af.getArticleStatus())) {
				n++;
			} else if (ArticleStatus.R.equals(af.getArticleStatus())) {
				r++;
			} else if (ArticleStatus.O.equals(af.getArticleStatus())) {
				o++;
			}

			String afId = uuid.getUUID();
			List<Object> insertData = new ArrayList<>();
			insertData.addAll(Arrays.asList(afId, af.getFeedGroupId(), af.getArticleStatus().name()));
			insertData.add(a.getRssArticleId());

			/*
			 * If article was ommitted then no matches were found in title or article.
			 * Otherwise check values individually to verify where match occurred.
			 */
			if(ArticleStatus.O.equals(af.getArticleStatus())) {
				insertData.add(null);
				insertData.add(null);
			} else {
				if(!af.getTitleTxt().equals(af.getFilterTitleTxt())) {
					insertData.add(StringUtil.checkVal(af.getFilterTitleTxt(), "Untitled"));
				} else {
					insertData.add(null);
				}

				//Only write the Filtered Article Text if it's different.
				if(!af.getArticleTxt().equals(af.getFilterArticleTxt())) {
					insertData.addAll(Arrays.asList(StringUtil.checkVal(af.getFilterArticleTxt(), "No Article Available")));
				} else {
					insertData.add(null);
				}
			}

			insertData.add(Convert.getCurrentTimestamp());
			insertData.add(af.getMatchCount());
			insertValues.put(afId, insertData);
		}

		log.info("Number of New: " + n + ", Number of Rejected: " + r + ", Number of Omitted: " + o);
		return insertValues;
	}


	/**
	 * Helper method that manages the HTTP Connection to Pubmed Servers and
	 * converting the response back to a manageable format.
	 * @param url
	 * @param queryParams
	 * @return
	 */
	protected byte [] getDataViaHttp(String url, Map<String, Object> queryParams) {
		SMTHttpConnectionManager conn = createBaseHttpConnection();
		byte[] data = null;
		try {
			throttleRequests(url);
			
			long start = System.currentTimeMillis();
			if (queryParams != null && !queryParams.isEmpty()) {
				//do a POST
				data = conn.retrieveDataViaPost(url, queryParams);
			} else {
				//do a GET - increases our odds of success
				data = conn.retrieveData(url);
			}
			log.info("http call took " + (System.currentTimeMillis()-start) + "ms");


			//trap all errors generated by LL
			if (404 == conn.getResponseCode()) {
				return data;
			}

			if (200 != conn.getResponseCode()) {
				throw new IOException("Transaction Unsuccessful, code=" + conn.getResponseCode());
			}

		} catch (Exception e) {
			StringBuilder err = new StringBuilder(100);
			err.append("Could not retrieve Feed: ").append(url).append(", Connection Response: ").append(conn.getResponseCode());
			log.error(err.toString());
		}
		return data;
	}


	/**
	 * creates the base http connection we'll want to use for all feeds - so regardless of RSS 
	 * target we're always using consistent http headers/settings. (like followRedirects=true)
	 **/
	protected SMTHttpConnectionManager createBaseHttpConnection() {
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		conn.setConnectionTimeout(30000);
		conn.setFollowRedirects(true);
		conn.addRequestHeader("User-Agent", mockUserAgent);
		conn.addRequestHeader("Accept", "*/*");
		conn.addRequestHeader("Accept-Language", "en-US,en;q=0.8");
		conn.addRequestHeader("Cache-Control" , "no-cache");
		conn.addRequestHeader("Connection", "keep-alive");
		conn.addRequestHeader("Pragma" , "no-cache");
		return conn;
	}

	/**
	 * Retrieves all the Feeds for Biomedgps smarttrak.
	 * @return
	 */
	protected void loadFilters(String rssEntityId) {
		Map<String, List<RSSFilterVO>> omits = new HashMap<>();
		Map<String, List<RSSFilterVO>> reqs = new HashMap<>();
		DBProcessor dbp = new DBProcessor(dbConn, customDb);
		List<Object> vals = new ArrayList<>();
		if (!StringUtil.isEmpty(rssEntityId)) {
			vals.add(rssEntityId);
		}

		groups = dbp.executeSelect(getFiltersSql(!StringUtil.isEmpty(rssEntityId)), vals, new RSSFeedGroupVO());
		for (RSSFeedGroupVO g : groups) {
			List<RSSFilterVO> o = new ArrayList<>();
			List<RSSFilterVO> r = new ArrayList<>();
			for (RSSFilterVO f : g.getFilters()) {
				if (FilterType.O.name().equals(f.getTypeCd())) {
					o.add(f);
				} else if (FilterType.R.name().equals(f.getTypeCd())) {
					r.add(f);
				}
			}
			if (!o.isEmpty()) omits.put(g.getFeedGroupId(), o);
			if (!r.isEmpty()) reqs.put(g.getFeedGroupId(), r);
			log.info("loaded feedGroupId=" + g.getFeedGroupId() + " with " + o.size() + " Omits and " + r.size() + " Requires");
		}

		filters.put(FilterType.O, omits);
		filters.put(FilterType.R, reqs);
	}


	/**
	 * Builds Feeds Info Sql Query.
	 * @return
	 */
	protected String getFiltersSql(boolean hasRSSEntityId) {
		StringBuilder sql = new StringBuilder(450);
		sql.append("select g.feed_group_id, f.filter_expression, f.filter_type_cd ");
		sql.append("from ").append(customDb).append("biomedgps_feed_group g ");
		sql.append(DBUtil.INNER_JOIN).append(customDb).append("biomedgps_feed_filter_group_xr gxr ");
		sql.append("on g.feed_group_id = gxr.feed_group_id ");
		sql.append(DBUtil.INNER_JOIN).append(customDb).append("biomedgps_rss_parser_filter f ");
		sql.append("on f.filter_id = gxr.filter_id ");
		if (hasRSSEntityId) {
			sql.append(DBUtil.INNER_JOIN).append(customDb).append("biomedgps_feed_source_group_xr xr ");
			sql.append("on g.feed_group_id = xr.feed_group_id and xr.rss_entity_id = ? ");
		}
		sql.append("order by g.feed_group_id, f.filter_type_cd ");
		return sql.toString();
	}


	/**
	 * Apply Filters to the articles.  Depending on if it is a Required or Omit
	 * filter, perform desired workflow.
	 * @param article
	 * @param feedGroupId
	 * @param useFilters
	 */
	protected void applyFilter(RSSArticleVO article, String feedGroupId, boolean useFilters) {
		long start = System.currentTimeMillis();
		Map<String, List<RSSFilterVO>> omitFilters = filters.get(FilterType.O);
		RSSArticleFilterVO af = new RSSArticleFilterVO(article, feedGroupId);

		if(!useFilters && StringUtil.isEmpty(article.getRssArticleId())) {
			af.setFilterArticleTxt(af.getArticleTxt());
			af.setArticleStatus(ArticleStatus.N);
			af.setFilterTitleTxt(af.getTitleTxt());
			article.addFilteredText(af);
			return;
		}

		//Check if the article passes any Omission Filters.
		if (omitFilters != null && omitFilters.containsKey(feedGroupId) && applyOmissionFilters(omitFilters.get(feedGroupId), af, article)) {
			return;
		}

		// If we haven't marked the article as omitted, check if we pass required filters.
		Map<String, List<RSSFilterVO>> rFilter = filters.get(FilterType.R);

		//Check if the article passes any Required Filters.
		if (!ArticleStatus.R.equals(af.getArticleStatus()) && rFilter != null && rFilter.containsKey(feedGroupId) && applyRequired(rFilter.get(feedGroupId), af, article)) {
			return;
		}

		//If we've never processed this then store the Omit, otherwise skip.
		if(StringUtil.isEmpty(article.getRssArticleId()))
			article.addFilteredText(af);

		long end = (System.currentTimeMillis()-start);
		if(end > 1000) {
			log.info("filter Processing took " + end + "ms for feedGroupId: " + feedGroupId);
		}
	}


	/**
	 * Perform Required Checks on the Article.
	 * @param rssFilters
	 * @param af
	 * @param article
	 * @return true if Required Filter is Triggered
	 */
	private boolean applyRequired(List<RSSFilterVO> rssFilters, RSSArticleFilterVO af, RSSArticleVO article) {
		boolean isRequired = false;
		for (RSSFilterVO filter: rssFilters) {
			if (checkReqMatch(af, filter)) {
				if(isRequired) {
					log.info("Matched twice");
				}
				// Null out FullArticleTxt to lessen memory overhead
				af.setFullArticleTxt(null);
				af.setArticleTxt(af.getFilterArticleTxt());
				af.setTitleTxt(af.getFilterTitleTxt());
				isRequired = true;
			} else {
				af.setArticleStatus(ArticleStatus.O);
				af.setFilterArticleTxt(null);
				af.setFilterTitleTxt(null);
				isRequired = false;
				break;
			}
		}

		if(isRequired) {
			af.setArticleTxt(article.getArticleTxt());
			af.setTitleTxt(article.getTitleTxt());
			article.addFilteredText(af);
		}
		return isRequired;
	}

	/**
	 * Perform Omission Checks on the Article.
	 * @param rssFilters
	 * @param af
	 * @param article
	 * @return true if OmitFilter is Triggered
	 */
	private boolean applyOmissionFilters(List<RSSFilterVO> rssFilters, RSSArticleFilterVO af, RSSArticleVO article) {
		boolean isOmitted = false;
		for (RSSFilterVO filter: rssFilters) {
			if (checkOmitMatch(af, filter)) {

				// Null out FullArticleTxt to lessen memory overhead
				af.setFullArticleTxt(null);
				article.addFilteredText(af);
				isOmitted = true;
			}
		}
		return isOmitted;
	}

	/**
	 * Check if we have a match on an Omit Filter.  If so, set Status Code R for Rejected.
	 * @param article
	 * @param filter
	 */
	protected boolean checkOmitMatch(RSSArticleFilterVO af, RSSFilterVO filter) {
		boolean isMatch = checkMatch(af, filter);

		if (isMatch)
			af.setArticleStatus(ArticleStatus.R);

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

		if (isMatch && !ArticleStatus.R.equals(af.getArticleStatus()))
			af.setArticleStatus(ArticleStatus.N);

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
		long start = System.currentTimeMillis();

		String regex = new StringBuilder(filter.getFilterExpression().length() + 10)
				.append("(?i)(").append(filter.getFilterExpression()).append(")").toString();

		af.setFilterArticleTxt(af.getArticleTxt().replaceAll(regex, replaceSpanText));
		af.setFilterTitleTxt(af.getTitleTxt().replaceAll(regex, replaceSpanText));

		//Build Matchers.
		if (!StringUtil.isEmpty(af.getFilterArticleTxt()) && !af.getFilterArticleTxt().equals(af.getArticleTxt())) {
			isMatch = true;
		} else if (!StringUtil.isEmpty(af.getFullArticleTxt())) {
			String filteredFull = af.getFullArticleTxt().replaceAll(regex, replaceSpanText);
			if (filteredFull.contains(SPAN_CLASS_HIT)) {
				isMatch = true;
				af.setMatchCount(af.getMatchCount() + filteredFull.split(SPAN_CLASS_HIT).length - 1);
			} else {
				af.setMatchCount(0);
			}
		}
		//if not already true, check one more place
		if (!isMatch && !af.getFilterTitleTxt().equals(af.getTitleTxt())) {
			isMatch = true;
		}
		long end = (System.currentTimeMillis()-start);
		if(end > 1000) {
			log.info("Regex Processing took " + end + "ms for filter: " + filter.getFilterGroupXrId());
		}

		return isMatch;
	}

	/**
	 * Return true if the Set<feedGroupId> for theis articleGuid contains a match
	 * @param article
	 * @param feedGroupId
	 * @param existingIds
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected boolean articleExists(RSSArticleVO article, String feedGroupId, Map<String, GenericVO> existsIds) {
		if (existsIds.containsKey(article.getArticleGuid())) {
			GenericVO info = existsIds.get(article.getArticleGuid());
			Set<String> grps = (Set<String>) info.getValue();
			article.setRssArticleId((String) info.getKey());
			return grps.contains(feedGroupId);
		}
		return false;
	}


	/**
	 * Puts the thread to sleep if we haven't waited at least a minimum amount of time between USPTO queries
	 * Calculate a wait time based on the last time we queried them.  If less than the threshold, put our thread to sleep.
	 */
	protected void throttleRequests(String url) {
		String domain = StringUtil.stripProtocol(url);
		domain = domain.substring(0, domain.indexOf('/'));

		Long lastAccessTime = accessTimes.get(domain);
		if (lastAccessTime == null) lastAccessTime = Long.valueOf(0);
		log.debug("domain from URL= " + domain + " last access=" + lastAccessTime);

		long mustWaitTime = System.currentTimeMillis() - lastAccessTime;
		if (mustWaitTime > 0 && mustWaitTime < LAG_TIME_MS) {
			try {
				//sleep the remaining time to get us to the threshold
				log.debug("sleeping for " + (LAG_TIME_MS - mustWaitTime));
				Thread.sleep(LAG_TIME_MS - mustWaitTime);
			} catch (Exception e) {
				//don't care - this would bubble up as runtime issues anyways
			}
		}
		accessTimes.put(domain, System.currentTimeMillis());
	}

	/**
	 * @return the feedName
	 */
	public String getFeedName() {
		return feedName;
	}

	/**
	 * @param feedName the feedName to set
	 */
	public void setFeedName(String feedName) {
		this.feedName = feedName;
	}

}