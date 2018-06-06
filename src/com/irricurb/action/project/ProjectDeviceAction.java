package com.irricurb.action.project;

// JDK 1.8
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// GSON 2.4
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

// Irricurb Libs
import com.irricurb.action.data.vo.DeviceAttributeVO;
import com.irricurb.action.data.vo.ProjectDeviceAttributeVO;
import com.irricurb.action.data.vo.ProjectDeviceVO;
import com.irricurb.io.ProjectLocationExclusionStrategy;
import com.irricurb.util.ICConstants;

// SMT Base Libs 3.2
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs 3.3
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: ProjectDeviceAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages grabbing the device information (list or attribute)
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Dec 13, 2017
 * @updates:
 ****************************************************************************/
public class ProjectDeviceAction extends SBActionAdapter {

	/**
	 * Widget action for devices
	 */
	public static final String DEVICE = "device";
	private static final String PROJECT_DEVICE_ID = "projectDeviceId";
	/**
	 * 
	 */
	public ProjectDeviceAction() {
		super();
	}

	/**
	 * 
	 * @param arg0
	 */
	public ProjectDeviceAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req ) throws ActionException {
		if (!req.hasParameter(ProjectFacadeAction.WIDGET_ACTION)) return;

		if(DEVICE.equalsIgnoreCase(req.getParameter(ProjectFacadeAction.WIDGET_ACTION)) && req.hasParameter(PROJECT_DEVICE_ID)){
			setModuleData(getProjectDeviceById(req));
		} else {
			String projectId = (String) req.getSession().getAttribute(ProjectSelectionAction.PROJECT_LOOKUP);
			log.debug("Project ID: " + projectId);
			GridDataVO<ProjectDeviceVO>  data = getProjectDevices(req, projectId);
			log.debug(data);
			setModuleData(data);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		if (!req.hasParameter(ProjectFacadeAction.WIDGET_ACTION)) return;
		
		if(DEVICE.equalsIgnoreCase(req.getParameter(ProjectFacadeAction.WIDGET_ACTION)) && req.hasParameter("deviceAttributeId")){
			updateDeviceAtrributes(new ProjectDeviceAttributeVO(req));
		}
		
		
	}
	
	/**
	 * Updates the database and sends the data to the controller
	 * @param pdvo
	 */
	public void updateDeviceAtrributes(ProjectDeviceAttributeVO pdvo) {
		try {
			// Send a request to the device so the change takes place in the real world device
			sendAttributeController(getProjectDevice(pdvo));

			// Update the record in the database
			updateDeviceAttribute(pdvo);
			
		} catch (Exception e) {
			log.error("Unable to update device", e);
			// Update the value to its original state so the UI can be reset
			pdvo.setValue(getCurrentAttributeValue(pdvo.getDeviceAttributeXrId()));
			
			// Add pdvo and error response to the json response
			this.putModuleData(pdvo, 1, false, e.getMessage(), true);
		}
		
	}
	
	/**
	 * When a device update request fails, we need to reset the UI to it's original value
	 * @param deviceAttributeXrId
	 * @return
	 */
	protected String getCurrentAttributeValue(String deviceAttributeXrId) {
		try {
			DBProcessor db = new DBProcessor(getDBConnection());
			ProjectDeviceAttributeVO pdvo = new ProjectDeviceAttributeVO();
			pdvo.setDeviceAttributeXrId(deviceAttributeXrId);
			db.getByPrimaryKey(pdvo);
			
			if (! StringUtil.checkVal(pdvo.getValue()).isEmpty()) return pdvo.getValue();
		} catch(Exception e)  { /* Nothing to do here.  If no value, returns null */ }
		
		return null;
	}
	
	/**
	 * Serializes the device attributes and sends the data to the controller
	 * @param device
	 * @throws IOException
	 */
	public void sendAttributeController(ProjectDeviceVO device) throws IOException {
		// Serialize the object
		Gson g = new GsonBuilder().setExclusionStrategies(new ProjectLocationExclusionStrategy()).create();
		String json = g.toJson(device);
		String url = getControllerAddress(device.getProjectDeviceId());

		// Perform the request
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		
		try {
			url += "?type=COMMAND&json=" + URLEncoder.encode(json, "UTF-8");
			byte[] res = conn.retrieveData(url);
			
			// Check the response and make sure it was successfully implemented
			if (res == null) throw new IOException();
			g = new Gson();
			Type type = new TypeToken<Map<String, Object>>(){}.getType();
			Map<String, Object> resData = g.fromJson(new String(res), type);
			
			boolean success = Convert.formatBoolean(resData.get(GlobalConfig.SUCCESS_KEY));
			if (! success) throw new IOException();
			
		} catch(Exception e) {
			throw new IOException("Unable to communicate with the Project Controller",e);
		}
	}
	
	/**
	 * Retrieves the ip address of the controller (assumes the project location has a single controller)
	 * @param projectDeviceId
	 * @return
	 * @throws IOException
	 */
	public String getControllerAddress(String projectDeviceId) throws IOException {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String controllerPath = (String)getAttribute(ICConstants.CONTROLLER_COMMAND_SERVLET);
		
		StringBuilder sql = new StringBuilder(280);
		sql.append("select c.network_address_txt as key from ").append(schema).append("ic_project_device a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("ic_project_zone b ");
		sql.append("on a.project_zone_id = b.project_zone_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("ic_project_location c ");
		sql.append("on b.project_location_id = c.project_location_id ");
		sql.append("where project_device_id = ?");
		
		DBProcessor db = new DBProcessor(getDBConnection());
		List<GenericVO> items = db.executeSelect(sql.toString(), Arrays.asList(projectDeviceId), new GenericVO());
		if (! items.isEmpty()) return items.get(0).getKey() + controllerPath;
		
		throw new IOException("Unable to locate Controller Network Information");
	}
	
	/**
	 * this method updates the 
	 * @param req
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	public void updateDeviceAttribute(ProjectDeviceAttributeVO pdvo) throws Exception {
		// Get the DB Processor
		DBProcessor db = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		
		// Check and make sure the xr_id doesn't exist. This is an edge case when there is no data in the 
		// Status table and the item is updated 2 times in a row on the UI
		StringBuilder sql = new StringBuilder(64);
		sql.append("select device_attribute_xr_id from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("ic_device_attribute_xr where project_device_id = ? and device_attribute_id = ?");
		List<Object> params =  Arrays.asList(pdvo.getProjectDeviceId(), pdvo.getDeviceAttributeId());
		List<ProjectDeviceAttributeVO> items = db.executeSelect(sql.toString(),params, new ProjectDeviceAttributeVO());
		if (! items.isEmpty()) pdvo.setDeviceAttributeXrId(items.get(0).getDeviceAttributeXrId());
		db.save(pdvo);
	}
	
	/**
	 * Gets a Project Device for the controller
	 * @param pdavo
	 * @return
	 */
	private ProjectDeviceVO getProjectDevice(ProjectDeviceAttributeVO pdavo) {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		ProjectDeviceVO device = null;
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from ").append(schema).append("ic_project_device a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("ic_device b on a.device_id = b.device_id ");
		sql.append("where a.project_device_id = ?");
		
		DBProcessor db = new DBProcessor(getDBConnection());
		List<ProjectDeviceVO> items = db.executeSelect(sql.toString(), Arrays.asList(pdavo.getProjectDeviceId()), new ProjectDeviceVO());
		if (!items.isEmpty()) {
			device = items.get(0);
			device.addAttribute(pdavo);
		}
		
		return device;
	}

	/**
	 * this method will get a project devices attributes 
	 * @param req
	 */
	private List<ProjectDeviceAttributeVO> getProjectDeviceById(ActionRequest req) {
		DBProcessor dbp = new DBProcessor(getDBConnection());
		String projectDeviceId = req.getStringParameter(PROJECT_DEVICE_ID);
		
		StringBuilder sql = new StringBuilder(400);
		sql.append("select icd.*, display_type_cd, unit_txt,b.*, device_attribute_xr_id, value_txt from ").append(getCustomSchema()).append("ic_device icd ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ic_attribute_device a on icd.device_id = a.device_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ic_device_attribute b on a.device_attribute_id = b.device_attribute_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("ic_device_attribute_xr c ");
		sql.append("on b.device_attribute_id = c.device_attribute_id and project_device_id = ? ");
		sql.append(DBUtil.WHERE_CLAUSE).append(" a.device_id in ( ");
		sql.append("select device_id from ").append(getCustomSchema()).append("ic_project_device where project_device_id = ? ");
		sql.append(") order by b.attribute_nm ");
		log.debug(sql + "|" + StringUtil.checkVal(req.getStringParameter(PROJECT_DEVICE_ID)));
		
		List<Object> params = new ArrayList<>();
		params.add(projectDeviceId);
		params.add(projectDeviceId);

		// Get the data and then assign the options
		List<ProjectDeviceAttributeVO> data = dbp.executeSelect(sql.toString(), params, new ProjectDeviceAttributeVO(), "device_attribute_id"); 
		for (ProjectDeviceAttributeVO attr : data) {
			// Add the options (if needed) to the return so the ui can be built
			getDeviceAttributeOptions(dbp, attr);
			
			// If there is no entry for this attribute (new device), need to add this value
			if (StringUtil.checkVal(attr.getProjectDeviceId()).isEmpty()) attr.setProjectDeviceId(projectDeviceId);
		}
		
		return data;
	}
	
	/**
	 * Retrieves the attribute options for a given attribute
	 * @param dbp
	 * @param attr
	 */
	protected void getDeviceAttributeOptions(DBProcessor dbp, ProjectDeviceAttributeVO attr) {
		StringBuilder sql = new StringBuilder(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("ic_device_attribute ");
		sql.append("where parent_id = ?");
		
		List<Object> params = new ArrayList<>();
		params.add(attr.getDeviceAttributeId());
		
		attr.setOptions(dbp.executeSelect(sql.toString(), params, new DeviceAttributeVO())); 
	}

	/**
	 * @param string
	 * @return
	 */
	private GridDataVO<ProjectDeviceVO> getProjectDevices(ActionRequest req, String projectId) {
		List<Object> params = new ArrayList<>();
		DBProcessor dbp = new DBProcessor(getDBConnection());
		
		StringBuilder sql = new StringBuilder(90);
		
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("ic_project_device icpd ");
		sql.append("inner join ").append(getCustomSchema()).append("ic_device icd on icpd.device_id = icd.device_id ");
		sql.append("inner join ").append(getCustomSchema()).append("ic_project_zone icpz on icpd.project_zone_id = icpz.project_zone_id ");
		sql.append("where project_id = ? ");
		params.add(projectId);
		
		// Add the filtering
		applyFilters(req, sql, params);
		log.debug(sql + ":" + params);
		
		// Return the data
		return dbp.executeSQLWithCount(sql.toString(), params, new ProjectDeviceVO(), null, req.getIntegerParameter("limit", 10), req.getIntegerParameter("offset", 0));
	}
	
	/**
	 * Adds the sql filters for the filter selections on the bootstrap table
	 * @param req
	 * @param sql
	 * @param params
	 */
	protected void applyFilters(ActionRequest req, StringBuilder sql, List<Object> params) {
		// Add a filter for the zone
		if (req.hasParameter("projectZone")) {
			sql.append("and icpz.project_zone_id = ? ");
			params.add(req.getParameter("projectZone"));
		}
		
		// Add a filter for the device type
		if (req.hasParameter("deviceType")) {
			sql.append("and icd.device_type_cd = ? ");
			params.add(req.getParameter("deviceType"));
		}
		
		// Add a filter for the project status
		if (req.hasParameter("projectStatus")) {
			sql.append("and icpd.status_cd = ? ");
			params.add(req.getParameter("projectStatus"));
		}
	}
}
