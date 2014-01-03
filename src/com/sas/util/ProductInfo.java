package com.sas.util;

// JDK 1.6.x
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

// Log4J 1.2.15
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// SMT Base Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;

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
public class ProductInfo {
	private String fileLoc = null;
	private Properties config = new Properties();
	private static final Logger log = Logger.getLogger("ProductImporter");
	private Connection conn = null;
	
	/**
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws InvalidDataException 
	 * @throws DatabaseException 
	 * 
	 */
	public ProductInfo() throws Exception {
		// Initialize the logger
		PropertyConfigurator.configure("scripts/sas_importer_log4j.properties");
		
		// Load the config file
		config.load(new FileInputStream(new File("scripts/sas_importer.properties")));
		
		// Load the file location
		fileLoc = "/Users/james/Public/prodInfo.txt";
		
		// Get the DB Connection
		conn = getConnection();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		long start = System.currentTimeMillis();
		ProductInfo pi = new ProductInfo();
		pi.conn = pi.getConnection();
		
		log.info("starting product info");
		//Open file
		File f = new File(pi.fileLoc);
		
		// Get a file Stream
		FileOutputStream fos = new FileOutputStream(f);
		String hdrRow = "Product SKU\tProduct ID\tCategory\tProduct Name\tURL\r\n";
		fos.write(hdrRow.getBytes());
		
		// Load the data
		pi.getData(fos);
		fos.flush();
		fos.close();
		
		long end = System.currentTimeMillis();
		log.info("Completed Product info in " + ((end - start) / 1000) + " seconds");
	}
	
	
	public void getData(OutputStream os) throws IOException, SQLException {
		StringBuilder s = new StringBuilder();
		s.append("WITH categories (PARENT_CD, PRODUCT_CATEGORY_CD, ORGANIZATION_ID, CATEGORY_NM, CATEGORY_DESC, level, product_id, product_nm, cust_product_no, url) ");
		s.append("AS ( ");
		s.append("SELECT PARENT_CD, a.PRODUCT_CATEGORY_CD, a.ORGANIZATION_ID, a.CATEGORY_NM, a.CATEGORY_DESC, 0, c.product_id, product_nm, cust_product_no, 'http://m.stacksandstacks.com/cat/qs/detail/' + PRODUCT_URL + '/' + CUST_PRODUCT_NO ");
		s.append("FROM PRODUCT_CATEGORY a ");
		s.append("inner join PRODUCT_CATEGORY_XR b on a.PRODUCT_CATEGORY_CD = b.PRODUCT_CATEGORY_CD ");
		s.append("inner join PRODUCT c on b.PRODUCT_ID = c.PRODUCT_ID ");
		//s.append("WHERE b.PRODUCT_ID in ('113871', '100039') ");
		s.append("UNION ALL ");
		s.append("SELECT c.PARENT_CD, c.PRODUCT_CATEGORY_CD, c.ORGANIZATION_ID, c.CATEGORY_NM, c.CATEGORY_DESC, level + 1, product_id, product_nm,cust_product_no,  url ");
		s.append("FROM PRODUCT_CATEGORY c ");
		s.append("INNER JOIN categories pc ON pc.PARENT_CD  = c.PRODUCT_CATEGORY_CD ");
		s.append(") ");
		s.append("select * from categories order by product_id, level desc; ");
		log.debug("Starting ...");
		PreparedStatement ps = conn.prepareStatement(s.toString());
		
		ResultSet rs = ps.executeQuery();
		String cat = "",name = "", url = "", sku = "";
		String pId = null, currPId = null;
		for (int i=0; rs.next(); i++) {
			if (i % 500 == 0) log.info("Number processed: " + i);
			pId = rs.getString("product_id");
			name = rs.getString("product_nm");
			url = rs.getString("url");
			sku = rs.getString("cust_product_no");
			
			if (i > 0 && ! pId.equals(currPId)) {
				String row = pId + "\t";
				row += sku + "\t";
				row += cat + "\t";
				row += name + "\t";
				row += url + "\r\n";
				os.write(row.getBytes());
				
				cat = "/" + StringUtil.checkVal(rs.getString("category_nm"));
			} else {
				cat += "/" + StringUtil.checkVal(rs.getString("category_nm"));
			}
			
			currPId = pId;
		}
		
		String row = pId + "\t";
		row += sku + "\t";
		row += cat + "\t";
		row += name + "\t";
		row += url + "\r\n";
		os.write(row.getBytes());

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
