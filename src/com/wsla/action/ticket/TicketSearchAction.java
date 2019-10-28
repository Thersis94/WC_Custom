package com.wsla.action.ticket;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: TicketSearchAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Performs a fuzzy lookup across the ticket by the 
 * following information:
 * <ul>
 * <li>Ticket No</li>
 * <li>First Name</li>
 * <li>Last Name</li>
 * <li>Email Address</li>
 * <li>Phone Number</li>
 * <li>Serial Number</li>
 * </ul>
 * This will be used in a Type ahead lookup with only 10 results returned
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 11, 2018
 * @updates:
 ****************************************************************************/
public class TicketSearchAction extends SBActionAdapter {

	/**
	 * 
	 */
	public TicketSearchAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TicketSearchAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	public TicketSearchAction(SMTDBConnection conn, Map<String, Object> attributes) {
		super();
		this.setDBConnection(conn);
		this.setAttributes(attributes);
	}
	
	/**
	 * Performs a search against multiple fields to find the desired ticket
	 * @param search
	 * @return
	 */
	public List<GenericVO> getTickets(String search) {
		if (StringUtil.isEmpty(search)) return new ArrayList<GenericVO>();
		
		// Build the sql
		StringBuilder sql = new StringBuilder(576);
		sql.append("select ticket_no as key, ticket_no || ' - ' || first_nm || ");
		sql.append("' ' || last_nm || ': ' || coalesce(serial_no_txt, 'NOSN') as value ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_ticket_assignment b  on a.ticket_id = b.ticket_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_user c on b.user_id = c.user_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema());
		sql.append("wsla_product_serial d on a.product_serial_id = d.product_serial_id ");
		sql.append("where ts_rank_cd(document_txt, to_tsquery('").append(search).append(":*')) > 0 ");
		sql.append("limit 10");
		log.debug(sql.length() + "|" + sql + "|" + search);
		
		// Execute and return the data
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), null, new GenericVO());
	}
}
