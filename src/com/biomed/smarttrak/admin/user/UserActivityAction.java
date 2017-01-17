package com.biomed.smarttrak.admin.user;

// Java 7
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

// SMTBaseLibs 2
import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

// WebCrescendo
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: UserActivityAction.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Jan 13, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserActivityAction extends SimpleActionAdapter {
	private static final String OP_GET_LAST_ACCESS = "getLastAccessedTimestamp";
	private static final String OP_GET_ATTRIBUTE = "getSessionAttribute";
	private static final String OP_LIST_SESSION_IDS = "listSessionIds";
	private static final String KEY_SITE_TRACK_ID = "siteTrackId";
	private static final String KEY_USER_DATA = "userData";
	
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		// TODO consider wrapping method call with try/catch so we can set an error msg if needed.
		List<UserActivityVO> userActivity = getUserActivity(req, (String)getAttribute(Constants.CONTEXT_PATH));
		mod.setActionData(userActivity);
		this.putModuleData(mod);
	}
	
	/**
	 * Override to do nothing.
	 */
	public void build(SMTServletRequest req) throws ActionException {
		return;
	}
		
	/**
	 * 
	 * @param req
	 * @param context
	 */
	private List<UserActivityVO> getUserActivity(SMTServletRequest req, String context) {
		String targetSiteId = StringUtil.checkVal(req.getParameter(KEY_SITE_TRACK_ID));
		List<UserActivityVO> userActivity = new ArrayList<>();

		// 1. retrieve session activity
		retrieveSessionActivity(context, targetSiteId, userActivity);

		// 2. retrieve user activity data
		retrieveLoggedActivity(userActivity);

		return userActivity;
	}
	
	/**
	 * 
	 * @param context
	 * @param targetSiteId
	 * @param userActivity
	 */
	private void retrieveSessionActivity(String context, String targetSiteId, List<UserActivityVO> userActivity) {
		MBeanServer mbServer;
		ObjectName mbObj; 
		try {
			// establish mgmt object for this app context
			mbServer = ManagementFactory.getPlatformMBeanServer();
			mbObj = new ObjectName("Catalina:type=Manager,context="+context+",host=localhost");
		} catch (Exception e) {
			log.error("Error obtaining handle to management context, ", e);
			return;
		}
		
		String sessionIdList;
		try {
			sessionIdList = retrieveSessionIdList(mbServer, mbObj);
		} catch (Exception e) {
			log.error("Error retrieving session ID list, ", e);
			return;
		}

		retrieveSessionAttributes(mbServer, mbObj, userActivity, sessionIdList, targetSiteId);
		
	}
	
	/**
	 * 
	 * @param mbServer
	 * @param mbObj
	 * @return
	 * @throws InstanceNotFoundException
	 * @throws ReflectionException
	 * @throws MBeanException
	 */
	private String retrieveSessionIdList(MBeanServer mbServer, ObjectName mbObj) 
			throws InstanceNotFoundException, ReflectionException, MBeanException {
		return (String) mbServer.invoke(mbObj, OP_LIST_SESSION_IDS, new Object[]{}, new String[]{});
	}
	
	/**
	 * Retrieves session attributes for a list of sessions that match a target site ID.
	 * @param mbServer
	 * @param mbObj
	 * @param userActivity
	 * @param sessionIdList
	 * @param targetSiteId
	 * @return List of UserActivityVO
	 */
	private List<UserActivityVO> retrieveSessionAttributes(MBeanServer mbServer, ObjectName mbObj, 
			List<UserActivityVO> userActivity, String sessionIdList, String targetSiteId) {
		// parse the session ID list into an array
		String[] sessionIds = sessionIdList.split(" ");
		String sessionTrackId;
		UserDataVO profile;
		long lastAccess;
		UserActivityVO vo;
		if (sessionIds != null) {
			for (String sessionId : sessionIds) {
				if (sessionId.length() == 0) continue;
				try {
					// retrieve the site tracking ID and compare with our target site ID.
					sessionTrackId = (String)mbServer.invoke(mbObj, OP_GET_ATTRIBUTE, new Object[]{sessionId, KEY_SITE_TRACK_ID}, new String[]{String.class.getName(),String.class.getName()});
					if (sessionTrackId.equals(targetSiteId)) {
						// retrieve the user's profile.
						profile = (UserDataVO)mbServer.invoke(mbObj, OP_GET_ATTRIBUTE, new Object[]{sessionId, KEY_USER_DATA}, new String[]{String.class.getName(),String.class.getName()});
						if (profile != null) {
							// retrieve the user's most recent access time.
							lastAccess = (Long)mbServer.invoke(mbObj, OP_GET_LAST_ACCESS, new Object[]{sessionId}, new String[]{String.class.getName()});
							vo = new UserActivityVO();
							vo.setSessionId(sessionId);
							vo.setProfile(profile);
							vo.setLastAccessTime(lastAccess);
							userActivity.add(vo);
						}
					}
				} catch (Exception e) {
					log.error("Error retrieving session data for session ID: " + sessionId + ", ", e);
				}
			}
		}
		return userActivity;
	}

	/**
	 * Retrieves user activity log data and merges it with user session data. 
	 */
	private void retrieveLoggedActivity(List<UserActivityVO> sessionLogs) {
		// 1. retrieve activity log data for a certain time TIME_INTERVAL (? 8, 12, 24 hours ?)
		/* TODO 
		 * 1. retrieve user activity data from activity log (page views, etc.) */
		List<UserActivityVO> pageLogs = retrievePageLogs();
		/* 2. merge activity data with user's session data
		 * 		2a. add new vo to List for each user who has log data within requested TIME_INTERVAL
		 * 			but who currently doesn't have a session.  Determine 'last accessed' time from 
		 * 			user's log data timestamp.
		 * 		2b. Need to use a field on the PageVO as an indicator as to the page link type 
		 * 			 (archives, markets, companies, products, or just a page) for the links we will
		 * 			 build in the JSTL.  We don't build a link for a page, but do build a link for a company or an
		 * 			 article. */
		mergeLogs(sessionLogs, pageLogs);
		 /* 3. sort via comparator?
		 */
	}
	
	/**
	 * 
	 * @return
	 */
	private List<UserActivityVO> retrievePageLogs() {
		/* retrieve logged data from db */
		List<UserActivityVO> pageLogs = new ArrayList<>();
		/*
		 * retrieve logs from db for given time interval
		 * collate/aggregate logs for each user into a vo
		 */
		return pageLogs;
	}
	
	/**
	 * 
	 * @param sessionLogs
	 * @param pageLogs
	 */
	private void mergeLogs(List<UserActivityVO> sessionLogs, List<UserActivityVO> pageLogs) {
		/* loop page logs and merge session data into the list  */
		for (UserActivityVO session : sessionLogs) {
			for (UserActivityVO pages : pageLogs) {
				if (session.getProfile().getProfileId().equals(pages.getProfile().getProfileId())) {
					// merge.
				}
			}
		}
	}

}
