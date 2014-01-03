package com.universal.catalog;

// JDK 1.6
import java.util.HashMap;
import java.util.Map;

/**
 * @author beaker
 *
 */
public class SupportPlusCatalogImport extends CatalogImport {
	
	public SupportPlusCatalogImport() {
		this.loadMaps();
	}

	private void loadMaps() {
		importMaps = new HashMap<String, Map<String, Integer>>();
		this.loadCategories();
		this.loadProducts();
		this.loadOptions();
		this.loadPersonalization();
	}
	
	private void loadCategories() {
		Map<String, Integer> categories = new HashMap<String, Integer>();
		categories.put("catCode",1);
		categories.put("catParent",2);
		categories.put("catName",3);
		this.importMaps.put("categories", categories);		
	}
	
	private void loadProducts() {
		Map<String, Integer> products = new HashMap<String, Integer>();
		products.put("productId",1);
		products.put("productName",2);
		products.put("msrpCostNo",4);
		products.put("descText",6);
		products.put("custProdNo",17);
		products.put("metaKeywords",32);
		products.put("image",33);
		products.put("thumbNail",36);
		products.put("urlAlias",1);
		products.put("children",21);
		products.put("category",22);
		this.importMaps.put("products", products);		
	}
	
	private void loadOptions() {
		Map<String, Integer> options = new HashMap<String, Integer>();
		options.put("productId",1);
		options.put("valText",2);
		options.put("msrpCost",3);
		options.put("attrib1",5);
		options.put("attribId",9);
		this.importMaps.put("options", options);		
	}
	
	private void loadPersonalization() {
		Map<String, Integer> personalization = new HashMap<String, Integer>();
		personalization.put("productId", 1);
		personalization.put("valText", 2);
		personalization.put("attrib1", 3);
		personalization.put("attrib2", 4);
		personalization.put("attrib3", 5);
		this.importMaps.put("personalization", personalization);		
	}
	
}
