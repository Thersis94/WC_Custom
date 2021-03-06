package com.mts.publication.action;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// MTS Libs
import com.mts.publication.data.MTSDocumentVO;
import com.mts.util.AppUtil;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: DocumentBrowseAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Performs document searches using metadata
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jun 21, 2019
 * @updates:
 ****************************************************************************/
public class DocumentBrowseAction extends SimpleActionAdapter {
	/**
	 * Ajax Controller key for this action
	 */
	public static final String AJAX_KEY = "browse";

	/**
	 * 
	 */
	public DocumentBrowseAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public DocumentBrowseAction(ActionInitVO arg0) {
		super(arg0);
	}

	/**
	 * 
	 * @param dbConn
	 * @param attributes
	 */
	public DocumentBrowseAction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		setDBConnection(dbConn);
		setAttributes(attributes);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		// On the initial page load, get the issue data if passed
		if (! req.hasParameter("json") && ! StringUtil.isEmpty(req.getParameter("issueId"))) {
			IssueAction ia = new IssueAction(getDBConnection(), getAttributes());
			setModuleData(ia.getIssue(req.getParameter("issueId")));
			return;
		}

		String userId = AppUtil.getMTSUserId(req);
		String pubs = StringUtil.checkVal(req.getParameter("publications")).toUpperCase();
		String topics = req.getParameter("topics");
		String cats = req.getParameter("categories");
		String issues = req.getParameter("issues");
		BSTableControlVO bst = new BSTableControlVO(req);
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		setModuleData(search(bst, pubs, topics, cats, issues, userId, page.isPreviewMode()));
	}

	/**
	 * Performs a complex search and returns the list of matches
	 * @param bst
	 * @return
	 */
	public GridDataVO<MTSDocumentVO> search(BSTableControlVO bst, String pubs, String topics, String cats, String issues, String userId, boolean isPagePreview) {
		List<Object> vals = new ArrayList<>();
		vals.add(userId);

		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(1672);
		sql.append("select b.document_id, b.unique_cd, a.action_id, action_nm, action_desc, b.publish_dt, direct_access_pth, ");
		sql.append("d.user_id, first_nm, last_nm, c.publication_id, publication_nm, newid() as document_asset_id, ");
		sql.append("coalesce(doc_img, cat_img, '/000/000/feature.png') as document_path, ");
		sql.append("coalesce(len(e.user_info_id), 0) as bookmark_flg, b.sponsor_id ");
		sql.append("from sb_action a ");
		sql.append("inner join document doc on a.action_id = doc.action_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("mts_document b on a.action_group_id = b.action_group_id and pending_sync_flg = 0 ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("mts_issue c on b.issue_id = c.issue_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("mts_publication p on c.publication_id = p.publication_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("mts_user d on b.author_id = d.user_id ");
		sql.append(DBUtil.INNER_JOIN);
		sql.append("(select document_id, ");
		sql.append("string_agg(da.document_path, ',') as cat_img, string_agg(da1.document_path, ',') as doc_img ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("mts_document a ");
		sql.append("inner join sb_action b on a.action_group_id = b.action_group_id and pending_sync_flg = 0 ");
		sql.append("left outer join widget_meta_data_xr c on b.action_id = c.action_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("mts_document_asset da on c.widget_meta_data_id = da.object_key_id and da.asset_type_cd = 'FEATURE_IMG' ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("mts_document_asset da1 on a.document_id = da1.object_key_id and da1.asset_type_cd = 'FEATURE_IMG' ");
		sql.append("group by document_id ");
		sql.append(") as i on b.document_id = i.document_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("mts_user_info e ");
		sql.append("on b.unique_cd = e.value_txt and e.user_id = ? and e.user_info_type_cd = 'BOOKMARK' ");
		sql.append("where c.approval_flg=1 ");

		if (!isPagePreview) {
			sql.append(" and (b.publish_dt < CURRENT_TIMESTAMP or b.publish_dt is null) "); //the article released
			sql.append(" and (c.issue_dt < CURRENT_TIMESTAMP or c.issue_dt is null) "); //the issue released
		}

		// Add the text search
		if (bst.hasSearch()) addSearchFilter(sql, vals, bst);
		if (! StringUtil.isEmpty(cats)) addCatFilter(sql, vals, cats);
		if (! StringUtil.isEmpty(topics)) addCatFilter(sql, vals, topics);
		if (! StringUtil.isEmpty(pubs)) addPublicationFilter(sql, vals, pubs);
		if (! StringUtil.isEmpty(issues)) addIssueFilter(sql, vals, issues);

		if (!StringUtil.isEmpty(bst.getOrder())) sql.append("order by ").append(bst.getOrder());
		else sql.append("order by action_nm ");
		log.debug(sql.length() + "|" + sql + "|" + vals);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), vals, new MTSDocumentVO(), bst);
	}

	/**
	 * Adds the filter for the text search
	 * @param sql
	 * @param vals
	 * @param bst
	 */
	public void addSearchFilter(StringBuilder sql, List<Object> vals, BSTableControlVO bst) {
		sql.append("and (lower(action_nm) like ? or lower(action_desc) like ? ");
		sql.append("or lower(first_nm || ' ' || last_nm) like ?) ");
		vals.add(bst.getLikeSearch().toLowerCase());
		vals.add(bst.getLikeSearch().toLowerCase());
		vals.add(bst.getLikeSearch().toLowerCase());
	}

	/**
	 * Creates the filter for the multi-select categories
	 * @param sql
	 * @param vals
	 * @param cat
	 */
	public void addCatFilter(StringBuilder sql, List<Object> vals, String cat) {
		List<String> cats = Arrays.asList(cat.split("\\,"));
		sql.append("and a.action_id in ( ");
		sql.append("select action_id ");
		sql.append("from widget_meta_data_xr ");
		sql.append("where widget_meta_data_id in (");
		sql.append(DBUtil.preparedStatmentQuestion(cats.size())).append(")) ");
		vals.addAll(cats);
	}

	/**
	 * Filters by publications
	 * @param sql
	 * @param vals
	 * @param pub
	 */
	public void addPublicationFilter(StringBuilder sql, List<Object> vals, String pub) {
		List<String> pubs = Arrays.asList(pub.split("\\,"));
		sql.append("and c.publication_id in (");
		sql.append(DBUtil.preparedStatmentQuestion(pubs.size())).append(") ");
		vals.addAll(pubs);
	}

	/**
	 * Filters by publication issues
	 * @param sql
	 * @param vals
	 * @param issue
	 */
	public void addIssueFilter(StringBuilder sql, List<Object> vals, String issue) {
		List<String> issues = Arrays.asList(issue.split("\\,"));
		sql.append("and c.issue_id in (");
		sql.append(DBUtil.preparedStatmentQuestion(issues.size())).append(") ");
		vals.addAll(issues);
	}
}
