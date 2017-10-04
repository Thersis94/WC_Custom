package com.biomed.smarttrak.action.rss.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import com.biomed.smarttrak.action.rss.RSSFilterAction.FilterType;
import com.biomed.smarttrak.action.rss.vo.RSSArticleVO;
import com.biomed.smarttrak.action.rss.vo.RSSFeedGroupVO;
import com.biomed.smarttrak.action.rss.vo.RSSFilterTerm;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: PubmedDataFeed.java
 * <b>Project</b>: WC_Custom
 * <b>Description</b>: CommandLineUtil for managing connecting to PubMed and
 * retrieving RSS Feed Data.
 * <b>Copyright</b>: Copyright (c) 2017
 * <b>Company</b>: Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 1.0
 * @since Apr 20, 2017
 ****************************************************************************/
public class PubmedDataFeed extends AbstractSmarttrakRSSFeed {

	private SAXParserFactory factory;
	private SAXParser saxParser;

	/**
	 * @param args
	 */
	public PubmedDataFeed(String[] args) {
		super(args);
		factory = SAXParserFactory.newInstance();
		try {
			saxParser = factory.newSAXParser();
		} catch (ParserConfigurationException | SAXException e) {
			log.error("Error Instantiating Sax Parser", e);
		}
	}

	public static void main(String... args) {
		PubmedDataFeed pdf = new PubmedDataFeed(args);
		pdf.run();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		//Retreive all Required Terms.
		List<RSSFilterTerm> reqs = retrieveRequiredTerms();

		//Retrieve all Omit Terms and group by feed group Id
		Map<String, List<RSSFilterTerm>> omits = retrieveOmitTerms();

		//For each Required Term
		for(RSSFilterTerm req : reqs) {
			//Build Query
			String query = buildPubmedQuery(req, omits.get(req.getFilterGroupId()));

			PubMedSearchResultVO results = getPubmedArticlesList(query);

			if(results != null && !results.getIdList().isEmpty()) {
				results.setQueryTerm(req);
				processArticleList(results);
			}
		}
	}

	/**
	 * Method queries out to Pubmed and retrieves list of PubMed Articles that
	 * match the query.
	 * @param query
	 * @return
	 */
	private PubMedSearchResultVO getPubmedArticlesList(String query) {
		byte[] results = getDataViaHTTP(props.getProperty("termsUrl"), buildTermsParams(query));

		//Process XML
		return processTermsResults(results);
	}

	/**
	 * Method builds Pubmed Filter Term Query.
	 * @param req
	 * @param list
	 * @return
	 */
	protected String buildPubmedQuery(RSSFilterTerm req, List<RSSFilterTerm> omits) {
		StringBuilder query = new StringBuilder(30 + (omits != null ? omits.size() * 30 : 0));
		query.append("\"").append(req.getFilterTerm()).append("\"[TIAB]");
		if(omits != null) {
			query.append(" NOT (");
			for(int i = 0; i < omits.size(); i++) {
				if(i > 0) {
					query.append(" OR ");
				}
				query.append("\"").append(omits.get(i).getFilterTerm()).append("\"[TIAB]");
			}
			query.append(")");
		}
		return query.toString();
	}

	/**
	 * Method returns List of required RSSFilterTerms.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected List<RSSFilterTerm> retrieveRequiredTerms() {
		List<Object> vals = new ArrayList<>();
		vals.add(FilterType.R.name());
		return (List<RSSFilterTerm>)(List<?>) new DBProcessor(dbConn, props.getProperty(Constants.CUSTOM_DB_SCHEMA)).executeSelect(getTermsSql(), vals, new RSSFilterTerm());
	}

	/**
	 * Method returns list of omit RSSFilterTerms.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, List<RSSFilterTerm>> retrieveOmitTerms() {
		Map<String, List<RSSFilterTerm>> terms = new HashMap<>();
		List<Object> vals = new ArrayList<>();
		vals.add(FilterType.O.name());

		List<RSSFeedGroupVO> groups = (List<RSSFeedGroupVO>)(List<?>) new DBProcessor(dbConn, props.getProperty(Constants.CUSTOM_DB_SCHEMA)).executeSelect(getTermsSql(), vals, new RSSFeedGroupVO());

		for(RSSFeedGroupVO g : groups)
			terms.put(g.getFeedGroupId(), g.getTerms());

		return terms;
	}

	/**
	 * Method builds the query for retrieving filter terms.
	 * @return
	 */
	protected String getTermsSql() {
		String schema = props.getProperty(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(300);
		sql.append("select g.feed_segment_id, t.* from ").append(schema).append("biomedgps_feed_group g ");
		sql.append("inner join ").append(schema).append("biomedgps_filter_term t on g.feed_group_id = t.feed_group_id ");
		sql.append("where filter_type_cd = ? order by g.feed_group_id;");
		return sql.toString();
	}

	/**
	 * Method manages retrieving full article data for PubMed Articles contained
	 * in the PubMedSearchResultVO.
	 * @param results
	 */
	protected void processArticleList(PubMedSearchResultVO vo) {
		Set<String> existingIds = super.getExistingArticles(vo.getIdList(), props.getProperty(PUBMED_ENTITY_ID));
		List<RSSArticleVO> articles = new ArrayList<>();

		//Retrieve Articles from Search.
		List<RSSArticleVO> results = retrieveArticles(vo);

		for(RSSArticleVO r : results) {
			if(!existingIds.contains(r.getArticleGuid())) {
				r.setArticleUrl(props.getProperty(PUBMED_ARTICLE_URL) + r.getArticleGuid());
				r.setRssEntityId(props.getProperty(PUBMED_ENTITY_ID));
				r.setFeedGroupId(vo.getReqTerm().getFilterGroupId());
				articles.add(highlightMatch(r, vo.getReqTerm()));
			}
		}

		//Save Pubmed Article
		super.storeArticles(articles);
	}

	/**
	 * Method manages highlighting the Matching Terms.
	 * @param r
	 * @param reqTerm
	 */
	private RSSArticleVO highlightMatch(RSSArticleVO r, RSSFilterTerm reqTerm) {
		StringBuilder filter = new StringBuilder(30);
		filter.append("(?i)(").append(reqTerm.getFilterTerm()).append(")");

		if(!StringUtil.isEmpty(r.getArticleTxt())) {
			r.setFilterArticleTxt(r.getArticleTxt().replaceAll(filter.toString(), props.getProperty(REPLACE_SPAN)));
			r.setFilterArticleTxt(r.getFilterArticleTxt().replaceAll("\n", "<br/>"));
		}

		if(!StringUtil.isEmpty(r.getTitleTxt())) {
			r.setFilterTitleTxt(r.getTitleTxt().replaceAll(filter.toString(), props.getProperty(REPLACE_SPAN)));
		}
		return r;
	}

	/**
	 * Method retrieves Article text for the given article Id.
	 * @param id
	 * @return
	 */
	private List<RSSArticleVO> retrieveArticles(PubMedSearchResultVO vo) {
		byte[] results = getDataViaHTTP(props.getProperty("docUrl"), buildDocParams(vo));

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
			PubmedArticleSaxHandler handler = new PubmedArticleSaxHandler();
			saxParser.parse(is, handler);
			articles = handler.getVos();
		} catch(SAXException | IOException se) {
			log.error("Problem Processing Pubmed Articles", se);
		}
		return articles;
	}

	/**
	 * Method Manages processing the Terms Search Result into a
	 * PubMedSearchResultVO.
	 * @param results
	 * @return
	 */
	protected PubMedSearchResultVO processTermsResults(byte[] results) {
		PubMedSearchResultVO vo = null;
		try {
			InputStream is = new ByteArrayInputStream(results);
			PubmedListSaxHandler handler = new PubmedListSaxHandler();
			saxParser.parse(is, handler);
			vo = handler.getVo();
		} catch(SAXException | IOException se) {
			log.error("Problem Processing Pubmed Search", se);
		}
		return vo;
	}

	/**
	 * Helper method that builds parameter map for a Terms Search Request.
	 * @param terms
	 * @return
	 */
	protected Map<String, Object> buildTermsParams(String terms) {
		Map<String, Object> params = new HashMap<>();
		params.put("db", props.getProperty("pubmedDb"));
		params.put("retmode", props.getProperty("pubmedRetMode"));
		params.put("retmax", props.getProperty("pubmedRetMax"));
		params.put("usehistory", props.getProperty("pubmedUseHistory"));
		params.put("datatype", props.getProperty("pubmeddatatype"));
		params.put("reldate", props.getProperty("pubmedsearchPeriodDays"));
		params.put("term", terms);
		return params;
	}

	/**
	 * Helper method that builds the parameter map for a Document Lookup Request.
	 * @param queryKey
	 * @param vo 
	 * @param webEnv
	 * @param retmax
	 * @return
	 */
	protected Map<String, Object> buildDocParams(PubMedSearchResultVO vo) {
		Map<String, Object> params = new HashMap<>();
		params.put("db", props.getProperty("pubmedDb"));
		params.put("query_key", Integer.toString(vo.getQueryKey()));
		params.put("retmode", props.getProperty("pubmedRetMode"));
		params.put("WebEnv", vo.getWebEnv());
		params.put("retmax", Integer.toString(vo.getRetMax()));

		return params;
	}
}