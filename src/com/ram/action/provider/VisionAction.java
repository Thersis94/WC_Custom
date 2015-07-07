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

import com.ram.action.data.RAMSearchVO;
import com.ram.action.util.RAMFabricParser;
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
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: VisionAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Action that manages data retrieval and formatting for
 * the RAM Vision System.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Jun 16, 2015
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class VisionAction extends SBActionAdapter {

	private static final String CACHE_PREFIX = "VISION_ACTION_";
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
		RAMSearchVO svo = new RAMSearchVO(req);

		//Determine if we are processing an Ajax request or the initial page request.
		if(req.hasParameter("amid")) {
			processAjaxRequest(svo);
		} else {
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			processStandardRequest(page, svo);
		}
	}

	/**
	 * Helper method that processes the AJAX Request for Vision System.  This
	 * call returns All the Products associated to the given kitProdId as a list.
	 * 
	 * @param kitProdId
	 */
	private void processAjaxRequest(RAMSearchVO svo) {
		List<RAMProductVO> prods = new ArrayList<RAMProductVO>();

		//Query for All Products contained in a kit.
		try(PreparedStatement ps = dbConn.prepareStatement(getProductSql())){
			ps.setInt(1, svo.getProductId());
 
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				prods.add(new RAMProductVO(rs));
			}
		} catch (SQLException e) {
			log.error(e);
		}

		//Store the total number of records.
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
	 */
	private void processStandardRequest(PageVO page, RAMSearchVO svo) {
		//Attempt to read from Cache.
		ModuleVO mod = readFromCache(svo.getProductId());

		//If not found in cache Load data.
		if(mod == null) {

			//Attempt to Load Data for given Product Id
			mod = loadVisionObject(svo.getProductId());

			//If we could load Kit Data, Update Cache Groups and store it.
			if(mod != null) {
				mod.setCacheable(true);
				mod.addCacheGroup(mod.getPageModuleId());
				mod.setPageModuleId(CACHE_PREFIX + svo.getProductId());

				// Add proper cache groups so that we clear when applicable.
				mod.addCacheGroup(page.getPageId());
				super.writeToCache(mod);
			} else {
				log.error("No Product found for productId: " + svo.getProductId());
			}
		} else {
			setAttribute(Constants.MODULE_DATA, mod);
		}
	}

	/**
	 * Helper method that builds and returns the Vision Systems ModuleVO Data.
	 * Data may either by a RAMProductVO for a standard product or a list of 
	 * Image Map VOs for each layer of a kit. 
	 * @param prodId
	 * @return
	 */
	private ModuleVO loadVisionObject(Integer prodId) {

		ModuleVO mod = null;

		//Perform lookup in database for Product.
		RAMProductVO p = getProduct(prodId);

		/*
		 * If we got a valid product and it's a kit.  Process it.
		 */
		if(p != null && p.getKitFlag() == 1) {

			//Instantiate the Parser
			FabricParserInterface<LayerCoordinateVO> fp = new RAMFabricParser<LayerCoordinateVO>();

			//Load Kit Layer Json
			List<KitLayerVO> layers = loadKitLayers(p);

			//Set Up List for ImageMaps.
			List<ImageMapVO> imageMaps = new ArrayList<ImageMapVO>();

			ImageMapVO map = null;

			//Parse each Layer
			for(KitLayerVO k : layers) {
				map = fp.getImageMap(JSONObject.fromObject(k.getJsonData()));
				map.setName(LAYER_ID + k.getDepthNumber());
				imageMaps.add(map);
			}

			//Set Image Map Data on the Request
			this.putModuleData(imageMaps, imageMaps.size(), false);

			//Retrieve the properly formatted Module Data off the request.
			mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		}

		return mod;
	}
	/**
	 * Helper method intended to load the KitData from Cache.
	 * @param prodId
	 * @return
	 */
	private ModuleVO readFromCache(Integer prodId) {
		
		ModuleVO mod = super.readFromCache(CACHE_PREFIX + prodId);
		
		return mod;
	}

	/**
	 * Helper method that loads up all the Kit Layers for a given Kit.
	 * @param p
	 * @return
	 */
	private List<KitLayerVO> loadKitLayers(RAMProductVO p) {
		List<KitLayerVO> layers = new ArrayList<KitLayerVO>();

		//Build the Prepared Statement, set the params and Query for Kit Layers.
		try(PreparedStatement ps = dbConn.prepareStatement(getLayerSql())) {
			ps.setInt(1, p.getProductId());

			ResultSet rs = ps.executeQuery();

			KitLayerVO k = null;

			//Iterate the Result Set and add all the Kit Layers.
			while(rs.next()) {
				k = new KitLayerVO(rs, false);
				k.setJsonData(rs.getString("JSON_DATA"));
				if(isValidJSON(k.getJsonData())) {
					layers.add(k);
				}
			}
		} catch (SQLException e) {
			log.error(e);
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
		StringBuilder sql = new StringBuilder(150);
		sql.append("select * from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_KIT_LAYER where PRODUCT_ID = ? order by LAYOUT_DEPTH_NO");

		return sql.toString();
	}

	/**
	 * Helper method that builds the Kit Product Retrieval Script.
	 * @return
	 */
	private String getProductSql() {
		String schema = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);

		StringBuilder sql = new StringBuilder(400);

		sql.append("select a.LAYOUT_DEPTH_NO, cast(a.KIT_LAYER_ID as nvarchar) + '-' + ");
		sql.append("cast(b.PRODUCT_KIT_ID as nvarchar) as 'COMPOSITE_ID', c.*, ");
		sql.append("d.CUSTOMER_NM, (select ROW_NUMBER() OVER (order by c.PRODUCT_NM)) ");
		sql.append("as RowNum from ");
		sql.append(schema).append("RAM_KIT_LAYER a ");
		sql.append("inner join ").append(schema).append("RAM_PRODUCT_LAYER_XR b ");
		sql.append("on a.KIT_LAYER_ID = b.KIT_LAYER_ID ");
		sql.append("inner join ").append(schema).append("RAM_PRODUCT c ");
		sql.append("on b.PRODUCT_ID = c.PRODUCT_ID ");
		sql.append("inner join ").append(schema).append("RAM_CUSTOMER d ");
		sql.append("on c.CUSTOMER_ID = d.CUSTOMER_ID ");
		sql.append("where a.PRODUCT_ID = ? ");
		sql.append("order by a.LAYOUT_DEPTH_NO, b.KIT_LAYER_ID ");

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
