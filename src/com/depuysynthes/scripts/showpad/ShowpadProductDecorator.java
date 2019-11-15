package com.depuysynthes.scripts.showpad;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.depuysynthes.scripts.MediaBinDeltaVO;
import com.depuysynthes.scripts.MediaBinDeltaVO.State;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.data.Tree;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: ShowpadProductDecorator.java<p/>
 * <b>Description: Reads the EMEA Product catalog and performs 3 tasks with the data:
 * 		1) Pushes to Showpad the hierarchy levels for each product, as mediabin asset tags.</b>
 * 		2) Reports products with no mediabin assets that are using the dynamic Product Attribute.
 * 		3) Reports Mediabin SOUS Product Names not used (referenced) by any products. 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Aug 29, 2016
 ****************************************************************************/
public class ShowpadProductDecorator extends ShowpadMediaBinDecorator {

	/*
	 * Contains products with their flattened hierarchy, who use the special 
	 * MEDIABIN Attribute.
	 */
	protected List<ProductVO> products = new ArrayList<>();

	/*
	 * The product catalog this script is hard-coded around
	 */
	public static final String CATALOG_ID = "DS_PRODUCTS_EMEA";

	protected CatalogReconcileUtil prodUtil;

	/**
	 * @param args
	 * @throws IOException
	 */
	public ShowpadProductDecorator(String[] args) throws IOException {
		super(args);
		prodUtil = new CatalogReconcileUtil(dbConn, props);
	}


	/**
	 * Create an instance of the ShowpadProductDecorator.
	 * Note this wraps ShowpadMediaBinDecorator, which wraps the DSMediaBinImporterV2 script.
	 * All three are run here-in.
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		ShowpadProductDecorator dmb = new ShowpadProductDecorator(args);
		dmb.run(); //run() is part of the superclass, not implemented here.
	}


	@Override
	protected Map<String, MediaBinDeltaVO> loadManifest() {
		Tree t = prodUtil.loadProductCatalog(CATALOG_ID);
		prodUtil.parseProductCatalog(t, products);
		log.info("loaded " + products.size() + " mediabin-using products in catalog " + CATALOG_ID);

		return super.loadManifest();
	}


	@Override
	public void saveRecords(Map<String, MediaBinDeltaVO> masterRecords, boolean isInsert) {
		super.saveRecords(masterRecords, isInsert);

		//this method gets called for both inserts & updates (superclass reusability!).
		//Block here for updates so we don't process the records twice.
		//Insert runs after deletes & updates, so wait for the 'inserts' invocation so 
		//all the mediabin records are already in our database.
		if (!isInsert) return;

		//marry product tag to mediabin assets
		syncTags(masterRecords, products);

		//push all tags (deltas) to Showpad
		pushTagsToShowpad(masterRecords);
	}
	

	/**
	 * marry product tag to mediabin assets - kept in it's own method so it can be overwritten by subclass.
	 * @param masterRecords
	 * @param products
	 */
	protected void syncTags(Map<String, MediaBinDeltaVO> masterRecords,  List<ProductVO> products) {
		prodUtil.syncTags(masterRecords, products);
	}
	

	/**
	 * pushes changes to Showpad via the ShowpadDivisionUtil
	 * @param masterRecords
	 * @throws QuotaException 
	 */
	protected void pushTagsToShowpad(Map<String, MediaBinDeltaVO> masterRecords) {
		Calendar cal = Calendar.getInstance();
		//consider a product change within 24hrs something we need to pay attention to.
		//set the config value to reflect the frequency of the script execution.  e.g. if we run once a week threshold should be -7.
		cal.add(Calendar.DATE, Convert.formatInteger(props.getProperty("productDateThresDays"), -1, false));
		Date thresDate = cal.getTime();

		for (ShowpadDivisionUtil util : divisions) {
			ShowpadTagManager tagMgr = util.getTagManager();
			Map<String, String> divisionAssets = util.getDivisionAssets();

			log.info("pushing product tags to Division=" + util.getDivisionNm());

			boolean needsUpdated;
			for (MediaBinDeltaVO mbAsset : masterRecords.values()) {
				//if the asset is new or updated, or the product is updated, we need to push tag changes to Showpad (all: adds/updates/deletes)
				needsUpdated = State.Insert == mbAsset.getRecordState() || State.Update == mbAsset.getRecordState();
				if (!needsUpdated && mbAsset.getProductUpdateDt() != null)
					needsUpdated = thresDate.before(mbAsset.getProductUpdateDt());

				//get the showpad asset id for this mediabin asset id, so the ShowpadTagMgr knows how to talk to Showpad
				mbAsset.setShowpadId(divisionAssets.get(mbAsset.getDpySynMediaBinId()));

				//skip any that have failed to ingest into Showpad or do not need updated
				if (!needsUpdated || ShowpadDivisionUtil.FAILED_PROCESSING.equals(mbAsset.getShowpadId())) {
					log.info("asset does not need updated based on logic: " + mbAsset.getDpySynMediaBinId());
					continue;
				}

				log.info("************************ Starting Asset *******************************");
				log.info("showpadId=" + mbAsset.getShowpadId() + " mbId=" + mbAsset.getDpySynMediaBinId());
				log.info("asset tags (" + mbAsset.getTags().size() + ") " + mbAsset.getTags());

				/**
				 * push the tags to Showpad.  Note this will do 3 things:
				 * 1) load all existing 'product' tags.
				 * 2) purge any we don't want to keep or should be removed.
				 * 3) add any we need to add that aren't already there.
				 **/
				try {
					tagMgr.updateProductTags(mbAsset);
				} catch (InvalidDataException e) {
					failures.add(e);
					log.error("data issue with " + mbAsset + " and Divsion=" + util.getDivisionId(), e);
				}
			}
		}
	}
}