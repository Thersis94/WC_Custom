package com.ram.action;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

// Log4J 1.2.15
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// SMT Base Libs
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;

/****************************************************************************
 * <b>Title</b>: ProductImporter.java <p/>
 * <b>Project</b>: Imports product data for RAM Group <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jul 18, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class ProductImporter {
	private String productFileLoc = null;
	private String dailyFileLoc = null;
	private Properties config = new Properties();
	private static final Logger log = Logger.getLogger("ProductImporter");
	private StringBuilder errorLog = new StringBuilder();
	private Connection conn = null;
	
	/**
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws InvalidDataException 
	 * @throws DatabaseException 
	 * 
	 */
	public ProductImporter() throws Exception {
		// Initialize the logger
		PropertyConfigurator.configure("scripts/ram_importer_log4j.properties");
		
		// Load the config file
		config.load(new FileInputStream(new File("scripts/ram_importer.properties")));
		
		// Load the file location
		productFileLoc = config.getProperty("productFileLoc");
		dailyFileLoc = config.getProperty("dailyFileLoc");
		
		// Get the DB Connection
		conn = getConnection();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)  throws Exception {
		ProductImporter pi = new ProductImporter();

		try {
			log.info("Starting RAM Product Importer");
			
			// Process the products
			pi.processProducts("701981");

		} catch (Exception e) {
			log.debug(pi.errorLog);
			pi.errorLog.append("Unable to complete: ").append(e.getMessage()).append("\n<br/>");
			log.error("Error creating product info", e);
		}
	}
	
	/**
	 * 
	 * @throws IOException
	 * @throws SQLException 
	 */
	protected void processProducts(String custId) throws IOException, SQLException {
		// Load the Product data
		byte[] data = retrieveFileData(productFileLoc);
		processProducts(data, custId);
	}
	
	/**
	 * 
	 * @param data
	 * @throws IOException
	 * @throws SQLException
	 */
	public void processProducts(byte[] data, String custId) throws IOException, SQLException {
		BufferedReader inData = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));
		List<ProductVO> p = loadProductFile(inData);
		StringBuilder s = new StringBuilder();
		s.append("insert into sitebuilder_custom.dbo.ram_product (product_id, ");
		s.append("currency_type_id, customer_id, cust_product_no, product_nm, ");
		s.append("create_dt, status_no) values (?,?,?,?,?,?,?) ");
		
		PreparedStatement ps = null;
		for (int i=0; i < p.size(); i++) {
			ProductVO pvo = p.get(i);
			log.debug("product: " + p.get(i).getCustProductNo() + "|" + p.get(i).getProductName());
			
			try {
				ps = conn.prepareStatement(s.toString());
				ps.setString(1, new UUIDGenerator().getUUID());
				ps.setString(2, "dollars");
				ps.setString(3, custId);
				ps.setString(4, pvo.getCustProductNo());
				ps.setString(5, pvo.getProductName());
				ps.setTimestamp(6, Convert.getCurrentTimestamp());
				ps.setInt(7, 5);
				ps.executeUpdate();
			} catch (Exception e) {
				log.error("Unable to load product ", e);
			}
		}

		try {
			ps.close();
		} catch (Exception e) {}
	}
	
	/**
	 * loads the daily file based on the file location in the config file and 
	 * processes it into the database
	 * @throws IOException
	 * @throws SQLException
	 */
	public void processDailyFile() throws IOException, SQLException {
		// Load the Product data
		byte[] data = retrieveFileData(dailyFileLoc);
		processDailyFile(data);
	}
	
	/**
	 * Loads the daily file based upon the byte data passed in
	 * @param data
	 * @throws IOException
	 * @throws SQLException
	 */
	public void processDailyFile(byte[] data) throws IOException, SQLException {
		
	}
	
	/**
	 * Retrieves the file data from the file system or a web server for download and 
	 * processing
	 * @return
	 * @throws IOException
	 */
	public byte[] retrieveFileData(String loc) throws IOException {
		byte[] b  = null;
		if (productFileLoc.startsWith("http")) {
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
	 * Loads the product data into the appropriate beans
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public List<ProductVO> loadProductFile(BufferedReader in) throws IOException {
		String temp = "";
		List<ProductVO> products = new ArrayList<ProductVO>();
		for (int i=0; (temp = in.readLine()) != null; i++) {
			// Skip the header row
			if (i == 0) continue;
			//log.debug("temp: " + temp);
			String[] row = temp.split("\t");
			
			// Build the product VO and add to the collection
			ProductVO p = new ProductVO();
			//p.setOrganizationId("RAM");  //TODO change to use productCatalogId schema - JM 03/29/12
			p.setProductId(new UUIDGenerator().getUUID());
			p.setCustProductNo(row[0]);
			p.setProductName(row[1]);
			products.add(p);
		}
		
		return products;
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
