package com.biomed.smarttrak.action.rss.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;

import com.biomed.smarttrak.action.rss.vo.RSSArticleVO;
import com.biomed.smarttrak.action.rss.vo.RSSArticleVO.ArticleSourceType;
import com.biomed.smarttrak.action.rss.vo.RSSFeedGroupVO;
import com.quertle.webservice.base.ClassNotFoundException_Exception;
import com.quertle.webservice.base.Exception_Exception;
import com.quertle.webservice.base.HitcountOption;
import com.quertle.webservice.base.IOException_Exception;
import com.quertle.webservice.base.ParseException_Exception;
import com.quertle.webservice.base.ResultAttributes;
import com.quertle.webservice.base.SearchParams;
import com.quertle.webservice.base.SearchParams.PostFilters;
import com.quertle.webservice.base.SearchParams.PostFilters.Entry;
import com.quertle.webservice.base.SearchWSImplementationsService;
import com.quertle.webservice.base.SearchingSEI;
import com.quertle.webservice.base.WSCheckFaultException;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

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
	 * @param args
	 */
	public QuertleDataFeed(String[] args) {
		super(args);
	}


	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	public void run() {

		//Load the filters
		loadFilters(props.getProperty(QUERTLE_ENTITY_ID));

		log.info("filters loaded.");
		//Build the configured Port
		SearchingSEI port = buildPort();

		//If Port is null, we can't connect to Quertle System.
		if(port == null) {
			log.error("Can not connect to Quertle");
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

		//Call to Quertle for each Classification
		for (String c : classifications) {

			//Load Results for the Classification
			List<ResultAttributes> results = queryResults(port, sp, c);

			log.info("Retrieved Articles, size: " + results.size());
			//If we have results, process them.
			if (results != null && !results.isEmpty()) {

				//Build and Save Article VO's
				processResults(results, searchType);
			}
		}
	}


	/**
	 * Process the Given Quertle ResultAttributes list and convert to
	 * RSSArticleVOs.  Apply Filtering to VO before returning.
	 * @param results
	 */
	private void processResults(List<ResultAttributes> results, String searchType) {

		//Load existing Article Ids for Quertle.
		Map<String, Set<String>> ids = getExistingIds(searchType, results);

		log.info("Found Existing Records: " + ids.size());
		RSSArticleVO a = null;
		//Iterate Results and Builds Article VOs.
		for (ResultAttributes r : results) {
			String id = searchType + r.getApplicationNumber();

			a = buildArticleVO(id, searchType, r);

			applyFilters(a, ids);

			//Remove the Full Article Text to lessen memory overhead.
			a.setFullArticleTxt(null);

			if(!a.getFilterVOs().isEmpty()) {
				//Save Articles.
				storeArticles(a);
			}

		}
	}


	/**
	 * @param a
	 * @param ids 
	 */
	private void applyFilters(RSSArticleVO a, Map<String, Set<String>> ids) {
		/*
		 * Iterate over the FeedGroups and create an article VO for each.
		 * Quertle results can appear in multiple groups per business
		 * requirements.
		 */
		for(RSSFeedGroupVO g : groups) {

			if(!articleExists(a.getArticleGuid(), g.getFeedGroupId(), ids)) {
				//Apply Matching Filters to article.
				applyFilter(a, g.getFeedGroupId());
			}
		}
	}


	/**
	 * Parse and build and filter an RSSArticleVO with the given information.
	 * @param id
	 * @param r
	 * @param g
	 * @return
	 */
	private RSSArticleVO buildArticleVO(String id, String searchType, ResultAttributes r) {

		//Instantiate the Article and set common fields.
		RSSArticleVO a = new RSSArticleVO();
		a.setTitleTxt(r.getTitle().replace("\u00a0"," "));
		a.setArticleTxt(r.getAbstractText().replace("\u00a0"," "));
		a.setPublishDt(getPubDate(r.getPubDate()));
		a.setArticleGuid(id);
		a.setRssEntityId(props.getProperty(QUERTLE_ENTITY_ID));
		a.setArticleSourceType(ArticleSourceType.QUERTLE);

		//Set special attributes based on Application or Grant Type.
		if(searchType.equals(props.get(PATENT_APPLICATION_TYPE))) {
			a.setArticleUrl(r.getPAppHtmlLink());
			//Set PDF Link
			a.setAttribute1Txt(r.getPAppPdfLink());
		} else {
			a.setArticleUrl(r.getPGrantHtmlLink());
			//Set PDF Link
			a.setAttribute1Txt(r.getPGrantPdfLink());
			//Set Patent No
			a.setAttribute1Txt(r.getPatentNumber());
		}

		a.setFullArticleTxt(loadArticle(a.getArticleUrl()));
		return a;
	}


	/**
	 * Format the Quertle Publish Date String.
	 * @param pubDate
	 * @return
	 */
	private Date getPubDate(String pubDate) {
		if(StringUtil.isEmpty(pubDate)) {
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
	private Map<String, Set<String>> getExistingIds(String searchType, List<ResultAttributes> results) {
		List<String> ids = new ArrayList<>(results.size());
		for(ResultAttributes r : results) {
			ids.add(searchType + r.getApplicationNumber());
		}
		return super.getExistingArticles(ids, props.getProperty(QUERTLE_ENTITY_ID));
	}


	/**
	 * Query Quertle for results matching the given searchparams and classification.
	 *
	 * @param service
	 * @param sp
	 * @param c
	 * @return
	 */
	private List<ResultAttributes> queryResults(SearchingSEI port, SearchParams sp, String c) {
		List<ResultAttributes> results = Collections.emptyList();
		int cnt = Convert.formatInteger(props.getProperty(HITS_PER_QUERY));

		//Set the passed classification.
		sp.setInternationalClassification(c);
		try {

			//Query for Results.
			List<HitcountOption> hits = port.resultHitcounts(sp, 0, cnt);

			//Check if we have results.
			if (!hits.isEmpty() && hits.size() > 1) {

				/*
				 * The results we care about are described by the object at index
				 * 1.
				 */
				HitcountOption h = hits.get(1);

				//Instantiate Results List with proper size.
				results = new ArrayList<>(h.getHitCount() + 1);

				//Add All of the current results.
				results.addAll(port.resultElements(null));
				int resCnt = cnt;

				/*
				 * Continue to Query the system and store results until we've
				 * gotten all the results.
				 */
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
			log.info("No Articles Available.");
		} catch (ParseException_Exception | ClassNotFoundException_Exception | Exception_Exception | IOException_Exception e) {
			log.error("Error Processing Code", e);
		}

		return results;
	}


	/**
	 * Initialize Default Search Parameters for Queries.
	 * 
	 * @return
	 */
	private SearchParams initializeSearchParams(String type, String dr) {
		String dateRange = StringUtil.checkVal(dr, props.getProperty(DEF_DATE_RANGE));
		SearchParams sp = new SearchParams();
		PostFilters pf = new PostFilters();
		Entry e = new Entry();
		StringBuilder value = new StringBuilder(25);
		value.append("f=").append(type).append("|").append(dateRange);
		e.setKey("value");
		e.setValue(value.toString());
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

		//Create the Service.
		SearchWSImplementationsService ss = new SearchWSImplementationsService(wsdlURL, SERVICE_NAME);

		//Get the Port.  Case to a BindingProvider per Quertle documentation.
		BindingProvider port = (BindingProvider) ss.getSearchWSImplementationsPort();

		String url = wsdlURL.toString();

		//Get the Request context Map for the Binding Port.
		Map<String, Object> ctx = port.getRequestContext();
		ctx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);

		/****
		 * User Authentication using partner code and authentication code
		 ***/
		ctx.put(MessageContext.HTTP_REQUEST_HEADERS, buildHeaders());

		/*** Session maintenance by providing it in the header ***/
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
		URL url = null;
		try {
			url = new URL(props.getProperty(WSDL_URL_STRING));
		} catch (MalformedURLException e) {
			log.error("Can not initialize the default wsdl from " + props.getProperty(WSDL_URL_STRING));
		}

		return url;
	}

	/**
	 * Load the page related to the given url.
	 * @param articleUrl
	 * @return
	 */
	private String loadArticle(String url) {

		String article = null;
		if(!StringUtil.isEmpty(url)) {
			SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
			byte[] data = null;
			try {
				data = conn.retrieveData(url);

				//trap all errors generated by LL
				if (404 == conn.getResponseCode()) {
					return null;
				}

				if (200 != conn.getResponseCode()) {
					throw new IOException("Transaction Unsuccessful, code=" + conn.getResponseCode());
				}

				if(data != null) {
					article = new String(data);
				}
			} catch (IOException e) {
				log.error(url);
			}
		}
		return article;
	}
}