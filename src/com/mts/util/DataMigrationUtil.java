package com.mts.util;

// JDK 1.8.x
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Log4j 1.2.17
import org.apache.log4j.Logger;

import com.mts.publication.data.AssetVO;
import com.mts.publication.data.AssetVO.AssetType;
// MTS Libs
import com.mts.publication.data.CategoryVO;
import com.mts.publication.data.DocumentCategoryVO;
import com.mts.publication.data.IssueVO;
import com.mts.publication.data.MTSDocumentVO;

// WC Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.content.DocumentVO;

/****************************************************************************
 * <b>Title</b>: DataMigrationUtil.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Importer of MTS Core data 
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 1, 2019
 * @updates:
 ****************************************************************************/

public class DataMigrationUtil {

	private static final Logger log = Logger.getLogger(DataMigrationUtil.class);
	private Map<String, String> userMap = new HashMap<>();
	private Map<String, String> catMap = new HashMap<>();
	private Set<String> missingCats = new HashSet<>();
	private List<DocumentCategoryVO> categories = new ArrayList<>();
	private List<AssetVO> assets = new ArrayList<>();
	
	// Static Constants
	private static final String CUSTOM_SCHEMA = "custom.";
	private static final String ISSUE_KEY = "_yoast_wpseo_primary_issue";
	private static final String PRI_CATEGORY_KEY = "_yoast_wpseo_primary_article_category";
	private static final String ARTICLE_PDF = "article_pdf";
	private static final String ISSUE_COVER_IMAGE = "issue_cover_image";
	private static final String ISSUE_PDF = "issue_pdf";
	private static final String MTS_ORG = "MTS";
	private static final String MTS_DOC_FOLDER = "MED_TECH_STRATEGIST";
	
	// Turn on/off items to process
	private static final boolean PROC_CAT = false;
	private static final boolean PROC_DOCS = true;
	private static final boolean PROC_DOC_CAT = false;
	private static final boolean PROC_ASSET = false;
	private static final boolean PROC_ISSUES = false;
	private static final boolean PROC_ART = true;
	
	/**
	 * 
	 */
	public DataMigrationUtil() {
		super();
		assignMaps();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		log.info("Starting Migration");
		
		DataMigrationUtil dmu = new DataMigrationUtil();
		Connection srcConn = dmu.getSourceConnection();
		Connection destConn = dmu.getDestConnection();
		
		log.info("Source Conn: " + ! srcConn.isClosed());
		log.info("Dest Conn: " + ! destConn.isClosed());
		
		// Migrate the cats
		if (PROC_CAT) dmu.migrateCategories(srcConn, destConn);

		// Migrate the issues
		if (PROC_ISSUES) dmu.migrateIssues(srcConn, destConn);
		
		// Migrate the articles
		List<MTSDocumentVO> docs = new ArrayList<>();
		if (PROC_ART) docs = dmu.migrateArticles(srcConn);
		DBProcessor dbCore = new DBProcessor(destConn, "core.");
		DBProcessor dbCustom = new DBProcessor(destConn, CUSTOM_SCHEMA);
		
		for(MTSDocumentVO doc : docs) {
			// Assign the SB Action entry
			SBActionVO avo = new SBActionVO(doc);
			if (PROC_DOCS) dbCore.insert(avo);
			log.info("SB Action written");
			
			// Assign the wc document entry
			DocumentVO dvo = doc.getCoreDocument();
			
			if (PROC_DOCS) dbCore.insert(dvo);
			log.info("Core.document written");
			
			// Assign the MTS Document info
			if (PROC_DOCS) dbCustom.insert(doc);
			log.info("MTS Document written");
		}
		
		// Add the categories and assets
		if (PROC_DOC_CAT) dbCustom.executeBatch(dmu.categories, true);
		if (PROC_ASSET) dbCustom.executeBatch(dmu.assets, true);
		
		log.info("Number Docs: " + docs.size());
		
		srcConn.close();
		destConn.close();
		log.info("Migration Completed");
	}
	
	/**
	 * Assigns the categories and users for mapping
	 */
	public void assignMaps() {
		// User Mapping
		userMap.put("kayleen brown", "MTS_USER_1");
		userMap.put("david filmore", "MTS_USER_2");
		userMap.put("tracy neilssien", "MTS_USER_6");
		userMap.put("tracy schaaf", "MTS_USER_6");
		userMap.put("nancy dvorin", "MTS_USER_3");
		userMap.put("david cassak", "MTS_USER_7");
		userMap.put("stephen levin", "MTS_USER_8");
		userMap.put("wendy diller", "MTS_USER_9");
		userMap.put("mary stuart", "MTS_USER_10");
		userMap.put("mary thompson", "MTS_USER_11");
		userMap.put("colin miller", "MTS_USER_12");
		
		// Category mapping
		catMap.put("business strategies", "BUS_STRAT");
		catMap.put("emerging markets", "EMERG_MARKETS");
		catMap.put("executive interviews", "EXECUTIVE_INTERVIEWS");
		catMap.put("investors & dealmaking", "INVENT_DEALS");
		catMap.put("perspective & commentary", "PERSPECTIVE_COMM");
		catMap.put("providers & payors", "PROVIDER_PAYER");
		catMap.put("regulatory & reimbursement", "REG_REIMBURS");
		catMap.put("startups to watch", "STARTUP_WATCH");
	}
	
	/**
	 * 
	 * @param srcConn
	 * @param destConn
	 * @throws SQLException
	 * @throws com.siliconmtn.db.util.DatabaseException
	 */
	public List<MTSDocumentVO> migrateArticles(Connection srcConn) 
	throws SQLException {
		StringBuilder sql = new StringBuilder(128);
		
		// Get the articles
		sql.append("select  * ");
		sql.append("from wp_1fvbn80q5v_posts where post_type = 'article' ");
		sql.append("limit 30 ");
		List<MTSDocumentVO> docs = new ArrayList<>();
		try (PreparedStatement ps = srcConn.prepareStatement(sql.toString()); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				
				MTSDocumentVO doc = new MTSDocumentVO();
				doc.setActionId(rs.getString("ID"));
				doc.setActionGroupId(rs.getString("ID"));
				doc.setDocumentId(rs.getString("ID"));
				doc.setDocumentSourceCode("CUSTOMER_IMPORT");
				doc.setDocument(rs.getString("post_content"));
				doc.setDocumentFolderId(MTS_DOC_FOLDER);
				doc.setFileType("html");
				doc.setUniqueCode(RandomAlphaNumeric.generateRandom(6));
				doc.setOrganizationId(MTS_ORG);
				doc.setModuleTypeId("DOCUMENT");
				doc.setActionDesc(rs.getString("post_title"));
				doc.setActionName(rs.getString("post_title"));
				doc.setPendingSyncFlag(0);
				doc.setCreateById("MTS_USER_1");
				doc.setDirectAccessPath(rs.getString("post_name"));
				doc.setCreateDate(rs.getDate("post_date"));
				doc.setPublishDate(rs.getDate("post_date"));
				doc.setFileSize(StringUtil.checkVal(doc.getDocument()).length());
				
				// Parse the title
				this.parseArticleTitle(doc);
				
				// Load the other categories
				getArticleMetaData(srcConn, doc);
				
				// Add to the document collection
				if (! StringUtil.isEmpty(doc.getIssueId())) docs.add(doc);
				else log.info(doc.getActionName() + "\t" + doc.getDirectAccessPath());
			}
		}
		
		return docs;
	}
	
	/**
	 * Gets the article meta data (PDF, Category, etc ..)
	 * @param srcConn
	 * @param doc
	 * @throws SQLException
	 */
	public void getArticleMetaData(Connection srcConn, MTSDocumentVO doc) throws SQLException {
		String s = "select meta_key, meta_value from wp_1fvbn80q5v_postmeta where post_id = ?";
		
		try(PreparedStatement ps = srcConn.prepareStatement(s)) {
			ps.setString(1, doc.getActionId());
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				AssetVO avo = new AssetVO();
				DocumentCategoryVO dcat = new DocumentCategoryVO();
				
				switch (rs.getString(1)) {
					case ISSUE_KEY:
						doc.setIssueId(rs.getString(2));
						break;
					case PRI_CATEGORY_KEY:
						dcat.setCategoryCode(rs.getString(2));
						dcat.setDocumentId(doc.getActionId());
						categories.add(dcat);
						break;
					case ARTICLE_PDF:
						avo.setObjectReferenceId(doc.getDocumentId());
						avo.setDocumentAssetId(rs.getString(2));
						avo.setAssetType(AssetType.PDF_DOC);
						assets.add(avo);
						break;
					default:
						break;
				}
			}
		}
	}
	
	/**
	 * 
	 * @param srcConn
	 * @param issue
	 * @throws SQLException
	 */
	public void getIssueMetaData(Connection srcConn, IssueVO issue) throws SQLException {
		String s = "select meta_key, meta_value from wp_1fvbn80q5v_termmeta where term_id = ?";
		
		try(PreparedStatement ps = srcConn.prepareStatement(s)) {
			ps.setString(1, issue.getIssueId());
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				AssetVO avo = new AssetVO();
				switch (rs.getString(1)) {
					case ISSUE_COVER_IMAGE:
						avo.setObjectReferenceId(issue.getIssueId());
						avo.setDocumentAssetId(rs.getString(2));
						avo.setAssetType(AssetType.COVER_IMG);
						assets.add(avo);
						break;
					case ISSUE_PDF:
						avo.setObjectReferenceId(issue.getIssueId());
						avo.setDocumentAssetId(rs.getString(2));
						avo.setAssetType(AssetType.PDF_DOC);
						assets.add(avo);
						break;
					default:
						break;
				}
			}
		}
	}
	
	/**
	 * Grabs the category and the title and the author
	 * @param doc
	 */
	public void parseArticleTitle(MTSDocumentVO doc) {
		
		String title = doc.getActionName();
		int idx = title.indexOf('–');
		if (idx < 0) idx = title.indexOf('—');
		if (idx < 0) idx = title.indexOf('-');

		// Get the first section for the cat
		if (idx > -1) {
			String cat = StringUtil.removeNonAlphaNumeric(title.substring(0, idx), false).toLowerCase().trim();
			String catCode = catMap.get(cat);
			if (catCode != null) {
				DocumentCategoryVO dcat = new DocumentCategoryVO();
				dcat.setCategoryCode(catCode);
				dcat.setDocumentId(doc.getActionId());
				categories.add(dcat);
			} else {
				if (! StringUtil.isEmpty(cat)) missingCats.add(cat);
			}
		}
		
		// Get the title
		String pTitle = "";
		if (title.indexOf(" by ") > -1) {
			pTitle = title.substring(idx + 2);
			int eIdx = pTitle.lastIndexOf("by");
			if (eIdx > -1) {
				pTitle = pTitle.substring(0, eIdx).trim();
				
			}
		} else {
			pTitle = title;
		}
		
		doc.setActionName(StringUtil.removeNonAlphaNumeric(pTitle, false));

		// Get the author
		int aidx = title.lastIndexOf(" by ");
		if (aidx > -1) {
			String author = StringUtil.checkVal(title.substring(aidx + 4));
			doc.setAuthorId(userMap.get(author.trim().toLowerCase()));
		}
	}
	
	/**
	 * Migrate the issues for a given article
	 * @param srcConn
	 * @param destConn
	 * @throws SQLException
	 * @throws com.siliconmtn.db.util.DatabaseException
	 */
	public void migrateIssues(Connection srcConn, Connection destConn) 
	throws SQLException {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from wp_1fvbn80q5v_term_taxonomy a ");
		sql.append("inner join wp_1fvbn80q5v_terms b on a.term_id = b.term_id ");
		sql.append("where taxonomy = 'issue'  ");
		sql.append("order by a.term_id, name ");
		
		List<IssueVO> issues = new ArrayList<>();
		try (PreparedStatement ps = srcConn.prepareStatement(sql.toString()); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				IssueVO issue = new IssueVO();
				String name = StringUtil.checkVal(rs.getString("name"));
				
				int index = name.lastIndexOf('-');
				if (index == -1) index = name.indexOf(',', name.indexOf("No."));
				if (index > -1) {
					issue.setIssueDate(Convert.formatDate(Convert.DATE_FULL_MONTH, name.substring(index + 2)));
					issue.setCreateDate(issue.getIssueDate());
				}
				
				if (name.contains(",")) {
					String[] items = name.split("\\,");
					for (String item : items) {
						
						if (StringUtil.checkVal(item).toLowerCase().contains("vol.")) {
							issue.setVolume(StringUtil.removeNonNumeric(item));
						} else if (StringUtil.checkVal(item).toLowerCase().contains("no.")) {
							if (item.indexOf('-') > -1)
								item = StringUtil.removeNonNumeric(item.substring(0, item.indexOf('-')));
							issue.setNumber(StringUtil.removeNonNumeric(item));
						} 
					}
				}
				
				issue.setIssueId(rs.getString("term_id"));
				issue.setPublicationId(MTS_DOC_FOLDER);
				issue.setName(name);
				issue.setEditorId("MTS_USER_3");
				issue.setSeoPath(rs.getString("slug"));
				issue.setApprovalFlag(1);
				
				// Add the Issue Metadata
				getIssueMetaData(srcConn, issue);
				
				issues.add(issue);
			}
		}
		
		
		log.info("Number of issues: " + issues.size());
		DBProcessor db = new DBProcessor(destConn, CUSTOM_SCHEMA);
		
		try {
			db.executeBatch(issues, true);
		} catch (Exception e) {
			throw new SQLException(e);
		}
	}
	
	/**
	 * Pulls the categories from the WP DB and copies to WC
	 * @param srcConn
	 * @param destConn
	 * @throws SQLException
	 * @throws com.siliconmtn.db.util.DatabaseException
	 */
	public void migrateCategories(Connection srcConn, Connection destConn) 
	throws SQLException {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from wp_1fvbn80q5v_term_taxonomy a ");
		sql.append("inner join wp_1fvbn80q5v_terms b on a.term_id = b.term_id ");
		sql.append("where taxonomy = 'article_category' ");
		sql.append("order by name");
		
		List<CategoryVO> cats = new ArrayList<>();
		try (PreparedStatement ps = srcConn.prepareStatement(sql.toString()); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				CategoryVO cat = new CategoryVO();
				cat.setCategoryCode(rs.getString("term_id"));
				cat.setGroupCode("MARKETS");
				cat.setParentCode("MARKETS");
				cat.setSlug(rs.getString("slug"));
				cat.setDescription(rs.getString("description"));
				cat.setName(rs.getString("name"));
				
				cats.add(cat);
			}
			
			log.info("Number of entries: " + cats.size());
			DBProcessor db = new DBProcessor(destConn, CUSTOM_SCHEMA);
			try {
				db.executeBatch(cats, true);
			} catch (Exception e) {
				throw new SQLException(e);
			}
		}
	}
	
	/**
	 * Gets the connection to the MySQL Source
	 * @return
	 * @throws InvalidDataException 
	 * @throws DatabaseException 
	 */
	public Connection getSourceConnection() throws DatabaseException {
		DatabaseConnection dc = new DatabaseConnection();
		dc.setDriverClass("com.mysql.cj.jdbc.Driver");
		dc.setUrl("jdbc:mysql://playstation:3306/medtechinno?useUnicode=true&characterEncoding=UTF8&zeroDateTimeBehavior=convertToNull&useOldAliasMetadataBehavior=true");
		dc.setUserName("smtdev");
		dc.setPassword("smtrul3s");
		Connection conn = null;
		
		try {
			conn = dc.getConnection();
		} catch (Exception e) {
			throw new DatabaseException(e);
		}
		
		return conn;
	}

	/**
	 * Gets the connection to the MySQL Source
	 * @return
	 * @throws InvalidDataException 
	 * @throws DatabaseException 
	 */
	public Connection getDestConnection() throws DatabaseException {
		DatabaseConnection dc = new DatabaseConnection();
		dc.setDriverClass("org.postgresql.Driver");
		dc.setUrl("jdbc:postgresql://sonic:5432/webcrescendo_wsla5_sb?defaultRowFetchSize=25&amp;prepareThreshold=3");
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

