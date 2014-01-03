package com.depuy.sitebuilder.compliance;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.gis.AbstractGeocoder;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.GoogleGeocoder;
import com.siliconmtn.gis.Location;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>:DataLoader.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Oct 23, 2007
 ****************************************************************************/
public class DataLoader {
	private String geoUrl = "http://maps.google.com/maps/geo?key=ABQIAAAAUq7CVtZdPqF5krHWuCRA6hT1jWPbhci0OxnQexaN3kHjrnTe3xRtPsQgpLeImzIcfOopluLg7SN4xQ";
	public DataLoader() {
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Starting Compliance Data Loader");
		DatabaseConnection dc = new DatabaseConnection();
		dc.setDriverClass("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		//dc.setUrl("jdbc:sqlserver://simon:2007");
		dc.setUrl("jdbc:sqlserver://localhost:1433");
		dc.setUserName("sb_user");
		//dc.setPassword("sqll0gin");
		dc.setPassword("sb_user");
		Connection conn = null;
		
		// Get the database Connection
		try {
			conn = dc.getConnection();
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		// Process the file
		DataLoader dl = new DataLoader();
		List<ComplianceVO> data = null;
		try {
			data = dl.loadFile("C:\\Develop\\Code\\WebDisclosure.txt");
			dl.loadData(conn, data);
		} catch(IOException ioe) {
			ioe.printStackTrace();
			System.exit(-1);
		}
		
		System.out.println("Compliance Data Loader Complete");
	}
	
	/**
	 * Loads the delimited text file and parses into Compliance objects
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public List<ComplianceVO> loadFile(String path) throws
	IOException {
		BufferedReader in = new BufferedReader(new FileReader(path));
		List<ComplianceVO> data = new ArrayList<ComplianceVO>();
		String temp = null;
		while((temp = in.readLine()) != null) {
			data.add(new ComplianceVO(temp));
		}
		
		in.close();
		return data;
	}
	
	/**
	 * Stores the data into the database
	 * @param conn
	 * @param data
	 */
	public void loadData(Connection conn, List<ComplianceVO> data) {
		StringBuffer sql = new StringBuffer();
		sql.append("insert into compliance_info (compliance_id, company_nm, ");
		sql.append("last_nm, first_nm, title_nm, city_nm, state_cd, zip_cd, ");
		sql.append("latitude_no, longitude_no, match_cd, consulting_no, ");
		sql.append("grant_no, research_no, royalty_no, create_dt) values ");
		sql.append("(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		
		try {
			AbstractGeocoder ag = new GoogleGeocoder();
			ag.addAttribute(AbstractGeocoder.CONNECT_URL, geoUrl);
			PreparedStatement ps =conn.prepareStatement(sql.toString());
			for (int i = 0; i < data.size(); i++) {
				ComplianceVO vo = data.get(i);
				Location loc = new Location();
				loc.setCity(vo.getCityName());
				loc.setState(vo.getStateCode());
				GeocodeLocation gl = new GeocodeLocation(); //ag.geocodeLocation(loc).get(0);
				
				ps.setString(1, vo.getComplianceId());
				ps.setString(2, vo.getCompanyName());
				ps.setString(3, vo.getLastName());
				ps.setString(4, vo.getFirstName());
				ps.setString(5, vo.getTitleName());
				ps.setString(6, vo.getCityName());
				ps.setString(7, vo.getStateCode());
				ps.setString(8, gl.getZipCode());
				ps.setDouble(9, gl.getLatitude());
				ps.setDouble(10, gl.getLongitude());
				ps.setString(11, gl.getMatchCode().toString());
				ps.setInt(12, vo.getConsultingNumber());
				ps.setInt(13, vo.getGrantNumber());
				ps.setInt(14, vo.getResearchNumber());
				ps.setInt(15, vo.getRoyaltyNumber());
				ps.setTimestamp(16, Convert.getCurrentTimestamp());
				
				ps.addBatch();
			}
			
			ps.executeBatch();
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

}
