package com.mts.publication.action;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.mts.action.SelectLookupAction;
import com.mts.publication.data.AssetVO;
// MTS Libs
import com.mts.publication.data.IssueVO;
// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
//WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: IssueAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Widget to manage MTS issues for a given publication
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 8, 2019
 * @updates:
 ****************************************************************************/

public class IssueAction extends SBActionAdapter {

	/**
	 * Ajax Controller key for this action
	 */
	public static final String AJAX_KEY = "issue";

	/**
	 * 
	 */
	public IssueAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public IssueAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * 
	 * @param dbConn
	 * @param attributes
	 */
	public IssueAction(SMTDBConnection dbConn, Map<String, Object> attributes) {
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
		if (! req.hasParameter("json")) return;
		String pubId = req.getParameter("publicationId");
		BSTableControlVO bst = new BSTableControlVO(req, IssueVO.class);
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		//turn off date filtering in the portal, or if the page is being previewed
		setModuleData(getIssues(pubId, false, bst, ("ajax_ctrl".equals(req.getParameter("amid")) || page.isPreviewMode())));
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);

		SelectLookupAction sla = new SelectLookupAction();
		sla.setDBConnection(getDBConnection());
		sla.setAttributes(getAttributes());
		req.setAttribute("mts_publications", sla.getPublications(req));
	}

	/**
	 * 
	 * @return
	 */
	public GridDataVO<IssueVO> getIssues(String pubId, boolean beenIssued, BSTableControlVO bst, boolean isPagePreview) {
		// Add the params
		List<Object> vals = new ArrayList<>();
		vals.add(pubId);

		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(352);
		sql.append("select coalesce(article_count, 0) as article_no, a.*, b.*, da.*  from ");
		sql.append(schema).append("mts_issue a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("mts_user b on a.editor_id = b.user_id ");
		sql.append("left outer join ( ");
		sql.append("select issue_id, count(*) as article_count ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("mts_document d ");
		sql.append("inner join sb_action s on d.action_group_id = s.action_group_id ");
		sql.append("and pending_sync_flg = 0 ");
		if (!isPagePreview) 
			sql.append("and (d.publish_dt < CURRENT_TIMESTAMP or d.publish_dt is null) ");
		sql.append("group by issue_id ");
		sql.append(") c on a.issue_id = c.issue_id ");
		sql.append("left outer join ( ");
		sql.append("select object_key_id, string_agg(document_path, ',') as document_path ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("mts_document_asset where asset_type_cd = 'COVER_IMG' ");
		sql.append("group by object_key_id ");
		sql.append(") as da on a.issue_id = da.object_key_id ");
		sql.append("where publication_id = ? ");
		if (beenIssued) sql.append("and issue_dt is not null ");
		if (!isPagePreview) 
			sql.append("and (a.issue_dt < CURRENT_TIMESTAMP or a.issue_dt is null) ");

		// Add the search vals
		if(bst.hasSearch()) {
			sql.append("and lower(issue_nm) like ? ");
			vals.add(bst.getLikeSearch().toLowerCase());
		}

		sql.append(bst.getSQLOrderBy("issue_dt", "desc"));
		log.debug(sql.length() + "|" + sql + "|" + pubId + "|" + bst.getOffset());

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), vals, new IssueVO(), bst);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		IssueVO issue = new IssueVO(req);

		try {
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			db.save(issue);
			putModuleData(issue);
		} catch (Exception e) {
			log.error("Unable to save publication info", e);
			putModuleData(issue, 1, false, e.getLocalizedMessage(), true);
		}
	}

	/**
	 * Retrieves the data for the feature asset
	 * @param issueId
	 * @return
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public IssueVO getIssue(String issueId) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(300);
		sql.append("select a.*, b.publication_nm, article_no from ");
		sql.append(schema).append("mts_issue a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("mts_publication b on a.publication_id=b.publication_id ");
		sql.append(DBUtil.INNER_JOIN).append("(select issue_id, count(*) as article_no ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("mts_document ");
		sql.append("where (publish_dt < CURRENT_TIMESTAMP or publish_dt is null) and issue_id=? ");
		sql.append("group by issue_id) as n on a.issue_id=n.issue_id ");
		sql.append("where a.issue_id=? ");

		List<Object> vals = Arrays.asList(issueId, issueId);
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		List<IssueVO> issues = db.executeSelect(sql.toString(), vals, new IssueVO());
		
		if (issues.isEmpty()) return null;

		IssueVO issue = issues.get(0);
		issue.setAssets(getFeatureAssets(issueId));
		return issue;
	}

	/**
	 * Gets the feature assets for the issue
	 * @param issueId
	 * @return
	 */
	protected List<AssetVO> getFeatureAssets(String issueId) {
		StringBuilder sql = new StringBuilder(128);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("mts_document_asset ");
		sql.append("where object_key_id = ? and asset_type_cd = 'COVER_IMG' ");

		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), Arrays.asList(issueId), new AssetVO());
	}

}

