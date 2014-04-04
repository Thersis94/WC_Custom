package com.universal.util;

// JDK 7.x
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

// Log4J 1.2.15
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// SMT Base Libs
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: USACatalogImporter.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Apr 01, 2014<p/>
 * <b>Changes: </b>
 * Apr 01, 2014: DBargerhuff: created class.
 ****************************************************************************/
public class USACatalogImporter {
	private static final Logger log = Logger.getLogger(USACatalogImporter.class);
	public static final String DELIMITER_SOURCE = "\t";
	private final String DELIMITER_CONFIG = ",";
	public static final String ATTRIBUTE_PREFIX = "USA_";
	private Properties config = null;
	private Connection dbConn = null;
	
	private List<String> catalogIds;
	
	private String topLevelCategoryId;
	private String skipCategoryId;
	private String featureCategoryId;
	
	private Map<String,String> prefixes; // key is catalogId
	private Map<String,String> sourceURLs; // key is catalogId
	private String[] sourceFileList;

	
	// collections to capture mismatches that occur in import data.
	private Set<String> misMatchedCategories = null;
	private Set<String> misMatchedParentCategories = null;
	private Set<String> misMatchedOptions = null;
	private Set<String> misMatchedPersonalization = null;
	private Set<String> misMatchedAttributes = null;

	/**
	 * 
	 * @throws InvalidDataException 
	 * @throws DatabaseException 
	 * @throws Exception
	 */
	public USACatalogImporter() throws IOException, DatabaseException, InvalidDataException {
		// Initialize the logger
		PropertyConfigurator.configure("scripts/USA_Auto_Importer_log4j.properties");
		config = new Properties();
		config.load(new FileInputStream(new File("scripts/USA_Auto_Importer.properties")));
		initialize(config);
		dbConn = getDbConnection();
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)  {
		long start = System.currentTimeMillis();
		USACatalogImporter uci = null;
		
		try {
			uci = new USACatalogImporter();
		} catch (IOException ioe) {
			log.error("Fatal Error loading configuration properties file, ", ioe);
			// sendErrorEmail(ioe.getMessage());
			System.exit(-1);
		} catch (DatabaseException | InvalidDataException de) {
			log.error("Fatal Error obtaining database connection: ", de);
			//sendErrorEmail(de.getMessage());
			System.exit(-1);
		}
		
		// Process the imports
		try {
			uci.processImports();
		} catch (IOException ioe) {
			log.error("Fatal Error importing catalogs, ", ioe);
			//uci.closeDbConnection();
			System.exit(-1);
		} catch (SQLException sqle) {
			log.error("Fatal Error importing catalogs, ", sqle);
			//uci.closeDbConnection();
			System.exit(-1);
		} finally {
			uci.closeDbConnection();
		}
		
		//uci.sendEmailNotification();
		// Display invalid cats/products/attributes
		uci.logMisMatches();
		
		long end = System.currentTimeMillis();
		log.info("Completed Product Import in " + ((end - start) / 1000) + " seconds");
	}

	/**
	 * Loops catalogs list and imports each catalog in the list.
	 * @throws IOException
	 * @throws SQLException
	 */
	private void processImports() throws IOException, SQLException {
		// configure a vo with base info
		CatalogImportVO iCat = new CatalogImportVO();
		iCat.setCatalogModelYear(config.getProperty("catalogModelYear"));
		iCat.addAttribute(CatalogImportVO.CATEGORY_TOP_LEVEL_ID, topLevelCategoryId);
		iCat.addAttribute(CatalogImportVO.CATEGORY_SKIP_ID, skipCategoryId);
		iCat.addAttribute(CatalogImportVO.CATEGORY_FEATURE_ID, featureCategoryId);
		iCat.setSourceFileDelimiter(DELIMITER_SOURCE);
		
		for (String catalogId : catalogIds) {
			
			try {
				// set catalog-specific values
				iCat.setCatalogId(catalogId);
				iCat.setCatalogPrefix(prefixes.get(catalogId));
				iCat.setSourceFileUrl(sourceURLs.get(catalogId));

				// retrieve source files
				String catalogSourcePath = retrieveSourceFiles(iCat);
				
				// set source file path for this catalog
				iCat.setSourceFilePath(catalogSourcePath);
				
				// import the catalog
				importCatalog(iCat);
				
			} catch (FileNotFoundException fnfe) {
				log.error("IMPORT FAILED for catalog : " + catalogId + ", " + fnfe);
				
			} catch (IOException | SQLException e) {
				log.error("IMPORT FAILED for catalog : " + catalogId + ", " + e);
			}
		}
	}
	
	/**
	 * Calls the catalog retrieval class to retrieve catalog
	 * source files.
	 * @param catalog
	 * @return
	 * @throws IOException
	 */
	private String retrieveSourceFiles(CatalogImportVO catalog) 
			throws IOException {
		//call retriever.
		CatalogRetriever cr = new CatalogRetriever(config);
		return cr.retrieveCatalogForImport(catalog.getCatalogId(), catalog.getSourceFileUrl(), sourceFileList);
	}
		
	/**
	 * Imports catalogs
	 * @param catalog
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws SQLException
	 */
	private void importCatalog(CatalogImportVO catalog) 
			throws FileNotFoundException, IOException, SQLException {
		
		// turn off auto commit so that we can rollback in case of exception
		dbConn.setAutoCommit(false);
		
		// delete categories and products before importing
		manageDeletes(catalog.getCatalogId());
		
		// import category data
		importCategories(catalog);
		
		// import product data
		List<ProductVO> products = importProducts(catalog);
		
		// import product options
		importOptions(catalog, products);
		
		// import product personalization options
		importPersonalization(catalog, products);
		
		// commit changes if all were successful
		dbConn.commit();
		dbConn.setAutoCommit(true);
		log.info("Catalog import for " + catalog.getCatalogId() + " has completed.");

	}
	
	/**
	 * Manages the deletion of categories and products for the specified catalog.
	 * @param catalogId
	 * @throws SQLException
	 */
	private void manageDeletes(String catalogId) throws SQLException {
		// delete categories for this catalog
		deleteElements(catalogId, "PRODUCT_CATEGORY");
		// delete products
		deleteElements(catalogId, "PRODUCT");
	}
		
	/**
	 * Remove existing entries from database to prevent duplicates.
	 * @param productCatalogId
	 * @param tableName
	 * @throws SQLException
	 */
	private void deleteElements(String productCatalogId, String tableName) 
			throws SQLException {
		StringBuilder sql = new StringBuilder();
		sql.append("delete from ").append(tableName).append(" where PRODUCT_CATALOG_ID=?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, productCatalogId);
			ps.execute();
		} catch (SQLException e) {
			log.error("Error deleting USA " + tableName + " data", e);
			throw new SQLException(e.getMessage());
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
			}
		}
	}
		
	/**
	 * Imports the categories for the specified catalog.
	 * @param catalog
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws SQLException
	 */
	private void importCategories(CatalogImportVO catalog) 
			throws FileNotFoundException, IOException, SQLException {
		log.info("importing categories...");
		catalog.setSourceFileName(sourceFileList[0]);
		CategoriesImporter ci = new CategoriesImporter(dbConn);
		ci.setCatalog(catalog);
		ci.manageCategories();
		
		// add any mismatch log entries
		misMatchedCategories.addAll(ci.getMisMatchedCategories());
		misMatchedParentCategories.addAll(ci.getMisMatchedParentCategories());
	}
	
	/**
	 * Imports the products for the specified catalog
	 * @param catalog
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws SQLException
	 */
	private List<ProductVO> importProducts(CatalogImportVO catalog) 
		throws FileNotFoundException, IOException, SQLException {
		log.info("importing products...");
		catalog.setSourceFileName(sourceFileList[1]);
		ProductsImporter pi = new ProductsImporter(dbConn);
		pi.setCatalog(catalog);
		pi.manageProducts();
		
		// add any mismatch log entries
		misMatchedCategories.addAll(pi.getMisMatchedCategories());
		
		return pi.getProducts();
	}
	
	/**
	 * Reads the options file and attempts to insert options into the 
	 * attributes table.
	 * @param catalogSourcePath
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	private void importOptions(CatalogImportVO catalog, List<ProductVO> products) 
			throws FileNotFoundException, IOException, SQLException {
		log.info("importing options...");
		catalog.setSourceFileName(sourceFileList[2]);
		OptionsImporter oi = new OptionsImporter(dbConn);
		oi.setCatalog(catalog);
		oi.manageOptions();
		oi.insertProductOptions(products);
		// add any mismatch log entries
		misMatchedOptions.addAll(oi.getMisMatchedOptions());
		
	}
	
	/**
	 * Imports product personalization options
	 * @param catalog
	 * @param products
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws SQLException
	 */
	private void importPersonalization(CatalogImportVO catalog, List<ProductVO> products) 
			throws FileNotFoundException, IOException, SQLException {
		log.info("importing personalization options...");
		catalog.setSourceFileName(sourceFileList[3]);
		PersonalizationImporter pli = new PersonalizationImporter(dbConn);
		pli.setCatalog(catalog);
		pli.managePersonalization(catalog, products);
		
		// add any mismatch log entries
		misMatchedPersonalization.addAll(pli.getMisMatchedPersonalization());
	}
	
	//private void sendAdminEmail() {}
		
	/**
	 * DEBUG method. Write mismatch data to the log
	 */
	private void logMisMatches() {
		log.info("");
		if (misMatchedParentCategories.size() > 0) {
			log.info("*** Invalid parent category references in category data file: ***");
			for (Iterator<String> iter = misMatchedParentCategories.iterator(); iter.hasNext(); ) {
				log.info("---->" + iter.next());
			}
			log.info("*** end ***");
			log.info("");
		}
		if (misMatchedCategories.size() > 0) {
			log.info("*** Invalid category references in products data file: ***");
			for (Iterator<String> iter = misMatchedCategories.iterator(); iter.hasNext(); ) {
				log.info("---->" + iter.next());
			}
			log.info("*** end ***");
			log.info("");
		}			
		if (misMatchedOptions.size() > 0) {
			log.info("*** Invalid product references in options file: ***");
			for (Iterator<String> iter = misMatchedOptions.iterator(); iter.hasNext(); ) {
				log.info("---->" + iter.next());
			}
			log.info("*** end ***");
			log.info("");
		}
		if (misMatchedPersonalization.size() > 0) {
			log.info("*** Invalid product ID references in personalization file: ***");
			for (Iterator<String> iter = misMatchedPersonalization.iterator(); iter.hasNext(); ) {
				log.info("---->" + iter.next());
			}
			log.info("*** end ***");
			log.info("");
		}
		if (misMatchedAttributes.size() > 0) {
			log.info("*** Invalid attribute references: ***");
			for (Iterator<String> iter = misMatchedAttributes.iterator(); iter.hasNext(); ) {
				log.info("---->" + iter.next());
			}
			log.info("*** end ***");
			log.info("");
		}

	}
	
	/**
	 * Initializes collection objects and parses config file properties into
	 * local fields.
	 * @param config
	 */
	private void initialize(Properties config) {
		// initialize Collections
		catalogIds =  new ArrayList<>();
		prefixes = new LinkedHashMap<>();
		sourceURLs = new LinkedHashMap<>();
		misMatchedCategories = new HashSet<String>();
		misMatchedParentCategories = new HashSet<String>();
		misMatchedOptions = new HashSet<String>();
		misMatchedPersonalization = new HashSet<String>();
		misMatchedAttributes = new HashSet<String>();
		
		// load catalog, prefix, and sourceURL Lists
		String tmpId;
		for (int cId = 0; cId < 10; cId++) {
			tmpId = config.getProperty("catalog_" + cId);
			if (tmpId == null) break;
			catalogIds.add(tmpId);
			tmpId = config.getProperty("catalog_prefix_" + cId);
			prefixes.put(catalogIds.get(cId), tmpId);
			tmpId = config.getProperty("sourceURL_" + cId);
			sourceURLs.put(catalogIds.get(cId), tmpId);
		}
		
		sourceFileList = StringUtil.checkVal(config.getProperty("sourceFileList")).split(DELIMITER_CONFIG);

		topLevelCategoryId = config.getProperty("topLevelCategoryId");
		skipCategoryId = config.getProperty("skipCategoryId");
		featureCategoryId = config.getProperty("featureCategoryId");
		
	}
	
	/**
	 * 
	 * @return
	 * @throws DatabaseException
	 * @throws InvalidDataException
	 */
	private Connection getDbConnection() throws DatabaseException, InvalidDataException {
		DatabaseConnection dbc = new DatabaseConnection();
		dbc.setDriverClass(config.getProperty("dbDriver"));
		dbc.setUrl(config.getProperty("dbUrl"));
		dbc.setUserName(config.getProperty("dbUser"));
		dbc.setPassword(config.getProperty("dbPassword"));
		log.info("Connecting to " + config.getProperty("dbUrl"));
		return dbc.getConnection();
	}
	
	/**
	 * 
	 */
	private void closeDbConnection() {
		if (dbConn != null) {
			try {
				log.info("Closing db connection.");
				dbConn.close();
			} catch (SQLException sqle) {
				log.error("Unable to close DB connection, ", sqle);
			}
		}
	}

}
