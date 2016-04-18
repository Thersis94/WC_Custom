package com.depuysynthesinst;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.action.tools.FavoriteVO;
import com.smt.sitebuilder.action.tools.FavoritesAction;
import com.smt.sitebuilder.action.tools.MyFavoritesAction;
import com.smt.sitebuilder.action.tools.PageViewReportingAction;
import com.smt.sitebuilder.action.tools.StatVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;

/****************************************************************************
 * <b>Title</b>: SolrSearchWrapper.java<p/>
 * <b>Description: a decorator around SolrAction, this class added support for 
 * sorting the search results by popularity and 'my favorites', which comes from the DB
 * (not the Solr index).</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Sep 24, 2014
 ****************************************************************************/
public class SolrSearchWrapper extends SimpleActionAdapter {

	protected static final String PAGEVIEWS = "PAGEVIEWS";
	protected static final String FAVORITES = MyFavoritesAction.MY_FAVORITES;
	private static final int RPP = 48; //DSI #results per page, could be moved to attribute2Text if needed to be configurable

	public SolrSearchWrapper() {
	}

	/**
	 * @param arg0
	 */
	public SolrSearchWrapper(ActionInitVO arg0) {
		super(arg0);
	}

	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}

	public void retrieve(SMTServletRequest req) throws ActionException {
		//determine if custom sort is needed
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String sortType = StringUtil.checkVal(req.getParameter("fieldSort"));
		boolean doCustomSort = "popular".equals(sortType) || "favorites".equals(sortType);
		boolean isFeatItem = StringUtil.checkVal(req.getParameter("fmid")).equals(mod.getPageModuleId());
		if (!isFeatItem && req.hasParameter("pmid"))
			isFeatItem = req.getParameter("pmid").equals(mod.getPageModuleId()) && StringUtil.checkVal(mod.getParamName()).length() == 0;

		//log.debug("isFocused=" + isFeatItem);
		//log.debug("isCustomSort=" + doCustomSort);

		//reset sortOrder or Solr will bomb (unknown sortType)
		if (isFeatItem && doCustomSort) {
			req.setParameter("fieldSort", "documentId");
			req.setParameter("rpp", "3000");
			req.setParameter("page", "0");
		} else if ("credits_i".equals(sortType) || "title_lcase".equals(sortType)) { 
			//LMS course credits or Future Leaders Technique Guides 
			//test for specific sorting values, or Solr will puke (@hack attempts)
			req.setParameter("sortField", sortType);
		}

		//call SolrAction 
		actionInit.setActionId((String)mod.getAttribute(SBModuleVO.ATTRIBUTE_1));
		SolrAction sa = new SolrAction(actionInit);
		sa.setAttributes(attributes);
		sa.setDBConnection(dbConn);
		sa.retrieve(req);

		//get the response object back from SolrAction
		mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		SolrResponseVO solrResp = (SolrResponseVO) mod.getActionData();
		if (solrResp == null || solrResp.getTotalResponses() == 0) return;

		if (solrResp.getTotalResponses() == 1 && req.hasParameter("reqParam_1"))
			applyPageData(solrResp.getResultDocuments().get(0), req);

		//if not custom sort, we're done
		if (!isFeatItem || !doCustomSort) return;

		//call the proper sort method
		Integer pageNo = Convert.formatInteger(req.getParameter("page"));
		if ("popular".equals(sortType)) {
			sortByPopular(solrResp, req, pageNo);
		} else if ("favorites".equals(sortType)) {
			sortByFavorite(solrResp, req, pageNo);
		}

		//put the proper 'page' of results back into the SolrResponse to forward on to the View.
		req.setParameter("fieldSort", sortType);
		super.putModuleData(solrResp);
	}


	/**
	 * If we're looking at a single asset (in detail), the URL is a /qs/.  We should
	 * override the page's meta data and title with those of the asset.
	 * @param resp
	 * @param req
	 */
	private void applyPageData(SolrDocument doc, SMTServletRequest req) {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		page.setTitleName(StringUtil.checkVal(doc.getFieldValue("title"), page.getTitleName()));
		//set a canonical that points to the first proclaimed hierarchy level
		page.setCanonicalPageUrl(this.buildDSIUrl(doc));
	}


	/**
	 * updates each SolrDocument to include a favorites indicator (1/0), then invokes a 
	 * Comparator to sort the List.
	 * @param resp
	 * @param req
	 * @throws ActionException
	 */
	private void sortByFavorite(SolrResponseVO resp, SMTServletRequest req, Integer pageNo) 
			throws ActionException {
		Collection<String> favs = loadFavorites(req);

		///iterate the solr results and encapsulate each SolrDocument with the extra fields we need for the Comparator
		List<SolrDocument> docs = new ArrayList<SolrDocument>(Long.valueOf(resp.getTotalResponses()).intValue());
		for (SolrDocument sd : resp.getResultDocuments()) {
			String docId = StringUtil.checkVal(sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID));
			sd.setField(FAVORITES, (favs.contains(docId)) ? 0 : 5); //use zero for 'like', because the compatator will put lowest #s first
			docs.add(sd);
		}

		//sort the Collection
		Collections.sort(docs, new SolrFavoritesComparator());
		resp.setResultDocuments(docs, pageNo, RPP);
	}


	/**
	 * updates each SolrDocument to include a pageview count, then invokes a 
	 * Comparator to sort the List.
	 * @param resp
	 * @param req
	 * @throws ActionException
	 */
	private void sortByPopular(SolrResponseVO resp, SMTServletRequest req, Integer pageNo) 
			throws ActionException {
		Map<String, Integer> favs = loadPageViews(req);
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		boolean isAnatomyRootPg = !"/search".equals(page.getFullPath()) && !"/futureleaders/surgical-technique-guides".equals(page.getFullPath());
		String baseUrl = (isAnatomyRootPg) ? page.getFullPath() + "/" + attributes.get(Constants.QS_PATH) : null;
		log.debug("base=" + baseUrl);

		///iterate the solr results and encapsulate each SolrDocument with the extra fields we need for the Comparator
		List<SolrDocument> docs = new ArrayList<SolrDocument>(Long.valueOf(resp.getTotalResponses()).intValue());
		for (SolrDocument sd : resp.getResultDocuments()) {
			String url = "";
			if (baseUrl == null) {
				url = this.buildDSIUrl(sd);
			} else {
				url = baseUrl + StringUtil.checkVal(sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID));
			}
			sd.setField(PAGEVIEWS, (favs.containsKey(url)) ? favs.get(url) : 0);
			docs.add(sd);
		}

		//sort the Collection
		Collections.sort(docs, new SolrPageviewComparator());
		resp.setResultDocuments(docs, pageNo, RPP);
	}

	/**
	 * loads the user's favorites and stores them in session for next time 
	 * (since the user is interested in this type of sort).
	 */
	@SuppressWarnings("unchecked")
	private Collection<String> loadFavorites(SMTServletRequest req) throws ActionException {
		if (req.getSession().getAttribute(FAVORITES) != null)
			return (List<String>) req.getSession().getAttribute(FAVORITES);

		FavoritesAction fa = new FavoritesAction(actionInit);
		fa.setDBConnection(dbConn);
		fa.setAttributes(attributes);
		fa.retrieve(req);

		//get the loaded data off ModuleVO
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		List<FavoriteVO> favs = (List<FavoriteVO>) mod.getActionData();

		//turn the map into one keyed using IDs, so we don't have to parse it every time.
		//for DSI, any URL containing /qs/ is proceeded by the Solr documentId, which is convenient.  :)
		List<String> data = new ArrayList<String>(favs.size());
		for (FavoriteVO vo : favs)
			if (vo.getRelId() != null) data.add(vo.getRelId());

		//save the favs for next time
		req.getSession().setAttribute(FAVORITES, data);
		return data;
	}


	/**
	 * loads the pageview hits for mediabin assets and returns them for sorting use.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Integer> loadPageViews(SMTServletRequest req) throws ActionException {
		//check for cached data
		if (req.getSession().getAttribute(PAGEVIEWS) != null)
			return (Map<String, Integer>) req.getSession().getAttribute(PAGEVIEWS);

		//call the action and load the stats for this website
		PageViewReportingAction pvra = new PageViewReportingAction(actionInit);
		pvra.setDBConnection(dbConn);
		pvra.setAttributes(attributes);
		pvra.retrieve(req);

		//get the loaded data off ModuleVO
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		Map<String, StatVO> stats = (Map<String, StatVO>) mod.getActionData();

		//turn the map into one keyed using IDs, so we don't have to parse it every time.
		//for DSI, any URL containing /qs/ is proceeded by the Solr documentId, which is convenient.  :)
		Map<String, Integer> data = new HashMap<String, Integer>(stats.size());
		for (StatVO vo : stats.values()) {
			String key = vo.getRequestUri();
			//log.debug("key=" + key);
			if (key != null && key.contains("/" + attributes.get(Constants.QS_PATH))) {
				data.put(key, vo.getHitCnt());
			}
		}

		//save the favs for next time
		req.getSession().setAttribute(PAGEVIEWS, data);
		return data;
	} 


	/**
	 * take the first hierachy definition and turn it into a dsi-business-rules-applied URL string
	 * This method is also used by the DePuySiteMapServlet
	 * @param sd
	 * @return
	 */
	public String buildDSIUrl(SolrDocument sd) {
		String hierarchy = "";
		try {
			hierarchy= StringUtil.checkVal(sd.getFieldValues(SearchDocumentHandler.HIERARCHY).iterator().next());
		} catch (Exception e) {};

		return buildDSIUrl(hierarchy, (String)sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID), (String)attributes.get(Constants.QS_PATH));
	}

	public static String buildDSIUrl(String hierarchy, String documentId, String qsPath) {
		//log.debug(hierarchy);
		if (hierarchy == null || hierarchy.length() == 0) return null;

		String rootLvl = (hierarchy.indexOf("~") > 0) ? hierarchy.substring(0, hierarchy.indexOf("~")) : hierarchy;
		rootLvl = StringUtil.checkVal(rootLvl).toLowerCase();
		if ("vet".equals(rootLvl)) {
			//vet hierarchies start at level 2, indent and find the rootLvl (at the second level)
			int tildeIndx = rootLvl.length() +1;
			int nextDelim = hierarchy.indexOf("~", tildeIndx);
			if (nextDelim == -1) nextDelim = hierarchy.length(); //if there isn't more than 1 level use the length as the endpoint.
			if (nextDelim > tildeIndx) rootLvl = hierarchy.substring(tildeIndx, nextDelim); //if we have a next level, capture it as the new root.
			rootLvl = StringUtil.checkVal(rootLvl).toLowerCase();

			rootLvl = "veterinary/" + rootLvl;
			//log.debug(rootLvl);
		} else if ("future leaders".equals(rootLvl)) {
			//future leaders start at level 2, indent and find the rootLvl (at the second level)
			int tildeIndx = rootLvl.length() +1;
			int nextDelim = hierarchy.indexOf("~", tildeIndx);
			if (nextDelim == -1) nextDelim = hierarchy.length(); //if there isn't more than 1 level use the length as the endpoint.
			if (nextDelim > tildeIndx) rootLvl = hierarchy.substring(tildeIndx, nextDelim); //if we have a next level, capture it as the new root.
			rootLvl = StringUtil.checkVal(rootLvl).toLowerCase();

			//prume verbose wording
			if ("general principles & fundamentals".equals(rootLvl)) {
				rootLvl = "futureleaders/kc/general";
			} else if ("musculoskeletal oncology".equals(rootLvl)) {
				rootLvl = "futureleaders/kc/musculoskeletal";
			} else if ("graduation resources".equals(rootLvl)) {
				rootLvl = "futureleaders/graduation-resources";
			} else {
				rootLvl =StringUtil.replace(rootLvl, " medicine", "");
				rootLvl =StringUtil.replace(rootLvl, " surgery", "");
				rootLvl = "futureleaders/kc/" + rootLvl;
			}
			
		}

		//remove ampersands and replace spaces
		rootLvl = StringUtil.replace(rootLvl, "& ", "");
		rootLvl = StringUtil.replace(rootLvl, " ", "-");

		if ("nurse-education".equals(rootLvl)) {
			rootLvl = "nurse-education/resource-library";
		}

		//log.debug(rootLvl);
		hierarchy = rootLvl;

		//assemble & return the URL
		if (hierarchy == null || hierarchy.length() == 0) return null;
		return "/" + hierarchy + "/" + qsPath + documentId;
	}

}
