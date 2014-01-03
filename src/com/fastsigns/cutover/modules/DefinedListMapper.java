package com.fastsigns.cutover.modules;

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
import java.util.StringTokenizer;

import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

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
public class DefinedListMapper {
	private Connection conn = null;
	private static final String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private static final String dbUrl = "jdbc:sqlserver://sql_server_db:1433;selectMethod=cursor;responseBuffering=adaptive";
	private static final String dbUser = "fastsigns";
	private static final String dbPass = "fastsigns";
	
	/**
	 * 
	 */
	public DefinedListMapper(Connection conn) {
		this.conn = conn;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		DatabaseConnection dbc = new DatabaseConnection(dbDriver, dbUrl, dbUser, dbPass);
		DefinedListMapper dlm = new DefinedListMapper(dbc.getConnection());
		Map<Integer, List<Integer>> dlm1 = dlm.retrieveData("DefinedListModule1 ", 14, 11000);
		dlm.insertData(dlm1);
		Map<Integer, List<Integer>> dlm2 = dlm.retrieveData("DefinedListModule2", 15, 12000);
		dlm.insertData(dlm2);
		Map<Integer, List<Integer>> dlm3 = dlm.retrieveData("DefinedListModule3", 16, 13000);
		dlm.insertData(dlm3);
	}
	
	public void insertData(Map<Integer, List<Integer>> data) throws SQLException {
		String s = "insert into Sitebuilder_custom.dbo.fts_cp_module_franchise_xr ";
		s += "(cp_location_module_xr_id, cp_module_option_id, create_dt) values (?,?,?)";
		PreparedStatement ps = conn.prepareStatement(s);
		
		Set<Integer> keys = data.keySet();
		for (Iterator<Integer> iter = keys.iterator(); iter.hasNext(); ) {
			Integer key = iter.next();
			List<Integer> options = data.get(key);
			for (int i=0; i < options.size(); i++) {
				Integer option = options.get(i);
				ps.setInt(1, key);
				ps.setInt(2, option);
				ps.setTimestamp(3, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			int[] count = ps.executeBatch();
			System.out.println("Number updated: " + count.length);
		}

	}
	
	
	public Map<Integer, List<Integer>> retrieveData(String fieldName, int modId, int offset) 
	throws SQLException {
		StringBuilder s = new StringBuilder();
		s.append("select ").append(fieldName).append(", CP_LOCATION_MODULE_XR_ID , StoreNumber ");
		s.append("from custom_franchise a ");
		s.append("inner join SiteBuilder_custom.dbo.FTS_CP_LOCATION_MODULE_XR b ");
		s.append("on a.StoreNumber = b.FRANCHISE_ID ");
		s.append("where LEN(?) > 0 and CP_MODULE_ID in (?)  ");
		System.out.println("SQL: " + s);
		
		PreparedStatement stmt = conn.prepareStatement(s.toString());
		stmt.setString(1, fieldName);
		stmt.setInt(2, modId);
		
		ResultSet rs = stmt.executeQuery();
		Map<Integer, List<Integer>> data = new LinkedHashMap<Integer, List<Integer>>();
		System.out.println("List Mod ID: " + fieldName);
		while (rs.next()) {
			List<Integer> options = parsePipes(rs.getString(1), offset);
			if (options.size() > 0)
				data.put(rs.getInt(2), options);
		}
		
		return data;
	}
	
	
	public List<Integer> parsePipes(String data, int offset) {
		List<Integer> coll = new ArrayList<Integer>();
		if (StringUtil.checkVal(data).length() == 0) return coll;
		
		StringTokenizer st = new StringTokenizer(data, "|");
		while (st.hasMoreElements()) {
			Integer val = Convert.formatInteger(st.nextToken()) + offset;
			coll.add(val);
		}
		
		return coll;
	}
	
}
