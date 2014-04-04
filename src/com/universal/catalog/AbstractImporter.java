package com.universal.catalog;

//JDK 7
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/****************************************************************************
 * <b>Title</b>: AbstractImporter.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Abstract importer class providing common utility methods.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Apr 03, 2014<p/>
 * <b>Changes: </b>
 * Apr 03, 2014: DBargerhuff: created class.
 ****************************************************************************/
public abstract class AbstractImporter {
	
	protected CatalogImportVO catalog;
	protected Connection dbConn;
	
	/**
	 * Parses the header row's column names into a map of column name, index.
	 * @param columns
	 */
	protected Map<String, Integer> parseHeaderRow(String[] columns) {
		Map<String, Integer> headers = new HashMap<>();
		for (int i = 0; i < columns.length; i++) {
			headers.put(columns[i].toUpperCase(), new Integer(i));
		}
		return headers;
	}

	/**
	 * @return the catalog
	 */
	public CatalogImportVO getCatalog() {
		return catalog;
	}

	/**
	 * @param catalog the catalog to set
	 */
	public void setCatalog(CatalogImportVO catalog) {
		this.catalog = catalog;
	}

	/**
	 * @return the dbConn
	 */
	public Connection getDbConn() {
		return dbConn;
	}

	/**
	 * @param dbConn the dbConn to set
	 */
	public void setDbConn(Connection dbConn) {
		this.dbConn = dbConn;
	}
	
}
