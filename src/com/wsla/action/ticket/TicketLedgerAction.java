package com.wsla.action.ticket;

// JDK 1.8.x
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.ticket.TicketLedgerVO;

/****************************************************************************
 * <b>Title</b>: TicketLedgerAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Action to manage the ledger for a ticket
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 15, 2018
 * @updates:
 ****************************************************************************/

public class TicketLedgerAction extends SBActionAdapter {

	public static final String AJAX_KEY = "ledger";
	
	/**
	 * 
	 */
	public TicketLedgerAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TicketLedgerAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * 
	 * @param dbConn
	 * @param attributes
	 */
	public TicketLedgerAction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		this.dbConn = dbConn;
		this.attributes = attributes;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		
		putModuleData(getLedgerForTicket(req.getParameter("ticketIdText")));
	}
	
	/**
	 * Loads the ledger entries for the desirec ticket id 
	 * @param ticketId
	 * @return
	 */
	public List<TicketLedgerVO> getLedgerForTicket(String ticketNumber) {
		
		StringBuilder sql = new StringBuilder(336);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("wsla_ticket t ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_ticket_ledger a on t.ticket_id = a.ticket_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_user c on a.disposition_by_id = c.user_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema());
		sql.append("wsla_ticket_status b on a.status_cd = b.status_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("role r on b.role_id = r.role_id ");
		sql.append("where t.ticket_no = ? order by a.create_dt desc");
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), Arrays.asList(ticketNumber), new TicketLedgerVO());
	}
}

