package com.depuysynthes.pa;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;

import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.search.SMTAbstractIndex;
import com.smt.sitebuilder.search.SearchDocumentHandler;

/****************************************************************************
 * <b>Title</b>: StorySolrIndexer.java<p/>
 * <b>Description: Can be called to rebuild the entirety of the Depuy Ambassador Story index
 * or can be used to add a single item to that index.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Jan 5, 2015
 ****************************************************************************/

public class SolrStoryIndexer extends SMTAbstractIndex {
	
	/**
	 * Index type for this index.  This value is stored in the INDEX_TYPE field
	 */
	public static final String INDEX_TYPE = "PATIENT_AMBASSADOR";
	
	/**
	 * @param config
	 */
	public SolrStoryIndexer(Properties config) {
		super(config);
	}

	public SolrStoryIndexer() {
		super();
	}

	/**
	 * Add a single SolrStoryVO to the index
	 * @param server
	 * @param vo
	 */
	public void addSingleItem(HttpSolrServer server, SolrStoryVO vo) {
		log.debug("Indexing new Patient Story");
		SolrInputDocument doc = new SolrInputDocument();
		
		try {
			setFields(doc, vo);
			
			server.add(doc);
			server.commit();
		} catch (Exception e) {
			log.error("Unable to index course: " + StringUtil.getToString(vo), e);
		}
	}
	
	/**
	 * Removes a single item from the index due to a story being deleted
	 * @param server
	 * @param documentId
	 */
	public void removeSingleItem (HttpSolrServer server, String documentId) {
		log.debug("removing Document: " + documentId);
		try {
			server.deleteById(documentId);
			server.commit();
		} catch (Exception e) {
			log.error("Unable to delete story with document id of " + documentId, e);
		}
	}
	
	/**
	 * Set the fields for the solr document based on the SolrStoryVO we have been given
	 * @param doc
	 * @param vo
	 */
	private void setFields(SolrInputDocument doc, SolrStoryVO vo) {
		doc.setField(SearchDocumentHandler.INDEX_TYPE, INDEX_TYPE);
		doc.setField(SearchDocumentHandler.ORGANIZATION, vo.getOrganizationId());
		doc.setField(SearchDocumentHandler.LANGUAGE, "en");
		doc.setField(SearchDocumentHandler.ROLE, 0);
		doc.setField(SearchDocumentHandler.TITLE, vo.getTitle());
		doc.setField(SearchDocumentHandler.DOCUMENT_ID, vo.getStoryId());
		doc.setField(SearchDocumentHandler.AUTHOR, vo.getAuthor());
		doc.setField(SearchDocumentHandler.SUMMARY, vo.getContent());
		doc.setField(SearchDocumentHandler.MODULE_TYPE, "PATIENT_AMBASSADOR");
		doc.setField(SearchDocumentHandler.DETAIL_IMAGE, vo.getDetailImage());
		doc.setField(SearchDocumentHandler.STATE, vo.getState());
		doc.setField(SearchDocumentHandler.CITY, vo.getCity());
		doc.setField(SearchDocumentHandler.ZIP, vo.getZip());
		doc.setField(SearchDocumentHandler.UPDATE_DATE, Convert.getCurrentTimestamp());
		doc.setField("lat_coordinate", Convert.formatDouble(vo.getLat()));
		doc.setField("lng_coordinate", Convert.formatDouble(vo.getLng()));
		
		
		for (String hobby : vo.getHobbies())
			doc.addField(SearchDocumentHandler.CATEGORY, hobby);
		
		for (String joint : vo.getJoints())
			doc.addField(SearchDocumentHandler.HIERARCHY, joint);
		
		// If we have multiple joints we set this to prevent them from being used in single joint lists
		if (vo.getJoints().size() > 1) {
			doc.addField("multijoint_s", "multijoint");
		} else {
			doc.addField("multijoint_s", "single");
		}
		
		// If we have a 'Other' hobby we add that and the Other type to the hobbies field
		if (vo.getOtherHobbies() != null) {
			doc.addField("category_other_s", vo.getOtherHobbies());
			doc.addField(SearchDocumentHandler.CATEGORY, "Other");
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#addIndexItems(org.apache.solr.client.solrj.impl.HttpSolrServer)
	 */
	@Override
	public void addIndexItems(HttpSolrServer server) {
		log.debug("Indexing Patient Stories");
		List<SolrStoryVO> data = this.indexEvents(dbConn);
		indexStories(server, data);
	}

	/**
	 * Index all stories as pulled from the database
	 * @param server
	 * @param data
	 */
	protected void indexStories(HttpSolrServer server, List<SolrStoryVO> data) {
		int cnt = 0;
		for (SolrStoryVO vo : data) {
			SolrInputDocument doc = new SolrInputDocument();
			
			try {
				setFields(doc, vo);
				
				server.add(doc);
				++cnt;
				if ((cnt % 100) == 0) {
					log.info("Committed " + cnt + " records");
					server.commit();
				}
			} catch (Exception e) {
				log.error("Unable to index course: " + StringUtil.getToString(vo), e);
			}
		}

		// Clean up any uncommitted files
		try {
			server.commit();
		} catch (Exception e) {
			log.error("Unable to commit remaining documents", e);
		}
	}
	
	
	/**
	 * Get the list of stories from the database in order to rebuild the index
	 * @param conn
	 * @return
	 */
	private List<SolrStoryVO> indexEvents(Connection conn) {
		List<SolrStoryVO> data = new ArrayList<SolrStoryVO>();
		// TODO After storage of stories is finalized this function can be completed
		
		return data;
	}

	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTAbstractIndex#getIndexType()
	 */
	@Override
	public String getIndexType() {
		return INDEX_TYPE;
	}

}