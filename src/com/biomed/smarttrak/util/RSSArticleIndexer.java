package com.biomed.smarttrak.util;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrClient;

import com.biomed.smarttrak.action.rss.NewsroomAction;
import com.biomed.smarttrak.action.rss.RSSGroupAction;
import com.biomed.smarttrak.action.rss.vo.RSSFeedGroupVO;
import com.siliconmtn.db.pool.SMTDBConnection;
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
			RSSGroupAction ga = new RSSGroupAction();
			ga.setDBConnection(new SMTDBConnection(dbConn));
			ga.setAttributes(getAttributes());
			List<RSSFeedGroupVO> groups = ga.loadGroupXrs(null, null, null);

			for(RSSFeedGroupVO g : groups) {
				log.info(String.format("Processing Feed Group %s.", g.getFeedGroupId()));
				util.addDocuments(getDocuments(g.getFeedGroupId(), null));
			}
		} catch (Exception e) {
			log.error("Failed to index Updates", e);
		}
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