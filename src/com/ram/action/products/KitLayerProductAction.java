package com.ram.action.products;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ram.datafeed.data.KitLayerProductVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.RecordDuplicatorUtility;

/****************************************************************************
 * <b>Title:</b> KitLayerProductAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Action that handles binding Products to a Kit Layer.
 * <b>Copyright:</b> Copyright (c) 2014
 * <b>Company:</b> Silicon Mountain Technologies
 * @author Billy Larsen
 * @version 3.3
 * @since Aug 15, 2017
 * <b>Changes: </b>
 ***************************************************************************/
public class KitLayerProductAction extends SBActionAdapter {

	public static final String PRODUCT_KIT_ID = "productKitIds";
	public static final String KIT_LAYER_ID = "kitLayerId";
	/**
	 * Default Constructor
	 */
	public KitLayerProductAction() {
		super();
	}

	/**
	 * General Constructor with ActionInitVO Data
	 * @param actionInit
	 */
	public KitLayerProductAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Copy method for cloning the Product XR Records for the new Kit Layers.
	 * After they are cloned, we call out to KitCoordinateAction to clone the
	 * coordinate data for each ProductXR.
	 */
	@Override
	public void copy(ActionRequest req) throws ActionException {
		@SuppressWarnings("unchecked")
		Map<String, Object> replaceVals = (Map<String, Object>) attributes.get(RecordDuplicatorUtility.REPLACE_VALS);
		RecordDuplicatorUtility rdu = new RecordDuplicatorUtility(attributes, dbConn, "RAM_PRODUCT_LAYER_XR", "PRODUCT_KIT_ID", true);
		rdu.setSchemaNm((String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
		rdu.addWhereListClause("KIT_LAYER_ID");
		Map<String, String> productKitIds = rdu.copy();
		replaceVals.put("PRODUCT_KIT_ID", productKitIds);

		//Continue propagating copy up the Action Chain.
		KitCoordinateAction kca = new KitCoordinateAction(getActionInit());
		kca.setDBConnection(dbConn);
		kca.setAttributes(attributes);
		kca.copy(req);
	}
	
	/**
	 * Retrieve the product layer xr bound to a given kitLayer with 
	 * associated product information.
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		//Fast fail if kitLayerId is missing.
		if(!req.hasParameter(KIT_LAYER_ID)) {
			return;
		} else {
			loadKitLayerProductData(req);
		}
	}

	/**
	 * @param req
	 */
	private void loadKitLayerProductData(ActionRequest req) {
		//Build Query
		StringBuilder sb = new StringBuilder(300);
		sb.append("select * from ").append(getCustomSchema()).append("RAM_PRODUCT_LAYER_XR a ");
		sb.append("inner join ").append(getCustomSchema()).append("RAM_PRODUCT c ");
		sb.append("on a.product_id = c.product_id ");
		sb.append("where a.KIT_LAYER_ID = ?");

		List<Object> vals = new ArrayList<>();
		vals.add(Convert.formatInteger(req.getParameter(KIT_LAYER_ID)));

		DBProcessor dbp = new DBProcessor(getDBConnection());
		List<Object> layers = dbp.executeSelect(sb.toString(), vals, new KitLayerProductVO());

		this.putModuleData(layers, layers.size(), false);
	}

	/**
	 * Process the request object and pull the relevant data for Layer Products off it.  
	 * Send the results of the processing through the relevant insert and update methods.
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {

		//Parse the request and get the map of Layer Products
		Map<String, List<KitLayerProductVO>> changes = getChanges(req);

		//Process Kit Products requiring an insert
		processInserts(changes.get("inserts"));

		//Process Kit Products requiring an update
		processUpdates(changes.get("updates"));
	}

	/**
	 * This method takes the request object and extracts the Layer Product from 
	 * of it.  The Results are sorted into lists of updates and inserts and 
	 * returned on a map.
	 * @param req
	 * @return
	 */
	private Map<String, List<KitLayerProductVO>> getChanges(ActionRequest req) {

		//Build Containers
		Map<String, List<KitLayerProductVO>> changes = new HashMap<>();
		List<KitLayerProductVO> inserts = new ArrayList<>();
		List<KitLayerProductVO> updates = new ArrayList<>();
		KitLayerProductVO vo = null;
		String [] values = null;

		/*
		 * Iterate over the req Parameters and process the properly prefixed values.
		 */
		for(String s : req.getParameterMap().keySet()) {
			if(s.startsWith("kitProduct_")) {
				values = req.getParameter(s).split("\\|");
				vo = new KitLayerProductVO();
				vo.setProductKitId(Convert.formatInteger(values[0]));
				vo.setProductId(Convert.formatInteger(values[1]));
				vo.setKitLayerId(Convert.formatInteger(values[2]));
				vo.setCoordinateTypeVal(values[3]);
				vo.setActiveFlag(Convert.formatInteger(values[4]));
				vo.setQuantity(Convert.formatInteger(values[5]));

				/*
				 * If the user saved and there were rows with empty
				 * products, ignore them. Else if there is a productKitId
				 * on the vo then add it to updates.  Otherwise add the
				 * vo to the inserts list.
				 */
				if(vo.getProductId() <= 0) {
					//Ignore these ones.
				}
				else if(vo.getProductKitId() > 0)
					updates.add(vo);
				else
					inserts.add(vo);	
			}
		}
		
		//Add the Lists to the map.
		changes.put("inserts", inserts);
		changes.put("updates", updates);
		
		//Return changes.
		return changes;
	}
	
	/**
	 * This method is responsible for processing the list of KitLayerProductVOs that in
	 * the updates list.  
	 * @param list
	 * @throws ActionException 
	 */
	private void processUpdates(List<KitLayerProductVO> updates) throws ActionException {

		//Build sql statement.
		StringBuilder update = new StringBuilder(200);
		update.append("update ").append(getCustomSchema()).append("RAM_PRODUCT_LAYER_XR set PRODUCT_ID = ?, ");
		update.append("COORDINATE_TYPE_CD = ?, UPDATE_DT = ?, ");
		update.append("ACTIVE_FLG = ?, QUANTITY = ? where PRODUCT_KIT_ID = ?");

		try(PreparedStatement ps = dbConn.prepareStatement(update.toString())) {
			//Update Existing Records
			log.debug(update.toString());
			for(KitLayerProductVO v : updates) {
				ps.setInt(1, v.getProductId());
				ps.setString(2, v.getCoordinateType().name());
				ps.setTimestamp(3, Convert.getCurrentTimestamp());
				ps.setInt(4, v.getActiveFlag());
				ps.setInt(5, v.getQuantity());
				ps.setInt(6, v.getProductKitId());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch(SQLException sqle) {
			log.error("Problem inserting/updating Kit Layer Products.", sqle);
			throw new ActionException(sqle);
		}
	}

	/**
	 * This method is responsible for processing the list of KitLayerProductVOs in
	 * the insert list.  
	 * @param list
	 * @throws ActionException 
	 */
	private void processInserts(List<KitLayerProductVO> inserts) throws ActionException {

		//Build the Sql Statement
		StringBuilder insert = new StringBuilder(200);
		insert.append("insert into ").append(getCustomSchema()).append("RAM_PRODUCT_LAYER_XR (PRODUCT_ID, ");
		insert.append("KIT_LAYER_ID, COORDINATE_TYPE_CD, CREATE_DT, ACTIVE_FLG, QUANTITY) ");
		insert.append("values (?,?,?,?,?,?)");

		try(PreparedStatement ps = dbConn.prepareStatement(insert.toString())) {
			//Insert new Records
			log.debug(insert);
			for(KitLayerProductVO v : inserts) {
				ps.setInt(1, v.getProductId());
				ps.setInt(2, v.getKitLayerId());
				ps.setString(3, v.getCoordinateType().name());
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				ps.setInt(5, v.getActiveFlag());
				ps.setInt(6, v.getQuantity());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch(SQLException sqle) {
			log.debug("There was an error inserting new records.", sqle);
			throw new ActionException(sqle);
		}
	}
}