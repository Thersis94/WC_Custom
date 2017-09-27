package com.depuysynthesinst;

// Java 8
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

// Solr 5.5
import org.apache.solr.common.SolrDocument;

import com.depuysynthes.solr.MediaBinSolrIndex;
// SMT / WC
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.security.UserRoleVO;
import com.smt.sitebuilder.action.menu.MenuObj;
import com.smt.sitebuilder.action.menu.SitemapGenerator;
import com.smt.sitebuilder.action.search.SolrActionIndexVO;
import com.smt.sitebuilder.action.search.SolrActionVO;
import com.smt.sitebuilder.action.search.SolrQueryProcessor;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.search.solr.DocumentSolrIndexer;
import com.smt.sitebuilder.security.SecurityController;


/*****************************************************************************
<p><b>Title</b>: DSISitemapGenerator</p>
<p>Extends the WC core sitemap loader with DSI-specific features.</p>
@author James McKain
@version 1.0
@since Sep 19, 2017
Code Updates
 ***************************************************************************/
public class DSISitemapGenerator extends SitemapGenerator {

	protected String qsPath;


	public DSISitemapGenerator(Map<String, Object> attributes, SMTDBConnection dbConn) {
		super(attributes, dbConn);
		qsPath = (String) attributes.get(Constants.QS_PATH);
	}


	/**
	 * overload the Template Method Pattern of the superclass to add some custom DePuy pages to the sitemap.
	 */
	@Override
	public List<Node> loadCustomLowPriPages(ActionRequest req, SiteVO site, UserRoleVO role) {
		return loadDSISolrAssets(site);
	}


	/**
	 * loads a list of assets for DSI, out of Solr.
	 * Then applies business logic to determine the pageURLs for each asset, based on hierarchy.
	 * @param site
	 * @param req
	 * @return
	 */
	protected List<Node> loadDSISolrAssets(SiteVO site) {
		String solrCollectionPath = (String) attributes.get(Constants.SOLR_COLLECTION_NAME);
		List<Node> data = new ArrayList<>();
		SolrSearchWrapper solrWrapper = new SolrSearchWrapper();
		solrWrapper.setAttributes(attributes);

		SolrActionVO qData = new SolrActionVO();
		qData.setNumberResponses(20000);
		qData.setOrganizationId(site.getOrganizationId()); //DPY_SYN_INST only
		qData.setRoleLevel(SecurityController.PUBLIC_ROLE_LEVEL); //public assets only
		qData.addIndexType(new SolrActionIndexVO(SearchDocumentHandler.INDEX_TYPE, MediaBinSolrIndex.INDEX_TYPE));
		qData.addIndexType(new SolrActionIndexVO(SearchDocumentHandler.INDEX_TYPE, DocumentSolrIndexer.INDEX_TYPE));
		SolrQueryProcessor sqp = new SolrQueryProcessor(attributes, solrCollectionPath);
		SolrResponseVO resp = sqp.processQuery(qData);

		if (resp == null || resp.getTotalResponses() == 0) return data;

		for (SolrDocument sd : resp.getResultDocuments()) {
			try {
				MenuObj mo = new MenuObj();
				mo.setFullPath(solrWrapper.buildDSIUrl(sd));
				if (mo.getFullPath() == null) continue; //asset does not have a valid DSI url and should not be promoted

				mo.setLastModified((Date)sd.getFieldValue(SearchDocumentHandler.UPDATE_DATE));
				mo.setFileExtension("");
				mo.setContextName(contextPath);

				Node n = new Node();
				n.setUserObject(mo);
				data.add(n);

			} catch(Exception e) {
				log.error("Unable to make URL for Solr asset", e);
			}
		}

		log.debug("size=" + data.size());
		return data;
	}
}
