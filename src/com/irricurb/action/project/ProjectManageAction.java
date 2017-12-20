package com.irricurb.action.project;

import java.util.ArrayList;
import java.util.List;

import com.irricurb.action.data.vo.ProjectVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: ProjectManageAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> TODO Put Something Here
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Dec 8, 2017
 * @updates:
 ****************************************************************************/
public class ProjectManageAction extends SBActionAdapter {
	
	public static final String MANAGE = "manage";
	
	
	public ProjectManageAction() {
		super();
	}

	public ProjectManageAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req ) throws ActionException {
		log.debug("project manage action retrieve called");
		if (req.hasParameter(ProjectFacadeAction.WIDGET_ACTION) && MANAGE.equalsIgnoreCase(req.getParameter(ProjectFacadeAction.WIDGET_ACTION)) ){
			setModuleData(getProjects(req));
		}
	}

	/**
	 * @param req
	 * @return
	 */
	private GridDataVO<ProjectVO> getProjects(ActionRequest req) {
		
		List<Object> params = new ArrayList<>();
		DBProcessor dbp = new DBProcessor(getDBConnection());
		
		StringBuilder sql = new StringBuilder(90);
		
		sql.append(DBUtil.SELECT_FROM_STAR).append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("ic_project ");
		sql.append("where 1=1 ");
		
		return dbp.executeSQLWithCount(sql.toString(), params, new ProjectVO(), null, req.getIntegerParameter("limit"), req.getIntegerParameter("offset"));
	}
	
}
