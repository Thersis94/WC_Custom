package com.rezdox.action;

import java.util.Map;

//SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.http.CookieUtil;
import com.siliconmtn.db.pool.SMTDBConnection;

//WC Core
import com.smt.sitebuilder.action.SimpleActionAdapter;

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
		//TODO update this to be dynamic once we have a data-stor for the notifs.
		int count = 5;

		//set the total in a cookie.  This may be excessive for repeat calls to the rewards page, but ensures cached data is flushed
		CookieUtil.add(req, MY_NOTIFS, String.valueOf(count), "/", -1);
	}


	/*
	 * form-submittal - the user is cashing in points for a reward.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		//flush the session value so it's reloaded after the redirect
		CookieUtil.remove(req, MY_NOTIFS);
	}	
}