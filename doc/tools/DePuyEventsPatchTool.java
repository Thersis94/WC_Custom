package com.smt.sitebuilder.db;

import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.util.Convert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/********************************************************************
 * <b>Title</b>: EmailCampaignImport.java <br/>
 * Description: <br/> This highly specific class translates imported table data (from Access)
 * 				into the three tables for email campaign reporting.  The method calls in 
 * 				the main method need to run in series, see javadocs above each method. 
 * References: ~\My Documents\projects\Joint Replacement\ExactTarget-LIM emailCampaigns.mdb
 * Copyright: Copyright (c) 2007<br/>
 * Company: Silicon Mountain Technologies
 * @author James McKain
 * @version 1.0
 * @since Oct 3, 2007
 * Last Updated:
 *******************************************************************/

public class DePuyEventsPatchTool {

	private final String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private final String dbUrl = "jdbc:sqlserver://192.168.3.120:2007";
	private final String dbUser = "sb_user";
	private final String dbPass = "sqll0gin";
	private Connection conn = null;
	private ResultSet rs = null;
	private Map<String, Integer> totals = new HashMap<String, Integer>(22000,100L);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DePuyEventsPatchTool ec = new DePuyEventsPatchTool();
		ec.processCities();
		ec.totals = new HashMap<String, Integer>(26000,100L);
		ec.processZips();

	}
	
	/**
	 * establishes database connectivity
	 *
	 */
	public DePuyEventsPatchTool() {
		openDBConnection();
	}
	
	/**
	 * this method encrypts emailAddress and does a lookup to the profile table
	 * It then inserts the discovered profileId into the imported table, [user]
	 */
	public void processCities() {
		Integer cnt=0, total=0;
		
		//build a Map of all the lead counts by state|city (as key)
		String sql = "select count(*), upper(state_cd), upper(city_nm) from KNEE_POSTCARD_EVENTS_VIEW group by state_cd, city_nm";
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
			while (rs.next()) {
				totals.put(rs.getString(2)+"|"+rs.getString(3), rs.getInt(1));
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
			return;
		} finally {
			try {
				ps.close();
				ps = null;
			} catch (Exception e) {}
		}
		System.out.println("totals.size=" + totals.size());
		
		sql = "select event_lead_source_id, upper(state_cd), upper(city_nm) " +
		      "from SiteBuilder_custom.dbo.DEPUY_EVENT_LEADS_DATASOURCE where city_nm is not null";
		try {
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
			return;
		}
		
		try {
			while (rs.next()) {
				total++;
				System.out.println("updating " + rs.getString(2)+"|"+rs.getString(3));

				//reinsert the data with a est_leads_no value
				sql = "update SiteBuilder_custom.dbo.DEPUY_EVENT_LEADS_DATASOURCE set " +
					  "est_leads_no=? where event_lead_source_id=?";
				PreparedStatement ps2 = null;
				try {
					ps2 = conn.prepareStatement(sql);
					ps2.setInt(1, getLeadsCount(rs.getString(2), rs.getString(3)));
					ps2.setString(2, rs.getString(1));
					
					ps2.executeUpdate();
					cnt++;
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						ps2.close();
						ps2 = null;
					} catch (Exception e) {}
				}
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
			return;
		}
		System.out.println("Updated " + cnt + " of " + total + " records");
	}
	
	public void processZips() {
		Integer cnt=0, total=0;
		
		//build a Map of all the lead counts by state|city (as key)
		String sql = "select count(*), zip_cd from KNEE_POSTCARD_EVENTS_VIEW where zip_cd is not null group by zip_cd";
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
			while (rs.next()) {
				totals.put(rs.getString(2)+"|", rs.getInt(1));
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
			return;
		} finally {
			try {
				ps.close();
				ps = null;
			} catch (Exception e) {}
		}
		System.out.println("totals.size=" + totals.size());
		
		sql = "select event_lead_source_id, zip_cd " +
		      "from SiteBuilder_custom.dbo.DEPUY_EVENT_LEADS_DATASOURCE where zip_cd is not null";
		try {
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
			return;
		}
		
		try {
			while (rs.next()) {
				total++;
				System.out.println("updating " + rs.getString(2)+"|");

				//reinsert the data with a est_leads_no value
				sql = "update SiteBuilder_custom.dbo.DEPUY_EVENT_LEADS_DATASOURCE set " +
					  "est_leads_no=? where event_lead_source_id=?";
				PreparedStatement ps2 = null;
				try {
					ps2 = conn.prepareStatement(sql);
					ps2.setInt(1, getLeadsCount(rs.getString(2),""));
					ps2.setString(2, rs.getString(1));
					
					ps2.executeUpdate();
					cnt++;
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						ps2.close();
						ps2 = null;
					} catch (Exception e) {}
				}
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
			return;
		}
		System.out.println("Updated " + cnt + " of " + total + " records");
	}
	
	private Integer getLeadsCount(String stateCd, String cityNm) {
		Integer cnt = 0;
		try {
			cnt = Convert.formatInteger(totals.get(stateCd+"|"+cityNm));
		} catch (Exception e) {
			System.out.println("no mapped value for " + stateCd+"|"+cityNm);
		}
		
		return cnt;
	}
	
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
