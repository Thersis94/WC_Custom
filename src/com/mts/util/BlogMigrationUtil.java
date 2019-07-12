package com.mts.util;

// JDK 1.8.x
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

// Log4j 1.2.17
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
// Dom
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

// MTS Docs
import com.mts.publication.data.MTSDocumentVO;

// SMT Base Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.ClassUtils;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.content.DocumentVO;
import com.smt.sitebuilder.action.metadata.WidgetMetadataVO;

/****************************************************************************
 * <b>Title</b>: BlogMigrationUtil.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Parses the XML File Export from the Blog Wordpress Site 
 * and loads the blog content into an article
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jul 8, 2019
 * @updates:
 ****************************************************************************/

public class BlogMigrationUtil {
	/**
	 * Log4j Logger
	 */
	protected static final Logger log = Logger.getLogger(BlogMigrationUtil.class);
	
	/**
	 * Location of the wordpress XML export file
	 */
	public static final String XML_FILE_LOC = "/home/ryan/Downloads/Squarespace-Wordpress-Export-07-05-2019.xml";
		
	/**
	 * Link Prefix on the RSS feed for an Item
	 */
	public static final String BLOG_LINK_PREFIX = "/mts-blog/";
	
	// Feed tag ids, misc mappings
	public static final String ITEM = "item";
	public static final String TITLE = "title";
	public static final String LINK = "link";
	public static final String CONTENT = "content:encoded";
	public static final String PUB_DATE = "wp:post_date";
	public static final String POST_ID = "wp:post_id";
	public static final String DEF_CAT_ID = "PERSPECTIVE_COMM";
	public static final String CREATED_BY_ID = "MTS_USER_1";
	public static final String BLOG_PUB_ID = "MTS_BLOG";
	public static final String AUTHOR_ID = "MTS_USER_1";
	public static final String ISSUE_ID = "MTS_BLOG_ISSUE_1";
	public static final String POST_NAME = "wp:post_name";
	public static final String MODULE_TYPE = "DOCUMENT";
	public static final String ORG_ID = "MTS";
	public static final String FILE_TYPE = "html";
	public static final String SOURCE_CODE = "CUSTOMER_IMPORT";
	public static final String CUSTOM_DB_SCHEMA = "custom.";
	
	/**
	 * 
	 */
	public BlogMigrationUtil() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		BlogMigrationUtil bmu = new BlogMigrationUtil();
		Connection conn = bmu.getConnection();
		
		// Get and parse the docs
		List<MTSDocumentVO> docs = bmu.parseXMLDoc();
		log.info("Num Blogs: " + docs.size());
		
		// Save the docs
		docs = bmu.getImageLinks(docs);
		
		bmu.saveDocuments(conn, docs);
		
		log.info("blogs migration completed ");
	}
	
	/**
	 * Save all of the documents to the db
	 * @param conn
	 * @param docs
	 * @throws com.siliconmtn.db.util.DatabaseException
	 * @throws InvalidDataException 
	 */
	public void saveDocuments(Connection conn, List<MTSDocumentVO> docs) 
	throws com.siliconmtn.db.util.DatabaseException, InvalidDataException {
		DBProcessor dbCustom = new DBProcessor(conn, CUSTOM_DB_SCHEMA);
		DBProcessor dbCore = new DBProcessor(conn);
		
		for (MTSDocumentVO doc : docs) {
			// Assign the SB Action entry
			SBActionVO avo = new SBActionVO(doc);
			dbCore.insert(avo);
			
			// Assign the wc document entry
			DocumentVO dvo = (DocumentVO)ClassUtils.cloneBean(doc, true);
			dbCore.insert(dvo);
			
			// Assign the MTS Document info
			dbCustom.insert(doc);
			
			/**
			 * Add the meta data
			 */
			for (WidgetMetadataVO cat : doc.getCategories()) {
				dbCustom.insert(cat);
			}
		}
	}
	
	
	/**
	 * Gets a list of the images used in the blogs
	 * @param docs
	 * @return 
	 * @throws UnsupportedEncodingException 
	 */
	public List<MTSDocumentVO> getImageLinks(List<MTSDocumentVO> docs) throws UnsupportedEncodingException {
		for (MTSDocumentVO document : docs) {
			org.jsoup.nodes.Document doc = Jsoup.parse(document.getDocument(), "");
			Elements images = doc.getElementsByTag("img");
			for (org.jsoup.nodes.Element image : images) {
				// Get the image path and relabel to the new path
				String src = image.attr("src");
				int index = src.lastIndexOf('/');
				String name = URLDecoder.decode(src.substring(index + 1), "UTF-8");
				name = URLDecoder.decode(name, "UTF-8");
				name = name.replace("?format=original", "");
				String ext = name.substring(name.lastIndexOf('.'));
				String base = name.substring(0, name.lastIndexOf('.'));
				name = StringUtil.formatFileName(base, true);
				String newPath = "/binary/org/MTS/Blog/article/" + name + ext;

				// Update the image URL Path
				image.attr("src", newPath);
				image.attr("alt", "altered");
			}
			document.setDocument(doc.body().html());

		}
		return docs;
	}
	
	/**
	 * Retrieves and parses the XML document into a collection of documents
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public List<MTSDocumentVO> parseXMLDoc() throws ParserConfigurationException, SAXException, IOException {
		List<MTSDocumentVO> docs = new ArrayList<>();
		
		// Build the XMl Parser
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(new File(XML_FILE_LOC));
		doc.getDocumentElement().normalize();
		
		// get the list of Item nodes
		NodeList nList = doc.getElementsByTagName(ITEM);
		
		// Loop the nodes and search for the blogs
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				
				// Make sure the item is a Blog
				String link = eElement.getElementsByTagName(LINK).item(0).getTextContent();
				if (link != null && ! link.startsWith(BLOG_LINK_PREFIX)) continue;
				
				// Build the MTS Document
				MTSDocumentVO mtsDoc = new MTSDocumentVO();
				mtsDoc.setActionGroupId(eElement.getElementsByTagName(POST_ID).item(0).getTextContent());
				mtsDoc.setActionId(mtsDoc.getActionGroupId());
				mtsDoc.setDocumentId(mtsDoc.getActionId());
				mtsDoc.setActionName(eElement.getElementsByTagName(TITLE).item(0).getTextContent());
				mtsDoc.setActionDesc(mtsDoc.getActionName());
				mtsDoc.setDocument(eElement.getElementsByTagName(CONTENT).item(0).getTextContent());
				mtsDoc.setDocumentFolderId(BLOG_PUB_ID);
				mtsDoc.setAuthorId(AUTHOR_ID);
				mtsDoc.setDirectAccessPath(eElement.getElementsByTagName(POST_NAME).item(0).getTextContent());
				mtsDoc.setIssueId(ISSUE_ID);
				mtsDoc.setUniqueCode(RandomAlphaNumeric.generateRandom(6));
				mtsDoc.setModuleTypeId(MODULE_TYPE);
				mtsDoc.setOrganizationId(ORG_ID);
				mtsDoc.setPendingSyncFlag(0);
				mtsDoc.setDocumentSourceCode(SOURCE_CODE);
				mtsDoc.setFileType(FILE_TYPE);
				
				// Set the publish Date
				String pubDate = eElement.getElementsByTagName(PUB_DATE).item(0).getTextContent();
				mtsDoc.setPublishDate(Convert.parseDateUnknownPattern(pubDate));
				mtsDoc.setCreateDate(mtsDoc.getPublishDate());
				
				// Add the category
				WidgetMetadataVO md = new WidgetMetadataVO();
				md.setWidgetMetadataId(DEF_CAT_ID);
				md.setCreateDate(new Date());
				md.setCreateById(CREATED_BY_ID);
				mtsDoc.addCategory(md);
				
				// Add the document to the collection
				docs.add(mtsDoc);
			}
		}
		
		return docs;
	}
	
	/**
	 * Gets the connection to the MySQL Source
	 * @return
	 * @throws InvalidDataException 
	 * @throws DatabaseException 
	 */
	public Connection getConnection() throws DatabaseException {
		DatabaseConnection dc = new DatabaseConnection();
		dc.setDriverClass("org.postgresql.Driver");
		dc.setUrl("jdbc:postgresql://sonic:5432/webcrescendo_mts5_sb?defaultRowFetchSize=25&amp;prepareThreshold=3");
		dc.setUserName("ryan_user_sb");
		dc.setPassword("sqll0gin");
		Connection conn = null;
		
		try {
			conn = dc.getConnection();
		} catch (Exception e) {
			throw new DatabaseException(e);
		}
		
		return conn;
	}

}

