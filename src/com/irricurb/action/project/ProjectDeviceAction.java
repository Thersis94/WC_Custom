package com.irricurb.action.project;

import java.util.ArrayList;
import java.util.List;

import com.irricurb.action.data.vo.ProjectDeviceVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: ProjectDeviceAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> TODO Put Something Here
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Dec 13, 2017
 * @updates:
 ****************************************************************************/
public class ProjectDeviceAction extends SBActionAdapter {
	
	public static final String DEVICE = "device";
	
	public ProjectDeviceAction() {
		super();
	}

	public ProjectDeviceAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req ) throws ActionException {
		log.debug("project device action retrieve called");
		if (req.hasParameter(ProjectFacadeAction.WIDGET_ACTION) && DEVICE.equalsIgnoreCase(req.getParameter(ProjectFacadeAction.WIDGET_ACTION)) ){
			setModuleData(getProjectDevices(req));
		}
	}

	/**
	 * @param string
	 * @return
	 */
	private GridDataVO<ProjectDeviceVO> getProjectDevices(ActionRequest req) {
		String projectId = StringUtil.checkVal(req.getStringParameter(ProjectFacadeAction.PROJECT_ID));
		String customSchema = StringUtil.checkVal(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		List<Object> params = new ArrayList<>();
		DBProcessor dbp = new DBProcessor(getDBConnection());
		
		StringBuilder sql = new StringBuilder(90);
		
		sql.append(DBUtil.SELECT_FROM_STAR).append(customSchema).append("ic_project_device icpd ");
		sql.append("inner join ").append(customSchema).append("ic_device icd on icpd.device_id = icd.device_id ");
		sql.append("inner join ").append(customSchema).append("ic_project_zone icpz on icpd.project_zone_id = icpz.project_zone_id ");
		sql.append("where project_id = ? ");
		params.add(projectId);
		
		GridDataVO<ProjectDeviceVO> data = dbp.executeSQLWithCount(sql.toString(), params, new ProjectDeviceVO(), null, req.getIntegerParameter("limit"), req.getIntegerParameter("offset"));
		log.debug("### Data size " + data.getTotal() + " project id " + projectId);
		return data;
	}
}
