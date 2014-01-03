package com.fastsigns.cutover;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: CenterAliasURLParser.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jan 3, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class CenterAliasURLParser {
	// Database Connection info
	private final String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	//private final String dbUrl = "jdbc:sqlserver://sql_server_db:1433;selectMethod=cursor;responseBuffering=adaptive";
	private final String dbUrl = "jdbc:sqlserver://192.168.56.101:31433;selectMethod=cursor;responseBuffering=adaptive";
	//private final String sbUser = "sb_user";
	private final String sbUser = "wc_user";
	private final String sbPass = "sqll0gin";
	//private String customDb = "sitebuilder_custom.dbo.";
	private String customDb = "wc_custom.dbo.";
	private Connection sbConn = null;
	
	/**
	 * 
	 */
	public CenterAliasURLParser() throws Exception {
		sbConn = this.getDBConnection(sbUser, sbPass);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		CenterAliasURLParser c = new CenterAliasURLParser();
		List<GenericVO> franchises = c.getFranchiseIds();
		System.out.println("Number of franchises: " + franchises.size());
		
		for (int i = 0; i < franchises.size(); i++) {
			GenericVO vo = franchises.get(i);
			String fid = vo.getKey().toString();
			String newid = vo.getValue().toString();
			
			System.out.println("Data: " + fid + "|" + newid);
			c.manageContent(fid, newid);
		}
	}
	
	/**
	 * 
	 * @param fId
	 * @param newid
	 */
	public void manageContent(String fid, String newid) throws SQLException {
		/*
		List<GenericVO> data = this.getContent(fid);
		for (int i=0; i < data.size(); i++) {
			GenericVO content = data.get(i);
			String newContent = this.parseContent(StringUtil.checkVal(content.getValue()), fid, newid);
			newContent = this.parseStoreId(newContent, fid);
			
			//System.out.println(newContent);
			//System.out.println("**************************");
			
			this.updateContent(new GenericVO(content.getKey(), newContent));
		}
		*/
		List<GenericVO> oData = this.getOptionContent(fid);
		for (int i=0; i < oData.size(); i++) {
			GenericVO content = oData.get(i);

			String newContent = this.parseTildeContent(StringUtil.checkVal(content.getValue()), fid, newid);
			//System.out.println(newContent);
			//System.out.println("**************************");

			this.updateOptionContent(new GenericVO(content.getKey(), newContent));
		}
	}
	
	public String parseTildeContent(String data, String fid, String newid) {
		String newData = data.replace("href='~/" + fid + "/", "href='/" + newid + "/");
		newData = newData.replace("href=\"~/" + fid + "/", "href=\"/" + newid + "/");

		return newData;
	}
	
	public String parseContent(String data, String fid, String newid) {
		String newData = data.replace("href='/" + fid, "href='/" + newid);
		newData = newData.replace("href=\"/" + fid, "href=\"/" + newid);
		
		return newData;
	}
	
	
	public String parseStoreId(String data, String fid) {
		StringBuilder pData = new StringBuilder(data);
		int start = 0;
		int loc = 0;
		while ((loc = pData.indexOf("/Request-A-Quote?storeid=", start)) > -1) {
			int begin = loc; // Start after the =
			int end = begin + 61;
			pData.replace(begin, end, "/request-a-quote?dealerLocationId=" + fid);
			start = end + 5;
			
		}
		
		return pData.toString();
	}
	
	/**
	 * 
	 * @param fid
	 * @return
	 * @throws SQLException
	 */
	public List<GenericVO> getContent(String fid) throws SQLException {
		String s = "select action_id, article_txt from content where organization_id = 'FTS_" + fid + "'";
		System.out.println("Content SQL: " + s);
		
		List<GenericVO> data = new ArrayList<GenericVO>();
		PreparedStatement ps = sbConn.prepareStatement(s);
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			data.add(new GenericVO(rs.getString(1), rs.getString(2)));
		}
		ps.close();
		return data;
	}
	
	/**
	 * 
	 * @param fid
	 * @return
	 * @throws SQLException
	 */
	public List<GenericVO> getOptionContent(String fid) throws SQLException {
		String s = "select cp_module_option_id, article_txt from ";
		s += customDb + "fts_cp_module_option where franchise_id = " + fid;
		System.out.println("Content SQL: " + s);
		
		List<GenericVO> data = new ArrayList<GenericVO>();
		PreparedStatement ps = sbConn.prepareStatement(s);
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			data.add(new GenericVO(rs.getString(1), rs.getString(2)));
		}
		ps.close();
		return data;
	}
	
	/**
	 * 
	 * @param vo
	 * @throws SQLException
	 */
	public void updateOptionContent(GenericVO vo) throws SQLException {
		String s = "update " + customDb + "fts_cp_module_option set article_txt = ? where cp_module_option_id = ?";
		System.out.println("Update SQL: " + s + "|" + vo.getKey());
		PreparedStatement ps = sbConn.prepareStatement(s);
		ps.setString(1, vo.getValue().toString());
		ps.setString(2, vo.getKey().toString());
		ps.executeUpdate();
		ps.close();
	}
	
	/**
	 * 
	 * @param vo
	 * @throws SQLException
	 */
	public void updateContent(GenericVO vo) throws SQLException {
		String s = "update content set article_txt = ? where action_id = ?";
		System.out.println("Update SQL: " + s + "|" + vo.getKey());
		PreparedStatement ps = sbConn.prepareStatement(s);
		ps.setString(1, vo.getValue().toString());
		ps.setString(2, vo.getKey().toString());
		ps.executeUpdate();
		ps.close();
	}
	
	/**
	 * 
	 * @return
	 * @throws SQLException
	 */
	public List<GenericVO> getFranchiseIds() throws SQLException {
		List<GenericVO> f = new ArrayList<GenericVO>();
		String s = "select franchise_id, new_franchise_id from " + customDb + "url_mapping ";//where franchise_id in (435)";
		PreparedStatement ps = sbConn.prepareStatement(s);
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			f.add(new GenericVO(rs.getString(1), rs.getString(2)));
		}
		
		ps.close();
		
		return f;
	}
	
	/**
	 * Creates a connection to the database
	 * @param dbUser Database User
	 * @param dbPass Database Password
	 * @return JDBC connection for the supplied user
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	private Connection getDBConnection(String dbUser, String dbPass) 
	throws InvalidDataException, DatabaseException  {
		DatabaseConnection dbc = new DatabaseConnection(dbDriver, dbUrl, dbUser, dbPass);
		return dbc.getConnection();
	}

}
