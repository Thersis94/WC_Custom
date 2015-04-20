package com.depuysynthesinst;

import java.io.IOException;
import java.util.Properties;

import org.apache.solr.client.solrj.impl.HttpSolrServer;

import com.quickstream.doccentral.storage.DocSearchDataObject;
import com.quickstream.doccentral.storage.DocSearchDataObjectContainer;
import com.siliconmtn.cms.QSSimpleSearch;
import com.siliconmtn.exception.CMSException;
import com.smt.sitebuilder.action.cms.CMSContentVO;
import com.smt.sitebuilder.action.cms.DocumentAction;
import com.smt.sitebuilder.search.SMTAbstractIndex;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SolrActionUtil;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: QuickstreamSolrIndexer.java<p/>
 * <b>Description: indexes DSI assets stored in Quickstream. 
 * Note: changes made in the admintool hit Solr realtime.  This code is only hooked 
 * into the SolrIndexBuilder script for full rebuilds...rarely if-ever needed.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Mar 3, 2015
 ****************************************************************************/
public class QuickstreamSolrIndexer extends SMTAbstractIndex {
	
	public static final String INDEX_TYPE = "QUICKSTREAM_DSI";
	private static final String ORG_ID = "DPY_SYN_INST"; //in CMS and WC, must match.

	public QuickstreamSolrIndexer(Properties config) {
		this.config = config;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#addIndexItems(org.apache.solr.client.solrj.impl.HttpSolrServer)
	 */
	@Override
	public void addIndexItems(HttpSolrServer server) {
		//get a list of CMS documents for our org
		DocumentAction da = new DocumentAction(); //use the WC core to load the articles
		da.setCMSConnection(cmsConn);
		QSSimpleSearch qss = new QSSimpleSearch();
		DocSearchDataObjectContainer objs = null;
		try {
			objs = qss.fileSearch(cmsConn, ORG_ID, null); //grab all DSI files
		} catch (CMSException e) {
			log.error("could not load CMS documents", e);
		}
		if (objs == null) return;
		log.info("cms assets found (incl folders): " + objs.count());
		
		SolrActionUtil solrUtil = new SolrActionUtil(server);
		
		// for each result in our query, load the full document the same was WC does it
		//  this allows us to reuse all the same code and annotations.
		for (int x =0; x < objs.count(); x++) {
			DocSearchDataObject obj = objs.get(x);
			String docId = obj.getID();
			if (obj.isFolder()) continue;
			
			log.info("loading docId: " + docId);
			CMSContentVO vo = null;
			try {
				vo = da.loadDocument(docId, null, false);
			} catch (Exception e) {
				log.warn("could not load CMS document", e);
				continue;
			}
			
			try {
				SolrDocumentVO solrDoc = SolrActionUtil.newInstance("com.depuysynthesinst.QuickstreamTemplate");
				solrDoc.setData(vo);
				solrDoc.addOrganization(ORG_ID);
				solrDoc.setDocumentId(DocumentAction.SOLR_PREFIX + docId);
				log.debug("adding to Solr: " + solrDoc.toString());
				solrUtil.addDocument(solrDoc);
			} catch (Exception e) {
				log.error("could not create document to add to Solr", e);
			}
		}

	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#purgeIndexItems(org.apache.solr.client.solrj.impl.HttpSolrServer)
	 */
	@Override
	public void purgeIndexItems(HttpSolrServer server) throws IOException {
		try {
			server.deleteByQuery(SearchDocumentHandler.INDEX_TYPE + ":" + getIndexType());
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#isDBConnection()
	 */
	@Override
	public boolean isDBConnection() {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#isCMSConnection()
	 */
	@Override
	public boolean isCMSConnection() {
		return true;
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTAbstractIndex#getIndexType()
	 */
	@Override
	public String getIndexType() {
		return INDEX_TYPE;
	}
}