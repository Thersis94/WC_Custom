package com.depuysynthes.lucene;

// JDK 1.6.x
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

// log4j 1.2-15
import org.apache.solr.client.solrj.impl.HttpSolrServer;

// SMT Base Libs
import com.depuysynthes.action.ProductCatalogUtil;
import com.depuysynthes.lucene.data.ProductCatalogSolrDocumentVO;
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
	public static final String INDEX_TYPE = "PRODUCT";
	public static final String SOLR_DOC_CLASS = "com.depuysynthes.lucene.data.ProductCatalogSolrDocumentVO";

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
	public void addIndexItems(HttpSolrServer server) {
		log.info("Indexing DePuySynthes US Products & Procedures");
		indexProducts("DS_PRODUCTS", server);
		indexProducts("DS_PROCEDURES", server);
	}
	
    
    /**
     * Flattens out the hierarchy and stores all fields in the content fields
	 * @param catalogId
	 * @param server
     */
    protected void indexProducts(String catalogId, HttpSolrServer server) {
    	List<Node> nodes = getProductData(catalogId);
    	log.info("Found " + nodes.size() + " nodes to index for " + catalogId + ".");
    	
    	SolrActionUtil solrUtil = new SolrActionUtil(server);
		ProductCatalogSolrDocumentVO pcSolrDoc = null;

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
    		} else {
    			//trim the leading slash, search-results will prepend contextPath
    			vo.setCategoryUrl(vo.getCategoryUrl().substring(1));
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
    		log.debug("adding product to index: url=" + vo.getCategoryName() + ", img=" + imagePath + " org=" + organizationId + " country=" + country);
    		try {
	    		// solrDoc = this.buildDocument(vo, pVo);  // TJ - 8/26/2015: Removed per JM, not needed
	    		pcSolrDoc = new ProductCatalogSolrDocumentVO();

	    		pcSolrDoc.setData(n, vo);
	    		pcSolrDoc.addOrganization(organizationId);
	    		pcSolrDoc.setModule(catalogId);
	    		pcSolrDoc.addSection(divisionNm);
	    		if (imagePath != null) pcSolrDoc.setThumbImage(imagePath);
	    		
	    		log.debug("adding to Solr: " + pcSolrDoc.toString());
				solrUtil.addDocument(pcSolrDoc);
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
    	util.setAttributes(attribs);
    	log.debug("loading product for catalogId=" + catalogId);
    	Tree t = util.loadCatalog(catalogId, null, true, null);
    	
    	//let the util assign page URLs for us
    	return util.assignPageviewsToCatalogUsingDivision(t.preorderList(), new HashMap<String, StatVO>(), "/hcp/", (catalogId.startsWith("DS_PRODUCTS")), true);
    }
    
    
    
    /**
     * the purpose of this method is to transpose the Category and Product VO's
     * into searchable strings for Lucene to match against.
     * Iterate the Category, Product, as well as the ProductAttributes
     * 
     * TJ - 8/26/2015: Removed per JM, was needed in Lucene,
     * 					but no longer needed for Solr.
     * 
     * @param catVo
     * @param prodVo
     * @return
     */
    /* private SolrDocumentVO buildDocument(ProductCategoryVO catVo, ProductVO prodVo) {
    	SolrDocumentVO solrDoc = null;
		try {
			solrDoc = SolrActionUtil.newInstance(SOLR_DOC_CLASS);
		} catch (Exception e) {
			log.debug("Could not create SolrDocumentVO. ", e);
		}
    	
    	StringBuilder txt = new StringBuilder();
    	
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
    } */

	@Override
	public void purgeIndexItems(HttpSolrServer server) throws IOException {
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
}
