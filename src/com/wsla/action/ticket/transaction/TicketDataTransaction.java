package com.wsla.action.ticket.transaction;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

//SMT Base Lbs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.ticket.TicketAssignmentVO;
// WSLA Libs
import com.wsla.data.ticket.UserVO;
import com.wsla.data.ticket.TicketAssignmentVO.TypeCode;
import com.wsla.data.ticket.TicketDataVO;

/****************************************************************************
 * <b>Title</b>: TicketDataTransaction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Updates Miscellaneous data elelments in the ticket
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Nov 3, 2018
 * @updates:
 ****************************************************************************/

public class TicketDataTransaction extends SBActionAdapter {

	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "data";
	
	
	/**
	 * 
	 */
	public TicketDataTransaction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TicketDataTransaction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Overloaded constructor used for calling between actions.
	 * @param conn
	 * @param attrs
	 */
	public TicketDataTransaction(SMTDBConnection conn, Map<String, Object> attrs) {
		this();
		setAttributes(attrs);
		setDBConnection(conn);
	}
	
	/**
	 * 
	 * @param req
	 * @throws ActionException
	 * @throws com.siliconmtn.db.util.DatabaseException 
	 * @throws InvalidDataException 
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		UserVO user = ((UserVO)getAdminUser(req).getUserExtendedInfo());
		String ticketId = req.getParameter("ticketId");
		
		try {
			if (req.hasParameter("lockState")) {
				toggleTicketLock(ticketId, user.getUserId(), req.getBooleanParameter("lockState"));
			} else if (req.hasParameter("watchedState")) {
				toggleTicketWatch(ticketId, user.getUserId(), req.getBooleanParameter("watchedState"));
			} else if (req.hasParameter("saveData")) {
				
				String attr = req.getParameter("attr");
				String value = req.getParameter("value");
				boolean overwrite = req.getBooleanParameter("overwrite");
				saveDataAttribute(ticketId, attr, value, overwrite);
			}
		} catch (Exception e) {
			setModuleData(null, 0, e.getLocalizedMessage());
		}
	}
	
	/**
	 * SAve the data attribute for a ticket
	 * @param ticketId
	 * @param attr
	 * @param value
	 * @param overwrite
	 * @throws SQLException
	 */
	public void saveDataAttribute(String ticketId, String attr, String value, boolean overwrite) 
	throws SQLException {
		String id = null;
		String lId = null;
		
		// If we are overwriting the value, find the value and assign the ids
		if (overwrite) {
			StringBuilder sql = new StringBuilder(112);
			sql.append("select data_entry_id, ledger_entry_id from ");
			sql.append(getCustomSchema()).append("wsla_ticket_data ");
			sql.append("where ticket_id = ? and attribute_cd = ? ");
			try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
				ps.setString(1, ticketId);
				ps.setString(2, attr);
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					id = rs.getString(1);
					lId = rs.getString(2);
				}
			}
		}
		
		// Save the data 
		this.saveTicketData(id, lId, ticketId, attr, value);
	}
	
	/**
	 * 
	 * @param id
	 * @param lId
	 * @param ticketId
	 * @param attr
	 * @param value
	 * @throws SQLException
	 */
	private void saveTicketData(String id, String lId, String ticketId, String attr, String value) 
	throws SQLException {
		
		// Build a ticket data vo
		TicketDataVO tdvo = new TicketDataVO();
		tdvo.setDataEntryId(id);
		tdvo.setLedgerEntryId(lId);
		tdvo.setTicketId(ticketId);
		tdvo.setAttributeCode(attr);
		tdvo.setValue(value);
		
		// Save the data
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.save(tdvo);
		} catch(Exception e) {
			throw new SQLException(e);
		}
	}
	
	/**
	 * Toggles the user watching the ticket to on or off by adding or deleting the 
	 * Ticket assignment
	 * @param ticketId
	 * @param userId
	 * @param watchState
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 * @throws SQLException
	 */
	public void toggleTicketWatch(String ticketId, String userId, boolean watchState) 
	throws InvalidDataException, DatabaseException, SQLException {
		// Either add or delete the watcher assignment based upon the current state 
		if (watchState) {
			StringBuilder sql = new StringBuilder(112);
			sql.append("delete from ").append(getCustomSchema()).append("wsla_ticket_assignment ");
			sql.append("where user_id = ? and ticket_id = ? and assg_type_cd = 'WATCHER' ");
			try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
				ps.setString(1, userId);
				ps.setString(2, ticketId);
				ps.executeUpdate();
			}
			log.debug(sql.length() + "|" + sql + "|" + userId + "|" + ticketId);
		} else {
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			TicketAssignmentVO watcher = new TicketAssignmentVO();
			watcher.setTicketId(ticketId);
			watcher.setTypeCode(TypeCode.WATCHER);
			watcher.setUserId(userId);
			db.insert(watcher);
		}
	}
	
	/**
	 * Toggles the lock state on a ticket
	 * @param ticketId
	 * @param userId
	 * @param state
	 * @throws SQLException
	 */
	public void toggleTicketLock(String ticketId, String userId, boolean state) 
	throws SQLException {
		
		StringBuilder sql = new StringBuilder(88);
		sql.append("update ").append(getCustomSchema()).append("wsla_ticket ");
		sql.append("set locked_by_id = ?, locked_dt = ? where ticket_id = ?");
		log.debug(sql.length() + "|" + sql + "|" + ticketId + "|" + state);
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, state ? null : userId);
			ps.setTimestamp(2, state ? null : Convert.getCurrentTimestamp());
			ps.setString(3, ticketId);
			
			ps.executeUpdate();
		}
	}
}

