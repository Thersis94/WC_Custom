package com.biomed.smarttrak.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
// Java 8
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

// Solr 5.5
import org.apache.solr.client.solrj.SolrClient;

//WC Custom
import com.biomed.smarttrak.admin.UpdatesAction;
import com.biomed.smarttrak.vo.UpdateVO;
// SMT base libs
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;
// WC Core
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
	private static final int BATCH_SIZE = 10000;
	public UpdateIndexer(Properties config) {
		super(config);
	}

	public static UpdateIndexer makeInstance(Map<String, Object> attributes) {
		return new UpdateIndexer(makeProperties(attributes));
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
		List<String> tempIds = new ArrayList<>();
		int i = 0;
		try {
			List<String> docIds = loadUpdateIds();
			Iterator<String> docIter = docIds.iterator();

			while(docIter.hasNext()) {

				//If we have proper number of companies, perform full lookup and send to Solr.
				if(i > 0 && i % BATCH_SIZE == 0) {
					util.addDocuments(getDocuments(tempIds.toArray(new String [tempIds.size()])));
					log.info(String.format("Processed %d updates of %d", tempIds.size(), i));
					tempIds.clear();
				}
				tempIds.add(docIter.next());
				docIter.remove();
				i++;
			}
			util.addDocuments(getDocuments(tempIds.toArray(new String [tempIds.size()])));
			log.info(String.format("Processed %d updates of %d", tempIds.size(), i));
		} catch (Exception e) {
			log.error("Failed to index Updates", e);
		}
	}


	/**
	 * Load a List of all Update Ids
	 * @return
	 * @throws SQLException
	 */
	private List<String> loadUpdateIds() throws SQLException {
		String sql = StringUtil.join("select update_id from ", getAttributes().get(Constants.CUSTOM_DB_SCHEMA).toString(), "biomedgps_update");
		List<String> updateIds = new ArrayList<>();
		try(PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				updateIds.add(rs.getString("UPDATE_ID"));
			}
		}

		return updateIds;
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
	@SuppressWarnings("unchecked")
	private List<SolrDocumentVO> getDocuments(String... documentIds) {
		UpdatesAction ua = new UpdatesAction();
		ua.setDBConnection(new SMTDBConnection(dbConn));
		ua.setAttributes(getAttributes());
		List<UpdateVO> list = ua.getAllUpdates(documentIds);

		//Load the Section Tree and set all the Hierarchies.
		SmarttrakTree tree = ua.loadSections();

		//attach the section tree to each Update retrieved.  This will internally bind 'selected sections'
		for (Object obj : list) {
			UpdateVO vo = (UpdateVO) obj;
			vo.configureSolrHierarchies(tree);
		}

		return(List<SolrDocumentVO>)(List<?>) list;
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#indexItems(java.lang.String[])
	 */
	@Override
	public void indexItems(String... itemIds) {
		log.debug("adding single Update: " + itemIds);
		SolrClient server = makeServer();
		try (SolrActionUtil util = new SmarttrakSolrUtil(server)) {
			util.addDocuments(getDocuments(itemIds));

		} catch (Exception e) {
			log.error("Failed to index Update with id=" + itemIds, e);
		}
	}
}