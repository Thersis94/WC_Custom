package com.depuysynthes.scripts.showpad;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.depuysynthes.scripts.MediaBinDeltaVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.data.Tree;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ShowpadProductDecoratorUnpublished.java<p/>
 * <b>Description: Wrap the ShowpadProductDecorator to include the Unpublished product
 * catalog as well.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 21, 2016
 ****************************************************************************/
public class ShowpadProductDecoratorUnpublished extends ShowpadProductDecorator {

	/*
	 * Contains products with their flattened hierarchy, who use the special 
	 * MEDIABIN Attribute.
	 */
	protected List<ProductVO> unpublishedProducts = new ArrayList<>();


	/**
	 * products in the unpublished catalog that do not have mediabin assets
	 */
	protected Map<String, Set<String>> unpubProductSOUSNames = new HashMap<>();

	/*
	 * The product catalog this script is hard-coded around
	 */
	protected static final String PRIV_CATALOG_ID = "DS_PRIVATE_PRODUCTS_EMEA";

	/**
	 * @param args
	 * @throws IOException
	 */
	public ShowpadProductDecoratorUnpublished(String[] args) throws IOException {
		super(args);
	}


	/**
	 * Create an instance of the ShowpadProductDecorator.
	 * Note this wraps ShowpadMediaBinDecorator, which wraps the DSMediaBinImporterV2 script.
	 * All three are run here-in.
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		ShowpadProductDecoratorUnpublished dmb = new ShowpadProductDecoratorUnpublished(args);
		dmb.run(); //run() is part of the superclass, not implemented here.
	}


	/**
	 * load EMEA's entire UNPUBLISHED product catalog, then call to 
	 * superclass to load the public catalog
	 */
	@Override
	protected Map<String, MediaBinDeltaVO> loadManifest() {
		Tree t = loadProductCatalog(PRIV_CATALOG_ID);
		parseProductCatalog(t, unpublishedProducts);
		log.info("loaded " + unpublishedProducts.size() + " mediabin-using unpublished products in catalog " + PRIV_CATALOG_ID);

		return super.loadManifest();
	}


	/**
	 * Intercept the call between parent-method and parent-method, and call it twice;
	 * once for the parent class, and once for us.
	 */
	@Override
	protected void syncTags(Map<String, MediaBinDeltaVO> masterRecords,  List<ProductVO> products) {
		log.debug("starting to sync tags");
		super.syncTags(masterRecords, products);
		log.debug("starting to sync tags from unpublished catalog");
		super.syncTags(masterRecords, unpublishedProducts);
	}


	/**
	 * Iterate the unpublished products. For each, determine if we have Mediabin assets matching
	 * it's SOUS Product Name.  If we don't report them to the Admins.
	 * @param masterRecords
	 */
	@Override
	protected void findEmptyProducts(Map<String, MediaBinDeltaVO> masterRecords) {
		super.findEmptyProducts(masterRecords);
		
		String name;
		String sousName;
		for (ProductVO prod : unpublishedProducts) {
			//check to see if the product is using the new dynamic connector and attaching dynamic assets.
			//those are the only ones they want reported.  ("SOUS attempted & failed")
			if (!"isDynamic".equals(prod.getImage())) continue;

			name = prod.getProductName();
			sousName = prod.getFullProductName();
			checkProdSousAgainstMediabin(masterRecords, sousName, name, unpubProductSOUSNames);
		}
	}


	/**
	 * trims the mediabinSOUSNames list of assets bound to products
	 * @param masterRecords
	 */
	@Override
	protected void removeProductReferences(Map<String, MediaBinDeltaVO> masterRecords, List<ProductVO> products) {
		//run against the public catalog
		super.removeProductReferences(masterRecords, products);
		//run against the unpublished catalog
		super.removeProductReferences(masterRecords, unpublishedProducts);
	}


	/**
	 * Appends a table to the email notification for products containing SOUS product name
	 * that doesn't match any mediabin assets.
	 * @param html
	 */
	@Override
	protected void addProductsWithNoAssetsToEmail(StringBuilder html) {
		super.addProductsWithNoAssetsToEmail(html);

		if (unpubProductSOUSNames.isEmpty()) return;
		
		html.append("<h4>Unpublished Products with no MediaBin Assets (");
		html.append(unpubProductSOUSNames.size()).append(")</h4>");
		html.append("The following products (from the Unpublished Catalog) indicate a ");
		html.append("SOUS Product Name not matching any MediaBin assets:<br/>");
		html.append("<table border='1' width='95%' align='center'><thead><tr>");
		html.append("<th>Product Name(s)</th>");
		html.append("<th>SOUS Product Name</th>");
		html.append("</tr></thead><tbody>");

		for (Entry<String, Set<String>> entry : unpubProductSOUSNames.entrySet()) {
			Set<String> prodNames = entry.getValue();
			String[] array = prodNames.toArray(new String[prodNames.size()]);
			html.append("<tr><td>").append(StringUtil.getToString(array, false, false, ", ")).append("</td>");
			html.append("<td>").append(entry.getKey()).append("</td></tr>");
		}
		html.append("</tbody></table><br/><hr/>");

	}
}