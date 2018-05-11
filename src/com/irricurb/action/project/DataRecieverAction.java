package com.irricurb.action.project;

// JDK 1.8.x
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;

// Google Gson 2.4
import com.google.gson.Gson;

// App Libs
import com.irricurb.action.data.vo.DeviceDataVO;
import com.irricurb.action.data.vo.DeviceEntityDataVO;
import com.irricurb.action.data.vo.ProjectDeviceAttributeVO;
import com.irricurb.action.data.vo.ProjectDeviceVO;
import com.irricurb.action.data.vo.ProjectLocationVO;
import com.irricurb.util.ControllerUtil;
import com.irricurb.util.ICConstants;

// SMT Base Libs 3.3
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.security.AuthorizationException;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.util.Convert;
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
		log.info("Data Received");
		String msg = null;
		String typeVal = StringUtil.checkVal(req.getParameter("type")).toUpperCase();
		DataType type = EnumUtil.safeValueOf(DataType.class, typeVal, DataType.DEVICE);
		
		try {
			String json = URLDecoder.decode(req.getStringParameter("data", ""), "UTF-8");
			// checkSecurityKey(req);
			
			if(type.equals(DataType.SENSOR)) {
				processSensor(json);
			} else {
				processDevice(json);
			}
		} catch (Exception e) {
			log.error("Failed to process data", e);
			msg = e.getMessage();
		}
		
		putModuleData(null, 0, false, msg, msg != null );
	}
	
	/**
	 * Validates the security key to authenticate that the request is valid
	 * @param req
	 * @throws UnsupportedEncodingException
	 * @throws EncryptionException
	 * @throws AuthorizationException
	 */
	protected void checkSecurityKey(ActionRequest req) throws UnsupportedEncodingException, EncryptionException, AuthorizationException {
		/// Make sure the security key is recognized
		String secKey = URLDecoder.decode(req.getParameter(ICConstants.SECURITY_KEY), "UTF-8");
		
		String projectLocationId = req.getParameter("projectLocationId");
		ProjectLocationVO projectLoc = getProjectLocation(projectLocationId, (String) getAttribute(Constants.CUSTOM_DB_SCHEMA));
		String encKey = (String) attributes.get(ICConstants.IC_ENCRYPT_KEY);
		ControllerUtil.checkAuthorization(encKey, secKey, projectLoc.getProjectId(), req.getRemoteAddr(), projectLoc.getNetworkAddress());
	
	}
	
	/**
	 * 
	 * @param locationId
	 * @param schema
	 * @return
	 */
	public ProjectLocationVO getProjectLocation(String locationId, String schema) {
		ProjectLocationVO loc = new ProjectLocationVO();
		loc.setProjectLocationId(locationId);
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		try {
			db.getByPrimaryKey(loc);
		} catch(Exception e) { /* nothing to do as an empty project id will result in no auth */ }
		
		return loc;
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
			updateDeviceValue(db, attr);
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
		
		log.info("Sensor: " + json);
		Gson g = new Gson();
		ProjectDeviceVO device = g.fromJson(json, ProjectDeviceVO.class);
		
		// Save the reading master entry
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		DeviceDataVO data = new DeviceDataVO();
		data.setReadingDate(device.getCreateDate());
		data.setProjectDeviceId(device.getProjectDeviceId());
		data.setCreateDate(new Date());
		db.save(data);
			
		// Save the data for each reading
		for (ProjectDeviceAttributeVO attr:  device.getAttributes()) {
			DeviceEntityDataVO reading = new DeviceEntityDataVO();
			reading.setProjectDeviceDataId(data.getProjectDeviceDataId());
			reading.setDeviceAttributeId(attr.getDeviceAttributeId());
			reading.setReadingValue(Convert.formatDouble(attr.getValue()));
			reading.setCreateDate(new Date());
			db.save(reading);
			
			// Update the sensor value that's stored in the DB.
			updateDeviceValue(db, attr);
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
	protected void updateDeviceValue(DBProcessor db, ProjectDeviceAttributeVO devAttr) 
	throws InvalidDataException, DatabaseException {
		StringBuilder sql = new StringBuilder(256);
		sql.append("delete from ").append(getCustomSchema()).append("ic_device_attribute_xr ");
		sql.append("where project_device_id =").append(StringUtil.checkVal(devAttr.getProjectDeviceId(), true));
		sql.append(" and device_attribute_id = ").append(StringUtil.checkVal(devAttr.getDeviceAttributeId(), true));
		db.executeSQLCommand(sql.toString());
		
		// Assign the values to the bean and update the table
		ProjectDeviceAttributeVO attr = new ProjectDeviceAttributeVO();
		attr.setProjectDeviceId(devAttr.getProjectDeviceId());
		attr.setDeviceAttributeId(devAttr.getDeviceAttributeId());
		attr.setValue(devAttr.getValue());
		db.insert(attr);
	}

}
