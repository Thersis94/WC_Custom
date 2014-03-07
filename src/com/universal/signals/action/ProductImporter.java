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
import java.sql.ResultSet;
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

// WC 2.0 libs
import com.smt.sitebuilder.action.commerce.product.ProductCatalogAction;

// WC_Custom libs
import com.universal.catalog.CatalogImport;
import com.universal.catalog.CatalogImportFactory;

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
public class ProductImporter {
	private String catFileLoc = null;
	private String prodFileLoc = null;
	private String optsFileLoc = null;
	private String persFileLoc = null;
	private Properties config = new Properties();
	private static final Logger log = Logger.getLogger("ProductImporter");
	private Connection conn = null;
	private Map<String, Integer> headerMap = null;
	private Map<String, String> prodCats = new LinkedHashMap<String, String>();
	private Map<String, String[]> productParent = new LinkedHashMap<String, String[]>();
	Set<String> misMatchCat = new HashSet<String>();
	Set<String> misMatchParentCat = new HashSet<String>();
	Set<String> misMatchProdOptions = new HashSet<String>();
	Set<String> misMatchProdCustom = new HashSet<String>();
	Set<String> misMatchAttrib = new HashSet<String>();

	private CatalogImport cImp;
	
	//private static final int BATCH_SIZE = 1000;
	private boolean preserveCategories = false;
	/*
	private String PRODUCT_CATALOG_ID = "9F78AAF478CE4AC79D0778D389EDEA9C";
	private String PRODUCT_AND_CATEGORY_PREFIX = "";
	private String CATALOG_MODEL_YEAR_ID = "USA_SIGNALS_2013";
	private String fileLoc = "C:\\Temp\\USA_catalog_import\\signals\\";
	*/
	/*
	private String PRODUCT_CATALOG_ID = "BAS_BLEU_CATALOG";
	private String PRODUCT_AND_CATEGORY_PREFIX = "USA_5_";
	private String CATALOG_MODEL_YEAR_ID = "BAS_BLEU_2013";
	private String fileLoc = "C:\\Temp\\USA_catalog_import\\basBleu\\";
	*/
	/*
	private String PRODUCT_CATALOG_ID = "SUPPORT_PLUS_CATALOG";
	private String PRODUCT_AND_CATEGORY_PREFIX = "USA_4_";
	private String CATALOG_MODEL_YEAR_ID = "SUPPORT_PLUS_2013";
	private String fileLoc = "C:\\Temp\\USA_catalog_import\\supportPlus\\";
	*/
	/*
	private String PRODUCT_CATALOG_ID = "THE_WIRELESS_CATALOG";
	private String PRODUCT_AND_CATEGORY_PREFIX = "USA_3_";
	private String CATALOG_MODEL_YEAR_ID = "THE_WIRELESS_2013";
	private String fileLoc = "C:\\Temp\\USA_catalog_import\\theWireless\\";
	*/
	
	private String PRODUCT_CATALOG_ID = "WHAT_ON_EARTH_CATALOG";
	private String PRODUCT_AND_CATEGORY_PREFIX = "USA_2_";
	private String CATALOG_MODEL_YEAR_ID = "WHAT_ON_EARTH_2013";
	private String fileLoc = "C:\\Temp\\USA_catalog_import\\whatOnEarth\\";
	
	private String TOP_LEVEL_CATEGORY_ID = PRODUCT_AND_CATEGORY_PREFIX + "100";
	private String SKIP_CATEGORY_ID = PRODUCT_AND_CATEGORY_PREFIX + "zz";
	
	/**
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws InvalidDataException 
	 * @throws DatabaseException 
	 * 
	 */
	public ProductImporter() throws Exception {
		// Initialize the logger
		PropertyConfigurator.configure("scripts/usa_importer_log4j.properties");
		
		// Load the config file
		config.load(new FileInputStream(new File("scripts/usa_importer.properties")));
		
		// load the catalogs for this org
		// this.queryForCatalogs();
		
		// Load the file location
		//String fileLoc = config.getProperty("fileLoc");
		catFileLoc = fileLoc + "categories.txt";
		prodFileLoc = fileLoc + "products.txt";
		optsFileLoc = fileLoc + "options.txt";
		persFileLoc = fileLoc + "personalization.txt";
		// Get the DB Connection
		conn = getConnection();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)  {
		long start = System.currentTimeMillis();
		ProductImporter pi = null;

		try {
			pi = new ProductImporter();
			log.info("Starting Product Importer");
			
			// Get the DB conenction 
			//pi.conn = pi.getConnection();
			
			// Process the importer
			pi.processRequest();
			
			// Display invalid cats/products/attributes
			pi.logMisMatches();
		} catch (Exception e) {
			log.error("Unable to process importer", e);
		} finally {
			try {
				pi.conn.close();
			} catch (Exception e) {
				log.error("Error closing connection, ", e);
			}
		}

		long end = System.currentTimeMillis();
		log.info("Completed Product Import in " + ((end - start) / 1000) + " seconds");
	}
	
	/**
	 * Initializes the appropriate values required for processing a catalog
	 * @param catId
	 * @param catModYr
	 * @param prefix
	 * @param sourcePath
	 */
	@SuppressWarnings("unused")
	private void initializeNextCatalog(String catId, String catModYr, String prefix, String sourcePath) {
		cImp = CatalogImportFactory.getCatalogImport(catId);
		cImp.setCatalogId(catId);
		cImp.setCatalogModelYear(catModYr);
		cImp.setProductAndCategoryPrefix(prefix);
		cImp.setImportSourcePath(sourcePath);
	}
	
	/**
	 * 
	 * @throws IOException
	 * @throws SQLException 
	 */
	public void processRequest() throws IOException, SQLException {
		//load the default product catalog for this org.
		
		deleteExistingEntries();
		log.debug("preserving categories?: " + preserveCategories);
		if (! preserveCategories) {
			// retrieve and load the categorires
			Map<String, ProductCategoryVO> cats = this.retrieveCategories();
			log.debug("category map size: " + cats.size());
			log.debug("category prefix: " + PRODUCT_AND_CATEGORY_PREFIX);
			Map<String, String> catParentMap = this.addCategories(cats);
			this.updateCategoryParents(catParentMap);
		}
		// Add the products
		List<ProductVO> prods = this.retrieveProducts();
		//int x = 1; if (x == 1) return;
				
		this.addProducts(prods);
		this.updateProductGroup();
		this.updateProducts(prods);
		log.debug("Number of products: " + prods.size());
		this.addProductAttribute(prods);
		this.addCustomProductAttribute(prods);
		// add the product categories
		this.addProductCategoryXR();
		
		// Update the price ranges for the product groups
		this.assignPriceRange();

	}
	/**
	 * Remove existing entries from database to prevent duplicates.
	 */
	public void deleteExistingEntries(){
		
		String[] sql = null;
		
		if (preserveCategories) {  //delete products only
			sql = new String[] {
				"delete from PRODUCT where product_catalog_id='" + PRODUCT_CATALOG_ID + "'"};
		} else {
			sql = new String[] { //delete categories and products
					"delete from PRODUCT_CATEGORY where product_catalog_id='" + PRODUCT_CATALOG_ID + "'",
					"delete from PRODUCT where product_catalog_id='" + PRODUCT_CATALOG_ID + "'"};
		}
		for (String s : sql) {
			log.debug("executing delete sql: " + s);
			try {
				PreparedStatement ps = conn.prepareStatement(s);
				ps.execute();
			} catch (SQLException e) {
				log.error("error purging USA product data", e);
			}
			
		}
	}
	
	/**
	 * 
	 * @throws SQLException
	 */
	public void updateProductGroup() throws SQLException {
		String s = "update product set parent_id = ? where product_id = ? ";
		PreparedStatement ps = conn.prepareStatement(s);
		int ctr=0;
		String parentId = null;
		String[] vals = null;
		String prodId = null;
		for (Iterator<String> iter = productParent.keySet().iterator(); iter.hasNext();  ) {
			parentId = iter.next();
			vals = productParent.get(parentId);
			for (int i=0; i < vals.length; i++) {
				prodId = PRODUCT_AND_CATEGORY_PREFIX + vals[i];
				ps.setString(1, parentId);
				ps.setString(2, prodId);
				ctr++;
				try {
					ps.executeUpdate();
					/*
					ps.addBatch();
					if(ctr % BATCH_SIZE == 0){
						log.debug("Updating product Groups: " + ctr);
						ps.executeBatch();
					}
					*/
				} catch (Exception e) {
					log.error("Unable to Add Parent: " + parentId + "|" + prodId + "\t\t" + e.getMessage());
				}
			}
		}
		try {
			// Update the final product Group
			//ps.executeBatch();
			conn.commit();
			log.debug("Updating product Groups: " + ctr);
			ps.close();
		} catch (Exception e) {
			log.error("error updating product groups, ", e);
		}
	}
	
	/**
	 * 
	 * @throws SQLException
	 */
	public void addProductCategoryXR() throws SQLException {
		String s = "insert into product_category_xr (product_category_cd, ";
		s += "product_id, create_dt) values (?,?,?)";
		
		PreparedStatement ps = conn.prepareStatement(s);
		int ctr=0;

		for (Iterator<String> iter = prodCats.keySet().iterator(); iter.hasNext(); ) {
			// Load the  categories.  If there are no categories, skip that entry
			String prodId = iter.next();
			String catIds = StringUtil.checkVal(prodCats.get(prodId)).trim();
			if (catIds.length() == 0) continue;
			
			String[] cats = catIds.split(";");
			for (int i=0; i < cats.length; i++) {
				if (StringUtil.checkVal(cats[i]).length() == 0) continue;
				//log.debug("cats[i]: " + cats[i]);
				String catCode = (PRODUCT_AND_CATEGORY_PREFIX + cats[i]);
				//log.debug("inserting prodId/catCode: " + prodId + "/" + catCode);
				ps.setString(1, catCode);
				ps.setString(2, prodId);
				ps.setTimestamp(3, Convert.getCurrentTimestamp());
				ctr++;
				try {
					ps.executeUpdate();
					/*
					ps.addBatch();
					if(ctr % BATCH_SIZE == 0){
						log.debug("Executing entries through " + ctr);
						ps.executeBatch();
					}
					*/
				}catch (Exception e) {
					misMatchCat.add(cats[i]);
					//log.error("Error trying to add product category XR record for catCode/prodId: " + catCode + "/" + prodId);
				}
			}
		}
		
		try {
			// Add the final ProductCategories
			//log.debug("Executing entries through " + ctr);
			//ps.executeBatch();
			conn.commit();
			log.debug("Added product category XR records " + ctr);
			ps.close();
		} catch (Exception e) {
			log.error("Error, Couldn't add product category XR records...", e);
		}
		
	}
	
	/**
	 * 
	 * @param data
	 * @throws SQLException
	 */
	public Map<String, String> addCategories(Map<String, ProductCategoryVO> data) throws SQLException {
		Map<String, String> catParentMap = new LinkedHashMap<String, String>();
		String s = "insert into product_category (product_category_cd, product_catalog_id, ";
		s += "parent_cd, category_nm, active_flg, create_dt, title_nm, meta_desc, ";
		s += "meta_kywd_txt, category_url, short_desc, url_alias_txt) ";
		s += "values(?,?,?,?,?,?,?,?,?,?,?,?)";
		
		PreparedStatement ps = conn.prepareStatement(s);
		int ctr=0;

		for (Iterator<String> iter = data.keySet().iterator(); iter.hasNext(); ) {
			String key = iter.next();
			//log.debug("category key: " + key);
			
			ProductCategoryVO vo = data.get(key);
			//if (TOP_LEVEL_CATEGORY_ID.equals(vo.getParentCode())) vo.setParentCode(null);
			if (SKIP_CATEGORY_ID.equalsIgnoreCase(vo.getParentCode())) {
				log.debug("found 'zz'...skipping this category...");
				continue;
			}
			
			//String cUrl = vo.getCategoryUrl();
			//if (StringUtil.checkVal(vo.getParentCode()).length() > 0 && data.get(vo.getParentCode()) != null) {
				//String parentPath = data.get(vo.getParentCode()).getCategoryUrl();
				//cUrl = parentPath + "|" + cUrl;
			//}
			
			String sDesc = this.buildShortDescription(vo, data);
			//log.debug("sDesc: " + sDesc);
						
			ps.setString(1, key);
			ps.setString(2, PRODUCT_CATALOG_ID);
			//ps.setString(3, vo.getParentCode());
			ps.setString(3, null);
			ps.setString(4, vo.getCategoryName());
			ps.setInt(5, 1);
			ps.setTimestamp(6, Convert.getCurrentTimestamp());
			ps.setString(7, "");
			ps.setString(8, "");
			ps.setString(9, "");
			ps.setString(10, vo.getCategoryUrl());
			ps.setString(11, sDesc); 
			if (key.indexOf(PRODUCT_AND_CATEGORY_PREFIX) > -1) {
				// strip prefix from key for use as
				ps.setString(12, key.substring(key.indexOf(PRODUCT_AND_CATEGORY_PREFIX) + PRODUCT_AND_CATEGORY_PREFIX.length()));
			} else {
				ps.setString(12, key);
			}
			
			try {
				//ps.addBatch();
				ps.executeUpdate();
				catParentMap.put(key, vo.getParentCode());
				ctr++;
				/*
				// Execute the batch
				if ((ctr % BATCH_SIZE) == 0) {
					log.debug("Adding categories: " + ctr);
					ps.executeBatch();
				}	
				*/		
				} catch (Exception e) {
				log.error("Failed insert, key/parent/catname: " + key + "/" + vo.getParentCode() + "/" + vo.getCategoryName() + " ---> " + e.getMessage());
			}
		}
		try {
			// Add the final categories
			//int[] count = ps.executeBatch();
			conn.commit();
			log.debug("Adding categories: " + ctr);
			ps.close();
		} catch (Exception e) {
			log.error("Error inserting categories into db...", e);
		}
		return catParentMap;
	}
	
	public void updateCategoryParents(Map<String, String> catParentMap) throws SQLException {
		String sql = "update product_category set parent_cd = ? where product_category_cd = ? and product_catalog_id = ?";
		PreparedStatement ps = conn.prepareStatement(sql);
		int ctr=0;
		for (String s : catParentMap.keySet()) {
			if (TOP_LEVEL_CATEGORY_ID.equals(catParentMap.get(s))) {
				ps.setString(1, null);
			} else {
				ps.setString(1, catParentMap.get(s));
			}
			ps.setString(2, s);
			ps.setString(3, PRODUCT_CATALOG_ID);
			
			try {
				ps.executeUpdate();
				ctr++;
			} catch (Exception e) {
				misMatchParentCat.add(catParentMap.get(s));
				//log.error("Failed category parent code update, key/parentCode: " + s + "/" + catParentMap.get(s) + " ---> " + e.getMessage());
			}
		}
		try {
			conn.commit();
			ps.close();
		} catch (Exception e) {
			log.error("Error updating category parents...", e);
		}
		log.debug("category parents updated: " + ctr);
	}
	
	public void updateProducts(List<ProductVO> prods) throws SQLException {
		String s = "update product set cust_product_no = ? where product_id = ?";
		PreparedStatement ps = conn.prepareStatement(s);
		int ctr=0;

		for (int i=0; i < prods.size(); i++) {
			ProductVO p = prods.get(i);
			ps.setString(1, p.getCustProductNo());
			ps.setString(2, p.getProductId());
			
			try {
				ps.executeUpdate();
				//ps.addBatch();
				ctr++;
				/*
				// Execute the batch
				if ((ctr % BATCH_SIZE) == 0) {
					log.debug("Updating Products: " + ctr);
					ps.executeBatch();
				}
				*/
				} catch (Exception e) {
				log.error("Unable to add custom product number for product: " + p.getProductId() + ", " + e.getMessage());
			}
		}
		try {
			// Update the final products
			//ps.executeBatch();
			conn.commit();
			log.debug("Updating Products: " + ctr);
			ps.close();
		} catch (Exception e) {
			log.error("Error updating products, ", e);
		}
	}
	
	/**
	 * 
	 * @param prods
	 * @throws SQLException
	 */
	public void addProducts(List<ProductVO> prods) throws SQLException {
		String s = "insert into product (product_id, product_catalog_id, parent_id, ";
		s += "cust_product_no, product_nm, desc_txt, status_no, msrp_cost_no, ";
		s += "create_dt, image_url, thumbnail_url, short_desc, product_url, ";
		s += "currency_type_id, title_nm, meta_desc, meta_kywd_txt, url_alias_txt) ";
		s += "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		
		PreparedStatement ps = conn.prepareStatement(s);
		int ctr=0;

		for (int i=0; i < prods.size(); i++) {
			ProductVO p = prods.get(i);
			ps.setString(1, p.getProductId());
			ps.setString(2, PRODUCT_CATALOG_ID);
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
				//ps.addBatch();
				ps.executeUpdate();
				ctr++;
				/*
				// Execute the batch
				if ((ctr % BATCH_SIZE) == 0) {
					log.debug("Adding products: " + ctr);
					ps.executeBatch();
				}	
				*/		
				} catch (Exception e) {
				log.error("Unable to add product: " + p.getProductId() + ", " + e);
			}
		}
		try {
			// Add the final products
			//ps.executeBatch();
			conn.commit();
			log.debug("Adding products: " + ctr);
			ps.close();
		} catch (Exception e) {
			log.error("Error adding products, ", e);
		}
	}
	
	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	public List<ProductVO> retrieveProducts() throws IOException {
		BufferedReader data = new BufferedReader(new FileReader(prodFileLoc));
		List<ProductVO> prods = new ArrayList<ProductVO>();
		String temp = null;
		for (int i=0; (temp = data.readLine()) != null; i++) {
			String[] fields = temp.split("\t");
			if (i == 0) {
				this.parseHeaderRow(fields);
				log.debug("header row: " + temp);
				continue; // skip the header row
			}
			//log.debug("processing row " + i);
			//log.debug("row/fields: " + i + "/" + fields.length);
			try {
				ProductVO prod = new ProductVO();
				prod.setProductId(PRODUCT_AND_CATEGORY_PREFIX + fields[headerMap.get("SKUID")]); // SKUID with prefix
				prod.setProductName(this.stripQuotes(fields[headerMap.get("NAME")])); // NAME
				prod.setMsrpCostNo(Convert.formatDouble(fields[headerMap.get("PRICE")])); //PRICE
				prod.setDescText(this.stripQuotes(fields[headerMap.get("DESCRIPTION")])); // DESCRIPTION
				prod.setCustProductNo(fields[headerMap.get("CUSTOM")]); // CUSTOM
				prod.setMetaKywds(this.stripQuotes(fields[headerMap.get("KEYWORDS")])); // KEYWORDS
				if (PRODUCT_CATALOG_ID.equals("SUPPORT_PLUS_CATALOG")) {
					if (headerMap.get("IMGA") != null) {
						prod.setImage(fields[headerMap.get("IMGA")]); // IMGA	
					} else {
						prod.setImage(fields[headerMap.get("IMAGE")]); // IMAGE
					}
				} else {
					prod.setImage(fields[headerMap.get("IMAGE")]); // IMAGE
				}
				prod.setThumbnail(fields[headerMap.get("SMLIMG")]); // SMLIMG
				prod.setUrlAlias(fields[headerMap.get("SKUID")]); // SKUID
				prods.add(prod);
				
				// Add the grouping data to a map.  If the product id ends with a "G"
				// it is a group.  Add the product id for the group and the children ids to
				// a map which will be run later to update the parent entries for the 
				// Product group.
				if (prod.getProductId().endsWith("G")) {
					int fieldIndex = -1;
					if (headerMap.get("ITEMGROUP") != null) {
						fieldIndex = headerMap.get("ITEMGROUP");
					} else if (headerMap.get("RELATED") != null) {
						fieldIndex = headerMap.get("RELATED");
					}
					if (fieldIndex > -1) {
						log.debug("found group: " + prod.getProductId());
						String[] children = fields[fieldIndex].split(";"); // ITEMGROUP	
						productParent.put(prod.getProductId(), children);
					}
				}
				
				// Add the categories for the products
				//log.debug("adding to prodCats...key/val: " + prod.getProductId() + "/" + fields[18]);
				prodCats.put(prod.getProductId(), fields[headerMap.get("CATEGORY")]); // CATEGORY
			} catch (Exception e) {
				log.error("Data error processing product record #: " + i + ", " + e);
			}
		}
		
		data.close();
		return prods;
	}
	
	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	public Map<String, ProductCategoryVO> retrieveCategories() throws IOException {
		BufferedReader data = new BufferedReader(new FileReader(catFileLoc));
		Map<String, ProductCategoryVO> cats = new LinkedHashMap<String, ProductCategoryVO>();
		String temp;
		
		for (int i=0; (temp = data.readLine()) != null; i++) {
			String[] fields = temp.split("\t");
			if (i == 0) {
				this.parseHeaderRow(fields);
				log.debug("header row: " + temp);
				continue; // skip the header row
			}
			
			String url = (fields[headerMap.get("CATEGORY_NAME")]).replace(" ", "_");
			url = url.replace("-", "_");
			url = StringUtil.formatFileName(url);
			url = url.replace("_", "-");
			url = url.replace("--", "-");
			
			String catCode = PRODUCT_AND_CATEGORY_PREFIX + fields[headerMap.get("CATEGORY_CODE")];
			ProductCategoryVO vo = new ProductCategoryVO();
			vo.setCategoryCode(catCode);
			vo.setParentCode(PRODUCT_AND_CATEGORY_PREFIX + fields[headerMap.get("CATEGORY_PARENT")]);
			vo.setCategoryUrl(url);
			vo.setCategoryName(fields[headerMap.get("CATEGORY_NAME")]);
			cats.put(catCode, vo);
			//log.debug("added category to map with catcode(key) and name: " + catCode + "<:>" + fields[3]);
		}

		data.close();
		return cats;
	}
	
	/**
	 * Assigns the range of prices to the product groups
	 * @throws SQLException
	 */
	public void assignPriceRange() throws SQLException {
		StringBuilder s = new StringBuilder();
		s.append("UPDATE PRODUCT SET PRICE_RANGE_LOW_NO = p2.LOW, PRICE_RANGE_HIGH_NO = p2.HIGH ");
		s.append("from product p inner join ( ");
		s.append("select PARENT_ID, max(msrp_cost_no) AS HIGH, MIN(msrp_cost_no) AS LOW "); 
		s.append("from product  ");
		s.append("where product_catalog_id='").append(PRODUCT_CATALOG_ID);
		s.append("' and PARENT_ID is not null ");
		s.append("group by PARENT_ID ");
		s.append(") as p2 on p.PRODUCT_ID = p2.PARENT_ID ");
		
		PreparedStatement ps = conn.prepareStatement(s.toString());
		ps.executeUpdate();
	}
	
	/**
	 * 
	 * @param catId
	 * @param prodId
	 * @throws SQLException
	 * @throws IOException 
	 */
	public void addProductAttribute(List<ProductVO> products) throws SQLException, IOException  {
		StringBuffer s = new StringBuffer();
		s.append("insert into product_attribute_xr (product_attribute_id, attribute_id, ");
		s.append("product_id, model_year_no, value_txt, create_dt, currency_type_id, ");
		s.append("msrp_cost_no, attrib1_txt, attrib2_txt, order_no) values (?,?,?,?,?,?,?,?,?,?,?)");
		
		BufferedReader data = null;
		try {
			data = new BufferedReader(new FileReader(optsFileLoc));	
		} catch (FileNotFoundException fnfe) {
			log.error("File not found! - " + optsFileLoc);
			return;
		}

		int ctr = 0;
		PreparedStatement ps = conn.prepareStatement(s.toString());
		String temp = null;
		String prevSrcProdId = "";
		String attribSelectLvl = null; //corresponds to a List index value
		int attribSelectOrder = 0;
		for (int i=0; (temp = data.readLine()) != null; i++) {
			attribSelectLvl = "0";
			String[] fields = temp.split("\t");
			if (i == 0) {
				this.parseHeaderRow(fields);
				continue; // skip to next row
			}
			String srcProdId = fields[headerMap.get("SKU")];
			String prodId = null;
			if (srcProdId.indexOf("_") > -1) {
				attribSelectLvl = srcProdId.substring(srcProdId.length() - 1);
				prodId = srcProdId.substring(0, (srcProdId.length() - 2));
			} else {
				prodId = srcProdId;
			}
			// add prefix to prodId
			prodId = PRODUCT_AND_CATEGORY_PREFIX + prodId;
			if (srcProdId.equals(prevSrcProdId)) {
				attribSelectOrder++; // increment select order
			} else {
				attribSelectOrder = 0; // reset select order
			}
			// retrieve attribId
			String attribId = null;
			try { // enclosed in try/catch in case of index array out of bounds due to missing field val.
				attribId = fields[headerMap.get("TABLETYPE")];
			} catch (Exception e) {
				log.error("Error getting product attribute ID value, source field is blank for product ID: " + prodId);
			}
			attribId = this.formatAttribute(attribId);
			if (attribId == null) continue;
			try {
				ps.setString(1, new UUIDGenerator().getUUID());	//prodattr_id
				ps.setString(2, attribId); //attrib_id
				ps.setString(3, prodId);	//prod_id
				ps.setString(4, CATALOG_MODEL_YEAR_ID);	//model_year_no
				ps.setString(5, fields[headerMap.get("CODE")]);	//code
				ps.setTimestamp(6, Convert.getCurrentTimestamp());	//crt_dt
				ps.setString(7, "dollars");	//curr_type
				ps.setInt(8, Convert.formatInteger(fields[headerMap.get("PRICEMOD")]));		//pricemod
				ps.setString(9, fields[headerMap.get("DESCRIPTION")]);	//attrib1
				ps.setString(10, attribSelectLvl); //attrib2
				ps.setInt(11, attribSelectOrder); //order_no
				ps.executeUpdate();
				ctr++;
				/*
				if (conn.isClosed())
					try {
						conn = this.getConnection();
					} catch (Exception e) {}
				ps.addBatch();
				
				// Execute the batch
				if ((ctr % BATCH_SIZE) == 0) {
					log.debug("Adding attributes: " + ctr);
					ps.executeBatch();
				}
				*/
			}catch (Exception e) {
				String cause = null;
				if (e.getMessage().contains("column 'PRODUCT_ID'")) {
					//cause = "product ID foreign key not found";
					misMatchProdOptions.add(prodId);
				} else if (e.getMessage().contains("column 'ATTRIBUTE_ID'")) {
					//cause = "attribute ID foreign key not found";
					log.error("mismatched attribute ID at line: " + (i + 1) + " in input file:prodId/attribId: " + prodId + "/" + attribId);
					misMatchAttrib.add(attribId);
				} else {
					cause = e.getMessage();
					log.error("Error adding attribute XR: prodId/attribId: " + prodId + "/" + attribId + ", " + cause);
				}
			}
			prevSrcProdId = srcProdId;
		}
		
		try {
			data.close();
			// Add the final attributes
			//ps.executeBatch();
			conn.commit();
			log.debug("Adding attributes: " + ctr);
			ps.close();
		} catch (Exception e) {
			log.error("Error adding attributes...", e);
		}
		
	}
	
	public void addCustomProductAttribute(List<ProductVO> products) throws SQLException, IOException  {
		String s = "insert into product_attribute_xr (product_attribute_id, attribute_id, ";
		s += "product_id, model_year_no, value_txt, create_dt, currency_type_id, msrp_cost_no, attrib1_txt, attrib3_txt, attrib2_txt) ";
		s += "values (?,?,?,?,?,?,?,?,?,?,?)";
		BufferedReader data = null;
		try {
			data = new BufferedReader(new FileReader(persFileLoc));
		} catch (FileNotFoundException fnfe) {
			log.error("File not found! - " + persFileLoc);
			return;
		}

		int ctr = 0;
		PreparedStatement ps = conn.prepareStatement(s);
		String temp = null;
		for (int i=0; (temp = data.readLine()) != null; i++) {
			String[] fields = temp.split("\t");
			if (i == 0) {
				this.parseHeaderRow(fields);
				log.debug("header row: " + temp);
				continue; // skip to next row
			}
			String prodId = PRODUCT_AND_CATEGORY_PREFIX + fields[headerMap.get("CUSTOM")];
			try {
				ps.setString(1, new UUIDGenerator().getUUID());		//prodattr_id
				ps.setString(2, "USA_CUSTOM");	//attrib_id
				ps.setString(3, prodId);	//prod_id
				ps.setString(4, CATALOG_MODEL_YEAR_ID);	//model_year_no
				ps.setString(5, fields[headerMap.get("DATA")]); 	//data
				ps.setTimestamp(6, Convert.getCurrentTimestamp());	//crt_dt
				ps.setString(7, "DOLLARS");	//curr_type
				ps.setInt(8, 0);		//msrp_cost
				ps.setString(9, fields[headerMap.get("PROMPT")]);							//prompt
				ps.setString(10, fields[headerMap.get("MAXLENGTH")]);						//attrib2
				if(fields.length > 5)
					ps.setString(11, fields[headerMap.get("REQUIRED")]);						//attrib3
				else
					ps.setString(11, "0");						//attrib3
				//log.debug("attr info: " + key + "|" + p.getProductId() + "|" + value + "|");
				ps.executeUpdate();
				ctr++;

			} catch (Exception e) {
				if (e.getMessage().contains("column 'PRODUCT_ID'")) {
					//cause = "product ID foreign key not found";
					misMatchProdCustom.add(prodId);
				} else {
					log.error("Error adding custom product attribute XR for product: " + prodId + ", " + e.getMessage());
				}
			}
		}
		
		try {
			// Add the final attributes
			conn.commit();
			log.debug("Adding custom attributes: " + ctr);
			ps.close();
			
			data.close();
		} catch (Exception e) {
			log.error("Error adding custom attributes, ", e);
		}
	}
	
	/**
	 * returns the first 'live' product catalog for this organization
	 * @param orgId
	 * @return
	 * @throws SQLException
	 */
	@SuppressWarnings("unused")
	private String loadCatalogId(String orgId) throws SQLException  {
		String sql = "select top 1 product_catalog_id from product_catalog where status_no=? and organization_id=?";
		String catalogId = null;
		PreparedStatement ps = conn.prepareStatement(sql);
		ps.setInt(1, ProductCatalogAction.STATUS_LIVE);
		ps.setString(2, orgId);
		ResultSet rs = ps.executeQuery();
		if (rs.next())
			catalogId = rs.getString(1);
		
		ps.close();
		return catalogId;
	}
	
	public String formatAttribute(String attrib) {
		String newAttr = null;
		if (attrib != null && attrib.length() > 0) {
			newAttr = attrib.replace(" ", "");
			newAttr = newAttr.replace("-", "");
			newAttr = StringUtil.formatFileName(newAttr);
			newAttr = "USA_" + newAttr.toUpperCase();	
		}
		return newAttr;
	}
	
	public void logMisMatches() {
		log.debug("");
		if (misMatchParentCat.size() > 0) {
			log.debug("*** Invalid parent category references in category data file: ***");
			for (Iterator<String> iter = misMatchParentCat.iterator(); iter.hasNext(); ) {
				log.debug("---->" + iter.next());
			}
			log.debug("*** end ***");
			log.debug("");
		}
		if (misMatchCat.size() > 0) {
			log.debug("*** Invalid category references in products data file: ***");
			for (Iterator<String> iter = misMatchCat.iterator(); iter.hasNext(); ) {
				log.debug("---->" + iter.next());
			}
			log.debug("*** end ***");
			log.debug("");
		}			
		if (misMatchProdOptions.size() > 0) {
			log.debug("*** Invalid product references in options file: ***");
			for (Iterator<String> iter = misMatchProdOptions.iterator(); iter.hasNext(); ) {
				log.debug("---->" + iter.next());
			}
			log.debug("*** end ***");
			log.debug("");
		}
		if (misMatchProdCustom.size() > 0) {
			log.debug("*** Invalid product ID references in personalization file: ***");
			for (Iterator<String> iter = misMatchProdCustom.iterator(); iter.hasNext(); ) {
				log.debug("---->" + iter.next());
			}
			log.debug("*** end ***");
			log.debug("");
		}
		if (misMatchAttrib.size() > 0) {
			log.debug("*** Invalid attribute references: ***");
			for (Iterator<String> iter = misMatchAttrib.iterator(); iter.hasNext(); ) {
				log.debug("---->" + iter.next());
			}
			log.debug("*** end ***");
			log.debug("");
		}

	}
	
	
	/**
	 * 
	 * @return
	 * @throws DatabaseException
	 * @throws InvalidDataException
	 */
	public Connection getConnection() {
		log.debug("connecting to db: " + config.getProperty("dbUrl"));
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
	 * Recursively builds the pipe-delimited short description (SHORT_DESC) for a product category based on the 
	 * category's parent(s) category url(s).
	 * @param prodCat
	 * @param data
	 * @return
	 */
	private String buildShortDescription(ProductCategoryVO prodCat, Map<String, ProductCategoryVO> data) {
		String cUrl = prodCat.getCategoryUrl();
		if (StringUtil.checkVal(prodCat.getParentCode()).length() > 0 && data.get(prodCat.getParentCode()) != null) {
			if (! data.get(prodCat.getParentCode()).getCategoryCode().equals(TOP_LEVEL_CATEGORY_ID)) {
				String parentPath = this.buildShortDescription((data.get(prodCat.getParentCode())),data);
				cUrl = parentPath + "|" + cUrl;
			}
		}
		return cUrl;
	}
	
	/**
	 * Parses the header row's column names into a map of column name, index.
	 * @param columns
	 */
	private void parseHeaderRow(String[] columns) {
		headerMap = new HashMap<String, Integer>();
		for (int i = 0; i < columns.length; i++) {
			headerMap.put(columns[i].toUpperCase(), new Integer(i));
			//log.debug("putting column name---->" + columns[i].toUpperCase() + "<----");
		}
		/*
		if (headerMap != null) {
			for (String key : headerMap.keySet()) {
				log.debug("headerMap key/val: " + key + "/" + headerMap.get(key));
			}
		}
		*/
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

}
