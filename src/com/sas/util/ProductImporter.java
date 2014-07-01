package com.sas.util;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import com.siliconmtn.exception.MailException;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.commerce.product.ProductCatalogAction;

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
	private String fileLoc = null;
	private Properties config = new Properties();
	private static final Logger log = Logger.getLogger(ProductImporter.class);
	private Map<String, ProductCategoryVO> categoryPaths = new HashMap<String, ProductCategoryVO>();
	private Map<String, String> categoryXRef = new HashMap<String, String>();
	private Map<ProductGroupVO, Integer> groups = new HashMap<ProductGroupVO, Integer>();
	private List<ProductVO> products = new ArrayList<ProductVO>();
	private static int catCount;
	private StringBuilder errorLog = new StringBuilder();
	
	public static final String PRODUCT_GROUP_PREFIX = "GROUP_"; 
	private static final int BATCH_SIZE = 1000;
	private String PRODUCT_CATALOG_ID = null;
	
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
		PropertyConfigurator.configure("scripts/sas_importer_log4j.properties");
		
		// Load the config file
		config.load(new FileInputStream(new File("scripts/sas_importer.properties")));
		
		// Load the file location
		fileLoc = config.getProperty("fileLoc");
		
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
			log.info("Starting SAS Product Importer");
			
			// Load the data
			byte[] data = pi.retrieveFileData();
			if (data != null && data.length > 0) {
				BufferedReader inData = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));
				
				//load the default product catalog into a constant				
				pi.loadCatalogId("SAS");

				// remove the current data
				pi.removeExistingEntries();
				
				// Load the categories
				pi.processRequest(inData);

			}
		} catch (Exception e) {
			if (pi != null) 
				pi.errorLog.append("Unable to complete: ").append(e.getMessage()).append("\n<br/>");
			log.error("Error creating product info", e);
		}

				
		// Send the email
		try {
			pi.sendEmail();
		} catch (Exception e) {
			log.error("Unable to send email", e);
		}
		
		long end = System.currentTimeMillis();
		log.info("Completed Product Import in " + ((end - start) / 1000) + " seconds");
	}
	
	/**
	 * 
	 * @throws IOException
	 * @throws SQLException 
	 */
	public void processRequest(BufferedReader in) throws IOException, SQLException {
		// Load File
		this.loadFile(in);
		
		// Manage the groups
		this.manageGroups();
		this.addGroupProduct();
		
		// Update categories
		log.debug("Number of cats/sub-cats: " + categoryPaths.size());
		this.createCategories();

		// Load the categories from the DB
		this.loadCategories();
		
		// Add the products
		log.info("Number of products: " + products.size());
		this.addProduct(products);
		
		// add the product categories
		this.addProductCategory(products);
		this.addGroupCat();
		
		// add the product attributes
		this.addProductAttribute(products);
		this.addGroupAttr();
	}
	
	/**
	 * 
	 * @throws SQLException
	 */
	public void addGroupCat() throws SQLException {
		String s = "insert into product_category_xr (product_category_cd, ";
		s += "product_id, create_dt) values (?,?,?)";
		
		PreparedStatement ps = conn.prepareStatement(s);
		int i=0;
		for (Iterator<ProductGroupVO> iter = groups.keySet().iterator(); iter.hasNext(); i++) {
			ProductGroupVO group = iter.next();
			String catPathId = group.getCategory();
			String catId = categoryXRef.get(catPathId);
			
			ps.setString(1, catId);
			ps.setString(2, PRODUCT_GROUP_PREFIX + group.getId());
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.addBatch();
		}
		
		log.debug("Added Prod Group Cat: " + i);
		ps.executeBatch();
		conn.commit();
		ps.close();
	}
	
	/**
	 * 
	 * @throws SQLException
	 */
	public void addGroupProduct() throws SQLException  {
		String s = "insert into product (product_id, product_catalog_id, parent_id, ";
		s += "cust_product_no, product_nm, desc_txt, status_no, create_dt, image_url, ";
		s += "thumbnail_url, currency_type_id, product_url, full_product_nm, url_alias_txt) ";
		s += "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		PreparedStatement ps = conn.prepareStatement(s);
		
		for (Iterator<ProductGroupVO> iter = groups.keySet().iterator(); iter.hasNext(); ) {
			ProductGroupVO group = iter.next();
			try {
				ps.setString(1, PRODUCT_GROUP_PREFIX + group.getId());
				ps.setString(2, PRODUCT_CATALOG_ID);
				ps.setString(3, null);
				ps.setString(4, null);
				ps.setString(5, group.getName());
				ps.setString(6, group.getDesc());
				ps.setInt(7, 5);
				ps.setTimestamp(8, Convert.getCurrentTimestamp());
				ps.setString(9, group.getImageUrl());
				ps.setString(10, group.getThumbnail());
				ps.setString(11, "dollars");
				ps.setString(12, group.getProductUrl());
				ps.setString(13, group.getPriceRange());
				ps.setString(14, PRODUCT_GROUP_PREFIX + group.getId());
				
				ps.addBatch();
			} catch (Exception e) {
				log.error("Unable to add product group", e);
			}
		}
		
		ps.executeBatch();
		conn.commit();
		ps.close();
	}
	
	/**
	 * 
	 */
	public void manageGroups() {
		log.debug("Before Number of Groups: " + groups.size());
		List<ProductGroupVO> removes = new ArrayList<ProductGroupVO>();
		for (Iterator<ProductGroupVO> iter = groups.keySet().iterator(); iter.hasNext(); ) {
			ProductGroupVO group = iter.next();
			int count = groups.get(group);
			if (count == 1) {
				removes.add(group);
			}
			
		}
		
		for (int i=0; i < removes.size(); i++) groups.remove(removes.get(i));
		
		log.info("After Number of Groups: " + groups.size());
	}
	
	/**
	 * 
	 * @throws SQLException
	 */
	public void addGroupAttr() throws SQLException {
		String s = "insert into product_attribute_xr (product_attribute_id, attribute_id, ";
		s += "product_id, model_year_no, value_txt, create_dt, currency_type_id) ";
		s += "values (?,?,?,?,?,?,?)";
		
		PreparedStatement ps = conn.prepareStatement(s);
		
		for (Iterator<ProductGroupVO> iter = groups.keySet().iterator(); iter.hasNext(); ) {
			try {
			ProductGroupVO group = iter.next();
				ps.setString(1, new UUIDGenerator().getUUID());
				ps.setString(2, "SAS_RATING");
				ps.setString(3, PRODUCT_GROUP_PREFIX + group.getId());
				ps.setString(4, "SAS_2011");
				ps.setString(5, group.getRating());
				ps.setTimestamp(6, Convert.getCurrentTimestamp());
				ps.setString(7, "dollars");
				ps.addBatch();
			} catch (Exception e) {
				log.error("Error adding group attributes", e);
			}
		}
		
		int[] count = ps.executeBatch();
		log.debug("Number of group attr: " + count.length);
		conn.commit();
		ps.close();
	}
	
	/**
	 * Loads the category name and ID so that products can be mapped to the appropriate 
	 * product category
	 * @throws SQLException
	 */
	public void loadCategories() throws SQLException {
		String s = "select * from product_category where product_catalog_id='" + PRODUCT_CATALOG_ID + "'";
		PreparedStatement ps = conn.prepareStatement(s);
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			categoryXRef.put(rs.getString("short_desc"), rs.getString("product_category_cd"));
		}
	}
	
	/**
	 * Rebuilds the categories
	 * @throws SQLException
	 */
	public void createCategories() throws SQLException {
		Map<String, String> level1 = new HashMap<String,String>();
		Map<String, String> level2 = new HashMap<String,String>();
		int ctr = 0;
		for (Iterator<String> iter = categoryPaths.keySet().iterator(); iter.hasNext(); ) {
			String val = iter.next();
			ProductCategoryVO vo = categoryPaths.get(val);
			
			String id2 = null;
			String id3 = null;
			String[] cats = val.split("[|]");
			

			String rootId = cats[0].toLowerCase().replace(" ", "_");
			if (cats.length > 1) id2 = cats[1].toLowerCase().replace(" ", "_");
			if (cats.length > 2) id3 = cats[2].toLowerCase().replace(" ", "_");
			//log.debug(val + " - " + rootId + "^" + id2 + "^" + id3);
			
			if (! level1.containsKey(rootId)) {
				String id = new UUIDGenerator().getUUID();
				level1.put(rootId, id);
				//log.debug("Root Cat: " + rootId + " % " + val);
				String newVal = val;
				if (val.indexOf("|") > -1) newVal = val.substring(0,val.indexOf("|"));
				this.addCategory(id, null, rootId, cats[0], newVal.replace(" ", "_").toLowerCase(), vo);
				ctr++;
			}
			
			if (id2 != null && ! level2.containsKey(rootId + "_" + id2)) {
				String uId2 = new UUIDGenerator().getUUID();
				level2.put(rootId + "_" + id2, uId2);
				String newVal = val;
				if (cats.length > 2 && val.lastIndexOf("|") > -1) 
					newVal = val.substring(0,val.lastIndexOf("|"));
				
				//log.debug("\tLevel 2 Cat: " + rootId + "_" + id2 + " % " + newVal);
				this.addCategory(uId2, level1.get(rootId), id2, cats[1], newVal.replace(" ", "_").toLowerCase(), vo);
				ctr++;
			}
			
			if (cats.length == 3) {
				String uId3 = new UUIDGenerator().getUUID();
				//log.debug("\t\tLevel 3 Cat: " + rootId + "_" + id2 + "_" + id3 + " % " + val);
				this.addCategory(uId3, level2.get(rootId + "_" + id2), id3, cats[2], val.replace(" ", "_").toLowerCase(), vo);
				
				ctr++;
			}
		}
		
		catCount = ctr;
		log.info("Number of category entries: " + catCount);
	}
	
	/**
	 * 
	 * @param id
	 * @param par
	 * @throws SQLException
	 */
	public void addCategory(String id, String pnt, String url, String name, String desc, ProductCategoryVO vo) 
	throws SQLException {
		String s = "insert into product_category (product_category_cd, product_catalog_id, ";
		s += "parent_cd, category_nm, active_flg, short_desc, create_dt, category_url, ";
		s += "title_nm, meta_desc, meta_kywd_txt, cust_category_id, url_alias_txt) ";
		s += "values(?,?,?,?,?,?,?,?,?,?,?,?,?)";
		
		// Add the category path in the SAS file to the xref map along with the
		// Category 
		categoryXRef.put(desc, id);
		//log.info("inserting: " + id + "|" + pnt + "|" + name);
		
		PreparedStatement ps = conn.prepareStatement(s);
		try {
			ps.setString(1, id);
			ps.setString(2, PRODUCT_CATALOG_ID);
			ps.setString(3, pnt);
			ps.setString(4, name);
			ps.setInt(5, 1);
			ps.setString(6, desc);
			ps.setTimestamp(7, Convert.getCurrentTimestamp());
			ps.setString(8, url);
			ps.setString(9, vo.getTitle());
			ps.setString(10, vo.getMetaDesc());
			ps.setString(11, vo.getMetaKeyword());
			ps.setString(12, vo.getCustCategoryId());
			ps.setString(13, id);
			
			ps.executeUpdate();
		} catch (Exception e) {
			log.debug("title: " + vo.getTitle().length());
			log.debug("desc: " + vo.getMetaDesc().length());
			log.debug("kyd: " + vo.getMetaKeyword().length());
		}
		ps.close();
	}
	
	/**
	 * 
	 * @param p
	 * @throws SQLException
	 */
	public void addProduct(List<ProductVO> products) {
		// Add the product entry
		String s = "insert into product (product_id, product_catalog_id, parent_id, ";
		s += "cust_product_no, product_nm, desc_txt, status_no, msrp_cost_no, ";
		s += "create_dt, image_url, thumbnail_url, short_desc, product_url, ";
		s += "currency_type_id, title_nm, meta_desc, meta_kywd_txt, url_alias_txt) ";
		s += "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		
		/*
		log.debug("Short Desc: " + StringUtil.checkVal(p.getShortDesc()).length());
		log.debug("Desc: " + StringUtil.checkVal(p.getDescText()).length());
		log.debug("URL: " + StringUtil.checkVal(p.getProductUrl()).length());
		log.debug("Name: " + StringUtil.checkVal(p.getProductName()).length());
		log.debug("*************");
		*/
		
		PreparedStatement ps = null;
		
		for (int i=0; i < products.size(); i++) {
			ProductVO p = products.get(i);
			String parentId = null;
			ProductGroupVO group = new ProductGroupVO(p.getParentId());
			if (groups.containsKey(group))
				parentId = PRODUCT_GROUP_PREFIX + group.getId();
			
			p.setParentId(parentId);
			
			try {
				if (i == 0) ps = conn.prepareStatement(s);
				ps.setString(1, p.getProductId());
				ps.setString(2, PRODUCT_CATALOG_ID);
				ps.setString(3, parentId);
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
				ps.setString(18, p.getProductId());
				
				// Store the data
				ps.addBatch();
				p.isDataTooLong();
				
				// execute the batch on the given interval
				if ((i % BATCH_SIZE) == 0) {
					log.debug("Add batch for products: " + i);
					ps.executeBatch();
				}
			} catch (Exception e) {
				/**
				try {
					log.error("Unable to add product info: ", e);
					log.error("Title: " + StringUtil.checkVal(p.getTitle()).length());
					log.error("Image: " + StringUtil.checkVal(p.getImage()).length());
					log.error("Thumbnail: " + StringUtil.checkVal(p.getThumbnail()).length());
					log.error("Cust PO: " + StringUtil.checkVal(p.getCustProductNo()).length());
					log.error("Product Name: " + StringUtil.checkVal(p.getProductName()).length());
					log.error("Product Id: " + StringUtil.checkVal(p.getProductId()).length());
					log.error("Desc Txt: " + StringUtil.checkVal(p.getDescText()).length());
					log.error("Meta Desc: " + StringUtil.checkVal(p.getMetaDesc()).length());
					log.error("Product Url: " + StringUtil.checkVal(p.getProductUrl()).length());
					log.error("Short Desc: " + StringUtil.checkVal(p.getShortDesc()).length());

					if (conn == null || conn.isClosed())  conn = getConnection();
				} catch (Exception e1) {
					log.error("Unable to find issue", e1);
				}
				*/
			}
		}
		
		try {
			ps.executeBatch();
			conn.commit();
		 ps.close();	
		} catch (Exception e) {}
	}
	
	/**
	 * 
	 * @param catId
	 * @param prodId
	 * @throws SQLException
	 */
	public void addProductCategory(List<ProductVO> data) throws SQLException {
		//Add the cat
		PreparedStatement ps = null;
		String s = "insert into product_category_xr (product_category_cd, ";
		s += "product_id, create_dt) values (?,?,?)";
		ProductVO p = new ProductVO();
		ps = conn.prepareStatement(s);
		
		for (int i=0; i < products.size(); i++) {
			try {
				p = products.get(i);
				if (StringUtil.checkVal(p.getParentId()).length() > 0) continue;
				
				String catPathId = p.getModelYearNm();
				String catId = categoryXRef.get(catPathId);
				String prodId = p.getProductId();

				ps.setString(1, catId);
				ps.setString(2, prodId);
				ps.setTimestamp(3, Convert.getCurrentTimestamp());
				ps.addBatch();
			
				if ((i % BATCH_SIZE) == 0) {
					log.debug("adding product categories " + i);
					ps.executeBatch();
				}
			} catch (Exception e) {
				log.error("Unable to add prod cat: " + p.getModelYearNm() + "|" + p.getProductId() + "|" + e.getMessage());
				if (conn.isClosed()) conn = this.getConnection();
			}
		}
		// Execute the last items in the batch
		try {
			ps.executeBatch();
			conn.commit();
			ps.close();
			
		} catch (Exception e) {
			log.error("Error adding prd cats", e);
		}

	}
	
	/**
	 * 
	 * @param catId
	 * @param prodId
	 * @throws SQLException
	 */
	public void addProductAttribute(List<ProductVO> products) throws SQLException  {
		String s = "insert into product_attribute_xr (product_attribute_id, attribute_id, ";
		s += "product_id, model_year_no, value_txt, create_dt, currency_type_id) ";
		s += "values (?,?,?,?,?,?,?)";

		int ctr = 0;
		ProductVO p = null;
		PreparedStatement ps = conn.prepareStatement(s);
		
		for (int i = 0; i < products.size(); i++) {
			p = products.get(i);
			try {
				Set<String> keys = p.getProdAttributes().keySet();
				for (Iterator<String> iter = keys.iterator(); iter.hasNext(); ) {
					String key = iter.next();
					ctr++;
					String value = (String) p.getProdAttributes().get(key);
					
					ps.setString(1, new UUIDGenerator().getUUID());
					ps.setString(2, key);
					ps.setString(3, p.getProductId());
					ps.setString(4, "SAS_2011");
					ps.setString(5, value);
					ps.setTimestamp(6, Convert.getCurrentTimestamp());
					ps.setString(7, "dollars");
					
					//log.debug("attr info: " + key + "|" + p.getProductId() + "|" + value + "|");
					
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
				}
			} catch (Exception e) {
				log.error("unable to add attribute for product: " + p.getProductId());
			}
		}
		
		try {
			// Add the final attributes
			ps.executeBatch();
			conn.commit();
			ps.close();
		} catch (Exception e) {}
	}
	
	/**
	 * Retrieves the file data from the file system or a web server for download and 
	 * processing
	 * @return
	 * @throws IOException
	 */
	public byte[] retrieveFileData() throws IOException {
		byte[] b  = null;
		if (fileLoc.startsWith("http")) {
			SMTHttpConnectionManager hConn = new SMTHttpConnectionManager();
			b = hConn.retrieveData(fileLoc);

		} else {
			BufferedReader data = new BufferedReader(new FileReader(fileLoc));
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
	 * 
	 * @param id
	 * @return
	 */
	public ProductGroupVO getGroup(String id) {
		Set<ProductGroupVO> myGroups = groups.keySet();
		for (Iterator<ProductGroupVO> iter = myGroups.iterator(); iter.hasNext();) {
			ProductGroupVO g = iter.next();
			if (id.equals(g.getId())) return g;
		}
		
		return null;
	}
	
	/**
	 * 
	 * @throws IOException
	 */
	public void loadFile(BufferedReader in) throws IOException {
		String temp = "";
		
		for (int i=0; (temp = in.readLine()) != null; i++) {
			// Skip the header row
			if (i == 0) continue;
			//log.debug("temp: " + temp);
			String[] row = temp.split("\t");
			
			// Build the product VO and add to the collection
			ProductVO p = new ProductVO();
			ProductGroupVO group = new ProductGroupVO();
			
			// Store the group ID in the parent ID field and add the group to the
			// Collection.  This is used to create a unique list of group items
			group.setId(row[0]);
			group.setName(row[1]);
			group.setCategory(StringUtil.checkVal(row[4]).replace(" ", "_").toLowerCase());
			group.setImageUrl(row[6]);
			group.setThumbnail(row[7]);
			group.setProductUrl(row[11].substring(31));
			double price = Convert.formatDouble(row[8]);
			if (row.length > 16) group.setRating(Convert.formatInteger(row[16]) + "");
			
			if (groups.containsKey(group)) {
				ProductGroupVO oGroup = this.getGroup(group.getId());
				
				if (price > oGroup.getPriceRangeHigh()) oGroup.setPriceRangeHigh(price);
				else if (price < oGroup.getPriceRangeLow()) oGroup.setPriceRangeLow(price);
				groups.put(oGroup, groups.get(group) + 1);
			} else {
				group.setPriceRangeHigh(price);
				group.setPriceRangeLow(price);
				groups.put(group, 1);
			}
			
			// Store the category ID in the title field
			p.setModelYearNm(StringUtil.checkVal(row[4]).replace(" ", "_").toLowerCase());
			
			// Standard fields
			p.setParentId(row[0]);
			p.setFullProductName(row[1]);
			p.setCustProductNo(row[2]);
			p.setProductId(row[3]);
			p.setProductName(row[5]);
			p.setImage(row[6]);
			p.setThumbnail(row[7]);
			p.setMsrpCostNo(Convert.formatDouble(row[8]));
			p.setTitle(row[21]);
			p.setMetaKywds(row[22]);
			p.setMetaDesc(row[23]);
			
			// add the size,color, stock, shipping and rating attribute
			p.addProdAttribute("SAS_SIZE", row[9]);
			p.addProdAttribute("SAS_COLOR", row[10]);

			if (row.length > 14) p.addProdAttribute("SAS_STOCK", row[14]);
			if (row.length > 15) p.addProdAttribute("SAS_FREESHIP", row[15]);
			if (row.length > 16) p.addProdAttribute("SAS_RATING", Convert.formatInteger(row[16]) + "");
			if (row.length > 17) p.addProdAttribute("SAS_STOCK_DESC", row[17]);
			
			// Standard fields
			p.setProductUrl(row[11].substring(31));
			p.setDescText(row[12]);
			if (row.length > 13) p.setShortDesc(row[13]);
			
			// Create the category info
			String val = StringUtil.checkVal(row[4]).replace("\"", "");
			ProductCategoryVO pCat = new ProductCategoryVO();
			pCat.setTitle(row[18]);
			pCat.setMetaDesc(row[19]);
			pCat.setMetaKeyword(row[20]);
			pCat.setCustCategoryId(row[24]); // Change column number with new data feed
			categoryPaths.put(val, pCat);
			
			// Add the data to the collection
			//log.debug("Product Info: " + p.getProductName() + "|" + p.getMsrpCostNo() + "|" + p.getMetaDesc() + "|" + row[4]);
			products.add(p);
		}
	}
	
	/**
	 * Removes the catalog info from the DB
	 * @throws SQLException
	 */
	public void removeExistingEntries() throws SQLException {
		String delProd = "delete from product where product_catalog_id='" + PRODUCT_CATALOG_ID + "'";
		String delCat = "delete from product_category where product_catalog_id='" + PRODUCT_CATALOG_ID + "'";
		
		log.debug("Starting product delete");
		//Delete the products
		PreparedStatement ps = conn.prepareStatement(delProd);
		ps.executeUpdate();
		ps.close();
		
		log.debug("Starting category delete");
		ps = conn.prepareStatement(delCat);
		ps.executeUpdate();
		ps.close();
		log.debug("Deletions Complete");
	}
	
	/**
	 * 
	 * @throws SQLException
	 * @throws MailException 
	 */
	public void sendEmail() throws SQLException, MailException {
		int numProducts = this.getNumCats(0);
		int numCats = this.getNumCats(1);
		int numAttr = this.getNumCats(2);
		
		// Add an error entry of the numbers don't match
		int pSize = products.size() + groups.size();
		if (pSize != numProducts) errorLog.append("Product counts do not match\n<br/>");
		if (catCount != numCats) errorLog.append("Category counts do not match\n<br/>");
		if (errorLog.length() == 0) errorLog.append("No Errors");
		
		StringBuilder sb = new StringBuilder();
		sb.append("<style>td, th {white-space:nowrap;border:solid black 1px; text-align:center;} ");
		sb.append("th {background:#E1EAFE;padding-left:5px;} th.main {background:silver;}");
		sb.append("td.main {text-align:left;padding-left:5px;} </style>");
		sb.append("<table style=\"border:solid black 1px;border-collapse:collapse;width:400px;\">");
		sb.append("<tr><th colspan='3'>SAS Product Importer</th></tr>");
		sb.append("<tr><th class=\"main\" style=\"text-align:left;\">Type</th>");
		sb.append("<th class=\"main\">Number Sent</th><th class=\"main\">Number Processed</th></tr>");
		
		// Add the products count
		sb.append("<tr><td class=\"main\">Products</td><td>").append(pSize);
		sb.append("</td><td>").append(numProducts).append("</td></tr>");
		
		// Add the category count
		sb.append("<tr><td class=\"main\">Categories</td><td>").append(catCount);
		sb.append("</td><td>").append(numCats).append("</td></tr>");
		sb.append("<tr><td class=\"main\">Errors</td><td colspan='2' style='color:red;'>&nbsp;");
		sb.append(errorLog).append("</td></tr>");
		
		// Add the Attribute count
		sb.append("<tr><td class=\"main\">Attributes</td><td>").append(numAttr);
		sb.append("</td><td>").append(numAttr).append("</td></tr>");
		
		// Load the email info
		String smtpServer= config.getProperty("smtpServer");
		String smtpUser = config.getProperty("smtpUser");
		String smtpPassword = config.getProperty("smtpPassword");
		int smtpPort= Convert.formatInteger(config.getProperty("smtpPort"), 25);
		String smtpRecipient = StringUtil.checkVal(config.getProperty("smtpRecipient"), "james@siliconmtn.com");
		
		// Send the email
		SMTMail email = new SMTMail(smtpServer, smtpPort, smtpUser, smtpPassword);
		email.setFrom("info@siliconmtn.com");
		email.addRecipient(smtpRecipient);
		email.setReplyTo(smtpRecipient);
		email.setSubject("SAS Nightly Product Update");
		email.setHtmlBody(sb.toString());
		email.postMail();
		
		log.info("Email Successfully Sent");
	}
	
	
	public int getNumCats(int type) throws SQLException  {
		String sql = "select count(*) from product where product_catalog_id='" + PRODUCT_CATALOG_ID + "' AND PRODUCT_GROUP_ID IS NULL ";
		if (type == 1) sql = "select count(*) from product_category where product_catalog_id='" + PRODUCT_CATALOG_ID + "'";
		if (type == 2) sql = "select count(*) from product a inner join product_attribute_xr b on a.product_id = b.product_id where product_catalog_id='" + PRODUCT_CATALOG_ID + "' AND a.PRODUCT_GROUP_ID IS NULL ";
		int count = 0;
		PreparedStatement ps = conn.prepareStatement(sql);
		ResultSet rs = ps.executeQuery();
		if (rs.next()) {
			count = rs.getInt(1);
		}
		
		ps.close();
		return count;
	}
	
	
	/**
	 * returns the first 'live' product catalog for this organization
	 * @param orgId
	 * @throws SQLException
	 */
	private void loadCatalogId(String orgId) throws SQLException  {
		String sql = "select top 1 product_catalog_id from product_catalog where status_no=? and organization_id=?";
		String catalogId = null;
		PreparedStatement ps = conn.prepareStatement(sql);
		ps.setInt(1, ProductCatalogAction.STATUS_LIVE);
		ps.setString(2, orgId);
		ResultSet rs = ps.executeQuery();
		if (rs.next())
			catalogId = rs.getString(1);
		
		ps.close();
		PRODUCT_CATALOG_ID = catalogId;
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
