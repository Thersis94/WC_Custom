package com.universal.util;

// JDK 7
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

// Log4J 1.2.15
import org.apache.log4j.Logger;

//SMT Base Libs
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: CategoriesImporter.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Parses a product options file, inserts any new options into the options table, 
 * and updated product-options associations.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Apr 03, 2014<p/>
 * <b>Changes: </b>
 * Apr 03, 2014: DBargerhuff: created class.
 ****************************************************************************/
public class CategoriesImporter extends AbstractImporter {

	private static final Logger log = Logger.getLogger(CategoriesImporter.class);
	private Set<String> misMatchedCategories = null;
	private Set<String> misMatchedParentCategories = null;
	private String topLevelCategory;
	private String skipCategory;
	private String featureCategory;
	
	public CategoriesImporter() {
		misMatchedCategories = new HashSet<>();
		misMatchedParentCategories = new HashSet<>();
	}
	
	/**
	 * Constructor
	 */
	public CategoriesImporter(Connection dbConn) {
		this();
		this.dbConn = dbConn;
	}
	
	/**
	 * Manages category imports
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws SQLException
	 */
	public void manageCategories() throws FileNotFoundException, IOException, SQLException {
		topLevelCategory = catalog.getAttributes().get(CatalogImportVO.CATEGORY_TOP_LEVEL_ID);
		skipCategory = catalog.getAttributes().get(CatalogImportVO.CATEGORY_SKIP_ID);
		featureCategory = catalog.getAttributes().get(CatalogImportVO.CATEGORY_FEATURE_ID);

		// retrieve categories from source file.
		Map<String, ProductCategoryVO> cats = retrieveCategories(catalog);
		
		// add categories
		Map<String, String> catParentMap = insertCategories(catalog, cats);
		
		// circle back and update category parent values
		updateCategoryParents(catalog, catParentMap);
		
	}
	
	/**
	 * Parses categories from the categories source file.
	 * @param catalog
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Map<String, ProductCategoryVO> retrieveCategories(CatalogImportVO catalog) 
			throws FileNotFoundException, IOException {
		BufferedReader data = null;
		String fullPath = catalog.getSourceFilePath() + catalog.getSourceFileName();
		try {
			data = new BufferedReader(new FileReader(fullPath));
		} catch (FileNotFoundException fnfe) {
			String errMsg = "Categories source file not found, file path: " + fullPath;
			log.error(errMsg);
			throw new FileNotFoundException(errMsg);
		}
		
		Map<String, ProductCategoryVO> cats = new LinkedHashMap<String, ProductCategoryVO>();
		String temp;
		Map<String, Integer> headers = null;
		for (int i=0; (temp = data.readLine()) != null; i++) {
			String[] fields = temp.split(catalog.getSourceFileDelimiter());
			if (i == 0) {
				headers = parseHeaderRow(fields);
				continue; // skip the header row
			}
			
			String url = (fields[headers.get("CATEGORY_NAME")]).replace(" ", "_");
			url = url.replace("-", "_");
			url = StringUtil.formatFileName(url);
			url = url.replace("_", "-");
			url = url.replace("--", "-");
			
			String catCode = catalog.getCatalogPrefix() + fields[headers.get("CATEGORY_CODE")];
			ProductCategoryVO vo = new ProductCategoryVO();
			vo.setCategoryCode(catCode);
			vo.setParentCode(catalog.getCatalogPrefix() + fields[headers.get("CATEGORY_PARENT")]);
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
	 * @param catalog
	 * @param categoryMap
	 * @return
	 * @throws SQLException
	 */
	public Map<String, String> insertCategories(CatalogImportVO catalog, Map<String, ProductCategoryVO> categoryMap) 
			throws SQLException {
		Map<String, String> categoryParentMap = new LinkedHashMap<String, String>();
		StringBuilder sb = new StringBuilder();
		sb.append("insert into product_category (product_category_cd, product_catalog_id, ");
		sb.append("parent_cd, category_nm, active_flg, create_dt, title_nm, meta_desc, ");
		sb.append("meta_kywd_txt, category_url, short_desc, url_alias_txt) ");
		sb.append("values(?,?,?,?,?,?,?,?,?,?,?,?)");
		
		PreparedStatement ps = dbConn.prepareStatement(sb.toString());
		int ctr=0;

		for (String key : categoryMap.keySet()) {
			ProductCategoryVO vo = categoryMap.get(key);
			if (skipCategory.equalsIgnoreCase(vo.getParentCode())) {
				log.info("Skipping 'zz' category");
				continue;
			}
			
			String sDesc = this.buildShortDescription(vo, categoryMap);						
			ps.setString(1, key);
			ps.setString(2, catalog.getCatalogId());
			ps.setString(3, null);
			ps.setString(4, vo.getCategoryName());
			ps.setInt(5, 1);
			ps.setTimestamp(6, Convert.getCurrentTimestamp());
			ps.setString(7, "");
			ps.setString(8, "");
			ps.setString(9, "");
			ps.setString(10, vo.getCategoryUrl());
			ps.setString(11, sDesc); 
			if (key.indexOf(catalog.getCatalogPrefix()) > -1) {
				// strip prefix from key for use as
				ps.setString(12, key.substring(key.indexOf(catalog.getCatalogPrefix()) + catalog.getCatalogPrefix().length()));
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
	 * @param catalog
	 * @param categoryParentMap
	 * @throws SQLException
	 */
	public void updateCategoryParents(CatalogImportVO catalog, Map<String, String> categoryParentMap) 
			throws SQLException {
		String sql = "update product_category set parent_cd = ? where product_catalog_id = ? and product_category_cd = ?";
		PreparedStatement ps = dbConn.prepareStatement(sql);
		int ctr=0;
		for (String s : categoryParentMap.keySet()) {
			if (topLevelCategory.equals(categoryParentMap.get(s))) {
				ps.setString(1, null);
			} else {
				ps.setString(1, categoryParentMap.get(s));
			}
			ps.setString(2, s);
			ps.setString(3, catalog.getCatalogId());
			
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
	 * Recursively builds the pipe-delimited short description (SHORT_DESC) for a product category based on the 
	 * category's parent(s) category url(s).
	 * @param prodCat
	 * @param data
	 * @return
	 */
	private String buildShortDescription(ProductCategoryVO prodCat, Map<String, ProductCategoryVO> data) {
		String cUrl = prodCat.getCategoryUrl();
		if (StringUtil.checkVal(prodCat.getParentCode()).length() > 0 && data.get(prodCat.getParentCode()) != null) {
			if (! data.get(prodCat.getParentCode()).getCategoryCode().equals(topLevelCategory)) {
				String parentPath = this.buildShortDescription((data.get(prodCat.getParentCode())),data);
				cUrl = parentPath + "|" + cUrl;
			}
		}
		return cUrl;
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

	/**
	 * @return the misMatchedParentCategories
	 */
	public Set<String> getMisMatchedParentCategories() {
		return misMatchedParentCategories;
	}

	/**
	 * @param misMatchedParentCategories the misMatchedParentCategories to set
	 */
	public void setMisMatchedParentCategories(Set<String> misMatchedParentCategories) {
		this.misMatchedParentCategories = misMatchedParentCategories;
	}
	
}
