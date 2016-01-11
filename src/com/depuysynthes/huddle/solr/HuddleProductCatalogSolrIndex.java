package com.depuysynthes.huddle.solr;

//JDK 1.6.x
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


//log4j 1.2-15
import org.apache.solr.client.solrj.impl.HttpSolrServer;


//SMT Base Libs
import com.depuysynthes.action.ProductCatalogUtil;
import com.depuysynthes.lucene.data.ProductCatalogSolrDocumentVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.pool.SMTDBConnection;

//WC Libs
import com.smt.sitebuilder.action.tools.StatVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SMTAbstractIndex;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.solr.SolrActionUtil;

/****************************************************************************
* <b>Title</b>: HuddleProductCatalogSolrIndex.java <p/>
* <b>Project</b>: WebCrescendo <p/>
* <b>Description: </b> This class gets invoked by the Solr Index Builder (batch)
* It gets all products from the supplied organization's catalog and places them into solr.
* <p/>
* <b>Copyright:</b> Copyright (c) 2016<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Eric Damschroder
* @version 1.0
* @since Jan 8, 2016<p/>
****************************************************************************/
public class HuddleProductCatalogSolrIndex extends SMTAbstractIndex {

	protected String organizationId = "DPY_SYN_HUDDLE";

	/**
	 * Index type for this index.  This value is stored in the INDEX_TYPE field
	 */
	public static String INDEX_TYPE = "PRODUCT";
	protected static String SOLR_DOC_CLASS = ProductCatalogSolrDocumentVO.class.getName();
	private static String CATALOG_ID = "c0a80241f53aa254e2a226f075d993a7";

	/**
	 * @param config
	 */
	public HuddleProductCatalogSolrIndex(Properties config) {
		super(config);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.lucene.custom.SMTCustomIndexIntfc#addIndexItems(java.sql.Connection, com.siliconmtn.cms.CMSConnection, org.apache.lucene.index.IndexWriter)
	 */
	@Override
	public void addIndexItems(HttpSolrServer server) {
		log.info("Indexing DePuySynthes Huddle Products");
		indexProducts(server, SOLR_DOC_CLASS, 50, INDEX_TYPE);
	}


	/**
	 * Flattens out the hierarchy and stores all fields in the content fields
	 * @param catalogId
	 * @param server
	 */
	protected void indexProducts(HttpSolrServer server, String solrDocClass, int dsOrderNo, String moduleType) {
		List<Node> nodes = getProductData(CATALOG_ID);
		log.info("Found " + nodes.size() + " nodes to index for " + CATALOG_ID + ".");

		SolrActionUtil solrUtil = new SolrActionUtil(server);
		ProductCatalogSolrDocumentVO solrDoc = null;
		List<String> hierarchy = null;
		Map<String, ProductCatalogSolrDocumentVO> docs = new HashMap<>();
		for (Node n : nodes) {
			ProductCategoryVO vo = (ProductCategoryVO)n.getUserObject();
			
			// Build the product Hierarchy
			if (n.getDepthLevel() == 1) {
				hierarchy = new ArrayList<>();
				hierarchy.add(vo.getCategoryName());
			} else {
				if (hierarchy.size() > n.getDepthLevel()-2) {
					for (int i = hierarchy.size()-1; i>=n.getDepthLevel()-2;i--) {
						hierarchy.remove(i);
					}
				}
				hierarchy.add(vo.getCategoryName());
			}
			
			if (vo.getProducts() == null || vo.getProducts().size() == 0) continue;
			// Remove the product from the hierarchy list.
			hierarchy.remove(hierarchy.size()-1);
			for (ProductVO pVo : vo.getProducts()) {
				try {
					if (docs.containsKey(pVo.getProductId())) {
						docs.get(pVo.getProductId()).addHierarchies(buildHierarchy(hierarchy));
					} else {
						solrDoc = new ProductCatalogSolrDocumentVO(INDEX_TYPE);
						solrDoc.setDocumentId(pVo.getProductId());
						solrDoc.setTitle(pVo.getTitle());
						solrDoc.setSummary(pVo.getDescText());
						solrDoc.setDetailImage(pVo.getImage());
						solrDoc.setDocumentUrl(pVo.getUrlAlias());
						solrDoc.addOrganization(organizationId);
						solrDoc.setModule(moduleType);
						solrDoc.setSpecialty(hierarchy.get(0));
						solrDoc.addRole(SecurityController.PUBLIC_ROLE_LEVEL);
						docs.put(pVo.getProductId(), solrDoc);
					}
					
				} catch (Exception e) {
					log.error(e);
				}
			}
		}
		for (String key : docs.keySet()) {
			try {
				solrUtil.addDocument(docs.get(key));
			} catch (ActionException e) {
				log.warn("Unable to add product: " + docs.get(key).getDocumentId());
			}
		}
	}


	private String buildHierarchy(List<String> hierarchy) {
		if (hierarchy == null) return "";
		StringBuilder fullHierarchy = new StringBuilder(100);
		for (String s : hierarchy) {
			fullHierarchy.append(s).append(SearchDocumentHandler.HIERARCHY_DELIMITER);
		}
		return fullHierarchy.substring(0, fullHierarchy.length()-1);
	}

	/**
	 * load the entire product catalog in the same way the public sites would.
	 * leverage the Util class to traverse and assign pageUrls for us.
	 * @param orgId
	 */
	private List<Node> getProductData(String catalogId) {
		ProductCatalogUtil util = new ProductCatalogUtil();
		util.setDBConnection(new SMTDBConnection(dbConn));
		Map<String, Object> attribs = new HashMap<String, Object>();
		attribs.put(Constants.MODULE_DATA, new ModuleVO());
		attribs.put(Constants.QS_PATH, config.get(Constants.QS_PATH));
		util.setAttributes(attribs);
		log.debug("loading product for catalogId=" + catalogId);
		Tree t = util.loadCatalog(catalogId, null, true, null);

		//let the util assign page URLs for us
		return util.assignPageviewsToCatalogUsingDivision(t.preorderList(), new HashMap<String, StatVO>(), "/hcp/", (catalogId.startsWith("DS_PRODUCTS")), true);
	}

	@Override
	public void purgeIndexItems(HttpSolrServer server) throws IOException {
		try {
			StringBuilder solrQuery = new StringBuilder(60);
			solrQuery.append(SearchDocumentHandler.INDEX_TYPE + ":" + getIndexType() + " AND ");
			solrQuery.append(SearchDocumentHandler.ORGANIZATION + ":" + organizationId);
			
			server.deleteByQuery(solrQuery.toString());
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public String getIndexType() {
		return INDEX_TYPE;
	}
}
