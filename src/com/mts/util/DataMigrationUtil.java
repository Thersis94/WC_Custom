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
	private static final String CUSTOM_SCHEMA = "custom.";
	
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
		//dmu.migrateCategories(srcConn, destConn);

		// Migrate the issues
		//dmu.migrateIssues(srcConn, destConn);
		
		// Migrate the articles
		List<MTSDocumentVO> docs = dmu.migrateArticles(srcConn);
		for(MTSDocumentVO doc : docs) {
			DBProcessor db = new DBProcessor(destConn);
			// Assign the SB Action entry
			SBActionVO avo = new SBActionVO(doc);
			db.insert(avo);
			
			// Assign the wc document entry
			DocumentVO dvo = (DocumentVO)doc;
			db.insert(dvo);
			
			// Assign the MTS Document info
			db = new DBProcessor(destConn, CUSTOM_SCHEMA);
			db.insert(dvo);
			
			for (DocumentCategoryVO dcvo : doc.getCategories()) {
				db.insert(dcvo);
			}
		}
		
		
		log.info(dmu.missingCats.size());
		
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
	throws SQLException, com.siliconmtn.db.util.DatabaseException {
		StringBuilder sql = new StringBuilder(128);
		
		// Get the articles
		sql.append("select  * ");
		sql.append("from wp_1fvbn80q5v_posts where post_type = 'article' ");
		sql.append("limit 100 ");
		List<MTSDocumentVO> docs = new ArrayList<>();
		try (PreparedStatement ps = srcConn.prepareStatement(sql.toString()); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				
				MTSDocumentVO doc = new MTSDocumentVO();
				doc.setActionId(rs.getString("ID"));
				doc.setActionGroupId(rs.getString("ID"));
				doc.setDocumentId(rs.getString("ID"));
				doc.setDocumentSourceCode("CUSTOMER_IMPORT");
				doc.setDocument(rs.getString("post_content"));
				doc.setFileType("html");
				doc.setUniqueCode(RandomAlphaNumeric.generateRandom(6));
				doc.setOrganizationId("MTS");
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
				docs.add(doc);
			}
		}
		
		return docs;
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
				doc.addCategory(dcat);
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
	throws SQLException, com.siliconmtn.db.util.DatabaseException {
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
				issue.setPublicationId("MED_TECH_STRATEGIST");
				issue.setName(name);
				issue.setEditorId("MTS_USER_3");
				issue.setSeoPath(rs.getString("slug"));
				issue.setApprovalFlag(1);
				issues.add(issue);
				
				log.info("Number of issues: " + issues.size());
				DBProcessor db = new DBProcessor(destConn, CUSTOM_SCHEMA);
				db.executeBatch(issues, true);
			}
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
	throws SQLException, com.siliconmtn.db.util.DatabaseException {
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
			db.executeBatch(cats, true);
		}
	}
	
	/**
	 * Gets the connection to the MySQL Source
	 * @return
	 * @throws InvalidDataException 
	 * @throws DatabaseException 
	 */
	public Connection getSourceConnection() throws DatabaseException, InvalidDataException {
		DatabaseConnection dc = new DatabaseConnection();
		dc.setDriverClass("com.mysql.cj.jdbc.Driver");
		dc.setUrl("jdbc:mysql://playstation:3306/medtechinno?useUnicode=true&characterEncoding=UTF8&zeroDateTimeBehavior=convertToNull&useOldAliasMetadataBehavior=true");
		dc.setUserName("smtdev");
		dc.setPassword("smtrul3s");

		return dc.getConnection();
	}

	/**
	 * Gets the connection to the MySQL Source
	 * @return
	 * @throws InvalidDataException 
	 * @throws DatabaseException 
	 */
	public Connection getDestConnection() throws DatabaseException, InvalidDataException {
		DatabaseConnection dc = new DatabaseConnection();
		dc.setDriverClass("org.postgresql.Driver");
		dc.setUrl("jdbc:postgresql://sonic:5432/webcrescendo_wsla5_sb?defaultRowFetchSize=25&amp;prepareThreshold=3");
		dc.setUserName("ryan_user_sb");
		dc.setPassword("sqll0gin");

		return dc.getConnection();
	}
}

