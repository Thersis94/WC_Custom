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

// WC Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;

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
		
		// Get the cats
		dmu.migrateCategories(srcConn, destConn);
		
		srcConn.close();
		destConn.close();
		log.info("Migration Completed");
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
		try (PreparedStatement ps = srcConn.prepareStatement(sql.toString())) {
			try (ResultSet rs = ps.executeQuery()) {
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

