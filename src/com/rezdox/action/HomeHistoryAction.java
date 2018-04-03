package com.rezdox.action;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.rezdox.vo.BusinessVO;
import com.rezdox.vo.ProjectVO;
import com.rezdox.vo.ResidenceVO;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.ModuleVO;

/****************************************************************************
 * <p><b>Title</b>: HomeHistoryAction.java</p>
 * <p><b>Description:</b> Wraps ProjectAction in a homeowner-friendly way.  
 * The lookup and save queries here are slightly different; otherwise the workflow is generally the same.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2018, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Mar 27, 2018
 * <b>Changes:</b>
 ****************************************************************************/
public class HomeHistoryAction extends ProjectAction {

	public HomeHistoryAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public HomeHistoryAction(ActionInitVO arg0) {
		super(arg0);
	}


	/**
	 * The prefilter is the dropdown at the top of the list page.  For business users its a list of businesses (this action),
	 * for HomeHistory its a list of residences - for which this method is overwritten in HomeHistoryAction.
	 * @param req
	 * @param mod
	 * @override
	 */
	@Override
	protected void loadPrefilter(ActionRequest req, ModuleVO mod) {
		//load a list of residences.  If there's only one, then choose the 1st as the default if one wasn't provided.
		List<ResidenceVO> resList = loadResidenceList(req);
		if (!req.hasParameter(ResidenceAction.RESIDENCE_ID) && !resList.isEmpty())
			req.setParameter(ResidenceAction.RESIDENCE_ID, resList.get(0).getResidenceId());


		log.debug(String.format("loaded %d residences", resList.size()));
		mod.setAttribute(FILTER_DATA_LST, resList);
	}


	/**
	 * Load a list of Residences this member has access to.
	 * @param projData
	 * @param req
	 * @return
	 */
	private List<ResidenceVO> loadResidenceList(ActionRequest req) {
		String oldResId = req.getParameter(ResidenceAction.RESIDENCE_ID);
		if (!StringUtil.isEmpty(oldResId))
			req.setParameter(ResidenceAction.RESIDENCE_ID, "");

		ResidenceAction ra = new ResidenceAction(getDBConnection(), getAttributes());
		List<ResidenceVO> rezList = ra.retrieveResidences(req);

		if (!StringUtil.isEmpty(oldResId)) //put this back the way we found it
			req.setParameter(ResidenceAction.RESIDENCE_ID, oldResId);

		return rezList;
	}


	/*
	 * Slightly different query from the superclass - hued on the homeowner rather than the business.
	 * (non-Javadoc)
	 * @see com.rezdox.action.ProjectAction#loadProjectList(com.siliconmtn.action.ActionRequest, java.lang.String)
	 */
	@Override
	protected List<ProjectVO> loadProjectList(ActionRequest req, String projectId) {
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		params.add(RezDoxUtils.getMemberId(req));

		StringBuilder sql = new StringBuilder(1000);
		sql.append("select a.*, b.attribute_id, b.slug_txt, b.value_txt, c.category_nm, d.type_nm, ");
		sql.append("r.residence_nm, rr.room_nm, m.member_id, m.profile_id as homeowner_profile_id ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("REZDOX_PROJECT a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PROJECT_ATTRIBUTE b on a.project_id=b.project_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PROJECT_CATEGORY c on a.project_category_cd=c.project_category_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PROJECT_TYPE d on a.project_type_cd=d.project_type_cd ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_RESIDENCE r on a.residence_id=r.residence_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_RESIDENCE_MEMBER_XR rm on r.residence_id=rm.residence_id and rm.status_flg=1 ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_MEMBER m on rm.member_id=m.member_id and m.member_id=? "); //this is the home owner
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_ROOM rr on r.residence_id=rr.residence_id ");
		sql.append("where a.residence_view_flg != 0 "); // 1=approved, -1=pending

		if (!StringUtil.isEmpty(projectId)) {
			sql.append("and a.project_id=? ");
			params.add(projectId);
		} else { //list page - filter by residenceId like we do for Inventory
			sql.append("and a.residence_id=? ");
			params.add(req.getParameter(ResidenceAction.RESIDENCE_ID));
		}
		sql.append("order by a.end_dt desc, a.project_nm");
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), params, new ProjectVO());
	}


	@Override
	protected void populateUserProfiles(List<ProjectVO> projects) {
		//populate the owner profiles (which are always me, but that's okay)
		super.populateUserProfiles(projects);

		//also populate the business profiles
		populateBusinessProfiles(projects);
	}


	/**
	 * populate the BusienssVO for each business for the loaded projects
	 * @param data
	 */
	protected void populateBusinessProfiles(List<ProjectVO> projects) {
		Set<String> ids = new HashSet<>(projects.size());

		for (ProjectVO vo : projects) {
			if (!StringUtil.isEmpty(vo.getBusinessId()))
				ids.add(vo.getBusinessId());
		}
		if (ids.isEmpty()) return;


		BusinessAction ba = new BusinessAction(getDBConnection(), getAttributes());
		List<BusinessVO> data = ba.retrieveBusinesses(ids.toArray(new String[ids.size()]));

		//turn the list into a Map for fast lookups
		Map<String, BusinessVO> dataMap = data.stream().collect(Collectors.toMap(BusinessVO::getBusinessId, Function.identity()));

		//marry the users back to their project
		for (ProjectVO vo : projects) {
			if (StringUtil.isEmpty(vo.getBusinessId())) continue;
			vo.setBusiness(dataMap.get(vo.getBusinessId()));
		}
	}
}