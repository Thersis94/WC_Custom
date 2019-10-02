package com.wsla.action.ticket;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import com.wsla.data.ticket.RefundReplacementVO;
import com.wsla.data.ticket.ShipmentVO.ShipmentType;
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

	private static final String WHERE_TICKET_ID_EQ = "where a.ticket_id=? ";

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
		boolean isUserPortal = "wsla_user_portal".equalsIgnoreCase(req.getParameter("amid"));

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

				putModuleData(getExtendedData(req.getParameter(TICKET_ID), req.getParameter("groupCode"), isUserPortal));
			} else {
				if (StringUtil.isEmpty(ticketNumber)) {
					ticketNumber = getTicketNumberFromId(req.getParameter(TICKET_ID));
				}
				
				TicketVO ticket = getCompleteTicket(ticketNumber);
				req.setAttribute("providerData", ticket.getOem());
				req.setAttribute("wsla.ticket.locale", ticket.getOriginator().getLocale());
				req.setAttribute("nextStep", new NextStepVO(ticket.getStatusCode()));
				putModuleData(ticket);
			}
		} catch (DatabaseException e) {
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
	public List<Node> getComments(String ticketId, boolean endUserFilter, boolean activity) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(416);
		sql.append(DBUtil.SELECT_CLAUSE).append(" coalesce(end_user_flg, 0) as end_user_flg, * from ");
		sql.append(schema).append("wsla_ticket_comment a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_user b ");
		sql.append("on a.user_id = b.user_id ");
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
		} catch (SQLException sqle) {
			log.error("could not load comments", sqle);
			return Collections.emptyList();
		}

		// Order the nodes using the tree class
		Tree tree = new Tree(comments);
		Node rootNode = tree.getRootNode();
		Set<Node> remNodes = new HashSet<>(comments.size());

		// If the display is for the user portal, filter out comments not submitted by 
		// an end user
		if (endUserFilter) {
			for (Node n : rootNode.getChildren()) {
				TicketCommentVO tc = (TicketCommentVO)n.getUserObject();
				
				if (tc.getEndUserFlag() != 1 && StringUtil.isEmpty(tc.getParentId())) {
					if (tc.getUserShareFlag() < 1) remNodes.add(n);
				}
			}

			for (Node n : remNodes) {
				rootNode.getChildren().remove(n);
			}
			tree.setRootNode(rootNode);
		}

		return tree.preorderList();
	}

	/**
	 * 
	 * @param ticketIdText
	 * @return
	 * @throws DatabaseException
	 */
	public TicketVO getCompleteTicket(String ticketIdText) throws DatabaseException  {
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

		// get refund replacements 
		ticket.setRar(getRefundReplacement(ticket.getTicketId()));

		//load the comments used for the pdf of the service order
		ticket.setComments(getComments(ticket.getTicketId(), false, false));

		return ticket;
	}

	/**
	 * loads the refund replacement section of the ticket
	 * @param ticketId
	 * @return
	 */
	private RefundReplacementVO getRefundReplacement(String ticketId) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(93);
		sql.append("select pm.msrp_cost_no, rr.*,cm.*, s.*, dl.location_nm as to_location_nm, sl.location_nm as from_location_nm  from ").append(schema);
		sql.append("wsla_ticket_ref_rep rr ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_credit_memo cm on rr.ticket_ref_rep_id = cm.ticket_ref_rep_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_shipment s on rr.ticket_id = s.ticket_id and s.shipment_type_cd in (?,?) ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_provider_location dl on s.to_location_id = dl.location_id " );
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_provider_location sl on s.from_location_id =sl.location_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_ticket t on rr.ticket_id = t.ticket_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_serial ps on t.product_serial_id = ps.product_serial_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_master pm on ps.product_id = pm.product_id ");
		sql.append(DBUtil.WHERE_CLAUSE).append("rr.ticket_id = ? ");
		log.debug(sql);

		List<Object> params = new ArrayList<>();
		params.add(ShipmentType.UNIT_MOVEMENT.name());
		params.add(ShipmentType.REPLACEMENT_UNIT.name());
		params.add(ticketId);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		List<RefundReplacementVO> data = db.executeSelect(sql.toString(), params, new RefundReplacementVO());



		if(data != null && data.size() == 1) {
			log.debug("data " + data.get(0));
			return data.get(0);
		}else {
			return new RefundReplacementVO();
		}

	}

	/**
	 * Gets the core ticket information
	 * @param ticketIdText
	 * @return
	 */
	public TicketVO getBaseTicket(String ticketIdText) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(256);
		sql.append(DBUtil.SELECT_FROM_STAR).append(schema);
		sql.append("wsla_ticket a ");
		sql.append(DBUtil.INNER_JOIN).append(schema);
		sql.append("wsla_user b on a.originator_user_id = b.user_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema);
		sql.append("wsla_provider c on a.oem_id = c.provider_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema);
		sql.append("wsla_ticket_status s on a.status_cd = s.status_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("role r on s.role_id = r.role_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema);
		sql.append("wsla_provider_location d on a.retailer_id = d.location_id ");
		sql.append(StringUtil.join("where ", UUIDGenerator.isUUID(ticketIdText) ? "a.ticket_id" : "ticket_no", " = ? "));

		// Gets the base ticket info
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
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
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(256);
		sql.append(DBUtil.SELECT_FROM_STAR).append(schema);
		sql.append("wsla_product_warranty a ");
		sql.append(DBUtil.INNER_JOIN).append(schema);
		sql.append("wsla_warranty b ON a.warranty_id = b.warranty_id ");
		sql.append("where a.product_warranty_id = ? ");

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
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
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(256);
		sql.append(DBUtil.SELECT_FROM_STAR).append(schema);
		sql.append("wsla_ticket_assignment a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema);
		sql.append("wsla_user b on a.user_id = b.user_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema);
		sql.append("wsla_provider_location c on a.location_id = c.location_id ");
		sql.append(WHERE_TICKET_ID_EQ);
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
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
	public List<TicketDataVO> getExtendedData(String ticketId, String groupCode, boolean isUserPortal) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(512);
		sql.append("select a.*, b.attribute_nm, c.group_nm, e.first_nm, e.last_nm, disposition_by_id from ");
		sql.append(schema).append("wsla_ticket_data a ");
		sql.append(DBUtil.INNER_JOIN).append(schema);
		sql.append("wsla_ticket_attribute b on a.attribute_cd = b.attribute_cd ");
		sql.append(DBUtil.INNER_JOIN).append(schema);
		sql.append("wsla_attribute_group c ON c.attribute_group_cd = b.attribute_group_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema);
		sql.append("wsla_ticket_ledger d on a.ledger_entry_id = d.ledger_entry_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema);
		sql.append("wsla_user e on d.disposition_by_id = e.user_id ");
		sql.append(WHERE_TICKET_ID_EQ);
		if (! StringUtil.isEmpty(groupCode)) sql.append("and b.attribute_group_cd = ? ");

		if (isUserPortal) {
			sql.append(" and ( b.attribute_cd = 'attr_proofPurchase' or b.attribute_cd = 'attr_serialNumberImage' ) ");
		}

		sql.append("order by b.attribute_group_cd ");
		log.debug(sql.length() + "|" + sql + "|" + ticketId);
		List<TicketDataVO> data = new ArrayList<>();
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, ticketId);
			if (! StringUtil.isEmpty(groupCode)) ps.setString(2, groupCode);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				TicketDataVO tdv = new TicketDataVO(rs);
				tdv.setAttribute(new TicketAttributeVO(rs));
				tdv.setLedger(new TicketLedgerVO(rs));

				if ("attr_unitDefect".equals(tdv.getAttributeCode()) || "attr_unitRepairCode".equals(tdv.getAttributeCode())) {
					tdv.setMetaValue(getDefectName(tdv.getValue()));
				}

				data.add(tdv);
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
	public String getDefectName(String defectCode) {
		if (StringUtil.isEmpty(defectCode)) return "";
		DefectVO dvo = new DefectVO();
		dvo.setDefectCode(defectCode);

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.getByPrimaryKey(dvo);
		} catch (Exception e) {
			log.error("could not load defect from code " + defectCode, e);
		}
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
			sql.append(WHERE_TICKET_ID_EQ);
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
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(256);
		sql.append(DBUtil.SELECT_FROM_STAR).append(schema);
		sql.append("wsla_diagnostic_run a ");
		sql.append(DBUtil.INNER_JOIN).append(schema);
		sql.append("wsla_user u on a.dispositioned_by_id = u.user_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema);
		sql.append("wsla_diagnostic_xr b on a.diagnostic_run_id = b.diagnostic_run_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema);
		sql.append("wsla_diagnostic c on b.diagnostic_cd = c.diagnostic_cd ");
		sql.append(WHERE_TICKET_ID_EQ);
		sql.append("order by a.create_dt desc ");

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), Arrays.asList(ticketId), new DiagnosticRunVO());
	}

	/**
	 * 
	 * @param id Product Serial ID 
	 * @return
	 */
	public ProductSerialNumberVO getProductInfo(String id) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(256);
		sql.append(DBUtil.SELECT_FROM_STAR).append(schema);
		sql.append("wsla_product_serial a ");
		sql.append(DBUtil.INNER_JOIN).append(schema);
		sql.append("wsla_product_master b on a.product_id = b.product_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema);
		sql.append("wsla_provider c on b.provider_id = c.provider_id ");
		sql.append("where product_serial_id = ? ");

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
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
			log.info("Comment Saved: " + comment);
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
	
	/**
	 * Gets the ticket number from the ticketId
	 * @param ticketId
	 * @return
	 */
	public String getTicketNumberFromId(String ticketId) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select ticket_no from ").append(getCustomSchema()).append("wsla_ticket ");
		sql.append("where ticket_id = ?");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, ticketId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) return rs.getString(1);
			}
		} catch (Exception e) { /**  nothing to do **/}
		
		return null;
	}
}
