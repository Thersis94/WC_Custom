package com.biomed.smarttrak.action.rss.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.action.rss.vo.RSSArticleVO;
import com.biomed.smarttrak.action.rss.vo.RSSFeedGroupVO;
import com.biomed.smarttrak.action.rss.vo.SmarttrakRssEntityVO;

import com.siliconmtn.db.orm.DBProcessor;

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
	private SAXParserFactory factory;
	private SAXParser saxParser;

	public RSSDataFeed(Connection dbConn, Properties props) {
		super(dbConn, props);
		factory = SAXParserFactory.newInstance();
		try {
			saxParser = factory.newSAXParser();
		} catch (ParserConfigurationException | SAXException e) {
			log.error("Error Instantiating Sax Parser", e);
		}
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
				List<RSSArticleVO> articles = retrieveArticles(f.getRssUrl());
				filterArticles(f, articles);
			} catch (Exception e) {
				log.error("Problem Processing Feed", e);
			}
		}
	}


	/**
	 * Method retrieves Article text for the given article Id.
	 * @param id
	 * @return
	 */
	private List<RSSArticleVO> retrieveArticles(String url) {
		log.info("Retrieving Url: " + url);
		byte[] results = getDataViaHttp(url, null);

		//Process XML
		return processArticleResult(results);
	}


	/**
	 * Method Processes Data Stream containing article list and Converts it to
	 * a list of RSSArticleVO.
	 * @param results
	 * @return
	 */
	private List<RSSArticleVO> processArticleResult(byte[] results) {
		List<RSSArticleVO> articles = Collections.emptyList();
		if (results == null || results.length == 0) return articles;

		try {
			InputStream is = new ByteArrayInputStream(results);
			RSSArticleSaxHandler handler = new RSSArticleSaxHandler();
			saxParser.parse(is, handler);
			articles = handler.getVos();
		} catch(Exception se) {
			log.error("Response was malformed: " + se.getMessage());
		}
		log.info("Loaded " + articles.size() + " articles.");
		return articles;
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

		Map<String, Set<String>> existsIds = getExistingArticles(buildArticleIdsList(articles), f.getRssEntityId());
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
	private void processArticles(SmarttrakRssEntityVO f, List<RSSArticleVO> articles, Map<String, Set<String>> existsIds) {
		for (RSSArticleVO article : articles) {
			for (RSSFeedGroupVO fg : f.getGroups()) {
				if (!articleExists(article.getArticleGuid(), fg.getFeedGroupId(), existsIds)) {
					applyFilter(article, fg.getFeedGroupId());
				}
			}
			if (!article.getFilterVOs().isEmpty()) {
				//Save Articles.
				storeArticle(article);
			}
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
		StringBuilder sql = new StringBuilder(375);
		sql.append("select e.rss_entity_id, e.rss_url, e.rss_feed_nm, fsg.feed_group_id ");
		sql.append("from rss_entity e inner join ").append(customDb).append("biomedgps_rss_entity bre ");
		sql.append("on e.rss_entity_id = bre.rss_entity_id ");
		sql.append("inner join ").append(customDb).append("biomedgps_feed_source_group_xr fsg ");
		sql.append("on bre.rss_entity_id = fsg.rss_entity_id ");
		sql.append("where e.organization_id = ? ");
		return sql.toString();
	}
}