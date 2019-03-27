package com.biomed.smarttrak.action.rss.util;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;

import com.biomed.smarttrak.action.rss.vo.RSSArticleVO;
import com.biomed.smarttrak.action.rss.vo.RSSArticleVO.ArticleSourceType;
import com.biomed.smarttrak.action.rss.vo.RSSFeedGroupVO;
import com.quertle.webservice.base.HitcountOption;
import com.quertle.webservice.base.ResultAttributes;
import com.quertle.webservice.base.SearchParams;
import com.quertle.webservice.base.SearchParams.PostFilters;
import com.quertle.webservice.base.SearchParams.PostFilters.Entry;
import com.quertle.webservice.base.SearchWSImplementationsService;
import com.quertle.webservice.base.SearchingSEI;
import com.quertle.webservice.base.WSCheckFaultException;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.search.SMTIndexIntfc;

/****************************************************************************
 * <b>Title:</b> QuertleDataFeed.java <b>Project:</b> WC_Custom
 * <b>Description:</b> Data Parser responsible for interfacing to Quertle
 * System. <b>Copyright:</b> Copyright (c) 2017 <b>Company:</b> Silicon Mountain
 * Technologies
 * 
 * @author Billy Larsen
 * @version 3.0
 * @since May 19, 2017
 ****************************************************************************/
public class QuertleDataFeed extends AbstractSmarttrakRSSFeed {

	public static final String AUTHORIZATION_CODE_KEY = "AuthorizationCode";
	public static final String PARTNER_CODE_KEY = "partnerCode";
	public static final String WSDL_URL_STRING = "wsdlUrlString";
	public static final String PATENT_APPLICATION_TYPE = "patentApplicationType";
	public static final String PATENT_GRANT_TYPE = "patentGrantType";
	public static final String DEF_DATE_RANGE = "defaultDateRange";
	public static final String CLASSIFICATIONS = "classifications";
	public static final String HITS_PER_QUERY = "defaultHitsPerQuery";
	public static final String QUERTLE_ENTITY_ID = "quertleEntityId";
	private static final QName SERVICE_NAME = new QName("http://base.webservice.quertle.com/", "SearchWSImplementationsService");

	/**
	 * @param index 
	 * @param args
	 */
	public QuertleDataFeed(Connection dbConn, Properties props, SMTIndexIntfc index) {
		super(dbConn,props,index);
		feedName = "Quertle RSS Feed";
	}


	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	public void run() {
		//Load the filters
		loadFilters(props.getProperty(QUERTLE_ENTITY_ID));

		//Build the configured Port
		SearchingSEI port = buildPort();

		//If Port is null, we can't connect to Quertle System.
		if (port == null) {
			log.fatal("Can not connect to Quertle");
			return;
		}

		//Load Patent Applications
		getPatentsFromQuertle(port, props.getProperty(PATENT_APPLICATION_TYPE));
		log.info("loaded Patent Applications.");

		//Load Patent Grants
		getPatentsFromQuertle(port, props.getProperty(PATENT_GRANT_TYPE));
		log.info("loaded Patent Grants.");
	}


	/**
	 * Method manages iterating Classifications and calling out to Quertle for
	 * Results.
	 * 
	 * @param port
	 * @param filters 
	 * @param sp
	 */
	private void getPatentsFromQuertle(SearchingSEI port, String searchType) {
		String[] classifications = props.getProperty(CLASSIFICATIONS).split(",");
		SearchParams sp = initializeSearchParams(searchType, null);
		int patentCnt = 0;

		//Call to Quertle for each Classification
		for (String classy : classifications) {
			//Load Results for the Classification
			List<ResultAttributes> results = loadArticlesFromVendor(port, sp, classy);
			log.info("retrieved " + results.size() + " records from vendor for class=" + classy);

			//If we have results, process them.
			if (!results.isEmpty()) {
				int existingCnt = processResults(results, searchType);
				patentCnt += results.size() - existingCnt; //increment our 'new' count using #found - #existing
			}
		}
		log.info("loaded " + patentCnt + " new patents from Quertle for type=" + searchType);
	}


	/**
	 * Process the Given Quertle ResultAttributes list and convert to RSSArticleVOs.  
	 * Apply Filtering to VO before returning.
	 * @param results
	 */
	private int processResults(List<ResultAttributes> results, String searchType) {
		// Load existing Article Ids for Quertle.
		Map<String, GenericVO> articleGuids = getExistingIds(searchType, results);
		log.info("found " + articleGuids.size() + " existing articles for type=" + searchType + " and loaded each one's feedGroupIds");

		RSSArticleVO article;
		// Iterate Results and Builds Article VOs.
		for (ResultAttributes resultAttrs : results) {
			long start = System.currentTimeMillis();
			String id = searchType + resultAttrs.getApplicationNumber();

			article = buildArticleVO(id, searchType, resultAttrs);
			log.info("article VO took " + (System.currentTimeMillis()-start) + "ms");

			applyFilters(article, articleGuids);
			log.info("article filters took " + (System.currentTimeMillis()-start) + "ms");

			// Remove the Full Article Text to lessen memory overhead.
			article.setFullArticleTxt(null);

			if (!article.getFilterVOs().isEmpty()) {
				storeArticle(article);
			} else {
				log.info("************** article did not match filters, discarding ****************");
			}
			log.info("article took " + (System.currentTimeMillis()-start) + "ms");
		}
		// return the # of existing records, so we can report how many new ones we found in the logs.
		return articleGuids.size();
	}


	/**
	 * Iterate over the FeedGroups and create an article VO for each.
	 * Quertle results can appear in multiple groups per business requirements.
	 * @param a
	 * @param ids 
	 */
	private void applyFilters(RSSArticleVO a, Map<String, GenericVO> articleGuids) {
		for (RSSFeedGroupVO g : groups) {
			if (! articleExists(a, g.getFeedGroupId(), articleGuids)) {
				//Apply Matching Filters to article.
				applyFilter(a, g.getFeedGroupId(), true);
			}
		}
	}


	/**
	 * Parse and build and filter an RSSArticleVO with the given information.
	 * @param id
	 * @param searchType
	 * @param g
	 * @return
	 */
	private RSSArticleVO buildArticleVO(String articleGuid, String searchType, ResultAttributes resultAttrs) {
		//Instantiate the Article and set common fields.
		RSSArticleVO article = new RSSArticleVO();
		article.setTitleTxt(resultAttrs.getTitle().replace("\u00a0"," "));
		article.setArticleTxt(resultAttrs.getAbstractText().replace("\u00a0"," "));
		article.setPublishDt(getPubDate(resultAttrs.getPubDate()));
		article.setArticleGuid(articleGuid);
		article.setRssEntityId(props.getProperty(QUERTLE_ENTITY_ID));
		article.setArticleSourceType(ArticleSourceType.QUERTLE);

		//Set special attributes based on Application or Grant Type.
		if (searchType.equals(props.get(PATENT_APPLICATION_TYPE))) {
			article.setArticleUrl(resultAttrs.getPAppHtmlLink());
			//Set PDF Link
			article.setAttribute1Txt(resultAttrs.getPAppPdfLink());
		} else {
			article.setArticleUrl(resultAttrs.getPGrantHtmlLink());
			//Set PDF Link - for patents we just capture the patent# - the JSP/View prepends the rest of the URL.
			if (articleGuid.startsWith("p10")) {
				article.setAttribute1Txt(resultAttrs.getPatentNumber());
			} else {
				article.setAttribute1Txt(resultAttrs.getPGrantPdfLink());
			}
		}

		article.setFullArticleTxt(loadArticle(article.getArticleUrl()));
		return article;
	}


	/**
	 * Format the Quertle Publish Date String.
	 * @param pubDate
	 * @return
	 */
	private Date getPubDate(String pubDate) {
		if (StringUtil.isEmpty(pubDate)) {
			return Convert.getCurrentTimestamp();
		} else {
			String d = pubDate.replace(" ", "-").replace("\"", "");
			return Convert.formatDate(d);
		}
	}


	/**
	 * Build a list of ids and check for existing guids in the system.
	 * @param results
	 * @return
	 */
	private Map<String, GenericVO> getExistingIds(String searchType, List<ResultAttributes> results) {
		List<String> articleGuids = new ArrayList<>(results.size());
		for (ResultAttributes resultAttrs : results) {
			articleGuids.add(searchType + resultAttrs.getApplicationNumber());
		}
		return super.getExistingArticles(articleGuids, props.getProperty(QUERTLE_ENTITY_ID));
	}


	/**
	 * Query Quertle for results matching the given searchparams and classification.
	 * @param service
	 * @param sp
	 * @param c
	 * @return
	 */
	private List<ResultAttributes> loadArticlesFromVendor(SearchingSEI port, SearchParams sp, String c) {
		List<ResultAttributes> results = Collections.emptyList();
		int cnt = Convert.formatInteger(props.getProperty(HITS_PER_QUERY));

		//Set the passed classification.
		sp.setInternationalClassification(c);
		try {

			//Query for Results.
			List<HitcountOption> hits = port.resultHitcounts(sp, 0, cnt);

			//Check if we have results.
			if (!hits.isEmpty() && hits.size() > 1) {

				// The results we care about are described by the object at index 1.
				HitcountOption h = hits.get(1);

				//Instantiate Results List with proper size.
				results = new ArrayList<>(h.getHitCount() + 1);

				//Add All of the current results.
				results.addAll(port.resultElements(null));
				int resCnt = cnt;

				// Continue to Query the system and store results until we've gotten all the results.
				while (h.getHitCount() > resCnt) {
					//Query for next batch of results.
					port.resultHitcounts(sp, resCnt, cnt);

					//Add results to list.
					results.addAll(port.resultElements(null));

					//Increment result count.
					resCnt += cnt;
				}
			}
		} catch(WSCheckFaultException ws) {
			log.error("Could not load articles", ws);
		} catch (Exception e) {
			log.error("Error Processing Code", e);
		}

		return results;
	}


	/**
	 * Initialize Default Search Parameters for Queries.
	 * @return
	 */
	private SearchParams initializeSearchParams(String type, String dr) {
		String dateRange = StringUtil.checkVal(dr, props.getProperty(DEF_DATE_RANGE));
		SearchParams sp = new SearchParams();
		PostFilters pf = new PostFilters();
		Entry e = new Entry();
		e.setKey("value");
		e.setValue(StringUtil.join("f=", type, "|", dateRange));
		pf.getEntry().add(e);
		sp.setPostFilters(pf);
		sp.setQuery(" ");
		return sp;
	}


	/**
	 * Build and prepare the Quertle Service.
	 * 
	 * @return
	 */
	public SearchingSEI buildPort() {
		URL wsdlURL = buildUrl();
		if (wsdlURL == null) return  null;

		//Create the Service.
		SearchWSImplementationsService ss = new SearchWSImplementationsService(wsdlURL, SERVICE_NAME);

		//Get the Port.  Case to a BindingProvider per Quertle documentation.
		BindingProvider port = (BindingProvider) ss.getSearchWSImplementationsPort();

		String url = wsdlURL.toString();

		//Get the Request context Map for the Binding Port.
		Map<String, Object> ctx = port.getRequestContext();
		ctx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);

		// User Authentication using partner code and authentication code
		ctx.put(MessageContext.HTTP_REQUEST_HEADERS, buildHeaders());

		// Session maintenance by providing it in the header
		port.getRequestContext().put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);
		port.getRequestContext().put("com.sun.xml.ws.request.timeout", 10 * 60 * 1000);

		//Cast back and return.
		return (SearchingSEI)port;
	}


	/**
	 * Build HTTP Request Headers Map.  PartnerCode and AuthorizationCode allow
	 * access through firewall.  Need to ensure IP Address is whitelisted in
	 * Quertle Systems before testing/deployment.
	 * 
	 * @return
	 */
	private Map<String, List<String>> buildHeaders() {
		Map<String, List<String>> headers = new HashMap<>();
		headers.put(PARTNER_CODE_KEY, Collections.singletonList(props.getProperty(PARTNER_CODE_KEY)));
		headers.put(AUTHORIZATION_CODE_KEY, Collections.singletonList(props.getProperty(AUTHORIZATION_CODE_KEY)));
		return headers;
	}


	/**
	 * Build URL for the WSDL File.
	 * 
	 * @return
	 */
	private URL buildUrl() {
		try {
			return new URL(props.getProperty(WSDL_URL_STRING));
		} catch (Exception e) {
			log.error("Can not initialize the default wsdl from " + props.getProperty(WSDL_URL_STRING));
			return null;
		}
	}

	/**
	 * Load the page related to the given url.
	 * @param articleUrl
	 * @return
	 */
	private String loadArticle(String url) {
		if (StringUtil.isEmpty(url)) return null;
		SMTHttpConnectionManager conn = createBaseHttpConnection();

		try {
			throttleRequests(url);
			
			//do special things for the US Patent Office, who thinks we're DDoS'ing them
			if (url.matches("(?i)https?://(app|pat)ft\\.uspto\\.gov/(.*)")) {
				//log.info("USPTO URL: " + url) //used to verify regex above
				conn.addCookie("TSPD_101", ":");
			}

			byte[] data = conn.retrieveData(url);

			//trap all errors generated by LL
			if (404 == conn.getResponseCode())
				return null;

			if (200 != conn.getResponseCode())
				throw new IOException("Transaction Unsuccessful, code=" + conn.getResponseCode());

			if (data != null)
				return new String(data);

		} catch (Exception ioe) {
			log.error("could not load data from url=" + url, ioe);
		}
		return null;
	}
}