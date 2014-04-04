package com.universal.catalog;

// JDK 7
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
import java.util.Set;

// Log4J 1.2.15
import org.apache.log4j.Logger;

//SMT Base Libs
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ProductsImporter.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Manages products import for a catalog.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Apr 04, 2014<p/>
 * <b>Changes: </b>
 * Apr 04, 2014: DBargerhuff: created class.
 ****************************************************************************/
public class ProductsImporter extends AbstractImporter {

	private static final Logger log = Logger.getLogger(ProductsImporter.class);
	private List<ProductVO> products;
	private Map<String, String> productCategories = null;
	private Map<String, String[]> productParents = null;
	private Set<String> misMatchedCategories = null;
	
	public ProductsImporter() {
		products = new ArrayList<>();
		productCategories = new LinkedHashMap<>();
		productParents = new LinkedHashMap<>();
		misMatchedCategories = new HashSet<>();
	}
	
	/**
	 * Constructor
	 */
	public ProductsImporter(Connection dbConn) {
		this();
		this.dbConn = dbConn;
	}
	
	/**
	 * Manages category imports
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws SQLException
	 */
	public void manageProducts() throws FileNotFoundException, IOException, SQLException {
		// retrieve products from source file
		products = this.retrieveProducts(catalog);
		// add products
		this.insertProducts(catalog, products);
		// update product groups
		this.updateProductGroups(catalog);
		// update products
		this.updateProducts(products);
		// add the product categories
		this.addProductCategoryXR(catalog);
		// Update the price ranges for the product groups
		this.assignPriceRange(catalog);
	}
	
	/**
	 * Retrieves and parses the products source file and inserts product records into
	 * the products table in the db.
	 * @return
	 * @throws IOException
	 */
	private List<ProductVO> retrieveProducts(CatalogImportVO catalog) throws FileNotFoundException, IOException {
		// TODO remove this debug...
		log.info("catalog is " + (catalog != null ? "not null" : "null"));
		BufferedReader data = null;
		String fullPath = catalog.getSourceFilePath() + catalog.getSourceFileName();
		try {
			data = new BufferedReader(new FileReader(fullPath));
		} catch (FileNotFoundException fnfe) {
			String errMsg = "Products source file not found, file path: " + fullPath;
			log.error(errMsg);
			throw new FileNotFoundException(errMsg);
		}
		
		List<ProductVO> prods = new ArrayList<ProductVO>();
		String temp = null;
		Map<String, Integer> headers = null;
		log.info("source file delimiter: " + catalog.getSourceFileDelimiter());
		for (int i=0; (temp = data.readLine()) != null; i++) {
			String[] fields = temp.split(catalog.getSourceFileDelimiter());
			//log.info("fields size: " + fields.length);
			if (i == 0) {
				headers = parseHeaderRow(fields);
				log.info("headers size: " + headers.size());
				//for (String h : headers.keySet()) {
					//log.info("headers | index: " + h + "|" + headers.get(h));
				//}
				continue; // skip the header row
			}

			try {
				ProductVO prod = new ProductVO();
				prod.setProductId(catalog.getCatalogPrefix() + fields[headers.get("SKUID")]); // SKUID with prefix
				prod.setProductName(this.stripQuotes(fields[headers.get("NAME")])); // NAME
				prod.setMsrpCostNo(Convert.formatDouble(fields[headers.get("PRICE")])); //PRICE
				prod.setDescText(this.stripQuotes(fields[headers.get("DESCRIPTION")])); // DESCRIPTION
				// TODO remove this after testing.
				prod.setCustProductNo(fields[headers.get("CUSTOMN")]); // CUSTOM
				//prod.setCustProductNo(fields[headers.get("CUSTOM")]); // CUSTOM
				prod.setMetaKywds(this.stripQuotes(fields[headers.get("KEYWORDS")])); // KEYWORDS
				if (catalog.getCatalogId().equals("SUPPORT_PLUS_CATALOG")) {
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
	private void updateProductGroups(CatalogImportVO catalog) throws SQLException {
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
				prodId = catalog.getCatalogPrefix() + vals[i];
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
	private void addProductCategoryXR(CatalogImportVO catalog) throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("insert into product_category_xr (product_category_cd, ");
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
				String catCode = (catalog.getCatalogPrefix() + cats[i]);
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
	private void insertProducts(CatalogImportVO catalog, List<ProductVO> prods) 
			throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("insert into product (product_id, product_catalog_id, parent_id, ");
		sb.append("cust_product_no, product_nm, desc_txt, status_no, msrp_cost_no, ");
		sb.append("create_dt, image_url, thumbnail_url, short_desc, product_url, ");
		sb.append("currency_type_id, title_nm, meta_desc, meta_kywd_txt, url_alias_txt) ");
		sb.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		
		PreparedStatement ps = dbConn.prepareStatement(sb.toString());
		int ctr=0;

		for (int i=0; i < prods.size(); i++) {
			ProductVO p = prods.get(i);
			ps.setString(1, p.getProductId());
			ps.setString(2, catalog.getCatalogId());
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
	private void assignPriceRange(CatalogImportVO catalog) throws SQLException {
		StringBuilder s = new StringBuilder();
		s.append("update PRODUCT set PRICE_RANGE_LOW_NO = p2.LOW, PRICE_RANGE_HIGH_NO = p2.HIGH ");
		s.append("from product p inner join ( ");
		s.append("select PARENT_ID, max(msrp_cost_no) AS HIGH, MIN(msrp_cost_no) AS LOW "); 
		s.append("from product where product_catalog_id='").append(catalog.getCatalogId()).append("' ");
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
	 * @return the products
	 */
	public List<ProductVO> getProducts() {
		return products;
	}

	/**
	 * @param products the products to set
	 */
	public void setProducts(List<ProductVO> products) {
		this.products = products;
	}

	/**
	 * @return the misMatchedCategories
	 */
	public Set<String> getMisMatchedCategories() {
		return misMatchedCategories;
	}

	/**
	 * @param misMatchedCategories the misMatchedCategories to set
	 */
	public void setMisMatchedCategories(Set<String> misMatchedCategories) {
		this.misMatchedCategories = misMatchedCategories;
	}

}
