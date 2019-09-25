package com.wsla.util.migration;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.MapUtil;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;
import com.wsla.common.WSLAConstants;
import com.wsla.data.ticket.CreditMemoVO;
import com.wsla.data.ticket.RefundReplacementVO;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketDataVO;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.TicketVO.UnitLocation;
import com.wsla.util.migration.vo.ExtTicketVO;
import com.wsla.util.migration.vo.SOHDRFileVO;

/****************************************************************************
 * <p><b>Title:</b> Refunds.java</p>
 * <p><b>Description:</b> Read the "these should be refunds" tickets from the raw files, then correct the
 * data/ticket in Cypher's database.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Sept 18, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class Refund extends AbsImporter {

	private List<SOHDRFileVO> data = new ArrayList<>(50000);
	private Map<String, String> ticketIds = new HashMap<>(5000);


	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		File[] files = listFilesMatching(props.getProperty("soHeaderFile"), "(.*)SOHDR(.*)");

		for (File f : files)
			data.addAll(readFile(f, SOHDRFileVO.class, SHEET_1));

		loadTicketIds();
		save();
	}


	/**
	 * Save the imported providers to the database.
	 * @param data
	 * @throws Exception 
	 */
	@Override
	protected void save() throws Exception {
		//turn the list of data into a unique list of tickets
		Map<String, ExtTicketVO> tickets= new HashMap<>(data.size());

		for (SOHDRFileVO dataVo : data) {
			//025 are refunds and all we care about here
			if ("025".equals(dataVo.getSoType())) { 
				ExtTicketVO vo = transposeTicketData(dataVo, new ExtTicketVO());
				tickets.put(vo.getTicketId(), vo);
			}
		}

		decomissionUnits(tickets.keySet()); //ticket

		purgeParts(tickets.keySet());

		purgeShipments(tickets.keySet());

		setDispositionCode(tickets); //ticket_data attr_dispositionCode	NONREPAIRABLE

		correctLedgerEntries(tickets);

		changeSchedule(tickets.keySet()); //wsla_ticket_schedule - remove PICKUP

		releaseCredits(tickets.values());  //add row to wsla_ticket_ref_rep, then wsla_credit_memo

	}


	/**
	 * @param values
	 */
	private void decomissionUnits(Set<String> ticketIds) {
		if (ticketIds == null || ticketIds.isEmpty()) return;
		String sql = StringUtil.join(DBUtil.UPDATE_CLAUSE, schema, "wsla_ticket set unit_location_cd=? where ticket_id=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (String ticketId : ticketIds) {
				ps.setString(1, "DECOMMISSIONED");
				ps.setString(2, ticketId);
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.info(String.format("updated %d tickets with to decomissioned unit locations", cnt.length));

		} catch (SQLException sqle) {
			log.error("could not update tickets", sqle);
		}
	}


	/**
	 * @param keySet
	 */
	private void purgeParts(Set<String> ticketIds) {
		if (ticketIds == null || ticketIds.isEmpty()) return;
		String sql = StringUtil.join(DBUtil.DELETE, schema, "wsla_part where ticket_id=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (String ticketId : ticketIds) {
				ps.setString(1, ticketId);
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.info(String.format("deleted %d parts from %d tickets", cnt.length, ticketIds.size()));

		} catch (SQLException sqle) {
			log.error("could not delete parts", sqle);
		}
	}


	/**
	 * @param keySet
	 */
	private void purgeShipments(Set<String> ticketIds) {
		if (ticketIds == null || ticketIds.isEmpty()) return;
		String sql = StringUtil.join(DBUtil.DELETE, schema, "wsla_shipment where ticket_id=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (String ticketId : ticketIds) {
				ps.setString(1, ticketId);
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.info(String.format("deleted %d shipments from %d tickets", cnt.length, ticketIds.size()));

		} catch (SQLException sqle) {
			log.error("could not delete shipments", sqle);
		}
	}


	/**
	 * get a list of tickets that need updated.  Often there is no record, so we need 
	 * to distinguish updates from inserts here
	 * @param keySet
	 * @throws Exception 
	 */
	private void setDispositionCode(Map<String, ExtTicketVO> tickets) throws Exception {
		if (tickets == null || tickets.isEmpty()) return;

		//create a ledger entry for each ticket
		TicketLedgerVO vo;
		Map<String, TicketLedgerVO> ledgers = new HashMap<>(tickets.size());
		for (ExtTicketVO tkt : tickets.values()) {
			vo = new TicketLedgerVO();
			vo.setTicketId(tkt.getTicketId());
			vo.setDispositionBy(SOHeader.LEGACY_USER_ID);
			vo.setSummary("Estatus de Tipo de Servicio Modificado : NONREPAIRABLE");
			vo.setStatusCode(StatusCode.UNREPAIRABLE);
			vo.setCreateDate(stepTime(tkt.getClosedDate(), -60));
			ledgers.put(tkt.getTicketId(), vo);
		}
		writeToDB(new ArrayList<>(ledgers.values()));

		//delete the existing dispositions from ticket_data - this is easier than trying to update some while inserting others
		String sql = StringUtil.join(DBUtil.DELETE, schema, "wsla_ticket_data where attribute_cd='attr_dispositionCode' and ticket_id=?");
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (String ticketId : tickets.keySet()) {
				ps.setString(1, ticketId);
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.info(String.format("deleted %d dispositions for %d tickets", cnt.length, tickets.size()));

		} catch (SQLException sqle) {
			log.error("could not delete dispositions", sqle);
		}

		//insert ticket_data dispositions for each ticket - make sure to tie-in the ledger entry
		TicketDataVO dataVo;
		List<TicketDataVO> inserts = new ArrayList<>(tickets.size());
		for (Map.Entry<String, ExtTicketVO> entry : tickets.entrySet()) {
			ExtTicketVO tkt = entry.getValue();
			dataVo = new TicketDataVO();
			dataVo.setTicketId(tkt.getTicketId());
			dataVo.setCreateDate(stepTime(tkt.getClosedDate(), -60));
			dataVo.setLedgerEntryId(ledgers.getOrDefault(tkt.getTicketId(), new TicketLedgerVO()).getLedgerEntryId());
			dataVo.setAttributeCode("attr_dispositionCode");
			dataVo.setValue("NONREPAIRABLE");
			inserts.add(dataVo);
		}
		log.info(String.format("inserting %d new dispositions for %d tickets", inserts.size(), tickets.size()));
		writeToDB(inserts);
	}


	/**
	 * add or subtract x minutes from the given date - used to create a realistic timeline of sequenced events
	 * @param date
	 * @param mins
	 * @return
	 */
	private Date stepTime(Date d, int mins) {
		if (d == null) return d;
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		cal.add(Calendar.MINUTE, mins);
		return cal.getTime();
	}


	/**
	 * @param ticketIds
	 * @throws Exception 
	 */
	private void correctLedgerEntries(Map<String, ExtTicketVO> tickets) throws Exception {
		//these units were never returned to the owner - delete those entries
		String sql = StringUtil.join(DBUtil.DELETE, schema, "wsla_ticket_ledger where ",
				"(status_cd='PENDING_PICKUP' or status_cd='PICKUP_COMPLETE') ",
				"and ticket_id=?");
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (String ticketId : tickets.keySet()) {
				ps.setString(1, ticketId);
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.info(String.format("deleted %d delivery-related ledger entries from %d tickets", cnt.length, tickets.size()));

		} catch (SQLException sqle) {
			log.error("could not delete delivery-related ledger entries", sqle);
		}

		//add entries for RAR and addtl diags
		TicketLedgerVO vo;
		List<TicketLedgerVO> ledgers = new ArrayList<>(tickets.size()*6);
		for (ExtTicketVO tkt : tickets.values()) {
			vo = new TicketLedgerVO();
			vo.setTicketId(tkt.getTicketId());
			vo.setDispositionBy(SOHeader.LEGACY_USER_ID);
			vo.setSummary("Equipo Pendiente de Diagnóstico");
			vo.setStatusCode(StatusCode.CAS_IN_DIAG);
			vo.setBillableActivityCode(StatusCode.CAS_IN_DIAG.name());
			vo.setCreateDate(stepTime(tkt.getClosedDate(), -90)); //30mins before unrepairable
			ledgers.add(vo);

			vo = new TicketLedgerVO();
			vo.setTicketId(tkt.getTicketId());
			vo.setDispositionBy(SOHeader.LEGACY_USER_ID);
			vo.setStatusCode(StatusCode.CAS_REPAIR_COMPLETE);
			vo.setCreateDate(stepTime(tkt.getClosedDate(), -55)); //5mins after diags (already written)
			ledgers.add(vo);

			vo = new TicketLedgerVO();
			vo.setTicketId(tkt.getTicketId());
			vo.setDispositionBy(SOHeader.LEGACY_USER_ID);
			vo.setStatusCode(StatusCode.RAR_PENDING_NOTIFICATION);
			vo.setSummary("Estatus de Reparación Modificado: need refund");
			vo.setCreateDate(stepTime(tkt.getClosedDate(), -54));
			ledgers.add(vo);

			vo = new TicketLedgerVO();
			vo.setTicketId(tkt.getTicketId());
			vo.setDispositionBy(SOHeader.LEGACY_USER_ID);
			vo.setStatusCode(StatusCode.RAR_OEM_NOTIFIED);
			vo.setCreateDate(stepTime(tkt.getClosedDate(), -53));
			ledgers.add(vo);

			vo = new TicketLedgerVO();
			vo.setTicketId(tkt.getTicketId());
			vo.setDispositionBy(SOHeader.LEGACY_USER_ID);
			vo.setStatusCode(StatusCode.REFUND_REQUEST);
			vo.setCreateDate(stepTime(tkt.getClosedDate(), -52));
			ledgers.add(vo);

			vo = new TicketLedgerVO();
			vo.setTicketId(tkt.getTicketId());
			vo.setDispositionBy(SOHeader.LEGACY_USER_ID);
			vo.setStatusCode(StatusCode.DISPOSE_UNIT);
			vo.setCreateDate(stepTime(tkt.getClosedDate(), -50));
			vo.setUnitLocation(UnitLocation.DECOMMISSIONED);
			ledgers.add(vo);
		}
		writeToDB(ledgers);
	}


	/**
	 * @param keySet
	 */
	private void changeSchedule(Set<String> ticketIds) {
		if (ticketIds == null || ticketIds.isEmpty()) return;
		String sql = StringUtil.join(DBUtil.DELETE, schema, "wsla_ticket_schedule where transfer_type_cd='PICKUP' and ticket_id=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (String ticketId : ticketIds) {
				ps.setString(1, ticketId);
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.info(String.format("deleted pickup scheduling from %d tickets", cnt.length));

		} catch (SQLException sqle) {
			log.error("could not delete pickup scheduling", sqle);
		}

	}


	/**
	 * add row to wsla_ticket_ref_rep, then to wsla_credit_memo
	 * @param tickets
	 * @throws Exception 
	 */
	private void releaseCredits(Collection<ExtTicketVO> tickets) throws Exception {
		RefundReplacementVO refRep;
		Map<String, RefundReplacementVO> refReps = new HashMap<>(tickets.size());
		for (ExtTicketVO tkt : tickets) {
			refRep = new RefundReplacementVO();
			refRep.setTicketId(tkt.getTicketId());
			refRep.setCreateDate(stepTime(tkt.getClosedDate(),-30)); //30mins before closing, ~20mins after unit disposal
			refRep.setRefundAmount(0);
			refRep.setApprovalType("REFUND_REQUEST");
			refRep.setUnitDisposition("DISPOSE");
			refRep.setBackOrderFlag(0);
			refReps.put(tkt.getTicketId(), refRep);
		}
		writeToDB(new ArrayList<>(refReps.values()));

		//create the credit memo, which references back to the refRep pkId above
		CreditMemoVO credit;
		List<CreditMemoVO> credits = new ArrayList<>(tickets.size());
		for (ExtTicketVO tkt : tickets) {
			credit = new CreditMemoVO();
			credit.setCreditMemoId("credit_" + tkt.getTicketId());
			credit.setTicketId(tkt.getTicketId());
			credit.setCreateDate(stepTime(tkt.getClosedDate(), -30));
			credit.setRefundAmount(0);
			credit.setRefundReplacementId(refReps.getOrDefault(tkt.getTicketId(), new RefundReplacementVO()).getRefundReplacementId());
			credit.setCustomerMemoCode(RandomAlphaNumeric.generateRandom(WSLAConstants.TICKET_RANDOM_CHARS).toUpperCase());
			credit.setApprovalDate(credit.getCreateDate());
			credit.setApprovedBy("Mariana Hernandez");
			credits.add(credit);
		}
		writeToDB(credits);
	}


	/**
	 * @param dataVo
	 * @param extTicketVO
	 * @return
	 */
	private ExtTicketVO transposeTicketData(SOHDRFileVO dataVo, ExtTicketVO vo) {
		vo.setTicketId(dataVo.getSoNumber());
		vo.setCreateDate(dataVo.getReceivedDate());
		vo.setClosedDate(dataVo.getClosedDate());
		vo.setUpdateDate(dataVo.getAltKeyDate());
		return vo;
	}


	/**
	 * Populate the Map<Ticket#, TicketId> from the database to marry the soNumbers in the Excel
	 */
	private void loadTicketIds() {
		String sql = StringUtil.join("select ticket_no as key, ticket_id as value from ", schema, "wsla_ticket");
		MapUtil.asMap(ticketIds, db.executeSelect(sql, null, new GenericVO()));
		log.debug(String.format("loaded %d ticketIds", ticketIds.size()));
	}
}
