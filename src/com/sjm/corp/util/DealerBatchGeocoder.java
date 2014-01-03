package com.sjm.corp.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;


import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.GoogleGeocoderV3;
import com.siliconmtn.gis.Location;
import com.siliconmtn.gis.MatchCode;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;

/****************************************************************************
 * <b>Title</b>: DealerBatchGeocoder.java <p/>
 * <b>Project</b>: SB_ANS_Medical <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Mar 7, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class DealerBatchGeocoder {
	// Database Connection info
	private final String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private final String dbUrl = "jdbc:sqlserver://sql_server_db:1433;selectMethod=cursor;responseBuffering=adaptive";
	private final String dbUser = "sb_user";
	private final String dbPass = "sqll0gin";
	
	/**
	 * 
	 */
	public DealerBatchGeocoder() {
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		DealerBatchGeocoder dbg = new DealerBatchGeocoder();
		Connection conn = dbg.getDBConnection();
		dbg.process(conn);
		conn.close();
	}
	
	public void process(Connection conn) throws Exception {
		// Get the entries to be geocoded
		BasicConfigurator.configure();
		List<DealerLocationVO> locs = this.getLocations(conn);
		System.out.println("Number of entries: " + locs.size());
		
		//Geocode and update the record
		for (int i=0; i < locs.size(); i++) {
			this.updateLocation(locs.get(i), conn);
		}
	}
	
	/**
	 * 
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	public List<DealerLocationVO> getLocations(Connection conn) throws SQLException {
		String s = "select * from dealer a inner join dealer_location b ";
		s += "on a.dealer_id = b.dealer_id where organization_id = 'SJM_CORP_LOC' ";
		s += "and geo_lat_no = 0";
		List<DealerLocationVO> data = new ArrayList<DealerLocationVO>();
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(s);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				data.add(new DealerLocationVO(rs));
			}
		} finally {
			ps.close();
		}
		
		return data;
	}

	/**
	 * 
	 * @param dlr
	 * @param conn
	 * @throws SQLException
	 */
	public void updateLocation(DealerLocationVO dlr, Connection conn) 
	throws SQLException {
		GeocodeLocation gl = this.geocodeLoc(dlr);
		if (MatchCode.noMatch.equals(gl.getMatchCode())) return;
		
		String s = "update dealer_location set geo_lat_no = ?, geo_long_no = ?, ";
		s += "match_cd = ? where dealer_location_id = ? ";
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(s);
			ps.setDouble(1, gl.getLatitude());
			ps.setDouble(2, gl.getLongitude());
			ps.setString(3, gl.getMatchCode().toString());
			ps.setString(4, dlr.getDealerLocationId());
			ps.executeUpdate();
		} finally {
			ps.close();
		}
	}

	/**
	 * 
	 * @param loc
	 * @return
	 */
	public GeocodeLocation geocodeLoc(Location loc) {
		GoogleGeocoderV3 gg = new GoogleGeocoderV3();
		gg.addAttribute(GoogleGeocoderV3.INCLUDE_POI, Boolean.FALSE);
		GeocodeLocation gl = gg.geocodeLocation(loc).get(0);
		return gl;
	}
	
	/**
	 * Creates a connection to the database
	 * @param dbUser Database User
	 * @param dbPass Database Password
	 * @return JDBC connection for the supplied user
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	private Connection getDBConnection() throws InvalidDataException, DatabaseException  {
		DatabaseConnection dbc = new DatabaseConnection(dbDriver, dbUrl, dbUser, dbPass);
		return dbc.getConnection();
	}
}
