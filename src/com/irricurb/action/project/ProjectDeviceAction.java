package com.irricurb.action.project;

// JDK 1.8
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// GSON 2.4
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// Irricurb Libs
import com.irricurb.action.data.vo.DeviceAttributeVO;
import com.irricurb.action.data.vo.ProjectDeviceAttributeVO;
import com.irricurb.action.data.vo.ProjectDeviceVO;
import com.irricurb.io.ProjectLocationExclusionStrategy;

// SMT Base Libs 3.2
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
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
	 * This constant is TEMPORARY for the demo.  This will be replaced
	 * at a later time when the full design has been completed
	 */
	public static final String CONTROLLER_URL = "http://test-frontend.oviattgreenhouse.com/api/gateways/gateway_test_001?token=3d15359b2872548acb89b7a2c0a0fe6f";
	
	/**
	 * Widget action for devices
	 */
	public static final String DEVICE = "device";
	
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

		if(DEVICE.equalsIgnoreCase(req.getParameter(ProjectFacadeAction.WIDGET_ACTION)) && req.hasParameter("projectDeviceId")){
			setModuleData(getProjectDeviceById(req));
		} else {
			String projectId = (String) req.getSession().getAttribute(ProjectSelectionAction.PROJECT_LOOKUP);
			setModuleData(getProjectDevices(req, projectId));
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		if (!req.hasParameter(ProjectFacadeAction.WIDGET_ACTION)) return;
		
		if(DEVICE.equalsIgnoreCase(req.getParameter(ProjectFacadeAction.WIDGET_ACTION)) && req.hasParameter("deviceAttributeXrId")){
			updateDeviceAtrributes(new ProjectDeviceAttributeVO(req));
		}
		
		
	}
	
	/**
	 * Updates the database and sends the data to the controller
	 * @param pdvo
	 */
	public void updateDeviceAtrributes(ProjectDeviceAttributeVO pdvo) {
		
		try {
			// Update the record in the database
			updateValueByAttrXRID(pdvo);
			log.debug(pdvo);
			// Send a request to the device so the change takes place in the real world device
			sendAtttributesController(pdvo);
			
		} catch (Exception e) {
			log.error("Unable to update device attrributes", e);
			this.putModuleData(pdvo, 1, false, e.getMessage(), true);
		}
		
	}
	
	/**
	 * Serializes the device attributes and sends the data to the controller
	 * @param pdvo
	 * @throws IOException
	 */
	public void sendAtttributesController(ProjectDeviceAttributeVO pdvo) throws IOException {
		// Serialize the object
		Gson g = new GsonBuilder().setExclusionStrategies(new ProjectLocationExclusionStrategy()).create();
		String json = g.toJson(pdvo);
		
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		byte[] res = conn.postDocument(CONTROLLER_URL, json.getBytes(), "application/json");
		
		if (res != null)
			log.info("Response: " + new String(res));
	}
	
	/**
	 * this method updates the 
	 * @param req
	 * @throws DatabaseException 
	 */
	public void updateValueByAttrXRID(ProjectDeviceAttributeVO pdvo) throws DatabaseException {
		DBProcessor db = new DBProcessor(dbConn);
		
		List<String> fields = new ArrayList<>();
		fields.add("value_txt");
		fields.add("device_attribute_xr_id");
		
		StringBuilder sql = new StringBuilder(125);
		sql.append("update ").append(getCustomSchema()).append("ic_device_attribute_xr set value_txt = ? ");
		sql.append("where device_attribute_xr_id = ? ");
		
		log.debug("sql " + sql.toString() + "|" + pdvo.getValue()+ "|" + pdvo.getDeviceAttributeXrId());
		db.executeSqlUpdate(sql.toString(), pdvo, fields);
	}

	/**
	 * this method will get a project devices attributes 
	 * @param req
	 */
	private List<ProjectDeviceAttributeVO> getProjectDeviceById(ActionRequest req) {
		DBProcessor dbp = new DBProcessor(getDBConnection());
		
		StringBuilder sql = new StringBuilder(400);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("ic_device_attribute a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ic_attribute_device b on a.device_attribute_id = b.device_attribute_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("ic_device_attribute_xr c ");
		sql.append("on b.attribute_device_id = c.attribute_device_id and project_device_id = ? ");
		sql.append(DBUtil.WHERE_CLAUSE).append(" device_id in ( ");
		sql.append("select device_id from ").append(getCustomSchema()).append("ic_project_device where project_device_id = ? ");
		sql.append(") order by a.attribute_nm ");
		log.debug(sql + "|" + StringUtil.checkVal(req.getStringParameter("projectDeviceId")));
		
		List<Object> params = new ArrayList<>();
		params.add(StringUtil.checkVal(req.getStringParameter("projectDeviceId")));
		params.add(StringUtil.checkVal(req.getStringParameter("projectDeviceId")));

		// Get the data and then assign the options
		List<ProjectDeviceAttributeVO> data = dbp.executeSelect(sql.toString(), params, new ProjectDeviceAttributeVO()); 
		for (ProjectDeviceAttributeVO attr : data) {
			getDeviceAttributeOptions(dbp, attr);
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
		return dbp.executeSQLWithCount(sql.toString(), params, new ProjectDeviceVO(), null, req.getIntegerParameter("limit"), req.getIntegerParameter("offset"));
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
