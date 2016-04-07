/**
 * 
 */
package com.ram.action.util;

import net.sf.json.JSONObject;

import com.ram.datafeed.data.LayerCoordinateVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.imageMap.FabricParser;

/****************************************************************************
 * <b>Title: </b>RAMFabricParser.java
 * <b>Project: </b>WC_Custom
 * <b>Description: </b> Class that manages converting a RAM Kit Coordinate set
 * into an HTML ImageMap.
 * <b>Copyright:</b> Copyright (c) 2015
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author raptor
 * @version 1.0
 * @since Apr 14, 2015
 *        <b>Changes: </b>
 ****************************************************************************/
public class RAMFabricParser<T extends LayerCoordinateVO> extends FabricParser<T> {

	/**
	 * No Arg Constructor will assume we want to use the standard LayerCoordinateVO.
	 */
	@SuppressWarnings("unchecked")
	public RAMFabricParser() {
		this((Class<T>)LayerCoordinateVO.class);
	}

	/**
	 * @param tClass
	 */
	public RAMFabricParser(Class<T> tClass) {
		super(tClass);
	}

	/**
	 * Custom post processor that sets ram specific data on the coordinate vo.
	 */
	@Override
	public void postProcessCoordinate(T coord, JSONObject json) {

		//Call Super.
		super.postProcessCoordinate(coord, json);

		//Set the Active Flag and Product Layer Id on the coordinateVO.
		coord.setActiveFlag(1);
		String id = json.getString("id");

		//Validate that we have a proper id on the object before parsing.
		if(!id.equals("null") && id.contains("_")) {
			coord.setProductLayerId(Convert.formatInteger(id.split("-")[1]));
		}
	}

	
}
