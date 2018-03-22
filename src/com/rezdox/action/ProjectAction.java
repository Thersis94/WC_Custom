package com.rezdox.action;

import java.util.ArrayList;
import java.util.List;

import com.rezdox.vo.ProjectVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
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

		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(400);
		sql.append("select a.*, "); //should this be honed?
		sql.append("b.attribute_id, b.slug_txt, b.value_txt, ");
		sql.append("c.category_nm, d.type_nm, r.residence_nm, rr.room_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("REZDOX_PROJECT a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PROJECT_ATTRIBUTE b on a.project_id=b.project_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PROJECT_CATEGORY c on a.project_category_cd=c.project_category_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PROJECT_TYPE d on a.project_type_cd=d.project_type_cd ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_RESIDENCE r on a.residence_id=r.residence_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_RESIDENCE_MEMBER_XR rm on r.residence_id=rm.residence_id and rm.status_flg=1 ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_ROOM rr on r.residence_id=rr.residence_id ");
		sql.append("where rm.member_id=? ");
		sql.append("order by cast(coalesce(a.update_dt, a.create_dt) as date) desc, a.project_nm");

		List<Object> params = new ArrayList<>();
		params.add(RezDoxUtils.getMemberId(req));

		List<Object> data = null;
		try {
			DBProcessor db = new DBProcessor(getDBConnection(), schema);
			data = db.executeSelect(sql.toString(), params, new ProjectVO());
		} catch (Exception e) {
			log.error("could not load projects for member", e);
		}
		putModuleData(data);
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