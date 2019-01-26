package com.biomed.smarttrak.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;

import com.biomed.smarttrak.admin.InsightAction;
import com.biomed.smarttrak.vo.InsightVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.smt.sitebuilder.search.SMTAbstractIndex;
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
		return new BiomedInsightIndexer(makeProperties(attributes));
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#addIndexItems(org.apache.solr.client.solrj.SolrClient)
	 */
	@SuppressWarnings("resource")
	@Override
	public void addIndexItems(SolrClient server) {
		// Never place this in a try with resources.
		// This server was given to this method and it is not this method's
		// job or right to close it.
		SolrActionUtil util = new SmarttrakSolrUtil(server);
		try {
			util.addDocuments(getDocuments());
		} catch (Exception e) {
			throw new SolrException(ErrorCode.BAD_REQUEST, e);
		}
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#addSingleItem(java.lang.String)
	 */
	@Override
	public void indexItems(String ...itemIds) {
		log.debug("Adding single insight: " + itemIds);
		SolrClient server = makeServer();
		try (SolrActionUtil util = new SmarttrakSolrUtil(server)) {
			util.addDocuments(getDocuments(itemIds));

			server.commit(false, false); //commit, but don't wait for Solr to acknowledge

		} catch (Exception e) {
			throw new SolrException(ErrorCode.BAD_REQUEST, e);
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


	@SuppressWarnings("unchecked")
	private List<SolrDocumentVO> getDocuments(String... documentIds) {
		InsightAction ia = new InsightAction();
		ia.setDBConnection(new SMTDBConnection(dbConn));
		Map<String, Object> attributes = new HashMap<>();
		for (final String name: config.stringPropertyNames())
			attributes.put(name, config.getProperty(name));
		ia.setAttributes(attributes);
		List<InsightVO> list = ia.loadForSolr(documentIds);

		//Load the Section Tree and set all the Hierarchies.
		SmarttrakTree t = ia.loadDefaultTree();
		for(InsightVO i : list) {
			i.configureSolrHierarchies(t);
		}
		return(List<SolrDocumentVO>)(List<?>) list;
	}
}