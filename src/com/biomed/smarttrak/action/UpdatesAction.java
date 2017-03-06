package com.biomed.smarttrak.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.biomed.smarttrak.util.UpdateIndexer;
import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrFieldVO.FieldType;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
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

	public static final int RPP = 15;
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
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (!req.hasParameter("loadSolrUpdates")) return;

		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		List<String> docIds = null;
		if("favorites".equals(req.getParameter("filter"))) {
			docIds = loadFavoriteDocs(req);
		}
		//transform some incoming reqParams to where Solr expects to see them
		transposeRequest(req, docIds);

		//Get SolrSearch ActionVO.
		SolrAction sa = new SolrAction(actionInit);
		sa.setDBConnection(dbConn);
		sa.setAttributes(attributes);
		sa.retrieve(req);
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

		/*
		 * Get the PageNo off the request.  Use this to filter the sql query down
		 * to a manageable size.  Replace the Request with 0 as the Start value
		 * so that Solr doesn't offset anything.
		 */
		int page = Convert.formatInteger(req.getParameter("page"));
		int offset = RPP * page;
		req.setParameter("page", "0");

		List<String> docIds = new ArrayList<>();
		try(PreparedStatement ps = dbConn.prepareStatement(getFavoriteUpdatesSql(offset))) {
			int i = 1;
			ps.setString(i++, AdminControllerAction.Section.MARKET.toString());
			ps.setString(i++, vo.getProfileId());
			ps.setString(i++, AdminControllerAction.Section.PRODUCT.toString());
			ps.setString(i++, vo.getProfileId());
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
	protected String getFavoriteUpdatesSql(int offset) {
		String custom = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(500);
		sql.append("select distinct *, row_number() OVER (ORDER BY publish_dt desc) as rnum from ( ");
		sql.append("select b.update_id, b.publish_dt from profile_favorite a ");
		sql.append("inner join ").append(custom).append("biomedgps_update b ");
		sql.append("on a.rel_id = b.market_id and a.type_cd = ? and a.profile_id = ? ");
		sql.append("union ");
		sql.append("select b.update_id, b.publish_dt from profile_favorite a ");
		sql.append("inner join ").append(custom).append("biomedgps_update b ");
		sql.append("on a.rel_id = b.product_id and a.type_cd = ? and a.profile_id = ? ");
		sql.append("union ");
		sql.append("select b.update_id, b.publish_dt from profile_favorite a ");
		sql.append("inner join ").append(custom).append("biomedgps_update b ");
		sql.append("on a.rel_id = b.company_id and a.type_cd = ? and a.profile_id = ? ");
		sql.append(") as update_id order by publish_dt desc ");
		sql.append("limit ").append(RPP).append(" offset ").append(offset);
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
	protected void transposeRequest(ActionRequest req, List<String> docIds) throws ActionException {
		//get the filter queries already on the request.  Add ours to the stack, and put the String[] back on the request for Solr
		String[] fqs = req.getParameterValues("fq");
		if (fqs == null) fqs = new String[0];
		List<String> data = new ArrayList<>(Arrays.asList(fqs));

		//Add Sections Check.  Append a filter query for each section requested
		if (req.hasParameter("hierarchyId")) {
			for (String s : req.getParameterValues("hierarchyId"))
				data.add(SearchDocumentHandler.HIERARCHY + ":" + s);
		}

		//Add Favorites Filter if applicable.
		if(docIds != null && !docIds.isEmpty()) {
			for(String s : docIds) {
				data.add(SearchDocumentHandler.DOCUMENT_ID + ":" + s);
			}
		}

		//Get a Date Range String.
		String dates = SolrActionUtil.makeRangeQuery(FieldType.DATE, req.getParameter("startDt"), req.getParameter("endDt"));
		if (!StringUtil.isEmpty(dates))
			data.add(SearchDocumentHandler.UPDATE_DATE + ":" + dates);

		//Add a ModuleType filter if typeId was passed
		if (req.hasParameter("typeId"))
			data.add(SearchDocumentHandler.MODULE_TYPE + ":" + req.getParameter("typeId"));

		//put the new list of filter queries back on the request
		req.setParameter("fq", data.toArray(new String[data.size()]), true);
	}
}