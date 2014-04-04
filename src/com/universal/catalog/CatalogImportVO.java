package com.universal.catalog;

import java.util.HashMap;
import java.util.Map;

/****************************************************************************
 * <b>Title</b>: CatalogImportVO.java <p/>
 * <b>Project</b>: WC_Custom<p/>
 * <b>Description: </b> VO that describes a catalog for import. 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Apr 04, 2014<p/>
 * Changes:
 * Apr 04, 2014: DBargerhuff; created class
 ****************************************************************************/
public class CatalogImportVO {
	
	public static final String CATEGORY_TOP_LEVEL_ID = "topLevelCategory";
	public static final String CATEGORY_SKIP_ID = "skipCategory";
	public static final String CATEGORY_FEATURE_ID = "featureCategory";
	private String sourceFileDelimiter = "\t";
	private String sourceFileUrl;
	private String sourceFileName;
	private String sourceFilePath;
	private String catalogPrefix;
	private String catalogModelYear;
	private String catalogId;
	private Map<String, String> attributes;
	
	/**
	 * 
	 */
	public CatalogImportVO() {
		attributes = new HashMap<>();
	}

	/**
	 * @return the sourceFileDelimiter
	 */
	public String getSourceFileDelimiter() {
		return sourceFileDelimiter;
	}

	/**
	 * @param sourceFileDelimiter the sourceFileDelimiter to set
	 */
	public void setSourceFileDelimiter(String sourceFileDelimiter) {
		this.sourceFileDelimiter = sourceFileDelimiter;
	}

	/**
	 * @return the sourceFileUrl
	 */
	public String getSourceFileUrl() {
		return sourceFileUrl;
	}

	/**
	 * @param sourceFileUrl the sourceFileUrl to set
	 */
	public void setSourceFileUrl(String sourceFileUrl) {
		this.sourceFileUrl = sourceFileUrl;
	}

	/**
	 * @return the sourceFileName
	 */
	public String getSourceFileName() {
		return sourceFileName;
	}

	/**
	 * @param sourceFileName the sourceFileName to set
	 */
	public void setSourceFileName(String sourceFileName) {
		this.sourceFileName = sourceFileName;
	}

	/**
	 * @return the sourceFilePath
	 */
	public String getSourceFilePath() {
		return sourceFilePath;
	}

	/**
	 * @param sourceFilePath the sourceFilePath to set
	 */
	public void setSourceFilePath(String sourceFilePath) {
		this.sourceFilePath = sourceFilePath;
	}

	/**
	 * @return the catalogPrefix
	 */
	public String getCatalogPrefix() {
		return catalogPrefix;
	}

	/**
	 * @param catalogPrefix the catalogPrefix to set
	 */
	public void setCatalogPrefix(String catalogPrefix) {
		this.catalogPrefix = catalogPrefix;
	}

	/**
	 * @return the catalogModelYear
	 */
	public String getCatalogModelYear() {
		return catalogModelYear;
	}

	/**
	 * @param catalogModelYear the catalogModelYear to set
	 */
	public void setCatalogModelYear(String catalogModelYear) {
		this.catalogModelYear = catalogModelYear;
	}

	/**
	 * @return the catalogId
	 */
	public String getCatalogId() {
		return catalogId;
	}

	/**
	 * @param catalogId the catalogId to set
	 */
	public void setCatalogId(String catalogId) {
		this.catalogId = catalogId;
	}

	/**
	 * @return the attributes
	 */
	public Map<String, String> getAttributes() {
		return attributes;
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}
	
	/**
	 * 
	 * @param key
	 * @param value
	 */
	public void addAttribute(String key, String value) {
		if (key != null && key.length() > 0) {
			attributes.put(key, value);
		}
	}
	
}
