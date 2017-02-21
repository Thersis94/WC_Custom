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

		Map<String, List<PageViewVO>> favs = processUserFavorites(mod);

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
		String collKey = req.getParameter(QuickLinksAction.PARAM_KEY_SECTION);
		try {
			checkCollectionKey(collKey);
		} catch (ActionException ae) {
			log.error("Error: " + ae.getMessage());
			return;
		}

		String pkId = parsePrimaryId(req.getParameter(QuickLinksAction.PARAM_KEY_URI_TXT));
		if (pkId == null) return;

		// set additional req params needed downstream for inserts.
		req.setParameter(QuickLinksAction.PARAM_KEY_TYPE_CD, collKey);
		req.setParameter(QuickLinksAction.PARAM_KEY_REL_ID, pkId);

		ModuleVO mod = new ModuleVO();
		String data = "failure";
		// add favorite to user's profile favorites

		try {
			updateProfileFavorites(req);
			data = "success";
			mod.setErrorCondition(false);
		} catch (Exception e) {
			mod.setErrorCondition(true);
			mod.setErrorMessage(e.getMessage());
		}
		
		// return a module vo to inform the AJAX caller of the result of this operation.
		putModuleData(data, 0, false, mod.getErrorMessage(), mod.getErrorCondition());

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
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	protected void updateProfileFavorites(ActionRequest req) throws ActionException {
		log.debug("updateProfileFavorite...");
		ActionInterface ai = new MyFavoritesAction(getActionInit());
		ai.setAttributes(getAttributes());
		ai.setDBConnection(dbConn);
		ai.build(req);
		
		/* NOTE: MyFavoritesAction 'build' method removes a user's favorites from their
		 * session.  We have to call retrieve now to retrieve favorites and place
		 * them on the session again.	 */
		this.retrieve(req);
		ModuleVO mod = (ModuleVO)getAttribute(Constants.MODULE_DATA);
		if (mod != null) {
			Map<String,List<PageViewVO>> favs = (Map<String,List<PageViewVO>>)mod.getActionData();
			req.getSession().setAttribute(MyFavoritesAction.MY_FAVORITES, favs);
		}
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
	protected Map<String, List<PageViewVO>> processUserFavorites(ModuleVO mod) 
			throws ActionException {
		log.debug("processUserFavorites...");
		if (mod.getErrorCondition()) return new HashMap<>();

		List<FavoriteVO> favs = (List<FavoriteVO>)mod.getActionData();
		Map<String, List<PageViewVO>> pageMap = initializePageMap();

		for (FavoriteVO fav : favs) {
			log.debug("found fav, typeCd | relId | uriTxt: " + fav.getTypeCd() + "|" + fav.getRelId() + "|" + fav.getUriTxt());
			if (fav.getAsset() == null) continue; 
			processFavorite(pageMap,fav);
		}
		return pageMap;
	}
	
	/**
	 * Parses a Favorite asset to determine if it is a section page favorite. If so, the asset is
	 * parsed into a PageViewVO and is added to the appropriate collection of pages on the page map.
	 * @param pages
	 * @param fav
	 */
	protected void processFavorite(Map<String,List<PageViewVO>> pages, FavoriteVO fav) {
		try {
			checkCollectionKey(fav.getTypeCd());
		} catch (Exception e) {
			// this fav is not a 'Section' type so return.
			return;
		}
		
		// convert favorite into a PageViewVO
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
