package com.sjm.corp.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.GoogleGeocoderV3;
import com.siliconmtn.gis.Location;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;

/****************************************************************************
 * <b>Title</b>: ClinicImporter.java <p/>
 * <b>Project</b>: SB_ANS_Medical <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Feb 28, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class ClinicImporter {
	
	private static Logger log = Logger.getLogger(ClinicImporter.class);
	// Database Connection info
	private final String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	//private final String dbUrl = "jdbc:sqlserver://192.168.0.101:2007;selectMethod=cursor;responseBuffering=adaptive";
	//private final String dbUrl = "jdbc:sqlserver://10.0.80.05:2007;selectMethod=cursor;responseBuffering=adaptive";
	private final String dbUrl = "jdbc:sqlserver://192.168.2.72:1433;selectMethod=cursor;responseBuffering=adaptive";
	//private final String dbUser = "sitebuilder_sb_user";
	private final String dbUser = "wc_user";
	private final String dbPass = "sqll0gin";
	private  String basePath = "C:\\Temp\\sjm_corp\\imports_07-24\\";
	private PhoneNumberFormat pnf = null;
	
	/**
	 * 
	 */
	public ClinicImporter() {
		pnf = new PhoneNumberFormat();
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		/*
		 * NOTE: This script assumes that your clinic import file has a header row.  The header
		 * row is not used, rather it is skipped and the 2nd row of the import file is assumed
		 * to contain the first row of clinic data.
		 */
		long start = Calendar.getInstance().getTimeInMillis();
		int dealerCount = 0;
		PropertyConfigurator.configure("C:\\Temp\\sjm_corp\\clinicImporter_log4j.properties");
		//BasicConfigurator.configure();
		log.debug("Starting Import ....");
		Connection conn = null;
		try {
			ClinicImporter ci = new ClinicImporter();
			conn = ci.getDBConnection();
			dealerCount = ci.process(conn, "Vietnam_Indonesia_Philippines_UTF8_v2.csv");
		} catch (Exception e) {
			log.error("Error importing clinics locations...", e);
		} finally {
			if (conn != null) {
				try {
					log.debug("closing db connection...");
					conn.close();
				} catch (Exception e) {
					log.error("Error closing db connection, ", e);
				}
			}
		}
		
		log.debug("Complete");
		long end = Calendar.getInstance().getTimeInMillis();
		log.debug("Imported " + dealerCount + " clinics in " + ((end - start)/1000) + " seconds.");
	}
	
	/**
	 * 
	 * @param conn
	 * @param fileName
	 * @throws Exception
	 */
	public int process(Connection conn, String fileName) throws Exception {
		List<DealerLocationVO> dlrs = this.getDealers(fileName);
		for (int i=0; i < dlrs.size(); i++) {
			DealerLocationVO dlr = dlrs.get(i);
			this.addDealer(dlr, conn);
			this.addDealerLocation(dlr, conn);
		}
		return dlrs.size();
	}
	
	/**
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public List<DealerLocationVO> getDealers(String path) throws Exception {
		File f = new File(basePath + path);
		BufferedReader bf = new BufferedReader(new FileReader(f));
		String temp = "";
		List<DealerLocationVO> data = new ArrayList<DealerLocationVO>();
		
		for (int i=0; (temp = bf.readLine()) != null; i++) {
			String val = temp.trim();
			if (i > 0 && val.length() > 0) {
			//if (val.length() > 0) {
				data.add(this.parseLine(val));
				System.out.println(temp);
			}
		}
		
		bf.close();
		return data;
	}
	
	/**
	 * 
	 * @param line
	 * @return
	 */
	public DealerLocationVO parseLine(String line) {
		DealerLocationVO dlr = new DealerLocationVO();
		StringTokenizer st = new StringTokenizer(line, "\t"); 
		
		for(int i = 0; st.hasMoreElements(); i++){
			String ele = st.nextToken().trim();
			switch(i) {
				case 0: // Location Name
					if (!"N/A".equalsIgnoreCase(ele))
						dlr.setLocationName(ele);
					break;
				case 1: // Address 1
					if (!"N/A".equalsIgnoreCase(ele))
						dlr.setAddress(ele);
					break;
				case 2: // Address 2
					if (!"N/A".equalsIgnoreCase(ele))
						dlr.setAddress2(ele);
					break;
				case 3: // City
					if (!"N/A".equalsIgnoreCase(ele))
						dlr.setCity(ele);
					break;
				case 4: // County
					if (!"N/A".equalsIgnoreCase(ele))
						dlr.setCounty(ele);
					break;
				case 5: // State
					if (!"N/A".equalsIgnoreCase(ele))
						dlr.setState(ele);
					break;
				case 6: // Zip (Postal Code)
					if (!"N/A".equalsIgnoreCase(ele))
						dlr.setZip(ele);
					break;
				case 7: // Country
					if (!"N/A".equalsIgnoreCase(ele))
						dlr.setCountry(ele);
					break;
				case 8: // Phone
					if (!"N/A".equalsIgnoreCase(ele))
						dlr.setPhone(ele);
					break;
				case 9: // Fax
					if (!"N/A".equalsIgnoreCase(ele)) 
						dlr.setFax(ele);				
					break;
				case 10: // ICD
					//log.debug("ICD: " + ele + "|" + Convert.formatBoolean(ele));
					boolean icd = Convert.formatBoolean(ele);
					if (icd) dlr.addAttribute("ICD",Integer.valueOf(1));
					else dlr.addAttribute("ICD",Integer.valueOf(0));
					break;
				case 11: // Pacemaker
					//log.debug("PCMKR: " + ele + "|" + Convert.formatBoolean(ele));
					boolean pcmkr = Convert.formatBoolean(ele);
					if (pcmkr) dlr.addAttribute("PCMKR",Integer.valueOf(1));
					else dlr.addAttribute("PCMKR",Integer.valueOf(0));
					break;
				case 12: // website URL
					if (!"N/A".equalsIgnoreCase(ele)) 
						dlr.setWebsite(ele);
					break;
				case 13: // admin email address
					if (!"N/A".equalsIgnoreCase(ele) && StringUtil.isValidEmail(ele)) 
						dlr.setEmailAddress(ele);
					break;
			}
		}
		
		// if we have a phone number, format it for the given country
		this.formatDealerPhones(dlr);
		
		return dlr;
	}
	
	/**
	 * Formats the dealer phone number based on the dealer's country.
	 * @param dlr
	 */
	private void formatDealerPhones(DealerLocationVO dlr) {
		if (StringUtil.checkVal(dlr.getPhone()).length() > 0) {
			pnf.setCountryCode(dlr.getCountry());
			pnf.setPhoneNumber(dlr.getPhone());
			if (dlr.getCountry().equalsIgnoreCase("US")) {
				pnf.setFormatType(PhoneNumberFormat.NATIONAL_FORMAT);
				dlr.setPhone(pnf.getFormattedNumber());
			} else {
				pnf.setFormatType(PhoneNumberFormat.INTERNATIONAL_FORMAT);
				// we need to remove the country code because the JSTL view supplies it for us
				dlr.setPhone(pnf.getFormattedNumber().substring(pnf.getFormattedNumber().indexOf(" ") + 1));
			}
		}
		
		if (StringUtil.checkVal(dlr.getFax()).length() > 0) {
			pnf.setCountryCode(dlr.getCountry());
			pnf.setPhoneNumber(dlr.getFax());
			if (dlr.getCountry().equalsIgnoreCase("US")) {
				pnf.setFormatType(PhoneNumberFormat.NATIONAL_FORMAT);
			} else {
				pnf.setFormatType(PhoneNumberFormat.INTERNATIONAL_FORMAT);
			}
			dlr.setFax(pnf.getFormattedNumber());
		}
	}
	
	/**
	 * 
	 * @param dlr
	 * @param conn
	 * @throws SQLException
	 */
	public void addDealer(DealerLocationVO dlr, Connection conn)  throws SQLException {
		log.debug("adding dealer: " + dlr.getLocationName());
		StringBuilder s = new StringBuilder();
		s.append("insert into dealer (dealer_id, dealer_type_id, organization_id, ");
		s.append("dealer_nm, create_dt) values (?, '5', 'SJM_CORP_LOC', ?, getdate())");
		String id = new UUIDGenerator().getUUID();
		dlr.setDealerId(id);
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(s.toString());
			ps.setString(1, id);
			ps.setString(2, dlr.getLocationName());
			ps.executeUpdate();
		} finally {
			if (ps != null) ps.close();
		}
	}
	
	/**
	 * 
	 * @param dlr
	 * @param conn
	 * @throws SQLException
	 */
	public void addDealerLocation(DealerLocationVO dlr, Connection conn)  throws SQLException {
		log.debug("adding dealer location for dealer ID: " + dlr.getDealerId());
		GeocodeLocation gl = this.geocodeLoc(dlr.getLocation());
		StringBuilder s = new StringBuilder();
		s.append("insert into dealer_location (dealer_id, dealer_location_id, location_nm, ");
		s.append("address_txt, address2_txt, city_nm, county_nm, state_cd, zip_cd, country_cd, ");
		s.append("primary_phone_no, fax_no, website_url, email_address_txt, geo_lat_no, geo_long_no, ");
		s.append("match_cd, bar_code_id, cass_validate_flg, active_flg, create_dt) ");
		s.append("values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(s.toString());
			ps.setString(1, dlr.getDealerId());
			ps.setString(2, new UUIDGenerator().getUUID());
			ps.setString(3, dlr.getLocationName());
			ps.setString(4, dlr.getAddress());
			ps.setString(5, dlr.getAddress2());
			ps.setString(6, dlr.getCity());
			ps.setString(7, dlr.getCounty());
			ps.setString(8, StringUtil.checkVal(dlr.getState()));
			ps.setString(9, dlr.getZip());
			ps.setString(10, dlr.getCountry());
			ps.setString(11, dlr.getPhone());
			ps.setString(12, dlr.getFax());
			ps.setString(13, dlr.getWebsite());
			ps.setString(14, dlr.getEmailAddress());
			ps.setDouble(15, gl.getLatitude());
			ps.setDouble(16, gl.getLongitude());
			ps.setString(17, gl.getMatchCode().toString());
			ps.setObject(18, StringUtil.checkVal(dlr.getAttributes().get("PCMKR")));
			ps.setObject(19, StringUtil.checkVal(dlr.getAttributes().get("ICD")));
			ps.setInt(20, 1);
			ps.setTimestamp(21, Convert.getCurrentTimestamp());
			
			log.debug("Name: " + dlr.getLocationName() + "|" + dlr.getLocationName().length());
			log.debug("Address: " + dlr.getAddress());
			log.debug("Address2: " + dlr.getAddress2());
			log.debug("City: " + dlr.getCity());
			log.debug("County: " + dlr.getCounty());
			log.debug("State: " + dlr.getState());
			log.debug("Zip: " + dlr.getZip());
			log.debug("Country: " + dlr.getCountry());
			log.debug("Phone: " + dlr.getPhone());
			log.debug("Fax: " + dlr.getFax());
			log.debug("Website: " + dlr.getWebsite());
			log.debug("Email Address: " + dlr.getEmailAddress());
			log.debug("ICD: " + dlr.getAttributes().get("ICD"));
			log.debug("PCMKR: " + dlr.getAttributes().get("PCMKR"));
			log.debug("lat: " + gl.getLatitude());
			log.debug("long: " + gl.getLongitude());
			log.debug("*********************\n");
			ps.executeUpdate();
		} finally {
			if (ps != null) ps.close();
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
		log.debug("obtained db connection...");
		return dbc.getConnection();
	}
}
