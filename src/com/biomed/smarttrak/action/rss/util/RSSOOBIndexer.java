package com.biomed.smarttrak.action.rss.util;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrClient;

import com.biomed.smarttrak.util.RSSArticleIndexer;
import com.smt.sitebuilder.search.SMTAbstractIndex;

/****************************************************************************
 * <b>Title:</b> RSSOOBSolrIndexer.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Solr Indexer for Feeds Articles processed in the OOB Scripts.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Mar 27, 2019
 ****************************************************************************/
public class RSSOOBIndexer extends SMTAbstractIndex {

	public RSSOOBIndexer(Properties config) {
		super(config);
	}

	public static RSSOOBIndexer makeInstance(Map<String, Object> attributes) {
		return new RSSOOBIndexer(makeProperties(attributes));
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#addIndexItems(org.apache.solr.client.solrj.SolrClient)
	 */
	@Override
	public void addIndexItems(SolrClient server) {
		
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#indexItems(java.lang.String[])
	 */
	@Override
	public void indexItems(String... itemIds) {
		
	}

	@Override
	public void purgeIndexItems(SolrClient server) throws IOException {
		
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTAbstractIndex#getIndexType()
	 */
	@Override
	public String getIndexType() {
		return RSSArticleIndexer.INDEX_TYPE;
	}

}
