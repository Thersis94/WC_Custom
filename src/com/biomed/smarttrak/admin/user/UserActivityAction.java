package com.biomed.smarttrak.admin.user;

//Java 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

// WebCrescendo libs
import com.smt.sitebuilder.action.SBActionAdapter;
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
		
		List<UserActivityVO> userPageViews =  null;
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		String profileId = (req.hasParameter("profileId") ? req.getParameter("profileId") : null);
		String dateStart = (req.hasParameter("dateStart") ? req.getParameter("dateStart") : null);
		String dateEnd = (req.hasParameter("dateEnd") ? req.getParameter("dateEnd") : null);
		try {
			userPageViews = retrieveUserPageViews(site, profileId, dateStart, dateEnd);
		} catch (ActionException ae) {
			mod.setError(ae);
		}
		
		mod.setActionData(userPageViews);
	}

	/**
	 * Retrieves page view history for 
	 * @return
	 * @throws ActionException
	 */
	protected List<UserActivityVO> retrieveUserPageViews(SiteVO site, String profileId,
			String dateStart, String dateEnd) throws ActionException {
		
		boolean startDateOnly = (dateStart != null && dateEnd == null);
		boolean noDates = (dateStart == null && dateEnd == null);
		
		/* Retrieve page views from db, parse into PageViewVO
		 * and return list */
		StringBuilder sql = formatQuery(site, profileId, noDates, startDateOnly);
		
		List<UserActivityVO> userActivity = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			
			formatPreparedStatement(ps,site,profileId,noDates,startDateOnly, dateStart, dateEnd);
			
			ResultSet rs = ps.executeQuery();
			parseResultSet(rs, userActivity);

		} catch (SQLException sqle) {
			log.error("Error retrieving user page views, ", sqle);
			throw new ActionException(sqle.getMessage());
		}
		
		return userActivity;
	}
	
	private void formatPreparedStatement(PreparedStatement ps, SiteVO site, 
			String profileId, boolean noDates, boolean startDateOnly, String dateStart, 
			String dateEnd) throws SQLException {
		int idx = 1;
		if (noDates || startDateOnly) {
			ps.setDate(idx++, Convert.formatSQLDate(formatSingleStartDate(dateStart, noDates)));
		} else {
			ps.setDate(idx++, Convert.formatSQLDate(Convert.formatStartDate(dateStart)));
			ps.setDate(idx++, Convert.formatSQLDate(Convert.formatEndDate(dateEnd)));
		}
		if (site.getSiteId() != null) ps.setString(idx++, site.getSiteId());
		if (profileId != null) ps.setString(idx++, profileId);		
	}
	
	private void parseResultSet(ResultSet rs, List<UserActivityVO> userActivity) 
			throws SQLException {
		PageViewVO pvo = null;
		List<PageViewVO> pageViews = null;
		while (rs.next()) {
			pvo = new PageViewVO();
			pvo.setSiteId(rs.getString("siteId"));
			pvo.setProfileId(rs.getString("profileId"));
			pvo.setSessionId(rs.getString("session_id"));
			pvo.setPageId(rs.getString("page_id"));
			pvo.setRequestUri(rs.getString("request_uri_txt"));
			pvo.setQueryString(rs.getString("query_str_txt"));
			pvo.setPageViewId(rs.getInt("src_pageviewId"));
			pvo.setVisitDate(rs.getDate("visit_dt"));
			pageViews.add(pvo);
		}
	}
	
	/**
	 * Formats the page view retrieval query
	 * @param site
	 * @param profileId
	 * @param noDates
	 * @param startDateOnly
	 * @return
	 */
	private StringBuilder formatQuery(SiteVO site, String profileId, 
			boolean noDates, boolean startDateOnly) {
		/* Retrieve page views from db, parse into PageViewVO
		 * and return list */
		StringBuilder sql = new StringBuilder(350);
		sql.append("select pageview_user_id, site_id, profile_id, session_id, ");
		sql.append("page_id, request_uri_txt, query_str_txt, src_pageview_id, ");
		sql.append("visit_dt from pageview_user ");
		if (noDates || startDateOnly) {
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
	private Date formatSingleStartDate(String dateStart, boolean noDates) {
		Date d = null;
		if (noDates) {
			// return the date/time from 12 hours ago
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTime(Convert.formatDate(dateStart));
			cal.add(Calendar.HOUR_OF_DAY, -12);
			d = cal.getTime();
		} else {
			// return "today @ midnight"
			d = Convert.formatStartDate(d);
		}
		return d;
	}
}
