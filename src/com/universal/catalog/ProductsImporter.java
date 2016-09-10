package com.universal.catalog;

// JDK 7
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
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
	private final String BAD_CHAR_SEQ_1 = "\u00e2" + "\u20ac" + "\u00a2";
	private static final int DISPLAY_ORDER_MAX_PRODUCT = 100000;
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
		// this.updateProducts(products);
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
		String origProdId = null;
		String origPrice = null;
		ProductVO prod = null;
		log.info("source file delimiter: " + catalog.getSourceFileDelimiter());
		for (int i=0; (temp = data.readLine()) != null; i++) {
			String[] fields = temp.split(catalog.getSourceFileDelimiter());
			if (i == 0) {
				headers = parseHeaderRow(fields);
				log.info("headers size: " + headers.size());
				for (String h : headers.keySet()) {
					log.info("headers|index: " + h + "|" + headers.get(h));
				}
				continue; // skip the header row
			}
			
			origProdId = StringUtil.checkVal(fields[headers.get("SKUID")]);
			if (origProdId.length() == 0) continue;
			//if (origProdId.toUpperCase().endsWith("G")) continue; // skip any product whose product ID ends with "G"
			
			try {
				//ProductVO prod = new ProductVO();
				prod = new ProductVO();
				prod.setProductId(catalog.getCatalogPrefix() + origProdId); // SKUID with prefix
				prod.setCustProductNo(fields[headers.get("SKUID")]); // SKUID, we use this as display value in JSTL
				prod.setProductName(this.stripQuotes(fields[headers.get("NAME")])); // NAME
				
				/*
				 * 2014-04-14: Business rule governing pricing.  If populated, ORIGPRICE represents the original price 
				 * of the item and PRICE represents the sale price of the item.  In this case we put PRICE as the
				 * msrp cost  number and ORIGPRICE as the discounted cost number.   In the JSTL, we check to see if
				 * discounted cost is > 0.  If so, we display both prices to show the difference to the viewer.
				 */
				try {
					origPrice = fields[headers.get("ORIGPRICE")]; // ORIGPRICE
					prod.setDiscountedCostNo(Convert.formatDouble(origPrice));
				} catch(ArrayIndexOutOfBoundsException e) {
					// 2014-04-15: suppressing this error
					//log.error("Error accessing ORIGPRICE: field.length|index: " + fields.length + "|" + headers.get("ORIGPRICE"));
				}
				
				// reset origPrice
				origPrice = null;
				// set price
				prod.setMsrpCostNo(Convert.formatDouble(fields[headers.get("PRICE")])); //PRICE
				prod.setDescText(this.stripQuotes(fields[headers.get("DESCRIPTION")])); // DESCRIPTION
				prod.setMetaKywds(this.stripQuotes(fields[headers.get("KEYWORDS")])); // KEYWORDS
				prod.setImage(fields[headers.get("IMAGE")]); // IMAGE
				prod.setThumbnail(fields[headers.get("SMLIMG")]); // SMLIMG
				prod.setUrlAlias(fields[headers.get("SKUID")]); // SKUID
				
				/* Mantis #9425: Add 'sales rank' field (i.e. SALES) to product import
				 * to caculate the display order of the product within its category.*/
				calculateProductDisplayOrder(fields, headers.get("SALES"), prod);
				
				// add to list
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
						log.info("found group: " + prod.getProductId());
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
		PreparedStatement ps = null;
		int ctr=0;
		String parentId = null;
		String[] vals = null;
		String prodId = null;
		Savepoint sp = null;
		for (Iterator<String> iter = productParents.keySet().iterator(); iter.hasNext();  ) {
			parentId = iter.next();
			vals = productParents.get(parentId);
			for (int i=0; i < vals.length; i++) {
				try {
					prodId = catalog.getCatalogPrefix() + vals[i];
					sp = dbConn.setSavepoint();
					ps = dbConn.prepareStatement(s);
					ps.setString(1, parentId);
					ps.setString(2, prodId);
					ps.executeUpdate();
					ctr++;
					dbConn.releaseSavepoint(sp);
				} catch (Exception e) {
					log.error("Unable to add product parent: " + parentId + "|" + prodId + "\t\t" + e.getMessage());
					dbConn.rollback(sp);
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
		
		PreparedStatement ps = null;
		Savepoint sp = null;
		int ctr=0;

		// Load the categories.  If there are no categories, skip that entry
		List<String> dupeList = null;
		for (String prodId : productCategories.keySet()) {
			String catIds = StringUtil.checkVal(productCategories.get(prodId)).trim();
			if (catIds.length() == 0) continue;
			
			String[] cats = catIds.split(";");
			dupeList = new ArrayList<>(cats.length);
			for (int i=0; i < cats.length; i++) {
				try {
					sp = dbConn.setSavepoint();
					ps = dbConn.prepareStatement(sb.toString());
					// if key is empty, continue.
					if (StringUtil.checkVal(cats[i]).length() == 0) continue;
					// if the category code is a duplicate, skip it.
					if (dupeList.contains(cats[i])) continue;
					
					// process the category xr
					dupeList.add(cats[i]);
					String catCode = (catalog.getCatalogPrefix() + cats[i]);
					ps.setString(1, catCode);
					ps.setString(2, prodId);
					ps.setTimestamp(3, Convert.getCurrentTimestamp());
					ps.executeUpdate();
					ctr++;
					dbConn.releaseSavepoint(sp);
				}catch (Exception e) {
					misMatchedCategories.add(cats[i]);
					dbConn.rollback(sp);
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
	@SuppressWarnings("unused")
	private void updateProducts(List<ProductVO> prods) throws SQLException {
		String s = "update product set cust_product_no = ? where product_id = ?";
		PreparedStatement ps = null;
		Savepoint sp = null;
		int ctr=0;

		for (int i=0; i < prods.size(); i++) {
			ProductVO p = prods.get(i);
			try {
				sp = dbConn.setSavepoint();
				ps = dbConn.prepareStatement(s);
				ps.setString(1, p.getCustProductNo());
				ps.setString(2, p.getProductId());
				ps.executeUpdate();
				ctr++;
				dbConn.releaseSavepoint(sp);
			} catch (Exception e) {
				log.error("Error adding custom product number for product: " + p.getProductId() + ", " + e.getMessage());
				dbConn.rollback(sp);
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
		sb.append("discounted_cost_no, create_dt, image_url, thumbnail_url, short_desc,  ");		
		sb.append("product_url, currency_type_id, title_nm, meta_desc, meta_kywd_txt, ");
		sb.append("url_alias_txt, display_order_no) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		log.info("Product insert SQL: " + sb.toString());
		PreparedStatement ps = null;
		Savepoint sp = null;
		int ctr=0;
		int index = 0;
		for (int i=0; i < prods.size(); i++) {
			ProductVO p = prods.get(i);
			// TODO remove after PostGreSQL testing.
			//writeProductDebug(p, catalog.getCatalogId());
			// END of DEBUG
			index = 1;
			try {
				sp = dbConn.setSavepoint();
				ps = dbConn.prepareStatement(sb.toString());
				ps.setString(index++, p.getProductId());
				ps.setString(index++, catalog.getCatalogId());
				ps.setString(index++, p.getParentId());
				ps.setString(index++, p.getCustProductNo());
				ps.setString(index++, p.getProductName());
				ps.setString(index++, p.getDescText());
				ps.setInt(index++, 5);
				ps.setDouble(index++, p.getMsrpCostNo());
				ps.setDouble(index++, p.getDiscountedCostNo());
				ps.setTimestamp(index++, Convert.getCurrentTimestamp());
				ps.setString(index++, p.getImage());
				ps.setString(index++, p.getThumbnail());
				ps.setString(index++, p.getShortDesc());
				ps.setString(index++, p.getProductUrl());
				ps.setString(index++, "dollars");
				ps.setString(index++, p.getTitle());
				ps.setString(index++, p.getMetaDesc());
				ps.setString(index++, StringUtil.truncate(p.getMetaKywds(), 255));
				ps.setString(index++, p.getUrlAlias());
				ps.setInt(index++, p.getDisplayOrderNo());
				ps.executeUpdate();
				ctr++;
				dbConn.releaseSavepoint(sp);
			} catch (Exception e) {
				log.error("Error inserting product: " + p.getProductId() + ", " + e);
				dbConn.rollback(sp);
			}
		}
		
		if (ps != null) {
			try {
				ps.close();
			} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
		}
		log.info("Products inserted: " + ctr);
	}
	
	@SuppressWarnings("unused")
	private void writeProductDebug(ProductVO p, String catId) {
		StringBuilder prod = new StringBuilder(200);
		prod.append(p.getProductId()).append("|");
		prod.append(catalog.getCatalogId()).append("|");
		prod.append(p.getParentId()).append("|");
		prod.append(p.getCustProductNo()).append("|");
		prod.append(p.getProductName()).append("|");
		prod.append(p.getDescText()).append("|");
		prod.append(5).append("|");
		prod.append(p.getMsrpCostNo()).append("|");
		prod.append(p.getDiscountedCostNo()).append("|");
		prod.append(Convert.getCurrentTimestamp()).append("|");
		prod.append(p.getImage()).append("|");
		prod.append(p.getThumbnail()).append("|");
		prod.append(p.getShortDesc()).append("|");
		prod.append(p.getProductUrl()).append("|");
		prod.append("dollars").append("|");
		prod.append(p.getTitle()).append("|");
		prod.append(p.getMetaDesc()).append("|");
		prod.append(StringUtil.truncate(p.getMetaKywds(), 255)).append("|");
		prod.append(p.getUrlAlias()).append("|");
		prod.append(p.getDisplayOrderNo());
		log.debug(prod.toString());
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
		s.append("from product where product_catalog_id = ? ");
		s.append("and PARENT_ID is not null group by PARENT_ID ");
		s.append(") as p2 on p.PRODUCT_ID = p2.PARENT_ID ");
		
		PreparedStatement ps = null;
		Savepoint sp = null;
		try {
			sp = dbConn.setSavepoint();
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, catalog.getCatalogId());
			ps.executeUpdate();
			dbConn.releaseSavepoint(sp);
		} catch (SQLException sqle) {
			log.error("Error updating price range for product." + sqle.getMessage());
			dbConn.rollback(sp);
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
	 * Calculates the display order number for the given product.  Defaults to 
	 * the maximum display order number value if no value exists on the field map
	 * or if the value found is invalid.
	 * @param fields
	 * @param fieldIndex
	 * @param vo
	 */
	private void calculateProductDisplayOrder(String[] fields, Integer fieldIndex, ProductVO vo) {
		int orderNo = DISPLAY_ORDER_MAX_PRODUCT;
		if (fieldIndex != null) {
			if (fieldIndex < fields.length) {
				// valid index, try to get a value.
				orderNo = Convert.formatInteger(fields[fieldIndex]);
				orderNo = DISPLAY_ORDER_MAX_PRODUCT - orderNo;
			}
		}
		vo.setDisplayOrderNo(orderNo);
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
			if (value.contains(BAD_CHAR_SEQ_1)) {
				value = value.replace(BAD_CHAR_SEQ_1, "&nbsp;&#8226;");
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
