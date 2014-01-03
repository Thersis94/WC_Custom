package com.universal.catalog;

/****************************************************************************
 * <b>Title</b>: CatalogImportFactory.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Factory class that returns a concrete subclass of the abstract CatalogImport class.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Sep 13, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class CatalogImportFactory {
	
	public static CatalogImport getCatalogImport(String catalog) {
		if (catalog.equalsIgnoreCase("signals")) {
			return new SignalsCatalogImport();
		} else if (catalog.equalsIgnoreCase("basBleu")) {
			return new BasBleuCatalogImport();
		} else if (catalog.equalsIgnoreCase("supportPlus")) {
			return new SupportPlusCatalogImport();
		} else if (catalog.equalsIgnoreCase("wireless")) {
			return new WirelessCatalogImport();
		} else if (catalog.equalsIgnoreCase("whatOneEarth")) {
			return new WhatOnEarthCatalogImport();
		} else {
			return null;
		}
	}
}
