package com.wsla.util.migration;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketDataVO;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.TicketVO.UnitLocation;
import com.wsla.util.migration.vo.ExtTicketVO;
import com.wsla.util.migration.vo.SOHDRFileVO;

/****************************************************************************
 * <p><b>Title:</b> Harvest.java</p>
 * <p><b>Description:</b> Read the "these should be harvested" tickets from the raw files, 
 * then correct the data/ticket in Cypher's database.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Sept 26, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class Harvest extends AbsImporter {

	private List<SOHDRFileVO> data = new ArrayList<>(50000);


	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		File[] files = listFilesMatching(props.getProperty("soHeaderFile"), "(.*)SOHDR(.*)");

		for (File f : files)
			data.addAll(readFile(f, SOHDRFileVO.class, SHEET_1));

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
			//010 are replacements and all we care about
			if ("035".equals(dataVo.getSoType())) {
				ExtTicketVO vo = transposeTicketData(dataVo, new ExtTicketVO());
				tickets.put(vo.getTicketId(), vo);
			}
		}

		populateTicketDBData(tickets);

		tickets = removeGhostRecords(tickets);

		//don't bother with the rest of this class if we have no tickets
		if (tickets.isEmpty()) return;

		decomissionUnits(tickets.keySet()); //ticket

		purgePartShipments(tickets.keySet()); //calls purgeShipments to cascade deletion

		setDispositionCode(tickets); //ticket_data attr_dispositionCode	NONREPAIRABLE

		correctLedgerEntries(tickets);
	}


	/**
	 * Prune any tickets we found in the Excel files that aren't also in the database.
	 * We can't update/mutated DB records that don't exist!
	 * @param tickets
	 * @return
	 */
	private Map<String, ExtTicketVO> removeGhostRecords(Map<String, ExtTicketVO> tickets) {
		Map<String, ExtTicketVO> tix = new HashMap<>(tickets.size());

		for (Map.Entry<String, ExtTicketVO> entry : tickets.entrySet()) {
			if (StringUtil.isEmpty(entry.getValue().getTicketIdText())) {
				log.warn(String.format("database does not contain ticket %s", entry.getKey()));
			} else {
				tix.put(entry.getKey(), entry.getValue());
			}
		}

		log.info(String.format("trimmed ticket list from %d to %d", tickets.size(), tix.size()));
		return tix;
	}


	/**
	 * @param values
	 */
	private void decomissionUnits(Set<String> ticketIds) {
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
	private void purgePartShipments(Set<String> ticketIds) {
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

		purgeShipments(ticketIds);
	}


	/**
	 * @param keySet
	 */
	private void purgeShipments(Set<String> ticketIds) {
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
		//create a ledger entry for each ticket
		TicketLedgerVO vo;
		Map<String, TicketLedgerVO> ledgers = new HashMap<>(tickets.size());
		for (ExtTicketVO tkt : tickets.values()) {
			vo = new TicketLedgerVO();
			vo.setLedgerEntryId(uuid.getUUID());
			vo.setTicketId(tkt.getTicketId());
			vo.setDispositionBy(SOHeader.LEGACY_USER_ID);
			vo.setSummary("Estatus de Tipo de Servicio Modificado : NONREPAIRABLE");
			vo.setStatusCode(StatusCode.UNREPAIRABLE);
			vo.setCreateDate(stepTime(tkt.getClosedDate(), -60));
			ledgers.put("disp-" + tkt.getTicketId(), vo);

			//create one for the harvest, and preserve it's ID so we can bind it to ticket_data
			vo = new TicketLedgerVO();
			vo.setLedgerEntryId(uuid.getUUID());
			vo.setTicketId(tkt.getTicketId());
			vo.setDispositionBy(SOHeader.LEGACY_USER_ID);
			vo.setSummary("Equipo Listo para Canibalización");
			vo.setStatusCode(StatusCode.HARVEST_APPROVED);
			vo.setCreateDate(stepTime(tkt.getClosedDate(),-30)); //30mins before close, allows for return shipping in-between
			ledgers.put("harv-" + tkt.getTicketId(), vo);
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
		List<TicketDataVO> inserts = new ArrayList<>(tickets.size()*2);
		for (Map.Entry<String, ExtTicketVO> entry : tickets.entrySet()) {
			ExtTicketVO tkt = entry.getValue();
			dataVo = new TicketDataVO();
			dataVo.setTicketId(tkt.getTicketId());
			dataVo.setCreateDate(stepTime(tkt.getClosedDate(), -60));
			dataVo.setLedgerEntryId(ledgers.getOrDefault("disp-" + tkt.getTicketId(), new TicketLedgerVO()).getLedgerEntryId());
			dataVo.setAttributeCode("attr_dispositionCode");
			dataVo.setValue("NONREPAIRABLE");
			inserts.add(dataVo);

			//also add one for harvest status
			dataVo = new TicketDataVO();
			dataVo.setTicketId(tkt.getTicketId());
			dataVo.setLedgerEntryId(ledgers.getOrDefault("harv-" + tkt.getTicketId(), new TicketLedgerVO()).getLedgerEntryId());
			dataVo.setCreateDate(stepTime(tkt.getClosedDate(), -30));
			dataVo.setAttributeCode("attr_harvest_status");
			dataVo.setValue("HARVEST_APPROVED");
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
		//add entries for Replacement and addtl diags
		TicketLedgerVO vo;
		List<TicketLedgerVO> ledgers = new ArrayList<>(tickets.size()*9);

		for (ExtTicketVO tkt : tickets.values()) {
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
			vo.setStatusCode(StatusCode.REPLACEMENT_REQUEST);
			vo.setCreateDate(stepTime(tkt.getClosedDate(), -52));
			ledgers.add(vo);

			vo = new TicketLedgerVO();
			vo.setTicketId(tkt.getTicketId());
			vo.setDispositionBy(SOHeader.LEGACY_USER_ID);
			vo.setStatusCode(StatusCode.DISPOSE_UNIT);
			vo.setCreateDate(stepTime(tkt.getClosedDate(), -50));
			vo.setUnitLocation(UnitLocation.DECOMMISSIONED);
			ledgers.add(vo);

			//NOTE: HARVEST_APPROVED slips in right here, but is coded in setDispositionCode() above
		}
		writeToDB(ledgers);
	}


	/**
	 * @param dataVo
	 * @param extTicketVO
	 * @return
	 */
	private ExtTicketVO transposeTicketData(SOHDRFileVO dataVo, ExtTicketVO vo) {
		vo.setTicketId(dataVo.getSoNumber());
		vo.setClosedDate(dataVo.getClosedDate());
		return vo;
	}


	/**
	 * go to the database for previously-computed data we can reuse...avoids 
	 * duplicating complex lookups & logic (using data we may not have).
	 * need to know the actual ticketId - on the rare chance its not the same as ticketNo/soNumber
	 * @param tickets
	 */
	private void populateTicketDBData(Map<String, ExtTicketVO> tickets) {
		if (tickets == null || tickets.isEmpty()) return;
		String sql = StringUtil.join("select distinct t.ticket_id, t.ticket_no",
				DBUtil.FROM_CLAUSE, schema, "wsla_ticket t",
				DBUtil.WHERE_CLAUSE, "t.ticket_no in (", DBUtil.preparedStatmentQuestion(tickets.size()), ")");
		log.debug(sql);

		int x = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (String ticketId : tickets.keySet())
				ps.setString(++x, ticketId);

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				ExtTicketVO tkt = tickets.get(rs.getString(2));
				if (tkt == null) {
					log.error("could not find ticket for " + rs.getString(2));
					continue;
				}
				tkt.setTicketId(rs.getString(1)); //these should be the same as ticketNo, but just in case
				tkt.setTicketIdText(rs.getString(2));
			}

		} catch (SQLException sqle) {
			log.error("could not populate tickets from DB", sqle);
		}
	}
}
