package com.ram.action.products;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.ram.datafeed.data.LayerCoordinateVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

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
public class KitCoordinateParser extends SBActionAdapter {

	/**
	 * 
	 */
	public KitCoordinateParser() {
		
	}
	
	public KitCoordinateParser(ActionInitVO init) {
		super(init);
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
		
		/*
		 * Build JSONObject from data and iterate over it parsing the proper 
		 * coordinate information.
		 */
		
		JSONObject json = JSONObject.fromObject(req.getParameter("jsonData"));
		for(Object obj : json.optJSONArray("objects")){
			coordinates.addAll(getCoordinatesFromShape((JSONObject) obj));
		}
		
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

	/**
	 * Method responsible for properly retrieving coordinates for a given shape.  For normal shapes
	 * a standard 2 point coordinate map is fine, however for polygons this will change so we need
	 * to parse them differently.
	 * @param obj
	 * @return
	 */
	private Collection<? extends LayerCoordinateVO> getCoordinatesFromShape(JSONObject shape) {
		List<LayerCoordinateVO> c = new ArrayList<LayerCoordinateVO>();
		switch(shape.getString("type")) {
		case "rect" : 
		case "circle" :
			c.add(getCoordinate(shape));
			c.add(getBottomRightCoordinate(shape));
		break;
		case "polygon" :
			c.addAll(getPolyCoordinates(shape));
			break;
		}			
		return c;
	}
	
	/**
	 * Parse out the Polygon coordinates from the points on the shape object.  The points 
	 * are caculated from the center of the polygon so we need to perform some slight
	 * calculation to get the real coordinates.
	 * @param shape
	 * @return
	 */
	private Collection<? extends LayerCoordinateVO> getPolyCoordinates(JSONObject shape) {
		List<LayerCoordinateVO> pc = new ArrayList<LayerCoordinateVO>();
		LayerCoordinateVO coord = new LayerCoordinateVO();
		
		//Get the Center of the shape.
		int cy = shape.getInt("top") + shape.getInt("height") / 2;
		int cx = shape.getInt("left") + shape.getInt("width") / 2;

		/*
		 * For each point on the shape, calculate the x and y and add it to the 
		 */
		JSONArray points = shape.getJSONArray("points");
		for(Object p : points.toArray()) {
			coord = new LayerCoordinateVO();
			coord.setActiveFlag(1);
			coord.setHorizontalPoint(cx + ((JSONObject)p).getInt("x"));
			coord.setVerticalPoint(cy + ((JSONObject)p).getInt("y"));
			coord.setProductLayerId(Convert.formatInteger(shape.getString("id").split("-")[1]));
			pc.add(coord);
		}
		return pc;
	}

	/**
	 * Parse out a basic Coordinate Point from the JSONShape.
	 * @param shape
	 * @return
	 */
	private LayerCoordinateVO getCoordinate(JSONObject shape) {
		LayerCoordinateVO coord = new LayerCoordinateVO();
		coord.setActiveFlag(1);
		coord.setHorizontalPoint(shape.getInt("left"));
		coord.setVerticalPoint(shape.getInt("top"));
		coord.setProductLayerId(Convert.formatInteger(shape.getString("id").split("-")[1]));
		return coord;
	}

	/**
	 * For Circles and Rectangles we have a second bottom right coordinate
	 * that we need to parse out.  In fabric this is handled via a calculation
	 * involving the basic coordinate and the dimension * scaleFactor.  We perform
	 * the math to get the right points and update a new LayerCoordinateVO
	 * accordingly.
	 * @param shape
	 * @return
	 */
	public LayerCoordinateVO getBottomRightCoordinate(JSONObject shape) {
		LayerCoordinateVO coord = new LayerCoordinateVO();
		coord.setActiveFlag(1);
		int bottom = 0;
		int right = 0;
		switch(shape.getString("type")) {
		case "rect" : 
			bottom = shape.getInt("top") + (int)(shape.getInt("height") * shape.getDouble("scaleY"));
			right = shape.getInt("left") + (int)(shape.getInt("width") * shape.getDouble("scaleX"));
		break;
		case "circle" :
			bottom = shape.getInt("top") + (int)(shape.getInt("width") * shape.getDouble("scaleY"));
			right = shape.getInt("left") + (int)(shape.getInt("height") * shape.getDouble("scaleX"));
		break;
		}
		
		coord.setHorizontalPoint(right);
		coord.setVerticalPoint(bottom);
		coord.setProductLayerId(Convert.formatInteger(shape.getString("id").split("-")[1]));
		return coord;
	}

}
