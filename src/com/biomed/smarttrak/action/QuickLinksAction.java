package com.biomed.smarttrak.action;

// Java 8
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.action.AdminControllerAction.Section;
// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.session.SMTSession;

// WebCrescendo
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.tools.MyFavoritesAction;
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

	public static final String MY_RECENTLY_VIEWED = "myRecentlyViewed";
	public static final char CHAR_SLASH = '/';
	public static final String LINK_TYPE = "type";
	public static final String LINK_TYPE_FAVORITES = "fv";
	public static final String LINK_TYPE_RECENTLY_VIEWED = "rv";
	public static final String URL_STUB = "qs/";
	public static final String PARAM_KEY_SECTION = "section";
	public static final String PARAM_KEY_NAME = "name";
	public static final String PARAM_KEY_URI_TXT = "uriTxt";
	public static final String PARAM_KEY_TYPE_CD = "typeCd";
	public static final String PARAM_KEY_REL_ID = "relId";
	public static final int MAX_LIST_SIZE = 3;

	/**
	 * Constructor
	 */
	public QuickLinksAction() {
		super();
	}

	/**
	 * Constructor
	 */
	public QuickLinksAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SMTSession sess = (SMTSession)req.getSession();
		if (sess.getAttribute(MyFavoritesAction.MY_FAVORITES) == null) {
			loadFavorites(req);
		}
		if (sess.getAttribute(MY_RECENTLY_VIEWED) == null) {
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
		log.debug("loadFavorites...");
		// call FavoritesAction
		ActionInterface ai = new FavoritesAction(actionInit);
		ai.setAttributes(getAttributes());
		ai.setDBConnection(dbConn);
		ai.retrieve(req);
		ModuleVO mod = (ModuleVO)getAttribute(Constants.MODULE_DATA);
		log.debug("favorites mod error condition: " + mod.getErrorCondition());
		if (! mod.getErrorCondition()) {
			SMTSession sess = req.getSession();
			Map<String, List<PageViewVO>> fv = (Map<String, List<PageViewVO>>) mod.getActionData();
			sess.setAttribute(MyFavoritesAction.MY_FAVORITES,fv);
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
			sess.setAttribute(MY_RECENTLY_VIEWED,rv);
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

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}


	/**
	 * iterates the session-stored List<PageViewVO> to see if the given ID for the given Section is on the list.
	 * Used in views to set button colors for 'Favorite' buttons.
	 * @param sec
	 * @param pkId
	 * @return
	 */
	public static boolean isFavorite(Map<String, List<PageViewVO>> data, Section sec, String pkId) {
		log.debug("isFavorite...");
		// no data, no match.
		if (data == null) return false;
		// unpack target section
		List<PageViewVO> section = data.get(sec);
		// invalid section key, or empty list, no match.
		if (section == null || section.isEmpty()) return false;
		// loop pageviews looking for match.
		for (PageViewVO page : section) {
			if (pkId == page.getPageId()) return true;
		}
		return false;
	}

}
