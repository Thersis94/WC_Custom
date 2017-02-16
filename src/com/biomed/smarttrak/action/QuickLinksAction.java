package com.biomed.smarttrak.action;

import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.session.SMTSession;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.PageViewVO;

/*****************************************************************************
 <p><b>Title</b>: QuickLinksAction.java</p>
 <p><b>Description: </b>Action that wraps Favorites and Recently Viewed management.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author groot
 @version 1.0
 @since Feb 16, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class QuickLinksAction extends SBActionAdapter {

	public static final String USER_FAVORITES = "userFavorites";
	public static final String USER_RECENTLY_VIEWED = "userRecentlyViewed";
	public static final String LINK_TYPE = "type";
	public static final String LINK_TYPE_FAVORITES = "fv";
	public static final String LINK_TYPE_RECENTLY_VIEWED = "rv";
	
	/**
	* Constructor
	*/
	public QuickLinksAction() {
		// constructor stub
	}

	/**
	* Constructor
	*/
	public QuickLinksAction(ActionInitVO actionInit) {
		super(actionInit);
		// constructor stub
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SMTSession sess = (SMTSession)req.getSession();
		if (sess.getAttribute(USER_FAVORITES) == null) {
			loadFavorites(req);
		}
		if (sess.getAttribute(USER_RECENTLY_VIEWED) == null) {
			loadRecentlyViewed(req);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		String type = req.getParameter(LINK_TYPE);
		if (type.equalsIgnoreCase(LINK_TYPE_FAVORITES)) {
			manageFavorites(req);
		} else if (type.equalsIgnoreCase(LINK_TYPE_RECENTLY_VIEWED)) {
			manageRecentlyViewed(req);
		}		
	}
	
	/**
	 * 
	 * @param req
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	protected void loadFavorites(ActionRequest req) throws ActionException {
		// call FavoritesAction
		ActionInterface ai = new FavoritesAction(actionInit);
		ai.setAttributes(getAttributes());
		ai.setDBConnection(dbConn);
		ai.retrieve(req);
		ModuleVO mod = (ModuleVO)getAttribute(Constants.MODULE_DATA);
		if (! mod.getErrorCondition()) {
			SMTSession sess = req.getSession();
			Map<String, List<PageViewVO>> fv = (Map<String, List<PageViewVO>>) mod.getActionData();
			sess.setAttribute(USER_FAVORITES,fv);
		}
	}
	
	/**
	 * 
	 * @param req
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	protected void loadRecentlyViewed(ActionRequest req) throws ActionException {
		// call RecentlyViewedAction
		ActionInterface ai = new RecentlyViewedAction(actionInit);
		ai.setAttributes(getAttributes());
		ai.setDBConnection(dbConn);
		ai.retrieve(req);
		ModuleVO mod = (ModuleVO)getAttribute(Constants.MODULE_DATA);
		if (! mod.getErrorCondition()) {
			SMTSession sess = req.getSession();
			Map<String, List<PageViewVO>> rv = (Map<String, List<PageViewVO>>) mod.getActionData();
			sess.setAttribute(USER_RECENTLY_VIEWED,rv);
		}
	}
	
	/**
	 * 
	 * @param req
	 * @throws ActionException 
	 */
	protected void manageFavorites(ActionRequest req) 
			throws ActionException {
		// manage Favorites session obj
		ActionInterface ai = new FavoritesAction(actionInit);
		ai.setAttributes(getAttributes());
		ai.setDBConnection(dbConn);
		ai.build(req);
	}
	
	/**
	 * 
	 * @param req
	 * @throws ActionException 
	 */
	protected void manageRecentlyViewed(ActionRequest req) 
			throws ActionException {
		// manage Recently Viewed session obj
		ActionInterface ai = new RecentlyViewedAction(actionInit);
		ai.setAttributes(getAttributes());
		ai.setDBConnection(dbConn);
		ai.build(req);
	}
}
