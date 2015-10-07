package com.fastsigns.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.fastsigns.action.franchise.vo.CenterModuleOptionVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;

public class CopyGlobalOptions {
	protected static String DESTINATION_DB_URL = "jdbc:sqlserver://localhost:1433";
	protected static String DESTINATION_DB_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	protected static String[] DESTINATION_AUTH = new String[] {
			"wc_user", "sqll0gin" };
	protected static final Logger log = Logger.getLogger(CopyGlobalOptions.class);
	final String customDb = "WebCrescendo_fs_custom.dbo.";
	final String srcOrgId = "FTS_UK";
	final String destOrgId = "FTS_SA";

	protected Connection dbConn = null;

	public CopyGlobalOptions() {
		PropertyConfigurator.configure("C:/Software/log4j.properties");

	}

	public static void main(String[] args) {

		CopyGlobalOptions cgo = new CopyGlobalOptions();
		cgo.execute();
	}

	public void execute() {
		ArrayList<CenterModuleOptionVO> opts = new ArrayList<CenterModuleOptionVO>();
		CenterModuleOptionVO temp = null;
		int count = 0;
		try {
			dbConn = getDBConnection(DESTINATION_AUTH[0], DESTINATION_AUTH[1],
					DESTINATION_DB_DRIVER, DESTINATION_DB_URL);
			String sql = getSql();
			PreparedStatement ps = null;
			try {
				ps = dbConn.prepareStatement(sql);
				ps.setString(1, srcOrgId);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					temp = new CenterModuleOptionVO(rs);
					temp.setModuleOptionId(new UUIDGenerator().getUUID());
					opts.add(temp);
				}
			} catch (SQLException e) {
				log.debug(e);
			}
			log.debug("Read in " + opts.size() + " entries, preparing to insert to database.");
			ps = null;
			sql = insertSql();
			try {
				ps = dbConn.prepareStatement(sql);
				for (CenterModuleOptionVO vo : opts) {
					int i = 0;
					ps.setString(++i, vo.getOptionName());
					ps.setString(++i, vo.getOptionDesc());
					ps.setString(++i, vo.getArticleText());
					ps.setInt(++i, vo.getRankNo());
					ps.setString(++i, vo.getLinkUrl());
					ps.setString(++i, vo.getFilePath());
					ps.setString(++i, vo.getThumbPath());
					ps.setString(++i, vo.getStillFramePath());
					ps.setString(++i, vo.getContentPath());
					ps.setDate(++i, Convert.formatSQLDate(vo.getStartDate()));
					ps.setDate(++i, Convert.formatSQLDate(vo.getEndDate()));
					ps.setTimestamp(++i, Convert.getCurrentTimestamp());
					ps.setNull(++i, java.sql.Types.INTEGER);
					ps.setInt(++i, vo.getModuleTypeId());
					ps.setInt(++i, vo.getApprovalFlag());
					ps.setString(++i, vo.getParentId());
					ps.setString(++i, destOrgId);
					ps.setString(++i, vo.getModuleOptionId());
					ps.addBatch();
					count++;
				}
				ps.executeBatch();
			} catch (Exception e) {
				log.debug(e);
			} finally {
				try {
					dbConn.close();
				} catch (Exception e) {
				}
			}
		} catch (DatabaseException e) {
			log.debug(e);
		}
		log.debug("inserted " + count + " records to the database.");
	}

	private String insertSql() {
		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(customDb);
		sb.append("FTS_CP_MODULE_OPTION (OPTION_NM, ");
		sb.append("OPTION_DESC, ARTICLE_TXT, RANK_NO, LINK_URL, FILE_PATH_URL, THUMB_PATH_URL, ");
		sb.append("VIDEO_STILLFRAME_URL, CONTENT_PATH_TXT, START_DT, END_DT, CREATE_DT,");
		sb.append("FRANCHISE_ID, FTS_CP_MODULE_TYPE_ID, APPROVAL_FLG, PARENT_ID, ORG_ID, CP_MODULE_OPTION_ID) ");
		sb.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		return sb.toString();
	}

	private String getSql() {
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ")
				.append(customDb)
				.append("FTS_CP_MODULE_OPTION where ORG_ID=? and franchise_ID is null");
		return sb.toString();
	}

	/**
	 * 
	 * @param userName
	 *            Login Account
	 * @param pwd
	 *            Login password info
	 * @param driver
	 *            Class to load
	 * @param url
	 *            JDBC URL to call
	 * @return Database Conneciton object
	 * @throws DatabaseException
	 */
	protected Connection getDBConnection(String userName, String pwd,
			String driver, String url) throws DatabaseException {
		// Load the Database jdbc driver
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException cnfe) {
			throw new DatabaseException("Unable to find the Database Driver",
					cnfe);
		}

		Connection conn = null;
		try {
			conn = DriverManager.getConnection(url, userName, pwd);
		} catch (SQLException sqle) {
			sqle.printStackTrace(System.out);
			throw new DatabaseException("Error Connecting to Database", sqle);
		}

		return conn;
	}

	protected void closeConnection(Connection conn) {
		try {
			conn.close();
		} catch (Exception e) {
		}
	}
}
