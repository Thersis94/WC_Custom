package com.rezdox.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.rezdox.vo.PhotoVO;
import com.rezdox.vo.ProjectMaterialVO;
import com.rezdox.vo.ProjectVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> ProjectAction.java<br/>
 * <b>Description:</b> Manages RezDox Projects.  See data model.
 * This is the action for a Business Owner's view of projects...aka Home History.
 * This action is invoked from the ProjectFacadeAction - not directly.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Mar 22, 2018
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
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String projectId = req.getParameter("projectId");
		List<ProjectVO> data = loadProjectList(req, projectId);
		populateUserProfiles(data);

		//if we're looking at a single project, load additional pieces (similar to Inventory)
		if (!StringUtil.isEmpty(projectId) && data.size() == 1) {
			//load the photos/documents
			mod.setAttribute("photos", loadPhotos(req));

			//load the project materials
			mod.setAttribute("materials", loadMaterials(req));

		} else {
			//calculate total valuation of all projects
			double total = 0;
			for (ProjectVO proj : data)
				total += proj.getTotalNo();
			mod.setAttribute("totalValue", total);
		}

		mod.setActionData(data);
		setAttribute(Constants.MODULE_DATA, mod);
	}


	/**
	 * load the list of projects to display - or one if we're in edit/view mode
	 * @param req
	 * @param projectId
	 * @return
	 */
	private List<ProjectVO> loadProjectList(ActionRequest req, String projectId) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(1000);
		sql.append("select a.*, "); //should this be honed?
		sql.append("b.attribute_id, b.slug_txt, b.value_txt, ");
		sql.append("c.category_nm, d.type_nm, r.residence_nm, rr.room_nm, m.member_id, m.profile_id as homeowner_profile_id ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("REZDOX_PROJECT a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PROJECT_ATTRIBUTE b on a.project_id=b.project_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PROJECT_CATEGORY c on a.project_category_cd=c.project_category_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PROJECT_TYPE d on a.project_type_cd=d.project_type_cd ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_RESIDENCE r on a.residence_id=r.residence_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_RESIDENCE_MEMBER_XR rm on r.residence_id=rm.residence_id and rm.status_flg=1 ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_MEMBER m on rm.member_id=m.member_id "); //this is the home owner
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("PROFILE p on m.profile_id=p.profile_id "); //this is the home owner's profile
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_ROOM rr on r.residence_id=rr.residence_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_BUSINESS_MEMBER_XR bm on a.business_id=bm.business_id and bm.status_flg=1 ");
		sql.append("where bm.member_id=? and business_view_flg=1 ");
		if (!StringUtil.isEmpty(projectId)) sql.append("and a.project_id=? ");
		sql.append("order by a.end_dt desc, a.project_nm");
		log.debug(sql);

		List<Object> params = new ArrayList<>();
		params.add(RezDoxUtils.getMemberId(req));
		if (!StringUtil.isEmpty(projectId)) params.add(projectId);


		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), params, new ProjectVO());
	}


	/**
	 * populate the UserDataVO for each homeowner for the loaded projects
	 * @param data
	 */
	private void populateUserProfiles(List<ProjectVO> projects) {
		ProfileManager pm = ProfileManagerFactory.getInstance(getAttributes());
		List<UserDataVO> users = new ArrayList<>(projects.size());
		UserDataVO user;
		for (ProjectVO vo : projects) {
			user = vo.getHomeowner();
			if (user != null && !StringUtil.isEmpty(user.getProfileId()))
				users.add(user);
		}

		try {
			pm.populateRecords(getDBConnection(), users);
		} catch (DatabaseException e) {
			log.error("could not load user profiles");
			return;
		}

		//turn the list into a Map for fast lookups
		Map<String, UserDataVO> userMap = users.stream().collect(Collectors.toMap(UserDataVO::getProfileId, Function.identity()));

		//marry the users back to their project
		for (ProjectVO vo : projects) {
			user = vo.getHomeowner();
			if (user == null || StringUtil.isEmpty(user.getProfileId())) continue;

			vo.setHomeowner(userMap.get(user.getProfileId()));
		}
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
	 */
	private List<ProjectMaterialVO> loadMaterials(ActionRequest req) {
		return new ProjectMaterialAction(getDBConnection(), getAttributes()).retrieveMaterials(req);
	}


	/**
	 * save materials tied to this project
	 * @param req
	 * @return 
	 * @throws ActionException 
	 */
	private void saveMaterials(ActionRequest req) throws ActionException {
		new ProjectMaterialAction(getDBConnection(), getAttributes()).build(req);
	}


	/*
	 * Saves or creates a project tied to the given residenceId.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		//this calls to a save handler?

		//sendRedirect
	}
}