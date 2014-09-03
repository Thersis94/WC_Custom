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

import com.ram.datafeed.data.CustomerVO;
import com.siliconmtn.db.DBUtil;
// SMT Base Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: CustomerImporter.java <p/>
 * <b>Project</b>: Imports ram customer data to the db <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Aug. 1, 2014<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class CustomerImporter {
	private String customerFileLoc = null;
	private Properties config = new Properties();
	private static final Logger log = Logger.getLogger("CustomerImporter");
	private StringBuilder errorLog = new StringBuilder();
	private Connection conn = null;
	
	/**
	 * Import Files and set up config params
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws InvalidDataException 
	 * @throws DatabaseException 
	 * 
	 */
	public CustomerImporter() throws Exception {
		// Initialize the logger
		PropertyConfigurator.configure("scripts/ram_importer_log4j.properties");
		
		// Load the config file
		config.load(new FileInputStream(new File("scripts/ram_importer.properties")));
		
		// Load the file location
		customerFileLoc = config.getProperty("customerFile");
		
		// Get the DB Connection
		conn = getConnection();
	}

	/**
	 * Run the Importer.
	 * @param args
	 */
	public static void main(String[] args)  throws Exception {
		CustomerImporter ci = new CustomerImporter();

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
	 * Controlling logic for processing data.
	 * @throws IOException
	 * @throws SQLException 
	 */
	protected void processCustomers() throws IOException, SQLException {
		// Load the Product data
		byte[] data = retrieveFileData(customerFileLoc);
		processCustomers(data);
	}
	
	/**
	 * Gather the Customer Data from the file and insert/update it based on the relevant data.
	 * @param data
	 * @throws IOException
	 * @throws SQLException
	 */
	public void processCustomers(byte[] data) throws IOException, SQLException {
		
		//Read File and populate list.
		BufferedReader inData = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));
		List<CustomerVO> cust = loadCustomerFile(inData);
		
		//Build SQL Statements
		StringBuilder ins = new StringBuilder();
		ins.append("insert into WebCrescendo_ram_custom.dbo.ram_customer (");
		ins.append("organization_id, customer_type_id, customer_nm, gtin_number_txt, ");
		ins.append("active_flg, create_dt, hibc_lic_cd, customer_id) values (?,?,?,?,?,?,?,?)");
		
		StringBuilder upd = new StringBuilder();
		upd.append("update WebCrescendo_ram_custom.dbo.ram_customer set organization_id = ?, ");
		upd.append("customer_type_id = ?, customer_nm = ?, gtin_number_txt = ?, ");
		upd.append("active_flg = ?, update_dt = ?, hibc_lic_cd = ? where customer_id = ?");
		
		//Turn on identity insert.
		Statement s = conn.createStatement();
		s.execute("set identity_insert WebCrescendo_ram_custom.dbo.ram_customer on");
		s.close();
		PreparedStatement pi = null;
		PreparedStatement pu = null;
		int i = 0, total = 0;

		//Process the list of customers based on if createDt is present or not.
		try {
			pi = conn.prepareStatement(ins.toString());
			pu = conn.prepareStatement(upd.toString());

			for(CustomerVO c : cust) {
				if(c.getCreateDate() != null) {
					prepCustomer(pi, c);
				} else {
					prepCustomer(pu, c);
				}
				
				//Batch Process both statements every 500 customers.
				i++;
				if(i == 500) {
					log.debug("Committing batch " + total + " - " + (total + i));
					pi.executeBatch();
					pu.executeBatch();
					total += i;
					i = 0;
				}
			}
			
			//Batch process remaining records.
			log.debug("Committing batch " + total + " - " + (total + i));
			total += i;
			pi.executeBatch();
			pu.executeBatch();
			
			//Turn off identity insert
			s = conn.createStatement();
			s.execute("set identity_insert WebCrescendo_ram_custom.dbo.ram_customer off");
			s.close();
			conn.commit();
		} catch(SQLException sqle) {
			log.error(sqle);
		} finally {
			DBUtil.close(pi);
			DBUtil.close(pu);
			DBUtil.close(s);
			DBUtil.close(conn);
		}
		log.debug("Committed " + total + " records");
	}
	
	/**
	 * Method responsible for setting up data on the prepared statement and adding
	 * it to the batch.
	 * @param p
	 * @param c
	 * @throws SQLException
	 */
	public void prepCustomer(PreparedStatement p, CustomerVO c) throws SQLException {
		p.setString(1, c.getOrganizationId());
		p.setString(2, c.getCustomerTypeId());
		p.setString(3, c.getCustomerName());
		p.setString(4, c.getGtinNumber());
		p.setInt(5, c.getActiveFlag());
		p.setTimestamp(6, Convert.getCurrentTimestamp());
		p.setString(7, c.getHibcLicCode());
		p.setInt(8, c.getCustomerId());
		p.addBatch();
	}
	
	/**
	 * Retrieves the file data from the file system or a web server for download and 
	 * processing
	 * @return
	 * @throws IOException
	 */
	public byte[] retrieveFileData(String loc) throws IOException {
		byte[] b  = null;
		if (customerFileLoc.startsWith("http")) {
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
	
	/**
	 * Loads the customer data into the appropriate beans
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public List<CustomerVO> loadCustomerFile(BufferedReader in) throws IOException {
		String temp = "";
		List<CustomerVO> customers = new ArrayList<CustomerVO>();
		for (int i=0; (temp = in.readLine()) != null; i++) {
			// Skip the header row
			if (i == 0) continue;
			//log.debug("temp: " + temp);
			String[] row = temp.split(",");
			
			// Build the product VO and add to the collection
			CustomerVO c = new CustomerVO();
			c.setCustomerId(Convert.formatInteger(row[0].trim()));
			c.setOrganizationId(row[1].trim());
			c.setCustomerTypeId(row[2].trim());
			c.setCustomerName(WordUtils.capitalizeFully(row[3].replace("_", " ").trim().toLowerCase()));
			c.setGtinNumber(row[4].trim());
			c.setHibcLicCode(row[5].trim());
			c.setActiveFlag(Convert.formatInteger(row[6].trim()));
			if(row[7].trim().length() > 0)
				c.setCreateDate(new Date());
			else
				c.setUpdateDate(new Date());
			customers.add(c);
			
		}
		
		return customers;
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
