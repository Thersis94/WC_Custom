package com.wsla.util.migration;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.MapUtil;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <p><b>Title:</b> ProfecoTickets.java</p>
 * <p><b>Description:</b> Phase 2 re-run to merge the Profeco tickets out of the SW files.
 * Run this importer after using the primary importer to load the tickets normally...this script will then 
 * merge the data from the profecos into their originals, and delete the profeco tickets.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Oct 03, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class ProfecoTickets extends AbsImporter {

	private Map<String, String> ticketIds = new HashMap<>(100000);

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		loadTicketIds();
		migrateAssets();
		migrateActivities();
		createOpenLedgers();
		createCloseLedgers();
		updateTicketStatus();
		deleteProfecoTickets();
	}


	/**
	 * move files from the profeco tickets to the original ticket
	 */
	private void migrateAssets() {
		String sql = StringUtil.join(DBUtil.UPDATE_CLAUSE, schema, "wsla_ticket_data set ticket_id=? where ",
				"attribute_id in ('attr_serialNumberImage', 'attr_unitImage', 'attr_proofPurchase') and ticket_id=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (Map.Entry<String, String> entry : ticketIds.entrySet()) {
				ps.setString(1, entry.getValue());
				ps.setString(2, entry.getKey());
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.info(String.format("migrated %d assets (files) from %d profeco tickets", cnt.length, ticketIds.size()));

		} catch (SQLException sqle) {
			log.error("could not migrate assets", sqle);
		}
	}


	/**
	 * move activities (which include comments) from the profeco tickets to the original ticket
	 */
	private void migrateActivities() {
		String sql = StringUtil.join(DBUtil.UPDATE_CLAUSE, schema, "wsla_ticket_comment set ticket_id=? where ticket_id=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (Map.Entry<String, String> entry : ticketIds.entrySet()) {
				ps.setString(1, entry.getValue());
				ps.setString(2, entry.getKey());
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.info(String.format("migrated %d ticket comments & activities from %d profeco tickets", cnt.length, ticketIds.size()));

		} catch (SQLException sqle) {
			log.error("could not migrate activities", sqle);
		}
	}


	/**
	 * Create an 'opened' ledger entry - one for each profeco ticket.
	 */
	private void createOpenLedgers() {
		String sql = StringUtil.join(DBUtil.INSERT_CLAUSE, schema, "wlsa_ticket_ledger ",
				"(ledger_entry_id, disposition_by_id, ticket_id, status_cd, summary_txt, create_dt, billable_amt_no, unit_location_cd, billable_activity_cd) ",
				"select replace(newid(),'-',''), disposition_by_id, ticket_id, status_cd, summary_txt, create_dt, billable_amt_no, unit_location_cd, billable_activity_cd ",
				DBUtil.FROM_CLAUSE, schema, "wsla_ticket_ledger where ticket_id=? and status_cd='OPENED' limit 1"); //limit 1 - just incase there are dups in the DB!
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (Map.Entry<String, String> entry : ticketIds.entrySet()) {
				ps.setString(1, entry.getValue());
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.info(String.format("created %d opened ledger entries for %d profeco tickets", cnt.length, ticketIds.size()));

		} catch (SQLException sqle) {
			log.error("could not create open ledger entries", sqle);
		}
	}


	/**
	 * Create an 'closed' ledger entry for each CLOSED ticket.  Note this is based off
	 * whether the profeco ticket was closed, not whether the original ticket was closed.
	 * The record is tied to the original ticket, however.
	 */
	private void createCloseLedgers() {
		String sql = StringUtil.join(DBUtil.INSERT_CLAUSE, schema, "wlsa_ticket_ledger ",
				"(ledger_entry_id, disposition_by_id, ticket_id, status_cd, summary_txt, create_dt, billable_amt_no, unit_location_cd, billable_activity_cd) ",
				"select replace(newid(),'-',''), disposition_by_id, ticket_id, status_cd, summary_txt, create_dt, billable_amt_no, unit_location_cd, billable_activity_cd ",
				DBUtil.FROM_CLAUSE, schema, "wsla_ticket_ledger where ticket_id=? and status_cd='CLOSED' limit 1"); //limit 1 - just incase there are dups in the DB!
		log.debug(sql);

		// Note the way this code is written we're running a query for each ticket.
		// That doesn't necessarily mean any DB records are getting created.
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (Map.Entry<String, String> entry : ticketIds.entrySet()) {
				ps.setString(1, entry.getValue());
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.info(String.format("created %d closed ledger entries for %d profeco tickets", cnt.length, ticketIds.size()));

		} catch (SQLException sqle) {
			log.error("could not create close ledger entries", sqle);
		}
	}


	/**
	 * Update the ticket table with the proper profeco status, based on profeco ticket's status.
	 * If the profeco ticket is closed, the original ticket gets profeco_status_cd=PROFECO_COMPLETE else it gets profeco_status_cd=IN_PROFECO
	 */
	private void updateTicketStatus() {
		String sql = StringUtil.join(DBUtil.UPDATE_CLAUSE, schema, "wlsa_ticket ",
				"set profeco_status_cd=(select case when status_cd='CLOSED' then 'PROFECO_COMPLETE' else 'IN_PROFECO' end ",
				DBUtil.FROM_CLAUSE, schema, "wsla_ticket where ticket_id=?) where ticket_id=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (Map.Entry<String, String> entry : ticketIds.entrySet()) {
				ps.setString(1, entry.getKey());
				ps.setString(2, entry.getValue());
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.info(String.format("update %d ticket status' for  %d profeco tickets", cnt.length, ticketIds.size()));

		} catch (SQLException sqle) {
			log.error("could not update ticket status", sqle);
		}
	}


	/**
	 * delete the profeco tickets from the DB.
	 * Note to re-run this importer you'd have to re-import the profeco tickets 
	 * using the 5 main importers (HDR,XDD,LNI,CMT,Assets), as well as phase 2's SOLineItemComments
	 */
	private void deleteProfecoTickets() {
		String sql = StringUtil.join(DBUtil.DELETE, schema, "wsla_ticket where ticket_id=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (Map.Entry<String, String> entry : ticketIds.entrySet()) {
				ps.setString(1, entry.getKey());
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.info(String.format("deleted %d out of %d profeco tickets", cnt.length, ticketIds.size()));

		} catch (SQLException sqle) {
			log.error("could not delete profeco tickets", sqle);
		}
	}


	/**
	 * Populate the Map<ProfecoTicket#, OriginalTicketId> from the database
	 */
	private void loadTicketIds() {
		//TODO - likely come from loaded config?  else hard-coded mapping from Steve.
		String sql = StringUtil.join("select ticket_no as key, ticket_id as value from ", schema, "wsla_ticket");
		MapUtil.asMap(ticketIds, db.executeSelect(sql, null, new GenericVO()));
		log.debug(String.format("loaded %d ticketIds", ticketIds.size()));
	}


	@Override
	protected void save() throws Exception {
		throw new RuntimeException("not implemented");
	}
}
