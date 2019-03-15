package com.biomed.smarttrak.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.common.SolrDocument;

import com.biomed.smarttrak.action.AdminControllerAction.Section;
import com.biomed.smarttrak.security.SmarttrakRoleVO;
import com.biomed.smarttrak.util.BiomedLinkCheckerUtil;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.util.UpdateIndexer;
import com.biomed.smarttrak.vo.SectionVO;
import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.http.CookieUtil;
import com.siliconmtn.data.Node;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrFieldVO.FieldType;
import com.smt.sitebuilder.action.tools.MyFavoritesAction;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.PageViewVO;
import com.smt.sitebuilder.util.solr.SolrActionUtil;

/****************************************************************************
 * <b>Title</b>: UpdatesAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Public Updates Action that talks to Solr.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Feb 16, 2017
 ****************************************************************************/
public class UpdatesAction extends SBActionAdapter {

	public UpdatesAction() { 
		super();
	}

	/**
	 * @param actionInit
	 */
	public UpdatesAction(ActionInitVO actionInit) { 
		super(actionInit);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		req.setParameter("fmid", mod.getPageModuleId());
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		List<String> docIds = null;
		if("favorites".equals(req.getParameter("filter"))) {
			docIds = loadFavoriteDocs(req);
		}
		//transform some incoming reqParams to where Solr expects to see them
		transposeRequest(req, docIds);

		//Get SolrSearch ActionVO.
		req.setAttribute(SmarttrakSolrAction.SECTION, Section.UPDATES_EDITION);
		SolrAction sa = new SmarttrakSolrAction(actionInit);
		sa.setDBConnection(dbConn);
		sa.setAttributes(attributes);
		sa.retrieve(req);

		//Sort Facet Hierarchy.
		mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		SolrResponseVO resp = (SolrResponseVO) mod.getActionData();

		if (resp != null && !resp.getResultDocuments().isEmpty()) {
			List<Node> sections = loadSections(req);
			sortFacets(sections, resp);
			//adjust links only manage tool
			adjustContentLinks(resp.getResultDocuments(), mod, req);

			Map<String,List<PageViewVO>> favs = (Map<String,List<PageViewVO>>)req.getSession().getAttribute(MyFavoritesAction.MY_FAVORITES);

			/*
			 * Attempt to Flag Favorite Updates using Session Favorites if available.
			 * Else, load Documents from DB and match that way.
			 */
			if(favs != null) {
				flagFavorites(resp.getResultDocuments(), favs);
			} else {
				docIds = loadFavoriteDocs(req);
				flagFavoritesByDocId(resp.getResultDocuments(), docIds);
			}
		}

	}

	/**
	 * Flag updates that match a users Favorite Preferences via database call.
	 * @param resultDocuments
	 * @param docIds
	 */
	private void flagFavoritesByDocId(List<SolrDocument> resp, List<String> docIds) {
		boolean isFav;
		for (SolrDocument solrDocument : resp) {
			isFav = docIds.contains((String)solrDocument.getFieldValue(SearchDocumentHandler.DOCUMENT_ID));
			solrDocument.setField("isFavorite", isFav);
		}
		log.debug("Favorites Flagged");
	}

	/**
	 * Flag updates that match a users Favorites Preferences.
	 * @param resultDocuments
	 * @param mod
	 * @param req
	 */
	private void flagFavorites(List<SolrDocument> resp, Map<String, List<PageViewVO>> favs) {
		boolean isFav;
		for (SolrDocument solrDocument : resp) {
			isFav = false;
			String docUrl = (String)solrDocument.getFieldValue(SearchDocumentHandler.DOCUMENT_URL);
			//If solrDocument has a documentUrl, can be favorited.
			if(!StringUtil.isEmpty(docUrl)) {
				log.info(docUrl);
				List<PageViewVO> favPages = favs.get((String)solrDocument.getFieldValue(SearchDocumentHandler.CONTENT_TYPE));

				log.debug(favPages != null && !favPages.isEmpty());
				isFav = checkFavPageUrl(solrDocument, favPages);
			}
			solrDocument.setField("isFavorite", isFav);
		}
		log.debug("Favorites Flagged");
	}

	/**
	 * Attempt to match the given solrDocument with a favorite Page.
	 * @param solrDocument
	 * @param favPages
	 * @return
	 */
	private boolean checkFavPageUrl(SolrDocument solrDocument, List<PageViewVO> favPages) {
		String docUrl = (String)solrDocument.getFieldValue(SearchDocumentHandler.DOCUMENT_URL);
		boolean isFav = false;
		if(favPages != null && !favPages.isEmpty()) {
			for(PageViewVO p : favPages) {
				if(docUrl.equals(p.getRequestUri())) {
					isFav = true;
					break;
				}
			}
		}
		return isFav;
	}

	/**
	 * Modifies public links to their corresponding manage tool link
	 * @param resp
	 * @param req
	 */
	protected void adjustContentLinks(List<SolrDocument> resp,  ModuleVO mod, ActionRequest req) {
		//The widget on the public site does not have a solr widget assigned to attribute_2 slot, return if not found
		if(mod.getAttribute(ModuleVO.ATTRIBUTE_2) == null) return;
		
		BiomedLinkCheckerUtil linkUtil = new BiomedLinkCheckerUtil(dbConn, (SiteVO) req.getAttribute(Constants.SITE_DATA));
		
		//adjust the appropriate content links
		for (SolrDocument solrDocument : resp) {
			String content = (String)solrDocument.getFieldValue(SearchDocumentHandler.SUMMARY);
			solrDocument.setField(SearchDocumentHandler.SUMMARY, linkUtil.modifySiteLinks(content));
		}
	}
	
	/**
	 * Helper method manages sorting Facets according to Section Hierarchy Order Values.
	 * Note: this method is shared with InsightAction
	 * @param resp
	 */
	protected void sortFacets(List<Node> sections, SolrResponseVO resp) {
		List<String> names = new ArrayList<>();
		List<FacetField> facets = resp.getFacets();
		int idx = 0;
		FacetField fOld = null;
		//find the correct facet, by name
		for (int x=0; x < facets.size(); x++) {
			FacetField ff = facets.get(x);
			if (SearchDocumentHandler.HIERARCHY.equals(ff.getName())) {
				fOld = ff;
				idx = x;
				break;
			}
		}
		
		//fail fast if we can't perform our duty here
		if (fOld == null) return;
		
		//TODO address this before Solr 6.6 releases! - Zoho V3-114
		//FacetField fNew = new FacetField(fOld.getName(), fOld.getGap(), fOld.getEnd());
		FacetField fNew = new FacetField(fOld.getName());

		/*
		 * Iterate over Section Nodes first as they have the order.  Iterate
		 * over FacetFields second as that is the returned data sets.  Compare
		 * the names and if it's a match, add it to the new Facet.
		 */
		for (Node n : sections) {
			for (Count f : fOld.getValues()) {
				/*Split to ensure matches are made against the entire facet name*/
				String[] parts = f.getName().split("~");
				String endName = parts[parts.length -1];
				if (endName.equals(n.getNodeName()) && !names.contains(f.getName())) {
					names.add(f.getName());
					fNew.add(f.getName(), f.getCount());
					break;
				}
			}
		}

		//Replace Facets List with New one and set on the VO.
		facets.set(idx, fNew); //put the rebuild facet back in place of the original
		resp.setFacets(facets);
	}

	/**
	 * Helper method that returns the List of Sections for ordering facets on
	 * the front end.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected List<Node> loadSections(ActionRequest req) {
		SmarttrakRoleVO role = (SmarttrakRoleVO) req.getSession().getAttribute(Constants.ROLE_DATA);
		ModuleVO mod = super.readFromCache(actionInit.getActionId());
		if (mod == null) {
			com.biomed.smarttrak.admin.UpdatesAction ua = new com.biomed.smarttrak.admin.UpdatesAction(actionInit);
			ua.setDBConnection(dbConn);
			ua.setAttributes(attributes);
			SmarttrakTree tree = ua.loadSections();
			List<Node> nodes = tree.getPreorderList();
			mod = new ModuleVO();
			mod.setActionData(nodes);
			mod.setPageModuleId(actionInit.getActionId());
			super.writeToCache(mod);
		}

		//now filter the global hierarchy down to only the areas the user can view:
		List<Node> masterList = (List<Node>)mod.getActionData();
		List<Node> data = new ArrayList<>(masterList.size());
		List<String> roles = Arrays.asList(role.getAuthorizedSections(Section.UPDATES_EDITION));
		for (Node n : masterList) {
			SectionVO vo = (SectionVO) n.getUserObject();
			if (roles.contains(vo.getSolrTokenTxt()))
				data.add(n);
		}

		return data;
	}

	/**
	 * Helper method loads all the Document Ids for Updates that the user has
	 * favorited.
	 * @param req
	 * @return
	 */
	protected List<String> loadFavoriteDocs(ActionRequest req) {
		SMTSession ses = req.getSession();
		UserVO vo = (UserVO) ses.getAttribute(Constants.USER_DATA);

		List<String> docIds = new ArrayList<>();
		try(PreparedStatement ps = dbConn.prepareStatement(getFavoriteUpdatesSql())) {
			int i = 1;
			ps.setString(i++, AdminControllerAction.Section.MARKET.toString() + "_");
			ps.setString(i++, AdminControllerAction.Section.MARKET.toString());
			ps.setString(i++, vo.getProfileId());
			ps.setString(i++, AdminControllerAction.Section.PRODUCT.toString() + "_");
			ps.setString(i++, AdminControllerAction.Section.PRODUCT.toString());
			ps.setString(i++, vo.getProfileId());
			ps.setString(i++, AdminControllerAction.Section.COMPANY.toString() + "_");
			ps.setString(i++, AdminControllerAction.Section.COMPANY.toString());
			ps.setString(i++, vo.getProfileId());
			
			ResultSet rs = ps.executeQuery();

			//Convert Update Id to DocumentId
			while(rs.next()) {
				StringBuilder docId = new StringBuilder(rs.getString("update_id"));
				if (docId.length() < AdminControllerAction.DOC_ID_MIN_LEN) {

					//Insert separator and then insert Index Type
					docId.insert(0, "_").insert(0, UpdateIndexer.INDEX_TYPE);
				}
				docIds.add(docId.toString());
			}
		} catch (SQLException e) {
			log.error(e);
		}

		return docIds;
	}

	/**
	 * Build sql query for Favorited Items in Updates.
	 * @return
	 */
	protected String getFavoriteUpdatesSql() {
		String custom = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(500);
		sql.append("select distinct *, row_number() OVER (ORDER BY publish_dt desc) as rnum from ( ");
		sql.append("select b.update_id, b.publish_dt from profile_favorite a ");
		sql.append("inner join ").append(custom).append("biomedgps_update b ");
		sql.append("on replace(a.rel_id, ?, '') = b.market_id and a.type_cd = ? and a.profile_id = ? ");
		sql.append("union ");
		sql.append("select b.update_id, b.publish_dt from profile_favorite a ");
		sql.append("inner join ").append(custom).append("biomedgps_update b ");
		sql.append("on replace(a.rel_id, ?, '') = b.product_id and a.type_cd = ? and a.profile_id = ? ");
		sql.append("union ");
		sql.append("select b.update_id, b.publish_dt from profile_favorite a ");
		sql.append("inner join ").append(custom).append("biomedgps_update b ");
		sql.append("on replace(a.rel_id, ?, '') = b.company_id and a.type_cd = ? and a.profile_id = ? ");
		sql.append(") as update_id order by publish_dt desc ");
		return sql.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}


	/**
	 * transpose incoming request parameters into values Solr understands, so they get executed for us.
	 * @param req
	 * @param docIds 
	 * @throws ActionException
	 */
	protected void transposeRequest(ActionRequest req, List<String> docIds)  {
		//Enusre action request does NOT encode params. Solr needs exact phrase.
		req.setValidateInput(Boolean.FALSE);

		//get the filter queries already on the request.  Add ours to the stack, and put the String[] back on the request for Solr
		String[] fqs = req.getParameterValues("fq");
		if (fqs == null) fqs = new String[0];
		List<String> data = new ArrayList<>(Arrays.asList(fqs));

		//get the filter terms already on the request.  Add ours to the stack and put the String [] back on the request for Solr.
		String [] fts = req.getParameterValues("ft");
		if(fts == null) fts = new String [0];
		List<String> terms = new ArrayList<>(Arrays.asList(fts));

		//Add Sections Check.  Append a filter query for each section requested
		transposeArray(data, SearchDocumentHandler.HIERARCHY, req.getParameterValues("hierarchyId"));

		//Add Favorites Filter if applicable.
		if(docIds != null && !docIds.isEmpty()) {
			for(String s : docIds) {
				terms.add(SearchDocumentHandler.DOCUMENT_ID + ":" + s);
			}
		}

		String userTimeZone = CookieUtil.getValue("userTimeZone", req.getCookies());
		userTimeZone = StringUtil.checkVal(userTimeZone, TimeZone.getDefault().toString());
		//Build the proper end date based on the users timezone.
		DateFormat formatter= new SimpleDateFormat(Convert.DATE_DASH_PATTERN);
		formatter.setTimeZone(TimeZone.getTimeZone(userTimeZone));
		Calendar c = Calendar.getInstance();
		String d = formatter.format(c.getTime());

		//If we have a specified date, ensure that we've offset time to midnight.
		c.setTime(Convert.formatDate(formatter.format(c.getTime())));
		c.set(Calendar.HOUR, 23);
		c.set(Calendar.MINUTE, 59);
		c.set(Calendar.SECOND, 59);
		d = Convert.formatDate(c.getTime(), Convert.DATE_TIME_DASH_PATTERN);
		req.setParameter("endDt", d);

		System.out.println(req.getParameter("endDt"));
		//Get a Date Range String.
		String dates = SolrActionUtil.makeRangeQuery(FieldType.DATE, req.getParameter("startDt"), d);
		if (!StringUtil.isEmpty(dates))
			data.add(SearchDocumentHandler.PUBLISH_DATE + ":" + dates);

		//Add a ModuleType filter if typeId was passed
		transposeArray(data, SearchDocumentHandler.MODULE_TYPE, req.getParameterValues("typeId"));

		//Custom Filtering for when looking at an Email View.
		transposeEmailFilter(req);

		//put the new list of filter queries back on the request
		req.setParameter("fq", data.toArray(new String[data.size()]), true);
		req.setParameter("ft", terms.toArray(new String[terms.size()]), true);
		req.setParameter("fieldOverride", SearchDocumentHandler.PUBLISH_DATE);
	}
	
	
	/**
	 * Ensure that there is data that can be worked with and add those items to the supplied list
	 * @param data
	 * @param field
	 * @param values
	 */
	private void transposeArray(List<String> data, String field, String[] values) {
		if (values == null || values.length ==0) return;
		
		for (String s : values) {
			data.add(field + ":" + s);
		}
	}

	/**
	 * Handles processing the custom filter for email views for Solr
	 * @param req
	 */
	protected void transposeEmailFilter(ActionRequest req){
		if(!req.hasParameter("isEmail")) return;

		//Set fmid from ModuleVO
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		req.setParameter("fmid", mod.getPageModuleId());

		//Set Custom Sort Field and Direction.
		req.setParameter("sortField", "moduleType asc, publishDate desc, order_i ");
		req.setParameter("sortDirection", "asc");
	}
}