package com.fastsigns.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.fastsigns.action.franchise.vo.CenterModuleOptionVO;
import com.fastsigns.action.franchise.vo.OptionAttributeVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.util.Convert;

public class CopyCPOptionAttributes {
	protected static String DESTINATION_DB_URL = "jdbc:sqlserver://localhost:1433";
	protected static String DESTINATION_DB_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	protected static String[] DESTINATION_AUTH = new String[] {
			"wc_user", "sqll0gin" };
	protected static final Logger log = Logger.getLogger(CopyGlobalOptions.class);
	final String customDb = "WebCrescendo_fs_custom.dbo.";
	final String srcOrgId = "FTS_UK";
	final String destOrgId = "FTS_SA";

	protected Connection dbConn = null;

	public CopyCPOptionAttributes() {
		PropertyConfigurator.configure("C:/Software/log4j.properties");

	}

	public static void main(String[] args) {

		CopyCPOptionAttributes cgo = new CopyCPOptionAttributes();
		cgo.execute();
	}

	public void execute() {
		ArrayList<CenterModuleOptionVO> opts = new ArrayList<CenterModuleOptionVO>();
		CenterModuleOptionVO temp = null;
		int count = 0;
		String orgId = null;
		Map<Integer, Integer> options = new HashMap<Integer, Integer>();
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
					if(orgId == null || !orgId.equals(rs.getString("CP_MODULE_OPTION_ID"))){
						if(orgId != null){
							opts.add(temp);						}
						temp = new CenterModuleOptionVO(rs);
						temp.addAttribute(rs.getString("cp_option_attr_id"), rs.getString("attr_key_cd"), rs.getString("attrib_value_txt"), rs.getInt("attr_order_no"));	
						orgId = rs.getString("CP_MODULE_OPTION_ID");
					}
					else{
						temp.addAttribute(rs.getString("cp_option_attr_id"), rs.getString("attr_key_cd"), rs.getString("attrib_value_txt"), rs.getInt("attr_order_no"));	
					}
				}
				opts.add(temp);
			} catch (SQLException e) {
				log.debug(e);
			}
			log.debug("Read in " + opts.size() + " entries, preparing to insert to database.");
			
			ps = null;
			sql = getSql2();
				try {
					ps = dbConn.prepareStatement(sql);
					ps.setString(1, srcOrgId);
					ps.setString(2, destOrgId);
					ResultSet rs = ps.executeQuery();
					while (rs.next()) {
						options.put(rs.getInt("new_cp_id"), rs.getInt("old_cp_id"));
					}
				} catch (SQLException e) {
					log.debug(e);
				}
			log.debug("Retrieved " + options.size() + " Options to update attributes for.");
			ps = null;
			sql = insertSql();
			try {
				ps = dbConn.prepareStatement(sql);
				for (CenterModuleOptionVO vo : opts) {
					int pos = 1;
					for(OptionAttributeVO v : vo.getAttributes()){
						ps.setInt(1, options.get(vo.getModuleOptionId()));
						ps.setString(2, (String)v.getKey());
						ps.setString(3, (String)v.getValue());
						ps.setInt(4, pos);
						ps.setTimestamp(5, Convert.getCurrentTimestamp());
						ps.setInt(6, 1);
						ps.addBatch();
						count++;
						pos++;
					}
				}
				ps.executeBatch();
			} catch (Exception e) {
				e.printStackTrace();
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
		sb.append("FTS_CP_OPTION_ATTR (CP_MODULE_OPTION_ID, ");
		sb.append("ATTR_KEY_CD, ATTRIB_VALUE_TXT, ORDER_NO, CREATE_DT, ACTIVE_FLG) ");
		sb.append("values (?,?,?,?,?,?)");
		return sb.toString();
	}
	
	private String getSql2(){
		StringBuilder sb = new StringBuilder();
		sb.append("select a.CP_MODULE_OPTION_ID as new_cp_id, b.CP_MODULE_OPTION_ID as old_cp_id from ")
		.append(customDb).append("FTS_CP_MODULE_OPTION a inner join ").append(customDb).append("FTS_CP_MODULE_OPTION b ")
		.append("on a.OPTION_NM = b.OPTION_NM and a.ORG_ID= ? where b.ORG_ID= ? and b.FTS_CP_MODULE_TYPE_ID = 10");
		return sb.toString();
	}

	private String getSql() {
		StringBuilder sb = new StringBuilder();
		sb.append("select *, a.order_no as attr_order_no from ")
				.append(customDb)
				.append("FTS_CP_OPTION_ATTR a inner join ")
				.append(customDb)
				.append("FTS_CP_MODULE_OPTION b on a.CP_MODULE_OPTION_ID = b.CP_MODULE_OPTION_ID ")
				.append("and b.FTS_CP_MODULE_TYPE_ID = 10 and b.ORG_ID = ?");
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
