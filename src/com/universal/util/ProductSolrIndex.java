package com.universal.util;

// Java 8
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

// log4j
import org.apache.solr.client.solrj.SolrClient;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.commerce.product.ProductCatalogAction;
import com.smt.sitebuilder.action.commerce.product.ProductController;
import com.smt.sitebuilder.search.SMTAbstractIndex;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SolrActionUtil;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: ProductSolrIndex.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim
 * @version 1.0
 * @since Aug 25, 2015<p/>
 * @updates:
 * TJ 08/25/15 
 * 		Copied the file and modified for the Solr Indexer
 ****************************************************************************/
public class ProductSolrIndex extends SMTAbstractIndex {

	public static final String ORGANIZATON_ID = "USA";
	public static final String CUSTOM_FIELD_CATALOG = "catalog_s";
	public static final String CATALOG_PAGE_URL = "catalog";
	public static final String FULL_PATH_PREFIX = "/" + ActionRequest.DIRECTORY_KEY + "/detail/";

	/**
	 * Index type for this index.  This value is stored in the INDEX_TYPE field
	 */
	public static final String INDEX_TYPE = "USA_PRODUCTS";
	public static final String SOLR_DOC_CLASS = "com.universal.util.data.ProductSolrDocumentVO";


	/**
	 * @param config
	 */
	public ProductSolrIndex(Properties config) {
		super(config);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#addIndexItems(org.apache.solr.client.solrj.SolrClient)
	 */
	public void addIndexItems(SolrClient server) {
		log.info("Indexing USA Products");

		// Index the products
		List<Node> nodes = this.getProductData();
		this.indexProducts(server, nodes);
	}


	/**
	 * Flattens out the hierarchy and stores all fields in the content fields
	 * @param server
	 */
	@SuppressWarnings("resource")
	private void indexProducts(SolrClient server, List<Node> nodes) {
		log.info("Found " + nodes.size() + " nodes containing products to index.");

		SolrActionUtil solrUtil = new SolrActionUtil(server);
		SolrDocumentVO solrDoc = null;

		for (Node n : nodes) {
			ProductVO vo = (ProductVO)n.getUserObject();
			try {
				solrDoc = SolrActionUtil.newInstance(SOLR_DOC_CLASS);
				solrDoc.setData(vo);
				solrDoc.addOrganization(ORGANIZATON_ID);
				solrDoc.setDocumentUrl(CATALOG_PAGE_URL + n.getFullPath());
				log.debug("adding to Solr: " + solrDoc.toString());
				solrUtil.addDocument(solrDoc);
			} catch (Exception e) {
				log.error("Unable to index products",e);
			}
		}
	}


	/**
	 * 
	 * @param conn
	 * @param orgId
	 */
	private List<Node> getProductData() {
		List<Node> nodes = new ArrayList<>();
		Tree tree = null;

		try(PreparedStatement ps = dbConn.prepareStatement(getProductDataSql())) {
			ps.setInt(1, ProductController.PRODUCT_STATUS_ACTIVE);
			ps.setString(2, ORGANIZATON_ID);

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				ProductVO prod = new ProductVO(rs); 
				Node n = new Node();
				n.setNodeId(prod.getProductUrl());
				n.setNodeName(prod.getProductName());
				n.setParentId(StringUtil.checkVal(prod.getParentId(), "wc_root"));
				n.setRoot(false);
				n.setUserObject(prod);
				nodes.add(n);
			}

		} catch(Exception e) {
			log.error("Unable to retrieve product info", e);
		}

		tree = new Tree(nodes, new Node("wc_root",null));
		return this.assignFullPath(tree.preorderList());
	}


	/**
	 * Gets the SQL for the product data query.
	 * @return
	 */
	private String getProductDataSql() {
		StringBuilder sql = new StringBuilder(300);
		sql.append("select * from PRODUCT where status_no=? AND PRODUCT_GROUP_ID IS NULL ");
		sql.append("and product_catalog_id in (select product_catalog_id from product_catalog where status_no=");
		sql.append(ProductCatalogAction.STATUS_LIVE + " and organization_id =?) order by PARENT_ID, PRODUCT_ID");

		log.debug("USA Product SQL: " + sql + "|" + ORGANIZATON_ID + "|" + ProductController.PRODUCT_STATUS_ACTIVE);

		return sql.toString();
	}


	/**
	 * Assigns the full path based upon the pre-order list
	 * @param data
	 * @return
	 */
	private List<Node> assignFullPath(List<Node> data) {
		for (int i=0; i < data.size(); i++) {
			Node n = data.get(i);

			if (n.getParentId() == null) 
				continue;
			else 
				n.setFullPath(FULL_PATH_PREFIX + ((ProductVO)n.getUserObject()).getProductId());

			log.debug("Full Path: " + n.getFullPath());
		}

		return data;
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
		return INDEX_TYPE;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#indexItems(java.lang.String[])
	 */
	@Override
	public void indexItems(String... itemIds) {
		// Universal products are loaded en mass from external documents and 
		// solr requires full refreshes each time.  This should never be called
		// from anywhere.
	}
}
