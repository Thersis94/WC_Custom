package com.mts.publication.action;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

// MTS Libs
import com.mts.publication.data.AssetVO;
import com.mts.publication.data.MTSDocumentVO;
import com.mts.publication.data.PublicationTeaserVO;
import com.mts.publication.data.PublicationVO;
import com.mts.publication.data.RelatedArticleVO;
import com.mts.subscriber.data.MTSUserVO;
import com.mts.util.AppUtil;
// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.*;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;

//WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.content.DocumentAction;
import com.smt.sitebuilder.action.metadata.WidgetMetadataVO;
import com.smt.sitebuilder.approval.ApprovalController;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: IssueArticleAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Widget to manage MTS articles for a given issue
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 8, 2019
 * @updates:
 ****************************************************************************/

public class IssueArticleAction extends SBActionAdapter {

	/**
	 * Ajax Controller key for this action
	 */
	public static final String AJAX_KEY = "articles";

	private static final String REQ_DOCUMENT_ID = "documentId";

	/**
	 * 
	 */
	public IssueArticleAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public IssueArticleAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * 
	 * @param db
	 * @param attributes
	 */
	public IssueArticleAction(SMTDBConnection db, Map<String, Object> attributes) {
		super();
		this.setDBConnection(db);
		this.setAttributes(attributes);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		req.setAttribute("mtsPagePreview", ApprovalController.generatePreviewApiKey(attributes));

		try {
			if (req.hasParameter("related")) {
				setModuleData(getRelatedArticles(req.getParameter("actionGroupId"), ("ajax_ctrl".equals(req.getParameter("amid")) || page.isPreviewMode())));

			} else if (req.hasParameter(REQ_DOCUMENT_ID)) {
				String userId = AppUtil.getMTSUserId(req);
				setModuleData(getDocument(req.getParameter(REQ_DOCUMENT_ID), null, false, userId));

			} else {
				setModuleData(getArticles(new BSTableControlVO(req, MTSDocumentVO.class), req));
			}
		} catch (Exception e) {
			setModuleData(null, 0, e.getLocalizedMessage());
		}
	}

	/**
	 * 
	 * @param groupId
	 * @return
	 */
	public List<RelatedArticleVO> getRelatedArticles(String groupId, boolean isPagePreview) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(408);
		sql.append("select d.action_id, action_nm, publish_dt, first_nm, last_nm, ");
		sql.append("a.document_id, related_article_id, user_id,f.widget_meta_data_id, f.field_nm, f.parent_id ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("mts_related_article a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("mts_document b on a.related_document_id = b.document_id ");
		sql.append("inner join sb_action d on a.related_document_id = d.action_group_id and pending_sync_flg = 0 ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("mts_user c on b.author_id = c.user_id ");
		sql.append("left outer join widget_meta_data_xr e on d.action_id = e.action_id ");
		sql.append("left outer join widget_meta_data f on e.widget_meta_data_id = f.widget_meta_data_id ");
		sql.append("where a.document_id=? ");

		if (!isPagePreview)
			sql.append("and (b.publish_dt < CURRENT_TIMESTAMP or b.publish_dt is null) "); //the article released

		sql.append("order by action_nm, related_article_id ");
		log.debug(sql.length() + "|" + sql + "|" + groupId);

		DBProcessor db = new DBProcessor(getDBConnection()); 
		return db.executeSelect(sql.toString(), Arrays.asList(groupId), new RelatedArticleVO());
	}

	/**
	 * Retrieves the document to be edited
	 * @param documentId
	 * @return
	 * @throws SQLException
	 */
	public MTSDocumentVO getDocument(String documentId, String directPath, boolean pagePreview, String userId) 
			throws SQLException {
		if (StringUtil.isEmpty(documentId) && StringUtil.isEmpty(directPath)) 
			throw new SQLException("No identifier passed for the document");

		StringBuilder sql = new StringBuilder(640);
		sql.append("select publication_nm, e.publication_id, d.issue_nm, ");
		sql.append("coalesce(len(f.user_info_id), 0) as bookmark_flg, a.*, b.*, c.* ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("mts_document a ");
		sql.append(DBUtil.INNER_JOIN).append("sb_action b on a.action_group_id = b.action_group_id ");
		sql.append(DBUtil.INNER_JOIN).append("document c on b.action_id = c.action_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema());
		sql.append("mts_issue d on a.issue_id = d.issue_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema());
		sql.append("mts_publication e on d.publication_id = e.publication_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("mts_user_info f ");
		sql.append("on a.unique_cd = f.value_txt and f.user_id = ? and f.user_info_type_cd = 'BOOKMARK' ");

		if (!StringUtil.isEmpty(documentId)) {
			sql.append("where a.document_id = ? order by pending_sync_flg desc ");
		} else {
			sql.append("where c.direct_access_pth = ? ");
			if (!pagePreview) 
				sql.append("and d.approval_flg=1 and b.pending_sync_flg=0 and (a.publish_dt < CURRENT_TIMESTAMP or a.publish_dt is null) and (d.issue_dt < CURRENT_TIMESTAMP or d.issue_dt is null) ");
			sql.append("order by pending_sync_flg ");
			sql.append(pagePreview ? "desc" : "asc");
		}

		log.debug(sql.length() + "|" + sql + "|" + documentId + "|" + directPath + "|" + pagePreview);

		MTSDocumentVO doc = new MTSDocumentVO();
		boolean isFound = false;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, userId);
			ps.setString(2, StringUtil.isEmpty(documentId) ? directPath : documentId);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					doc = new MTSDocumentVO(rs);
					isFound = true;
				}
			}
		}

		if (isFound) doc.setCategories(getCategories(doc.getActionId()));
		return doc;
	}

	/**
	 * Gets the categories for the given action id
	 * @param actionId
	 * @return
	 */
	public List<WidgetMetadataVO> getCategories(String actionId) {
		// Get the categories
		StringBuilder s = new StringBuilder(184);
		s.append("select a.widget_meta_data_id, parent_id, field_nm, b.widget_meta_data_xr_id from widget_meta_data a left outer join widget_meta_data_xr b ");
		s.append("on a.widget_meta_data_id = b.widget_meta_data_id and action_id = ? ");
		s.append("where organization_id = 'MTS' order by parent_id desc, field_nm");
		DBProcessor db = new DBProcessor(getDBConnection());

		return db.executeSelect(s.toString(), Arrays.asList(actionId), new WidgetMetadataVO(), "widget_meta_data_id");
	}

	/**
	 * 
	 * @param pubId
	 * @return
	 */
	public PublicationTeaserVO getArticleTeasers(String pubId, String catId, boolean useLatest) {
		StringBuilder sql = new StringBuilder(1088);
		String schema = getCustomSchema();

		sql.append("select a.document_id, c.action_id, first_nm, last_nm, a.publish_dt, a.author_id, ");
		sql.append("c.action_nm, c.action_desc, b.issue_nm, m.field_nm as value_txt, m.widget_meta_data_id, p.publication_id, ");
		sql.append("publication_nm, p.publication_desc, b.category_cd, direct_access_pth, b.issue_id ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("mts_document a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("mts_issue b on a.issue_id = b.issue_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("mts_publication p on b.publication_id = p.publication_id ");
		sql.append("inner join sb_action c on a.action_group_id = c.action_group_id and c.pending_sync_flg = 0 ");
		sql.append("inner join document doc on c.action_id = doc.action_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("mts_user u on a.author_id = u.user_id ");
		sql.append("left outer join ( ");
		sql.append("select action_id, field_nm, b.widget_meta_data_id ");
		sql.append("from widget_meta_data_xr a ");
		sql.append("inner join widget_meta_data b on a.widget_meta_data_id = b.widget_meta_data_id ");
		sql.append("where organization_id = 'MTS' and parent_id = 'CHANNELS' ");
		sql.append(") m on c.action_id = m.action_id ");

		if (! useLatest && ! StringUtil.isEmpty(catId)) {
			sql.append("where b.approval_flg = 1 and p.publication_id = ? ");
		} else {
			sql.append("where issue_dt in ( ");
			sql.append("select max(issue_dt) as latest ");
			sql.append(DBUtil.FROM_CLAUSE).append(schema).append("mts_issue ");
			sql.append("where publication_id = ? and approval_flg = 1 ) and publish_dt is not null ");
			sql.append("and p.publication_id = ? ");
		}
		sql.append("order by a.publish_dt desc, document_id limit 10");
		log.debug(sql + "|" + pubId + "|" + catId);

		PublicationTeaserVO ptvo = null;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {

			if (! useLatest && ! StringUtil.isEmpty(catId)) {
				ps.setString(1, pubId);
			} else {
				ps.setString(1, pubId);
				ps.setString(2, pubId);
			}
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (ptvo == null) {
					ptvo = new PublicationTeaserVO(rs);
					ptvo.setPublication(new PublicationVO(rs));
				}

				MTSDocumentVO doc = new MTSDocumentVO(rs);
				doc.addCategory(new WidgetMetadataVO(rs));
				doc.setAuthor(new MTSUserVO(rs));
				ptvo.addDocument(doc);
			}
			if (ptvo != null) assignAssets(ptvo);

		} catch (Exception e) {
			log.error("Unable to retrieve teaser data", e);
		}

		return ptvo;
	}

	/**
	 * Adds the assets to the teaser vo
	 * @param ptvo
	 * @throws SQLException
	 * @throws DatabaseException 
	 */
	private void assignAssets(PublicationTeaserVO ptvo) throws SQLException, DatabaseException {
		Set<String> ids = ptvo.getAssetObjectKeys();
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from ").append(getCustomSchema()).append("mts_document_asset ");
		sql.append("where asset_type_cd in ('FEATURE_IMG') ");
		sql.append("and object_key_id in ( ");
		sql.append(DBUtil.preparedStatmentQuestion(ids.size())).append(") ");
		sql.append("order by object_key_id");
		log.debug(sql.length() + "|" + sql + "|" + ids);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			DBUtil.preparedStatementValues(ps, 1, new ArrayList<Object>(ids));

			try (ResultSet rs = ps.executeQuery()) {
				while(rs.next()) {
					ptvo.addAsset(new AssetVO(rs));
				}
			}
		}
	}

	/**
	 * 
	 * @return
	 */
	public GridDataVO<MTSDocumentVO> getArticles(BSTableControlVO bst, ActionRequest req) {
		// Add the params
		List<Object> vals = new ArrayList<>();

		// Build the sql
		StringBuilder sql = new StringBuilder(1280);
		sql.append("select * from ( ");
		sql.append("select action_nm, action_desc, publish_dt, b.action_id, b.action_group_id, ");
		sql.append("b.pending_sync_flg, m.approvable_flg, d.issue_id, issue_nm, d.publication_id, document_id, info_bar_txt, c.* ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("mts_document a "); 
		sql.append("inner join sb_action b on a.action_group_id = b.action_group_id "); 
		sql.append("inner join module_type m on b.module_type_id = m.module_type_id "); 
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("mts_user c ");
		sql.append("on a.author_id = c.user_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("mts_issue d ");
		sql.append("on a.issue_id = d.issue_id ");
		sql.append("where pending_sync_flg > 0 ");
		sql.append("union ");
		sql.append("select action_nm, action_desc, publish_dt, b.action_id, b.action_group_id, ");
		sql.append("b.pending_sync_flg, m.approvable_flg, d.issue_id, issue_nm, d.publication_id, document_id, info_bar_txt, c.* ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("mts_document a ");
		sql.append("inner join sb_action b on a.action_group_id = b.action_group_id "); 
		sql.append("inner join module_type m on b.module_type_id = m.module_type_id "); 
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("mts_user c ");
		sql.append("on a.author_id = c.user_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("mts_issue d ");
		sql.append("on a.issue_id = d.issue_id ");
		sql.append("where b.action_group_id not in ( ");
		sql.append("select action_group_id from sb_action where organization_id = 'MTS' ");
		sql.append("group by action_group_id having count(*) > 1 ");
		sql.append(") ");
		sql.append(") as articles ");
		sql.append("where 1=1 ");

		// Add the filters
		assignFilters(sql, vals, req, bst);

		// Set the order by
		sql.append(bst.getSQLOrderBy("publish_dt desc, action_nm", ""));
		log.debug(sql.length() + "|" + sql + "|" + vals);

		// Get the articles
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSQLWithCount(sql.toString(), vals, new MTSDocumentVO(), bst);
	}

	/**
	 * Assigns the filter to the listing of articles
	 * @param sql
	 * @param vals
	 * @param req
	 * @param bst
	 */
	private void assignFilters(StringBuilder sql, List<Object> vals, ActionRequest req, BSTableControlVO bst) {
		// Filter by the issue id (Publication drill down for articles)
		if (! StringUtil.isEmpty(req.getParameter("issueId"))) {
			sql.append("and issue_id = ? ");
			vals.add(req.getParameter("issueId"));
		}

		// Filter by the search bar text input
		if (bst.hasSearch()) {
			sql.append("and (lower(action_nm) like ? or lower(action_desc) = ?) ");
			vals.add(bst.getLikeSearch().toLowerCase());
			vals.add(bst.getLikeSearch().toLowerCase());
		}

		// Filter by tool bar publication filter
		if (! StringUtil.isEmpty(req.getParameter("filterPublicationId"))) {
			sql.append("and publication_id = ? ");
			vals.add(req.getParameter("filterPublicationId"));
		}

		// Filter by tool bar Issue filter
		if (! StringUtil.isEmpty(req.getParameter("filterIssueId"))) {
			sql.append("and issue_id = ? ");
			vals.add(req.getParameter("filterIssueId"));
		}

		// Filter by tool bar category filter
		if (! StringUtil.isEmpty(req.getParameter("filterCategoryId"))) {
			sql.append("and document_id in (select action_id from ");
			sql.append("widget_meta_data_xr ");
			sql.append("where widget_meta_data_id = ?) ");
			vals.add(req.getParameter("filterCategoryId"));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {

		try {
			if (req.hasParameter("assignArticle")) {
				String did = req.getParameter(REQ_DOCUMENT_ID);
				String raid = req.getParameter("relatedDocumentId");
				setModuleData(assignRelatedArticle(did, raid));
			} else if (req.hasParameter("deleteRelated")) {
				deleteRelatedArticle(req.getParameter("relatedArticleId"));
			} else if (req.hasParameter("saveInfoBar")) {
				saveInfoBar(new MTSDocumentVO(req));
			} else {
				setModuleData(saveInfo(req));
			}
		} catch (Exception e) {
			log.error("Unable to save publication info", e);
			putModuleData(null, 0, false, e.getLocalizedMessage(), true);
		}
	}

	/**
	 * Updates the info bar data
	 * @param doc
	 * @throws InvalidDataException
	 * @throws com.siliconmtn.db.util.DatabaseException
	 */
	public void saveInfoBar(MTSDocumentVO doc) throws Exception {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.update(doc, Arrays.asList("info_bar_txt", "document_id"));
	}

	/**
	 * 
	 * @param req
	 * @throws InvalidDataException
	 * @throws com.siliconmtn.db.util.DatabaseException
	 * @throws ActionException
	 */
	public MTSDocumentVO saveInfo(ActionRequest req) throws Exception {
		// Save the SB Action data and the WC Document
		MTSDocumentVO doc = new MTSDocumentVO(req);
		DocumentAction da = new DocumentAction(getDBConnection(), getAttributes());
		da.update(req);

		// Save the info into the SB Action
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.save(doc);

		// Return the data
		return doc;
	}

	/**
	 * Removes a related article assignment
	 * @param relatedArticleId
	 * @throws InvalidDataException
	 * @throws com.siliconmtn.db.util.DatabaseException
	 */
	public void deleteRelatedArticle(String relatedArticleId) throws Exception {
		RelatedArticleVO vo = new RelatedArticleVO();
		vo.setRelatedArticleId(relatedArticleId);

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.delete(vo);
	}


	/**
	 * Assigns a related article to another article
	 * @param documentId
	 * @param relatedDocumentId
	 * @throws InvalidDataException
	 * @throws com.siliconmtn.db.util.DatabaseException
	 */
	public RelatedArticleVO assignRelatedArticle(String documentId, String relatedDocumentId) throws Exception {
		if (StringUtil.isEmpty(relatedDocumentId)) throw new InvalidDataException("Related document is required");
		RelatedArticleVO vo = new RelatedArticleVO();
		vo.setDocumentId(documentId);
		vo.setRelatedDocumentId(relatedDocumentId);

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.insert(vo);
		return vo;
	}
}
