package com.mts.util;

// JDK 1.8.x
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Log4j 1.2.17
import org.apache.log4j.Logger;

// MTS Libs
import com.mts.publication.data.CategoryVO;
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
	
	/**
	 * 
	 */
	public DataMigrationUtil() {
		super();
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
		dmu.migrateArticles(srcConn, destConn);

		srcConn.close();
		destConn.close();
		log.info("Migration Completed");
	}
	
	
	public void migrateArticles(Connection srcConn, Connection destConn) 
	throws SQLException, com.siliconmtn.db.util.DatabaseException {
		StringBuilder sql = new StringBuilder(128);
		
		// Get the articles
		sql.append("select  * ");
		sql.append("from wp_1fvbn80q5v_posts where post_type = 'article' ");
		sql.append("limit 10 ");
		List<MTSDocumentVO> docs = new ArrayList<>();
		try (PreparedStatement ps = srcConn.prepareStatement(sql.toString()); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				
				MTSDocumentVO doc = new MTSDocumentVO();
				doc.setActionId(rs.getString("ID"));
				doc.setActionGroupId(rs.getString("ID"));
				doc.setDocumentId(rs.getString("ID"));
				doc.setDocumentSourceCode("CUSTOMER_IMPORT");
				//doc.setDocument(rs.getString("post_content"));
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
				
				this.parseArticle(doc);
				docs.add(doc);
			}
		}
		
		// Assign the SB Action entry
		
		
		// Assign the wc document entry
		
		
		// Assign the MTS Document info
		
		
		// Assign the categories
		
		
	}
	
	
	public void parseArticle(MTSDocumentVO doc) {
		log.info(doc.getActionName());
		String title = doc.getActionName();
		int idx = title.indexOf('–');
		if (idx < 0) idx = title.indexOf('-');
		
		// Get the first section for the cat
		String cat = "";
		if (idx > -1) cat = StringUtil.removeNonAlphaNumeric(title.substring(0, idx), false);
		
		// Get the title
		if (title.contains("“") || title.contains("\"")) {
			int lastIdx = title.lastIndexOf('"') > -1 ? title.lastIndexOf('"') : title.lastIndexOf('“');
			String artTitle = title.substring(idx, title.lastIndexOf('"'));
			log.info("*** " + artTitle);
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
				DBProcessor db = new DBProcessor(destConn, "custom.");
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
				cat.setGroupCode(rs.getString("term_id"));
				cat.setSlug(rs.getString("slug"));
				cat.setDescription(rs.getString("description"));
				cat.setName(rs.getString("name"));
				
				cats.add(cat);
			}
			
			log.info("Number of entries: " + cats.size());
			DBProcessor db = new DBProcessor(destConn, "custom.");
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

