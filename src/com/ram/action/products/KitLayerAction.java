package com.ram.action.products;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.ram.datafeed.data.KitLayerVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.RecordDuplicatorUtility;

/****************************************************************************
 * <b>Title</b>: KitLayerAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Action that handles all calls to creating and editing
 * Kit Layers for a product. We also handle calling out to the coordinate
 * parser to extract coordinate data from the Canvas Elements JSON Data that
 * is returned on update.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since May 20, 2014
 *        <p/>
 *        <b>Changes: </b>
 *************************************************************************** 
 */
public class KitLayerAction extends SBActionAdapter {

	public static final String KIT_LAYER_ID = "kitLayerId";
	
	/**
	 * Default Constructor
	 */
	public KitLayerAction() {
		super();
	}

	/**
	 * General Constructor with ActionInitVO Data
	 * @param actionInit
	 */
	public KitLayerAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Copy method for cloning the Kit Layers of a Kit.  After copying the layers
	 * we call out to the KitLayerProductAction to clone the associated Products
	 * per layer.  Upon completion, we update the JSONData associated on each
	 * kit layer to match the new layer and product xr Ids.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void copy(ActionRequest req) throws ActionException {
		//Clone the Kit Product Layers.
		Map<String, Object> replaceVals = (Map<String, Object>) attributes.get(RecordDuplicatorUtility.REPLACE_VALS);
		RecordDuplicatorUtility rdu = new RecordDuplicatorUtility(attributes, dbConn, "RAM_KIT_LAYER", "KIT_LAYER_ID", true);
		rdu.setSchemaNm((String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
		rdu.addWhereListClause("PRODUCT_ID");
		Map<String, String> kitLayerIds = rdu.copy();
		replaceVals.put("KIT_LAYER_ID", kitLayerIds);

		//Continue propagating copy up the Action Chain.
		KitLayerProductAction klpa = new KitLayerProductAction(getActionInit());
		klpa.setDBConnection(dbConn);
		klpa.setAttributes(attributes);
		klpa.copy(req);

		//Update the Kit Layers Image Paths and JSON Data from the data on the attributes map.
		Map<String, String> data = getJsonData(req.getParameter("productId"));
		String id = null;
		if(data != null) {
			Map<String, String> updatedJson = new HashMap<String, String>();

			//Loop over the Kit Layers
			for(String kitLayerId : data.keySet()) {
				String newKitLayerId = kitLayerIds.get("" + kitLayerId);
				Map<String, String> prodKitIds = (Map<String, String>)replaceVals.get("PRODUCT_KIT_ID");
				JSONObject obj = JSONObject.fromObject(data.get(kitLayerId));
				log.debug(obj);
				JSONArray objs = obj.getJSONArray("objects");

				//Loop over the JSONData Object and update the id field.
				for(int i = 0; i < objs.size(); i++) {
					JSONObject o = objs.getJSONObject(i);
					id = o.getString("id");
					if(id != null) {
						o.put("id", newKitLayerId + "-" + prodKitIds.get(id.split("-")[1]));
					}
				}

				//Place the updated data on the map.
				log.debug(obj);
				updatedJson.put(newKitLayerId, obj.toString());
			}

			//Update the JSON Data in the Database.
			saveJsonData(updatedJson);
			log.debug("JSONData Updated!");
		}
	}
	
	/**
	 * We handle inserting or updating information about kit layers here. 
	 * We also handle calling out to extract Coordinate Data from the canvas
	 * elements JSON Data that gets returned at the end if there is any present.
	 * Lastly, if this is an insert, we get the inserted primary key off the 
	 * preparedStatement and return that to the view.
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		
		//Deactivate request validation so we can save the Json Data for canvas element.
		req.setValidateInput(false);
		
		//Build initial success map.
		Map<String, String> result = new HashMap<String, String>();
		result.put("success", "true");
		result.put("msg", "Data Successfully Updated");
		
		//Build Query depending on if this is insert or update.
		StringBuilder sb = new StringBuilder();
		if(req.hasParameter(KIT_LAYER_ID)) {
			sb.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sb.append("RAM_KIT_LAYER set IMAGE_PATH_URL = ?, LAYOUT_DEPTH_NO = ?, ");
			sb.append("UPDATE_DT = ?, ACTIVE_FLG = ?, JSON_DATA = ? ");
			sb.append("where KIT_LAYER_ID = ?");
		}
		else {
			sb.append("insert into ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sb.append("RAM_KIT_LAYER (IMAGE_PATH_URL, LAYOUT_DEPTH_NO, CREATE_DT, ");
			sb.append("ACTIVE_FLG, PRODUCT_ID) values (?,?,?,?,?)");
		}

		//Log sql Statement for verification
		log.debug("sql: " + sb.toString());
		
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, req.getParameter("imagePathUrl"));
			ps.setString(2, req.getParameter("layoutDepthNo"));
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.setInt(4, Convert.formatInteger(Convert.formatBoolean(req.getParameter("activeFlag"))));
			
			//Set additional parameters based on isnert or update.
			if(req.hasParameter(KIT_LAYER_ID)) {
				ps.setString(5, req.getParameter("jsonData"));
				ps.setInt(6, Convert.formatInteger(req.getParameter(KIT_LAYER_ID)));
			} else {
				ps.setInt(5, Convert.formatInteger(req.getParameter("kitId")));
			}
			
			ps.execute();
		} catch(SQLException sqle) {
			log.error(sqle);
			result.put("success", "false");
			result.put("msg", "Problem Saving Record");
		} finally {
			try {
				/*
				 * Since the primary key is autogenerated, we need to get that back
				 * from the PreparedStatement so that the view can work with valid
				 * information.
				 */
				if (ps != null) {
					ResultSet rs = ps.getGeneratedKeys();
					if(rs.next())
						result.put("kitLayerId", rs.getString(1));
					
					DBUtil.close(ps);
				}
			} catch(Exception e) {}
		}
		
		/*
		 * If this is an update, we need to parse the Canvas Json element to
		 * get the Coordinate data for the kit layer products.
		 */
		if(req.hasParameter(KIT_LAYER_ID)) {
			ActionInterface sai = new KitCoordinateAction(this.actionInit);
			sai.setAttributes(attributes);
			sai.setDBConnection(dbConn);
			sai.build(req);
		}
		super.putModuleData(result);

	}

	/**
	 * We can't delete anything in RAM due to sync issues so we mark items
	 * inactive.  This handles marking a given kitLayer as inactive.
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		//Build Query
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("RAM_KIT_LAYER set ACTIVE_FLG = 0 ");
		sb.append("where KIT_LAYER_ID = ? order by LAYER_DEPTH_NO");

		//Log sql Statement for verification
		log.debug("sql: " + sb.toString());
		
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, req.getParameter(KIT_LAYER_ID));
			ps.execute();
			((ModuleVO)attributes.get(Constants.MODULE_DATA)).setErrorMessage("Deactivation Successful");
		} catch(SQLException sqle) {
			log.error(sqle);
			((ModuleVO)attributes.get(Constants.MODULE_DATA)).setErrorMessage("Deactivation Failed");
			throw new ActionException(sqle);
		} finally {
			DBUtil.close(ps);
		}		
		
	}

	/**
	 * Retrieve all the kit layers for a given productId
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		
		List<KitLayerVO> layers = new ArrayList<KitLayerVO>();
		KitLayerVO layer = new KitLayerVO();
				
		//Build Query
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("RAM_KIT_LAYER where PRODUCT_ID = ?");
		
		sb.append(" order by LAYOUT_DEPTH_NO, KIT_LAYER_ID");
		
		//Log sql Statement for verification
		log.debug("sql: " + sb.toString());
		
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, req.getParameter("productId"));
			
			ResultSet rs = ps.executeQuery();
			
			//Build list of layers and set the JSON_DATA on them.
			while(rs.next()) {
				layer = new KitLayerVO(rs, false);
				layer.setJsonData(rs.getString("JSON_DATA"));
				layers.add(layer);
			}
		} catch(SQLException sqle) {
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			DBUtil.close(ps);
		}
		
		//Set the layers on the Module Data.
		this.putModuleData(layers, layers.size(), false);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		
	}

	/**
	 * Helper method for retrieving the JSONData for the original kits layers.
	 * @param prodId
	 * @return
	 */
	private Map<String, String> getJsonData(String prodId) {
		Map<String, String> jsonData = new LinkedHashMap<String, String>();
		StringBuilder sb = new StringBuilder(90);
		sb.append("select KIT_LAYER_ID, JSON_DATA from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("RAM_KIT_LAYER where PRODUCT_ID = ?");

		try(PreparedStatement ps = dbConn.prepareStatement(sb.toString())) {
			ps.setString(1, prodId);
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				jsonData.put(rs.getString("KIT_LAYER_ID"), rs.getString("JSON_DATA"));
			return jsonData;
		} catch (SQLException e) {
			log.debug(e);
		}
		return null;
	}

	/**
	 * Helper method for updating the JSONData for the new Kit layers.
	 * @param data
	 */
	private void saveJsonData(Map<String, String> data) {
		StringBuilder sb = new StringBuilder(110);
		sb.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("RAM_KIT_LAYER set JSON_DATA = ?, UPDATE_DT = ? where KIT_LAYER_ID = ?");

		try (PreparedStatement ps = dbConn.prepareStatement(sb.toString())){
			for(String key : data.keySet()) {
				ps.setString(1, data.get(key));
				ps.setTimestamp(2, Convert.getCurrentTimestamp());
				ps.setString(3, key);
				ps.addBatch();
			}
			ps.executeBatch();
		} catch(SQLException e) {
			log.error("There was a problem updating the JSON Data", e);
		}
	}
}
