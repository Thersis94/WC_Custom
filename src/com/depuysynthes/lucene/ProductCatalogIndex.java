package com.depuysynthes.lucene;

// JDK 1.6.x
import java.sql.Connection;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

// log4j 1.2-15
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;

// SMT Base Libs
import com.depuysynthes.action.ProductCatalogUtil;
import com.siliconmtn.cms.CMSConnection;
import com.siliconmtn.commerce.catalog.ProductAttributeContainer;
import com.siliconmtn.commerce.catalog.ProductAttributeVO;
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.tools.StatVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.lucene.DocumentHandler;
import com.smt.sitebuilder.search.lucene.custom.SMTCustomIndexIntfc;

/****************************************************************************
 * <b>Title</b>: ProductCatalogIndex.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> This class gets invoked by the Lucene Index Builder (batch)
 * It adds the DS product catalogs to the Lucene Indexes to be usable in site search.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since May 19, 2013<p/>
 ****************************************************************************/
public class ProductCatalogIndex implements SMTCustomIndexIntfc {
	protected Logger log = null;
	
	protected String ORGANIZATION_ID = null;
	
	
	public ProductCatalogIndex() {
		log = Logger.getLogger(this.getClass());
        ORGANIZATION_ID = "DPY_SYN";
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.lucene.custom.SMTCustomIndexIntfc#addIndexItems(java.sql.Connection, com.siliconmtn.cms.CMSConnection, org.apache.lucene.index.IndexWriter)
	 */
	@Override
	public void addIndexItems(Connection conn, CMSConnection cmsConn, IndexWriter writer, Properties config) {
		log.info("Indexing DePuySynthes US Products & Procedures");
		indexProducts(conn, "DS_PRODUCTS", writer);
		indexProducts(conn, "DS_PROCEDURES", writer);
	}
	
    
    /**
     * Flattens out the hierarchy and stores all fields in the content fields
     * @param conn
     * @param orgId
     * @param writer
     */
    protected void indexProducts(Connection conn, String catalogId, IndexWriter writer) {
    	List<Node> nodes = getProductData(conn, catalogId);
    	//DocumentHandler dh = new DocumentHandlerImpl(ldm.getClassName("html"));
    	Document doc = null;

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
    		log.info("adding product to index: url=" + vo.getCategoryName() + ", img=" + imagePath + " org=" + ORGANIZATION_ID + " country=" + country);
    		try {
    			//doc = dh.getDocument(vo.toString().getBytes());
	    		doc = this.buildDocument(vo, pVo);
	    		doc.add(new StringField(DocumentHandler.ORGANIZATION, 	ORGANIZATION_ID,	Field.Store.YES));
	    		//doc.add(new StringField(DocumentHandler.COUNTRY, 		country,			Field.Store.YES));
		        doc.add(new StringField(DocumentHandler.LANGUAGE, 		"en",				Field.Store.YES));
		        doc.add(new StringField(DocumentHandler.ROLE, 			"000",				Field.Store.YES));
		        doc.add(new TextField(DocumentHandler.SITE_PAGE_URL,	vo.getCategoryUrl(),Field.Store.YES));
		        doc.add(new TextField(DocumentHandler.DOCUMENT_URL, 	vo.getCategoryUrl(),Field.Store.YES));
		        doc.add(new StringField(DocumentHandler.DOCUMENT_ID, 	n.getNodeId(),		Field.Store.YES));
		        doc.add(new TextField(DocumentHandler.FILE_NAME, 		n.getNodeName(),	Field.Store.YES));
		        doc.add(new TextField(DocumentHandler.TITLE, 			n.getNodeName(),	Field.Store.YES));
		        doc.add(new TextField(DocumentHandler.SUMMARY, 			StringUtil.checkVal(vo.getCategoryDesc()),	Field.Store.YES));
		        doc.add(new TextField(DocumentHandler.SECTION,			divisionNm,			Field.Store.YES));
		        doc.add(new StringField(DocumentHandler.MODULE_TYPE,	catalogId,			Field.Store.YES));
		        if (imagePath != null) doc.add(new TextField(DocumentHandler.THUMBNAIL_IMAGE, imagePath,	Field.Store.YES));
		        
		        Date start = Convert.formatDate(Calendar.getInstance().getTime(), Calendar.MONTH, -1);
	    		Date end = Convert.formatDate(Calendar.getInstance().getTime(), Calendar.MONTH, 1);
		        doc.add(new TextField(DocumentHandler.START_DATE, 	Convert.formatDate(start, Convert.DATE_NOSPACE_PATTERN),		Field.Store.YES));
	            doc.add(new TextField(DocumentHandler.END_DATE,		Convert.formatDate(end, Convert.DATE_NOSPACE_PATTERN),			Field.Store.YES));
	            doc.add(new TextField(DocumentHandler.UPDATE_DATE,	Convert.formatDate(new Date(), Convert.DATE_NOSPACE_PATTERN),	Field.Store.YES));
		        writer.addDocument(doc);
    		} catch (Exception e) {
    			log.error("Unable to index product " + n.getNodeId(),e);
    		}
    	}
    }
    
    
    /**
     * load the entire product catalog in the same way the public sites would.
     * leverage the Util class to traverse and assign pageUrls for us.
     * @param conn
     * @param orgId
     */
    private List<Node> getProductData(Connection conn, String catalogId) {
    	ProductCatalogUtil util = new ProductCatalogUtil();
    	util.setDBConnection(new SMTDBConnection(conn));
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
     * @param catVo
     * @param prodVo
     * @return
     */
    protected Document buildDocument(ProductCategoryVO catVo, ProductVO prodVo) {
    	Document doc = new Document();
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
    	doc.add(new TextField(DocumentHandler.CONTENTS, txt.toString(), Field.Store.NO));
    	return doc;
    }
}
