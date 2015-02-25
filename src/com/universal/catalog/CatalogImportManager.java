package com.universal.catalog;

// JDK 7.x
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: CatalogImportManager.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Manages the import process for importing USA catalogs.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Apr 01, 2014<p/>
 * <b>Changes: </b>
 * Apr 01, 2014: DBargerhuff: created class.
 * Apr 04, 2014: DBargerhuff: refactored into modular classes.
 ****************************************************************************/
public class CatalogImportManager {
	private static final Logger log = Logger.getLogger(CatalogImportManager.class);
	public static final String DELIMITER_SOURCE = "\t";
	private final String DELIMITER_CONFIG = ",";
	public static final String ATTRIBUTE_PREFIX = "USA_";
	private final String BREAK = "\n";
	private Properties config = null;
	private Connection dbConn = null;
	
	private List<String> catalogIds;
	
	private String topLevelParentCategoryId;
	private String topLevelCategoryId;
	private String skipCategoryId;
	private String featureCategoryId;
	
	private Map<String,String> prefixes; // key is catalogId
	private Map<String,String> sourceURLs; // key is catalogId
	private String[] sourceFileList;
	private boolean importLocalFiles = false;

	
	// collections to capture mismatches that occur in import data.
	private Set<String> misMatchedCategories = null;
	private Set<String> misMatchedParentCategories = null;
	private Set<String> misMatchedOptions = null;
	private Set<String> misMatchedPersonalization = null;
	private Set<String> misMatchedAttributes = null;
	
	private StringBuilder messageBuilder = null;

	/**
	 * 
	 * @throws InvalidDataException 
	 * @throws DatabaseException 
	 * @throws Exception
	 */
	public CatalogImportManager() throws IOException, DatabaseException, InvalidDataException {
		// Initialize the logger
		PropertyConfigurator.configure("scripts/USA_Auto_Importer_log4j.properties");
		config = new Properties();		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)  {
		boolean isSuccess = false;
		long start = System.currentTimeMillis();
		CatalogImportManager uci = null;
		try {
			uci = new CatalogImportManager();
			uci.initialize();
		} catch (IOException ioe) {
			log.error("Fatal Error:  Failed to load configuration properties file, ", ioe);
			uci.addMessage(ioe.getMessage());
			uci.sendAdminEmail(isSuccess);
			System.exit(-1);
		} catch (DatabaseException | InvalidDataException de) {
			log.error("Fatal Error: Failed to obtain a database connection: ", de);
			uci.addMessage(de.getMessage());
			uci.sendAdminEmail(isSuccess);
			System.exit(-1);
		}
		
		// Process the imports
		String errMsg = null;
		try {
			//String flag = uci.checkTime();
			isSuccess = uci.processImports();
			uci.logMisMatches();
		} catch (IOException ioe) {
			errMsg = "Fatal Error importing catalogs, " + ioe.getMessage();
			log.error(errMsg);
			uci.addMessage(errMsg);
			uci.sendAdminEmail(isSuccess);
			System.exit(-1);
		} catch (SQLException sqle) {
			errMsg = "Fatal Error importing catalogs, " + sqle.getMessage();
			log.error(errMsg);
			uci.addMessage(errMsg);
			uci.sendAdminEmail(isSuccess);
			System.exit(-1);
		} finally {
			uci.closeDbConnection();
		}
		
		long end = System.currentTimeMillis();
		uci.addMessage("Completed Product Import in " + ((end - start) / 1000) + " seconds.");
		uci.sendAdminEmail(isSuccess);
	}

	/**
	 * Loops catalogs list and imports each catalog in the list.
	 * @throws IOException
	 * @throws SQLException
	 */
	private boolean processImports() throws IOException, SQLException {
		// configure a vo with base info
		CatalogImportVO iCat = new CatalogImportVO();
		iCat.setCatalogModelYear(config.getProperty("catalogModelYear"));
		iCat.addAttribute(CatalogImportVO.CATEGORY_TOP_LEVEL_PARENT_ID, topLevelParentCategoryId);
		iCat.addAttribute(CatalogImportVO.CATEGORY_TOP_LEVEL_ID, topLevelCategoryId);
		iCat.addAttribute(CatalogImportVO.CATEGORY_SKIP_ID, skipCategoryId);
		iCat.addAttribute(CatalogImportVO.CATEGORY_FEATURE_ID, featureCategoryId);
		iCat.setSourceFileDelimiter(DELIMITER_SOURCE);
		iCat.setUseLocalImportFiles(importLocalFiles);
		String errMsg = null;
		boolean success = true;
		// loop the catalogs by catalog ID and import.
		for (String catalogId : catalogIds) {
			try {
				addMessage(BREAK + "Processing catalog ID: " + catalogId);
				// set catalog-specific values
				iCat.setCatalogId(catalogId);
				iCat.setCatalogPrefix(prefixes.get(catalogId));
				iCat.setSourceFileUrl(sourceURLs.get(catalogId));

				// retrieve source files
				String catalogSourcePath = retrieveSourceFiles(iCat, importLocalFiles);
				
				// set source file path for this catalog
				iCat.setSourceFilePath(catalogSourcePath);
				
				addMessage("Catalog prefix: " + iCat.getCatalogPrefix());
				addMessage("Catalog source URL: " + iCat.getSourceFileUrl());
				addMessage("Catalog source file: " + iCat.getSourceFilePath());
				
				// import the catalog
				importCatalog(iCat);
			} catch (FileNotFoundException fnfe) {
				errMsg = "IMPORT FAILED for catalog : " + catalogId + ", " + fnfe.getMessage() + BREAK;
				log.error(errMsg);
				addMessage(errMsg);
				success = false;
			} catch (IOException | SQLException e) {
				errMsg = "IMPORT FAILED for catalog : " + catalogId + ", " + e.getMessage() + BREAK;
				log.error(errMsg);
				addMessage(errMsg);
				success = false;
			}
			errMsg = null;
		}
		return success;
	}
	
	/**
	 * Calls the catalog retrieval class to retrieve catalog
	 * source files.
	 * @param catalog
	 * @return
	 * @throws IOException
	 */
	private String retrieveSourceFiles(CatalogImportVO catalog, boolean importLocalFiles) 
			throws IOException {
		//call retriever.
		CatalogRetriever cr = new CatalogRetriever(config);
		return cr.retrieveCatalogForImport(catalog.getCatalogId(), 
				catalog.getSourceFileUrl(), sourceFileList, importLocalFiles);
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
		
		List<String> productFilter = buildProductFilter(products);
		
		Map<String,List<Map<String,List<String>>>> optionsIndexHierarchy = null;
		optionsIndexHierarchy = importOptionsIndexHierarchy(catalog, productFilter);
		
		// import product options
		importOptions(catalog, productFilter, optionsIndexHierarchy);
		
		// import product personalization options
		importPersonalization(catalog, products);
		
		// commit changes if all were successful
		dbConn.commit();
		dbConn.setAutoCommit(true);
		addMessage("Catalog import for " + catalog.getCatalogId() + " has completed.\n");

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
		log.info("delete SQL: " + sql.toString() + "|" + productCatalogId);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, productCatalogId);
			ps.execute();
		} catch (SQLException e) {
			String errMsg = "Error deleting USA " + tableName + " data: " + e;
			log.error(errMsg);
			addMessage(errMsg);
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
		addMessage("importing categories...");
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
		addMessage("importing products...");
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
	 * @param catalog
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws SQLException
	 */
	private void importOptions (CatalogImportVO catalog, List<String> productFilter,
			Map<String,List<Map<String,List<String>>>> optionsIndexHierarchy) 
			throws FileNotFoundException, IOException, SQLException {
		log.info("importing options...");
		addMessage("importing options...");
		catalog.setSourceFileName(sourceFileList[2]);
		OptionsImporter oi = new OptionsImporter(dbConn);
		oi.setCatalog(catalog);
		oi.setOptionsIndexHierarchy(optionsIndexHierarchy);
		oi.manageOptions();
		oi.insertProductOptions(productFilter);
		// add any mismatch log entries
		misMatchedOptions.addAll(oi.getMisMatchedOptions());
	}
	
	/**
	 * Loads and parses the options index file into a map of product ID to
	 * a list of options hierarchies where the List index corresponds to the level of
	 * the hierarchy (e.g. index 0 is the top level parent in the hierarchy).
	 * @param catalog
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private Map<String, List<Map<String, List<String>>>> 
		importOptionsIndexHierarchy(CatalogImportVO catalog, List<String> productFilter) 
				throws FileNotFoundException, IOException {
		log.info("importing options index...");
		addMessage("importing options index...");
		catalog.setSourceFileName(sourceFileList[3]);
		OptionsIndexImporter oii = new OptionsIndexImporter();
		oii.setCatalog(catalog);
		return oii.manageOptionsIndex(productFilter);
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
		addMessage("importing personalization options...");
		catalog.setSourceFileName(sourceFileList[4]);
		PersonalizationImporter pli = new PersonalizationImporter(dbConn);
		pli.setCatalog(catalog);
		pli.managePersonalization(catalog, products);
		
		// add any mismatch log entries
		misMatchedPersonalization.addAll(pli.getMisMatchedPersonalization());
	}
	
	/**
	 * Returns a List of raw, non-prefixed product IDs derived from the List of 
	 * ProductVOs that of products imported by the Product importer. We use this
	 * to filter the options that we import.
	 * @param products
	 * @return
	 */
	private List<String> buildProductFilter(List<ProductVO> products) {
		List<String> pIds = new ArrayList<>();
		for (ProductVO p : products) {
			/* 
			 * !! NOTE: custProductNo is the raw, non-prefixed product ID.  We build 
			 * this List to use as a filter when importing options because the import
			 * source file is dirty (contains data for products that don't exist in the
			 * product import source file).
			 */
			pIds.add(p.getCustProductNo());
		}
		return pIds;
	}
	
	/**
	 * 
	 * @param success
	 * @param message
	 */
	private void sendAdminEmail(boolean success) {
		SMTMail mail = new SMTMail();
		mail.setSmtpServer(config.getProperty("smtpServer"));
		mail.setPort(Convert.formatInteger(config.getProperty("smtpPort")));
		mail.setUser(config.getProperty("smtpUser"));
		mail.setPassword(config.getProperty("smtpPassword"));
		try {
			mail.addRecipient(config.getProperty("smtpRecipient"));
			mail.setFrom(config.getProperty("smtpSender"));
			mail.setSubject("USA Catalog Import Results:  " + (success ? "Success" : "Failed: Check logs!"));
			mail.setTextBody(messageBuilder.toString());
			mail.postMail();
		} catch (Exception e) {
			log.error("Error sending admin email, ", e);
		}
	}
		
	/**
	 * Writes mismatch data to the log and to the List of messages
	 * to include in the admin email.
	 */
	private void logMisMatches() {
		StringBuilder info = new StringBuilder();
		String arrow = "---->";
		String end = "*** end ***";
		info.append(BREAK);
		
		if (misMatchedParentCategories.size() > 0) {
			info.append("*** Invalid parent category references in category data file: ***").append(BREAK);
			for (Iterator<String> iter = misMatchedParentCategories.iterator(); iter.hasNext(); ) {
				info.append(arrow).append(iter.next()).append(BREAK);
			}
			info.append(end).append(BREAK);
			info.append(BREAK);
		}
		
		if (misMatchedCategories.size() > 0) {
			info.append("*** Invalid category references in products data file: ***").append(BREAK);
			for (Iterator<String> iter = misMatchedCategories.iterator(); iter.hasNext(); ) {
				info.append(arrow).append(iter.next()).append(BREAK);
			}
			info.append(end).append(BREAK);
			info.append(BREAK);
		}
		
		if (misMatchedOptions.size() > 0) {
			info.append("*** Invalid product references in options file: ***").append(BREAK);
			for (Iterator<String> iter = misMatchedOptions.iterator(); iter.hasNext(); ) {
				info.append(arrow).append(iter.next()).append(BREAK);
			}
			info.append(end).append(BREAK);
			info.append(BREAK);
		}
		
		if (misMatchedPersonalization.size() > 0) {
			info.append("*** Invalid product ID references in personalization file: ***").append(BREAK);
			for (Iterator<String> iter = misMatchedPersonalization.iterator(); iter.hasNext(); ) {
				info.append(arrow).append(iter.next()).append(BREAK);
			}
			info.append(end).append(BREAK);
			info.append(BREAK);
		}
		
		if (misMatchedAttributes.size() > 0) {
			info.append("*** Invalid attribute references: ***").append(BREAK);
			for (Iterator<String> iter = misMatchedAttributes.iterator(); iter.hasNext(); ) {
				info.append(arrow).append(iter.next()).append(BREAK);
			}
			info.append(end).append(BREAK);
			info.append(BREAK);
		}
		log.info(info);
		addMessage(info.toString());
	}
	
	/**
	 * Calls the methods that load the properties file and that establish a database connection.
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws DatabaseException
	 * @throws InvalidDataException
	 */
	private void initialize() 
			throws FileNotFoundException, IOException, DatabaseException, InvalidDataException {
		config.load(new FileInputStream(new File("scripts/USA_Auto_Importer.properties")));
		loadProperties(config);
		dbConn = getDbConnection();
	}
	
	/**
	 * Initializes collection objects and parses config file properties into
	 * local fields.
	 * @param config
	 */
	private void loadProperties(Properties config) {
		// initialize Collections
		catalogIds =  new ArrayList<>();
		prefixes = new LinkedHashMap<>();
		sourceURLs = new LinkedHashMap<>();
		messageBuilder = new StringBuilder(1000);
		misMatchedCategories = new LinkedHashSet<String>();
		misMatchedParentCategories = new LinkedHashSet<String>();
		misMatchedOptions = new LinkedHashSet<String>();
		misMatchedPersonalization = new LinkedHashSet<String>();
		misMatchedAttributes = new LinkedHashSet<String>();
		
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
		importLocalFiles = Convert.formatBoolean(StringUtil.checkVal(config.getProperty("useLocalFilesForImport")));
		log.info("Use Local File for Import: " + importLocalFiles);
		topLevelParentCategoryId = config.getProperty("topLevelParentCategoryId");
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
		String msg = "DB Connection: " + config.getProperty("dbUrl");
		log.info(msg);
		addMessage(msg);
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
				addMessage("Closed db connection.");
			} catch (SQLException sqle) {
				String errMsg = "Unable to close db connection, " + sqle.getMessage();
				log.error(errMsg);
				addMessage(errMsg);
			}
		}
	}
	
	/**
	 * Adds a message to the message log
	 * @param msg
	 */
	private void addMessage(String msg) {
		messageBuilder.append(msg).append(BREAK);
	}
	
}
