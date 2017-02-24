/**
 *
 */
package com.biomed.smarttrak.solr;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;

import com.biomed.smarttrak.admin.InsightAction;
import com.biomed.smarttrak.vo.InsightVO;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SMTAbstractIndex;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SolrActionUtil;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: BiomedInsightIndexer.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Biomed Insight Solr Indexer
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Ryan Riker
 * @version 1.0
 * @since Feb 16, 2017
 ****************************************************************************/
public class BiomedInsightIndexer extends SMTAbstractIndex {
	public static final String INDEX_TYPE = "BIOMED_INSIGHT";

	public BiomedInsightIndexer(Properties config) {
		super(config);
	}
	
	public static BiomedInsightIndexer makeInstance(Map<String, Object> attributes) {
		Properties props = new Properties();
		props.putAll(attributes);
		return new BiomedInsightIndexer(props);
	}

	@Override
	public void addIndexItems(SolrClient server) throws SolrException {
		try (SolrActionUtil util = new SolrActionUtil(makeServer())) {
			List<SolrDocumentVO> docs = getDocuments(null);
			if (docs.isEmpty()) {
				throw new Exception("No Documents found");
			}
			util.addDocuments(docs);
			util.commitSolr();
		} catch (Exception e) {
			throw new SolrException(ErrorCode.BAD_REQUEST, e);
		}
	}

	@Override
	public void addSingleItem(String itemId) throws SolrException {
		log.debug("Adding single item.");
		try (SolrActionUtil util = new SolrActionUtil(makeServer())) {
			List<SolrDocumentVO> docs = getDocuments(itemId);
			if (docs.isEmpty()) {
				throw new Exception("Document " + itemId + " not found");
			}
			util.addDocuments(docs);
			util.commitSolr();
		} catch (Exception e) {
			throw new SolrException(ErrorCode.BAD_REQUEST, e);
		}
	}

	@Override
	public void purgeIndexItems(SolrClient server) throws IOException {
		try {
			server.deleteByQuery(SearchDocumentHandler.INDEX_TYPE + ":" + getIndexType());
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public String getIndexType() {
		return INDEX_TYPE;
	}

	@SuppressWarnings("unchecked")
	private List<SolrDocumentVO> getDocuments(String documentId) throws SQLException {
		InsightAction ia = new InsightAction();
		ia.setDBConnection(new SMTDBConnection(this.dbConn));
		ia.setAttribute(Constants.CUSTOM_DB_SCHEMA, config.getProperty(Constants.CUSTOM_DB_SCHEMA));
		ia.setAttribute(Constants.QS_PATH, config.getProperty(Constants.QS_PATH));
		List<Object> list = ia.getInsights(documentId, null, null, null);

		//Load the Section Tree and set all the Hierarchies.
		Tree t = ia.loadSections();
		for(Object o : list) {
			InsightVO u = (InsightVO)o;
			u.setHierarchies(t);
		}
		return(List<SolrDocumentVO>)(List<?>) list;
	}
}