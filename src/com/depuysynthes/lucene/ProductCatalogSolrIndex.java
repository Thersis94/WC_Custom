package com.depuysynthes.lucene;

// JDK 1.6.x
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;



// log4j 1.2-15
import org.apache.solr.client.solrj.SolrClient;



// SMT Base Libs
import com.depuysynthes.action.ProductCatalogUtil;
import com.depuysynthes.lucene.data.ProductCatalogSolrDocumentVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.commerce.catalog.ProductAttributeContainer;
import com.siliconmtn.commerce.catalog.ProductAttributeVO;
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.tools.StatVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SMTAbstractIndex;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SolrActionUtil;

/****************************************************************************
 * <b>Title</b>: ProductCatalogSolrIndex.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> This class gets invoked by the Solr Index Builder (batch)
 * It adds the DS product catalogs to the Solr Indexes to be usable in site search.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since May 19, 2013<p/>
 * @updates:
 * TJ 08/24/15 
 * 		Copied the file and modified for the Solr Indexer
 ****************************************************************************/
public class ProductCatalogSolrIndex extends SMTAbstractIndex {

	protected String organizationId = null;

	/**
	 * Index type for this index.  This value is stored in the INDEX_TYPE field
	 */
	public static String INDEX_TYPE = "DS_PRODUCTS";
	protected static String SOLR_DOC_CLASS = ProductCatalogSolrDocumentVO.class.getName();

	/**
	 * @param config
	 */
	public ProductCatalogSolrIndex(Properties config) {
		super(config);
		organizationId = "DPY_SYN";
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.lucene.custom.SMTCustomIndexIntfc#addIndexItems(java.sql.Connection, com.siliconmtn.cms.CMSConnection, org.apache.lucene.index.IndexWriter)
	 */
	@Override
	public void addIndexItems(SolrClient server) {
		log.info("Indexing DePuySynthes US Products & Procedures");
		indexProducts("DS_PRODUCTS", server, SOLR_DOC_CLASS, 50, "DS_PRODUCT");
		indexProducts("DS_PROCEDURES", server, SOLR_DOC_CLASS, 45, "DS_PROCEDURE");
	}


	/**
	 * Flattens out the hierarchy and stores all fields in the content fields
	 * @param catalogId
	 * @param server
	 */
	@SuppressWarnings("resource")
	protected void indexProducts(String catalogId, SolrClient server, String solrDocClass, int dsOrderNo, String moduleType) {
		List<Node> nodes = getProductData(catalogId);
		log.info("Found " + nodes.size() + " nodes to index for " + catalogId + ".");

		SolrActionUtil solrUtil = new SolrActionUtil(server);
		ProductCatalogSolrDocumentVO solrDoc = null;
		String divisionNm = null;
		String countryNodeId = null;
		String country = "";
		String lastCategoryName = null;
		
		for (Node n : nodes) {
			ProductCategoryVO vo = (ProductCategoryVO)n.getUserObject();

			//top level categories define our countries
			if (n.getParentId() == null) {
				country = vo.getUrlAlias();
				countryNodeId = n.getNodeId();
				log.debug("changed country to " + country);
				continue; //these are not products (to index)
			} else if (n.getParentId().equals(countryNodeId)) {
				divisionNm = n.getNodeName();
				log.debug("set division to: " + divisionNm);
				lastCategoryName = divisionNm;
				continue; //these are child categories, not products (to index)
			}

			if (StringUtil.checkVal(vo.getCategoryUrl()).length() == 0 || vo.getProducts().size() == 0) {
				log.debug("not a product: " + vo.getCategoryName());
				lastCategoryName = divisionNm + " " + vo.getCategoryName();
				continue;
			}

			//pull the category name off the parent node
			vo.setCategoryName(lastCategoryName);

			String imagePath = null;
			ProductVO pVo = null;
			if (vo.getProducts() != null && vo.getProducts().size() > 0) {
				try {
					pVo = vo.getProducts().get(0);
					//log.debug("product= " + StringUtil.getToString(pVo));
					if (vo.getCategoryDesc() == null || vo.getCategoryDesc().length() == 0)
						vo.setCategoryDesc(pVo.getDescText());

					ProductAttributeContainer attrs = pVo.getAttributes();
					//log.debug("#attribs=" + attrs.getRootAttributes().size());
					for (Node an : attrs.getRootAttributes()) {
						ProductAttributeVO attr = (ProductAttributeVO) an.getUserObject();
						//log.debug("attr=" + attr.getAttributeId() + " par=" + attr.getParentId());
						if (attr.getAttributeId().startsWith("DS_IMAGE")) {
							//log.debug("found image for " + attr.getAttributeId());
							imagePath = attr.getValueText();
							break;
						}
					}
				} catch (Exception e) {
					log.warn("could not extract productImage for: " + vo.getCategoryName());
				}
			}
			log.debug("adding product to index: section=" + vo.getCategoryName() + ", img=" + imagePath + " org=" + organizationId + " country=" + country);
			try {
				solrDoc = this.buildDocument(vo, pVo, solrDocClass);
				solrDoc.setData(n, vo);
				solrDoc.setDsOrderNo(dsOrderNo);
				solrDoc.addOrganization(organizationId);
				solrDoc.setModule(moduleType);
				solrDoc.addSection(divisionNm);
				if (imagePath != null) solrDoc.setThumbImage(imagePath);

				log.debug("adding to Solr: " + solrDoc.toString());
				solrUtil.addDocument(solrDoc);
			} catch (Exception e) {
				log.error("Unable to index product " + n.getNodeId(),e);
			}
		}
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



	/**
	 * the purpose of this method is to transpose the Category and Product VO's
	 * into searchable strings for Lucene to match against.
	 * Iterate the Category, Product, as well as the ProductAttributes.
	 * The value-add here is that we're making the database-values searchable in the index.
	 * If my product has an attribute "Resources", this will allow us to match against searches for "Resources"
	 * @param catVo
	 * @param prodVo
	 * @return
	 * @throws ActionException 
	 */
	private ProductCatalogSolrDocumentVO buildDocument(ProductCategoryVO catVo, ProductVO prodVo, String solrDocClass) throws ActionException {
		ProductCatalogSolrDocumentVO solrDoc = (ProductCatalogSolrDocumentVO) SolrActionUtil.newInstance(solrDocClass);
		StringBuilder txt = new StringBuilder(5000);

		//add the CategoryVO
		txt.append(catVo.toString()).append("\r");
		//log.debug("cat=" + catVo.toString());

		//add the ProductVO
		if (prodVo != null) {
			txt.append(prodVo.toString()).append("\r");
			//log.debug("prod=" + prodVo.toString());

			//add each of the ProductAttributeVOs
			try {
				for (Node n : prodVo.getAttributes().getAllAttributes()) {
					txt.append(((ProductAttributeVO)n.getUserObject()).toString()).append("\r");
					//	log.debug("added attrib: " + n);
				}

			} catch (Exception e) {
				//log.warn("could not add attributes to product: " + prodVo.getProductId(), e);
			}
		}

		//log.debug(txt);
		solrDoc.setContents(txt.toString());
		return solrDoc;
	}

	@Override
	public void purgeIndexItems(SolrClient server) throws IOException {
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

	@Override
	public void addSingleItem(String arg0) {
		// Currently nothing makes use of single item inserts with this class.
		// When products are added to the list of dynamic solr groups the class
		// will have to be reworked to support this function.
	}
}
