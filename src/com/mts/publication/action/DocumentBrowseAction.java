package com.mts.publication.action;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// MTS Libs
import com.mts.publication.data.MTSDocumentVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.util.StringUtil;

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
		StringBuilder sql = new StringBuilder(1408);
		sql.append("select b.unique_cd, a.action_id, action_nm, action_desc, b.publish_dt, direct_access_pth, ");
		sql.append("user_id, first_nm, last_nm, publication_id, newid() as document_asset_id, ");
		sql.append("case ");
		sql.append("when doc_img is not null then doc_img ");
		sql.append("when cat_img is not null then cat_img ");
		sql.append("else '/000/000/feature.png' ");
		sql.append("end as document_path ");
		sql.append("from sb_action a ");
		sql.append("inner join document doc on a.action_id = doc.action_id ");
		sql.append("inner join custom.mts_document b ");
		sql.append("on a.action_group_id = b.action_group_id and pending_sync_flg = 0 ");
		sql.append("inner join custom.mts_issue c on b.issue_id = c.issue_id ");
		sql.append("inner join custom.mts_user d on b.author_id = d.user_id ");
		sql.append("inner join ( ");
		sql.append("select document_id, ");
		sql.append("string_agg(da.document_path, ',') as cat_img, string_agg(da1.document_path, ',') as doc_img ");
		sql.append("from custom.mts_document a ");
		sql.append("inner join sb_action b on a.action_group_id = b.action_group_id and pending_sync_flg = 0 ");
		sql.append("left outer join widget_meta_data_xr c on b.action_id = c.action_id ");
		sql.append("left outer join custom.mts_document_asset da on c.widget_meta_data_id = da.object_key_id ");
		sql.append("left outer join custom.mts_document_asset da1 on a.document_id = da1.object_key_id ");
		sql.append("group by document_id ");
		sql.append(") as i on b.document_id = i.document_id ");
		sql.append("where 1=1 ");
		
		// Add the text search
		if (bst.hasSearch()) addSearchFilter(sql, vals, bst);
		if (! StringUtil.isEmpty(cats)) addCatFilter(sql, vals, cats);
		if (! StringUtil.isEmpty(topics)) addCatFilter(sql, vals, topics);
		if (! StringUtil.isEmpty(pubs)) addPublicationFilter(sql, vals, pubs);
		
		sql.append("order by action_nm ");
		log.debug(sql.length() + "|" + sql + "|" + vals);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return  db.executeSQLWithCount(sql.toString(), vals, new MTSDocumentVO(), bst);
	}
	
	/**
	 * Adds the filter for the text search
	 * @param sql
	 * @param vals
	 * @param bst
	 */
	public void addSearchFilter(StringBuilder sql, List<Object> vals, BSTableControlVO bst) {
		sql.append("and (lower(action_nm) like ? or lower(action_desc) like ?)");
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
}
