package com.wsla.action.ticket;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.wsla.data.product.ProductSerialNumberVO;
import com.wsla.data.product.ProductWarrantyVO;
import com.wsla.data.ticket.DefectVO;
import com.wsla.data.ticket.DiagnosticRunVO;
import com.wsla.data.ticket.NextStepVO;
import com.wsla.data.ticket.TicketAssignmentVO;
import com.wsla.data.ticket.TicketAttributeVO;
import com.wsla.data.ticket.TicketCommentVO;
import com.wsla.data.ticket.TicketDataVO;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.TicketScheduleVO;
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
	 * key for the value of ticket schedule id
	 */
	public static final String REQ_TICKET_SCHEDULE_ID = "ticketScheduleId";
	
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
	
	/**
	 * 
	 * @param dbConn
	 * @param attributes
	 */
	public TicketEditAction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		this.dbConn = dbConn;
		this.attributes = attributes;
	}

	/**
	 * Overloaded constructor used for calling between actions.
	 * @param attrs
	 * @param conn
	 */
	public TicketEditAction(Map<String, Object> attrs, SMTDBConnection conn) {
		this();
		this.setAttributes(attrs);
		this.setDBConnection(conn);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		//if its the admintool do nothing at the moment
		if(req.hasParameter("manMod")) {return;}
		
		String ticketNumber = req.getParameter("ticketIdText");
		boolean json = req.getBooleanParameter("json");
		try {
			if (json && req.hasParameter("diagnostic")) {
				putModuleData(getDiagnostics(req.getParameter(TICKET_ID)));
			} else if (json && req.hasParameter("comment")) {
				boolean isActivity = req.getBooleanParameter("activity");
				putModuleData(getComments(req.getParameter(TICKET_ID), req.getBooleanParameter("isEndUser"), isActivity));

			} else if (json && req.hasParameter("schedule")) {
				putModuleData(getSchedule(req.getParameter(TICKET_ID), req.getParameter(REQ_TICKET_SCHEDULE_ID)));
			} else if (json && req.hasParameter("assets")) {
				boolean hasPublicAmid = false;
				if("wsla_user_portal".equalsIgnoreCase(req.getParameter("amid"))) {
					hasPublicAmid =true;
				}
				putModuleData(getExtendedData(req.getParameter(TICKET_ID), req.getParameter("groupCode"), hasPublicAmid));
			} else {
				TicketVO ticket = getCompleteTicket(ticketNumber);
				req.setAttribute("providerData", ticket.getOem());
				req.setAttribute("wsla.ticket.locale", ticket.getOriginator().getLocale());
				req.setAttribute("nextStep", new NextStepVO(ticket.getStatusCode()));
				putModuleData(ticket);
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
	public List<Node> getComments(String ticketId, boolean endUserFilter, boolean activity) throws SQLException {
		
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
		sql.append("where ticket_id = ? ");
		
		if (activity) sql.append("and activity_type_cd != 'COMMENT' ");
		else sql.append("and activity_type_cd = 'COMMENT' ");
		sql.append("order by priority_ticket_flg desc, a.create_dt desc ");
		log.debug(sql);
		List<Node> comments = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, ticketId);
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					TicketCommentVO tc = new TicketCommentVO(rs);
					tc.setUser(new UserVO(rs));
					comments.add(new Node(tc.getTicketCommentId(), tc.getParentId(), tc));
				}
			}
		}
		
		// Order the nodes using the tree class
		Tree tree = new Tree(comments);
		Node rootNode = tree.getRootNode();
		Set<Node> remNodes = new HashSet<>(comments.size());

		// If the display is for the user portal, filter out comments not submitted by 
		// an end user
		if (endUserFilter) {
			for (Node n : rootNode.getChildren()) {
				if (! ((TicketCommentVO)n.getUserObject()).isEndUser()) remNodes.add(n);
			}
			
			for (Node n : remNodes) rootNode.getChildren().remove(n);
			tree.setRootNode(rootNode);
		}
		
		return tree.preorderList();
	}
 	
	/**
	 * 
	 * @param ticketIdText
	 * @return
	 * @throws SQLException
	 */
	public TicketVO getCompleteTicket(String ticketIdText) throws SQLException, DatabaseException  {
		// Get the base ticket data
		TicketVO ticket = getBaseTicket(ticketIdText);
		
		// Get the user's profile
		ticket.getOriginator().setProfile(getProfile(ticket.getOriginator().getProfileId()));
		
		// Get the product info
		ticket.setProductSerial(getProductInfo(ticket.getProductSerialId()));
		
		// Get the warranty info
		ticket.setWarranty(getWarranty(ticket.getProductWarrantyId()));
		
		// Get the extended data
		ticket.setTicketData(getExtendedData(ticket.getTicketId(), null));
		
		// Get the assignments
		ticket.setAssignments(getAssignments(ticket.getTicketId()));
		
		// Get the schedule
		List<TicketScheduleVO> schedules = getSchedule(ticket.getTicketId(), null);
		ticket.addSchedules(schedules);
		populateScheduleAssignments(schedules, ticket.getAssignments());
		
		return ticket;
	}
	
	/**
	 * Gets the core ticket information
	 * @param ticketIdText
	 * @return
	 */
	public TicketVO getBaseTicket(String ticketIdText) {
		StringBuilder sql = new StringBuilder(256);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema());
		sql.append("wsla_ticket a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_user b on a.originator_user_id = b.user_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_provider c on a.oem_id = c.provider_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_ticket_status s on a.status_cd = s.status_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("role r on s.role_id = r.role_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema());
		sql.append("wsla_provider_location d on a.retailer_id = d.location_id ");
		sql.append(StringUtil.join("where ", UUIDGenerator.isUUID(ticketIdText) ? "a.ticket_id" : "ticket_no", " = ? "));
		
		// Gets the base ticket info
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<TicketVO> tickets = db.executeSelect(sql.toString(), Arrays.asList(ticketIdText), new TicketVO());
		if (tickets.isEmpty()) return new TicketVO();
		else return tickets.get(0);
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
	 * @throws DatabaseException 
	 */
	public List<TicketAssignmentVO> getAssignments(String ticketId) throws DatabaseException {
		StringBuilder sql = new StringBuilder(256);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema());
		sql.append("wsla_ticket_assignment a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema());
		sql.append("wsla_user b on a.user_id = b.user_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema());
		sql.append("wsla_provider_location c on a.location_id = c.location_id ");
		sql.append("where a.ticket_id = ? ");
		log.debug(sql);
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<TicketAssignmentVO> data = db.executeSelect(sql.toString(), Arrays.asList(ticketId), new TicketAssignmentVO());
		
		for (TicketAssignmentVO assignment : data) {
			if (!StringUtil.isEmpty(assignment.getUserId())) {
				assignment.getUser().setProfile(getProfile(assignment.getUser().getProfileId()));
			}
		}
		
		return data;
	}
	
	/**
	 *  Retrieves a list of extended data elements for the given ticket
	 * @param ticketId
	 * @param groupCode
	 * @return
	 */
	public List<TicketDataVO> getExtendedData(String ticketId, String groupCode) {
		return getExtendedData(ticketId, groupCode, false);
	}
	
	/**
	 * Retrieves a list of extended data elements for the given ticket
	 * @param ticketId
	 * @param b 
	 * @return
	 */
	public List<TicketDataVO> getExtendedData(String ticketId, String groupCode, boolean hasPublicAmid) {

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
		
		if (hasPublicAmid) {
			sql.append(" and ( b.attribute_cd = 'attr_proofPurchase' or b.attribute_cd = 'attr_serialNumberImage' ) ");
		}
		
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
					
					if ("attr_unitDefect".equals(tdv.getAttributeCode())) {
						tdv.setMetaValue(getDefectName(tdv.getValue()));
					}
					
					data.add(tdv);
				}
			}
		} catch(Exception e) {
			log.error("Unable to get assets", e);
		}

		return data;
	}
	
	/**
	 * Gets the label for the defect
	 * @param defectCode
	 * @return
	 * @throws InvalidDataException
	 * @throws com.siliconmtn.db.util.DatabaseException
	 */
	public String getDefectName(String defectCode) throws InvalidDataException, 
	com.siliconmtn.db.util.DatabaseException {
		if (StringUtil.isEmpty(defectCode)) return "";
		DefectVO dvo = new DefectVO();
		dvo.setDefectCode(defectCode);
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.getByPrimaryKey(dvo);
		return dvo.getDefectName();
	}
	
	/**
	 * Gets the schedule data for an entire ticket or an individual record
	 * 
	 * @param ticketId
	 * @param ticketScheduleId
	 * @return
	 */
	public List<TicketScheduleVO> getSchedule(String ticketId, String ticketScheduleId) {
		StringBuilder sql = new StringBuilder(256);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema());
		sql.append("wsla_ticket_schedule a ");
		
		List<Object> params = new ArrayList<>();
		if (!StringUtil.isEmpty(ticketId)) {
			sql.append("where a.ticket_id = ? ");
			params.add(ticketId);
		} else if (!StringUtil.isEmpty(ticketScheduleId)) {
			sql.append("where a.ticket_schedule_id = ? ");
			params.add(ticketScheduleId);
		}
		
		sql.append("order by a.create_dt ");
		
		log.debug(sql);
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), params, new TicketScheduleVO());
	}
	
	/**
	 * Adds assignment addresses/data from a ticket into the schedule data 
	 * 
	 * @param schedules
	 * @param assignments
	 */
	public void populateScheduleAssignments (List<TicketScheduleVO> schedules, List<TicketAssignmentVO> assignments) {
		for (TicketScheduleVO schedule : schedules) {
			for (TicketAssignmentVO assignment : assignments) {
				if (assignment.getTicketAssignmentId().equals(schedule.getCasLocationId())) {
					schedule.setCasLocation(assignment);
				} else if (assignment.getTicketAssignmentId().equals(schedule.getOwnerLocationId())) {
					schedule.setOwnerLocation(assignment);
				}
			}
		}
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
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		
		// Set the action to use the simple admin view
		ModuleVO module = (ModuleVO)attributes.get(AdminConstants.ADMIN_MODULE_DATA);
		module.setSimpleAction(true);
		super.list(req);

	}
}

