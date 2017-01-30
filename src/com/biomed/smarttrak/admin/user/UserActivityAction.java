package com.biomed.smarttrak.admin.user;

//Java 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.StringEncrypter;

// WebCrescendo libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.PageViewRetriever;
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
		
		Map<String,UserActivityVO> userActivity =  null;
		String siteId = parseSiteId(req);
		String profileId = (req.hasParameter("profileId") ? req.getParameter("profileId") : null);
		String dateStart = (req.hasParameter("dateStart") ? req.getParameter("dateStart") : null);
		String dateEnd = (req.hasParameter("dateEnd") ? req.getParameter("dateEnd") : null);
		log.debug("siteId | profileId: " + siteId + " | " + profileId);
		log.debug("dateStart | dateEnd: " + dateStart + " | " + dateEnd);
		
		try {
			/* Check caller security here so that we can gracefully catch/set the error 
			 * on the module response if the caller has an insufficient role level. */
			checkSecurityRole(req);
			
			userActivity = retrieveUserPageViews(siteId, profileId, dateStart, dateEnd);
			// merge certain profile data (first/last names) with user activity data
			mergeUserNames(userActivity);
			
		} catch (ActionException ae) {
			mod.setError(ae.getMessage(), ae);
		}
		
		if (userActivity == null) userActivity = new HashMap<>();
		this.putModuleData(userActivity, userActivity.size(), false, mod.getErrorMessage(), mod.getErrorCondition());
		
	}
	
	/**
	 * Checks caller's security role and throws an exception if insufficient role level is found.
	 * @param req
	 * @throws ActionException
	 */
	private void checkSecurityRole(SMTServletRequest req) throws ActionException {
		String errMsg = "Widget access not authorized, Site Administrator role required.";
		HttpSession sess = req.getSession();
		if (sess == null) throw new ActionException(errMsg);
		SBUserRole roles = (SBUserRole)sess.getAttribute(Constants.ROLE_DATA);
		if (roles == null || roles.getRoleLevel() < SecurityController.ADMIN_ROLE_LEVEL) 
			throw new ActionException(errMsg);
	}
	
	/**
	 * Determines the siteId value to use for this retrieving page views.
	 * @param req
	 * @return
	 */
	private String parseSiteId(SMTServletRequest req) {
		if (req.hasParameter("siteId")) {
			return req.getParameter("siteId");
		} else {
			SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
			return (site.getSiteId());
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
	private Map<String,UserActivityVO> retrieveUserPageViews(String siteId, String profileId,
			String dateStart, String dateEnd) throws ActionException {
		
		/* Retrieve page views from db, parse into PageViewVO
		 * and return list */
		PageViewRetriever pvr = new PageViewRetriever(dbConn);
		List<PageViewVO> pageViews = pvr.retrievePageViews(siteId, profileId, dateStart, dateEnd);
		log.debug("Total number of raw page views found: " + pageViews.size());
		return parseResults(pageViews);
	}
	
	/**
	 * Parses the resulting list of page views into a map of profile IDs mapped to UserActivityVOs.
	 * @param pageViews
	 * @return
	 */
	private Map<String,UserActivityVO> parseResults(List<PageViewVO> pageViews) {
		UserActivityVO user = null;
		Map<String,UserActivityVO> userActivity = new HashMap<>();
		String prevPid = null;
		String currPid;
		
		for (PageViewVO pageView : pageViews ) {
			
			currPid = pageView.getProfileId();
			if (currPid.equals(prevPid)) {
				// add pageview to current user's list
				user.addPageView(pageView);
				
			} else {
				// first time through loop or we changed users
				if (user != null) {
					// close out prev user
					user.setLastAccessTime();
					userActivity.put(user.getProfileId(), user);
				}
				// capture new user
				user = new UserActivityVO();
				user.setSiteId(pageView.getSiteId());
				user.setProfileId(pageView.getProfileId());
				user.addPageView(pageView);
			}
			
			prevPid = currPid;
		}
		
		// tie off the dangling user
		if (user != null) {
			user.setLastAccessTime();
			userActivity.put(user.getProfileId(), user);
		}
		log.debug("Unique users with page views: " + userActivity.size());
		return userActivity;
	}
		
	/**
	 * Retrieves and merges specific profile data values into user activity VOs. This uses
	 * a batching mechanism for efficiency.
	 * @param userActivity
	 */
	private void mergeUserNames(Map<String,UserActivityVO> userActivity) {
		if (userActivity.isEmpty()) return;
		// instantiate StringEncrypter or die
		StringEncrypter se = null;
		try {
			se = new StringEncrypter((String)getAttributes().get(Constants.ENCRYPT_KEY));
		} catch (Exception e) {
			log.error("Error instantiating StringEncrypter, ", e);
			return;
		}
		// batch loop the profile IDs and retrieve just the first/last names encrypted
		int listSize = userActivity.keySet().size();
		int maxBatch = 100;
		int start = 0;
		int end = (listSize > maxBatch) ? maxBatch : listSize;
		// convert profile IDs into an iterable object with guaranteed order.
		String[] ids = userActivity.keySet().toArray(new String[0]);
		do {
			retrieveUserNames(se, userActivity, ids, start, end);
			start = end;
			end = (listSize > end + maxBatch) ? end + maxBatch : listSize;
		} while (start < listSize);
	}
	
	/**
	 * Retrieves and merges specific profile data values (first/last names) into user activity VOs.
	 * This purposely does not leverage ProfileManager as we do not need entire profiles 
	 * returned (expensive!).  Instead, this directly queries the profile table for
	 * just the encrypted first/last name values and decrypts them before setting them on the
	 * appropriate user activity VO.
	 * @param se
	 * @param userActivity
	 * @param profileIds
	 * @param start
	 * @param end
	 */
	private void retrieveUserNames(StringEncrypter se, Map<String,UserActivityVO> userActivity, 
			String[] profileIds, int start, int end) {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select profile_id, first_nm, last_nm from profile where profile_id in ");
		sql.append("(");
		for (int i = start; i < end; i++) {
			if (i > start) sql.append(",");
			sql.append("?");
		}
		sql.append(")");
		log.debug("Profile names SQL: " + sql.toString());
		int idx = 1;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (int i = start; i < end; i++) {
				ps.setString(idx++, profileIds[i]);
			}
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				formatNameValues(se, rs, userActivity.get(rs.getString("profile_id")));
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving user profile data (first/last names), ", sqle);
		}
	}
	
	/**
	 * Parses and decrypts user's first/last name from result set and sets values
	 * on user's activity VO.
	 * @param se
	 * @param rs
	 * @param user
	 * @throws SQLException
	 */
	private void formatNameValues(StringEncrypter se, ResultSet rs, 
			UserActivityVO user) throws SQLException {
		if (user == null) return;
		user.setFirstName(decryptName(se,rs.getString("first_nm")));
		user.setLastName(decryptName(se,rs.getString("last_nm")));
	}
	
	/**
	 * Decrypts an encrypted String value.
	 * @param se
	 * @param encrypted
	 * @return
	 */
	private String decryptName(StringEncrypter se, String encrypted) {
		if (encrypted == null) return encrypted;
		try {
			return se.decrypt(encrypted);
		} catch (Exception e) {
			log.error("Error decrypting String, ", e);
			return encrypted;
		}
	}
}
