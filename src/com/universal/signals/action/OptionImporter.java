package com.universal.signals.action;

// JDK 1.6.x
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

// Log4J 1.2.15
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// SMT Base Libs
import com.siliconmtn.commerce.catalog.ProductAttributeVO;
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;

/****************************************************************************
 * <b>Title</b>: ProductImporter.java <p/>
 * <b>Project</b>: WC_Misc <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jul 18, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class OptionImporter {
	private String optFileLoc = null;
	private Properties config = new Properties();
	private static final Logger log = Logger.getLogger("OptionImporter");
	private Connection conn = null;
	private List<ProductAttributeVO> attributes = new ArrayList<ProductAttributeVO>();
	Set<String> mismatchCat = new HashSet<String>();
	
	/**
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws InvalidDataException 
	 * @throws DatabaseException 
	 * 
	 */
	public OptionImporter() throws Exception {
		// Initialize the logger
		PropertyConfigurator.configure("scripts/usa_importer_log4j.properties");
		
		// Load the config file
		config.load(new FileInputStream(new File("scripts/usa_importer.properties")));
		
		// Load the file location
		optFileLoc = config.getProperty("optFileLoc");
		
		// Get the DB Connection
		conn = getConnection();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)  {
		long start = System.currentTimeMillis();
		OptionImporter pi = null;

		try {
			pi = new OptionImporter();
			log.info("Starting Signals Product Importer");
			
			// Get the DB conenction 
			pi.conn = pi.getConnection();
			
			// Process the importer
			pi.processRequest();

		} catch (Exception e) {
			log.error("Unable to process importer", e);
		}

		
		long end = System.currentTimeMillis();
		log.info("Completed Product Import in " + ((end - start) / 1000) + " seconds");
	}
	
	/**
	 * 
	 * @throws IOException
	 * @throws SQLException 
	 */
	public void processRequest() throws IOException, SQLException {
		// Grab the options from the file
		this.retrieveOptions();
		
		// Load the options into the associative table
		this.addProductOption();
	}

	
	/**
	 * 
	 * @param prods
	 * @throws SQLException
	 */
	public void addProductOption() throws SQLException {
		String s = "insert into product_attribute_xr (product_attribute_id, product_id, ";
		s += "attribute_id, model_year_no, currency_type_id, value_txt, msrp_cost_no, ";
		s += "attrib1_txt, attrib2_txt, create_dt) ";
		s += "values (?,?,?,?,?,?,?,?,?,?)";
		
		PreparedStatement ps = conn.prepareStatement(s);
		int errorCtr = 1;
		for (int i=0; i < attributes.size(); i++) {
			ProductAttributeVO p = attributes.get(i);
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, p.getProductId());
			ps.setString(3, p.getAttributeId());
			ps.setString(4, "USA_SIGNALS_2012");
			ps.setString(5, "dollars");
			ps.setString(6, p.getValueText());
			ps.setDouble(7, p.getMsrpCostNo());
			ps.setString(8, p.getAttribute1());
			ps.setString(9, p.getAttribute2());
			ps.setTimestamp(10, Convert.getCurrentTimestamp());
			
			try {
				ps.executeUpdate();
			} catch (Exception e) {
				log.error(errorCtr++ + " - Unable to add product: " + p.getProductId());
			}
		}
		
		ps.close();
	}

	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	public void retrieveOptions() throws IOException {
		BufferedReader data = new BufferedReader(new FileReader(optFileLoc));
		
		String temp;
		Map<String, String> keys = new HashMap<String, String>();
		for (int i=0; (temp = data.readLine()) != null; i++) {
			if (i == 0) continue;
			
			String[] fields = temp.split("\t");

			ProductAttributeVO vo = new ProductAttributeVO();
			vo.setProductId(fields[1]);
			vo.setValueText(fields[3]);
			vo.setMsrpCostNo(Convert.formatDouble(0.0));
			vo.setAttribute("attrib1","@" + fields[2]);
			vo.setAttribute("attrib2",fields[4]);
			vo.setAttributeId("USA_" + "CUSTOM");
			attributes.add(vo);
			
			keys.put(vo.getAttributeId(), vo.getAttribute1());

		}
		
		data.close();
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

}
