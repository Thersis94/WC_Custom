package com.sas.util;

// JDK 1.6.x
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

// log4j 1.2-15
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;

// SMT Base Libs
import com.siliconmtn.cms.CMSConnection;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.commerce.product.ProductCatalogAction;
import com.smt.sitebuilder.action.commerce.product.ProductController;
import com.smt.sitebuilder.search.lucene.DocumentHandler;
import com.smt.sitebuilder.search.lucene.DocumentHandlerImpl;
import com.smt.sitebuilder.search.lucene.DocumentMap;
import com.smt.sitebuilder.search.lucene.custom.SMTCustomIndexIntfc;

/****************************************************************************
 * <b>Title</b>: FASTSIGNSProductIndex.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jan 11, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class ProductIndex implements SMTCustomIndexIntfc {
	private static Logger log = Logger.getLogger("SASProductIndex");
	private DocumentMap ldm;
	
	public static final String ORGANIZATON_ID = "SAS";
	
	/**
	 * 
	 */
	public ProductIndex() {
        ldm = new DocumentMap();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.lucene.custom.SMTCustomIndexIntfc#addIndexItems(java.sql.Connection, com.siliconmtn.cms.CMSConnection, org.apache.lucene.index.IndexWriter)
	 */
	@Override
	public void addIndexItems(Connection conn, CMSConnection cmsConn, IndexWriter writer, Properties config) {
		log.info("Indexing SAS Products");
		this.indexProducts(conn, ORGANIZATON_ID, writer);
	}
	
    
    /**
     * Flattens out the hierarchy and stores all fields in the content fields
     * @param conn
     * @param orgId
     * @param writer
     */
    public void indexProducts(Connection conn, String orgId, IndexWriter writer) {
    	List<Node> nodes = this.getProductData(conn, orgId);
        DocumentHandler dh = new DocumentHandlerImpl(ldm.getClassName("html"));
        Document doc = null;
        log.debug("Starting indexing");
    	for (int i = 0; i < nodes.size(); i++) {
    		Node n = nodes.get(i);
    		ProductVO vo = (ProductVO)n.getUserObject();
    		//log.debug("Full Path: /cat" + n.getFullPath());
    		try {
	    		doc = dh.getDocument(vo.toString().getBytes());
	    		Date start = Convert.formatDate(new java.util.Date(), Calendar.MONTH, -1);
	    		Date end = Convert.formatDate(new java.util.Date(), Calendar.MONTH, 1);
		        doc.add(new StringField(DocumentHandler.ORGANIZATION, orgId,Field.Store.YES));
		        doc.add(new StringField(DocumentHandler.LANGUAGE, "en",Field.Store.YES));
		        doc.add(new StringField(DocumentHandler.ROLE, "000",Field.Store.YES));
		        doc.add(new TextField(DocumentHandler.SITE_PAGE_URL, "cat" + n.getFullPath(),Field.Store.YES));
		        doc.add(new TextField(DocumentHandler.DOCUMENT_URL, "cat" + n.getFullPath(),Field.Store.YES));
		        doc.add(new TextField(DocumentHandler.DOCUMENT_ID, vo.getProductId(),Field.Store.YES));
		        doc.add(new TextField(DocumentHandler.FILE_NAME, vo.getProductName(),Field.Store.YES));
		        doc.add(new TextField(DocumentHandler.TITLE, vo.getProductName(),Field.Store.YES));
		        doc.add(new TextField(DocumentHandler.SUMMARY, vo.getDescText(),Field.Store.YES));
	            doc.add(new TextField(DocumentHandler.START_DATE, Convert.formatDate(start, Convert.DATE_NOSPACE_PATTERN),Field.Store.YES));
	            doc.add(new TextField(DocumentHandler.END_DATE,Convert.formatDate(end, Convert.DATE_NOSPACE_PATTERN),Field.Store.YES));
	            doc.add(new TextField(DocumentHandler.UPDATE_DATE,Convert.formatDate(new Date(), Convert.DATE_NOSPACE_PATTERN),Field.Store.YES));
		        writer.addDocument(doc);
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
    public List<Node> getProductData(Connection conn, String orgId) {
    	String s = "select * from PRODUCT where product_id not like '"; 
    	s += ProductImporter.PRODUCT_GROUP_PREFIX + "%' and status_no=? AND PRODUCT_GROUP_ID IS NULL ";
    	s += "and product_catalog_id in (select product_catalog_id from product_catalog where status_no=" + 
    		 ProductCatalogAction.STATUS_LIVE + " and organization_id=?)  ";//order by PARENT_ID, PRODUCT_ID";
    	log.debug("SAS Product SQL: " + s + "|" + orgId + "|" + ProductController.PRODUCT_STATUS_ACTIVE);
    	
    	PreparedStatement ps = null;
    	List<Node> nodes = new ArrayList<Node>(35000);

    	try {
    		ps = conn.prepareStatement(s);
    		ps.setInt(1, ProductController.PRODUCT_STATUS_ACTIVE);
    		ps.setString(2, orgId);
    		ResultSet rs = ps.executeQuery();
    		log.debug("Query Executed");
    		for (int i=0; rs.next(); i++) {
    			ProductVO prod = new ProductVO(rs); 
    			Node n = new Node();
    			n.setNodeId(prod.getProductUrl());
    			n.setNodeName(prod.getProductName());
    			n.setParentId(StringUtil.checkVal(prod.getParentId(), "wc_root"));
    			n.setRoot(false);
    			n.setUserObject(prod);
    			nodes.add(n);
    			
    			if (i % 1000 == 0) log.debug("Number retrieved from DB: " + i);
    		}
    		
    	} catch(Exception e) {
    		log.error("Unable to retrieve product info", e);
    	} finally {
    		try {
    			ps.close();
    		} catch(Exception e){}
    	}
    	log.debug("Number of products: " + nodes.size());

    	return this.assignFullPath(nodes);
    }
    
    /**
     * Assigns the full path based upon the pre-order list
     * @param data
     * @return
     */
    public List<Node> assignFullPath(List<Node> data) {
    	//Map<String, String> fp = new HashMap<String, String>(35000);
    	for (int i=0; i < data.size(); i++) {
    		Node n = data.get(i);
    		
    		if (n.getParentId() == null) continue;
    		else 
    			n.setFullPath("/" + SMTServletRequest.DIRECTORY_KEY + "/detail/" + n.getNodeId() + "/" + ((ProductVO)n.getUserObject()).getCustProductNo());
    		
    		//log.debug("Full Path: " + n.getFullPath());
    		//fp.put(n.getNodeId(), n.getFullPath());
    	}
    	
    	return data;
    }

}
