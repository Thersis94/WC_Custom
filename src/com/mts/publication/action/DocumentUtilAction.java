package com.mts.publication.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.mts.publication.data.MTSDocumentVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.content.DocumentAction;
import com.smt.sitebuilder.approval.ApprovalController;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: DocumentUtilAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Utilities for the MTS Document
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Aug 2, 2019
 * @updates:
 ****************************************************************************/
public class DocumentUtilAction extends SBActionAdapter {
	
	/**
	 * Ajax Controller key for this action
	 */
	public static final String AJAX_KEY = "doc-util";
	
	/**
	 * 
	 */
	public DocumentUtilAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public DocumentUtilAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		try {
			// Clone the main WC Document
			DocumentAction da = new DocumentAction(getDBConnection(), getAttributes());
			da.copy(req);
			String sbActionId = (String) req.getAttribute("sbActionId"); 
			
			// Update the actionGroupId and document folder id 
			this.updateActionGroup(sbActionId);
			updateDocumentFolderId(sbActionId, req.getParameter("publicationId"));

			// Add the MTS Document Information
			MTSDocumentVO doc = this.addMTSDocument(req);
			
			// Copy the categories
			this.copyRelatedArticles(req.getParameter("documentId"), doc.getDocumentId());

			
		} catch (Exception e) {
			log.error("unable to clone document", e);
		}
		
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.FALSE);
		req.setAttribute(Constants.REDIRECT_URL, null);
	}
	
	/**
	 * Changes the path to the publication
	 * @param actionID
	 * @param pubId
	 * @throws SQLException
	 */
	public void updateDocumentFolderId(String actionID, String pubId) throws SQLException {
		StringBuilder sql = new StringBuilder(64);
		sql.append("update document set document_folder_id = ? where action_id = ?");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, pubId);
			ps.setString(2, actionID);
			ps.executeUpdate();
		}
	}
	
	/**
	 * Copies the related articles to the newly cloned article
	 * @param origDocId
	 * @param docId
	 * @throws SQLException
	 */
	public void copyRelatedArticles(String origDocId, String docId) throws SQLException {
		StringBuilder sql = new StringBuilder(128);
		sql.append("insert into custom.mts_related_article (related_article_id, document_id, ");
		sql.append("related_document_id, create_dt) ");
		sql.append("select replace(newid(), '-', ''), ?, related_document_id, now() "); 
		sql.append("from custom.mts_related_article ");
		sql.append("where document_id = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, docId);
			ps.setString(2, origDocId);
			ps.executeUpdate();
			
			log.debug(ps.toString());
		}
	}
	
	/**
	 * Adds the MTS document wrapper data around the cloned document
	 * @param req
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public MTSDocumentVO addMTSDocument(ActionRequest req) throws InvalidDataException, DatabaseException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		// load the original.  This is mostly to get the info bar data
		MTSDocumentVO doc = new MTSDocumentVO();
		doc.setDocumentId(req.getParameter("documentId"));
		db.getByPrimaryKey(doc);
		
		// Update the info to be saved
		doc.setDocumentId(null);
		doc.setActionGroupId((String)req.getAttribute("sbActionId"));
		doc.setIssueId(req.getParameter("issueId"));
		doc.setCreateDate(null);
		doc.setUpdateDate(null);
		doc.setPublishDate(null);
		doc.setUniqueCode(RandomAlphaNumeric.generateRandom(6, true).toUpperCase());
		db.insert(doc);
		
		return doc;
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		
		try {
			if (req.hasParameter("getUrl")) {
				setModuleData(getDocumentUrl(req.getParameter("documentId")));
			}
		} catch (Exception e) {
			setModuleData(null, 0, e.getLocalizedMessage());
		}
	}
	
	/**
	 * Gets the url for a given document id
	 * @param documentId
	 * @return
	 * @throws SQLException
	 */
	public String getDocumentUrl(String documentId) throws SQLException {
		StringBuilder sql = new StringBuilder(320);
		sql.append("select '/' || lower(publication_id) || '/article/' || direct_access_pth as url ");
		sql.append("from document a ");
		sql.append("inner join sb_action b on a.action_id = b.action_id ");
		sql.append("inner join custom.mts_document c on b.action_group_id = c.action_group_id ");
		sql.append("inner join custom.mts_issue d on c.issue_id = d.issue_id ");
		sql.append("where a.action_id = ? ");
		String url = "";
		log.debug(sql.length() + "|" + sql + "|" + documentId);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, documentId);
			
			try(ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					url = rs.getString(1) + "?pagePreview=";
					url += ApprovalController.generatePreviewApiKey(attributes);
				}
			}
		}
		
		return url;
	}
}
