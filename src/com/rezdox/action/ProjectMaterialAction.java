package com.rezdox.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.rezdox.data.ProjectMaterialFormProcessor;
import com.rezdox.vo.ProjectMaterialVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerUtil;
import com.smt.sitebuilder.data.vo.GenericQueryVO;
import com.smt.sitebuilder.data.vo.QueryParamVO;

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

	protected static final String REQ_PROJ_MATERIAL_ID = "projectMaterialId";


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
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);

		if (req.hasParameter(REQ_PROJ_MATERIAL_ID)) {
			mod.setAttribute("dataContainer", loadForm(req));
		} 
		mod.setActionData(retrieveMaterials(req));

		setAttribute(Constants.MODULE_DATA, mod);
	}


	/**
	 * @param req
	 * @return
	 */
	private DataContainer loadForm(ActionRequest req) {
		String formId = RezDoxUtils.getFormId(getAttributes());
		log.debug("Retrieving Project Materials Form: " + formId);

		// Set the requried params
		QueryParamVO param = new QueryParamVO("PROJECT_MATERIAL_ID", Boolean.FALSE);
		param.setValues(req.getParameterValues(REQ_PROJ_MATERIAL_ID));
		GenericQueryVO query = new GenericQueryVO(formId);
		query.addConditional(param);

		// Get the form and the saved data for re-display onto the form.
		DataManagerUtil util = new DataManagerUtil(getAttributes(), getDBConnection());
		return util.loadFormWithData(formId, req, query, ProjectMaterialFormProcessor.class);
	}



	/**
	 * Retrieves a list of project materials tied to the given projectId.
	 * @param req
	 * @return
	 */
	public List<ProjectMaterialVO> retrieveMaterials(ActionRequest req) {
		String projectId = req.getParameter("projectId");
		String projectMaterialId = req.getParameter(REQ_PROJ_MATERIAL_ID);
		//fail fast if we don't have a projectId to query against
		if (StringUtil.isEmpty(projectId)) return Collections.emptyList();

		List<Object> params = new ArrayList<>();
		params.add(projectId);

		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(300);
		sql.append("select a.project_material_id, a.material_nm, a.quantity_no, a.cost_no, ");
		sql.append("b.attribute_id, b.slug_txt, b.value_txt");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("REZDOX_PROJECT_MATERIAL a");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PROJECT_MATERIAL_ATTRIBUTE b ");
		sql.append("on a.project_material_id=b.project_material_id ");
		sql.append("where a.project_id=? ");
		if (!StringUtil.isEmpty(projectMaterialId)) {
			sql.append("and a.project_material_id=? ");
			params.add(projectMaterialId);
		}
		sql.append("order by a.material_nm");
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), params, new ProjectMaterialVO());
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		boolean doRedirect = false;

		if (req.hasParameter("deleteItem")) {
			req.setParameter("isDelete", "1");
			save(req);
			doRedirect = true;

		} else {
			// Call DataManagerUtil to save the form.
			String formId = RezDoxUtils.getFormId(getAttributes());
			DataManagerUtil util = new DataManagerUtil(getAttributes(), getDBConnection());
			util.saveForm(formId, req, ProjectMaterialFormProcessor.class);
		}

		//redirect the user if the request wasn't made over ajax
		if (doRedirect) {
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			String url = StringUtil.join(page.getFullPath(), "?page=materials&projectId=", req.getParameter("projectId"));
			sendRedirect(url, (String)getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE), req);
		}
	}


	/**
	 * Saves the Project record (only)
	 * @param req
	 * @throws ActionException 
	 */
	public void save(ActionRequest req) throws ActionException {
		ProjectMaterialVO vo = ProjectMaterialVO.instanceOf(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (req.hasParameter("isDelete")) {
				db.delete(vo);
			} else {
				db.save(vo);
				//transpose the primary key
				req.setParameter(REQ_PROJ_MATERIAL_ID, vo.getProjectMaterialId());
			}

		} catch (Exception e) {
			throw new ActionException("could not save project material", e);
		}

	}
}
