package com.ram.action.products;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import net.sf.json.JSONObject;

import com.ram.action.util.RAMFabricParser;
import com.ram.datafeed.data.LayerCoordinateVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.imageMap.FabricParserInterface;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.RecordDuplicatorUtility;

/****************************************************************************
 * <b>Title</b>: KitCoordinateParser.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> This class is responsible for extracting coordinate
 * data from the Canvas Elements JSON Data that gets returned on a Kit Layer
 * update.  We calculate the coordinate points for each shape, dump them to a
 * list.  Retrieve all the existing coordinates from the db.  Merge the two
 * lists using a fuzzy match into a map of Lists containing coordinates that
 * require update and insert.  Lastly, we take all the coordinates and 
 * update and/or insert the changes to the db via a batch statement. 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Jul 1, 2014
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class KitCoordinateAction extends SBActionAdapter {

	public static final String KIT_COORDINATE_ID = "kitCoordinateId"; 
	/**
	 * 
	 */
	public KitCoordinateAction() {
		
	}
	
	public KitCoordinateAction(ActionInitVO init) {
		super(init);
	}
	
	/**
	 * Copy Method clones all Coordinates related to the PRODUCT_KIT_ID map on the
	 * replaceVals Map.
	 */
	@Override
	public void copy(SMTServletRequest req) throws ActionException {
		@SuppressWarnings("unchecked")
		Map<String, Object> replaceVals = (Map<String, Object>) attributes.get(RecordDuplicatorUtility.REPLACE_VALS);
		RecordDuplicatorUtility rdu = new RecordDuplicatorUtility(attributes, dbConn, "RAM_LAYER_COORDINATE", "LAYER_COORDINATE_ID", true);
		rdu.setSchemaNm((String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
		rdu.addWhereListClause("PRODUCT_KIT_ID");
		Map<String, String> kitCoordinateIds = rdu.copy();
		replaceVals.put("KIT_COORDINATE_ID", kitCoordinateIds);
	}
	
	/**
	 * This method handles the business logic for what must happen to parse and update
	 * the Coordinate data.
	 */
	public void build(SMTServletRequest req) throws ActionException {
		
		//If there is no jsonData, quick fail.
		if(!req.hasParameter("jsonData"))
			return;
		
		//Initial list to hold coordinate data parsed from json Data
		List<LayerCoordinateVO> coordinates = new ArrayList<LayerCoordinateVO>();
		
		//Build JSONObject of fabric js coordinate data.
		JSONObject json = JSONObject.fromObject(req.getParameter("jsonData"));

		//Instantiate a FabricParser and parse the coordinate data.
		FabricParserInterface<LayerCoordinateVO> fp = new RAMFabricParser<LayerCoordinateVO>();
		coordinates = fp.getCoordinatesFromShape(json);

		/*
		 * TODO - Possibly manage saving/updating the VisionSystem ImageMap of a given kit layer here.
		 */
		//Create coordinate map of data.
//		ImageMapVO map = fp.getImageMap(json);
//		map.setName("test");
//		log.debug(map.toString());

		//We need to clear existing coordinates for this update.
		List<LayerCoordinateVO> existingCoordinates = getExistingCoordinates(req.getParameter("kitLayerId"));

		//Merge the coordinates.
		Map<String, List<LayerCoordinateVO>> changes = mergeCoordinates(coordinates, existingCoordinates);

		//Insert or Update Coordinates
		updateCoordinates(changes);
	}

	/**
	 * Update the database with the new coordinate information.  We perform updates
	 * and inserts on the data depending on what is required.
	 * @param changes
	 * @throws ActionException
	 */
	private void updateCoordinates(Map<String, List<LayerCoordinateVO>> changes) throws ActionException{
		String customDb = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		
		//Build the sql call for coordinate updates.
		StringBuilder update = new StringBuilder();
		update.append("update ").append(customDb).append("RAM_LAYER_COORDINATE ");
		update.append("set UPDATE_DT = ?, ACTIVE_FLG = ?, X_POINT_NO = ?, ");
		update.append("Y_POINT_NO = ? where LAYER_COORDINATE_ID = ?");
		
		//Build the sql call for coordiante inserts.
		StringBuilder insert = new StringBuilder();
		insert.append("insert into ").append(customDb).append("RAM_LAYER_COORDINATE ");
		insert.append("(CREATE_DT, ACTIVE_FLG, X_POINT_NO, Y_POINT_NO, PRODUCT_KIT_ID) ");
		insert.append("values (?,?,?,?,?)");
		
		PreparedStatement ps = null;
		
		//If we have pending updates, iterate the list into a batch call and execute.
		if(changes.containsKey("update") && changes.get("update").size() > 0) {
			try {
				ps = dbConn.prepareStatement(update.toString());
				for(LayerCoordinateVO v : changes.get("update")) {
					ps.setTimestamp(1, Convert.getCurrentTimestamp());
					ps.setInt(2, v.getActiveFlag());
					ps.setInt(3, v.getHorizontalPoint());
					ps.setInt(4, v.getVerticalPoint());
					ps.setInt(5, v.getId());
					ps.addBatch();
				}
				ps.executeBatch();
			} catch (SQLException sqle) {
				log.error("Problem Updating Layer coordinates.", sqle);
				throw new ActionException(sqle);
			} finally {
				DBUtil.close(ps);
			}
		}
		//Ensure we reset the ps
		ps = null;
		
		//If we have pending inserts, iterate the list into a batch call and execute.
		if(changes.containsKey("insert") && changes.get("insert").size() > 0) {
			try {
				ps = dbConn.prepareStatement(insert.toString());
				for(LayerCoordinateVO v : changes.get("insert")) {
					ps.setTimestamp(1, Convert.getCurrentTimestamp());
					ps.setInt(2, v.getActiveFlag());
					ps.setInt(3, v.getHorizontalPoint());
					ps.setInt(4, v.getVerticalPoint());
					ps.setInt(5, v.getProductLayerId());
					ps.addBatch();
				}
				ps.executeBatch();
			} catch (SQLException sqle) {
				log.error("Problem Inserting Layer coordinates.", sqle);
				throw new ActionException(sqle);
			} finally {
				DBUtil.close(ps);
			}
		}
	}

	/**
	 * This method handles merging json parsed coordinates with existing coordinates
	 * retrieved from the database.
	 * @param coordinates
	 * @param existingCoordinates
	 */
	private Map<String, List<LayerCoordinateVO>> mergeCoordinates(List<LayerCoordinateVO> nc, List<LayerCoordinateVO> ec) {
		
		//Build initial map and lists.
		Map<String, List<LayerCoordinateVO>> changes = new HashMap<String, List<LayerCoordinateVO>>();
		List<LayerCoordinateVO> updates = new ArrayList<LayerCoordinateVO>();
		List<LayerCoordinateVO> inserts = new ArrayList<LayerCoordinateVO>();
		
		/*
		 * Match the Coordinates based on productLayerId on a first come first served basis.
		 * As We match them, update the existing record to preserve the coordinateId in the db,
		 * add it to the merged records list and remove both the new and existing record from 
		 * the other lists.
		 */
		
		//Build initial iterators
		ListIterator<LayerCoordinateVO> eci = ec.listIterator();
		ListIterator<LayerCoordinateVO> nci;

		//Loop over existing db coordinates on the outside.
		while(eci.hasNext()) {
			
			//Get the next existing coordinate in the list and create iterator of new points.
			LayerCoordinateVO v = eci.next();
			nci = nc.listIterator();
			
			//Loop over new coordinates
			while(nci.hasNext()) {
				
				//Get the next new coordinate from the list
				LayerCoordinateVO n = nci.next();
				
				/*
				 * Perform fuzzy search on KitProductId as there is no way to compare unique 
				 * coordinate id's here.  If they match then update the existing coordinates
				 * data with that of the new coordinates. Add the updated existing point to the
				 * updates List and remove the current existing and new points from their respective
				 * lists.  Break out of the loop as we've made our match.
				 */
				if(v.getProductLayerId().equals(n.getProductLayerId())) {
					v.setHorizontalPoint(n.getHorizontalPoint());
					v.setVerticalPoint(n.getVerticalPoint());
					v.setActiveFlag(1);
					updates.add(v);
					eci.remove();
					nci.remove();
					break;
				}
			}
		}
		
		//Reset the existing lists iterator
		eci = ec.listIterator();
		
		/*
		 * After we have made all available matches, assume that anything left in the ec list
		 * has been removed/disabled.  Set the activeFlag to 0 and add it to the updates list.
		 */
		while(eci.hasNext()) {
			LayerCoordinateVO v = eci.next();
			v.setActiveFlag(0);
			updates.add(v);
			eci.remove();
		}
		
		//Store the updates list on the change map.
		changes.put("update", updates);
		
		/*
		 * After we have made all available matches, assume that anything left in the nc list
		 * is a new product coordinate and add it to the inserts list.
		 */
		nci = nc.listIterator();
		while(nci.hasNext())
			inserts.add(nci.next());
		
		//Store the inserts list on the change map.
		changes.put("insert", inserts);
		
		//The nc and ec lists should be empty now, verify.
		log.debug("NC Size: " + nc.size());
		log.debug("EC Size: " + ec.size());
		
		//Return the map of coordinates.
		return changes;
	}

	/**
	 * This method is responsible for retrieving all the existing Coordinates for a given
	 * kitLayer.
	 * @param kitLayerId
	 * @return
	 * @throws ActionException
	 */
	private List<LayerCoordinateVO> getExistingCoordinates(String kitLayerId) throws ActionException{
		List<LayerCoordinateVO> coordinates = new ArrayList<LayerCoordinateVO>();
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		
		//Build Query
		StringBuilder sb = new StringBuilder();
		sb.append("select a.* from ").append(customDb);
		sb.append("RAM_LAYER_COORDINATE a inner join ").append(customDb);
		sb.append("RAM_PRODUCT_LAYER_XR b on a.PRODUCT_KIT_ID = b.PRODUCT_KIT_ID ");
		sb.append("inner join ").append(customDb).append("RAM_KIT_LAYER c ");
		sb.append("on b.KIT_LAYER_ID = c.KIT_LAYER_ID where c.KIT_LAYER_ID = ?");
		
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, kitLayerId);
			ResultSet rs = ps.executeQuery();
			
			//Add all coordinates to list.
			while(rs.next())
				coordinates.add(new LayerCoordinateVO(rs, false));
		} catch (SQLException sqle) {
			log.error("Error retrieving Kit Layer Coordinates", sqle);
			throw new ActionException(sqle);
		} finally {
			DBUtil.close(ps);
		}
		
		return coordinates;
	}

}
