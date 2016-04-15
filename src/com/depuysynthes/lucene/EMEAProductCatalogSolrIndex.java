package com.depuysynthes.lucene;

import java.util.Properties;

import org.apache.solr.client.solrj.impl.CloudSolrClient;

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

	@Override
	public void addIndexItems(CloudSolrClient server) {
		log.info("Indexing DePuySynthes EMEA Products & Procedures");
		indexProducts("DS_PRODUCTS_EMEA", server, SOLR_DOC_CLASS, 50, "DS_PRODUCT");
		indexProducts("DS_PROCEDURES_EMEA", server, SOLR_DOC_CLASS, 45, "DS_PROCEDURE");
	}

}