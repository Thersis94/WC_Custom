package com.wsla.action.ticket.transaction;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.SQLException;

//SMT Base Lbs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

// WSLA Libs
import com.wsla.data.ticket.UserVO;

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
			}
		} catch (Exception e) {
			setModuleData(null, 0, e.getLocalizedMessage());
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

