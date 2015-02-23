package com.smt.sitebuilder.db;

import com.siliconmtn.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

/********************************************************************
 * <b>Title</b>: CatalogBrandKeywordImporter.java <br/>
 * Description: <br/> This one-off class was built to batch-insert an Excel file of
 * product brands and their keywords into the <orgName>_ProdCatalog database
 * Copyright: Copyright (c) 2008<br/>
 * Company: Silicon Mountain Technologies
 * @author James McKain
 * @version 1.0
 * @since May 20, 2008
 * Last Updated:
 *******************************************************************/

public class CatalogBrandKeywordImporter {

	private final String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private final String dbUrl = "jdbc:sqlserver://10.0.20.43:2007";
	private final String dbUser = "sb_user";
	private final String dbPass = "sqll0gin";
	private Connection conn = null;
	private ResultSet rs = null;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CatalogBrandKeywordImporter ec = new CatalogBrandKeywordImporter();
		
		//run this method first to parse the keywords and populate the keyword table
		ec.runFirst();
		
		/* run this query against the DB before executing "runSecond()".
		 * This query assigns prod_brand_id to the brand names in the brand_tmp table
		 *
		   	UPDATE brand_tmp
		    SET prod_brand_id=b.prod_brand_id
		    FROM brand_tmp a 
			inner join prod_brand b on a.prod_brand_txt=b.prod_brand_txt
			inner join prod_category c on b.prod_category_id=c.prod_category_id and a.prod_category_txt=c.prod_category_txt
			inner join prod_line d on c.prod_line_id=d.prod_line_id and a.prod_line_txt=d.prod_line_txt
		 */
		
		//run this lastly to populate the associative table between prod_brand and keyword
		//ec.runSecond();

	}
	
	/**
	 * establishes database connectivity
	 *
	 */
	public CatalogBrandKeywordImporter() {
		openDBConnection();
	}
	
	/**
	 * This method reads all the rows in brand_tmp and iterates each's comma-delimited
	 * list of keywords; storing the values on a Map to ensure zero duplicates.  
	 * The unique keywords are then inserted into the keyword table in the DB.
	 */
	public void runFirst() {
		Integer cnt=0;
		String sql = "select KEYWORD_TXT from Codman_ProdCatalog.dbo.brand_tmp where keyword_txt is not null";
		Map<String, Integer> kywdsMap = new HashMap<String, Integer>();
		
		//load the keywords into a Map by parsing the comma-delimited values
		try {
			PreparedStatement ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
			while (rs.next()) {
				StringTokenizer st = new StringTokenizer(rs.getString("KEYWORD_TXT"),",");
				while (st.hasMoreTokens()) {
					String kywd = st.nextToken().trim();
					if (kywd.length() > 0 && !kywdsMap.containsKey(kywd))
						kywdsMap.put(kywd, 0);
				}
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
			return;
		}
		
		//insert the unique keywords into the DB
		Iterator<String> iter = kywdsMap.keySet().iterator();
		while (iter.hasNext()) {
			sql = "insert into Codman_ProdCatalog.dbo.keyword (KEYWORD_TXT) values (?)";
			try {
				PreparedStatement ps2 = conn.prepareStatement(sql);
				ps2.setString(1, iter.next().trim());
				ps2.executeUpdate();
				cnt++;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("Added " + cnt + " unique keywords to the system");
	}
	
	
	/**
	 * This method reads all keywords from the database into a Map.  It the reads
	 * all the rows in brand_tmp and iterates each's comma-delimited list of keywords,
	 * compares them to the values in the Map, and inserts to two primary keys into
	 * our associative table keyword_assoc
	 */
	public void runSecond() {
		Integer cnt=0, totalBrands=0;
		String sql = "select KEYWORD_ID, KEYWORD_TXT from Codman_ProdCatalog.dbo.keyword";
		Map<String, Integer> kywdsMap = new HashMap<String, Integer>();
		
		//read all the keywords and populate our Map
		try {
			PreparedStatement ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
			while (rs.next()) {
				kywdsMap.put(rs.getString("KEYWORD_TXT").trim(), rs.getInt("KEYWORD_ID"));
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
			return;
		}
		System.out.println(kywdsMap.size() + " keywords loaded");
		
		//read all the brands, match with keywordIds, and call the associative insert
		sql = "select PROD_BRAND_ID, KEYWORD_TXT from Codman_ProdCatalog.dbo.brand_tmp";
		try {
			PreparedStatement ps2 = conn.prepareStatement(sql);
			ResultSet rs2 = ps2.executeQuery();
			while (rs2.next()) {
				Integer prodBrandId = rs2.getInt("PROD_BRAND_ID");
				StringTokenizer st = new StringTokenizer(rs2.getString("KEYWORD_TXT"),",");
				while (st.hasMoreTokens()) {
					String kywd = st.nextToken().trim();
					if (kywdsMap.containsKey(kywd)) {
						addKeywordAssoc(prodBrandId, kywdsMap.get(kywd));
						cnt++;
					}
				}
				++totalBrands;
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		System.out.println("Associated " + cnt + " keywords to " + totalBrands + " brands");
	}
	
	/** 
	 * adds the brand/keyword associative-table relationship
	 * @param prodBrandId
	 * @param keywordId
	 */
	private void addKeywordAssoc(int prodBrandId, int keywordId) {
		String sql = "insert into Codman_ProdCatalog.dbo.keyword_assoc " +
					 "(KEYWORD_ID, PROD_BRAND_ID) values (?,?)";
		try {
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setInt(1, keywordId);
			ps.setInt(2, prodBrandId);
			ps.execute();
			
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
	}
	
	/**
	 * connects the database
	 */
	private void openDBConnection() {
		DatabaseConnection dbc = new DatabaseConnection(dbDriver, dbUrl, dbUser, dbPass);
		try {
			conn = dbc.getConnection();
		} catch (Exception de) {
			de.printStackTrace();
			System.exit(-1);
		}
	}

}
