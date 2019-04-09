package com.mts.publication.action;

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
import com.siliconmtn.db.orm.*;

//WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.content.DocumentAction;

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
		String issueId = req.getParameter("issueId");
		BSTableControlVO bst = new BSTableControlVO(req, MTSDocumentVO.class);
		setModuleData(getArticles(issueId, bst));
	}
	
	/**
	 * 
	 * @return
	 */
	public GridDataVO<MTSDocumentVO> getArticles(String issueId, BSTableControlVO bst) {
		StringBuilder sql = new StringBuilder(184);
		sql.append("select * from custom.mts_document a ");
		sql.append("inner join sb_action b on a.action_id = b.action_id ");
		sql.append("left outer join custom.mts_user c on a.author_id = c.user_id ");
		sql.append("where issue_id = ? ");
		sql.append("order by action_nm ");

		log.info(sql.length() + "|" + sql + "|" + issueId);
		
		// Add the params
		List<Object> vals = new ArrayList<>();
		vals.add(issueId);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSQLWithCount(sql.toString(), vals, new MTSDocumentVO(), bst);
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

