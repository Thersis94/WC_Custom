package com.biomed.smarttrak.util;

// Java 8
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;


// Solr 5.5
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;


//WC Custom
import com.biomed.smarttrak.admin.UpdatesAction;
import com.biomed.smarttrak.vo.UpdatesVO;

// SMT base libs
import com.siliconmtn.db.pool.SMTDBConnection;

// WC Core
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SMTAbstractIndex;
import com.smt.sitebuilder.util.solr.SolrActionUtil;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: UpdateIndexer.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Biomed Updates Solr Indexer
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Feb 16, 2017
 ****************************************************************************/
public class UpdateIndexer extends SMTAbstractIndex {

	public static final String INDEX_TYPE = "BIOMED_UPDATE";

	public UpdateIndexer(Properties config) {
		super(config);
	}

	public static UpdateIndexer makeInstance(Map<String, Object> attributes) {
		Properties props = new Properties();
		props.putAll(attributes);
		return new UpdateIndexer(props);
	}


	@Override
	@SuppressWarnings("resource")
	public void addIndexItems(SolrClient server) throws SolrException {
		try {
			SolrActionUtil util = new SmarttrakSolrUtil(server);
			List<SolrDocumentVO> docs = getDocuments(null);
			if (docs.isEmpty())
				throw new Exception("No Documents found");

			util.addDocuments(docs);
		} catch (Exception e) {
			throw new SolrException(ErrorCode.BAD_REQUEST, e);
		}
	}


	@Override
	public void addSingleItem(String itemId) throws SolrException {
		log.debug("adding single Update: " + itemId);
		try (SolrActionUtil util = new SmarttrakSolrUtil(makeServer())){
			List<SolrDocumentVO> docs = getDocuments(itemId);
			if (docs.isEmpty())
				throw new Exception("Update not found: " + itemId);

			util.addDocuments(docs);
		} catch (Exception e) {
			throw new SolrException(ErrorCode.BAD_REQUEST, e);
		}
	}


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
	@SuppressWarnings("unchecked")
	private List<SolrDocumentVO> getDocuments(String documentId) throws SQLException {
		UpdatesAction ua = new UpdatesAction();
		ua.setDBConnection(new SMTDBConnection(dbConn));
		ua.setAttribute(Constants.CUSTOM_DB_SCHEMA, config.getProperty(Constants.CUSTOM_DB_SCHEMA));
		ua.setAttribute(Constants.QS_PATH, config.getProperty(Constants.QS_PATH));
		List<Object> list = ua.getUpdates(documentId, null, null, null);

		//Load the Section Tree and set all the Hierarchies.
		SmarttrakTree tree = ua.loadSections();

		//attach the section tree to each Update retrieved.  This will internally bind 'selected sections'
		for(Object obj : list) {
			UpdatesVO vo = (UpdatesVO) obj;
			vo.configureSolrHierarchies(tree);
		}

		return(List<SolrDocumentVO>)(List<?>) list;
	}
}