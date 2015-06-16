package com.ram.action.util;

// JDK 1.6.x
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.WordUtils;
// Log4J 1.2.15
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.ram.datafeed.data.CustomerLocationVO;
import com.siliconmtn.db.DBUtil;
// SMT Base Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: CustomerLocationImporter.java <p/>
 * <b>Project</b>: Imports customer location data for RAM Group <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Aug. 1, 2014<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class CustomerLocationImporter {
	private String customerFileLoc = null;
	private String updatesFileLoc = null;
	private Properties config = new Properties();
	private static final Logger log = Logger.getLogger("CustomerLocationImporter");
	private StringBuilder errorLog = new StringBuilder();
	private Connection conn = null;
	private List<String> updates;
	
	/**
	 * Instantiate and configure member variables.
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws InvalidDataException 
	 * @throws DatabaseException 
	 * 
	 */
	public CustomerLocationImporter() throws Exception {
		// Initialize the logger
		PropertyConfigurator.configure("scripts/ram_importer_log4j.properties");
		
		// Load the config file
		config.load(new FileInputStream(new File("scripts/ram_importer.properties")));
		
		// Load the file location
		customerFileLoc = config.getProperty("customerLocFile");
		updatesFileLoc = config.getProperty("customerUpdateFileLoc");
		
		updates = new ArrayList<String>();
		
		// Get the DB Connection
		conn = getConnection();
	}

	/**
	 * Process import
	 * @param args
	 */
	public static void main(String[] args)  throws Exception {
		CustomerLocationImporter ci = new CustomerLocationImporter();

		try {
			log.info("Starting RAM Product Importer");
			ci.processCustomers();
			
		} catch (Exception e) {
			log.debug(ci.errorLog);
			ci.errorLog.append("Unable to complete: ").append(e.getMessage()).append("\n<br/>");
			log.error("Error creating product info", e);
		}
	}
	
	/**
	 * Controlling logic for importing customers.  Here we want to process them and then verify that
	 * they all got added properly as some of the data might give false positives for updates.
	 * @throws IOException
	 * @throws SQLException 
	 */
	protected void processCustomers() throws IOException, SQLException {
		// Load the Product data
		retrieveUpdates(updatesFileLoc);
		byte[] data = retrieveFileData(customerFileLoc);
		BufferedReader inData = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));
		List<CustomerLocationVO> cust = loadCustomerFile(inData);
		processCustomers(cust);
		verifyImport(cust);
	}
	
	/**
	 * Verify that the data got imported properly.  If it doesn't exist then we need to insert it.
	 * @param cust
	 */
	private void verifyImport(List<CustomerLocationVO> cust) {
		
		//Sql statment
		StringBuilder sel = new StringBuilder();
		sel.append("select customer_location_id from WebCrescendo_ram_custom.dbo.ram_customer_location ");
		sel.append("where customer_location_id = ?");
		
		PreparedStatement ps = null;
		PreparedStatement p = null;
		int i = 0;
		try {
			
			//Turn on identity insert
			Statement s = conn.createStatement();
			s.execute("set identity_insert WebCrescendo_ram_custom.dbo.ram_customer_location on");
			ps = conn.prepareStatement(sel.toString());
			p = conn.prepareStatement(getInsert());
			
			/*
			 * Loop over customer Locations and if it does not exist then insert it.
			 */
			for(CustomerLocationVO c : cust) {
				ps.setInt(1, c.getCustomerLocationId());
				ResultSet rs = ps.executeQuery();
				i++;
				if(!rs.next()) {
					log.debug("Location " + c.getCustomerLocationId() + " doesn't exist.");
					prepCustomer(p, c, false);
				}
				if(i % 500 == 0)
					log.debug("checked " + i + " records");
			}
			
			//Turn off Identity insert
			s.execute("set identity_insert WebCrescendo_ram_custom.dbo.ram_customer_location off");

		} catch(SQLException s) {
			log.error(s);
		} finally {
			DBUtil.close(ps);
		}
	}

	/**
	 * Handle initial processing of the data attempting to either insert of update the data.
	 * @param cust
	 * @throws IOException
	 * @throws SQLException
	 */
	public void processCustomers(List<CustomerLocationVO> cust) throws IOException, SQLException {
		
		//Build update statment.
		StringBuilder upd = new StringBuilder();
		upd.append("update WebCrescendo_ram_custom.dbo.ram_customer_location set ");
		upd.append("customer_id = ?, region_id = ?, location_nm = ?, address_txt = ?, address2_txt = ?, ");
		upd.append("city_nm = ?, state_cd = ?, zip_cd = ?, country_cd = ?, latitude_no = ?, longitude_no = ?, ");
		upd.append("match_cd = ?, stocking_location_txt = ?, update_dt = ?, active_flg = ? where customer_location_id = ? ");
		
		//Turn on identity insert
		Statement s = conn.createStatement();
		s.execute("set identity_insert WebCrescendo_ram_custom.dbo.ram_customer_location on");
		PreparedStatement pi = null;
		PreparedStatement pu = null;
		int i = 0, total = 0;
		log.debug(conn.isClosed());
		try {

			//Loop over params and attempt to insert/update them via batch calls.
			pi = conn.prepareStatement(getInsert());
			pu = conn.prepareStatement(upd.toString());
			boolean isBatch = true;
			for(CustomerLocationVO c : cust) {
				//log.debug(c.getCustomerLocationId());
				if(c.getCreateDate() != null) {
					prepCustomer(pi, c, isBatch);
				} else {
					prepCustomer(pu, c, isBatch);
				}
				
				//Commit every 500 locations.
				if(isBatch) {
					i++;
					if(i == 500) {
						log.debug("Committing batch " + total + " - " + (total + i));
						pi.executeBatch();
						pu.executeBatch();
						total += i;
						i = 0;
					}
				}
			}
			
			//Commit stragglers
			log.debug("Committing batch " + total + " - " + (total + i));
			total += i;
			pi.executeBatch();
			pu.executeBatch();
			
			//turn off identity insert.
			s.execute("set identity_insert WebCrescendo_ram_custom.dbo.ram_customer_location off");
		} catch(SQLException sqle) {
			log.error(sqle);
		} finally {
			try {
				pu.close();
				pi.close();
			}catch(Exception e){}
		}
		log.debug("Committed " + total + " records");
	}
	
	/**
	 * Set up the data in the prepared statements.  Depending on whether this is a batch statement or not
	 * we will also either add it to the batch or commit it here.
	 * @param p
	 * @param c
	 * @param isBatch
	 * @throws SQLException
	 */
	public void prepCustomer(PreparedStatement p, CustomerLocationVO c, boolean isBatch) throws SQLException {
		p.setInt(1, c.getCustomerId());
		p.setString(2, c.getRegionId());
		p.setString(3, c.getLocationName());
		p.setString(4, c.getAddress());
		p.setString(5, c.getAddress2());
		p.setString(6, c.getCity());
		p.setString(7, c.getState());
		p.setString(8, c.getZipCode());
		p.setString(9, c.getCountry());
		p.setDouble(10, c.getLatitude());
		p.setDouble(11, c.getLongitude());
		p.setString(12, c.getMatchCode().name());
		p.setString(13, c.getStockingLocation());
		p.setTimestamp(14, Convert.getCurrentTimestamp());
		p.setInt(15, c.getActiveFlag());
		p.setInt(16, c.getCustomerLocationId());
		
		if(isBatch)
			p.addBatch();
		else
			p.execute();
	}
	
	/**
	 * Retreive the insert sql statement.  This is used in initial insert and validation so it lives
	 * separate of both of access.
	 * @return
	 */
	public String getInsert() {
		StringBuilder ins = new StringBuilder();
		ins.append("insert into WebCrescendo_ram_custom.dbo.ram_customer_location (");
		ins.append("customer_id, region_id, location_nm, address_txt, address2_txt, ");
		ins.append("city_nm, state_cd, zip_cd, country_cd, latitude_no, longitude_no, ");
		ins.append("match_cd, stocking_location_txt, create_dt, active_flg, customer_location_id) ");
		ins.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		return ins.toString();
	}
	
	/**
	 * Retrieves the file data from the file system or a web server for download and 
	 * processing
	 * @return
	 * @throws IOException
	 */
	public byte[] retrieveFileData(String loc) throws IOException {
		byte[] b  = null;
		if (loc.startsWith("http")) {
			SMTHttpConnectionManager hConn = new SMTHttpConnectionManager();
			b = hConn.retrieveData(loc);

		} else {
			BufferedReader data = new BufferedReader(new FileReader(loc));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int c;
			while ((c = data.read()) > -1) {
				baos.write(c);
			}
			
			b = baos.toByteArray();
			data.close();
			baos.flush();
			baos.close();
			
		}
		
		log.info("File Size: " + b.length);
		return b;
	}
	
	@SuppressWarnings("unused")
	public void retrieveUpdates(String loc) throws IOException {
		byte[] b  = null;
		if (loc.startsWith("http")) {
			SMTHttpConnectionManager hConn = new SMTHttpConnectionManager();
			b = hConn.retrieveData(loc);

		} else {
			BufferedReader data = new BufferedReader(new FileReader(loc));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int c;
			while ((c = data.read()) > -1) {
				baos.write(c);
			}
			
			b = baos.toByteArray();
			data.close();
			baos.flush();
			baos.close();
			
		}
		
		log.info("File Size: " + b.length);
		BufferedReader inData = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(b)));
		String temp = "";
		for (int i=0; (temp = inData.readLine()) != null; i++) {
			// Skip the header row
			updates.add(temp.trim());
		}

	}
	/**
	 * Loads the customer locations data into the appropriate beans
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public List<CustomerLocationVO> loadCustomerFile(BufferedReader in) throws IOException {
		String temp = "";
		List<CustomerLocationVO> locations = new ArrayList<CustomerLocationVO>();
		for (int i=0; (temp = in.readLine()) != null; i++) {
			// Skip the header row
			if (i == 0) continue;
			//log.debug("temp: " + temp);
			String[] row = temp.split("\t");
			//log.debug(temp);
			// Build the product VO and add to the collection
			CustomerLocationVO c = new CustomerLocationVO();
			c.setCustomerLocationId(Convert.formatInteger(row[0]));
			c.setCustomerId(Convert.formatInteger(row[1]));
			c.setRegionId(row[2]);
			c.setLocationName(WordUtils.capitalizeFully(row[3].replace("_", " ").toLowerCase()).trim());
			c.setAddress(WordUtils.capitalizeFully(row[4].toLowerCase()).trim());
			c.setAddress2(WordUtils.capitalizeFully(row[5].toLowerCase()).trim());
			c.setCity(WordUtils.capitalizeFully(row[6].toLowerCase()).trim());
			c.setState(row[7].trim());
			c.setZipCode(row[8].trim());
			c.setCountry(row[9].trim());
			c.setLatitude(0.0);
			c.setLongitude(0.0);
			c.setMatchCode(row[12]);
			c.setStockingLocation(row[13]);
			c.setActiveFlag(Convert.formatInteger(row[14]));
			
			//Depending on what data is present we may be inserting or updating.  Make check here.
			if(row[15].length() > 0 && !updates.contains(row[0]))
				c.setCreateDate(new Date());
			else
				c.setUpdateDate(new Date());
			locations.add(c);
			
		}
		
		return locations;
	}

	/**
	 * 
	 * @return
	 * @throws DatabaseException
	 * @throws InvalidDataException
	 */
	public Connection getConnection() {
		DatabaseConnection dbc = new DatabaseConnection();
		dbc.setDriverClass(config.getProperty("dbDriver"));
		dbc.setUrl(config.getProperty("dbUrl"));
		dbc.setUserName(config.getProperty("dbUser"));
		dbc.setPassword(config.getProperty("dbPassword"));
		try {
			return dbc.getConnection();
		} catch (Exception e) {
			log.error("Unable to get a DB Connection",e);
			System.exit(-1);
		} 
		
		return null;
	}

	/**
	 * @return the conn
	 */
	public Connection getDatabaseConnection() {
		return conn;
	}

	/**
	 * @param conn the conn to set
	 */
	public void setDatabaseConnection(Connection conn) {
		this.conn = conn;
	}

}
