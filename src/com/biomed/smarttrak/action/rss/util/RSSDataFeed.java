package com.biomed.smarttrak.action.rss.util;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.action.rss.vo.RSSArticleVO;
import com.biomed.smarttrak.action.rss.vo.RSSFeedGroupVO;
import com.biomed.smarttrak.action.rss.vo.SmarttrakRssEntityVO;
import com.ernieyu.feedparser.Feed;
import com.ernieyu.feedparser.FeedParser;
import com.ernieyu.feedparser.FeedParserFactory;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title:</b> RSSDataFeed.java
 * <b>Project:</b> WebCrescendo
 * <b>Description:</b> Command Line Utility for ingesting new RSS Data Feeds.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 1.0
 * @since Apr 18, 2017
 ****************************************************************************/
public class RSSDataFeed extends AbstractSmarttrakRSSFeed {
	private FeedParser feedParser;

	public RSSDataFeed(Connection dbConn, Properties props) {
		super(dbConn, props);
		feedParser = FeedParserFactory.newParser();
		feedName = "RSS Feed";
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		//Load Feeds
		List<SmarttrakRssEntityVO> feeds = loadFeeds();
		//Load Filters
		loadFilters(null);
		//Process Feeds.
		process(feeds);
	}


	/**
	 * Process RSS Feeds for given Schedule.
	 * @param scheduleNo
	 */
	public void process(List<SmarttrakRssEntityVO> feeds) {
		for(SmarttrakRssEntityVO f : feeds) {
			//for some reason PubMed gets picked up here - skip over it.
			if (f.getRssEntityId().equals(props.get(PUBMED_ENTITY_ID))) continue;

			try {
				List<RSSArticleVO> articles = retrieveArticles(f.getRssUrl(), f.getRssEntityId());
				filterArticles(f, articles);
			} catch (Exception e) {
				log.error("Problem Processing Feed", e);
			}
		}
	}


	/**
	 * Method retrieves Article text for the given article Id.
	 * @param rssEntityId
	 * @param id
	 * @return
	 */
	private List<RSSArticleVO> retrieveArticles(String url, String rssEntityId) {
		log.info("Retrieving Url: " + url);
		byte[] results = getDataViaHttp(url, null);

		//Process XML
		return processArticleResult(results, rssEntityId);
	}


	/**
	 * Method Processes Data Stream containing article list and Converts it to
	 * a list of RSSArticleVO.
	 * @param results
	 * @param rssEntityId
	 * @return
	 */
	private List<RSSArticleVO> processArticleResult(byte[] results, String rssEntityId) {
		List<RSSArticleVO> articles = Collections.emptyList();

		if (results == null || results.length == 0) return articles;

		try {
			Feed feed = feedParser.parse(new ByteArrayInputStream(results));
			articles = convertFeed(feed, rssEntityId);
		} catch(Exception se) {
			log.error("Response was malformed: " + se.getMessage());
		}
		log.info("Loaded " + articles.size() + " articles.");
		return articles;
	}

	/**
	 * Convert the given Feed Object to our RSSArticleVO List Structure
	 * and return it.
	 * @param feed
	 * @param rssEntityId
	 * @return
	 */
	private List<RSSArticleVO> convertFeed(Feed feed, String rssEntityId) {
		log.debug(feed.getType());

		List<RSSArticleVO> articles = feed.getItemList().stream().map(RSSArticleVO::new).collect(Collectors.toList());

		//Match Articles Retrieved against db articles to get proper Dates and Article Id for existing records.
		matchArticles(articles);

		log.debug("Retrieved " + articles.size() + " Articles.");
		int initialSize = articles.size();
			articles = articles.stream().filter(a -> a.getPublishDt() != null ? a.getPublishDt().after(cutOffDate) : true).collect(Collectors.toList());
		int diff = initialSize - articles.size();
		if(diff != 0) {
			log.debug("Removed " + diff + "articles");
			this.addMessage(String.format("Feed: %s - PubDate Filter Removed %d articles.", rssEntityId, diff));
		}
		log.debug(articles.size() + " articles remaining after 30 day filter.");
		return articles;
	}


	/**
	 * Before we begine processing, lookup articles and attempt to set rss_Article_id
	 * and publish_dt before we process.  Ensures that if there is no publish_dt
	 * parsed from the Feed, we use what we have in the system which would be
	 * ingestion date.  Prevents non-dated articles that are historically out
	 * dated from getting indexed again.
	 * @param articles
	 */
	protected void matchArticles(List<RSSArticleVO> articles) {
		Map<String, RSSArticleVO> temp = new HashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(getArticleMatchSql(articles.size()))) {
			int i = 0;

			for (RSSArticleVO a: articles) {
				ps.setString(++i, a.getArticleGuid());
				temp.put(a.getArticleGuid(), a);
			}

			ResultSet rs = ps.executeQuery();
			RSSArticleVO a = null;
			while (rs.next()) {
				a = temp.get(rs.getString("key"));
				a.setPublishDt(rs.getDate("publish_dt"));
				a.setRssArticleId(rs.getString("rss_article_id"));
			}
		} catch (SQLException e) {
			log.error("error checking if RSS article exists", e);
		}
	}

	/**
	 * Build Article Match Query.
	 * @param size
	 * @return
	 */
	private String getArticleMatchSql(int size) {
		StringBuilder sql = new StringBuilder(500);
		sql.append("select a.rss_article_id, a.article_guid as key, a.publish_dt from ").append(customDb);
		sql.append("BIOMEDGPS_RSS_ARTICLE a ");
		sql.append("where a.article_guid in (");
		DBUtil.preparedStatmentQuestion(size, sql);
		sql.append(") order by a.article_guid");
		return sql.toString();
	}

	/**
	 * Method manages applying filters to each of the messages in a feed.
	 * Query if any of the retrieved articles are already processed.
	 * @param f 
	 * @param feed
	 * @param filters
	 */
	private void filterArticles(SmarttrakRssEntityVO f, List<RSSArticleVO> articles) {
		if (articles.isEmpty()) return;

		Map<String, GenericVO> existsIds = getExistingArticles(buildArticleIdsList(articles));
		articles.stream().forEach(a -> this.populateFeed(a, f));
		processArticles(f, articles, existsIds);
	}


	/**
	 * Set EntityId, PublicationName and Publish Date on the given Article.
	 * @param a
	 * @param f
	 */
	private void populateFeed(RSSArticleVO a, SmarttrakRssEntityVO f) {
		a.setRssEntityId(f.getRssEntityId());
		a.setPublicationName(f.getFeedName());
		if (a.getPublishDt() == null) {
			a.setPublishDt(Calendar.getInstance().getTime());
		}
	}


	/**
	 * Iterate over each Message in the Feed and apply all filters in the
	 * related groups to the message.
	 * @param f
	 * @param articles
	 * @param existsIds
	 * @return
	 */
	private void processArticles(SmarttrakRssEntityVO f, List<RSSArticleVO> articles, Map<String, GenericVO> existsIds) {
		long start = System.currentTimeMillis();
		this.addMessage(String.format("Processing Feed Url: %s</br>", f.getRssUrl()));
		for (RSSArticleVO article : articles) {
			processArticleFeedGroups(f, article, existsIds);
		}
		this.addMessage("Finished Processing Feed</br>");
		log.info("article Processing took " + (System.currentTimeMillis()-start) + "ms");
	}


	/**
	 * Iterate over all the Feedgroups in a given Entity and determine if we need
	 * to process an article for it.  If any articles are processed, persist them
	 * to the database using storeArticle.
	 * @param f 
	 * @param article
	 * @param existsIds
	 */
	private void processArticleFeedGroups(SmarttrakRssEntityVO f, RSSArticleVO article, Map<String, GenericVO> existsIds) {
		log.debug(String.format("Processing Article: %s, Title: %s", article.getArticleUrl(), article.getTitleTxt()));
		for (RSSFeedGroupVO fg : f.getGroups()) {
			processArticle(f, fg, article, existsIds);
		}

		if (!article.getFilterVOs().isEmpty()) {
			//Save Articles.
			storeArticle(article);

			//After article is saved, we can flush out the data.  Prevent carrying around a lot of data.
			article.flushFilteredText();
		}
	}


	/**
	 * Process an articles through an individual Feed Group.  If the articles already
	 * exists for the FeedGroup, log it but don't filter it.
	 * @param f
	 * @param fg
	 * @param article
	 * @param existsIds
	 */
	private void processArticle(SmarttrakRssEntityVO f, RSSFeedGroupVO fg, RSSArticleVO article, Map<String, GenericVO> existsIds) {
		//Only process new Articles that aren't in system.  If an articleId is present, this article exists.
		if (!articleExists(article, fg.getFeedGroupId(), existsIds)) {
			applyFilter(article, fg.getFeedGroupId(), fg.getFeedGroupNm(), Convert.formatBoolean(f.getUseFiltersNo()));
			log.debug(String.format("Processed Feed Group: %s", fg.getFeedGroupId()));
		} else {

			//Log if the article exists but we have matched un-processed feed groups.
			if(!article.getFilterVOs().isEmpty()) {
				log.debug("Skipping Group but have articles!");
			}

			log.info(String.format("Article Already Processed for Feed Group: %s", fg.getFeedGroupId()));
		}
	}


	/**
	 * Method converts List of RSSArticleVOs into a list of String ids.
	 * @param articles
	 * @return
	 */
	private List<String> buildArticleIdsList(List<RSSArticleVO> articles) {
		List<String> articleIds = new ArrayList<>();
		for (RSSArticleVO rss : articles) {
			articleIds.add(rss.getArticleGuid());
		}
		return articleIds;
	}


	/**
	 * Retrieves all the Feeds for Biomedgps smarttrak.
	 * @return
	 */
	private List<SmarttrakRssEntityVO> loadFeeds() {
		List<Object> vals = new ArrayList<>();
		vals.add(AdminControllerAction.BIOMED_ORG_ID);

		DBProcessor dbp = new DBProcessor(dbConn, customDb);
		return dbp.executeSelect(getFeedsSql(), vals, new SmarttrakRssEntityVO());
	}


	/**
	 * Builds Feeds Info Sql Query.
	 * @return
	 */
	private String getFeedsSql() {
		StringBuilder sql = new StringBuilder(475);
		sql.append("select e.rss_entity_id, e.rss_url, e.rss_feed_nm, fsg.feed_group_id, fg.feed_group_nm, e.use_filters_no ");
		sql.append("from rss_entity e inner join ").append(customDb).append("biomedgps_rss_entity bre ");
		sql.append("on e.rss_entity_id = bre.rss_entity_id ");
		sql.append("inner join ").append(customDb).append("biomedgps_feed_source_group_xr fsg ");
		sql.append("on bre.rss_entity_id = fsg.rss_entity_id ");
		sql.append("inner join ").append(customDb).append("biomedgps_feed_group fg ");
		sql.append("on fg.feed_group_id = fsg.feed_group_id ");
		sql.append("where e.organization_id = ? ");
		return sql.toString();
	}
}