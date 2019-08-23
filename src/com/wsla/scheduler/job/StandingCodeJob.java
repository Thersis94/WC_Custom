package com.wsla.scheduler.job;

//JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// Quartz 2.2.3
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

// SMT Base Libs
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.DatabaseNote;
import com.siliconmtn.db.DatabaseNote.DBType;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.util.Convert;

// WC Libs
import com.smt.sitebuilder.scheduler.AbstractSMTJob;

// WC Custom
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.TicketVO.Standing;

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
	static {
		statusDaysToCritical.put(StatusCode.CAS_ASSIGNED, 2);
	}
	
	// Number of days the service order can be in the given status before being CLOSED.
	private static Map<StatusCode, Integer> statusDaysToClosed = new EnumMap<>(StatusCode.class);
	static {
		statusDaysToClosed.put(StatusCode.OPENED, 5);
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
		
		// Get the list of open tickets in good/critical standing, including # of business days in the current status
		List<TicketVO> tickets = getTickets();
		
		// Close or update tickets according to the given criteria
		try {
			closeTickets(tickets);
			setCritical(tickets);
		} catch (DatabaseException | SQLException dbe) {
			message = "Could not update tickets due to a database exception: " + dbe.getMessage();
			success = false;
		}
		
		// Finalize the job
		try {
			this.finalizeJob(success, message);
		} catch (InvalidDataException e) {
			log.error("Unable to finalize job");
		}
	}

	/**
	 * Get's the open tickets which might require a status change or standing code change.
	 * 
	 * @return
	 */
	@DatabaseNote(type = DBType.POSTGRES)
	private List<TicketVO> getTickets() {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		
		// Build the sql
		StringBuilder sql = new StringBuilder(625);
		sql.append(DBUtil.SELECT_CLAUSE).append("ticket_id, originator_user_id, status_cd, standing_cd, ");
		sql.append("sum(case when extract(dow from series.series_dt) in (1,2,3,4,5) then 1 else 0 end) as weekdays_age_no");
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
			if (daysToClose != null && ticket.getWeekdaysAge() >= daysToClose) {
				BaseTransactionAction bta = new BaseTransactionAction(new SMTDBConnection(conn), attributes);
				bta.changeStatus(ticket.getTicketId(), ticket.getUserId(), StatusCode.CLOSED, LedgerSummary.TICKET_CLOSED.summary + ": Cerrado Automáticamente", null);
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
			if (daysToCritical != null && ticket.getWeekdaysAge() >= daysToCritical) {
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
		StringBuilder sql = new StringBuilder(300);
		sql.append(DBUtil.UPDATE_CLAUSE).append(schema).append("wsla_ticket ");
		sql.append("set standing_cd = ?, update_dt = ? ");
		sql.append(DBUtil.WHERE_CLAUSE).append("ticket_id in (");
		sql.append(DBUtil.preparedStatmentQuestion(ticketIds.size()));
		sql.append(")");
		
		// Update the records
		try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			int ctr = 1;
			ps.setString(ctr++, standingCode.name());
			ps.setDate(ctr++, Convert.formatSQLDate(new Date()));
			
			// Set all of the ticket id parameters
			for (String ticketId : ticketIds) {
				ps.setString(ctr++, ticketId);
			}
			
			// Update the ticket records
			ps.executeUpdate();
		}
	}
}

