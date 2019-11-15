package com.biomed.smarttrak.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrClient;

import com.biomed.smarttrak.action.rss.vo.RSSSolrDocumentVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.smt.sitebuilder.search.SMTAbstractIndex;
import com.smt.sitebuilder.util.solr.SolrActionUtil;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title:</b> RSSArticleIndexer.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Indexer for processing RSS Filtered Articles in Bulk.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Mar 26, 2019
 ****************************************************************************/
public class RSSArticleIndexer extends SMTAbstractIndex {

	public static final String INDEX_TYPE = "BIOMEDGPS_FEED";
	
	private static final int batchSize = 1000;
	
	public RSSArticleIndexer(Properties config) {
		super(config);
	}

	public static RSSArticleIndexer makeInstance(Map<String, Object> attributes) {
		return new RSSArticleIndexer(makeProperties(attributes));
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#addIndexItems(org.apache.solr.client.solrj.SolrClient)
	 */
	@SuppressWarnings("resource")
	@Override
	public void addIndexItems(SolrClient server) {
		// Never place this in a try with resources.
		// This server was given to this method and it is not this method's
		// job or right to close it.
		SolrActionUtil util = new SolrActionUtil(server);
		try {
			List<String> articleIds = loadArticleIds(null);
			int start = 0;
			while (start < articleIds.size()) {
				util.addDocuments(getDocuments(articleIds.subList(start, Math.min(start+batchSize, articleIds.size()))));
				start+=batchSize;
			}
		} catch (Exception e) {
			log.error("Failed to index Updates", e);
		}
	}

	protected List<String> loadArticleIds(List<String> filterIds) throws SQLException {
		StringBuilder sql = new StringBuilder(125);
		String customDb = config.getProperty(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select rs.rss_article_id from ").append(customDb).append("biomedgps_rss_article rs ");
		sql.append("inner join ").append(customDb).append("biomedgps_rss_filtered_article rfa on rfa.rss_article_id = rs.rss_article_id ");
		if (filterIds != null && !filterIds.isEmpty()) 
			sql.append("where rfa.rss_article_filter_id in (").append(DBUtil.preparedStatmentQuestion(filterIds.size())).append(") ");
		sql.append("group by rs.rss_article_id ");
		
		List<String> ids = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			
			if (filterIds != null && !filterIds.isEmpty()) {
				int i = 1;
				for (String id : filterIds)
					ps.setString(i++, id);
			}
			ResultSet rs = ps.executeQuery();
			
			while (rs.next())
				ids.add(rs.getString("rss_article_id"));
		}
		
		return ids;
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTAbstractIndex#getIndexType()
	 */
	@Override
	public String getIndexType() {
		return INDEX_TYPE;
	}

	/**
	 * Call UpdateAction to get a list of updates to push...or one Update.
	 * @param util 
	 * @param documentId
	 * @return
	 * @throws SQLException
	 */
	protected List<SolrDocumentVO> getDocuments(List<String> documentIds) {
		StringBuilder sql = new StringBuilder(250);
		String customDb = config.getProperty(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select rs.rss_article_id, rs.article_txt, rs.title_txt, rs.affiliation_txt, rs.publish_dt, "); 
		sql.append("string_agg(rfa.feed_group_id+'~'+ case when rfa.complete_flg = 1 then 'C' else rfa.article_status_cd "); 
		sql.append("end +'~'+coalesce(rfa.bucket_id, ''), ',') as combo_key, string_agg(rfa.rss_article_filter_id, ',') as filter_id ");
		sql.append("from ").append(customDb).append("biomedgps_rss_article rs "); 
		sql.append("inner join ").append(customDb).append("biomedgps_rss_filtered_article rfa on rfa.rss_article_id = rs.rss_article_id "); 
		if (documentIds != null && !documentIds.isEmpty())
			sql.append("where rs.rss_article_id in (").append(DBUtil.preparedStatmentQuestion(documentIds.size())).append(") ");
		sql.append("group by rs.rss_article_id, rs.article_txt, rs.title_txt, rs.affiliation_txt ");
		
		DBProcessor db = new DBProcessor(dbConn);
		db.setGenerateExecutedSQL(true);
		List<Object> params;
		
		if (documentIds != null && !documentIds.isEmpty()) {
			params = new ArrayList<Object>(documentIds);
		} else {
			 params = Collections.emptyList();
		}
		
		List<SolrDocumentVO> results = db.executeSelect(sql.toString(),params, new RSSSolrDocumentVO());
		
		log.info(String.format("Loaded %d RSS Articles.", results.size()));
		return results;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#indexItems(java.lang.String[])
	 */
	@Override
	public void indexItems(String... itemIds) {
		log.debug("adding single Update: " + itemIds);
		SolrClient server = makeServer();
		try (SolrActionUtil util = new SolrActionUtil(server)) {
			util.addDocuments(getDocuments(loadArticleIds(Arrays.asList(itemIds))));
		} catch (Exception e) {
			log.error("Failed to index Update with id=" + itemIds, e);
		}
	}
}