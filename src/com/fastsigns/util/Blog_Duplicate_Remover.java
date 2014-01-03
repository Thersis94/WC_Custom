package com.fastsigns.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.blog.BlogVO;

public class Blog_Duplicate_Remover {
	protected static String DESTINATION_DB_URL = "jdbc:sqlserver://localhost:1433";
	protected static String DESTINATION_DB_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	protected static String[] DESTINATION_AUTH = new String[] {
			"wc_user", "sqll0gin" };
	protected static final Logger log = Logger.getLogger(CopyGlobalOptions.class);
	final String customDb = "sitebuilder_fs.dbo.";
	final String actionId = "7f000101886cb77081d6cfc22d97ba1b";

	protected Connection dbConn = null;

	public Blog_Duplicate_Remover() {
		PropertyConfigurator.configure("C:/Software/log4j.properties");

	}

	public static void main(String[] args) {

		Blog_Duplicate_Remover bdr = new Blog_Duplicate_Remover();
		bdr.execute();
	}

	public void execute() {
		Map<String, BlogVO> blogs = new HashMap<String, BlogVO>();
		//Map<String, BlogVO> updated = new HashMap<String, BlogVO>();
		BlogVO blog = null;
		int i = 0;
		int count = 0;
		try {
			dbConn = getDBConnection(DESTINATION_AUTH[0], DESTINATION_AUTH[1],
					DESTINATION_DB_DRIVER, DESTINATION_DB_URL);
			dbConn.setAutoCommit(false);
			String sql = getSql();
			PreparedStatement ps = null;
				ps = dbConn.prepareStatement(sql);
				ps.setString(1, actionId);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					i++;
					blog = new BlogVO(rs);
					//if(blog.getUpdateDate() != null)
					//	updated.put(blog.getTitle() + blog.getCreateDate(), blog);
					//else
						blogs.put(blog.getTitle() + blog.getCreateDate(), blog);
				}
			//log.debug("Read in " + updated.size() + " updated entries.");
			//blogs.putAll(updated);
			log.debug("Read in " + i + " entries. Weeded down to " + blogs.size());
			
			ps = null;
			sql = deleteSql();
			ps = dbConn.prepareStatement(sql);
			ps.setString(1, actionId);
			ps.executeUpdate();
			
			sql = insertSql();
				ps = dbConn.prepareStatement(sql);
				Iterator<BlogVO> iter = blogs.values().iterator();
				 while(iter.hasNext()){
					int ctr = 1;
					blog = iter.next();
					ps.setString(ctr++, blog.getTitle());
					ps.setString(ctr++, blog.getAuthor().getBloggerId());
					ps.setString(ctr++, blog.getActionId());
					ps.setString(ctr++, blog.getBlogText());
					ps.setString(ctr++, blog.getUrl());
					ps.setInt(ctr++, blog.getApprovalFlag());
					ps.setDate(ctr++, Convert.formatSQLDate(blog.getPublishDate()));
					ps.setTimestamp(ctr++, Convert.getTimestamp(blog.getCreateDate(), false));
					ps.setTimestamp(ctr++, Convert.getTimestamp(blog.getUpdateDate(), false));
					ps.setInt(ctr++, blog.getHasImages() ? 1 : 0);
					ps.setInt(ctr++, blog.getHasVideos() ? 1 : 0);
					ps.setString(ctr++, blog.getShortDesc());
					ps.setString(ctr++, blog.getBrowserTitle());
					ps.setString(ctr++, blog.getThumbUrl());
					ps.setString(ctr++, blog.getBlogId());
					ps.executeUpdate();
					count++;
				}
				log.debug("inserted " + count + " records to the database.");

				//ps.executeBatch();
				dbConn.commit();
				dbConn.setAutoCommit(true);

		} catch (DatabaseException e) {
			log.debug(count, e);
			try {
				dbConn.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		} catch (SQLException e) {
			log.debug(count, e);
			try {
				dbConn.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
	}

	private String insertSql() {
		StringBuilder sb = new StringBuilder();
		sb.append("insert into blog (title_nm,  blogger_id, ");
		sb.append("action_id, blog_txt, blog_url, approval_flg, publish_dt, ");
		sb.append("create_dt, update_dt, images_flg, video_flg, short_desc_txt, browser_title, ");
		sb.append("thumb_url, blog_id )values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		return sb.toString();
	}
	
	private String deleteSql(){
		StringBuilder sb = new StringBuilder();
		sb.append("delete from BLOG where ACTION_ID = ?");
		return sb.toString();
	}

	private String getSql() {
		StringBuilder sb = new StringBuilder();
		sb.append("select * from BLOG where ACTION_ID = ? order by CREATE_DT, UPDATE_DT ");
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
