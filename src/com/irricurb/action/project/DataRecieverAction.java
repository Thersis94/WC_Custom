package com.irricurb.action.project;

import com.google.gson.Gson;
import com.irricurb.action.data.vo.DeviceDataVO;
import com.irricurb.action.data.vo.DeviceEntityDataVO;
import com.irricurb.action.data.vo.ProjectDeviceAttributeVO;
import com.irricurb.action.data.vo.ProjectDeviceVO;

// SMT Base Libs 3.3
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;

// WC Libs 3.2
import com.smt.sitebuilder.action.SimpleActionAdapter;

/********************************************************************
 * <b>Title: </b>DataRecieverAction.java<br/>
 * <b>Description: </b>Action receiving data from the controller for 
 * sensors as well as updated device state.  The updated device state 
 * will be device attribute information that changes due to local logic. 
 * This will ensure the portal is in sync with the controller<br/>
 * <b>Copyright: </b>Copyright (c) 2018<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Jan 7, 2018
 * Last Updated: 
 *******************************************************************/
public class DataRecieverAction extends SimpleActionAdapter {

	public static final String DATA_TYPE = "type";
	
	public enum DataType {
		SENSOR, DEVICE;
	}
	
	/**
	 * 
	 */
	public DataRecieverAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public DataRecieverAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String typeVal = StringUtil.checkVal(req.getParameter("type")).toUpperCase();
		DataType type = EnumUtil.safeValueOf(DataType.class, typeVal, DataType.DEVICE);
		String json = req.getStringParameter("data", "");
		log.info("Data Receiver Type: " + type);
		try {
			if(type.equals(DataType.SENSOR)) {
				processSensor(json);
			} else {
				processDevice(json);
			}
		} catch (Exception e) {
			putModuleData(null, 0, false, e.getMessage(), true);
		}
	}
	
	/**
	 * 
	 * @param json
	 */
	public void processDevice(String json) throws InvalidDataException, DatabaseException {
		if (json.isEmpty()) throw new InvalidDataException("No JSON Data Available");
		Gson g = new Gson();
		ProjectDeviceVO data = g.fromJson(json, ProjectDeviceVO.class);
		
		// Updates or inserts each attribute into the table
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		for (ProjectDeviceAttributeVO attr : data.getAttributes()) {
			db.save(attr);
		}
		
	}
	
	/**
	 * 
	 * @param json
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	public void processSensor(String json) throws InvalidDataException, DatabaseException {
		if (json.isEmpty()) throw new InvalidDataException("No JSON Data Available");
		Gson g = new Gson();
		DeviceDataVO data = g.fromJson(json, DeviceDataVO.class);
		
		// Save the reading master entry
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.save(data);
		
		// Save the data for each reading
		for (DeviceEntityDataVO reading:  data.getReadings()) {
			db.save(reading);
		}
	}

}
