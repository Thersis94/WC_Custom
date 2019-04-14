package com.mts.publication.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;

// MTS Libs
import com.mts.publication.data.MTSDocumentVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.*;

//WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.content.DocumentAction;

import opennlp.tools.util.StringUtil;

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
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		try {
			if (req.hasParameter("documentId")) {
				setModuleData(getDocument(req.getParameter("documentId")));
			} else {
				setModuleData(getArticles(req));
			}
		} catch (Exception e) {
			setModuleData(null, 0, e.getLocalizedMessage());
		}
	}
	
	/**
	 * Retrieves the document to be edited
	 * @param documentId
	 * @return
	 * @throws SQLException
	 */
	public MTSDocumentVO getDocument(String documentId) throws SQLException {
		StringBuilder sql = new StringBuilder(384);
		sql.append("select publication_nm, e.publication_id, d.issue_nm, a.*, b.*, c.* ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("mts_document a ");
		sql.append(DBUtil.INNER_JOIN).append("sb_action b on a.document_id = b.action_id ");
		sql.append(DBUtil.INNER_JOIN).append("document c on b.action_id = c.action_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema());
		sql.append("mts_issue d on a.issue_id = d.issue_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema());
		sql.append("mts_publication e on d.publication_id = e.publication_id ");
		sql.append("where document_id = ? ");
		log.debug(sql.length() + "|" + sql + "|" + documentId);
		
		MTSDocumentVO doc = new MTSDocumentVO();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, documentId);
			
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) doc = new MTSDocumentVO(rs);
			}
		}
		
		return doc;
	}
	
	/**
	 * 
	 * @return
	 */
	public GridDataVO<MTSDocumentVO> getArticles(ActionRequest req) {
		BSTableControlVO bst = new BSTableControlVO(req, MTSDocumentVO.class);
		
		// Add the params
		List<Object> vals = new ArrayList<>();
		
		// Build the sql
		StringBuilder sql = new StringBuilder(448);
		sql.append("select * from ").append(getCustomSchema()).append("mts_document a ");
		sql.append("inner join sb_action b on a.action_id = b.action_id ");
		sql.append("left outer join ").append(getCustomSchema());
		sql.append("mts_user c on a.author_id = c.user_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema());
		sql.append("mts_issue d on a.issue_id = d.issue_id ");
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
			sql.append("and a.issue_id = ? ");
			vals.add(req.getParameter("issueId"));
		}
		
		// Filter by the search bar text input
		if (bst.hasSearch()) {
			sql.append("and (lower(action_nm) like ? or lower(action_desc) = ?) ");
			vals.add(bst.getLikeSearch());
			vals.add(bst.getLikeSearch());
		}
		
		// Filter by tool bar publication filter
		if (! StringUtil.isEmpty(req.getParameter("filterPublicationId"))) {
			sql.append("and d.publication_id = ? ");
			vals.add(req.getParameter("filterPublicationId"));
		}
		
		// Filter by tool bar Issue filter
		if (! StringUtil.isEmpty(req.getParameter("filterIssueId"))) {
			sql.append("and a.issue_id = ? ");
			vals.add(req.getParameter("filterIssueId"));
		}
		
		// Filter by tool bar category filter
		if (! StringUtil.isEmpty(req.getParameter("filterCategoryId"))) {
			sql.append("and a.document_id in (select document_id from ");
			sql.append(getCustomSchema()).append("mts_category_document_xr ");
			sql.append("where category_cd = ?) ");
			vals.add(req.getParameter("filterCategoryId"));
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// Define the vo
		MTSDocumentVO doc = new MTSDocumentVO(req);
		try {
			// Save the SB Action data and the WC Document
			DocumentAction da = new DocumentAction(getDBConnection(), getAttributes());
			da.update(req);
			
			// Save the info into the SB Action
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			db.save(doc);
			
			// Return the data
			putModuleData(doc);
		} catch (Exception e) {
			log.error("Unable to save publication info", e);
			putModuleData(doc, 1, false, e.getLocalizedMessage(), true);
		}
	}

}

