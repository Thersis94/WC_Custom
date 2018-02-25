package com.rezdox.action;

import java.util.ArrayList;
import java.util.List;

import com.rezdox.vo.ProjectMaterialVO;
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
public class ProjectMaterialAction extends SimpleActionAdapter {

	public ProjectMaterialAction() {
		super();
	}

	public ProjectMaterialAction(ActionInitVO arg0) {
		super(arg0);
	}


	/*
	 * Retrieves a list of project materials tied to the given projectId.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String projectId = req.getParameter("projectId");
		//fail fast if we don't have a projectId to query against
		if (StringUtil.isEmpty(projectId)) return;

		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(300);
		sql.append("select a.project_material_id, a.material_nm, a.quantity_no, a.cost_no, ");
		sql.append("b.attribute_id, b.slug_txt, b.value_txt");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("REZDOX_PROJECT_MATERIAL a");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PROJECT_MATERIAL_ATTRIBUTE b ");
		sql.append("on a.project_material_id=b.project_material_id ");
		sql.append("where project_id=? ");
		sql.append("order by material_nm");

		List<Object> params = new ArrayList<>();
		params.add(projectId);

		List<Object> data = null;
		try {
			DBProcessor db = new DBProcessor(getDBConnection(), schema);
			data = db.executeSelect(sql.toString(), params, ProjectMaterialVO.class);
		} catch (Exception e) {
			log.error("could not load project materials for projectId=" + projectId, e);
		}
		putModuleData(data);
	}
}
