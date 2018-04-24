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
		String memberId = RezDoxUtils.getMemberId(req);
		
		// Member half of union
		sql.append(DBUtil.SELECT_FROM_STAR);
		sql.append("(select m.member_id as user_id, c.connection_id, m.first_nm, m.last_nm, m.profile_pic_pth, pa.city_nm, ");
		sql.append("pa.state_cd, '' as business_summary, cast(0 as numeric) as rating, 'MEMBER' as category_cd, m.privacy_flg, m.create_dt, m.member_id || '_m' as unique_id ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_member m ");
		sql.append(DBUtil.INNER_JOIN).append("profile_address pa on m.profile_id = pa.profile_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_connection c on (m.member_id = c.rcpt_member_id and c.sndr_member_id = ?) or (m.member_id = c.sndr_member_id and c.rcpt_member_id = ?) ");
		sql.append(DBUtil.WHERE_CLAUSE).append("m.member_id != ? ");
		params.addAll(Arrays.asList(memberId, memberId, memberId));
		
		// Business half of union
		sql.append(DBUtil.UNION_ALL);
		sql.append("select b.business_id, c.connection_id, b.business_nm, '', b.photo_url, b.city_nm, b.state_cd, ba.value_txt, ");
		sql.append("cast(coalesce(r.rating, 0) as numeric), bc.business_category_cd, b.privacy_flg, b.create_dt, b.business_id || '_b' ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_business b ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_connection c on (b.business_id = c.rcpt_business_id and c.sndr_member_id = ?) or (b.business_id = c.sndr_business_id and c.rcpt_member_id = ?) ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_business_attribute ba on b.business_id = ba.business_id and slug_txt = 'BUSINESS_SUMMARY' ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("(select business_id, avg(rating_no) as rating ").append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_member_business_review group by business_id) as r on b.business_id = r.business_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_business_category_xr bcx on b.business_id = bcx.business_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_business_category bcs on bcx.business_category_cd = bcs.business_category_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_business_category bc on bcs.parent_cd = bc.business_category_cd) ");
		sql.append("as user_directory ");
		params.addAll(Arrays.asList(memberId, memberId));
		
		// Generate the where clause
		generateSqlWhere(sql, req, params);
		
		// Create default ordering
		String order = "order by first_nm asc ";
		
		// If sort/order parameters passed use them instead
		if (!StringUtil.isEmpty(req.getParameter(DBUtil.TABLE_SORT)) && !StringUtil.isEmpty(req.getParameter(DBUtil.TABLE_ORDER))) {
			String sortField = StringUtil.checkVal(sortFields.get(req.getParameter(DBUtil.TABLE_SORT)), "first_nm");
			SortDirection sortDirection = EnumUtil.safeValueOf(SortDirection.class, req.getParameter(DBUtil.TABLE_ORDER).toUpperCase(), SortDirection.ASC);
			order = StringUtil.join(DBUtil.ORDER_BY, sortField, " ", sortDirection.name());
		}
		sql.append(order);
		
		log.debug("Directory SQL: " + sql + " | " + memberId);
		
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
