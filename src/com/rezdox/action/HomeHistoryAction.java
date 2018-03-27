package com.rezdox.action;

import java.util.ArrayList;
import java.util.List;

import com.rezdox.vo.ProjectVO;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;

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


	/*
	 * Slightly different query from the superclass - hued on the homeowner rather than the business.
	 * (non-Javadoc)
	 * @see com.rezdox.action.ProjectAction#loadProjectList(com.siliconmtn.action.ActionRequest, java.lang.String)
	 */
	@Override
	protected List<ProjectVO> loadProjectList(ActionRequest req, String projectId) {
		String schema = getCustomSchema();
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
		sql.append("where a.residence_view_flg=1 ");
		if (!StringUtil.isEmpty(projectId)) sql.append("and a.project_id=? ");
		sql.append("order by a.end_dt desc, a.project_nm");
		log.debug(sql);

		List<Object> params = new ArrayList<>();
		params.add(RezDoxUtils.getMemberId(req));
		if (!StringUtil.isEmpty(projectId)) params.add(projectId);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), params, new ProjectVO());
	}

}
