package com.wsla.util.migration;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
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
				if (!StringUtil.isEmpty(entry.getValue())) {
					ps.setString(1, entry.getValue());
					ps.setString(2, entry.getKey());
					ps.addBatch();
				}
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
				if (!StringUtil.isEmpty(entry.getValue())) {
					ps.setString(1, entry.getValue());
					ps.setString(2, entry.getKey());
					ps.addBatch();
				}
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
				"select replace(newid(),'-',''), disposition_by_id, ticket_id, null, 'Profeco Started', create_dt, billable_amt_no, unit_location_cd, billable_activity_cd ",
				DBUtil.FROM_CLAUSE, schema, "wsla_ticket_ledger where ticket_id=? and status_cd='OPENED' limit 1"); //limit 1 - just incase there are dups in the DB!
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (Map.Entry<String, String> entry : ticketIds.entrySet()) {
				if (!StringUtil.isEmpty(entry.getValue())) {
					ps.setString(1, entry.getValue());
					ps.addBatch();
				}
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
				"select replace(newid(),'-',''), disposition_by_id, ?, null, 'Profeco Closed', create_dt, billable_amt_no, unit_location_cd, billable_activity_cd ",
				DBUtil.FROM_CLAUSE, schema, "wsla_ticket_ledger where ticket_id=? and status_cd='CLOSED' limit 1"); //limit 1 - just in case there are dups in the DB!
		log.debug(sql);

		// Note the way this code is written we're running a query for each ticket.
		// That doesn't necessarily mean any DB records are getting created.
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (Map.Entry<String, String> entry : ticketIds.entrySet()) {
				if (!StringUtil.isEmpty(entry.getValue())) {
					ps.setString(1, entry.getKey());
					ps.setString(2, entry.getValue());
					ps.addBatch();
				}
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
				if (!StringUtil.isEmpty(entry.getValue())) {
					ps.setString(1, entry.getKey()); // w/status from profeco tkt,
					ps.setString(2, entry.getValue()); //update the original tkt
					ps.addBatch();
				}
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
				if (!StringUtil.isEmpty(entry.getValue())) { //don't delete the profeco ticket if it has no ancestor
					ps.setString(1, entry.getKey());
					ps.addBatch();
				}
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
		ticketIds.put("WSL00109867","VIZ00021265");
		ticketIds.put("WSL00109305","SEI00015330");
		ticketIds.put("WSL00109869","SAN00080754");
		ticketIds.put("WSL00119288","SAN00102599");
		ticketIds.put("WSL00109852","SAN00076545");
		ticketIds.put("WSL00121962","SAN00070927");
		ticketIds.put("WSL00109872","SAN00076713");
		ticketIds.put("WSL00109285","SAN00091097");
		ticketIds.put("WSL00109526","SAN00056052");
		ticketIds.put("WSL00121790","SAN00100111");
		ticketIds.put("WSL00122481","SAN00100474");
		ticketIds.put("WSL00124151","SAN00101110");
		ticketIds.put("WSL00122442","SAN00104174");
		ticketIds.put("WSL00123900","SAN00109334");
		ticketIds.put("WSL00107746","SAN00060321");
		ticketIds.put("WSL00124117","SAN00068756");
		ticketIds.put("WSL00122421","SAN00069262");
		ticketIds.put("WSL00099297","SAN00069352");
		ticketIds.put("WSL00124322","SAN00070362");
		ticketIds.put("WSL00107763","SAN00070861");
		ticketIds.put("WSL00124130","SAN00075380");
		ticketIds.put("WSL00124300","SAN00077132");
		ticketIds.put("WSL00124272","SAN00078444");
		ticketIds.put("WSL00103471","SAN00080154");
		ticketIds.put("WSL00121955","SAN00082252");
		ticketIds.put("WSL00124288","SAN00082656");
		ticketIds.put("WSL00122475","SAN00082854");
		ticketIds.put("WSL00124143","SAN00082916");
		ticketIds.put("WSL00124314","SAN00084191");
		ticketIds.put("WSL00121785","SAN00084271");
		ticketIds.put("WSL00124306","SAN00084310");
		ticketIds.put("WSL00123916","SAN00087541");
		ticketIds.put("WSL00122453","SAN00088112");
		ticketIds.put("WSL00109287","SAN00089978");
		ticketIds.put("WSL00122466","SAN00090409");
		ticketIds.put("WSL00120983","SAN00091261");
		ticketIds.put("WSL00124328","SAN00091705");
		ticketIds.put("WSL00109565","SAN00093767");
		ticketIds.put("WSL00122468","SAN00093778");
		ticketIds.put("WSL00121957","SAN00094067");
		ticketIds.put("WSL00124106","SAN00094181");
		ticketIds.put("WSL00121518","SAN00095730");
		ticketIds.put("WSL00123892","SAN00097676");
		ticketIds.put("WSL00109538","SAN00097751");
		ticketIds.put("WSL00123914","SAN00099989");
		ticketIds.put("WSL00091859","SKC00066546");
		ticketIds.put("WSL00109849","SAN00061959");
		ticketIds.put("WSL00091838","WMT00091220");
		ticketIds.put("WSL00103473","WMT00093356");
		ticketIds.put("WSL00109672",""); //these 5 are both the source & dest
		ticketIds.put("WSL00109744","");
		ticketIds.put("WSL00091829","");
		ticketIds.put("WSL00109188","");
		ticketIds.put("WSL00109195","");
		ticketIds.put("WSL00023040","HIT00017362");
		ticketIds.put("WSL00023037","HIT00022939");
		ticketIds.put("WSL00099301","SAN00103928");
		ticketIds.put("WSL00109863","SAN00109276");
		ticketIds.put("WSL00099306","SAN00110991");
		ticketIds.put("WSL00025692","SAN00022499");
		ticketIds.put("WSL00031391","SAN00031371");
		ticketIds.put("WSL00109182","SAN00034701");
		ticketIds.put("WSL00109174","SAN00044715");
		ticketIds.put("WSL00091813","SAN00049120");
		ticketIds.put("WSL00107700","SAN00062378");
		ticketIds.put("WSL00109294","SAN00063648");
		ticketIds.put("WSL00109319","SAN00063917");
		ticketIds.put("WSL00107744","SAN00064503");
		ticketIds.put("WSL00103468","SAN00064571");
		ticketIds.put("WSL00109299","SAN00064830");
		ticketIds.put("WSL00122435","SAN00071696");
		ticketIds.put("WSL00109559","SAN00072332");
		ticketIds.put("WSL00107828","SAN00073122");
		ticketIds.put("WSL00091881","SAN00078750");
		ticketIds.put("WSL00109750","SAN00081800");
		ticketIds.put("WSL00109855","SAN00083307");
		ticketIds.put("WSL00109534","SAN00089238");
		ticketIds.put("WSL00109529","SAN00089917");
		ticketIds.put("WSL00107712","SAN00089988");
		ticketIds.put("WSL00115130","SAN00093598");
		ticketIds.put("WSL00109186","SAN00093715");
		ticketIds.put("WSL00109858","SAN00096841");
		ticketIds.put("WSL00109541","SAN00098200");
		ticketIds.put("WSL00103460","SEI00102388");
		ticketIds.put("WSL00018454","SEI00015330");
		ticketIds.put("WSL00099294",""); //SKC-**** - sent to Steve
		ticketIds.put("WSL00107833","SKC00090780");
		ticketIds.put("WSL00091854","SKC00091385");
		ticketIds.put("WSL00024600","VIZ00021613");
		ticketIds.put("WSL00091782","VIZ00090107");
		ticketIds.put("WSL00109531","WMT00112639");
		ticketIds.put("WSL00099277","WMT00049076");
		ticketIds.put("WSL00109307","WMT00084363");
		log.debug(String.format("loaded %d ticketIds", ticketIds.size()));
	}


	@Override
	protected void save() throws Exception {
		throw new RuntimeException("not implemented");
	}
}
