package com.rezdox.action;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.rezdox.vo.MyProVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <p><b>Title</b>: MyProsAction.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2018, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since May 30, 2018
 * <b>Changes:</b>
 ****************************************************************************/
public class MyProsAction extends SimpleActionAdapter {

	public MyProsAction() {
		super();
	}

	public MyProsAction(ActionInitVO arg0) {
		super(arg0);
	}

	/**
	 * @param dbConn
	 * @param attributes
	 */
	public MyProsAction(Connection dbConn, Map<String, Object> attributes) {
		this();
		this.setAttributes(attributes);
		this.setDBConnection((SMTDBConnection) dbConn);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SBUserRole role = (SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA);
		if (role == null || role.getRoleLevel() == 0) return;

		//cache this list - it won't change often enough to be rebuilding on every pageview
		if (!req.hasParameter("reloadPros") && req.getSession().getAttribute("MY_PROS") != null) return;

		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(250);
		sql.append("select a.my_pro_id, b.*, m.first_nm, m.last_nm, m.profile_pic_pth, bc.category_nm, bc.business_category_cd ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_my_pro a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_BUSINESS b on a.business_id=b.business_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_BUSINESS_MEMBER_XR mxr on b.business_id=mxr.business_id and mxr.status_flg=1 ");  //biz owner
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_MEMBER m on m.member_id=mxr.member_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_BUSINESS_CATEGORY bc on a.business_category_cd=bc.business_category_cd ");
		sql.append("where a.member_id=? ");
		sql.append("order by coalesce(a.update_dt, a.create_dt) desc");
		log.debug(sql);

		String memberId = RezDoxUtils.getMemberId(req);
		List<Object> params = Arrays.asList(memberId);

		//generate a list of VO's
		DBProcessor dbp = new DBProcessor(getDBConnection(), schema);
		req.getSession().setAttribute("MY_PROS", dbp.executeSelect(sql.toString(), params, new MyProVO()));
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		MyProVO vo = MyProVO.instanceOf(req);
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (req.hasParameter("isDelete")) {
				dbp.delete(vo);
			} else {
				//see if a record already exists we can update
				if (StringUtil.isEmpty(vo.getMyProId()))
					checkExisting(vo);
				dbp.save(vo);
			}
		} catch (Exception e) {
			log.error("could not save my pro", e);
		}
	}


	/**
	 * updates the VO with the pkId of the existing record, so we can update it rather than creating a duplicate or delete+recreate.
	 * @param vo
	 */
	private void checkExisting(MyProVO vo) {
		String sql = StringUtil.join("select my_pro_id from ", getCustomSchema(), 
				"rezdox_my_pro where member_id=? order by coalesce(update_dt, create_dt) desc  offset 5 limit 1");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, vo.getMemberId());
			ResultSet rs = ps.executeQuery();
			//if the user has 6 pros, update the incoming record so it replaces the oldest one (last in this RS).
			if (rs.next())
				vo.setMyProId(rs.getString(1));

		} catch (SQLException sqle) {
			log.error("could not find existing MyPro", sqle);
		}
	}
}
