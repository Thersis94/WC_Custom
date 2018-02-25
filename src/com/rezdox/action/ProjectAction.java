package com.rezdox.action;

import java.util.ArrayList;
import java.util.List;

import com.rezdox.vo.ProjectVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title:</b> ProjectMaterialAction.java<br/>
 * <b>Description:</b> Manages RezDox Project Materials.  See data model.
 * This action is invoked from the ProjectFacadeAction - not directly.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Feb 25, 2018
 ****************************************************************************/
public class ProjectAction extends SimpleActionAdapter {

	public ProjectAction() {
		super();
	}

	public ProjectAction(ActionInitVO arg0) {
		super(arg0);
	}


	/*
	 * Retrieves a list of projects tied to the given residenceId.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String residenceId = req.getParameter("residenceId");
		//fail fast if we don't have a residenceId to query against
		if (StringUtil.isEmpty(residenceId)) return;

		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(300);
		sql.append("select a.*, "); //should this be honed?
		sql.append("b.attribute_id, b.slug_txt, b.value_txt");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("REZDOX_PROJECT a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PROJECT_ATTRIBUTE b ");
		sql.append("on a.project_id=b.project_id ");
		sql.append("where a.residence_id=? ");
		sql.append("order by cast(a.create_dt as date) desc, a.project_nm");

		List<Object> params = new ArrayList<>();
		params.add(residenceId);

		List<Object> data = null;
		try {
			DBProcessor db = new DBProcessor(getDBConnection(), schema);
			data = db.executeSelect(sql.toString(), params, ProjectVO.class);
		} catch (Exception e) {
			log.error("could not load projects for residenceId=" + residenceId, e);
		}
		putModuleData(data);
	}
}
