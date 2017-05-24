package com.biomed.smarttrak.action.rss.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.action.rss.RSSDataAction.ArticleStatus;
import com.biomed.smarttrak.action.rss.RSSFilterAction.FilterType;
import com.biomed.smarttrak.action.rss.vo.RSSArticleVO;
import com.biomed.smarttrak.action.rss.vo.RSSFeedGroupVO;
import com.biomed.smarttrak.action.rss.vo.RSSFilterVO;
import com.biomed.smarttrak.action.rss.vo.SmarttrakRssEntityVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.smt.sitebuilder.common.constants.Constants;

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
	/**
	 *
	 */
	public RSSDataFeed(String... args) {
		super(args);
		factory = SAXParserFactory.newInstance();
		try {
			saxParser = factory.newSAXParser();
		} catch (ParserConfigurationException | SAXException e) {
			log.error("Error Instantiating Sax Parser", e);
		}
	}


	/**
	 * @param args
	 */
	public static void main(String... args) {
		RSSDataFeed rdf = new RSSDataFeed(args);
		rdf.run();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		//Load Feeds
		List<SmarttrakRssEntityVO> feeds = loadFeeds();
		//Load Filters
		Map<String, RSSFeedGroupVO> filters = loadFilters();
		//Process Feeds.
		process(feeds, filters);
	}

	/**
	 * Process RSS Feeds for given Schedule.
	 * @param scheduleNo
	 */
	public void process(List<SmarttrakRssEntityVO> feeds, Map<String, RSSFeedGroupVO> filters) {
		for(SmarttrakRssEntityVO f : feeds) {
			if(!f.getRssEntityId().equals(props.get(PUBMED_ENTITY_ID))) {
				try {
					List<RSSArticleVO> articles = retrieveArticles(f.getRssUrl());
					filterArticles(f, articles, filters);
				} catch (Exception e) {
					log.info("Deactivating Feed: " + f.getRssEntityId());
					log.error("Problem Processing Feed", e);
					updateFeed(f);
				}
			}
		}
	}

	/**
	 * Method retrieves Article text for the given article Id.
	 * @param id
	 * @return
	 */
	private List<RSSArticleVO> retrieveArticles(String url) {
		byte[] results = getDataViaHTTP(url, null);

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
		List<RSSArticleVO> articles = null;

		try {
			InputStream is = new ByteArrayInputStream(results);
			RSSArticleSaxHandler handler = new RSSArticleSaxHandler();
			saxParser.parse(is, handler);
			articles = handler.getVos();
		} catch(SAXException | IOException se) {
			log.error("Problem Processing Pubmed Articles", se);
		}
		return articles;
	}

	/**
	 * Method manages applying filters to each of the messages in a feed.
	 * @param f 
	 * @param feed
	 * @param filters
	 */
	private void filterArticles(SmarttrakRssEntityVO f, List<RSSArticleVO> articles, Map<String, RSSFeedGroupVO> filters) {
		//Query if any of the retrieved articles are already processed.
		Set<String> existsIds = getExistingArticles(buildArticleIdsList(articles), f.getRssEntityId());

		/**
		 * Iterate over each Message in the Feed and apply all filters in the
		 * related groups to the message.
		 */
		List<RSSArticleVO> nArticles = new ArrayList<>();
		for(RSSArticleVO a : articles) {
			if(!existsIds.contains(a.getArticleGuid())) {
				a.setRssEntityId(f.getRssEntityId());
				a.setArticleStatusCd(ArticleStatus.O.name());
				a.setPublicationName(f.getFeedName());
				if(a.getPublishDt() == null)
					a.setPublishDt(Calendar.getInstance().getTime());
				for(RSSFeedGroupVO fg : f.getGroups()) {
					RSSFeedGroupVO g = filters.get(fg.getFeedGroupId());
					matchArticle(a, g.getFilters());
					a.setFeedGroupId(g.getFeedGroupId());
				}
				nArticles.add(a);
			}
		}

		//Save Articles.
		storeArticles(nArticles);
	}


	/**
	 * Method converts List of RSSArticleVOs into a list of String ids.
	 * @param articles
	 * @return
	 */
	private List<String> buildArticleIdsList(List<RSSArticleVO> articles) {
		List<String> articleIds = new ArrayList<>();
		for(RSSArticleVO rss : articles) {
			articleIds.add(rss.getArticleGuid());
		}
		return articleIds;
	}


	/**
	 * Apply Filters to the articles.  Depending on if it is a Required or Omit
	 * filter, perform desired workflow.
	 * @param article
	 * @param filters
	 */
	private void matchArticle(RSSArticleVO article, List<RSSFilterVO> filters) {
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
	private void checkOmitMatch(RSSArticleVO article, RSSFilterVO filter) {
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
	private void checkReqMatch(RSSArticleVO article, RSSFilterVO filter) {
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
	private boolean checkMatch(RSSArticleVO article, RSSFilterVO filter) {
		boolean isMatch = false; 

		article.setFilterArticleTxt(article.getArticleTxt().replaceAll(filter.getFilterExpression(), props.getProperty(REPLACE_SPAN)));
		article.setFilterTitleTxt(article.getTitleTxt().replaceAll(filter.getFilterExpression(), props.getProperty(REPLACE_SPAN)));

		//Build Matchers.
		if(article.getFilterArticleTxt().contains("<span class='f-match'>")) {
			isMatch = true;
		}
		if(article.getFilterTitleTxt().contains("<span class='f-match'>")) {
			isMatch = true;
		}

		return isMatch;
	}

	/**
	 * Retrieves all the Feeds for Biomedgps smarttrak.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Map<String, RSSFeedGroupVO> loadFilters() {
		Map<String, RSSFeedGroupVO> groupMap = new HashMap<>();
		DBProcessor dbp = new DBProcessor(dbConn, props.getProperty(Constants.CUSTOM_DB_SCHEMA));
		List<RSSFeedGroupVO> groups = (List<RSSFeedGroupVO>)(List<?>)dbp.executeSelect(getFiltersSql(), null, new RSSFeedGroupVO());
		for(RSSFeedGroupVO g : groups) {
			groupMap.put(g.getFeedGroupId(), g);
		}

		return groupMap;
	}


	/**
	 * Builds Feeds Info Sql Query.
	 * @return
	 */
	private String getFiltersSql() {
		String schema = props.getProperty(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(450);
		sql.append("select g.feed_group_id, f.filter_expression, f.filter_type_cd ");
		sql.append("from ").append(schema).append("biomedgps_feed_group g ");
		sql.append("inner join ").append(schema).append("biomedgps_feed_filter_group_xr gxr ");
		sql.append("on g.feed_group_id = gxr.feed_group_id ");
		sql.append("inner join ").append(schema).append("biomedgps_rss_parser_filter f ");
		sql.append("on f.filter_id = gxr.filter_id ");
		sql.append("order by g.feed_group_id, f.filter_type_cd ");
		return sql.toString();
	}


	/**
	 * Retrieves all the Feeds for Biomedgps smarttrak.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<SmarttrakRssEntityVO> loadFeeds() {
		List<Object> vals = new ArrayList<>();
		vals.add(AdminControllerAction.BIOMED_ORG_ID);

		DBProcessor dbp = new DBProcessor(dbConn, props.getProperty(Constants.CUSTOM_DB_SCHEMA));
		return (List<SmarttrakRssEntityVO>)(List<?>) dbp.executeSelect(getFeedsSql(), vals, new SmarttrakRssEntityVO());
	}


	/**
	 * Builds Feeds Info Sql Query.
	 * @return
	 */
	private String getFeedsSql() {
		String schema = props.getProperty(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(375);
		sql.append("select e.rss_entity_id, e.rss_url, e.rss_feed_nm, fsg.feed_group_id ");
		sql.append("from rss_entity e inner join ").append(schema).append("biomedgps_rss_entity bre ");
		sql.append("on e.rss_entity_id = bre.rss_entity_id ");
		sql.append("inner join ").append(schema).append("biomedgps_feed_source_group_xr fsg ");
		sql.append("on bre.rss_entity_id = fsg.rss_entity_id ");
		sql.append("where e.organization_id = ? and e.is_active = 1");
		return sql.toString();
	}
}