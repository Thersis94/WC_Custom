package com.biomed.smarttrak.action;

//Java 8
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Apache Solr
import org.apache.solr.common.SolrDocument;

// WC custom
import com.biomed.smarttrak.util.BiomedCompanyIndexer;
import com.biomed.smarttrak.util.BiomedMarketIndexer;
import com.biomed.smarttrak.util.BiomedProductIndexer;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.security.UserRoleVO;
import com.siliconmtn.util.Convert;

// WebCrescendo libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.search.SolrActionIndexVO;
import com.smt.sitebuilder.action.search.SolrActionVO;
import com.smt.sitebuilder.action.search.SolrFieldVO;
import com.smt.sitebuilder.action.search.SolrFieldVO.BooleanType;
import com.smt.sitebuilder.action.search.SolrFieldVO.FieldType;
import com.smt.sitebuilder.action.search.SolrQueryProcessor;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.PageViewRetriever;
import com.smt.sitebuilder.util.PageViewVO;

/*****************************************************************************
 <p><b>Title</b>: UserRecentActivityAction.java</p>
 <p><b>Description: </b>Retrieves user's 'recently viewed' activity based on page view 
 history data.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 15, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class RecentlyViewedAction extends SBActionAdapter {
	
	private static final String SLASH = "/";
	private static final String MARKETS = "markets";
	private static final String COMPANIES = "companies";
	private static final String PRODUCTS = "products";
	private static final String TOOLS = "tools";
	private static final int MAX_LIST_SIZE = 10;
	private static final int DATE_START_DAY_OFFSET = -30;
	/**
	 * Constructor
	 */
	public RecentlyViewedAction() {
		super();
	}
	
	/**
	 * Constructor
	 */
	public RecentlyViewedAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("UserRecentActivityAction retrieve...");
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		Map<String, List<PageViewVO>> recentActivity;
		try {
			String siteId = parseSiteId(req);
			String profileId = checkProfileId(req);
			recentActivity = retrieveRecentlyViewedPages(req, siteId, profileId, formatStartDate());
			
		} catch (ActionException ae) {
			recentActivity = new HashMap<>();
			mod.setError(ae.getMessage(), ae);
			log.debug("UserRecentActivityAction retrieve error condition | message: " + mod.getErrorCondition() + "|" + mod.getErrorMessage());
		}
		
		this.putModuleData(recentActivity, recentActivity.size(), false, mod.getErrorMessage(), mod.getErrorCondition());
	}
	
	/**
	 * Validates a user's security role and then returns a profile ID or null value based
	 * on the role of the of the logged in user.
	 * @param req
	 * @param siteId
	 * @return
	 * @throws ActionException
	 */
	protected String checkProfileId(ActionRequest req) throws ActionException {
		StringBuilder errMsg = new StringBuilder(100);
		errMsg.append("User activity access not authorized. ");
		
		SMTSession sess = (SMTSession)req.getSession();
		if (sess == null) {
			errMsg.append("Session is Invalid.");
			throw new ActionException(errMsg.toString());
		}
		
		String profileId = null;
		UserDataVO user = (UserDataVO)sess.getAttribute(Constants.USER_DATA);
		if (user != null) profileId = user.getProfileId();
		if (profileId == null) {
			errMsg.append("Not logged in.");
			throw new ActionException(errMsg.toString());
		}
		return profileId;

	}

	/**
	 * Determines the siteId value to use for this retrieving page views.
	 * @param req
	 * @return
	 */
	protected String parseSiteId(ActionRequest req) {
		if (req.hasParameter("siteId")) {
			return req.getParameter("siteId");
		} else {
			SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
			return site.getAliasPathParentId() != null ? site.getAliasPathParentId() : site.getSiteId();
		}
	}

	/**
	 * Retrieves page view history for all logged in users within the requested timeframe.  If
	 * no dates are specified, page view history for the last 12 hours is returned. 
	 * @param site
	 * @param profileId
	 * @param dateStart
	 * @return
	 * @throws ActionException
	 */
	protected Map<String, List<PageViewVO>> retrieveRecentlyViewedPages(ActionRequest req, 
			String siteId, String profileId, String dateStart) throws ActionException {
		/* Retrieve page views from db, parse into PageViewVO and return list */
		PageViewRetriever pvr = new PageViewRetriever(dbConn);
		pvr.setSortDescending(true);
		List<PageViewVO> pageViews = pvr.retrieveRecentlyViewedPages(siteId, profileId, dateStart);
		log.debug("Total number of raw page views found: " + pageViews.size());
		return parseResults(req, pageViews);
	}
	
	/**
	 * Returns a String representing a start date of 'today minus 30 days'.
	 * @return
	 */
	protected String formatStartDate() {
		Calendar cal = GregorianCalendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, DATE_START_DAY_OFFSET);
		return Convert.formatDate(cal.getTime(),Convert.DATE_TIME_DASH_PATTERN);
	}
	
	/**
	 * Parses the resulting list of page views into a map of profile IDs mapped to UserActivityVOs.
	 * @param pageViews
	 * @return
	 */
	protected Map<String, List<PageViewVO>> parseResults(ActionRequest req, List<PageViewVO> pages) {
		// collate pages into buckets
		Map<String, List<PageViewVO>> recentActivity = collatePages(pages);
		
		// leverage solr to retrieve display names
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		UserRoleVO role = (UserRoleVO)req.getSession().getAttribute(Constants.ROLE_DATA);
		for (Map.Entry<String, List<PageViewVO>> entry : recentActivity.entrySet()) {
			try {
				formatNamesFromPage(site.getOrganizationId(), role.getRoleLevel(), entry);
			} catch(ActionException ae) {
				continue;
			}
		}
		
		return recentActivity;
	}
	
	/**
	 * Separates pages into their respective bucket.
	 * @param pages
	 * @return
	 */
	protected Map<String,List<PageViewVO>> collatePages(List<PageViewVO> pages) {
		List<PageViewVO> markets = new ArrayList<>();
		List<PageViewVO> companies = new ArrayList<>();
		List<PageViewVO> products = new ArrayList<>();
		List<PageViewVO> tools = new ArrayList<>();
		// loop pages, parse into buckets, filter out duplicates.
		for (PageViewVO page : pages) {
			// set page ID to represent entity ID (e.g. market ID, company ID, etc.)
			formatIdFromPage(page);
			// skip root pages.
			if (page.getPageId() == null) continue;
			// collate according to type
			if (page.getRequestUri().indexOf(SLASH + MARKETS) > -1) {
				if (markets.size() < MAX_LIST_SIZE) markets.add(page);
			} else if (page.getRequestUri().indexOf(SLASH + COMPANIES) > -1) {
				if (companies.size() < MAX_LIST_SIZE) companies.add(page);
			} else if (page.getRequestUri().indexOf(SLASH + PRODUCTS) > -1) {
				if (products.size() < MAX_LIST_SIZE) products.add(page);
			} else if (page.getRequestUri().indexOf(SLASH + TOOLS) > -1) {
				if (tools.size() < MAX_LIST_SIZE) tools.add(page);
			}
		}
		
		Map<String, List<PageViewVO>> recentActivity = new HashMap<>();
		recentActivity.put(MARKETS, markets);
		recentActivity.put(COMPANIES, companies);
		recentActivity.put(PRODUCTS, products);
		recentActivity.put(TOOLS, tools);
		return recentActivity;
	}
	
	/**
	 * Formats the page ID of the recently viewed page.  We use page ID to represent
	 * the ID of the entity we are dealing with.  For a market entity, page ID represents 
	 * the market ID, for company, the company ID, and so on.
	 * @param page
	 */
	protected void formatIdFromPage(PageViewVO page) {
		int idx = page.getRequestUri().lastIndexOf('/');
		if (idx == 0 || idx == page.getRequestUri().length() - 1) return;
		page.setPageId(page.getRequestUri().substring(idx+1));
	}
	
	/**
	 * Formats entity names
	 * @param orgId
	 * @param roleLevel
	 * @param entry
	 * @throws ActionException 
	 */
	protected void formatNamesFromPage(String orgId, int roleLevel, 
			Map.Entry<String, List<PageViewVO>> entry) throws ActionException {
		Map<String,String> pageNames = retrieveEntityNames(orgId,roleLevel,entry);
		for (PageViewVO page : entry.getValue()) {
			page.setPageDisplayName(pageNames.get(page.getPageId()));
		}
	}
	
	/**
	 * Leverages Solr to query for entity names.
	 * @param orgId
	 * @param roleLevel
	 * @param pages
	 * @return
	 * @throws ActionException 
	 */
	protected Map<String,String> retrieveEntityNames(String orgId, int roleLevel, 
			Map.Entry<String, List<PageViewVO>> entry) throws ActionException {
		String indexer = findIndexerType(entry.getKey());
		SolrQueryProcessor sqp = new SolrQueryProcessor(getAttributes(), getAttribute(Constants.SOLR_COLLECTION_NAME).toString());
		SolrActionVO qData = new SolrActionVO();
		for (PageViewVO page : entry.getValue()) {
			SolrFieldVO field = new SolrFieldVO();
			field.setBooleanType(BooleanType.OR);
			field.setFieldType(FieldType.SEARCH);
			field.setFieldCode(SearchDocumentHandler.DOCUMENT_ID);
			field.setValue((String) page.getPageId());
			qData.addSolrField(field);
		}

		qData.setNumberResponses(entry.getValue().size());
		qData.setStartLocation(0);
		qData.setRoleLevel(roleLevel);
		qData.setOrganizationId(orgId);
		qData.addIndexType(new SolrActionIndexVO("", indexer));

		SolrResponseVO resp = sqp.processQuery(qData);
		Map<String,String> names = new HashMap<>();
		for (SolrDocument doc : resp.getResultDocuments()) {
			names.put(doc.getFieldValue(SearchDocumentHandler.DOCUMENT_ID).toString(), doc.getFieldValue(SearchDocumentHandler.TITLE).toString());
		}
		return names;
	}
	
	/**
	 * Helper method to return index type based on a key.
	 * @param key
	 * @return
	 */
	protected String findIndexerType(String key) throws ActionException {
		switch(key) {
			case MARKETS:
				BiomedMarketIndexer bmi = new BiomedMarketIndexer(null);
				return bmi.getIndexType();
			case COMPANIES:
				return BiomedCompanyIndexer.INDEX_TYPE;
			case PRODUCTS:
				return BiomedProductIndexer.INDEX_TYPE;
			default:
				throw new ActionException("Error: Unknown indexer type: " + key);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void build(ActionRequest req) throws ActionException {
		SMTSession sess = (SMTSession)req.getSession();
		Map<String, List<PageViewVO>> recent = (Map<String,List<PageViewVO>>)sess.getAttribute(QuickLinksAction.USER_RECENTLY_VIEWED);
		if (recent == null) return;

		// determine page type
		String entity;
		try {
			entity = getEntityString(req.getRequestURI());
		} catch(Exception e) {
			return;
		}

		// get the proper collection
		List<PageViewVO> pages = recent.get(entity);

		PageViewVO page = new PageViewVO();
		page.setPageId(req.getParameter("id"));
		page.setPageDisplayName(req.getParameter("name"));
		page.setRequestUri(req.getRequestURI());

		// add to top of list
		pages.add(0,page);

		// if list size > max, remove 1
		if (pages.size() > MAX_LIST_SIZE) 
			pages.remove(pages.size() - 1);

		// set update collection on session
		sess.setAttribute(QuickLinksAction.USER_RECENTLY_VIEWED, pages);
	}

	/**
	 * Helper method for returning a valid key for retrieving the correct 
	 * collection from the 'recently viewed' map on the session. 
	 * @param val
	 * @return
	 * @throws ActionException
	 */
	protected String getEntityString(String val) throws ActionException {
		if (val.indexOf(SLASH+MARKETS) > -1) {
			return MARKETS;
		} else if (val.indexOf(SLASH+COMPANIES) > -1) {
			return COMPANIES;
		} else if (val.indexOf(SLASH+PRODUCTS) > -1) {
			return PRODUCTS;
		} else if (val.indexOf(SLASH+TOOLS) > -1) {
			return TOOLS;
		}
		throw new ActionException("Unknown entity value.");
	}

}
