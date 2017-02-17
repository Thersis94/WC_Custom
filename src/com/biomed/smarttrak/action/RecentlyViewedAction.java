package com.biomed.smarttrak.action;

//Java 8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.security.UserRoleVO;
import com.siliconmtn.util.StringUtil;

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
	
	private static final char CHAR_SLASH = '/';
	private static final String MARKETS = "/markets";
	private static final String COMPANIES = "/companies";
	private static final String PRODUCTS = "/products";
	private static final int MAX_LIST_SIZE = 10;
	
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
	
	// Enum used as Map keys and for literals where appropriate.
	public enum KEY_TYPE {
		MARKET,COMPANY,PRODUCT
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
			recentActivity = retrieveRecentlyViewedPages(req, siteId, profileId);
			
		} catch (ActionException ae) {
			recentActivity = new HashMap<>();
			mod.setError(ae.getMessage(), ae);
			log.debug("UserRecentActivityAction retrieve error condition | message: " + mod.getErrorCondition() + "|" + mod.getErrorMessage());
		}
		
		this.putModuleData(recentActivity, recentActivity.size(), false, mod.getErrorMessage(), mod.getErrorCondition());
	}

	/**
	 * Determines the siteId value to use for this retrieving page views.
	 * @param req
	 * @return
	 */
	protected String parseSiteId(ActionRequest req) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		return site.getSiteId();
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
		
		UserDataVO user = (UserDataVO)sess.getAttribute(Constants.USER_DATA);
		if (user == null || user.getProfileId() == null) {
			errMsg.append("Not logged in.");
			throw new ActionException(errMsg.toString());
		}
		return user.getProfileId();

	}
	
	/**
	 * Retrieves recently viewed pages for a specific user on a specific site. 
	 * @param site
	 * @param profileId
	 * @return
	 * @throws ActionException
	 */
	protected Map<String, List<PageViewVO>> retrieveRecentlyViewedPages(ActionRequest req, 
			String siteId, String profileId) throws ActionException {
		/* Retrieve page views from db, parse into PageViewVO and return list */
		List<PageViewVO> pages = new ArrayList<>();
		
		StringBuilder sql = formatRecentlyViewedQuery();
		int idx = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(++idx, StringUtil.checkVal(KEY_TYPE.MARKET.name(),true));
			ps.setString(++idx, profileId);
			ps.setString(++idx, siteId);
			ps.setString(++idx, StringUtil.checkVal(MARKETS+"%",true));
			ps.setString(++idx, StringUtil.checkVal(KEY_TYPE.COMPANY.name(),true));
			ps.setString(++idx, profileId);
			ps.setString(++idx, siteId);
			ps.setString(++idx, StringUtil.checkVal(COMPANIES+"%",true));
			ps.setString(++idx, StringUtil.checkVal(KEY_TYPE.PRODUCT.name(),true));
			ps.setString(++idx, profileId);
			ps.setString(++idx, siteId);
			ps.setString(++idx, StringUtil.checkVal(PRODUCTS+"%",true));
			
			ResultSet rs = ps.executeQuery();
			PageViewVO page = null;
			DBUtil db = new DBUtil();
			while(rs.next()) {
				page = new PageViewVO();
				page.setReferenceCode(db.getStringVal("reference_cd", rs));
				page.setRequestUri(db.getStringVal("request_uri_txt", rs));
				pages.add(page);
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving recently viewed pages for profile ID: " + profileId);
			throw new ActionException(sqle.getMessage());
		}
				
		log.debug("Total number of raw page views found: " + pages.size());
		return parseResults(req, pages);
	}

	/**
	 * Parses the resulting list of page views into a map of profile IDs mapped to UserActivityVOs.
	 * @param pageViews
	 * @return
	 */
	protected Map<String, List<PageViewVO>> parseResults(ActionRequest req, List<PageViewVO> pages) {
		// collate pages into buckets
		Map<String, List<PageViewVO>> recentViewed = collatePages(pages);
		
		// leverage solr to retrieve display names
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		UserRoleVO role = (UserRoleVO)req.getSession().getAttribute(Constants.ROLE_DATA);
		for (Map.Entry<String, List<PageViewVO>> entry : recentViewed.entrySet()) {
			try {
				formatNamesFromPage(site.getOrganizationId(), role.getRoleLevel(), entry);
			} catch(ActionException ae) {
				continue;
			}
		}
		
		return recentViewed;
	}
	
	/**
	 * Separates pages into their respective bucket.
	 * @param pages
	 * @return
	 */
	protected Map<String,List<PageViewVO>> collatePages(List<PageViewVO> pages) {
		Map<String, List<PageViewVO>> pageMap = initializePageMap();
		// loop pages, parse into buckets, filter out duplicates.
		for (PageViewVO page : pages) {
			// set page ID to represent entity ID (e.g. market ID, company ID, etc.)
			formatIdFromPage(page);
			// skip root pages.
			if (page.getPageId() == null) continue;
			// add page to the appropriate List.
			pageMap.get(page.getReferenceCode()).add(page);
		}
		return pageMap;
	}

	
	/**
	 * Initialize a Map of List of PageViewVO based on the key types enum.
	 * @return
	 */
	protected Map<String,List<PageViewVO>> initializePageMap() {
		Map<String,List<PageViewVO>> pm = new HashMap<>();
		for (KEY_TYPE kt : KEY_TYPE.values()) {
			pm.put(kt.name(), new ArrayList<>());
		}
		return pm;
	}
	
	/**
	 * Formats the page ID of the recently viewed page.  We use page ID to represent
	 * the ID of the entity we are dealing with.  For a market entity, page ID represents 
	 * the market ID, for company, the company ID, and so on.
	 * @param page
	 */
	protected void formatIdFromPage(PageViewVO page) {
		int idx = page.getRequestUri().lastIndexOf(CHAR_SLASH);
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

		// determine page collection type
		String collKey;
		try {
			collKey = getEntityString(req.getRequestURI());
		} catch(Exception e) {
			return;
		}

		// get the proper collection
		List<PageViewVO> pages = recent.get(collKey);

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
		if (val.indexOf(MARKETS) > -1) {
			return KEY_TYPE.MARKET.name();
		} else if (val.indexOf(COMPANIES) > -1) {
			return KEY_TYPE.COMPANY.name();
		} else if (val.indexOf(PRODUCTS) > -1) {
			return KEY_TYPE.PRODUCT.name();
		}
		throw new ActionException("Unknown entity value.");
	}
	
	/**
	 * Formats the query to retrieve the top 10 rows from each entity type.
	 * @return
	 */
	protected StringBuilder formatRecentlyViewedQuery() {
		StringBuilder sql = new StringBuilder(850);
		sql.append("select ? as reference_cd, request_uri_txt, visit_dt from ( ");
		sql.append("select distinct(request_uri_txt), visit_dt from pageview_user ");
		sql.append("where profile_id = ? and site_id = ? and request_uri_txt like ? ");
		sql.append("order by visit_dt desc limit 10 ) as x ");
		sql.append("union all ");
		sql.append("select ? as reference_cd, request_uri_txt, visit_dt from ( ");
		sql.append("select distinct(request_uri_txt), visit_dt from pageview_user ");
		sql.append("where profile_id = ? and site_id = ? and request_uri_txt like ? ");
		sql.append("order by visit_dt desc limit 10 ) as y ");
		sql.append("union all ");
		sql.append("select ? as reference_cd, request_uri_txt, visit_dt from ( ");
		sql.append("select distinct(request_uri_txt), visit_dt from pageview_user ");
		sql.append("where profile_id = ? and site_id = ? and request_uri_txt like ? ");
		sql.append("order by visit_dt desc limit 10 ) as z ");
		sql.append("order by visit_dt desc ");
		return sql;
	}


}
