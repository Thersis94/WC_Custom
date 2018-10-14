package com.wsla.action.ticket;

// JDK 1.8.x
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.product.ProductSerialNumberVO;
import com.wsla.data.product.ProductWarrantyVO;
import com.wsla.data.ticket.DiagnosticRunVO;
import com.wsla.data.ticket.TicketAssignmentVO;
import com.wsla.data.ticket.TicketDataVO;
import com.wsla.data.ticket.TicketVO;

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
		
		try {
			putModuleData(getCompleteTicket(ticketNumber));
		} catch (SQLException e) {
			log.error("Unable to retrieve ticket #: " + ticketNumber, e);
		}
		
	}
	
	/**
	 * 
	 * @param ticketIdText
	 * @return
	 * @throws SQLException
	 */
	public TicketVO getCompleteTicket(String ticketIdText) throws SQLException {
		StringBuilder sql = new StringBuilder(256);
		sql.append("select * from wsla_ticket a ");
		sql.append("inner join wsla_user b on a.originator_user_id = b.user_id ");
		sql.append("inner join wsla_provider c on a.oem_id = c.provider_id ");
		sql.append("inner join wsla_ticket_status s on a.status_cd = s.status_cd ");
		sql.append("left outer join wsla_provider_location d on a.retailer_id = d.location_id ");
		sql.append("where ticket_no = ? ");
		
		// Gets the base ticket info
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<TicketVO> tickets = db.executeSelect(sql.toString(), Arrays.asList(ticketIdText), new TicketVO());
		if (tickets.isEmpty()) return new TicketVO();
		TicketVO ticket = tickets.get(0);
		
		// Get the product info
		ticket.setProductSerial(getProductInfo(ticket.getProductSerialId()));
		
		// Get the warranty info
		ticket.setWarranty(getWarranty(ticket.getProductWarrantyId()));
		
		// Get the diagnostics
		ticket.setDiagnosticRun(getDiagnostics(ticket.getTicketId()));
		
		// Get the extended data
		ticket.setTicketData(getExtendedData(ticket.getTicketId()));
		
		// Get the assignments
		ticket.setAssignments(getAssignments(ticket.getTicketId()));
		
		
		return ticket;
	}
	
	/**
	 * Retrieves the warranty info for the ticket
	 * @param pwId
	 * @return
	 */
	public ProductWarrantyVO getWarranty(String pwId) {
		StringBuilder sql = new StringBuilder(256);
		sql.append("select * from wsla_product_warranty a ");
		sql.append("inner join wsla_warranty b ON a.warranty_id = b.warranty_id ");
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
		
		sql.append("select * from wsla_ticket_assignment a ");
		sql.append("where ticket_id = ? ");
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), Arrays.asList(ticketId), new TicketAssignmentVO());
	}
	
	/**
	 * Retrieves a list of extended data elements for the given ticket
	 * @param ticketId
	 * @return
	 */
	public List<TicketDataVO> getExtendedData(String ticketId) {
		StringBuilder sql = new StringBuilder(256);
		sql.append("select * from wsla_ticket_data a ");
		sql.append("inner join wsla_ticket_attribute b on a.attribute_cd = b.attribute_cd ");
		sql.append("inner join wsla_attribute_group c ON c.attribute_group_cd = b.attribute_group_cd ");
		sql.append("where a.ticket_id = ? ");
		sql.append("order by b.attribute_group_cd ");
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), Arrays.asList(ticketId), new TicketDataVO());
	}
	
	/**
	 * 
	 * @param ticketId
	 * @return
	 */
	public List<DiagnosticRunVO> getDiagnostics(String ticketId) {
		StringBuilder sql = new StringBuilder(256);
		sql.append("select * from wsla_diagnostic_run a ");
		sql.append("inner join wsla_diagnostic_xr b on a.diagnostic_run_id = b.diagnostic_run_id ");
		sql.append("inner join wsla_diagnostic c on b.diagnostic_cd = c.diagnostic_cd ");
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
		sql.append("select * from wsla_product_serial a ");
		sql.append("inner join wsla_product_master b on a.product_id = b.product_id ");
		sql.append("inner join wsla_provider c on b.provider_id = c.provider_id ");
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
		log.info("building");
	}
}

