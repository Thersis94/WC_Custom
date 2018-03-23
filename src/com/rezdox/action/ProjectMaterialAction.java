package com.rezdox.action;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.rezdox.vo.ProjectMaterialVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
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

	/**
	 * overloaded constructor to simplify calling actions
	 * @param dbConnection
	 * @param attributes
	 */
	public ProjectMaterialAction(SMTDBConnection dbConnection, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConnection);
		setAttributes(attributes);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		putModuleData(retrieveMaterials(req));
	}


	/**
	 * Retrieves a list of project materials tied to the given projectId.
	 * @param req
	 * @return
	 */
	public List<ProjectMaterialVO> retrieveMaterials(ActionRequest req) {
		String projectId = req.getParameter("projectId");
		//fail fast if we don't have a projectId to query against
		if (StringUtil.isEmpty(projectId)) return Collections.emptyList();

		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(300);
		sql.append("select a.project_material_id, a.material_nm, a.quantity_no, a.cost_no, ");
		sql.append("b.attribute_id, b.slug_txt, b.value_txt");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("REZDOX_PROJECT_MATERIAL a");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PROJECT_MATERIAL_ATTRIBUTE b ");
		sql.append("on a.project_material_id=b.project_material_id ");
		sql.append("where project_id=? ");
		sql.append("order by material_nm");
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), Arrays.asList(projectId), new ProjectMaterialVO());
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		throw new RuntimeException("not coded yet");
	}
}
