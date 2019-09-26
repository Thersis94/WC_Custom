package com.wsla.util.migration;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.ticket.PartVO;
import com.wsla.data.ticket.RefundReplacementVO;
import com.wsla.data.ticket.ShipmentVO;
import com.wsla.data.ticket.ShipmentVO.CarrierType;
import com.wsla.data.ticket.ShipmentVO.ShipmentStatus;
import com.wsla.data.ticket.ShipmentVO.ShipmentType;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketDataVO;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.TicketVO.UnitLocation;
import com.wsla.util.migration.vo.ExtTicketVO;
import com.wsla.util.migration.vo.SOHDRFileVO;

/****************************************************************************
 * <p><b>Title:</b> Refunds.java</p>
 * <p><b>Description:</b> Read the "these should be replacements" tickets from the raw files, 
 * then correct the data/ticket in Cypher's database.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Sept 23, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class Replacement extends AbsImporter {

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
			if ("010".equals(dataVo.getSoType())) {
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

		createPartShipments(tickets.values()); //calls createShipments to appease dependency

		setDispositionCode(tickets); //ticket_data attr_dispositionCode	NONREPAIRABLE

		releaseRepairs(tickets.values()); //ref_rep entry

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
			} else if (StringUtil.isEmpty(entry.getValue().getProductId())) {
				log.warn(String.format("database does not contain product for ticket %s", entry.getKey()));
			} else if (StringUtil.isEmpty(entry.getValue().getCasLocationId())) {
				log.warn(String.format("database does not contain CAS for ticket %s", entry.getKey()));
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
	 * create a part & shipment for the new TV going back to the CAS (from the warehouse)
	 * @param tickets
	 * @throws Exception 
	 */
	private void createPartShipments(Collection<ExtTicketVO> tickets) throws Exception {
		Map<String, String> shipmentMap = createShipments(tickets);
		PartVO vo;
		List<PartVO> parts = new ArrayList<>(tickets.size());
		for (ExtTicketVO tkt : tickets) {
			vo = new PartVO();
			vo.setTicketId(vo.getTicketId());
			vo.setProductId(tkt.getProductId());
			vo.setShipmentId(shipmentMap.get(tkt.getTicketId()));
			vo.setQuantity(1);
			vo.setQuantityReceived(1);
			vo.setUsedQuantityNo(1);
			vo.setCreateDate(stepTime(tkt.getClosedDate(), -110));
			vo.setSubmitApprovalFlag(1);
			parts.add(vo);
		}
		log.info(String.format("saving %d parts for %d tickets", parts.size(), tickets.size()));
		writeToDB(parts);
	}


	/**
	 * create a shipment for the new TV going back to the CAS (from the warehouse)
	 * @param tickets
	 * @throws Exception 
	 */
	private Map<String, String> createShipments(Collection<ExtTicketVO> tickets) throws Exception {
		if (tickets == null || tickets.isEmpty()) return Collections.emptyMap();

		ShipmentVO vo;
		Map<String, String> shipmentMap = new HashMap<>(tickets.size());
		List<ShipmentVO> shipments = new ArrayList<>(tickets.size());
		for (ExtTicketVO tkt : tickets) {
			vo = new ShipmentVO();
			vo.setShipmentId(uuid.getUUID());
			vo.setTicketId(tkt.getTicketId());
			vo.setFromLocationId(SOHeader.LEGACY_PARTS_LOCN);
			vo.setToLocationId(tkt.getCasLocationId());
			vo.setShipmentType(ShipmentType.REPLACEMENT_UNIT);
			vo.setShippedById(SOHeader.LEGACY_USER_ID);
			vo.setCarrierType(CarrierType.ESTAFETA); //a presumption to fill a void
			vo.setCreateDate(stepTime(tkt.getClosedDate(), -110));
			vo.setArrivalDate(vo.getCreateDate());
			vo.setStatus(ShipmentStatus.RECEIVED); //implies created & sent
			shipments.add(vo);
			shipmentMap.put(tkt.getTicketId(), vo.getShipmentId());
		}
		log.info(String.format("saving %d shipments for %d tickets", shipments.size(), tickets.size()));
		writeToDB(shipments);
		return shipmentMap;
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
		//add entries for Replacement and addtl diags
		TicketLedgerVO vo;
		List<TicketLedgerVO> ledgers = new ArrayList<>(tickets.size()*9);

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

			vo = new TicketLedgerVO();
			vo.setTicketId(tkt.getTicketId());
			vo.setDispositionBy(SOHeader.LEGACY_USER_ID);
			vo.setStatusCode(StatusCode.REPLACEMENT_CONFIRMED);
			vo.setCreateDate(stepTime(tkt.getClosedDate(), -40));
			ledgers.add(vo);

			vo = new TicketLedgerVO();
			vo.setTicketId(tkt.getTicketId());
			vo.setDispositionBy(SOHeader.LEGACY_USER_ID);
			vo.setStatusCode(StatusCode.RPLC_DELIVERY_SCHED);
			vo.setSummary("Envío Programado");
			vo.setCreateDate(stepTime(tkt.getClosedDate(), -21));
			ledgers.add(vo);

			vo = new TicketLedgerVO();
			vo.setTicketId(tkt.getTicketId());
			vo.setDispositionBy(SOHeader.LEGACY_USER_ID);
			vo.setStatusCode(StatusCode.RPLC_DELIVEY_RCVD);
			vo.setSummary("Envío Recibido");
			vo.setCreateDate(stepTime(tkt.getClosedDate(), -20));
			ledgers.add(vo);
		}
		writeToDB(ledgers);
	}


	/**
	 * add row to wsla_ticket_ref_rep
	 * @param tickets
	 * @throws Exception 
	 */
	private void releaseRepairs(Collection<ExtTicketVO> tickets) throws Exception {
		RefundReplacementVO refRep;
		List<RefundReplacementVO> refReps = new ArrayList<>(tickets.size());
		for (ExtTicketVO tkt : tickets) {
			refRep = new RefundReplacementVO();
			refRep.setTicketId(tkt.getTicketId());
			refRep.setCreateDate(stepTime(tkt.getClosedDate(),-30)); //30mins before closing, ~20mins after unit disposal
			refRep.setRefundAmount(0);
			refRep.setApprovalType("REPLACEMENT_REQUEST");
			refRep.setUnitDisposition("DISPOSE");
			refRep.setBackOrderFlag(0);
			refRep.setReplacementLocationId(tkt.getCasLocationId());
			refRep.setReplacementProductId(tkt.getProductId());
			refReps.add(refRep);
		}
		writeToDB(refReps);
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
	 * go to the database for previously-computed data we can reuse...avoids 
	 * duplicating complex lookups & logic (using data we may not have).
	 * need to know:  
	 * 1) The CAS assigned
	 * 2) the productId
	 * 3) the actual ticketId - on the rare chance its not the same as ticketNo/soNumber
	 * @param tickets
	 */
	private void populateTicketDBData(Map<String, ExtTicketVO> tickets) {
		if (tickets == null || tickets.isEmpty()) return;
		String sql = StringUtil.join("select distinct t.ticket_id, t.ticket_no, ta.location_id, ps.product_id",
				DBUtil.FROM_CLAUSE, schema, "wsla_ticket t",
				DBUtil.LEFT_OUTER_JOIN, schema, "wsla_ticket_assignment ta on t.ticket_id=ta.ticket_id and ta.assg_type_cd='CAS'",
				DBUtil.LEFT_OUTER_JOIN, schema, "wsla_product_serial ps on t.product_serial_id=ps.product_serial_id",
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
				tkt.setCasLocationId(rs.getString(3));
				tkt.setProductId(rs.getString(4));
			}

		} catch (SQLException sqle) {
			log.error("could not populate tickets from DB", sqle);
		}
	}
}
