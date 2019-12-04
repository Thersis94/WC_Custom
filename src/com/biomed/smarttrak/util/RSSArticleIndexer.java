package com.biomed.smarttrak.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrClient;

import com.biomed.smarttrak.action.rss.NewsroomAction;
import com.biomed.smarttrak.action.rss.RSSGroupAction;
import com.biomed.smarttrak.action.rss.vo.RSSFeedGroupVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.common.constants.Constants;
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

	//Constants and Defaults
	public static final String INDEX_TYPE = "BIOMEDGPS_FEED";
	private static final String BATCH_SIZE = "batchSize";
	private static final String MEMORY_LIMIT = "memoryLimit";
	private static final int BATCH_DEFAULT = 5000;
	private static final int MEMORY_DEFAULT = 75000000;
	private final String articleSql;

	//Member variables.
	private int articleLimit;
	private int memoryLimit;

	public RSSArticleIndexer(Properties config) {
		super(config);
		articleLimit = Convert.formatInteger(config.getProperty(BATCH_SIZE), BATCH_DEFAULT);
		memoryLimit = Convert.formatInteger(config.getProperty(MEMORY_LIMIT), MEMORY_DEFAULT);
		articleSql = buildSql();
	}

	/**
	 * Build the articleSQL String once and re-use it.
	 */
	private final String buildSql() {
		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(250);
		sql.append(DBUtil.SELECT_CLAUSE).append("rss_article_filter_id, a.bytes_no ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema);
		sql.append("biomedgps_rss_filtered_article brfa ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_rss_article a ");
		sql.append("on brfa.rss_article_id = a.rss_article_id ");
		sql.append(DBUtil.WHERE_CLAUSE).append("feed_group_id = ? ");
		sql.append(DBUtil.ORDER_BY).append("a.bytes_no asc");
		return sql.toString();
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
			RSSGroupAction ga = new RSSGroupAction();
			ga.setDBConnection(new SMTDBConnection(dbConn));
			ga.setAttributes(getAttributes());
			Iterator<RSSFeedGroupVO> gIter = ga.loadGroupXrs(null, null, null).iterator();
			List<String> tempIds;
			Map<String, Integer> ids;
			RSSFeedGroupVO g;
			while(gIter.hasNext()) {
				g = gIter.next();

				//Get Id Iterator
				ids = loadFilteredArticleIds(g.getFeedGroupId());
				while(!ids.isEmpty()) {
					tempIds = processData(ids);
					util.addDocuments(getDocuments(g.getFeedGroupId(), tempIds));
					ids.keySet().removeAll(tempIds);
				}
				gIter.remove();
			}
		} catch (Exception e) {
			log.error("Failed to index Updates", e);
		}
	}

	/**
	 * Processing Articles using Data size counter.  Attempt to prevent OOM Errors.
	 * @param ids
	 * @return
	 */
	private List<String> processData(Map<String, Integer> ids) {
		List<String> tempIds = new ArrayList<>();
		int dataSize = 0;
		for(Entry<String, Integer> id : ids.entrySet()) {

			//If we have proper number of companies, perform full lookup and send to Solr.
			if((!tempIds.isEmpty() && dataSize + id.getValue() > memoryLimit) || tempIds.size() == articleLimit) {
				log.info(String.format("Returning with dataSize: %d", dataSize));
				return tempIds;
			}
			tempIds.add(id.getKey());
			dataSize += id.getValue();
		}
		return tempIds;
	}

	/**
	 * Load all article Ids for a given feedGroupId
	 * @param feedGroupId
	 * @return
	 */
	private Map<String, Integer> loadFilteredArticleIds(String feedGroupId) {

		Map<String, Integer> ids = new HashMap<>();
		try(PreparedStatement ps = dbConn.prepareStatement(articleSql)) {
			ps.setString(1, feedGroupId);
			ResultSet rs = ps.executeQuery();
			while(rs.next() ) {
				ids.put(rs.getString("rss_article_filter_id"), rs.getInt("bytes_no"));
			}
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
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
	 * @param documentId
	 * @return
	 * @throws SQLException
	 */
	protected List<SolrDocumentVO> getDocuments(String feedGroupId, List<String> documentIds) {
		NewsroomAction na = new NewsroomAction();
		na.setDBConnection(new SMTDBConnection(dbConn));
		na.setAttributes(getAttributes());
		List<SolrDocumentVO> vos = na.loadAllArticles(feedGroupId, documentIds);
		log.info(String.format("Loaded %d RSS Articles.", vos.size()));
		return vos;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#indexItems(java.lang.String[])
	 */
	@Override
	public void indexItems(String... itemIds) {
		log.debug("adding single Update: " + itemIds);
		SolrClient server = makeServer();
		try (SolrActionUtil util = new SolrActionUtil(server)) {
				util.addDocuments(getDocuments(null, Arrays.asList(itemIds)));
		} catch (Exception e) {
			log.error("Failed to index Update with id=" + itemIds, e);
		}
	}
}