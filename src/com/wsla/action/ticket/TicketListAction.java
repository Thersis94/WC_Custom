package com.wsla.action.ticket;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.orm.SQLTotalVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.wsla.common.UserSqlFilter;
import com.wsla.common.WSLAConstants.WSLARole;
import com.wsla.data.product.ProductSerialNumberVO;

// WSLA Libs
import com.wsla.data.provider.ProviderLocationVO;
import com.wsla.data.provider.ProviderVO;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.UserVO;


/****************************************************************************
 * <b>Title</b>: TicketListAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Complex set of filters and information to display on the 
 * service orders page.  This class manages that data
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 11, 2018
 * @updates:
 ****************************************************************************/

public class TicketListAction extends SimpleActionAdapter {

	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "listServiceOrder";

	/**
	 * 
	 */
	public TicketListAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public TicketListAction(ActionInitVO arg0) {
		super(arg0);
	}

	/**
	 * Overloaded constructor used for calling between actions.
	 * @param attrs
	 * @param conn
	 */
	public TicketListAction(Map<String, Object> attrs, SMTDBConnection conn) {
		this();
		setAttributes(attrs);
		setDBConnection(conn);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if(! req.hasParameter("json")) return;
		
		try {
			setModuleData(retrieveTickets(req));
		} catch (SQLException e) {
			log.error("Unable to retrieve service orders", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}

	/**
	 * Retrieves the list of tickets for grid page list
	 * @return
	 * @throws SQLException 
	 */
	public GridDataVO<TicketVO> retrieveTickets(ActionRequest req ) throws SQLException {
		BSTableControlVO bst = new BSTableControlVO(req, TicketVO.class);
		List<String> params = new ArrayList<>();
		
		// Build the sql for the main query
		StringBuilder sql = new StringBuilder(1600);
		sql.append(DBUtil.SELECT_CLAUSE).append("a.ticket_id, a.historical_flg, ticket_no, provider_nm, ");
		sql.append("product_nm, status_nm, a.status_cd, e.first_nm, e.last_nm, g.location_nm, ");
		sql.append("a.create_dt, e.email_address_txt, locked_by_id, a.product_serial_id, ");
		sql.append("serial_no_txt, oem_id, locked_dt, h.first_nm || ' ' || h.last_nm as locked_nm, ");
		sql.append("pl.location_nm as cas_nm, pl.address_txt || ', ' || pl.zip_cd || ' ' || pl.country_cd as cas_loc_nm ");
		
		// Build the select for the count query
		StringBuilder cSql = new StringBuilder(1600);
		cSql.append(DBUtil.SELECT_CLAUSE).append("cast(count(*) as int) ");

		// Get the base query and append to the count and display select
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		String roleId = ((SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA)).getRoleId();
		StringBuilder base = getBaseSql(req.getParameter("status", ""), params, user, roleId);
		cSql.append(base);
		sql.append(base);

		// Manage the search
		if (bst.hasSearch()) {
			String search = getBSTSearch(bst, params);
			cSql.append(search);
			sql.append(search);
		}
		
		// Add the standing search
		if (! StringUtil.isEmpty(req.getParameter("standingCode"))) {
			String scSearch = this.getStandingFilter(req.getParameter("standingCode"), params);
			sql.append(scSearch);
			cSql.append(scSearch);
		}

		// Add the oemSearch
		String oemSearch = getOemFilter(req.getParameter("oemId"), params);
		sql.append(oemSearch);
		cSql.append(oemSearch);

		// Add the Status Code
		List<String> statusCodes = new ArrayList<>();
		if (! StringUtil.isEmpty(req.getParameter("statusCode"))) 
			statusCodes = Arrays.asList(req.getParameter("statusCode").split("\\,"));
		String sfFilter = getStatusFilter(statusCodes, req.getParameter("status"), params);
		sql.append(sfFilter);
		cSql.append(sfFilter);

		// Add the Status Code
		String roleFilter = getRoleFilter(req.getParameter("roleId"), user.getUserId(),params);
		sql.append(roleFilter);
		cSql.append(roleFilter);		

		// Add the limit and offset for the display query
		sql.append(bst.getSQLOrderBy("create_dt", "desc"));
		sql.append(" limit ").append(bst.getLimit()).append(" offset ").append(bst.getOffset());
		log.debug(sql.length() + "|" + sql + "| " + params);

		// Build the grid object and assign the number of rows total
		GridDataVO<TicketVO> grid = new GridDataVO<>();
		grid.setSqlTotal(getCount(cSql.toString(), params));
		grid.setRowData(getDataCollection(sql.toString(), params));
		return grid;
	}

	/**
	 * Creates the standing filter
	 * @param sc
	 * @param params
	 * @return
	 */
	public String getStandingFilter(String sc, List<String> params) {
		String sql = "and standing_cd = ? ";
		params.add(sc);
		
		return sql;
	}
	
	/**
	 * Adds a filter by assigned (which uses the status code role assignment)
	 * @param roleId
	 * @param params
	 * @return
	 */
	public String getRoleFilter(String roleId, String userId, List<String> params) {
		if (StringUtil.isEmpty(roleId)) return "";
		String filter = null;
		
		if (WSLARole.WSLA_CALL_CENTER.getRoleId().equals(roleId)) {
			filter = "and i.disposition_by_id = ? ";
			params.add(userId);
		} else {
			params.add(roleId);
			filter = "and d.role_id = ? ";
		}
		
		return filter;
	}

	/**
	 * Adds the filter for the OEMs
	 * @param oemId
	 * @param params
	 * @return
	 */
	public String getOemFilter(String oemId, List<String> params) {
		if (StringUtil.isEmpty(oemId)) return "";

		params.add(oemId);
		return "and oem_id = ? ";
	}

	/**
	 * Adds the filters for the status code
	 * @param statusCode
	 * @param params
	 * @return
	 */
	public String getStatusFilter(List<String> statusCodes, String status, List<String> params) {
		if(!statusCodes.isEmpty() || "CLOSED".equals(status)) {
			params.addAll("CLOSED".equals(status) ? Arrays.asList(status) : statusCodes);
			int questions = statusCodes.size();
			
			if (questions == 0 && "CLOSED".equals(status)) {
				questions = 1;
			}
			return "and a.status_cd in ( " + DBUtil.preparedStatmentQuestion(questions) + ") ";
		} else if (!StringUtil.isEmpty(status) && !"ALL".equals(status) && !"CM_OPEN".equals(status)) {
			return "and a.status_cd != 'CLOSED' ";
		} 

		return "";
	}

	/**
	 * Formats a like query for the filter
	 * @param bst
	 * @param params
	 * @return
	 */
	public String getBSTSearch(BSTableControlVO bst, List<String> params) {
		StringBuilder where = new StringBuilder(64);
		where.append("and (lower(ticket_no) like ? or lower(product_nm) like ? ");
		where.append("or lower(e.email_address_txt) like ? )"); 
		where.append("or lower(e.first_nm || ' ' || e.last_nm) like ? ");
		params.add(bst.getLikeSearch().toLowerCase());
		params.add(bst.getLikeSearch().toLowerCase());
		params.add(bst.getLikeSearch().toLowerCase());
		params.add(bst.getLikeSearch().toLowerCase());

		return where.toString();
	}

	/**
	 * Executes the query and returns the rows
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	public List<TicketVO> getDataCollection(String sql, List<String> params) throws SQLException {
		List<TicketVO> tickets = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			// add the ps paramters for the search
			for (int i=0; i < params.size(); i++) ps.setString(i + 1, params.get(i));

			try (ResultSet rs = ps.executeQuery()) {
				while(rs.next()) {
					TicketVO ticket = new TicketVO(rs);
					ticket.setOriginator(new UserVO(rs));
					ticket.setOem(new ProviderVO(rs));
					ticket.setRetailer(new ProviderLocationVO(rs));
					ticket.setProductSerial(new ProductSerialNumberVO(rs));
					tickets.add(ticket);
				}
			}
		}

		return tickets;
	}

	/**
	 * Retrieves the number of matching rows in the provided query
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	public SQLTotalVO getCount(String sql, List<String> params) throws SQLException {
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			// add the ps paramters for the search
			for (int i=0; i < params.size(); i++) ps.setString(i + 1, params.get(i));

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return new SQLTotalVO(rs.getInt(1));
			}
		}

		return new SQLTotalVO(0);
	}

	/**
	 * Builds the base query and joins
	 * @return
	 */
	public StringBuilder getBaseSql(String status, List<String> params, UserVO user, String roleId) {
		StringBuilder base = new StringBuilder(768);
		base.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket a ");
		base.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_user e ");
		base.append("on a.originator_user_id = e.user_id ");
		base.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket_ledger i ");
		base.append("on a.ticket_id = i.ticket_id and i.status_cd = 'OPENED' ");
		base.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket_status d ");
		base.append("ON a.status_cd = d.status_cd ");
		base.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("wsla_product_serial b ");
		base.append("on a.product_serial_id = b.product_serial_id ");
		base.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("wsla_product_master c ");
		base.append("on b.product_id = c.product_id ");
		base.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("wsla_provider f ");
		base.append("on a.oem_id = f.provider_id ");
		base.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("wsla_provider_location g ");
		base.append("on a.retailer_id = g.location_id ");
		base.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("wsla_ticket_assignment ta ");
		base.append("on a.ticket_id = ta.ticket_id and ta.assg_type_cd = 'CAS' ");
		base.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("wsla_provider_location pl ");
		base.append("on ta.location_id = pl.location_id ");
		base.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("wsla_user h ");
		base.append("on a.locked_by_id = h.user_id ");
		
		// Join if searching only for tickets that need a comment reply or have an open credit memo
		if ("NEEDS_REPLY".equals(status)) {
			base.append(DBUtil.INNER_JOIN).append("(select ticket_id ");
			base.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket_comment ");
			base.append("where end_user_flg = 1 and (wsla_reply_flg = 0 or wsla_reply_flg is null) ");
			base.append("group by ticket_id) tc on a.ticket_id = tc.ticket_id ");
		} else if ("CM_OPEN".equals(status)) {
			base.append(DBUtil.INNER_JOIN).append("(select ticket_id ");
			base.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket_ref_rep rr ");
			base.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_credit_memo cm ");
			base.append("on rr.ticket_ref_rep_id = cm.ticket_ref_rep_id ");
			base.append("where cm.approval_dt is null) trr on a.ticket_id = trr.ticket_id ");
		} else if ("WSLA_CAS".equals(status)) {
			base.append(DBUtil.INNER_JOIN).append("(select ticket_id ");
			base.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket_assignment ta ");
			base.append("where assg_type_cd = 'CAS' and location_id in ( ");
			base.append("select location_id from ").append(getCustomSchema()).append("wsla_provider_location ");
			base.append("where provider_id = 'WSLA_ID')) tat on a.ticket_id = tat.ticket_id ");
		}
		
		// Add a filter to make sure users only see the tickets they are supposed to.
		// (i.e. CAS sees only tickets they are assigned to, etc.)
		UserSqlFilter filter = new UserSqlFilter(user, roleId, getCustomSchema());
		List<Object> filterParams = new ArrayList<>();
		base.append(filter.getTicketFilter("a", filterParams));
		filterParams.forEach(param -> params.add(param.toString()));
		
		base.append("where 1 = 1 ");

		return base;
	}

	/**
	 * added this layer for backwards compatibility
	 * @param status
	 * @return
	 */
	public  List<GenericVO> getTickets(StatusCode status) {
		return getTickets(status, null);
	}

	/**
	 * Return a <K,V> list of tickets, at the specific status level.  Called from SelectLookupAction for Logistics UI.
	 * @param req
	 * @return
	 */
	public List<GenericVO> getTickets(StatusCode status, String search) {

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(200);
		
		sql.append("select distinct ticket_id as key, ticket_no as value from ");
		sql.append(getCustomSchema()).append("wsla_ticket ");
		sql.append(DBUtil.WHERE_1_CLAUSE);
		if (status != null) {
			sql.append("and status_cd=? ");
			params.add(status.toString());
		}
		
		if (search != null) {
			sql.append("and lower(ticket_id) like ? or lower(ticket_no) like ? ");
			params.add("%"+search.toLowerCase()+"%");
			params.add("%"+search.toLowerCase()+"%");
		}
		
		sql.append("order by ticket_no");
		log.debug(sql);
		
		if (search != null) {
			sql.append(" limit 10 ");
		}

		// Execute and return
		List<GenericVO> data = db.executeSelect(sql.toString(), params, new GenericVO());
		
		if(data != null && ! data.isEmpty())log.debug("number of tickets found " + data.size());
		
		return data;
	}
}