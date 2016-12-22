package com.depuysynthes.scripts.showpad;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.depuysynthes.scripts.MediaBinDeltaVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.data.Tree;

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
}