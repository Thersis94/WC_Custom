package com.irricurb.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.irricurb.action.data.vo.ProjectDeviceVO;
import com.irricurb.action.data.vo.ProjectDeviceVO.ProjectDeviceStatusCode;
import com.irricurb.action.project.ProjectSelectionAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;

import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: LookupAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Util action to manage data lookups for selects and other items in the irricurb
 *  project.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Dec 12, 2017
 * @updates:
 ****************************************************************************/
public class LookupAction extends SimpleActionAdapter {
	
    private static final Map<String, GenericVO> METHOD_MAP;
    
    /**
     * builds the static map so these methods can be used anywhere
     */
    static {
        Map<String, GenericVO> statMap = new HashMap<>();
        statMap.put("CUSTOMERS", new GenericVO("getProjectCustomers",null));
        statMap.put("PROJECTS", new GenericVO("getProjects",ActionRequest.class));
        statMap.put("DEVICE_TYPES", new GenericVO("getDeviceTypes",ActionRequest.class));
        statMap.put("PROJECT_ZONES", new GenericVO("getProjectZones",ActionRequest.class));
        statMap.put("DEVICE_STATUS", new GenericVO("getDeviceStatus",ActionRequest.class));
        METHOD_MAP = Collections.unmodifiableMap(statMap);
    }

	public LookupAction() {
		super();
	}
	

	public LookupAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("look up action retrieve called");
		String type = req.getStringParameter("type", "none");
		GenericVO gvo = METHOD_MAP.get(type);
		List<?> data = null;
		
		try {
			Method m = null;
			if (gvo.getValue() == null ){
				m = getClass().getMethod((String)gvo.getKey());
				data = (List<?>) m.invoke(this);
			}else{
				m = getClass().getMethod((String)gvo.getKey(), (Class<?>)gvo.getValue());
				//TODO figure out if there is away to get the objects needed dynamically or should we always just assume the req object
				data = (List<?>) m.invoke(this, req);
			}

		
		} catch (Exception e) {
			log.error("could not invoke the target method ",e);
		}
		
		if(data == null) data = new ArrayList<>();
				
		this.putModuleData(data, data.size(), false);
	}
	
	/**
	 * 
	 * @param req
	 * @return
	 */
	public List<GenericVO> getProjectCustomers() {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);

		StringBuilder sql = new StringBuilder(75);
		sql.append("select a.customer_id as key, customer_nm as value from ").append(schema).append("ic_customer a ");
		sql.append("inner join custom.ic_project b on a.customer_id = b.customer_id ");
		sql.append("order by customer_nm ");
		
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		List<GenericVO> data = dbp.executeSelect(sql.toString(), null, new GenericVO());

		return data;
	}
	
	/**
	 * 
	 * @param req
	 * @return
	 */
	public List<GenericVO> getProjects(ActionRequest req) {
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(75);
		sql.append("select project_id as key, project_nm as value from ").append(getCustomSchema()).append("ic_project ");
		
		if (req.hasParameter("customerId") && !req.getStringParameter("customerId").isEmpty()){
			sql.append("where customer_id = ? ");
			params.add(req.getStringParameter("customerId"));
		}
		
		log.debug("sql: " + sql.toString() + params);
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		return dbp.executeSelect(sql.toString(), params, new GenericVO());
	}
	
	/**
	 * Returns the list of device types
	 * @param req
	 * @return
	 */
	public List<GenericVO> getDeviceTypes(ActionRequest req) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select device_type_cd as key, type_nm as value from ").append(getCustomSchema()).append("ic_device_type ");
		sql.append("order by type_nm ");
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		return dbp.executeSelect(sql.toString(), null, new GenericVO());
	}
	
	/**
	 * returns the list of device status
	 * @param req
	 * @return
	 */
	public List<GenericVO> getDeviceStatus(ActionRequest req){
		List<GenericVO> data = new ArrayList<>();
		for (ProjectDeviceStatusCode status : ProjectDeviceVO.ProjectDeviceStatusCode.values()){
			data.add(new GenericVO(status.name(),status.getStatusName()));
		}
		return data;
	}
	/**
	 * Returns a list of nodes for the given project
	 * @param req
	 * @return
	 */
	public List<GenericVO> getProjectZones(ActionRequest req) {
		String projectId = (String)req.getSession().getAttribute(ProjectSelectionAction.PROJECT_LOOKUP);
		StringBuilder sql = new StringBuilder(128);
		sql.append("select project_zone_id as key, zone_nm as value from ").append(getCustomSchema()).append("ic_project_location a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ic_project_zone b on a.project_location_id = b.project_location_id ");
		sql.append(DBUtil.WHERE_CLAUSE).append("project_id = ? order by zone_nm");
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		return dbp.executeSelect(sql.toString(), Arrays.asList(new Object[] {projectId}), new GenericVO());
	}
}
