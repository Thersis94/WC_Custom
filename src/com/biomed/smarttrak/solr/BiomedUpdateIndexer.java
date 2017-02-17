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

import com.biomed.smarttrak.admin.UpdatesAction;
import com.biomed.smarttrak.vo.UpdatesVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SMTAbstractIndex;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SolrActionUtil;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: BiomedUpdateIndexer.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Biomed Updates Solr Indexer
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Feb 16, 2017
 ****************************************************************************/
public class BiomedUpdateIndexer extends SMTAbstractIndex {

	public BiomedUpdateIndexer(Properties config) {
		super(config);
	}
	
	public static BiomedUpdateIndexer makeInstance(Map<String, Object> attributes) {
		Properties props = new Properties();
		props.putAll(attributes);
		return new BiomedUpdateIndexer(props);
	}

	@Override
	public void addIndexItems(SolrClient server) throws SolrException {
		try (SolrActionUtil util = new SolrActionUtil(makeServer())) {
			List<SolrDocumentVO> docs = getDocuments(null);
			if (docs.size() < 1) {
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
		return UpdatesVO.INDEX_TYPE;
	}

	@SuppressWarnings("unchecked")
	private List<SolrDocumentVO> getDocuments(String documentId) throws SQLException {
		UpdatesAction ua = new UpdatesAction();
		ua.setDBConnection(new SMTDBConnection(this.dbConn));
		ua.setAttribute(Constants.CUSTOM_DB_SCHEMA, config.getProperty(Constants.CUSTOM_DB_SCHEMA));
		ua.setAttribute(Constants.QS_PATH, config.getProperty(Constants.QS_PATH));
		return (List<SolrDocumentVO>)(List<?>) ua.getUpdates(documentId);
	}
}