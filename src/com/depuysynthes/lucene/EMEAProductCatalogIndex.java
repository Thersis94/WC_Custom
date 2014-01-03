package com.depuysynthes.lucene;

import java.sql.Connection;
import java.util.Properties;

import org.apache.lucene.index.IndexWriter;

import com.siliconmtn.cms.CMSConnection;

/****************************************************************************
 * <b>Title</b>: EMEAProductCatalogIndex.java<p/>
 * <b>Description: Overloads ProductCatalogIndex class to work for the EMEA org/catalogs. </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Aug 21, 2013
 ****************************************************************************/
public class EMEAProductCatalogIndex extends ProductCatalogIndex {

	public EMEAProductCatalogIndex() {
		super();
		ORGANIZATION_ID = "DPY_SYN_EMEA";
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.lucene.custom.SMTCustomIndexIntfc#addIndexItems(java.sql.Connection, com.siliconmtn.cms.CMSConnection, org.apache.lucene.index.IndexWriter)
	 */
	@Override
	public void addIndexItems(Connection conn, CMSConnection cmsConn, IndexWriter writer, Properties config) {
		log.info("Indexing DePuySynthes EMEA Products & Procedures");
		indexProducts(conn, "DS_PRODUCTS_EMEA", writer);
		indexProducts(conn, "DS_PROCEDURES_EMEA", writer);
	}
}
