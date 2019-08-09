package com.mts.publication.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// MTS Libs
import com.mts.publication.data.AssetVO;
import com.mts.publication.data.MTSDocumentVO;
import com.mts.publication.data.PublicationTeaserVO;
// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title</b>: ArticleByCategoryAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Gets a collection of articles by the given category
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jun 20, 2019
 * @updates:
 ****************************************************************************/
public class ArticleByCategoryAction extends SimpleActionAdapter {
	/**
	 * Ajax Controller key for this action
	 */
	public static final String AJAX_KEY = "art-by-cat";
	
	/**
	 * 
	 */
	public ArticleByCategoryAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public ArticleByCategoryAction(ActionInitVO arg0) {
		super(arg0);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String catId = req.getParameter("catId");
		String pubId = req.getParameter("publicationId");
		setModuleData(getArticles(catId, pubId, req.getIntegerParameter("number", 4)));
	}
	
	/**
	 * Gets a list of articles for the 
	 * @param cat
	 * @param count
	 * @return
	 */
	public List<MTSDocumentVO> getArticles(String cat, String pubId, int count) {
		// Create the structure
		List<Object> vals = new ArrayList<>();
		vals.add(cat);
		
		StringBuilder sql = new StringBuilder(464);
		sql.append("select action_nm, action_desc, a.action_id, document_id, ");
		sql.append("unique_cd, publication_id, direct_access_pth, publish_dt ");
		sql.append("from widget_meta_data_xr  a ");
		sql.append("inner join sb_action b on a.action_id = b.action_id and b.pending_sync_flg = 0 ");
		sql.append("inner join document doc on b.action_id = doc.action_id ");
		sql.append("inner join ").append(getCustomSchema()).append("mts_document c ");
		sql.append("on b.action_group_id = c.document_id ");
		sql.append("inner join ").append(getCustomSchema()).append("mts_issue d ");
		sql.append("on c.issue_id = d.issue_id ");
		sql.append("where widget_meta_data_id = ? "); 
		if (! StringUtil.isEmpty(pubId)) {
			sql.append("and publication_id = ? ");
			vals.add(pubId);
		}
		
		sql.append("order by random() limit ? ");
		vals.add(count);
		log.debug(sql.length() + "|" + sql + "|" + vals);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		List<MTSDocumentVO> docs =  db.executeSelect(sql.toString(), vals, new MTSDocumentVO());
		try {
			assignDocumentAsset(docs, cat);
		} catch (SQLException e) {
			log.error("Unabel to load assets", e);
		}
		
		return docs;
	}
	
	/**
	 * Gets an image asset for a given document
	 * @param docs
	 * @param cat
	 * @throws SQLException
	 */
	public void assignDocumentAsset(List<MTSDocumentVO> docs, String cat) throws SQLException {
		Map<String, AssetVO> assets = new HashMap<>();
		
		StringBuilder sql = new StringBuilder(256);
		sql.append("select object_key_id, document_path, document_asset_id, asset_type_cd ");
		sql.append("from ").append(getCustomSchema()).append("mts_document_asset ");
		sql.append("where asset_type_cd in ('FEATURE_IMG') and object_key_id in (");
		sql.append(DBUtil.preparedStatmentQuestion(docs.size() + 3));
		sql.append(") and asset_type_cd != 'PDF_DOC' ");
		sql.append("order by random() ");
		log.debug(sql.length() + "|" + sql);
		
		// Get the assets for the document ids and category
		int ctr = 1;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
	
			for (MTSDocumentVO doc : docs) ps.setString(ctr++, doc.getDocumentId());
			ps.setString(ctr++, PublicationTeaserVO.DEFAULT_FEATURE_IMG);
			ps.setString(ctr++, PublicationTeaserVO.DEFAULT_TEASER_IMG);
			ps.setString(ctr++, cat);
			
			try (ResultSet rs = ps.executeQuery()) {
				while(rs.next())
					assets.put(rs.getString(1), new AssetVO(rs));
			}
		}
		
		log.debug("assets size " + assets.size());
	
		// Add an asset to the document
		for (MTSDocumentVO doc : docs) {
			if (assets.containsKey(doc.getDocumentId())) {
				doc.addAsset(assets.get(doc.getDocumentId()));
			} else {
				doc.addAsset(assets.get(cat));
			}
		}
		
	}
}
