package com.depuysynthesinst;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrClient;

import com.depuysynthesinst.lms.CourseSolrTemplate;
import com.depuysynthesinst.lms.LMSCourseVO;
import com.depuysynthesinst.lms.LMSWSClient;
import com.siliconmtn.action.ActionException;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SMTAbstractIndex;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SolrActionUtil;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: TTLMSSolrIndexer.java<p/>
 * <b>Description:</b> indexes DSI eCourses stored in the 3rd-party LMS system.. 
 *  Makes a SOAP call to the LMS for a JSON document, and pushes the active LMS 
 *  courses into Solr.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 16, 2015
 ****************************************************************************/
public class TTLMSSolrIndexer extends SMTAbstractIndex {
	
	public static final String INDEX_TYPE = "LMS_DSI";
	public static final String SOLR_PREFIX = "LMS"; //used as prefix for documentId - for Solr
	
	public TTLMSSolrIndexer(Properties config) {
		this.config = config;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#addIndexItems(org.apache.solr.client.solrj.SolrClient)
	 */
	@SuppressWarnings("resource")
	@Override
	public void addIndexItems(SolrClient server) {
		//call the LMS and get a list of courses
		LMSWSClient wcClient = new LMSWSClient(config.getProperty(LMSWSClient.CFG_SECURITY_KEY));
		List<LMSCourseVO> courses = null;
		try {
			courses = wcClient.getCourseList();
			log.info("Loaded " + courses.size() + " from TTLMS");
		} catch (ActionException ae) {
			log.error("could not load course list", ae);
		}
		
		if (courses == null || courses.size() == 0) return;
		
		SolrActionUtil solrUtil = new SolrActionUtil(server);
		
		// for each result in our query, load the full document the same was WC does it
		//  this allows us to reuse all the same code and annotations.
		for (LMSCourseVO course : courses) {
			if (!course.isLive()) continue; //skip courses that shouldn't be live yet
			if (course.getC_ID() <=0) { //can't put course in solr without a unique documentId
				log.error("course has no primary key: " + course);
				continue;
			}
			
			course.setQsPath(config.getProperty(Constants.QS_PATH)); //need this so we can derrive hierarchy in the VO

			try {
				SolrDocumentVO solrDoc = new CourseSolrTemplate();
				solrDoc.setData(course);
				log.debug("adding to Solr: " + solrDoc.getDocumentId());
				solrUtil.addDocument(solrDoc);
			} catch (Exception e) {
				log.error("could not create document to add to Solr", e);
			}
			log.info("added course " + course.getC_NAME());
		}

	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#purgeIndexItems(org.apache.solr.client.solrj.SolrClient)
	 */
	@Override
	public void purgeIndexItems(SolrClient server) throws IOException {
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
		return false;
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTAbstractIndex#getIndexType()
	 */
	@Override
	public String getIndexType() {
		return INDEX_TYPE;
	}

	
	@Override
	public void addSingleItem(String arg0) {
		// This class is only used in the mass indexer and is never used
		// in an area where precision updates are needed.
	}
}