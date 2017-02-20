package com.biomed.smarttrak.action;

//Java 8
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Apache Solr
import org.apache.solr.common.SolrDocument;

// WC custom
import com.biomed.smarttrak.action.AdminControllerAction.Section;

//SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.StringUtil;
// WebCrescendo libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.tools.FavoriteVO;
import com.smt.sitebuilder.action.tools.MyFavoritesAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.util.PageViewVO;

/*****************************************************************************
 <p><b>Title</b>: FavoritesAction.java</p>
 <p><b>Description: </b>Manages a user's 'favorites'.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 16, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class FavoritesAction extends SBActionAdapter {
	
	/**
	 * Constructor
	 */
	public FavoritesAction() {
		super();
	}
	
	/**
	 * Constructor
	 */
	public FavoritesAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("FavoritesAction retrieve...");
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		if (role == null) throw new ActionException("Not logged in.");
		ModuleVO mod;
		// retrieve user favorites
		ActionInterface ai = new MyFavoritesAction(actionInit);
		ai.setAttributes(getAttributes());
		ai.setDBConnection(dbConn);
		try {
			ai.retrieve(req);
			mod = (ModuleVO)getAttribute(Constants.MODULE_DATA);
		} catch (ActionException ae) {
			mod = new ModuleVO();
			mod.setErrorCondition(true);
			mod.setError(ae);
		}

		Map<String, List<PageViewVO>> favs = parseFavorites(mod);

		log.debug("FavoritesAction retrieve error condition | message: " + mod.getErrorCondition() + "|" + mod.getErrorMessage());
		log.debug("favs size: " + favs.size());
		this.putModuleData(favs, favs.size(), false, mod.getErrorMessage(), mod.getErrorCondition());
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("FavoritesAction build...");
		
		PageViewVO fav = buildFavoritePage(req);
		if (fav == null) return;
		
		// 1. add favorite to user's profile favorites
		updateProfileFavorites(req,fav);

	}
	

	/**
	 * Builds a PageViewVO representing the page being favorited using request params.
	 * @param req
	 * @return
	 */
	protected PageViewVO buildFavoritePage(ActionRequest req) {
		PageViewVO fav = null;
		String collKey = req.getParameter(QuickLinksAction.PARAM_KEY_SECTION);
		try {
			checkCollectionKey(collKey);
		} catch (ActionException ae) {
			log.error("Error: " + ae.getMessage());
			return fav;
		}
		
		String uriTxt = req.getParameter(QuickLinksAction.PARAM_KEY_URI_TXT);
		String pkId = parsePrimaryId(uriTxt);
		if (pkId == null) return fav;
		
		// format fav page
		fav = new PageViewVO();
		fav.setReferenceCode(collKey);
		fav.setPageId(pkId);
		fav.setRequestUri(uriTxt);
		fav.setPageDisplayName(req.getParameter(QuickLinksAction.PARAM_KEY_NAME));
		
		return fav;
	}
	
	/**
	 * Parses the primary section entity ID from the request uri.
	 * @param uriTxt
	 * @return
	 */
	protected String parsePrimaryId(String uriTxt) {
		String id = null;
		int idx = uriTxt.lastIndexOf(QuickLinksAction.CHAR_SLASH);
		if (idx < uriTxt.length()) 
			id = uriTxt.substring(idx+1);
		return id;
	}

	/**
	 * Adds the favorite to the user's profile favorites.
	 * @param req
	 * @param page
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	protected void updateProfileFavorites(ActionRequest req, PageViewVO page) throws ActionException {
		log.debug("persistProfileFavorite...");
		// set additional req params needed downstream for inserts.
		req.setParameter(QuickLinksAction.PARAM_KEY_TYPE_CD, page.getReferenceCode());
		req.setParameter(QuickLinksAction.PARAM_KEY_REL_ID, page.getPageId());
		
		ActionInterface ai = new MyFavoritesAction(getActionInit());
		ai.setAttributes(getAttributes());
		ai.setDBConnection(dbConn);
		ai.build(req);
		
		/* now retrieve favorites again as the action we called removes favorites
		 * from the user's session. */
		this.retrieve(req);
		ModuleVO mod = (ModuleVO)getAttribute(Constants.MODULE_DATA);
		if (mod != null) {
			Map<String,List<PageViewVO>> favs = (Map<String,List<PageViewVO>>)mod.getActionData();
			req.getSession().setAttribute(MyFavoritesAction.MY_FAVORITES, favs);
		}
	}
	
	/**
	 * Retrieves collection map from session and updates the appropriate collection with 
	 * the passed in favorite.
	 * @param req
	 * @param fav
	 */
	@SuppressWarnings("unchecked")
	protected void updateSessionMap(ActionRequest req, PageViewVO fav) {
		SMTSession sess = req.getSession();
		Map<String, List<PageViewVO>> favMap = (Map<String,List<PageViewVO>>)sess.getAttribute(MyFavoritesAction.MY_FAVORITES);
		// if no map found on session, create a map.
		if (favMap == null) favMap = new HashMap<>();

		// get the proper collection or create it if it doesn't exist
		List<PageViewVO> favs = favMap.get(fav.getReferenceCode());
		if (favs == null) {
			favs = new ArrayList<>();
		}

		// add to top of list
		favs.add(0,fav);

		// if list size > max, remove 1
		if (favs.size() > QuickLinksAction.MAX_LIST_SIZE) 
			favs.remove(favs.size() - 1);

		// update collection on map
		favMap.put(fav.getReferenceCode(), favs);

		// update collection on session
		sess.setAttribute(MyFavoritesAction.MY_FAVORITES, favMap);
	}
	
	/**
	 * Retrieves the map collection key by using the enum to validate the 
	 * section value passed in.
	 * @param section
	 * @return
	 * @throws ActionException 
	 */
	protected String checkCollectionKey(String section) throws ActionException {
		log.debug("evaluating section val: " + section);
		String key = StringUtil.checkVal(section).toUpperCase();
		try {
			Section.valueOf(key);
		} catch (Exception e) {
			throw new ActionException("Unknown section value: " + section);
		}
		return key;
	}
	
	/**
	 * Parses a user's favorites into a map of page views keyed by section (e.g. MARKET,
	 * COMPANY, etc.).
	 * @param mod
	 * @return
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, List<PageViewVO>> parseFavorites(ModuleVO mod) 
			throws ActionException {
		log.debug("parsing favorites...");
		if (mod.getErrorCondition()) return new HashMap<>();

		List<FavoriteVO> favs = (List<FavoriteVO>)mod.getActionData();
		log.debug("favs size: " + favs != null ? favs.size() : "null");
		Map<String, List<PageViewVO>> pageMap = initializePageMap();

		for (FavoriteVO fav : favs) {
			log.debug("found fav, typeCd | relId | uriTxt: " + fav.getTypeCd() + "|" + fav.getRelId() + "|" + fav.getUriTxt());
			if (fav.getAsset() == null) continue; 
			parseFavorite(pageMap,fav);
		}
		return pageMap;
	}
	
	/**
	 * Parses a Favorite asset to determine if it is a section page favorite. If so, the asset is
	 * parsed into a PageViewVO and is added to the appropriate collection of pages on the page map.
	 * @param pages
	 * @param fav
	 */
	protected void parseFavorite(Map<String,List<PageViewVO>> pages, FavoriteVO fav) {
		try {
			checkCollectionKey(fav.getTypeCd());
		} catch (Exception e) {
			// this fav is not a 'Section' type so return.
			return;
		}

		PageViewVO page = new PageViewVO();
		page.setReferenceCode(fav.getTypeCd());
		page.setPageId(fav.getRelId());
		page.setRequestUri(fav.getUriTxt());

		if (fav.getAsset() != null) {
			SolrDocument sDoc = (SolrDocument)fav.getAsset();
			page.setPageDisplayName(sDoc.getFieldValue(SearchDocumentHandler.TITLE).toString());
		}

		log.debug("adding favorite: ref cd | pageId | uri | name: " + page.getReferenceCode() +"|"+page.getPageId() +"|"+page.getRequestUri() +"|"+page.getPageDisplayName());

		List<PageViewVO>pList = pages.get(page.getReferenceCode());
		if (pList != null && 
				pList.size() < QuickLinksAction.MAX_LIST_SIZE)
			pList.add(page);
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
}
