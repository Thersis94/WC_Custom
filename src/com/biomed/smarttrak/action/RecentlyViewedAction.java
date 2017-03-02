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
import com.biomed.smarttrak.action.AdminControllerAction.Section;
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
		log.debug("RecentlyViewedAction retrieve...");
		SMTSession sess = (SMTSession)req.getSession();
		UserDataVO user = (UserDataVO)sess.getAttribute(Constants.USER_DATA);
		if (user == null) throw new ActionException("Not logged in.");

		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		Map<String, List<PageViewVO>> recentActivity;
		try {
			recentActivity = retrieveRecentlyViewedPages(req, user.getProfileId());

		} catch (ActionException ae) {
			recentActivity = new HashMap<>();
			mod.setError(ae.getMessage(), ae);
			log.debug("UserRecentActivityAction retrieve error condition | message: " + mod.getErrorCondition() + "|" + mod.getErrorMessage());
		}

		this.putModuleData(recentActivity, recentActivity.size(), false, mod.getErrorMessage(), mod.getErrorCondition());
	}

	/**
	 * Retrieves recently viewed pages for a specific user on a specific site. 
	 * @param req
	 * @param profileId
	 * @return
	 * @throws ActionException
	 */
	protected Map<String, List<PageViewVO>> retrieveRecentlyViewedPages(ActionRequest req, 
			String profileId) throws ActionException {
		/* Retrieve page views from db, parse into PageViewVO and return list */

		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		UserRoleVO userRole = (UserRoleVO)req.getSession().getAttribute(Constants.ROLE_DATA);

		List<PageViewVO> pages = new ArrayList<>();

		StringBuilder sql = formatRecentlyViewedQuery();
		int idx = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (Section section : Section.values()) {
				ps.setString(++idx, profileId);
				ps.setString(++idx, site.getSiteId());
				ps.setString(++idx, section.getURLToken()+QuickLinksAction.URL_STUB + "%");
			}
			ResultSet rs = ps.executeQuery();
			PageViewVO page = null;
			while(rs.next()) {
				page = new PageViewVO();
				page.setReferenceCode(rs.getString("reference_cd"));
				page.setRequestUri(rs.getString("request_uri_txt"));

				/* Use page bean's page ID field to represent entity ID 
				 * (e.g. market ID, company ID, etc.) */
				setPageId(page);
				// only add to list if we set a page ID value.l
				if (page.getPageId() != null) 
					pages.add(page);
			}
		} catch (SQLException sqle) {
			throw new ActionException(sqle.getMessage());
		}

		log.debug("Total number of raw page views found: " + pages.size());
		return parseResults(site, userRole, pages);
	}

	/**
	 * Parses the resulting list of page views into a map of profile IDs mapped to UserActivityVOs.
	 * @param site
	 * @param userRole
	 * @param pages
	 * @return
	 */
	protected Map<String, List<PageViewVO>> parseResults(SiteVO site, 
			UserRoleVO userRole, List<PageViewVO> pages) {
		// collate pages into buckets
		Map<String, List<PageViewVO>> recentViewed = collatePages(pages);

		// leverage solr to retrieve display names
		for (Map.Entry<String, List<PageViewVO>> entry : recentViewed.entrySet()) {
			try {
				formatNamesFromPage(site.getOrganizationId(), userRole.getRoleLevel(), entry);
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
		// loop pages, parse into buckets
		List<PageViewVO> pList;
		for (PageViewVO page : pages) {
			// add page to the appropriate List.
			pList = pageMap.get(page.getReferenceCode());
			if (pList != null && 
					pList.size() < QuickLinksAction.MAX_LIST_SIZE)
				pList.add(page);
		}
		return pageMap;
	}

	/**
	 * Initialize a Map of List of PageViewVO based on the key types enum.
	 * @return
	 */
	protected Map<String,List<PageViewVO>> initializePageMap() {
		Map<String,List<PageViewVO>> pm = new HashMap<>();
		for (Section sect : Section.values()) {
			pm.put(sect.name(), new ArrayList<>());
		}
		return pm;
	}

	/**
	 * We use page ID on the PageViewVO to represent the ID of the 
	 * section entity we are dealing with. For a market entity, page ID 
	 * represents the market ID, for company, the company ID, and so on.
	 * @param page
	 */
	protected void setPageId(PageViewVO page) {
		int idx = page.getRequestUri().lastIndexOf(QuickLinksAction.CHAR_SLASH);
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
			Map.Entry<String, List<PageViewVO>> pages) throws ActionException {
		Map<String,String> pageNames = retrieveEntityNames(orgId,roleLevel,pages);
		for (PageViewVO page : pages.getValue()) {
			page.setPageDisplayName(pageNames.get(page.getPageId()));
		}
	}

	/**
	 * Leverages Solr to query for entity names.
	 * @param orgId
	 * @param roleLevel
	 * @param entry
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
		qData.setRoleLevel(AdminControllerAction.DEFAULT_ROLE_LEVEL);
		qData.setOrganizationId(orgId);
		qData.addIndexType(new SolrActionIndexVO("", indexer));

		Map<String,String> names = new HashMap<>();
		SolrResponseVO resp = sqp.processQuery(qData);
		if (resp == null) return names;

		for (SolrDocument doc : resp.getResultDocuments()) {
			names.put(doc.getFieldValue(SearchDocumentHandler.DOCUMENT_ID).toString(), doc.getFieldValue(SearchDocumentHandler.TITLE).toString());
		}
		log.debug("names size: " + names.size());
		return names;
	}

	/**
	 * Helper method to return index type based on a key.
	 * @param key
	 * @return
	 */
	protected String findIndexerType(String key) throws ActionException {
		Section s = Section.valueOf(key);
		switch(s) {
			case MARKET:
				return BiomedMarketIndexer.INDEX_TYPE;
			case COMPANY:
				return BiomedCompanyIndexer.INDEX_TYPE;
			case PRODUCT:
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
		log.debug("RecentlyViewedAction build...");

		// determine page collection type
		String collKey;
		try {
			collKey = checkCollectionKey(req.getParameter(QuickLinksAction.PARAM_KEY_SECTION));
		} catch(Exception e) {
			log.error("Error: " + e.getMessage());
			return;
		}

		SMTSession sess = (SMTSession)req.getSession();
		Map<String, List<PageViewVO>> rvMap = (Map<String,List<PageViewVO>>)sess.getAttribute(QuickLinksAction.MY_RECENTLY_VIEWED);
		// if map doesn't exist, create it.
		if (rvMap == null) rvMap = new HashMap<>();
		log.debug("rvMap size is: " + rvMap.size());
		// get the proper collection or create it if it doesn't exist
		List<PageViewVO> pages = rvMap.get(collKey);
		if (pages == null) {
			pages = new ArrayList<>();
		}

		log.debug("pages is size: " + pages.size());
		PageViewVO page = new PageViewVO();
		page.setPageDisplayName(req.getParameter(QuickLinksAction.PARAM_KEY_NAME));
		page.setRequestUri(req.getParameter(QuickLinksAction.PARAM_KEY_URI_TXT));
		int idx = page.getRequestUri().lastIndexOf(QuickLinksAction.CHAR_SLASH);
		if (idx < page.getRequestUri().length()) 
			page.setPageId(page.getRequestUri().substring(idx+1));
		if (isDuplicatePage(pages, page)) return;

		log.debug("section name | url: " + page.getPageDisplayName() + "|" + page.getRequestUri());

		// add to top of list
		pages.add(0,page);

		// if list size > max, remove the last one in the list
		if (pages.size() > QuickLinksAction.MAX_LIST_SIZE) 
			pages.remove(pages.size() - 1);

		// replace the collection on the map.
		rvMap.put(collKey, pages);
		// update the collection map on session
		sess.setAttribute(QuickLinksAction.MY_RECENTLY_VIEWED, rvMap);

	}

	/**
	 * Helper method for returning a valid key for retrieving the correct 
	 * collection from the 'recently viewed' map on the session. 
	 * @param section
	 * @return
	 * @throws ActionException
	 */
	protected String checkCollectionKey(String section) throws ActionException {
		String key = StringUtil.checkVal(section).toUpperCase();
		try {
			Section.valueOf(key);
		} catch (Exception e) {
			log.debug("exception!  " + e);
			throw new ActionException("Unknown section value: " + section);
		}
		return key;
	}

	/**
	 * Checks to see if the page being viewed is already on the list for the given collection.
	 * @param pages
	 * @param page
	 * @return
	 */
	protected boolean isDuplicatePage(List<PageViewVO> pages, PageViewVO page) {
		if (page.getPageId() == null) return true;
		for (PageViewVO tmp : pages) {
			if (page.getPageId().equalsIgnoreCase(tmp.getPageId())) 
				return true;
		}
		return false;
	}

	/**
	 * Formats the query to retrieve the top 10 rows from each entity type.
	 * @return
	 */
	protected StringBuilder formatRecentlyViewedQuery() {
		StringBuilder sql = new StringBuilder(850);
		int count = 1;
		for (Section section : Section.values()) {
			sql.append("select '");
			sql.append(section.name());
			sql.append("' as reference_cd, request_uri_txt, visit_dt from ( ");
			sql.append("select distinct(request_uri_txt), visit_dt from pageview_user ");
			sql.append("where profile_id = ? and site_id = ? and request_uri_txt like ? ");
			sql.append("order by visit_dt desc limit ");
			sql.append(QuickLinksAction.MAX_LIST_SIZE);
			sql.append(" ) as ").append(section.name()).append(" ");

			if (count < Section.values().length)
				sql.append("union all ");

			count++;
		}
		sql.append("order by visit_dt desc ");
		log.debug("Recently viewed SQL: " + sql.toString());
		return sql;
	}

}
