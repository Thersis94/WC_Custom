package com.depuysynthes.huddle.solr;

//JDK 1.7
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

//Solr libs
import org.apache.solr.client.solrj.impl.HttpSolrServer;

// WC libs
import com.siliconmtn.commerce.catalog.ProductAttributeContainer;
import com.siliconmtn.commerce.catalog.ProductAttributeVO;
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.pool.SMTDBConnection;

//WC Libs
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SMTAbstractIndex;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.solr.SolrActionUtil;

// WC Custom libs
import com.depuysynthes.action.ProductCatalogUtil;
import com.depuysynthes.huddle.HuddleUtils;
import com.depuysynthes.lucene.data.ProductCatalogSolrDocumentVO;


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
	public static final String INDEX_TYPE = "PRODUCT";
	
	private Map<String, ProductCatalogSolrDocumentVO> products = new HashMap<>(1500);
	private Map<Integer, String> hierarchy = new HashMap<>();

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
		log.info("Indexing DSHuddle Products");
		indexProducts(server);
	}


	/**
	 * Flattens out the hierarchy and stores all fields in the content fields
	 * @param catalogId
	 * @param server
	 */
	@SuppressWarnings("unchecked")
	protected void indexProducts(HttpSolrServer server) {
		log.info("Indexing products in " + HuddleUtils.CATALOG_ID);
		Tree tree = getProductData(HuddleUtils.CATALOG_ID);
		
		//begin iterating the category tree; this call is recursive and will iterate the entire tree sequentially
		for (Node child : tree.getRootNode().getChildren()) {
			loopNode(child);
		}
		
		//verify we have something to index
		if (products == null || products.size() == 0) return;
		
		//push the list to Solr all at once
		try {
			SolrActionUtil solrUtil = new SolrActionUtil(server);
			solrUtil.addDocuments(products.values());
		} catch (Exception e) {
			log.error("Unable to index products", e);
		}
	}
	
	
	/**
	 * recursive method that walks the Product Tree from a certain level (down)
	 * @param n
	 */
	private void loopNode(Node par) {
		//add this level to the hierarchy tree
		log.debug("adding hierarchy: " + par.getDepthLevel() + "=" + par.getNodeName());
		hierarchy.put(Integer.valueOf(par.getDepthLevel()), par.getNodeName());
		
		for (Node child : par.getChildren()) {
			ProductCategoryVO vo = (ProductCategoryVO)child.getUserObject();
			if (vo.getProductId() != null) {
				//index this product
				indexProduct(vo, child.getDepthLevel() -1); //ignore the depth of the product
			} else {
				//dig a level deeper
				loopNode(child);
			}
		}
	}
	
	
	/**
	 * creates a SolrDocumentVO from the passed ProductCategoryVO,
	 * then adds the hierarchy or opco_ss to it based on the pre-built hierarchy.
	 * @param vo
	 * @param depth
	 */
	private void indexProduct(ProductCategoryVO vo, int depth) {
		for (ProductVO pVo : vo.getProducts()) { //these are actually 1:1, but we'll iterate the loop anyways
				
			ProductCatalogSolrDocumentVO solrDoc = products.get(vo.getProductId());
			if (solrDoc == null) {
				solrDoc = new ProductCatalogSolrDocumentVO(INDEX_TYPE);
				solrDoc.setDocumentId(pVo.getProductId());
				solrDoc.setTitle(pVo.getTitle());
				solrDoc.setSummary(pVo.getDescText());
				solrDoc.setDetailImage(pVo.getImage());
				solrDoc.setDocumentUrl(pVo.getUrlAlias());
				solrDoc.addOrganization(organizationId);
				solrDoc.setModule(INDEX_TYPE);
				solrDoc.addRole(SecurityController.PUBLIC_ROLE_LEVEL);
				attachProductCategories(solrDoc, depth);
				addProductAttributes(solrDoc, pVo);
			}

			//add a new hierarchy to this product; its either Specialty or Category (we only support two)
			attachProductCategories(solrDoc, depth);
			products.put(pVo.getProductId(), solrDoc);
			log.info("added product " + solrDoc.getTitle());
		}
	}
	
	
	/**
	 * Turns the recursively built hierarchy into either Specialty (opco_ss) 
	 * or Category (hierarchy) on the SolrDoc
	 * @param solrDoc
	 * @param depth
	 */
	private void attachProductCategories(ProductCatalogSolrDocumentVO solrDoc, int depth) {
		//verify we have quality data to parse
		if (hierarchy == null || hierarchy.size() < depth) return;
		
		//determine if this is a Speciaty or Category tree; they get added to the document differently
		if ("Specialties".equalsIgnoreCase(hierarchy.get(Integer.valueOf(1)))) { //level 1 of category tree
			solrDoc.setSpecialty(hierarchy.get(depth));
			log.debug("set specialty= " + hierarchy.get(depth));
		} else {
			StringBuilder sb = new StringBuilder(100);
			for (int x=2; x <= depth; x++) { //level 2 here is the 1st level BELOW "Categories"
				String lvl = hierarchy.get(Integer.valueOf(x));
				sb.append(lvl);
				if (x < depth) sb.append(SearchDocumentHandler.HIERARCHY_DELIMITER);
			}
			log.debug("set hierarchy= " + sb);
			solrDoc.addHierarchies(sb.toString());
		}
	}
	
	
	/**
	 * loops and adds the product attributes to the Solr record
	 * @param solrDoc
	 * @param pVo
	 */
	private void addProductAttributes(ProductCatalogSolrDocumentVO solrDoc, ProductVO pVo) {
		ProductAttributeContainer attrContainer = pVo.getAttributes();
		if (attrContainer == null) return;
		
		// Loop over all attributes and add them to the
		// custom field map on the solr document
		for (Node attrNode : attrContainer.getAllAttributes()) {
			if (attrNode.getUserObject() == null) continue;
			ProductAttributeVO attr = (ProductAttributeVO)attrNode.getUserObject();
			
			// This attribute has nothing we need and can be skipped.
			if (attr.getValueText() == null) continue;
			
			// All attributes go into lists with keys made from the attribute name
			// and prefixed by the attribute type so that classes parsing those
			// attributes know what kind of data is being worked with.
			String fieldNm = attr.getAttributeType() + "_" + attr.getAttributeName();
			@SuppressWarnings("unchecked")
			List<String> values = (List<String>) solrDoc.getAttribute(fieldNm);
			if (values == null) values = new ArrayList<>();
			
			//TODO if this is a Mediabin field,
			//turn the value back into a JSON object and index the mediabinAssetIds only

			values.add(attr.getValueText());
			solrDoc.addAttribute(fieldNm, values);
		}
	}
	

	/**
	 * load the entire product catalog in the same way the public sites would.
	 * leverage the Util class to traverse and assign pageUrls for us.
	 * @param orgId
	 */
	private Tree getProductData(String catalogId) {
		log.debug("loading product for catalogId=" + catalogId);
		ProductCatalogUtil util = new ProductCatalogUtil();
		util.setDBConnection(new SMTDBConnection(dbConn));
		Map<String, Object> attribs = new HashMap<String, Object>();
		attribs.put(Constants.MODULE_DATA, new ModuleVO());
		attribs.put(Constants.QS_PATH, config.get(Constants.QS_PATH));
		util.setAttributes(attribs);
		return util.loadCatalog(catalogId, null, true, null);
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