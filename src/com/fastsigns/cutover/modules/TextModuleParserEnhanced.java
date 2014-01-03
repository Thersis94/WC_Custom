package com.fastsigns.cutover.modules;

// JDK 1.6.x
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

// FS Libs
import com.fastsigns.cutover.ContentParser;

// SMT Base Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: ContentParser.java <p/>
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
public class TextModuleParserEnhanced {
	private Connection conn = null;
	private static final String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private static final String dbUrl = "jdbc:sqlserver://sql_server_db:1433;selectMethod=cursor;responseBuffering=adaptive";
	private static final String dbUser = "sb_user";
	private static final String dbPass = "sqll0gin";
	
	private static final String dbFSUser = "fastsigns";
	private static final String dbFSPass = "fastsigns";
	ContentParser parser = null;
	
	public static final String TEXT_MODULE_ID = "edittextfsplc_lt_myfscentermodules_modules_ctl0";
	private static final Logger log = Logger.getLogger("TextModuleParserEnhanced");
	
	/**
	 * 
	 */
	public TextModuleParserEnhanced(Connection conn, Connection fsConn) {
		BasicConfigurator.configure();
		this.conn = conn;
		parser = new ContentParser(fsConn);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		DatabaseConnection dbc = new DatabaseConnection(dbDriver, dbUrl, dbUser, dbPass);
		DatabaseConnection fsDbc = new DatabaseConnection(dbDriver, dbUrl, dbFSUser, dbFSPass);
		TextModuleParserEnhanced cp = new TextModuleParserEnhanced(dbc.getConnection(), fsDbc.getConnection());
		cp.process();
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void process() throws Exception {
		List<Integer> franchises = this.getFranchiseData();
		int ctr = 1;
		for (int i=0; i < franchises.size(); i++) {
			Integer fId = franchises.get(i);
			Map<Integer, String> data = this.getContentList(fId);
			List<Integer> modLocation = this.getLocationModule(fId);
			
			log.debug("************* Results");
			Set<Integer> s = data.keySet();
			int j=0;
			for (Iterator<Integer> iter = s.iterator(); iter.hasNext(); j++) {
				Integer key = iter.next();
				String value = data.get(key);
				
				int modOptionId = 20000 + ctr++;
				
				// Insert the data into the module
				this.insertModuleOption(value, fId, modOptionId);
				
				// Get the mod location.  If there are more modules than elements,
				// Then we need to add the content and continue
				if (j >= modLocation.size()) continue;
				int modLocId = modLocation.get(j);
				
				// Insert the cross ref into the module franchise xr
				this.insertFranchiseModule(modLocId, modOptionId);
			}
		}
	}
	
	/**
	 * 
	 * @param content
	 * @param id
	 * @return
	 */
	public void insertModuleOption(String content, int fid, int modOptId) {
		String s = "insert into SiteBuilder_custom.dbo.FTS_CP_MODULE_OPTION (CP_MODULE_OPTION_ID, FTS_CP_MODULE_TYPE_ID, ";
		s += "OPTION_NM, ARTICLE_TXT, CREATE_DT, STANDARD_FLG, APPROVAL_FLG, FRANCHISE_ID) ";
		s += "values(?,?,?,?,?,?,?,?) ";
		
		PreparedStatement ps = null;
		try {
				ps = conn.prepareStatement(s);
				ps.setInt(1, modOptId);
				ps.setInt(2, 9);
				ps.setString(3, "Text Module");
				ps.setString(4, content);
				ps.setTimestamp(5, Convert.getCurrentTimestamp());
				ps.setInt(6, 0);
				ps.setInt(7, 1);
				ps.setInt(8, fid);
				
				ps.executeUpdate();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
	}
	
	/**
	 * 
	 */
	public void insertFranchiseModule(int modLocId, int modOptionId) {
		String s = "insert into sitebuilder_custom.dbo.fts_cp_module_franchise_xr ";
		s += "(cp_location_module_xr_id, cp_module_option_id, create_dt) values (?,?,?)";
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(s);
			ps.setInt(1, modLocId);
			ps.setInt(2, modOptionId);
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			
			ps.executeUpdate();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
	}
	
	/**
	 * Inserts the location module
	 * @param fid
	 * @param moduleId
	 * @param locationId
	 * @return
	 */
	public List<Integer> getLocationModule(int fid) {
		String s = "select CP_LOCATION_MODULE_XR_ID from SiteBuilder_custom.dbo.FTS_CP_LOCATION_MODULE_XR ";
		s += "where FRANCHISE_ID = ? and CP_MODULE_ID in (1,2) order by CP_LOCATION_ID  ";
		
		List<Integer> data = new ArrayList<Integer>(); 
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(s);
			ps.setInt(1, fid);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				data.add(rs.getInt(1));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		return data;
	}
	
	/**
	 * 
	 * @param val
	 * @return
	 */
	protected Map<Integer,String> parseContentText(String val) {
		StringBuilder sb = new StringBuilder(val);
		Map<Integer,String> data = new TreeMap<Integer,String>();
		int start = 0;
		int textLen = TEXT_MODULE_ID.length();
		
		while(true) {
			int loc = sb.indexOf(TEXT_MODULE_ID, start);
			if (loc == -1) break;
			
			//loc += 59;
			
			String content = sb.substring(loc, sb.indexOf("</webpart>", loc) - 3);
			int idLoc = content.indexOf(TEXT_MODULE_ID) + textLen;
			String order = content.substring(idLoc, idLoc + 1);
			log.debug("Order: " + order + "|" + Convert.formatInteger(order));
			
			content = content.substring(59);
			//log.debug("Content: " + content);
			data.put(Convert.formatInteger(order), parser.parseAll(content));
			
			loc += 7;
			start = loc;
		}
		
		return data;
	}

	/**
	 * 
	 * @return
	 * @throws SQLException
	 */
	public List<Integer> getFranchiseData() throws SQLException {
		String s = "Select franchise_id from fastsigns.dbo.custom_franchise a ";
		s+= "inner join SiteBuilder_custom.dbo.FTS_FRANCHISE b on a.StoreNumber = b.FRANCHISE_ID  ";
		s += "where franchise_id > 0 order by franchise_id";
		
		//System.out.println("Franchise Data: " + s);
		
		List<Integer> data = new ArrayList<Integer>();
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(s);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				data.add(rs.getInt(1));
			}
		} finally {
			try {
				ps.close();
			} catch(Exception e){}
		}
		
		return data;
	}
	
	/**
	 * 
	 * @return
	 */
	protected Map<Integer, String> getContentList(Integer fId) {
		String sql = "select * from fastsigns.dbo.cms_tree a ";
		sql += "inner join fastsigns.dbo.CMS_Document b on a.NodeID = b.DocumentNodeID ";
		sql += "where DocumentContent like '%" + TEXT_MODULE_ID + "%' ";
		sql += "and NodeAliasPath = '/Franchise/" + fId + "'  ";
		sql += "order by NodeAliasPath";
		
		PreparedStatement ps = null;
		Map<Integer, String> data = new LinkedHashMap<Integer,String>();
		try {
			ps = conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				String content = rs.getString("DocumentContent");
				data = this.parseContentText(content);
			} 
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return data;
	}

}
