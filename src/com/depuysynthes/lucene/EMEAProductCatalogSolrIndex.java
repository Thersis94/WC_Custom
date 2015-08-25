package com.depuysynthes.lucene;

import java.util.Properties;

import org.apache.solr.client.solrj.impl.HttpSolrServer;

/****************************************************************************
 * <b>Title</b>: EMEAProductCatalogSolrIndex.java<p/>
 * <b>Description: Overloads ProductCatalogSolrIndex class to work for the EMEA org/catalogs. </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Aug 21, 2013
 * @updates:
 * TJ 08/25/15 
 * 		Copied the file and modified for the Solr Indexer
 ****************************************************************************/
public class EMEAProductCatalogSolrIndex extends ProductCatalogSolrIndex {

	/**
	 * @param config
	 */
	public EMEAProductCatalogSolrIndex(Properties config) {
		super(config);
		organizationId = "DPY_SYN_EMEA";
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.lucene.custom.SMTCustomIndexIntfc#addIndexItems(java.sql.Connection, com.siliconmtn.cms.CMSConnection, org.apache.lucene.index.IndexWriter)
	 */
	@Override
	public void addIndexItems(HttpSolrServer server) {
		log.info("Indexing DePuySynthes EMEA Products & Procedures");
		indexProducts("DS_PRODUCTS_EMEA", server);
		indexProducts("DS_PROCEDURES_EMEA", server);
	}
}
