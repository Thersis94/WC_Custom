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
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;

// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;

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

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String pubs = req.getParameter("publications");
		String topics = req.getParameter("topics");
		String cats = req.getParameter("categories");
		BSTableControlVO bst = new BSTableControlVO(req);
		setModuleData(search(bst, pubs, topics, cats));
	}
	
	/**
	 * Performs a complex search and returns the list of matches
	 * @param bst
	 * @return
	 */
	public GridDataVO<MTSDocumentVO> search(BSTableControlVO bst, String pubs, String topics, String cats) {
		List<Object> vals = new ArrayList<>();
		StringBuilder sql = new StringBuilder(640);
		sql.append("select b.unique_cd, a.action_id, action_nm, action_desc, b.publish_dt, ");
		sql.append("user_id, first_nm, last_nm, publication_id ");
		sql.append("from sb_action a ");
		sql.append("inner join custom.mts_document b ");
		sql.append("on a.action_group_id = b.action_group_id and pending_sync_flg = 0 ");
		sql.append("inner join custom.mts_issue c on b.issue_id = c.issue_id ");
		sql.append("inner join custom.mts_user d on b.author_id = d.user_id ");
		sql.append("where 1=1 ");
		
		if (bst.hasSearch()) {
			sql.append("and (lower(action_nm) like ? or lower(action_desc) like ?)");
			vals.add(bst.getLikeSearch().toLowerCase());
			vals.add(bst.getLikeSearch().toLowerCase());
		}
		
		sql.append("order by action_nm ");
		log.info(sql + "|" + vals);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSQLWithCount(sql.toString(), vals, new MTSDocumentVO(), bst);
	}
}
