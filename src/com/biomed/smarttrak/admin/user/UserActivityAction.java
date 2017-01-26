package com.biomed.smarttrak.admin.user;

//Java 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;

// WebCrescendo libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.PageViewVO;

/*****************************************************************************
 <p><b>Title</b>: UserActivityAction.java</p>
 <p><b>Description: </b>Retrieves user activity based on page view history data.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Jan 17, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserActivityAction extends SBActionAdapter {

	private final int HISTORY_INTERVAL_DEFAULT = -12;
	
	/**
	 * Constructor
	 */
	public UserActivityAction() {
		super();
	}
	
	/**
	 * Constructor
	 */
	public UserActivityAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		
		Map<String,UserActivityVO> userActivity =  null;
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		String profileId = (req.hasParameter("profileId") ? req.getParameter("profileId") : null);
		String dateStart = (req.hasParameter("dateStart") ? req.getParameter("dateStart") : null);
		String dateEnd = (req.hasParameter("dateEnd") ? req.getParameter("dateEnd") : null);
		
		try {
			userActivity = retrieveUserPageViews(site, profileId, dateStart, dateEnd);
		} catch (ActionException ae) {
			mod.setError(ae);
		}
		
		mod.setActionData(userActivity);
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
	protected Map<String,UserActivityVO> retrieveUserPageViews(SiteVO site, String profileId,
			String dateStart, String dateEnd) throws ActionException {
		
		/* Retrieve page views from db, parse into PageViewVO
		 * and return list */
		StringBuilder sql = formatQuery(site, profileId, dateStart, dateEnd);
		Map<String,UserActivityVO> userActivity = new HashMap<>();
		//List<UserActivityVO> userActivity = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			
			formatPreparedStatement(ps, site, profileId, dateStart, dateEnd);
			
			ResultSet rs = ps.executeQuery();
			parseResults(rs, userActivity);

		} catch (SQLException sqle) {
			log.error("Error retrieving user page views, ", sqle);
			throw new ActionException(sqle.getMessage());
		}
		
		// retrieve user data
		retrieveUsers(userActivity);
		
		return userActivity;
	}
	
	/**
	 * Formats the prepared statement for the query
	 * @param ps
	 * @param site
	 * @param profileId
	 * @param dateStart
	 * @param dateEnd
	 * @throws SQLException
	 */
	private void formatPreparedStatement(PreparedStatement ps, SiteVO site, 
			String profileId, String dateStart, String dateEnd) throws SQLException {
		int idx = 1;
		if (useSingleDate(dateStart,dateEnd)) {
			ps.setDate(idx++, Convert.formatSQLDate(formatSingleStartDate(dateStart)));
		} else {
			ps.setDate(idx++, Convert.formatSQLDate(Convert.formatStartDate(dateStart)));
			ps.setDate(idx++, Convert.formatSQLDate(Convert.formatEndDate(dateEnd)));
		}
		if (site.getSiteId() != null) ps.setString(idx++, site.getSiteId());
		if (profileId != null) ps.setString(idx++, profileId);		
	}
	
	/**
	 * Parses the result set.
	 * @param rs
	 * @param userActivity
	 * @return
	 * @throws SQLException
	 */
	private void parseResults(ResultSet rs, Map<String,UserActivityVO> userActivity) 
			throws SQLException {
		UserActivityVO user = null;
		PageViewVO pageView = null;
		List<PageViewVO> pageViews = null;
		String prevPid = null;
		String currPid;
		
		while (rs.next()) {
			currPid = rs.getString("profile_id");

			// capture the current page view
			pageView = new PageViewVO();
			pageView.setSiteId(rs.getString("siteId"));
			pageView.setProfileId(rs.getString("profileId"));
			pageView.setSessionId(rs.getString("session_id"));
			pageView.setPageId(rs.getString("page_id"));
			pageView.setRequestUri(rs.getString("request_uri_txt"));
			pageView.setQueryString(rs.getString("query_str_txt"));
			pageView.setPageViewId(rs.getInt("src_pageviewId"));
			pageView.setVisitDate(rs.getDate("visit_dt"));
			
			if (currPid.equals(prevPid)) {
				// add the pageview to the current user's list
				pageViews.add(pageView);
			} else {
				// either first time through or we changed users
				if (pageViews != null) {
					// changed users, close out prev user
					// capture the 'last accessed' time from the last page view.
					user.setLastAccessTime(pageViews.get(pageViews.size() - 1).getVisitDate());
					user.setPageViews(pageViews);
					//userActivity.add(user);
					userActivity.put(user.getProfileId(), user);
				}
				// establish new user data
				pageViews = new ArrayList<>();
				pageViews.add(pageView);
				
				user = new UserActivityVO();
				user.setSiteId(pageView.getSiteId());
				user.setProfileId(pageView.getProfileId());
			}
			
		}
		
		// tie off the dangling user
		if (user != null) {
			user.setLastAccessTime(pageViews.get(pageViews.size() - 1).getVisitDate());
			user.setPageViews(pageViews);
			//userActivity.add(user);
			userActivity.put(user.getProfileId(), user);
		}
		
	}
	
	private void retrieveUsers(Map<String,UserActivityVO> userActivity) {
		StringEncrypter se = null;
		try {
			se = new StringEncrypter("");
		} catch (Exception e) {
			log.error("Error instantiating StringEncrypter, ", e);
			return;
		}
		int listSize = userActivity.keySet().size();
		int maxBatch = 100;
		int start = 0;
		int end = (listSize > maxBatch) ? maxBatch : listSize;
		do {
			retrieveUserNames(se, userActivity.keySet(), start, end);
			start = end;
			end = (listSize > end + maxBatch) ? end + maxBatch : listSize;
			
		} while (start < listSize);
	}
	
	
	private void retrieveUserNames(StringEncrypter se, Set<String> profileIds, start, end) {
		
	}
	
	/**
	 * Decrypts an encrypted String value.
	 * @param se
	 * @param encrypted
	 * @return
	 */
	private String decrypt(StringEncrypter se, String encrypted) {
		if (encrypted == null) return encrypted;
		try {
			return se.decrypt(encrypted);
		} catch (Exception e) {
			log.error("Error decrypting String, ", e);
			return encrypted;
		}
		
	}
	
	/**
	 * Formats the page view retrieval query
	 * @param site
	 * @param profileId
	 * @param dateStart
	 * @param dateEnd
	 * @return
	 */
	private StringBuilder formatQuery(SiteVO site, String profileId, 
			String dateStart, String dateEnd) {
		/* Retrieve page views from db, parse into PageViewVO
		 * and return list */
		StringBuilder sql = new StringBuilder(350);
		sql.append("select pageview_user_id, site_id, profile_id, session_id, ");
		sql.append("page_id, request_uri_txt, query_str_txt, src_pageview_id, ");
		sql.append("visit_dt from pageview_user ");
		if (useSingleDate(dateStart,dateEnd)) {
			sql.append("where visit_dt >= ?  ");
		} else {
			sql.append("where visit_dt between ? and ? ");
		}
		
		if (site.getSiteId() != null) sql.append("and site_id = ? ");
		if (profileId != null) sql.append("and profile_id = ? ");
		sql.append("order by site_id, profile_id, visit_dt");
		return sql;
	}
	
	/**
	 * Formats the start date to use for the page view query.  If no start date is supplied
	 * we use "now minus 12 hours".
	 * @param dateStart
	 * @param noDates
	 * @return
	 */
	private Date formatSingleStartDate(String dateStart) {
		if (dateStart == null) {
			// return "now minus 12 hours"
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTime(Convert.formatDate(dateStart));
			cal.add(Calendar.HOUR_OF_DAY, HISTORY_INTERVAL_DEFAULT);
			return cal.getTime();
		} else {
			// return "today @ 00:00:00"
			return Convert.formatStartDate(dateStart);
		}
	}

	/**
	 * Convenience method to determine whether or not to use a single
	 * date when formatting/executing for this query. Returns true only
	 * if dateEnd is null, otherwise returns false.
	 * @param dateStart
	 * @param dateEnd
	 * @return
	 */
	private boolean useSingleDate(String dateStart, String dateEnd) {
		if (dateEnd == null) return true;
		return false;
	}
}
