package com.mts.publication.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: DocumentUtilAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> ***Change Me
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
				if (rs.next()) url = rs.getString(1);
			}
		}
		
		return url;
	}
}
