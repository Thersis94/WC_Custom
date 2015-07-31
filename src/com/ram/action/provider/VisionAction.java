/**
 *
 */
package com.ram.action.provider;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import com.ram.action.data.RAMProductSearchVO;
import com.ram.action.util.RAMFabricParser;
import com.ram.datafeed.data.KitLayerProductVO;
import com.ram.datafeed.data.KitLayerVO;
import com.ram.datafeed.data.LayerCoordinateVO;
import com.ram.datafeed.data.RAMProductVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.imageMap.FabricParserInterface;
import com.siliconmtn.util.imageMap.ImageMapVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: VisionAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Action that manages data retrieval and formatting for
 * the RAM Vision System.  We retrieve all Kit Information we'll want to display
 * on the Front end and cache the results to limit database interactions.  In
 * the event any changes are made to the cached data, we are clearing and the
 * data is recreated on the next access.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Jun 22, 2015
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class VisionAction extends SBActionAdapter {

	public static final String CACHE_PREFIX = "VISION_ACTION_";
	private static final String LAYER_ID = "Kit_Layer_";

	/**
	 * 
	 */
	public VisionAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public VisionAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {

		//Create RAMSearchVO to manage the Search/Query Params.
		RAMProductSearchVO svo = new RAMProductSearchVO(req);

		//Don't process if we don't have a productId.
		if(svo.getProductId() == 0) {
			return;
		}

		//Determine if we are processing an Ajax request or the initial page request.
		if(req.hasParameter("amid")) {
			processAjaxRequest(svo);
		} else {
			processStandardRequest(svo);
		}
	}

	/**
	 * Helper method that processes the AJAX Request for Vision System.  This
	 * call returns All the Products associated to the given kitProdId as a list.
	 * 
	 * @param kitProdId
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	private void processAjaxRequest(RAMProductSearchVO svo) throws ActionException {
		List<RAMProductVO> prods = new ArrayList<RAMProductVO>();

		//Get the Cached Data Object.
		ModuleVO mod = getVisionData(svo);

		//Retrieve the Layer data.
		List<KitLayerVO> layers = (List<KitLayerVO>) mod.getActionData();

		//Iterate the Kit Layers and build the proper product list for the call.
		for(KitLayerVO l : layers) {
			for(KitLayerProductVO klp : l.getProducts().values()) {
				if(svo.getLayoutDepthNo() > 0 && l.getDepthNumber() != svo.getLayoutDepthNo()) {
					continue;
				} else {
					prods.add(klp.getProduct());
				}
			}
		}

		//Store the number of products in our compiled list.
		int count = prods.size();

		//SubDivide our product List if paginated.
		if(svo.isPaginated()) {
			prods = prods.subList(svo.getStart(), svo.getLimit() < prods.size() ? svo.getLimit() : prods.size());
		}

		//Put List of Products on Module Data.
		super.putModuleData(prods, count, false);
	}

	/**
	 * Helper method that processes the standard request for Kit Layer Image Map
	 * Data.
	 * @param prodId
	 * @throws ActionException
	 */
	private void processStandardRequest(RAMProductSearchVO svo) throws ActionException {
		//Attempt to read from Cache.
		ModuleVO mod = getVisionData(svo);

		//Place results on Request.
		this.putModuleData(mod.getActionData(), ((List<?>)mod.getActionData()).size(), false);
	}

	/**
	 * Helper method that builds and returns the Vision Systems ModuleVO Data.
	 * Data may either by a RAMProductVO for a standard product or a list of 
	 * Image Map VOs for each layer of a kit. 
	 * @param prodId
	 * @return
	 * @throws ActionException
	 */
	private ModuleVO loadVisionObject(Integer prodId) throws ActionException {

		//Use a new ModuleVO so as to prevent issues with cache.
		ModuleVO mod = new ModuleVO();

		//Perform lookup in database for Product.
		RAMProductVO p = getProduct(prodId);

		/*
		 * If we got a valid product and it's a kit.  Process it. Otherwise throw
		 * Error for invalid lookup.  Probably someone fooling around with data
		 * calls.
		 */
		if(p != null && p.getKitFlag() == 1) {

			//Instantiate the Parser
			FabricParserInterface<LayerCoordinateVO> fp = new RAMFabricParser<LayerCoordinateVO>();

			//Load Kit Layer Json
			List<KitLayerVO> layers = loadKitLayers(p);

			ImageMapVO map = null;

			//Parse each Layer
			for(KitLayerVO k : layers) {
				map = fp.getImageMap(JSONObject.fromObject(k.getJsonData()));
				map.setName(LAYER_ID + k.getDepthNumber());

				k.setImageMap(map);
			}

			//Set Proper ModuleVO Data
			mod.setActionData(layers);
			mod.setDataSize(layers.size());

		} else {
			throw new ActionException("No Product Found for request");
		}

		return mod;
	}
	/**
	 * Helper method intended to load Vision System Data. Attempt a lookup in
	 * cache and if found, return it.  Otherwise generate all the necessary data
	 * and store it in cache before returning.
	 * @param prodId
	 * @return
	 * @throws ActionException
	 */
	private ModuleVO getVisionData(RAMProductSearchVO svo) throws ActionException {

		ModuleVO mod = super.readFromCache(CACHE_PREFIX + svo.getProductId());

		//If not found in cache Load data.
		if(mod == null) {

			//Attempt to Load Data for given Product Id
			mod = loadVisionObject(svo.getProductId());

			//If we could load Kit Data, Update Cache Groups and store it.
			if(mod != null) {
				mod.setCacheable(true);

				//Common Cache Group for all Vision System Items.
				mod.addCacheGroup("VISION_SYSTEM");

				//Generated Id based on Root ProductId.
				mod.setPageModuleId(CACHE_PREFIX + svo.getProductId());

				//Write to Cache.
				super.writeToCache(mod);
			} else {
				log.error("No Product found for productId: " + svo.getProductId());
			}
		}
		return mod;
	}

	/**
	 * Helper method that loads up all the Kit Layers, KitLayer Product Xrs and
	 * Product Records for a given Kit.  We also generate some additional data
	 * for ease of access such as totals and unique KitLayer/XR Ids for Products.
	 * @param p
	 * @return
	 * @throws ActionException
	 */
	private List<KitLayerVO> loadKitLayers(RAMProductVO p) throws ActionException {
		List<KitLayerVO> layers = new ArrayList<KitLayerVO>();

		//Build the Prepared Statement, set the params and Query for Kit Layers.
		try(PreparedStatement ps = dbConn.prepareStatement(getLayerSql())) {
			ps.setInt(1, p.getProductId());

			ResultSet rs = ps.executeQuery();

			KitLayerVO layer = null;
			KitLayerProductVO lpxr = null;
			RAMProductVO prod = null;

			int totalProdNo = 0;

			//Iterate the Result Set and add all the Kit Layers.
			if(rs.next()) {

				//Build Kit Layer VO
				layer = new KitLayerVO(rs, false);
				layer.setJsonData(rs.getString("JSON_DATA"));
				layer.setKitProductId(p.getProductId());

				//Build Kit Layer PRoduct XR VO
				lpxr = new KitLayerProductVO(rs, false);

				//Build ProductVO
				prod = new RAMProductVO(rs);
				prod.setCompositeId(rs.getString("KIT_LAYER_ID") + "-" + rs.getString("PRODUCT_KIT_ID"));
				prod.setQuantity(rs.getInt("QUANTITY"));
				totalProdNo += prod.getQuantity();

				//Add Product to Xr
				lpxr.setProduct(prod);
				layer.addProduct(lpxr);

				while(rs.next()) {

					//If this is a new Layer, Save all Relevant Information.
					if(layer.getKitLayerId() != rs.getInt("KIT_LAYER_ID")) {

						/*
						 * Ensure we have valid JSON Data on this layer before
						 * saving.  We don't want to break front end because
						 * there was a data issue.
						 */
						if(isValidJSON(layer.getJsonData())) {
							layer.setTotalProdNo(totalProdNo);
							layer.setTotalProdXRNo(layer.getProducts().size());
							layers.add(layer);
							totalProdNo = 0;
						}

						//Create new Layer Info and proceed.
						layer = new KitLayerVO(rs, false);
						layer.setJsonData(rs.getString("JSON_DATA"));
						layer.setKitProductId(p.getProductId());
					}

					//Build Kit Layer PRoduct XR VO
					lpxr = new KitLayerProductVO(rs, false);

					//Build ProductVO
					prod = new RAMProductVO(rs);
					prod.setCompositeId(rs.getString("KIT_LAYER_ID") + "-" + rs.getString("PRODUCT_KIT_ID"));
					prod.setQuantity(rs.getInt("QUANTITY"));
					totalProdNo += prod.getQuantity();

					//Add Product to Xr
					lpxr.setProduct(prod);

					//Add Xr to Layer.
					layer.addProduct(lpxr);
				}

				//Add Trailing record to the Layers if valid.
				if(isValidJSON(layer.getJsonData())) {
					layer.setTotalProdNo(totalProdNo);
					layer.setTotalProdXRNo(layer.getProducts().size());
					layers.add(layer);
				}
			}
		} catch (SQLException e) {
			log.error(e);
			throw new ActionException("Problem retrieveing Kit Information", e);
		}

		return layers;
	}

	/**
	 * Helper method that validates if a given string will parse to a proper
	 * JSONObject or JSONArray.
	 * @param json
	 * @return
	 */
	private boolean isValidJSON(String json) {
		try {
			JSONObject.fromObject(json);
		} catch(JSONException e) {
			try {
				JSONArray.fromObject(json);
			} catch(JSONException ex) {
				log.error("Not Valid JSON");
				return false;
			}
		}
		return true;
	}

	/**
	 * Helper method that builds the Kit Layer Retrieval Script.
	 * @return
	 */
	private String getLayerSql() {
		StringBuilder sql = new StringBuilder(800);
		sql.append("select ");
		sql.append("a.KIT_LAYER_ID, a.IMAGE_PATH_URL, a.LAYOUT_DEPTH_NO, a.JSON_DATA, ");
		sql.append("b.PRODUCT_KIT_ID, b.PRODUCT_ID, b.QUANTITY, b.COORDINATE_TYPE_CD, ");
		sql.append("c.CUST_PRODUCT_ID, c.CUSTOMER_ID, c.PRODUCT_NM, c.GTIN_PRODUCT_ID, ");
		sql.append("d.CUSTOMER_NM ");
		sql.append("from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_KIT_LAYER a ");
		sql.append("inner join ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("RAM_PRODUCT_LAYER_XR b ");
		sql.append("on a.KIT_LAYER_ID = b.KIT_LAYER_ID ");
		sql.append("inner join ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("RAM_PRODUCT c ");
		sql.append("on b.PRODUCT_ID = c.PRODUCT_ID ");
		sql.append("inner join ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("RAM_CUSTOMER d ");
		sql.append("on d.CUSTOMER_ID = c.CUSTOMER_ID ");
		sql.append("where a.PRODUCT_ID = ? ");
		sql.append("order by LAYOUT_DEPTH_NO");

		log.debug(sql.toString());
		return sql.toString();
	}

	/**
	 * Attempt to retrieve a product record from that database by ProductId.
	 * If there is any error, return null.
	 * @param prodId
	 * @return
	 */
	public RAMProductVO getProduct(Integer prodId) {
		DBProcessor db = new DBProcessor(dbConn, (String) attributes.get(Constants.CUSTOM_DB_SCHEMA));
		RAMProductVO prod = new RAMProductVO();
		prod.setProductId(prodId);

		try {
			db.getByPrimaryKey(prod);
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e);
		}

		//Validate that the lookup was successful.
		if(StringUtil.checkVal(prod.getProductName()).length() > 0) {
			return prod;
		} else {
			return null;
		}
	}
}
