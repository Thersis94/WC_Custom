package com.fastsigns.cutover.modules;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ModuleOptionBinaryMapper.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Nov 12, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class ModuleOptionBinaryMapper {
	private Connection conn = null;
	private static final String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private static final String dbUrl = "jdbc:sqlserver://sql_server_db:1433;selectMethod=cursor;responseBuffering=adaptive";
	private static final String dbUser = "fastsigns";
	private static final String dbPass = "fastsigns";
	
	/**
	 * 
	 */
	public ModuleOptionBinaryMapper(Connection conn) {
		this.conn = conn;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("Starting ...");
		DatabaseConnection dbc = new DatabaseConnection(dbDriver, dbUrl, dbUser, dbPass);
		ModuleOptionBinaryMapper mobm = new ModuleOptionBinaryMapper(dbc.getConnection());
		//mobm.updateFilePathData();
		mobm.updatePathData("VIDEO_STILLFRAME_URL ");
		mobm.updatePathData("file_path_url ");
		mobm.updatePathData("thumb_path_url ");
		mobm.updateCustomFranchise();
	}
	
	/**
	 * 
	 * @throws SQLException
	 */
	public void updateFilePathData() throws SQLException {
		String s = "select * ";
		s += "from Sitebuilder_custom.dbo.FTS_CP_MODULE_OPTION a ";
		s += "inner join fastsigns.dbo.CMS_Tree b on a.FILE_PATH_URL = b.NodeGUID  ";
		s += "inner join fastsigns.dbo.CMS_Document c on b.NodeId = c.DocumentNodeId ";
		s += "where CP_MODULE_OPTION_ID >= 4000 and CP_MODULE_OPTION_ID < 7000  ";
		s+= "and FILE_PATH_URL not like '/%'  ";
		System.out.println("FP SQL: " + s);
		
		PreparedStatement ps = conn.prepareStatement(s);
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			int id = rs.getInt("cp_module_option_id"); 
			String loc = rs.getString("NodeAliasPath");
			String folder = loc.substring(0, loc.lastIndexOf("/") + 1);
			folder = this.parseFolder(folder);
			
			String file = rs.getString("NodeName");
			if (file.indexOf(".f4v") == -1) {
				String dt = StringUtil.checkVal(rs.getString("DocumentType"));
				if (dt.length() == 0) {
					file = "";
					folder = "";
				} else {
					file += dt;
				}
			}
			
			// Update the DB
			this.updateOption(id, "file_path_url", folder + file);
		}
	}
	
	/**
	 * 
	 * @throws SQLException
	 */
	public void updatePathData(String field) throws SQLException {
		String s = "select * ";
		s += "from Sitebuilder_custom.dbo.FTS_CP_MODULE_OPTION a ";
		s += "inner join fastsigns.dbo.CMS_Tree b on a." + field + " = b.NodeGUID ";
		s += "inner join fastsigns.dbo.CMS_Document c on b.NodeId = c.DocumentNodeId  ";
		s += "where len(" + field + ") = 36 and " + field + " != '00000000-0000-0000-0000-000000000000'  ";
		s += "and " + field + " not like 'http%'  ";
		
		System.out.println("FP SQL: " + s);
		
		PreparedStatement ps = conn.prepareStatement(s);
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			int id = rs.getInt("cp_module_option_id"); 
			String loc = rs.getString("NodeAliasPath");
			String folder = loc.substring(0, loc.lastIndexOf("/") + 1);
			folder = this.parseFolder(folder);
			
			String file = rs.getString("NodeName");
			if (file.indexOf(".f4v") == -1) {
				String dt = StringUtil.checkVal(rs.getString("DocumentType"));
				if (dt.length() == 0) {
					file = "";
					folder = "";
				} else {
					file += dt;
				}
			}
			
			// Update the DB
			this.updateOption(id, field, folder + file);
		}
	}
	
	public String parseFolder(String fldr) {
		if (fldr.startsWith("/Uploaded-Files")) fldr = "/binary/org/FTS" + fldr;
		
		if (fldr.startsWith("/Franchise/Template-Assets/")) {
			fldr = "/binary/org/FTS" + fldr.substring(10);
		}
		
		if (fldr.startsWith("/Franchise/")) {
			fldr = "/binary/org/FTS_" + fldr.substring(11);
		}
		
		return fldr;
	}
	
	public void updateOption(int id, String field, String path) throws SQLException {
		String s = "update Sitebuilder_custom.dbo.fts_cp_module_option ";
		s += "set " + field + " = ? where cp_module_option_id = ?";
		System.out.println("Update SQL: " + s + "|" + field + "|" + path + "|" + id);
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(s);
			ps.setString(1, path);
			ps.setInt(2, id);
			ps.executeUpdate();
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
	}
	
	
	public void updateCustomFranchise() throws SQLException {
		String s = "select * from Sitebuilder_custom.dbo.fts_cp_module_option";
		String upSql = "update Sitebuilder_custom.dbo.fts_cp_module_option ";
		upSql += "set FRANCHISE_ID = ? where cp_module_option_id = ?";
		
		PreparedStatement ps = null;
		PreparedStatement psUp = null;
		try {
			ps = conn.prepareStatement(s);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String val = rs.getString("file_path_url");
				if (val != null && val.indexOf("/FTS_") > -1) {
					int start = val.indexOf("/FTS_") + 5;
					int fId = Convert.formatInteger(val.substring(start, val.indexOf("/", start)));
					System.out.println("FranchiseID: " + fId);
					if (fId > 0) {
						psUp = conn.prepareStatement(upSql);
						psUp.setInt(1, fId);
						psUp.setInt(2, rs.getInt("cp_module_option_id"));
						psUp.executeUpdate();
					}
				}
			}
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
			
			try {
				psUp.close();
			} catch(Exception e) {}
		}
	}
}
