package com.rezdox.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerUtil;
import com.smt.sitebuilder.data.vo.FormFieldVO;
import com.smt.sitebuilder.data.vo.GenericQueryVO;
import com.smt.sitebuilder.data.vo.QueryParamVO;

import com.rezdox.data.ProjectFormProcessor;
import com.rezdox.vo.PhotoVO;
import com.rezdox.vo.ProjectVO;

/****************************************************************************
 * <b>Title:</b> ProjectAction.java<br/>
 * <b>Description:</b> Manages RezDox Projects.  See data model.
 * This is the action for a Business Owner's view of projects (aka Home History).
 * This action is invoked from the ProjectFacadeAction - not directly.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Mar 22, 2018
 ****************************************************************************/
public class ProjectAction extends SimpleActionAdapter {

	protected static final String REQ_PROJECT_ID = "projectId";


	public ProjectAction() {
		super();
	}

	public ProjectAction(ActionInitVO arg0) {
		super(arg0);
	}

	/**
	 * overloaded constructor to simplify calling actions
	 * @param dbConnection
	 * @param attributes
	 */
	public ProjectAction(SMTDBConnection dbConnection, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConnection);
		setAttributes(attributes);
	}


	/*
	 * Retrieves a list of projects visible to the logged-in business member
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String projectId = req.getParameter(REQ_PROJECT_ID);
		String page = req.getParameter("page");
		List<ProjectVO> data = loadProjectList(req, projectId);
		populateUserProfiles(data);

		mod.setActionData(data);
		setAttribute(Constants.MODULE_DATA, mod);

		//if we're looking at a single project, load additional pieces depending on which tab is being displayed
		if (!StringUtil.isEmpty(projectId)) {

			loadTabSpecifics(mod, req, page, projectId, data);

		} else {
			//calculate total valuation of all projects
			double total = 0;
			for (ProjectVO proj : data)
				total += proj.getTotalNo();
			mod.setAttribute("totalValue", total);
		}
	}


	/**
	 * Fork logic to load data needed for specific tab displays
	 * @param mod
	 * @param req
	 * @param page
	 * @param projectId
	 * @param data
	 * @throws ActionException
	 */
	protected void loadTabSpecifics(ModuleVO mod, ActionRequest req, String page, 
			String projectId, List<ProjectVO> data) throws ActionException {
		if ("edit".equals(page) || "view".equals(page)) {
			//load the form if we're in edit mode
			DataContainer dc = loadForm(req);
			mod.setAttribute("dataContainer", dc);

			if (data.size() == 1) {
				//move the form data over to the VO for transparency in View display
				ProjectVO vo = data.get(0);
				for (FormFieldVO ff: dc.getTransactions().get(projectId).getCustomData().values())
					vo.addAttribute(ff.getSlugTxt(), ff.getResponseText());
			}

		} else if ("materials".equals(page)) {
			//load the project materials
			loadMaterials(req);

		}
		//photos are used in two tabs
		if ("files".equals(page) || "view".equals(page)) {
			//load the photos/documents
			mod.setAttribute("photos", loadPhotos(req));
		}
	}


	/**
	 * load the list of projects to display - or one if we're in edit/view mode
	 * @param req
	 * @param projectId
	 * @return
	 */
	protected List<ProjectVO> loadProjectList(ActionRequest req, String projectId) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(1000);
		sql.append("select a.*, b.attribute_id, b.slug_txt, b.value_txt, c.category_nm, d.type_nm, ");
		sql.append("r.residence_nm, rr.room_nm, m.member_id, m.profile_id as homeowner_profile_id ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("REZDOX_PROJECT a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PROJECT_ATTRIBUTE b on a.project_id=b.project_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PROJECT_CATEGORY c on a.project_category_cd=c.project_category_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PROJECT_TYPE d on a.project_type_cd=d.project_type_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_RESIDENCE r on a.residence_id=r.residence_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_RESIDENCE_MEMBER_XR rm on r.residence_id=rm.residence_id and rm.status_flg=1 ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_MEMBER m on rm.member_id=m.member_id "); //this is the home owner
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_ROOM rr on r.residence_id=rr.residence_id ");
		sql.append("where a.business_id=? and a.business_view_flg=1 ");
		if (!StringUtil.isEmpty(projectId)) sql.append("and a.project_id=? ");
		sql.append("order by a.end_dt desc, a.project_nm");
		log.debug(sql);

		List<Object> params = new ArrayList<>();
		params.add(RezDoxUtils.getBusinessId(req));
		if (!StringUtil.isEmpty(projectId)) params.add(projectId);


		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), params, new ProjectVO());
	}


	/**
	 * populate the UserDataVO for each homeowner for the loaded projects
	 * @param data
	 */
	protected void populateUserProfiles(List<ProjectVO> projects) {
		ProfileManager pm = ProfileManagerFactory.getInstance(getAttributes());
		Map<String, UserDataVO> userMap = new HashMap<>(projects.size());
		UserDataVO user;
		for (ProjectVO vo : projects) {
			user = vo.getHomeowner();
			if (user != null && !StringUtil.isEmpty(user.getProfileId()))
				userMap.put(user.getProfileId(), user);
		}
		if (userMap.isEmpty()) return;


		List<UserDataVO> users = new ArrayList<>(userMap.values());
		try {
			pm.populateRecords(getDBConnection(), users);
		} catch (DatabaseException e) {
			log.error("could not load user profiles");
			return;
		}

		//turn the list into a Map for fast lookups
		userMap = users.stream().collect(Collectors.toMap(UserDataVO::getProfileId, Function.identity()));

		//marry the users back to their project
		for (ProjectVO vo : projects) {
			user = vo.getHomeowner();
			if (user == null || StringUtil.isEmpty(user.getProfileId())) continue;

			vo.setHomeowner(userMap.get(user.getProfileId()));
		}
	}


	/**
	 * @param req
	 * @return
	 */
	private DataContainer loadForm(ActionRequest req) {
		String formId = RezDoxUtils.getFormId(getAttributes());
		log.debug("Retrieving Project Form: " + formId);

		// Set the requried params
		QueryParamVO param = new QueryParamVO("PROJECT_ID", Boolean.FALSE);
		param.setValues(req.getParameterValues(REQ_PROJECT_ID));
		GenericQueryVO query = new GenericQueryVO(formId);
		query.addConditional(param);

		// Get the form and the saved data for re-display onto the form.
		DataManagerUtil util = new DataManagerUtil(getAttributes(), getDBConnection());
		return util.loadFormWithData(formId, req, query, ProjectFormProcessor.class);
	}


	/**
	 * load a list of photos tied to this treasure box item
	 * @param vo
	 * @param req
	 * @return 
	 */
	private List<PhotoVO> loadPhotos(ActionRequest req) {
		return new PhotoAction(getDBConnection(), getAttributes()).retrievePhotos(req);
	}


	/**
	 * load a list of photos tied to this treasure box item
	 * @param vo
	 * @param req
	 * @return 
	 * @throws ActionException 
	 */
	private void savePhoto(ActionRequest req) throws ActionException {
		new PhotoAction(getDBConnection(), getAttributes()).build(req);
	}


	/**
	 * load a list of materials tied to this project
	 * @param vo
	 * @param req
	 * @return 
	 * @throws ActionException 
	 */
	private void loadMaterials(ActionRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		mod.setAttribute(ModuleVO.ATTRIBUTE_1, mod.getAttribute(ModuleVO.ATTRIBUTE_2)); //transpose form2 into slot 1
		setAttribute(Constants.ACTION_DATA, getActionInit());
		new ProjectMaterialAction(getDBConnection(), getAttributes()).retrieve(req);
	}


	/**
	 * save materials tied to this project
	 * @param req
	 * @return 
	 * @throws ActionException 
	 */
	private void saveMaterial(ActionRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		mod.setAttribute(ModuleVO.ATTRIBUTE_1, mod.getAttribute(ModuleVO.ATTRIBUTE_2)); //transpose form2 into slot 1
		setAttribute(Constants.ACTION_DATA, getActionInit());
		new ProjectMaterialAction(getDBConnection(), getAttributes()).build(req);
	}


	/*
	 * Saves or creates a project tied to the given residenceId.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		boolean doRedirect = false;

		if (req.hasParameter("savePhoto")) {
			savePhoto(req);

		} else if (req.hasParameter("saveMaterial")) {
			saveMaterial(req);

		} else if (req.hasParameter("deleteItem")) {
			req.setParameter("isDelete", "1");
			save(req);
			doRedirect = true;

		} else {
			// Place ActionInit on the Attributes map for the Data Save Handler.
			setAttribute(Constants.ACTION_DATA, getActionInit());

			// Call DataManagerUtil to save the form.
			String formId = RezDoxUtils.getFormId(getAttributes());
			DataManagerUtil util = new DataManagerUtil(getAttributes(), getDBConnection());
			util.saveForm(formId, req, ProjectFormProcessor.class);
		}

		//redirect the user if the request wasn't made over ajax
		if (doRedirect) {
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			sendRedirect(page.getFullPath(), (String)getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE), req);
		}
	}


	/**
	 * Saves the Project record (only)
	 * @param req
	 * @throws ActionException 
	 */
	public void save(ActionRequest req) throws ActionException {
		ProjectVO vo = ProjectVO.instanceOf(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (req.hasParameter("isDelete")) {
				db.delete(vo);
			} else {
				db.save(vo);
				//transpose the primary key
				req.setParameter(REQ_PROJECT_ID, vo.getProjectId());
			}

		} catch (Exception e) {
			throw new ActionException("could not save project", e);
		}
	}
}