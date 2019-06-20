package com.rezdox.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rezdox.vo.DirectoryReportVO;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.DBUtil.SortDirection;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: DirectoryAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the member/business directory
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 1.0
 * @since Apr 10, 2018
 * <b>Changes:</b>
 ****************************************************************************/
public class DirectoryAction extends SimpleActionAdapter {
	// Maps the sort field values passed in to database fields
	private Map<String, String> sortFields;

	public DirectoryAction() {
		super();
		sortFields = new HashMap<>();
		sortFields.put("firstName", "first_nm");
		sortFields.put("cityName", "city_nm");
		sortFields.put("stateCode", "state_cd");
	}

	/**
	 * @param arg0
	 */
	public DirectoryAction(ActionInitVO arg0) {
		super(arg0);
	}

	/**
	 * @param dbConnection
	 * @param attributes
	 */
	public DirectoryAction(SMTDBConnection dbConnection, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConnection);
		setAttributes(attributes);
	}

	/**
	 * Gets the member/business directory
	 */
	protected void getDirectory(ActionRequest req) {
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(1500);
		SBUserRole role = (SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA);
		if (role == null) role = new SBUserRole(); 

		String memberId = null;

		// Member half of union
		sql.append(DBUtil.SELECT_FROM_STAR).append("(");
		memberId = RezDoxUtils.getMemberId(req);
		sql.append("select m.member_id as user_id, c.connection_id, m.first_nm, m.last_nm, m.profile_pic_pth, pa.city_nm, ");
		sql.append("pa.state_cd, '' as business_summary, cast(0 as numeric) as rating, 'MEMBER' as category_cd, '' as category_lvl2_cd, ");
		sql.append("m.privacy_flg, m.create_dt, m.profile_id || '_m' as unique_id, '' as my_pro_id, 'MEMBER' as type ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_member m ");
		sql.append(DBUtil.INNER_JOIN).append("profile_address pa on m.profile_id = pa.profile_id ");
		//only display people who are ACTIVE and are NOT business members (hybrid or residence, OK)
		sql.append(DBUtil.INNER_JOIN).append("profile_role pr on m.profile_id=pr.profile_id and pr.site_id=? and pr.role_id in (?,?) and pr.status_id=? ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_connection c on ((m.member_id = c.rcpt_member_id and c.sndr_member_id = ?) or (m.member_id = c.sndr_member_id and c.rcpt_member_id = ?)) and c.approved_flg >= 0 ");
		sql.append(DBUtil.WHERE_CLAUSE).append("m.member_id != ? ");
		params.add(RezDoxUtils.MAIN_SITE_ID);
		params.add(RezDoxUtils.REZDOX_RESIDENCE_ROLE);
		params.add(RezDoxUtils.REZDOX_RES_BUS_ROLE);
		params.add(SecurityController.STATUS_ACTIVE);
		params.add(memberId); //my prods
		params.addAll(Arrays.asList(memberId, memberId));

		sql.append(DBUtil.UNION_ALL);

		// Business half of union
		sql.append("select b.business_id as user_id, ");
		if (role.getRoleLevel() > 0) {
			sql.append("c.connection_id, ");
		} else {
			sql.append("'' as connection_id, ");
		}
		sql.append("b.business_nm as first_nm, '' as last_nm, b.photo_url as profile_pic_pth, b.city_nm, b.state_cd, ba.value_txt as business_summary, ");
		sql.append("cast(coalesce(r.rating, 0) as numeric) as rating, bc.business_category_cd as category_cd, bcs.business_category_cd as category_lvl2_cd, ");
		sql.append("b.privacy_flg, b.create_dt, b.business_id || '_b' as unique_id, mp.my_pro_id, 'BUSINESS' as type");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_business b ");
		sql.append(DBUtil.INNER_JOIN).append("(select business_id from ").append(schema).append("rezdox_business_member_xr where status_flg=1 group by business_id) bmxa on b.business_id = bmxa.business_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_business_attribute ba on b.business_id = ba.business_id and slug_txt='BUSINESS_SUMMARY' ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("(select business_id, avg(rating_no) as rating ").append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_member_business_review where parent_id is null group by business_id) as r on b.business_id = r.business_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_business_category_xr bcx on b.business_id=bcx.business_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_business_category bcs on bcx.business_category_cd=bcs.business_category_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_business_category bc on bcs.parent_cd=bc.business_category_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_my_pro mp on mp.business_category_cd=bc.business_category_cd and mp.business_id=b.business_id and mp.member_id=? ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_connection c on ((b.business_id = c.rcpt_business_id and c.sndr_member_id = ?) or (b.business_id = c.sndr_business_id and c.rcpt_member_id = ?)) and c.approved_flg >= 0 ");
		params.addAll(Arrays.asList(memberId, memberId, memberId));
		sql.append(") as user_directory ");

		// Generate the where clause
		generateSqlWhere(sql, req, params);

		// Create default ordering
		String order = "order by first_nm asc";
		// If sort/order parameters passed use them instead
		if (!StringUtil.isEmpty(req.getParameter(DBUtil.TABLE_SORT)) && !StringUtil.isEmpty(req.getParameter(DBUtil.TABLE_ORDER))) {
			String sortField = StringUtil.checkVal(sortFields.get(req.getParameter(DBUtil.TABLE_SORT)), "first_nm");
			SortDirection sortDirection = EnumUtil.safeValueOf(SortDirection.class, req.getParameter(DBUtil.TABLE_ORDER).toUpperCase(), SortDirection.ASC);
			order = StringUtil.join(DBUtil.ORDER_BY, sortField, " ", sortDirection.name());
		}
		sql.append(order);
		log.debug(sql + " | " + memberId);

		// Get the data
		DBProcessor dbp = new DBProcessor(dbConn, schema);
		List<DirectoryReportVO> data = dbp.executeSelect(sql.toString(), params, new DirectoryReportVO(), "unique_id");
		putModuleData(data, data.size(), false);
	}


	/**
	 * Creates the where clause for searching/filtering
	 * 
	 * @param sql
	 * @param req
	 * @param params
	 */
	private void generateSqlWhere(StringBuilder sql, ActionRequest req, List<Object> params) {
		sql.append(DBUtil.WHERE_1_CLAUSE);

		if (req.hasParameter(ConnectionAction.CATEGORY_SEARCH) && !"All".equalsIgnoreCase(StringUtil.checkVal(req.getParameter(ConnectionAction.CATEGORY_SEARCH)))) {
			sql.append("and category_cd = ? ");
			params.add(req.getParameter(ConnectionAction.CATEGORY_SEARCH));
		}

		if (req.hasParameter(ConnectionAction.STATE_SEARCH)) {
			sql.append("and state_cd = ? ");
			params.add(req.getParameter(ConnectionAction.STATE_SEARCH));
		}

		if (req.hasParameter(ConnectionAction.SEARCH)) {
			String search = StringUtil.checkVal(req.getParameter(ConnectionAction.SEARCH)).toLowerCase();
			sql.append("and (lower(first_nm) like ? or lower(last_nm) like ? ) ");
			params.add("%" + search + "%");
			params.add("%" + search + "%");
		}
	}
}
