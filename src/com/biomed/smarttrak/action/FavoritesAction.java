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
		this.putModuleData(favs, favs.size(), false, mod.getErrorMessage(), mod.getErrorCondition());
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void build(ActionRequest req) throws ActionException {
		SMTSession sess = (SMTSession)req.getSession();
		List<PageViewVO> favs = (List<PageViewVO>)sess.getAttribute(MyFavoritesAction.MY_FAVORITES);
		if (favs == null) return;

		PageViewVO fav = new PageViewVO();

		fav.setPageId(req.getParameter("id"));
		fav.setPageDisplayName(req.getParameter("name"));
		fav.setRequestUri(req.getRequestURI());

		// add to top of list
		favs.add(0,fav);

		// if list size > max, remove 1
		if (favs.size() > QuickLinksAction.MAX_LIST_SIZE) 
			favs.remove(favs.size() - 1);

		// set update collection on session
		sess.setAttribute(MyFavoritesAction.MY_FAVORITES, favs);
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

		SolrDocument sDoc;
		for (FavoriteVO fav : favs) {
			if (fav.getAsset() == null) continue; 
			sDoc = (SolrDocument)fav.getAsset();
			parseFavoriteAsset(pageMap,sDoc);
		}
		return pageMap;
	}
	
	/**
	 * Parses a Favorite asset to determine if it is a section page favorite. If so, the asset is
	 * parsed into a PageViewVO and is added to the appropriate collection of pages on the page map.
	 * @param pages
	 * @param asset
	 */
	protected void parseFavoriteAsset(Map<String,List<PageViewVO>> pages, SolrDocument asset) {
		String docUrl = asset.getFieldValue(SearchDocumentHandler.DOCUMENT_URL).toString();
		String sectionType = checkAssetSectionType(docUrl);
		if (sectionType == null) return;

		PageViewVO page = new PageViewVO();
		page.setReferenceCode(sectionType);
		page.setPageId(asset.getFieldValue(SearchDocumentHandler.DOCUMENT_ID).toString());
		page.setRequestUri(docUrl);
		page.setPageDisplayName(asset.getFieldValue(SearchDocumentHandler.TITLE).toString());

		List<PageViewVO>pList = pages.get(page.getReferenceCode());
		if (pList != null && 
				pList.size() < QuickLinksAction.MAX_LIST_SIZE)
			pList.add(page);
	}
	
	/**
	 * Tests the pageUrl to see if it is a reference to a section (Market, Company, etc.).
	 * @param pageUrl
	 * @return
	 */
	protected String checkAssetSectionType(String pageUrl) {
		String type = null;
		for (Section section : Section.values()) {
			if (pageUrl.indexOf(section.getURLToken() + QuickLinksAction.URL_STUB) > -1) {
				type = section.name();
				break;
			}
		}
		return type;
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
