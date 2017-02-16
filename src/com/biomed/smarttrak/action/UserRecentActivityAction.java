package com.biomed.smarttrak.action;

//Java 8
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.security.UserDataVO;

// WebCrescendo libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
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
public class UserRecentActivityAction extends SBActionAdapter {
	
	private static final String SLASH = "/";
	private static final String MARKETS = "markets";
	private static final String COMPANIES = "companies";
	private static final String PRODUCTS = "products";
	private static final String TOOLS = "tools";
	private static final int MAX_LIST_SIZE = 10;
	
	/**
	 * Constructor
	 */
	public UserRecentActivityAction() {
		super();
	}
	
	/**
	 * Constructor
	 */
	public UserRecentActivityAction(ActionInitVO actionInit) {
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
			recentActivity = retrieveRecentlyViewedPages(siteId, profileId, null, null);
			
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
	private String checkProfileId(ActionRequest req) throws ActionException {
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
	private String parseSiteId(ActionRequest req) {
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
	 * @param dateEnd
	 * @return
	 * @throws ActionException
	 */
	private Map<String, List<PageViewVO>> retrieveRecentlyViewedPages(String siteId, 
			String profileId, String dateStart, String dateEnd) throws ActionException {
		/* Retrieve page views from db, parse into PageViewVO and return list */
		PageViewRetriever pvr = new PageViewRetriever(dbConn);
		pvr.setSortDescending(true);
		List<PageViewVO> pageViews = pvr.retrievePageViews(siteId, profileId, dateStart, dateEnd);
		log.debug("Total number of raw page views found: " + pageViews.size());
		return parseResults(pageViews);
	}
	
	/**
	 * Parses the resulting list of page views into a map of profile IDs mapped to UserActivityVOs.
	 * @param pageViews
	 * @return
	 */
	private Map<String, List<PageViewVO>> parseResults(List<PageViewVO> pageViews) {
		List<PageViewVO> markets = new ArrayList<>();
		List<PageViewVO> companies = new ArrayList<>();
		List<PageViewVO> products = new ArrayList<>();
		List<PageViewVO> tools = new ArrayList<>();
		
		Map<String, List<PageViewVO>> recentActivity = new HashMap<>();
		
		// loop pages, parse into buckets, filter out duplicates.
		List<String> dupeList = new ArrayList<>();
		for (PageViewVO page : pageViews) {
			if (dupeList.contains(page.getRequestUri())) continue;
			if (page.getRequestUri().indexOf(SLASH + MARKETS) > -1) {
				addMarketPage(markets,page);
			} else if (page.getRequestUri().indexOf(SLASH + COMPANIES) > -1) {
				addCompaniesPage(companies,page);
			} else if (page.getRequestUri().indexOf(SLASH + PRODUCTS) > -1) {
				addProductsPage(products,page);
			} else if (page.getRequestUri().indexOf(SLASH + TOOLS) > -1) {
				addToolsPage(tools, page);
			}
			dupeList.add(page.getRequestUri());
		}

		recentActivity.put(MARKETS, markets);
		recentActivity.put(COMPANIES, companies);
		recentActivity.put(PRODUCTS, products);
		recentActivity.put(TOOLS, tools);

		return recentActivity;
	}
	
	private void addMarketPage(List<PageViewVO> markets, PageViewVO page) {
		// TODO process markets page url here.
	}
	
	private void addCompaniesPage(List<PageViewVO> companies, PageViewVO page) {
		// TODO process companies page url here.
	}
	
	private void addProductsPage(List<PageViewVO> products, PageViewVO page) {
		// TODO process products page url here.
	}
	
	private void addToolsPage(List<PageViewVO> tools, PageViewVO page) {
		// TODO process tools page url here.
	}
	
}
