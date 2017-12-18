package com.irricurb.action.project;

import java.util.ArrayList;
import java.util.List;

import com.irricurb.action.data.vo.ProjectDeviceVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
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
		if (req.hasParameter(ProjectFascadeAction.WIDGET_ACTION) && DEVICE.equalsIgnoreCase(req.getParameter(ProjectFascadeAction.WIDGET_ACTION)) ){
			setModuleData(getProjectDevices(req));
		}
	}

	/**
	 * @param string
	 * @return
	 */
	private GridDataVO<ProjectDeviceVO> getProjectDevices(ActionRequest req) {
		String projectId = StringUtil.checkVal(req.getStringParameter(ProjectFascadeAction.PROJECT_ID));
		List<Object> params = new ArrayList<>();
		DBProcessor dbp = new DBProcessor(getDBConnection());
		
		StringBuilder sql = new StringBuilder(90);
		
		sql.append(ProjectFascadeAction.SELECT_STAR).append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("ic_project_device ");
		sql.append("where project_id = ? ");
		params.add(projectId);
		
		return dbp.executeSQLWithCount(sql.toString(), params, new ProjectDeviceVO(), null, req.getIntegerParameter("limit"), req.getIntegerParameter("offset"));
	}
}
