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
		boolean doCustomSort = "popular".equals(sortType) || "favorites".equals(sortType) ;
		boolean isThemeLocn = StringUtil.checkVal(mod.getParamName()).length() > 0;
		Integer pageNo = Convert.formatInteger(req.getParameter("page"));
		
		//reset sortOrder or Solr will bomb (unknown sortType)
		if (!isThemeLocn && doCustomSort) {
			req.setParameter("fieldSort", "documentId");
			req.setParameter("rpp", "3000");
			req.setParameter("page", "0");
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
		
		if (solrResp.getTotalResponses() == 1)
			applyPageData(solrResp.getResultDocuments().get(0), req);
		
		//if not custom sort, we're done
		if (isThemeLocn || !doCustomSort) return;
				
		//call the proper sort method
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
		List<SolrDocument> docs = new ArrayList<SolrDocument>(Integer.valueOf(""+resp.getTotalResponses()));
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
		
		///iterate the solr results and encapsulate each SolrDocument with the extra fields we need for the Comparator
		List<SolrDocument> docs = new ArrayList<SolrDocument>(Integer.valueOf(""+resp.getTotalResponses()));
		for (SolrDocument sd : resp.getResultDocuments()) {
			String docId = StringUtil.checkVal(sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID));
			sd.setField(PAGEVIEWS, (favs.containsKey(docId)) ? favs.get(docId) : 0);
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
			if (key != null && key.contains("/qs/")) {
				key = key.substring(key.indexOf("/qs/")+4);
				if (key != null && key.length() > 0)
					data.put(key, vo.getHitCnt());
			}
		}
		
		//save the favs for next time
		req.getSession().setAttribute(PAGEVIEWS, data);
		return data;
	} 

}
