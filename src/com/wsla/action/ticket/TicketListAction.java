package com.wsla.action.ticket;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.orm.SQLTotalVO;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.wsla.data.product.ProductSerialNumberVO;

// WSLA Libs
import com.wsla.data.provider.ProviderLocationVO;
import com.wsla.data.provider.ProviderVO;
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
		
		// Build the sql for the main query
		StringBuilder sql = new StringBuilder(768);
		sql.append(DBUtil.SELECT_CLAUSE).append("a.ticket_id, ticket_no, provider_nm, ");
		sql.append("product_nm, status_nm, first_nm, last_nm, location_nm, ");
		sql.append("a.create_dt, email_address_txt, locked_by_id, a.product_serial_id, ");
		sql.append("serial_no_txt, oem_id ");
		
		// Build the select for the count query
		StringBuilder cSql = new StringBuilder(768);
		cSql.append(DBUtil.SELECT_CLAUSE).append("cast(count(*) as int) ");
		
		// Get the base query and append to the count and display select
		StringBuilder base = getBaseSql();
		cSql.append(base);
		sql.append(base);
		
		// Manage the search
		List<String> params = new ArrayList<>();
		if (bst.hasSearch()) {
			String search = getBSTSearch(bst, params);
			cSql.append(search);
			sql.append(search);
		}
		
		// Add the oemSearch
		String oemSearch = getOemFilter(req.getParameter("oemId"), params);
		sql.append(oemSearch);
		cSql.append(oemSearch);
		
		// Add the Status Code
		String statusCode = getStatusFilter(req.getParameter("statusCode"), req.getParameter("status"), params);
		sql.append(statusCode);
		cSql.append(statusCode);
		
		// Add the Status Code
		String roleFilter = getRoleFilter(req.getParameter("roleId"),params);
		sql.append(roleFilter);
		cSql.append(roleFilter);		
		
		// Add the limit and offset for the display query
		sql.append(bst.getSQLOrderBy("create_dt", "desc"));
		sql.append(" limit ").append(bst.getLimit()).append(" offset ").append(bst.getOffset());
		
		// Build the grid object and assign the number of rows total
		GridDataVO<TicketVO> grid = new GridDataVO<>();
		grid.setSqlTotal(getCount(cSql.toString(), params));
		grid.setRowData(getDataCollection(sql.toString(), params));
		return grid;
	}
	
	/**
	 * Adds a filter by assigned (which uses the status code role assignment)
	 * @param roleId
	 * @param params
	 * @return
	 */
	public String getRoleFilter(String roleId, List<String> params) {
		if (StringUtil.isEmpty(roleId)) return "";
		
		params.add(roleId);
		return "and d.role_id = ? ";
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
	public String getStatusFilter(String statusCode, String status, List<String> params) {
		if (StringUtil.isEmpty(statusCode) && StringUtil.isEmpty(status)) return "";
		
		if(StringUtil.isEmpty(status) || "CLOSED".equals(status)) {
			params.add("CLOSED".equals(status) ? status : statusCode);
			return "and a.status_cd = ? ";
		} else {
			return "and a.status_cd != 'CLOSED' ";
		}
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
		where.append("or lower(email_address_txt) like ? )"); 
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
	public StringBuilder getBaseSql() {
		StringBuilder base = new StringBuilder(768);
		base.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket a ");
		base.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_user e ");
		base.append("on a.originator_user_id = e.user_id ");
		base.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("wsla_product_serial b ");
		base.append("on a.product_serial_id = b.product_serial_id ");
		base.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("wsla_product_master c ");
		base.append("on b.product_id = c.product_id ");
		base.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("wsla_ticket_status d ");
		base.append("ON a.status_cd = d.status_cd ");
		base.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("wsla_provider f ");
		base.append("on a.oem_id = f.provider_id ");
		base.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("wsla_provider_location g ");
		base.append("on a.retailer_id = g.location_id ");
		base.append("where 1 = 1 ");
		
		return base;
	}
}
