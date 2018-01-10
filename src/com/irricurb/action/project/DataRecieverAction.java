package com.irricurb.action.project;

import java.lang.reflect.Type;
// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;

// Google Gson 2.4
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
// App Libs
import com.irricurb.action.data.vo.DeviceDataVO;
import com.irricurb.action.data.vo.DeviceEntityDataVO;
import com.irricurb.action.data.vo.ProjectDeviceAttributeVO;
import com.irricurb.action.data.vo.ProjectDeviceVO;

// SMT Base Libs 3.3
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
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
		
		log.info(json);
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
			attr.setProjectDeviceId(data.getProjectDeviceId());
			getDeviceIds(db, attr);
			log.info(attr);
			db.save(attr);
		}
		
	}
	
	/**
	 * Retrieves the proper keys for the attributes being sent
	 * @param db
	 * @param attr
	 */
	public void getDeviceIds(DBProcessor db, ProjectDeviceAttributeVO attr) {
		StringBuilder sql = new StringBuilder(256); 
		sql.append("select device_attribute_xr_id as key, c.attribute_device_id as value from ");
		sql.append(getCustomSchema()).append("ic_project_device a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ic_device b ");
		sql.append("on a.device_id = b.device_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ic_attribute_device c ");
		sql.append("on b.device_id = c.device_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("ic_device_attribute_xr d ");
		sql.append("on c.attribute_device_id = d.attribute_device_id ");
		sql.append("where a.project_device_id = ? and c.device_attribute_id = ? ");
		
		List<Object> params = new ArrayList<>(2); 
		params.add(attr.getProjectDeviceId());
		params.add(attr.getDeviceAttributeId());
		log.debug(sql + "|" + params);
		List<GenericVO> items = db.executeSelect(sql.toString(), params, new GenericVO());
		log.info(items);
		if (! items.isEmpty()) {
			attr.setDeviceAttributeXrId((String)items.get(0).getKey());
			attr.setAttributeDeviceId((String)items.get(0).getValue());
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
		log.info("Ok Here");
		Type listType = new TypeToken<ArrayList<DeviceDataVO>>(){}.getType();
		List<DeviceDataVO> readings = g.fromJson(json, listType);
		log.info(readings);
		
		// Save the reading master entry
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		for (DeviceDataVO data : readings) {
			db.save(data);
			
			// Save the data for each reading
			for (DeviceEntityDataVO reading:  data.getReadings()) {
				reading.setProjectDeviceDataId(data.getProjectDeviceDataId());
				db.save(reading);
				
				// Update the sensor value that's stored in the DB.
				updateSensorValue(db, data.getProjectDeviceId(), reading.getDeviceAttributeId(), reading.getReadingValue());
			}
		}
	}
	
	/**
	 * Updates the value of the sensor to the current value given the attribute id
	 * @param db
	 * @param projectDeviceId
	 * @param deviceAttributeId
	 * @param value
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	protected void updateSensorValue(DBProcessor db, String projectDeviceId, String deviceAttributeId, double value) 
	throws InvalidDataException, DatabaseException {
		StringBuilder sql = new StringBuilder(256);
		sql.append("select device_attribute_xr_id as key, c.attribute_device_id as value from ");
		sql.append(getCustomSchema()).append("ic_project_device a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ic_device b ");
		sql.append("on a.device_id = b.device_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ic_attribute_device c ");
		sql.append("on b.device_id = c.device_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("ic_device_attribute_xr d ");
		sql.append("on c.attribute_device_id = d.attribute_device_id ");
		sql.append("where a.project_device_id = ? and c.device_attribute_id = ? ");
		
		List<Object> params = new ArrayList<>(2); 
		params.add(projectDeviceId);
		params.add(deviceAttributeId);
		log.info(sql + "|" + params);
		
		// Get the AProj device attribute id
		List<GenericVO> xrIds = db.executeSelect(sql.toString(), params, new GenericVO());
		String xrId = null, attrDeviceId = null;
		if (!xrIds.isEmpty()) {
			xrId = (String) xrIds.get(0).getKey();
			attrDeviceId = (String) xrIds.get(0).getValue();
		}
		
		// Assign the values to the bean and update the table
		ProjectDeviceAttributeVO attr = new ProjectDeviceAttributeVO();
		attr.setDeviceAttributeXrId(xrId);
		attr.setProjectDeviceId(projectDeviceId);
		attr.setAttributeDeviceId(attrDeviceId);
		attr.setValue(value + "");
		db.save(attr);
	}

}
