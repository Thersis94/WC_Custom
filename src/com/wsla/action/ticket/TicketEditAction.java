package com.wsla.action.ticket;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.wsla.data.product.ProductSerialNumberVO;
import com.wsla.data.product.ProductWarrantyVO;
import com.wsla.data.ticket.DiagnosticRunVO;
import com.wsla.data.ticket.TicketAssignmentVO;
import com.wsla.data.ticket.TicketAttributeVO;
import com.wsla.data.ticket.TicketCommentVO;
import com.wsla.data.ticket.TicketDataVO;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: TicketEditAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the data, workflow and security for the edit 
 * screen on the ticketing system
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 14, 2018
 * @updates:
 ****************************************************************************/

public class TicketEditAction extends SBActionAdapter {

	/**
	 * Key for the Facade / Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "editServiceOrder";
	
	/**
	 * key for the value of ticket id
	 */
	public static final String TICKET_ID = "ticketId";
	
	/**
	 * 
	 */
	public TicketEditAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TicketEditAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String ticketNumber = req.getParameter("ticketIdText");
		boolean json = req.getBooleanParameter("json");
		try {
			if (json && req.hasParameter("diagnostic")) {
				putModuleData(getDiagnostics(req.getParameter(TICKET_ID)));
			} else if (json && req.hasParameter("comment")) {
				putModuleData(getComments(req.getParameter(TICKET_ID)));
			} else if (json && req.hasParameter("assets")) {
				putModuleData(getExtendedData(req.getParameter(TICKET_ID), req.getParameter("groupCode")));
			} else {
				putModuleData(getCompleteTicket(ticketNumber));
			}
		} catch (SQLException | DatabaseException e) {
			log.error("Unable to retrieve ticket #: " + ticketNumber, e);
			this.putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * gets the comments and uses the tree class to order them
	 * @param ticketId
	 * @return
	 * @throws SQLException
	 */
	public List<Node> getComments(String ticketId) throws SQLException {
		
		StringBuilder sql = new StringBuilder(416);
		sql.append(DBUtil.SELECT_CLAUSE);
		sql.append(" (coalesce(num_roles, 0) = 0) as end_user, * from ");
		sql.append(getCustomSchema()).append("wsla_ticket_comment a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_user b ");
		sql.append("on a.user_id = b.user_id ");
		sql.append("left outer join ( ");
		sql.append("select user_id, count(*) as num_roles from ");
		sql.append(getCustomSchema()).append("wsla_user a ");
		sql.append(DBUtil.INNER_JOIN).append("profile_role b ");
		sql.append("on a.profile_id = b.profile_id ");
		sql.append("group by user_id ");
		sql.append(") as rc on b.user_id = rc.user_id ");
		
		sql.append("where ticket_id = ? order by priority_ticket_flg desc, a.create_dt desc ");
		
		List<Node> comments = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, ticketId);
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					TicketCommentVO tc = new TicketCommentVO(rs);
					tc.setUser(new UserVO(rs));
					Node n = new Node(tc.getTicketCommentId(), tc.getParentId());
					n.setUserObject(tc);
					
					comments.add(n);
				}
			}
		}
		
		// Order the nodes using the tree class
		Tree tree = new Tree(comments);
		return tree.preorderList();
	}
 	
	/**
	 * 
	 * @param ticketIdText
	 * @return
	 * @throws SQLException
	 */
	public TicketVO getCompleteTicket(String ticketIdText) throws SQLException, DatabaseException  {
		StringBuilder sql = new StringBuilder(256);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema());
		sql.append("wsla_ticket a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_user b on a.originator_user_id = b.user_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_provider c on a.oem_id = c.provider_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_ticket_status s on a.status_cd = s.status_cd ");
		sql.append(DBUtil.INNER_JOIN).append("role r on s.role_id = r.role_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema());
		sql.append("wsla_provider_location d on a.retailer_id = d.location_id ");
		sql.append("where ticket_no = ? ");
		
		// Gets the base ticket info
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<TicketVO> tickets = db.executeSelect(sql.toString(), Arrays.asList(ticketIdText), new TicketVO());
		if (tickets.isEmpty()) return new TicketVO();
		TicketVO ticket = tickets.get(0);
		
		// Get the user's profile
		ticket.getOriginator().setProfile(getProfile(ticket.getOriginator().getProfileId()));
		
		// Get the product info
		ticket.setProductSerial(getProductInfo(ticket.getProductSerialId()));
		
		// Get the warranty info
		ticket.setWarranty(getWarranty(ticket.getProductWarrantyId()));
		
		// Get the extended data
		//ticket.setTicketData(getExtendedData(ticket.getTicketId(), null));
		
		// Get the assignments
		ticket.setAssignments(getAssignments(ticket.getTicketId()));
		
		
		return ticket;
	}
	
	/**
	 * Gets the user's profile
	 * @param profileId
	 * @return
	 * @throws DatabaseException
	 */
	public UserDataVO getProfile(String profileId) throws DatabaseException {
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		return pm.getProfile(profileId, dbConn, ProfileManager.PROFILE_ID_LOOKUP, null);
	}
	
	/**
	 * Retrieves the warranty info for the ticket
	 * @param pwId
	 * @return
	 */
	public ProductWarrantyVO getWarranty(String pwId) {
		StringBuilder sql = new StringBuilder(256);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema());
		sql.append("wsla_product_warranty a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_warranty b ON a.warranty_id = b.warranty_id ");
		sql.append("where a.product_warranty_id = ? ");
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<ProductWarrantyVO> data = db.executeSelect(sql.toString(), Arrays.asList(pwId), new ProductWarrantyVO());
		return data.isEmpty() ? null : data.get(0);
	}
	
	/**
	 * Gets the ticket assignments
	 * @param ticketId
	 * @return
	 */
	public List<TicketAssignmentVO> getAssignments(String ticketId) {
		StringBuilder sql = new StringBuilder(256);
		
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema());
		sql.append("wsla_ticket_assignment a ");
		sql.append("where ticket_id = ? ");
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), Arrays.asList(ticketId), new TicketAssignmentVO());
	}
	
	/**
	 * Retrieves a list of extended data elements for the given ticket
	 * @param ticketId
	 * @return
	 */
	public List<TicketDataVO> getExtendedData(String ticketId, String groupCode) {

		StringBuilder sql = new StringBuilder(256);
		sql.append("select a.*, b.attribute_nm, c.group_nm, e.first_nm, e.last_nm, disposition_by_id from ");
		sql.append(getCustomSchema()).append("wsla_ticket_data a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_ticket_attribute b on a.attribute_cd = b.attribute_cd ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_attribute_group c ON c.attribute_group_cd = b.attribute_group_cd ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_ticket_ledger d on a.ledger_entry_id = d.ledger_entry_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_user e on d.disposition_by_id = e.user_id ");
		sql.append("where a.ticket_id = ? ");
		if (! StringUtil.isEmpty(groupCode)) sql.append("and b.attribute_group_cd = ? ");
		sql.append("order by b.attribute_group_cd ");
		
		List<TicketDataVO> data = new ArrayList<>();
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, ticketId);
			if (! StringUtil.isEmpty(groupCode)) ps.setString(2, groupCode);
			
			try(ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					TicketDataVO tdv = new TicketDataVO(rs);
					tdv.setAttribute(new TicketAttributeVO(rs));
					tdv.setLedger(new TicketLedgerVO(rs));
					data.add(tdv);
				}
			}
		} catch(SQLException e) {
			log.error("Unabel to get assets", e);
		}

		return data;
	}
	
	/**
	 * 
	 * @param ticketId
	 * @return
	 */
	public List<DiagnosticRunVO> getDiagnostics(String ticketId) {
		StringBuilder sql = new StringBuilder(256);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema());
		sql.append("wsla_diagnostic_run a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_user u on a.dispositioned_by_id = u.user_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_diagnostic_xr b on a.diagnostic_run_id = b.diagnostic_run_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_diagnostic c on b.diagnostic_cd = c.diagnostic_cd ");
		sql.append("where a.ticket_id = ? ");
		sql.append("order by a.create_dt desc ");
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), Arrays.asList(ticketId), new DiagnosticRunVO());
	}
	
	/**
	 * 
	 * @param id Product Serial ID 
	 * @return
	 */
	public ProductSerialNumberVO getProductInfo(String id) {
		StringBuilder sql = new StringBuilder(256);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema());
		sql.append("wsla_product_serial a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_product_master b on a.product_id = b.product_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_provider c on b.provider_id = c.provider_id ");
		sql.append("where product_serial_id = ? ");
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<ProductSerialNumberVO> data = db.executeSelect(sql.toString(), Arrays.asList(id), new ProductSerialNumberVO());
		
		return data.isEmpty() ? null : data.get(0);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		try {
			if (req.getBooleanParameter("isComment")) {
				addTicketComment(new TicketCommentVO(req));
			}
		} catch(Exception e) {
			log.error("Unable to perform action", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * 
	 * @param comment
	 * @throws SQLException
	 */
	public void addTicketComment(TicketCommentVO comment) throws SQLException {
		try {
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			db.save(comment);
		} catch(Exception e) {
			throw new SQLException("unable to save comment", e);
		}
	}
}

