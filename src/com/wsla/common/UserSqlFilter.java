package com.wsla.common;

import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.EnumUtil;
import com.wsla.common.WSLAConstants.WSLARole;
import com.wsla.data.ticket.TicketAssignmentVO.TypeCode;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: UserSqlFilter.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Provides common sql filters for filtering queries based
 * on the user data & role.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Jan 10, 2019
 * @updates:
 ****************************************************************************/
public class UserSqlFilter {

	private WSLARole role;
	private String schema;
	private UserVO user;

	/**
	 * Constructor for creating user filters
	 * @param user
	 * @param roleId
	 * @param schema
	 */
	public UserSqlFilter(UserVO user, String roleId, String schema) {
		this.user = user;
		this.schema = schema;

		switch (roleId) {
			case "0":
				role = WSLARole.PUBLIC;
				break;
			case "10":
				role = WSLARole.REGISTERED;
				break;
			case "100":
				role = WSLARole.ADMIN;
				break;
			default:
				role = EnumUtil.safeValueOf(WSLARole.class, roleId, WSLARole.PUBLIC);
		}
	}


	/**
	 * Returns a filter for ticket based queries, according to the user's role.
	 * This is an inner join filter and must be placed with the other tables in the query.
	 * @param tableAlias - wsla_ticket table alias
	 * @param params
	 * @return
	 */
	public String getTicketFilter(String tableAlias, List<Object> params) {
		StringBuilder sql = new StringBuilder(200);
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_ticket_assignment taf on ");
		sql.append(tableAlias).append(".ticket_id = taf.ticket_id and taf.location_id = ? and taf.assg_type_cd = ? ");

		// Use a temporary params list, as they may or may not be used
		List<Object> filterParams = new ArrayList<>();
		filterParams.add(user != null ? user.getLocationId() : null);

		// Add parameters based on the role
		switch (role) {
			case WSLA_SERVICE_CENTER:
				filterParams.add(TypeCode.CAS.name());
				break;
			case WSLA_RETAILER:
				filterParams.add(TypeCode.RETAILER.name());
				break;
			case WSLA_OEM:
				filterParams.add(TypeCode.OEM.name());
				break;
			default:
				sql.setLength(0);
				filterParams.clear();
		}

		// Finalize the filter to be added to the query
		params.addAll(filterParams);
		return sql.toString();
	}
}