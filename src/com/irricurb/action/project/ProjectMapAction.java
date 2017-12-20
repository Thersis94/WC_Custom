package com.irricurb.action.project;

import java.util.Arrays;
import java.util.List;

import com.irricurb.action.data.vo.ProjectDeviceVO;
import com.irricurb.action.data.vo.ProjectLocationVO;
import com.irricurb.action.data.vo.ProjectZoneVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.smt.sitebuilder.action.SBActionAdapter;

/********************************************************************
 * <b>Title: </b>ProjectMapAction.java<br/>
 * <b>Description: </b>Manages Retrieving and updating data for the map view<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Dec 19, 2017
 * Last Updated: 
 *******************************************************************/
public class ProjectMapAction extends SBActionAdapter {

	public static final String MAP = "projectMap";
	
	/**
	 * 
	 */
	public ProjectMapAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public ProjectMapAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.info("Mapping");
		if (req.hasParameter("zoneId")) {
			putModuleData(getZoneDevices(req.getParameter("zoneId")));
		} else {
			putModuleData(getLocationData(req.getStringParameter("projectId", "PROJECT1")));
		}
	}
	
	public List<ProjectDeviceVO> getZoneDevices(String zoneId) {
		StringBuilder sql = new StringBuilder(128);
		return null;
	}
	
	/**
	 * Retrieves a list of 
	 * @param projectId
	 * @return
	 */
	public List<ProjectLocationVO> getLocationData(String projectId) {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		StringBuilder sql = new StringBuilder(128);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("ic_project_location ");
		sql.append("where project_id = ? ");
		List<ProjectLocationVO> projectLocations = db.executeSelect(sql.toString(), Arrays.asList(new Object[] {projectId}), new ProjectLocationVO());
		
		sql = new StringBuilder(256);
		sql.append("select b.*, c.* from ").append(getCustomSchema()).append("ic_project_location a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("ic_project_zone b ");
		sql.append("on a.project_location_id = b.project_location_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("ic_project_zone_geo c ");
		sql.append("on b.project_zone_id = c.project_zone_id ");
		sql.append("where a.project_location_id = ? ");
		sql.append("order by b.project_zone_id, order_no ");
		log.info(sql + "|" + projectId);
		
		// Loop the locations and add the zone data
		for (ProjectLocationVO location : projectLocations) {
			List<ProjectZoneVO> zones = db.executeSelect(sql.toString(), Arrays.asList(new Object[] {location.getProjectLocationId()}), new ProjectZoneVO());
			location.setZones(zones);
		}
		
		return projectLocations;
	}

}
