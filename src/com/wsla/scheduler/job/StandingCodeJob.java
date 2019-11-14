package com.wsla.scheduler.job;

//JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// Quartz 2.2.3
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

// SMT Base Libs
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.DatabaseNote;
import com.siliconmtn.db.DatabaseNote.DBType;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.util.Convert;
import com.siliconmtn.workflow.SMTWorkflowQueueHandler;
// WC Libs
import com.smt.sitebuilder.scheduler.AbstractSMTJob;
// WC Custom
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.TicketVO.Standing;
import com.wsla.data.ticket.TicketVO.UnitLocation;

/****************************************************************************
 * <b>Title</b>: StandingCodeJob.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Nightly job that looks at each job and determines whether 
 * the standing of the job is GOOD, CRITICAL or DELAYED
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 24, 2019
 * @updates:
 ****************************************************************************/

public class StandingCodeJob extends AbstractSMTJob {

	// Members
	private Map<String, Object> attributes;

	// Number of days the service order can be in the given status before being marked CRITICAL.
	private static Map<StatusCode, Integer> statusDaysToCritical = new EnumMap<>(StatusCode.class);

	// Number of days the service order can be in the given status before being CLOSED.
	private static Map<StatusCode, Integer> statusDaysToClosed = new EnumMap<>(StatusCode.class);

	static {
		// Set the Days to Critical rules
		statusDaysToCritical.put(StatusCode.CAS_ASSIGNED, 2);
		statusDaysToCritical.put(StatusCode.CAS_IN_DIAG, 2);
		statusDaysToCritical.put(StatusCode.CAS_IN_REPAIR, 2);
		statusDaysToCritical.put(StatusCode.MISSING_SERIAL_NO, 3);
		statusDaysToCritical.put(StatusCode.PENDING_UNIT_RETURN, 3);
		statusDaysToCritical.put(StatusCode.RAR_PENDING_NOTIFICATION, 3);
		statusDaysToCritical.put(StatusCode.UNLISTED_SERIAL_NO, 1);
		statusDaysToCritical.put(StatusCode.USER_DATA_INCOMPLETE, 3);

		// Set the Days to Closed rules
		statusDaysToClosed.put(StatusCode.MISSING_SERIAL_NO, 7);
		statusDaysToClosed.put(StatusCode.OPENED, 5);
		statusDaysToClosed.put(StatusCode.USER_DATA_INCOMPLETE, 7);
	}

	public StandingCodeJob() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.scheduler.AbstractSMTJob#execute(org.quartz.JobExecutionContext)
	 */
	@Override
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		super.execute(ctx);

		String message = "Success";
		boolean success = true;
		attributes = ctx.getMergedJobDataMap().getWrappedMap();

		// Transpose the attributes for the workflow queue handler
		Map<Object, Object> queueAttr = new HashMap<>();
		for (Map.Entry<String, Object> entry : attributes.entrySet()) {
			queueAttr.put(entry.getKey(), entry.getValue());
		}

		// Override the Quartz workflow handler used elsewhere, this is used when sending notifications on a status change
		attributes.put(GlobalConfig.KEY_DEFAULT_WORKFLOW_HANDLER, new SMTWorkflowQueueHandler(queueAttr));

		// Get the list of open tickets in good/critical standing, including # of business days in the current status
		List<TicketVO> tickets = getTickets();

		// Close or update tickets according to the given criteria
		try {
			closeTickets(tickets);
			setCritical(tickets);
			closeRefuseReturn();
		} catch (DatabaseException | SQLException | InvalidDataException dbe) {
			message = "Could not update tickets due to a database exception: " + dbe.getMessage();
			success = false;
			log.error("Unable to run WSLA standing job", dbe);
		}

		// Finalize the job
		finalizeJob(success, message);
	}

	/**
	 * @throws SQLException 
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 * 
	 */
	private void closeRefuseReturn() throws SQLException, DatabaseException, InvalidDataException {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);

		// Get the tickets that a user refused return for over 30 days
		StringBuilder sql = new StringBuilder(256);
		sql.append("select a.ticket_id, originator_user_id, refuse_dt ");
		sql.append("from ").append(schema).append("wsla_ticket a ");
		sql.append("inner join ( ");
		sql.append("select ticket_id, min(create_dt) as refuse_dt ");
		sql.append("from ").append(schema).append("wsla_ticket_data ");
		sql.append("where attribute_cd = 'attr_returnRefused' ");
		sql.append("group by ticket_id ");
		sql.append(") as td on a.ticket_id = td.ticket_id ");
		sql.append("where standing_cd = 'CRITICAL' and status_cd = 'DELIVERY_SCHEDULED' ");
		sql.append("and date_part('days', current_timestamp - refuse_dt::timestamp) > 30; ");

		BaseTransactionAction bta = new BaseTransactionAction(new SMTDBConnection(conn), attributes);
		try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			try (ResultSet rs = ps.executeQuery()) {

				while(rs.next()) {
					String ticketId = rs.getString(1);

					// Close the ticket
					bta.changeStatus(ticketId, rs.getString(2), StatusCode.CLOSED, LedgerSummary.TICKET_CLOSED.summary + ": Cerrado Automáticamente", null);

					// Mark the unit as disposed - DECOMMISSIONED
					bta.addLedger(ticketId, rs.getString(2), null, LedgerSummary.UNIT_DECOMISSIONED.summary, UnitLocation.DECOMMISSIONED);
					updateUnitLocation(ticketId, UnitLocation.DECOMMISSIONED);
				}
			}
		} 
	}

	/**
	 * 
	 * @param ticketId
	 * @param loc
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	public void updateUnitLocation(String ticketId, UnitLocation loc) 
			throws InvalidDataException, DatabaseException {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		TicketVO ticket = new TicketVO();
		ticket.setTicketId(ticketId);
		ticket.setUnitLocation(loc);

		DBProcessor db = new DBProcessor(conn, schema);
		db.update(ticket, Arrays.asList("ticket_id", "unit_location_cd"));
	}

	/**
	 * Get's the open tickets which might require a status change or standing code change.
	 * The status age is calculated in business days (M-F only).
	 * 
	 * @return
	 */
	@DatabaseNote(type = DBType.POSTGRES)
	private List<TicketVO> getTickets() {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);

		// Build the sql
		StringBuilder sql = new StringBuilder(625);
		sql.append(DBUtil.SELECT_CLAUSE).append("ticket_id, originator_user_id, status_cd, standing_cd, ");
		sql.append("sum(case when extract(dow from series.series_dt) in (1,2,3,4,5) then 1 else 0 end) as status_age_no");
		sql.append(DBUtil.FROM_CLAUSE).append("(");
		sql.append(DBUtil.SELECT_CLAUSE).append("t.ticket_id, t.originator_user_id, t.status_cd, t.standing_cd, ");
		sql.append("generate_series(max(tl.create_dt) + interval '1 day', now(), '1 day'::interval) as series_dt");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_ticket t");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_ticket_ledger tl on t.ticket_id = tl.ticket_id and t.status_cd = tl.status_cd");
		sql.append(DBUtil.WHERE_CLAUSE).append("t.status_cd != ? and t.standing_cd in (?, ?)");
		sql.append(DBUtil.GROUP_BY).append("t.ticket_id");
		sql.append(") as series");
		sql.append(DBUtil.GROUP_BY).append("ticket_id, originator_user_id, status_cd, standing_cd");

		//  Load the params
		List<Object> params = new ArrayList<>();
		params.add(StatusCode.CLOSED.name());
		params.add(Standing.GOOD.name());
		params.add(Standing.CRITICAL.name());

		// Get/return the tickets
		DBProcessor dbp = new DBProcessor(conn);
		dbp.setGenerateExecutedSQL(log.isDebugEnabled());
		return dbp.executeSelect(sql.toString(), params, new TicketVO());
	}

	/**
	 * Closes tickets that match the required criteria for closing. If the age in weekdays (M-F),
	 * is greater than or equal to the number of days for the given status, the ticket is closed.
	 * 
	 * Removes tickets from the list that were closed, as they will not need further processing.
	 * 
	 * @param tickets
	 * @throws DatabaseException 
	 */
	private void closeTickets(List<TicketVO> tickets) throws DatabaseException {
		Iterator<TicketVO> iter = tickets.iterator();

		while (iter.hasNext()) {
			TicketVO ticket = iter.next();

			// Check if criteria are matched, and close the ticket accordingly
			Integer daysToClose = statusDaysToClosed.get(ticket.getStatusCode());
			if (daysToClose != null && ticket.getStatusAge() >= daysToClose) {
				BaseTransactionAction bta = new BaseTransactionAction(new SMTDBConnection(conn), attributes);
				bta.changeStatus(ticket.getTicketId(), ticket.getOriginatorUserId(), StatusCode.CLOSED, LedgerSummary.TICKET_CLOSED.summary + ": Cerrado Automáticamente", null);
				iter.remove();
			}
		}
	}

	/**
	 * Sets tickets to CRITICAL standing that match the required criteria. If the age in weekdays (M-F), is greater
	 * than or equal to the number of days for the given status, the ticket standing is set to CRITICAL.
	 * 
	 * Skips over any tickets that are already in CRITICAL standing. They will not need an update. 
	 * 
	 * Removes tickets from the list that were updated, as they will not need further processing.
	 * 
	 * @param tickets
	 * @throws SQLException 
	 */
	private void setCritical(List<TicketVO> tickets) throws SQLException {
		Iterator<TicketVO> iter = tickets.iterator();
		List<String> criticalTicketIds = new ArrayList<>();

		while (iter.hasNext()) {
			TicketVO ticket = iter.next();

			// Already CRITICAL, no further processing required
			if (ticket.getStandingCode() == Standing.CRITICAL) {
				iter.remove();
				continue; 
			}

			// Check if criteria are matched, and add to the list of tickets to update
			Integer daysToCritical = statusDaysToCritical.get(ticket.getStatusCode());
			if (daysToCritical != null && ticket.getStatusAge() >= daysToCritical) {
				criticalTicketIds.add(ticket.getTicketId());
				iter.remove();
			}
		}

		// Update the standing for the tickets that need to be updated
		updateTicketStandings(criticalTicketIds, Standing.CRITICAL);
	}

	/**
	 * Updates the given tickets to the specified standing code.
	 * 
	 * @param ticketIds
	 * @param standingCode
	 * @throws SQLException 
	 */
	private void updateTicketStandings(List<String> ticketIds, Standing standingCode) throws SQLException {
		if (ticketIds.isEmpty()) return;

		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);

		// Build the sql
		StringBuilder sql = new StringBuilder(100);
		sql.append(DBUtil.UPDATE_CLAUSE).append(schema).append("wsla_ticket ");
		sql.append("set standing_cd = ?, update_dt = ? ");
		sql.append(DBUtil.WHERE_CLAUSE).append("ticket_id = ?");

		// Update the records
		try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			for (String ticketId : ticketIds) {
				ps.setString(1, standingCode.name());
				ps.setDate(2, Convert.formatSQLDate(new Date()));
				ps.setString(3, ticketId);
				ps.addBatch();
			}

			// Execute the batch
			ps.executeBatch();
		}
	}
}

