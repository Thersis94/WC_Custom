package com.universal.catalog;

// JDK 1.6
import java.util.Map;

/****************************************************************************
 * <b>Title</b>: CatalogImport.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Abstract class representing a catalog configuration for importing a catalog.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Sep 13, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public abstract class CatalogImport {
	
	private final String TOP_LEVEL_CATEGORY_ID = "100";
	private final String SKIP_CATEGORY_ID = "zz";
	protected String importSourcePath;
	protected String catalogId;
	protected String productAndCategoryPrefix;
	protected String catalogModelYear;
	protected Map<String, Map<String, Integer>> importMaps;
	
	public String getCatalogId() {
		return catalogId;
	}
	
	public void setCatalogId(String catalogId) {
		this.catalogId = catalogId;
	}

	public String getProductAndCategoryPrefix() {
		return productAndCategoryPrefix;
	}
	
	public void setProductAndCategoryPrefix(String productAndCategoryPrefix) {
		this.productAndCategoryPrefix = productAndCategoryPrefix;
	}

	public String getCatalogModelYear() {
		return catalogModelYear;
	}
	
	public void setCatalogModelYear(String catalogModelYear) {
		this.catalogModelYear = catalogModelYear;
	}

	public Map<String, Map<String, Integer>> getImportMaps() {
		return importMaps;
	}
	
	public String getImportSourcePath() {
		return importSourcePath;
	}
	
	public void setImportSourcePath(String importSourcePath) {
		this.importSourcePath = importSourcePath;
	}

	public String getTopLevelCategoryId() {
		return productAndCategoryPrefix + TOP_LEVEL_CATEGORY_ID;
	}
	
	public String getSkipCategoryId() {
		return productAndCategoryPrefix + SKIP_CATEGORY_ID;
	}

}
