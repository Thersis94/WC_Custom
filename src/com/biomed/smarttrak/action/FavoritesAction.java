package com.biomed.smarttrak.action;

//Java 8
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
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
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WebCrescendo libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.tools.FavoriteVO;
import com.smt.sitebuilder.action.tools.MyFavoritesAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.util.PageViewUserVO;

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
	@SuppressWarnings("unchecked")
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("FavoritesAction retrieve...");
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		if (role == null) throw new ActionException("Not logged in.");

		ModuleVO mod;
		Map<String, List<PageViewUserVO>> favs;

		if (req.hasParameter(QuickLinksAction.PARAM_KEY_REFRESH_FAVORITES)) {
			// retrieve favs from session.
			mod = new ModuleVO();
			favs = (Map<String, List<PageViewUserVO>>)req.getSession().getAttribute(MyFavoritesAction.MY_FAVORITES);
			if (favs == null) favs = new HashMap<>();

		} else {
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
			favs = processUserFavorites(mod);
		}

		log.debug("FavoritesAction retrieve, size|error condition|message: " + favs.size() + "|"  + mod.getErrorCondition() + "|" + mod.getErrorMessage());
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

		PageViewUserVO fav = new PageViewUserVO();
		fav.setReferenceCode(collKey);
		// Old favoritable items exist in solr with section cd_id, not just id
		if(pkId.length() < AdminControllerAction.DOC_ID_MIN_LEN){
			fav.setPageId(collKey + "_" + pkId);
		}else {
			fav.setPageId(pkId);
		}
		fav.setRequestUri(req.getParameter(QuickLinksAction.PARAM_KEY_URI_TXT));
		fav.setPageDisplayName(req.getParameter(QuickLinksAction.PARAM_KEY_NAME));

		ModuleVO mod = new ModuleVO();
		String data = "failure";
		// add favorite to user's profile favorites
		try {
			updateProfileFavorites(req,fav);
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
	 * @param fav
	 * @throws ActionException
	 */
	protected void updateProfileFavorites(ActionRequest req, PageViewUserVO fav) throws ActionException {
		log.debug("updateProfileFavorites...");

		MyFavoritesAction mfa = new MyFavoritesAction(getActionInit());
		mfa.setAttributes(getAttributes());
		mfa.setDBConnection(dbConn);

		boolean isDelete = Convert.formatBoolean(req.getParameter("isDelete"));
		if (isDelete) {
			mfa.deleteFavorite(req);
		} else {
			// set additional req params needed for inserts.
			req.setParameter(QuickLinksAction.PARAM_KEY_TYPE_CD, fav.getReferenceCode());
			req.setParameter(QuickLinksAction.PARAM_KEY_REL_ID, fav.getPageId());
			mfa.insertFavorite(req);
		}

		updateSessionFavorites(req.getSession(),fav,isDelete);

	}

	/**
	 * Updates the appropriate Favorite collection on the session.
	 * @param session
	 * @param fav
	 * @param isDelete
	 */
	@SuppressWarnings("unchecked")
	protected void updateSessionFavorites(SMTSession session, PageViewUserVO fav, boolean isDelete) {
		// get the Favs map off of the session.
		Map<String,List<PageViewUserVO>> favMap = (Map<String,List<PageViewUserVO>>)session.getAttribute(MyFavoritesAction.MY_FAVORITES);
		List<PageViewUserVO> favs = favMap.get(fav.getReferenceCode());
		if (favs == null) favs = new ArrayList<>();
		if (isDelete) {
			// remove fav
			removeFromSession(favs, fav.getPageId());
		} else {
			// add fav
			favs.add(fav);
		}
		// replace the favs map on the session.
		session.setAttribute(MyFavoritesAction.MY_FAVORITES, favMap);
	}
	
	/**
	 * Iterates a List of PageViewUserVO and removes the page that matches the pageId 
	 * passed in.
	 * @param pages
	 * @param favPageId
	 */
	protected void removeFromSession(List<PageViewUserVO> pages, String favPageId) {
		if (pages.isEmpty()) return;
		// loop and remove
		ListIterator<PageViewUserVO> li = pages.listIterator();
		while (li.hasNext()) {
			PageViewUserVO tmp = li.next();
			if (favPageId.equalsIgnoreCase(tmp.getPageId())) {
				li.remove();
				break;
			}
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
	protected Map<String, List<PageViewUserVO>> processUserFavorites(ModuleVO mod) 
			throws ActionException {
		log.debug("processUserFavorites...");
		Map<String, List<PageViewUserVO>> pageMap = initializePageMap();
		if (mod.getErrorCondition()) return pageMap;

		List<FavoriteVO> favs = (List<FavoriteVO>)mod.getActionData();

		for (FavoriteVO fav : favs) {
			log.debug("found fav, typeCd | relId | uriTxt: " + fav.getTypeCd() + "|" + fav.getRelId() + "|" + fav.getUriTxt());
			if (fav.getAsset() == null) continue; 
			processFavorite(pageMap,fav);
		}
		return pageMap;
	}
	
	/**
	 * Parses a Favorite asset to determine if it is a section page favorite. If so, the asset is
	 * parsed into a PageViewUserVO and is added to the appropriate collection of pages on the page map.
	 * @param pages
	 * @param fav
	 */
	protected void processFavorite(Map<String,List<PageViewUserVO>> pages, FavoriteVO fav) {
		try {
			checkCollectionKey(fav.getTypeCd());
		} catch (Exception e) {
			// this fav is not a 'Section' type so return.
			return;
		}
		
		// convert favorite into a PageViewUserVO
		PageViewUserVO page = new PageViewUserVO();
		page.setReferenceCode(fav.getTypeCd());
		page.setPageId(fav.getRelId());
		page.setRequestUri(fav.getUriTxt());

		if (fav.getAsset() != null) {
			SolrDocument sDoc = (SolrDocument)fav.getAsset();
			page.setPageDisplayName(sDoc.getFieldValue(SearchDocumentHandler.TITLE).toString());
		}

		log.debug("adding favorite: ref cd | pageId | uri | name: " + page.getReferenceCode() +"|"+page.getPageId() +"|"+page.getRequestUri() +"|"+page.getPageDisplayName());

		List<PageViewUserVO>pList = pages.get(page.getReferenceCode());
		if (pList != null)
			pList.add(page);
	}

	/**
	 * Initialize a Map of List of PageViewUserVO based on the key types enum.
	 * @return
	 */
	protected Map<String,List<PageViewUserVO>> initializePageMap() {
		Map<String,List<PageViewUserVO>> pm = new HashMap<>();
		for (Section sect : Section.values()) {
			pm.put(sect.name(), new ArrayList<>());
		}
		return pm;
	}
}
