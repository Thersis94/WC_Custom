package com.universal.util;

// JDK 7.x
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
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

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
	
	private String categoriesSourceFile = null;
	private String productsSourceFile = null;
	private String optionsSourceFile = null;
	private String personalizationSourceFile = null;
	
	private List<String> catalogIds;
	private String catalogId;
	private String catalogPrefix;
	private String catalogModelYear;
	
	private String topLevelCategoryId;
	private String skipCategoryId;
	private String featureCategoryId;
	
	private Map<String,String> prefixes; // key is catalogId
	private Map<String,String> sourceURLs; // key is catalogId
	private String[] sourceFileList;
	private Map<String, Integer> headers = null;
	private Map<String, String> productCategories = null;
	private Map<String, String[]> productParents = null;
	
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
			uci.closeDbConnection();
			System.exit(-1);
		} catch (SQLException sqle) {
			log.error("Fatal Error importing catalogs, ", sqle);
			uci.closeDbConnection();
			System.exit(-1);
		} finally {
			uci.closeDbConnection();
		}
		
		//uci.sendEmailNotification();
		// Display invalid cats/products/attributes
		//uci.logMisMatches();
		
		long end = System.currentTimeMillis();
		log.info("Completed Product Import in " + ((end - start) / 1000) + " seconds");
	}

	/**
	 * Loops catalogs list and imports each catalog in the list.
	 * @throws IOException
	 * @throws SQLException
	 */
	private void processImports() throws IOException, SQLException {
		String catalogSourcePath = null;
		for (String catalog : catalogIds) {
			try {
				catalogSourcePath = retrieveSourceFiles(catalog);
				importCatalog(catalog, catalogSourcePath);
			} catch (FileNotFoundException fnfe) {
				log.error("IMPORT FAILED for catalog : " + catalog + ", " + fnfe);
			} catch (IOException | SQLException e) {
				log.error("IMPORT FAILED for catalog : " + catalog + ", " + e);
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
	private String retrieveSourceFiles(String catalogId) 
			throws IOException {
		//call retriever.
		CatalogRetriever cr = new CatalogRetriever(config);
		return cr.retrieveCatalogForImport(catalogId, sourceURLs.get(catalogId), sourceFileList);
	}
		
	/**
	 * Imports catalogs
	 * @param catalogId
	 * @param catalogSourcePath
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws SQLException
	 */
	private void importCatalog(String catalogId, String catalogSourcePath) 
			throws FileNotFoundException, IOException, SQLException {
		// turn off auto commit so that we can rollback in case of exception
		dbConn.setAutoCommit(false);
		
		// delete categories and products before importing
		manageDeletes(catalogId);
		// import category data
		importCategories(catalogId, catalogSourcePath);
		// import product data
		importProducts(catalogId, catalogSourcePath);
		
		// commit changes if all were successful
		dbConn.commit();
		dbConn.setAutoCommit(true);
		log.info("Catalog import for " + catalogId + " has completed.");

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
	 * @param catalogId
	 * @param catalogSourcePath
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws SQLException
	 */
	private void importCategories(String catalogId, String catalogSourcePath) 
			throws FileNotFoundException, IOException, SQLException {
		// retrieve categories from source file.
		Map<String, ProductCategoryVO> cats = this.retrieveCategories(catalogSourcePath);
		
		// add categories
		Map<String, String> catParentMap = this.insertCategories(cats);
		
		// circle back and update category parent values
		this.updateCategoryParents(catParentMap);
	}
	
	/**
	 * Parses categories from the categories source file.
	 * @param catalogSourcePath
	 * @return A Map of categories.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private Map<String, ProductCategoryVO> retrieveCategories(String catalogSourcePath) 
			throws FileNotFoundException, IOException {
		BufferedReader data = null;
		String fullPath = catalogSourcePath + categoriesSourceFile;
		try {
			data = new BufferedReader(new FileReader(fullPath));
		} catch (FileNotFoundException fnfe) {
			String errMsg = "Categories source file not found, file path: " + fullPath;
			log.error(errMsg);
			throw new FileNotFoundException(errMsg);
		}
		
		Map<String, ProductCategoryVO> cats = new LinkedHashMap<String, ProductCategoryVO>();
		String temp;
		
		for (int i=0; (temp = data.readLine()) != null; i++) {
			String[] fields = temp.split(DELIMITER_SOURCE);
			if (i == 0) {
				this.parseHeaderRow(fields);
				continue; // skip the header row
			}
			
			String url = (fields[headers.get("CATEGORY_NAME")]).replace(" ", "_");
			url = url.replace("-", "_");
			url = StringUtil.formatFileName(url);
			url = url.replace("_", "-");
			url = url.replace("--", "-");
			
			String catCode = catalogPrefix + fields[headers.get("CATEGORY_CODE")];
			ProductCategoryVO vo = new ProductCategoryVO();
			vo.setCategoryCode(catCode);
			vo.setParentCode(catalogPrefix + fields[headers.get("CATEGORY_PARENT")]);
			vo.setCategoryUrl(url);
			vo.setCategoryName(fields[headers.get("CATEGORY_NAME")]);
			cats.put(catCode, vo);
		}
		
		try {
			data.close();
		} catch (Exception e) {
			log.error("Error closing BufferedReader, ", e);
		}
		
		log.info("Categories retrieved: " + cats.size());
		
		return cats;
	}
	
	/**
	 * Inserts category records into the product category table in the db.
	 * @param categoryMap
	 * @return
	 * @throws SQLException
	 */
	private Map<String, String> insertCategories(Map<String, ProductCategoryVO> categoryMap) throws SQLException {
		Map<String, String> categoryParentMap = new LinkedHashMap<String, String>();
		StringBuilder sb = new StringBuilder();
		sb.append("insert into product_category (product_category_cd, catalogId, ");
		sb.append("parent_cd, category_nm, active_flg, create_dt, title_nm, meta_desc, ");
		sb.append("meta_kywd_txt, category_url, short_desc, url_alias_txt) ");
		sb.append("values(?,?,?,?,?,?,?,?,?,?,?,?)");
		
		PreparedStatement ps = dbConn.prepareStatement(sb.toString());
		int ctr=0;

		for (String key : categoryMap.keySet()) {
			ProductCategoryVO vo = categoryMap.get(key);
			if (skipCategoryId.equalsIgnoreCase(vo.getParentCode())) {
				log.info("Skipping 'zz' category");
				continue;
			}
			
			String sDesc = this.buildShortDescription(vo, categoryMap);						
			ps.setString(1, key);
			ps.setString(2, catalogId);
			ps.setString(3, null);
			ps.setString(4, vo.getCategoryName());
			ps.setInt(5, 1);
			ps.setTimestamp(6, Convert.getCurrentTimestamp());
			ps.setString(7, "");
			ps.setString(8, "");
			ps.setString(9, "");
			ps.setString(10, vo.getCategoryUrl());
			ps.setString(11, sDesc); 
			if (key.indexOf(catalogPrefix) > -1) {
				// strip prefix from key for use as
				ps.setString(12, key.substring(key.indexOf(catalogPrefix) + catalogPrefix.length()));
			} else {
				ps.setString(12, key);
			}
			
			try {
				ps.executeUpdate();
				categoryParentMap.put(key, vo.getParentCode());
				ctr++;

			} catch (Exception e) {
				log.error("Failed insert, key/parent/catname: " + key + "/" + vo.getParentCode() + "/" + vo.getCategoryName() + " ---> " + e.getMessage());
			}
		}
		
		if (ps != null) {
			try {
				ps.close();
			} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
		}
		
		log.info("Categories inserted: " + ctr);
		return categoryParentMap;
	}
	
	/**
	 * Updates the parent category code on newly inserted category records.
	 * @param categoryParentMap
	 * @throws SQLException
	 */
	private void updateCategoryParents(Map<String, String> categoryParentMap) throws SQLException {
		String sql = "update product_category set parent_cd = ? where product_category_cd = ? and catalogId = ?";
		PreparedStatement ps = dbConn.prepareStatement(sql);
		int ctr=0;
		for (String s : categoryParentMap.keySet()) {
			if (topLevelCategoryId.equals(categoryParentMap.get(s))) {
				ps.setString(1, null);
			} else {
				ps.setString(1, categoryParentMap.get(s));
			}
			ps.setString(2, s);
			ps.setString(3, catalogId);
			
			try {
				ps.executeUpdate();
				ctr++;
			} catch (Exception e) {
				misMatchedParentCategories.add(categoryParentMap.get(s));
			}
		}

		if (ps != null) {
			try {
				ps.close();
			} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
		}
		log.info("Category parents updated: " + ctr);
	}
	
	/**
	 * Manages importing products 
	 * @param catalogId
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws SQLException
	 */
	private void importProducts(String catalogId, String catalogSourcePath) throws FileNotFoundException, IOException, SQLException {
		// retrieve products from source file
		List<ProductVO> prods = this.retrieveProducts(catalogSourcePath);
		// add products
		this.insertProducts(prods);
		// update product groups
		this.updateProductGroups();
		// update products
		this.updateProducts(prods);
		
		this.manageOptions(catalogSourcePath);
		
		// add product options.
		this.insertProductOptions(prods, catalogSourcePath);
		// add product personalization
		this.insertProductPersonalization(prods, catalogSourcePath);
		// add the product categories
		this.addProductCategoryXR();
		// Update the price ranges for the product groups
		this.assignPriceRange();
	}
	
	/**
	 * Retrieves and parses the products source file and inserts product records into
	 * the products table in the db.
	 * @return
	 * @throws IOException
	 */
	private List<ProductVO> retrieveProducts(String catalogSourcePath) throws FileNotFoundException, IOException {
		BufferedReader data = null;
		String fullPath = catalogSourcePath + productsSourceFile;
		try {
			data = new BufferedReader(new FileReader(fullPath));
		} catch (FileNotFoundException fnfe) {
			String errMsg = "Products source file not found, file path: " + fullPath;
			log.error(errMsg);
			throw new FileNotFoundException(errMsg);
		}
		
		List<ProductVO> prods = new ArrayList<ProductVO>();
		String temp = null;
		
		for (int i=0; (temp = data.readLine()) != null; i++) {
			String[] fields = temp.split(DELIMITER_SOURCE);
			if (i == 0) {
				this.parseHeaderRow(fields);
				continue; // skip the header row
			}

			try {
				ProductVO prod = new ProductVO();
				prod.setProductId(catalogPrefix + fields[headers.get("SKUID")]); // SKUID with prefix
				prod.setProductName(this.stripQuotes(fields[headers.get("NAME")])); // NAME
				prod.setMsrpCostNo(Convert.formatDouble(fields[headers.get("PRICE")])); //PRICE
				prod.setDescText(this.stripQuotes(fields[headers.get("DESCRIPTION")])); // DESCRIPTION
				prod.setCustProductNo(fields[headers.get("CUSTOM")]); // CUSTOM
				prod.setMetaKywds(this.stripQuotes(fields[headers.get("KEYWORDS")])); // KEYWORDS
				if (catalogId.equals("SUPPORT_PLUS_CATALOG")) {
					if (headers.get("IMGA") != null) {
						prod.setImage(fields[headers.get("IMGA")]); // IMGA	
					} else {
						prod.setImage(fields[headers.get("IMAGE")]); // IMAGE
					}
				} else {
					prod.setImage(fields[headers.get("IMAGE")]); // IMAGE
				}
				prod.setThumbnail(fields[headers.get("SMLIMG")]); // SMLIMG
				prod.setUrlAlias(fields[headers.get("SKUID")]); // SKUID
				prods.add(prod);
				
				// Add the grouping data to a map.  If the product id ends with a "G"
				// it is a group.  Add the product id for the group and the children ids to
				// a map which will be run later to update the parent entries for the 
				// Product group.
				if (prod.getProductId().endsWith("G")) {
					int fieldIndex = -1;
					if (headers.get("ITEMGROUP") != null) {
						fieldIndex = headers.get("ITEMGROUP");
					} else if (headers.get("RELATED") != null) {
						fieldIndex = headers.get("RELATED");
					}
					if (fieldIndex > -1) {
						log.debug("found group: " + prod.getProductId());
						String[] children = fields[fieldIndex].split(";"); // ITEMGROUP	
						productParents.put(prod.getProductId(), children);
					}
				}
				
				// Add the categories for the products
				productCategories.put(prod.getProductId(), fields[headers.get("CATEGORY")]); // CATEGORY
			} catch (Exception e) {
				log.error("Data error processing product record #: " + i + ", " + e);
			}
		}
		
		try {
			data.close();
		} catch (Exception e) {
			log.error("Error closing BufferedReader, ", e);
		}
		
		log.info("Products found: " + prods.size());
		return prods;
	}
	
	
	/**
	 * Updates product parent records
	 * @throws SQLException
	 */
	private void updateProductGroups() throws SQLException {
		String s = "update product set parent_id = ? where product_id = ? ";
		PreparedStatement ps = dbConn.prepareStatement(s);
		int ctr=0;
		String parentId = null;
		String[] vals = null;
		String prodId = null;
		for (Iterator<String> iter = productParents.keySet().iterator(); iter.hasNext();  ) {
			parentId = iter.next();
			vals = productParents.get(parentId);
			for (int i=0; i < vals.length; i++) {
				prodId = catalogPrefix + vals[i];
				ps.setString(1, parentId);
				ps.setString(2, prodId);
				ctr++;
				try {
					ps.executeUpdate();
				} catch (Exception e) {
					log.error("Unable to add product parent: " + parentId + "|" + prodId + "\t\t" + e.getMessage());
				}
			}
		}

		if (ps != null) {
			try {
				ps.close();
			} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
		}
		log.info("Product group parents updated: " + ctr);
	}
	
	/**
	 * Inserts product-category relationship records.
	 * @throws SQLException
	 */
	private void addProductCategoryXR() throws SQLException {
		StringBuilder sb = new StringBuilder("insert into product_category_xr (product_category_cd, ");
		sb.append("product_id, create_dt) values (?,?,?)");
		
		PreparedStatement ps = dbConn.prepareStatement(sb.toString());
		int ctr=0;

		// Load the categories.  If there are no categories, skip that entry
		for (String prodId : productCategories.keySet()) {
			String catIds = StringUtil.checkVal(productCategories.get(prodId)).trim();
			if (catIds.length() == 0) continue;
			
			String[] cats = catIds.split(";");
			for (int i=0; i < cats.length; i++) {
				if (StringUtil.checkVal(cats[i]).length() == 0) continue;
				String catCode = (catalogPrefix + cats[i]);
				ps.setString(1, catCode);
				ps.setString(2, prodId);
				ps.setTimestamp(3, Convert.getCurrentTimestamp());
				ctr++;
				try {
					ps.executeUpdate();
				}catch (Exception e) {
					misMatchedCategories.add(cats[i]);
				}
			}
		}
		
		if (ps != null) {
			try {
				ps.close();
			} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
		}
		log.info("Product-category XR records added: " + ctr);		
	}

	/**
	 *Updates product records. 
	 * @param prods
	 * @throws SQLException
	 */
	private void updateProducts(List<ProductVO> prods) throws SQLException {
		String s = "update product set cust_product_no = ? where product_id = ?";
		PreparedStatement ps = dbConn.prepareStatement(s);
		int ctr=0;

		for (int i=0; i < prods.size(); i++) {
			ProductVO p = prods.get(i);
			ps.setString(1, p.getCustProductNo());
			ps.setString(2, p.getProductId());
			
			try {
				ps.executeUpdate();
				ctr++;
			} catch (Exception e) {
				log.error("Error adding custom product number for product: " + p.getProductId() + ", " + e.getMessage());
			}
		}

		if (ps != null) {
			try {
				ps.close();
			} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
		}
		log.info("Products updated: " + ctr);
	}
	
	/**
	 * 
	 * @param prods
	 * @throws SQLException
	 */
	private void insertProducts(List<ProductVO> prods) throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("insert into product (product_id, catalogId, parent_id, ");
		sb.append("cust_product_no, product_nm, desc_txt, status_no, msrp_cost_no, ");
		sb.append("create_dt, image_url, thumbnail_url, short_desc, product_url, ");
		sb.append("currency_type_id, title_nm, meta_desc, meta_kywd_txt, url_alias_txt) ");
		sb.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		
		PreparedStatement ps = dbConn.prepareStatement(sb.toString());
		int ctr=0;

		for (int i=0; i < prods.size(); i++) {
			ProductVO p = prods.get(i);
			ps.setString(1, p.getProductId());
			ps.setString(2, catalogId);
			ps.setString(3, p.getParentId());
			ps.setString(4, p.getCustProductNo());
			ps.setString(5, p.getProductName());
			ps.setString(6, p.getDescText());
			ps.setInt(7, 5);
			ps.setDouble(8, p.getMsrpCostNo());
			ps.setTimestamp(9, Convert.getCurrentTimestamp());
			ps.setString(10, p.getImage());
			ps.setString(11, p.getThumbnail());
			ps.setString(12, p.getShortDesc());
			ps.setString(13, p.getProductUrl());
			ps.setString(14, "dollars");
			ps.setString(15, p.getTitle());
			ps.setString(16, p.getMetaDesc());
			ps.setString(17, p.getMetaKywds());
			ps.setString(18, p.getUrlAlias());
			try {
				ps.executeUpdate();
				ctr++;
			} catch (Exception e) {
				log.error("Error inserting product: " + p.getProductId() + ", " + e);
			}
		}
		
		if (ps != null) {
			try {
				ps.close();
			} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
		}
		log.debug("Products inserted: " + ctr);
	}
		
	/**
	 * Assigns the range of prices to the product groups
	 * @throws SQLException
	 */
	private void assignPriceRange() throws SQLException {
		StringBuilder s = new StringBuilder();
		s.append("update PRODUCT set PRICE_RANGE_LOW_NO = p2.LOW, PRICE_RANGE_HIGH_NO = p2.HIGH ");
		s.append("from product p inner join ( ");
		s.append("select PARENT_ID, max(msrp_cost_no) AS HIGH, MIN(msrp_cost_no) AS LOW "); 
		s.append("from product where catalogId='").append(catalogId).append("' ");
		s.append("and PARENT_ID is not null group by PARENT_ID ");
		s.append(") as p2 on p.PRODUCT_ID = p2.PARENT_ID ");
		
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		try {
			ps.executeUpdate();
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
			}
		}
		log.info("Price ranges updated.");
	}
	
	/**
	 * Reads the options file and attempts to insert options into the 
	 * attributes table.
	 * @param catalogSourcePath
	 */
	private void manageOptions(String catalogSourcePath) {
		OptionsImporter oi = new OptionsImporter(dbConn);
		try {
			oi.manageOptions(catalogSourcePath, optionsSourceFile);
		} catch (Exception e) {
			// catch only, no action, logged already
		}
	}
	
	/**
	 * Inserts product-options associations into the appropriate table. 
	 * @param products
	 * @param catalogSourcePath
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws SQLException
	 */
	private void insertProductOptions(List<ProductVO> products, String catalogSourcePath) 
			throws FileNotFoundException, IOException, SQLException  {
		BufferedReader data = null;
		String fullPath = catalogSourcePath + optionsSourceFile;
		try {
			data = new BufferedReader(new FileReader(fullPath));	
		} catch (FileNotFoundException fnfe) {
			String errMsg = "Options source file not found!  File path is: " + fullPath;
			log.error(errMsg);
			throw new FileNotFoundException(errMsg);
		}

		StringBuilder s = new StringBuilder();
		s.append("insert into product_attribute_xr (product_attribute_id, attribute_id, ");
		s.append("product_id, model_year_no, value_txt, create_dt, currency_type_id, ");
		s.append("msrp_cost_no, attrib1_txt, attrib2_txt, order_no) values (?,?,?,?,?,?,?,?,?,?,?)");
		
		int ctr = 0;
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		String temp = null;
		String prevSrcProdId = "";
		String attribSelectLvl = null; //corresponds to a List index value
		int attribSelectOrder = 0;
		for (int i=0; (temp = data.readLine()) != null; i++) {
			attribSelectLvl = "0";
			String[] fields = temp.split(DELIMITER_SOURCE);
			if (i == 0) {
				this.parseHeaderRow(fields);
				continue; // skip to next row
			}
			String srcProdId = fields[headers.get("SKU")];
			String prodId = null;
			if (srcProdId.indexOf("_") > -1) {
				attribSelectLvl = srcProdId.substring(srcProdId.length() - 1);
				prodId = srcProdId.substring(0, (srcProdId.length() - 2));
			} else {
				prodId = srcProdId;
			}
			// add prefix to prodId
			prodId = catalogPrefix + prodId;
			if (srcProdId.equals(prevSrcProdId)) {
				attribSelectOrder++; // increment select order
			} else {
				attribSelectOrder = 0; // reset select order
			}
			// retrieve attribId
			String attribId = null;
			try { // enclosed in try/catch in case of index array out of bounds due to missing field val.
				attribId = fields[headers.get("TABLETYPE")];
			} catch (Exception e) {
				log.error("Error getting product attribute ID value, source field is blank for product ID: " + prodId);
			}
			attribId = this.formatAttribute(attribId);
			if (attribId == null) continue;
			try {
				ps.setString(1, new UUIDGenerator().getUUID());	//product_attribute_id
				ps.setString(2, attribId); //attribute_id
				ps.setString(3, prodId);	//product_id
				ps.setString(4, catalogModelYear);	//model_year_no
				ps.setString(5, fields[headers.get("CODE")]);	// value_txt
				ps.setTimestamp(6, Convert.getCurrentTimestamp());	//create_dt
				ps.setString(7, "dollars");	//curr_type
				ps.setInt(8, Convert.formatInteger(fields[headers.get("PRICEMOD")]));		//msrp_cost_no
				ps.setString(9, fields[headers.get("DESCRIPTION")]);	//attrib1
				ps.setString(10, attribSelectLvl); //attrib2
				ps.setInt(11, attribSelectOrder); //order_no
				ps.executeUpdate();
				ctr++;

			} catch (Exception e) {
				String cause = null;
				if (e.getMessage().contains("column 'PRODUCT_ID'")) {
					//cause = "product ID foreign key not found";
					misMatchedOptions.add(prodId);
				} else if (e.getMessage().contains("column 'ATTRIBUTE_ID'")) {
					//cause = "attribute ID foreign key not found";
					log.error("misMatcheded attribute ID at line: " + (i + 1) + " in input file:prodId/attribId: " + prodId + "/" + attribId);
					misMatchedAttributes.add(attribId);
				} else {
					cause = e.getMessage();
					log.error("Error adding attribute XR: prodId/attribId: " + prodId + "/" + attribId + ", " + cause);
				}
			}
			prevSrcProdId = srcProdId;
		}

		try {
			data.close();
		} catch (Exception e) {
			log.error("Error closing BufferedReader, ", e);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
			}
		}
		log.info("Options added: " + ctr);
	}
	
	/**
	 * 
	 * @param products
	 * @throws SQLException
	 * @throws IOException
	 */
	private void insertProductPersonalization(List<ProductVO> products, String catalogSourcePath) 
			throws FileNotFoundException, IOException, SQLException  {
		StringBuilder sb = new StringBuilder();
		sb.append("insert into product_attribute_xr (product_attribute_id, attribute_id, ");
		sb.append("product_id, model_year_no, value_txt, create_dt, currency_type_id, msrp_cost_no, attrib1_txt, attrib2_txt, attrib3_txt) ");
		sb.append("values (?,?,?,?,?,?,?,?,?,?,?)");
		
		BufferedReader data = null;
		String fullPath = catalogSourcePath + personalizationSourceFile;
		try {
			data = new BufferedReader(new FileReader(fullPath));
		} catch (FileNotFoundException fnfe) {
			String errMsg = "Personalization source file not found, file path: " + fullPath;
			log.error(errMsg);
			throw new FileNotFoundException(errMsg);
		}

		int ctr = 0;
		PreparedStatement ps = dbConn.prepareStatement(sb.toString());
		String temp = null;
		for (int i=0; (temp = data.readLine()) != null; i++) {
			String[] fields = temp.split(DELIMITER_SOURCE);
			if (i == 0) {
				this.parseHeaderRow(fields);
				continue; // skip to next row
			}
			String prodId = catalogPrefix + fields[headers.get("CUSTOM")];
			try {
				ps.setString(1, new UUIDGenerator().getUUID());	//product_attribute_id
				ps.setString(2, "USA_CUSTOM");	//attribute_id
				ps.setString(3, prodId);	//product_id
				ps.setString(4, catalogModelYear);	//model_year_no
				ps.setString(5, fields[headers.get("DATA")]); //value_txt
				ps.setTimestamp(6, Convert.getCurrentTimestamp());	//create_dt
				ps.setString(7, "DOLLARS");	//currency_type_id
				ps.setInt(8, 0);	//msrp_cost_no
				ps.setString(9, fields[headers.get("PROMPT")]);		//attrib1_txt
				ps.setString(10, fields[headers.get("MAXLENGTH")]);	//attrib2_txt
				if(fields.length > 5) {
					ps.setString(11, fields[headers.get("REQUIRED")]);	//attrib3_txt
				} else {
					ps.setString(11, "0");	//attrib3_txt
				}
				ps.executeUpdate();
				ctr++;

			} catch (Exception e) {
				if (e.getMessage().contains("column 'PRODUCT_ID'")) {
					//cause = "product ID foreign key not found";
					misMatchedPersonalization.add(prodId);
				} else {
					log.error("Error adding personalization option for product: " + prodId + ", " + e.getMessage());
				}
			}
		}
		
		try {
			data.close();
		} catch (Exception e) {
			log.error("Error closing BufferedReader, ", e);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
			}
		}
		log.info("Personalization options added: " + ctr);
		
	}
	
	/**
	 * Formats an attribute String
	 * @param attrib
	 * @return
	 */
	private String formatAttribute(String attrib) {
		String newAttr = null;
		if (attrib != null && attrib.length() > 0) {
			newAttr = attrib.replace(" ", "");
			newAttr = newAttr.replace("-", "");
			newAttr = StringUtil.formatFileName(newAttr);
			newAttr = ATTRIBUTE_PREFIX + newAttr.toUpperCase();	
		}
		return newAttr;
	}
	
	/**
	 * DEBUG method. Write mismatch data to the log
	 */
	private void logMisMatches() {
		log.debug("");
		if (misMatchedParentCategories.size() > 0) {
			log.debug("*** Invalid parent category references in category data file: ***");
			for (Iterator<String> iter = misMatchedParentCategories.iterator(); iter.hasNext(); ) {
				log.debug("---->" + iter.next());
			}
			log.debug("*** end ***");
			log.debug("");
		}
		if (misMatchedCategories.size() > 0) {
			log.debug("*** Invalid category references in products data file: ***");
			for (Iterator<String> iter = misMatchedCategories.iterator(); iter.hasNext(); ) {
				log.debug("---->" + iter.next());
			}
			log.debug("*** end ***");
			log.debug("");
		}			
		if (misMatchedOptions.size() > 0) {
			log.debug("*** Invalid product references in options file: ***");
			for (Iterator<String> iter = misMatchedOptions.iterator(); iter.hasNext(); ) {
				log.debug("---->" + iter.next());
			}
			log.debug("*** end ***");
			log.debug("");
		}
		if (misMatchedPersonalization.size() > 0) {
			log.debug("*** Invalid product ID references in personalization file: ***");
			for (Iterator<String> iter = misMatchedPersonalization.iterator(); iter.hasNext(); ) {
				log.debug("---->" + iter.next());
			}
			log.debug("*** end ***");
			log.debug("");
		}
		if (misMatchedAttributes.size() > 0) {
			log.debug("*** Invalid attribute references: ***");
			for (Iterator<String> iter = misMatchedAttributes.iterator(); iter.hasNext(); ) {
				log.debug("---->" + iter.next());
			}
			log.debug("*** end ***");
			log.debug("");
		}

	}
	
	/**
	 * Recursively builds the pipe-delimited short description (SHORT_DESC) for a product category based on the 
	 * category's parent(s) category url(s).
	 * @param prodCat
	 * @param data
	 * @return
	 */
	private String buildShortDescription(ProductCategoryVO prodCat, Map<String, ProductCategoryVO> data) {
		String cUrl = prodCat.getCategoryUrl();
		if (StringUtil.checkVal(prodCat.getParentCode()).length() > 0 && data.get(prodCat.getParentCode()) != null) {
			if (! data.get(prodCat.getParentCode()).getCategoryCode().equals(topLevelCategoryId)) {
				String parentPath = this.buildShortDescription((data.get(prodCat.getParentCode())),data);
				cUrl = parentPath + "|" + cUrl;
			}
		}
		return cUrl;
	}
	
	/**
	 * Strips consecutive double quotes and leading/trailing double quotes from a String.
	 * @param value
	 */
	private String stripQuotes(String value) {
		if (value.length() > 0) {
			// replace double quotes
			value = value.replace("\"\"", "\"");
			if (value.startsWith("\"")) {
				// remove leading double quote
				value = value.substring(1);
			} 
			if (value.endsWith("\"")) {
				// remove trailing double quote
				value = value.substring(0, (value.length() - 1));
			}
		}
		return value;
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
		headers = new HashMap<>();
		productCategories = new LinkedHashMap<String, String>();
		productParents = new LinkedHashMap<String, String[]>();
		misMatchedCategories = new HashSet<String>();
		misMatchedParentCategories = new HashSet<String>();
		misMatchedOptions = new HashSet<String>();
		misMatchedPersonalization = new HashSet<String>();
		misMatchedAttributes = new HashSet<String>();
		
		catalogModelYear = config.getProperty("catalogModelYear");
		
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
		categoriesSourceFile = sourceFileList[0];
		productsSourceFile = sourceFileList[1];
		optionsSourceFile = sourceFileList[2];
		personalizationSourceFile = sourceFileList[3];
		
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
		log.info("Attempting to connect to " + config.getProperty("dbUrl"));
		return dbc.getConnection();
	}
	
	/**
	 * 
	 */
	private void closeDbConnection() {
		if (dbConn != null) {
			try {
				dbConn.close();
			} catch (SQLException sqle) {
				log.error("Unable to close DB connection, ", sqle);
			}
		}
	}
	
	/**
	 * Parses the header row's column names into a map of column name, index.
	 * @param columns
	 */
	private void parseHeaderRow(String[] columns) {
		headers = new HashMap<String, Integer>();
		for (int i = 0; i < columns.length; i++) {
			headers.put(columns[i].toUpperCase(), new Integer(i));
		}
	}
}
