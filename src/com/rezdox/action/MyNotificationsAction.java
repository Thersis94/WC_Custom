package com.rezdox.action;

import java.util.List;
import java.util.Map;

//SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.http.CookieUtil;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.http.session.SMTCookie;
import com.siliconmtn.util.Convert;
//WC Core
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.tools.NotificationLogUtil;
import com.smt.sitebuilder.action.tools.NotificationLogVO;

/****************************************************************************
 * <b>Title:</b> MyNotificationsAction.java<br/>
 * <b>Description:</b> Displays the user's notifications (called for Dashboard page-load).  
 * Also puts the count into a cookie for use in ajax calls (for menu #).
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Apr 24, 2018
 ****************************************************************************/
public class MyNotificationsAction extends SimpleActionAdapter {

	public static final String MY_NOTIFS = "rezdoxMemberNotifications";


	public MyNotificationsAction() {
		super();
	}

	public MyNotificationsAction(ActionInitVO arg0) {
		super(arg0);
	}

	/**
	 * overloaded constructor to simplify calling actions
	 * @param dbConnection
	 * @param attributes
	 */
	public MyNotificationsAction(SMTDBConnection dbConnection, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConnection);
		setAttributes(attributes);
	}


	/*
	 * Display the user's points, and a list of available rewards if the Widget is in focus
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		int count = 0;
		String profileId = RezDoxUtils.getMember(req).getProfileId();

		if (req.hasParameter("countOnly")) {
			count = getCount(profileId);
		} else {
			//get the actual notifications so we can print them on-screen
			NotificationLogUtil util = new NotificationLogUtil(getDBConnection());
			List<NotificationLogVO> data = util.getNotifications(RezDoxUtils.MEMBER_SITE_ID, profileId);
			count = data.size();
			putModuleData(data);
		}

		//set the total in a cookie.  This may be excessive for repeat calls to the dashboard page, but ensures cached data is regularly flushed
		CookieUtil.add(req, MY_NOTIFS, String.valueOf(count), "/", -1);
	}


	/**
	 * returns a count of notifications for the given user
	 * @param profileId
	 * @return
	 */
	public int getCount(String profileId) {
		NotificationLogUtil util = new NotificationLogUtil(getDBConnection());
		return util.getNotificationCount(RezDoxUtils.MEMBER_SITE_ID, profileId);
	}


	/*
	 * form-submittal - the user is marking a notification as read - thereby hiding it from view.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		if (req.hasParameter("markRead")) {
			NotificationLogUtil util = new NotificationLogUtil(getDBConnection());
			util.markRead(req.getParameter("markRead"));
		}

		//decrement the cookie and re-set it
		SMTCookie cook = req.getCookie(MY_NOTIFS);
		int cnt = cook != null ? Convert.formatInteger(cook.getValue()) : 1;
		cnt = cnt > 0 ? cnt-1 : 0;

		CookieUtil.add(req, MY_NOTIFS, String.valueOf(cnt), "/", -1);
	}	
}