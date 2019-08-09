package com.wsla.util.migration;

import java.io.File;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketAssignmentVO;
import com.wsla.data.ticket.TicketAssignmentVO.TypeCode;
import com.wsla.data.ticket.TicketDataVO;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.util.migration.vo.SOXDDFileVO;

/****************************************************************************
 * <p><b>Title:</b> SOLineItems.java</p>
 * <p><b>Description:</b> Ingest data from the XDD migration files.  Most of these go in ticket_data.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Feb 1, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class SOExtendedData extends AbsImporter {

	private List<SOXDDFileVO> data = new ArrayList<>(50000);

	private static Map<String, String> ticketMap = new HashMap<>(30000, 1);

	/**
	 * Retailer locations - fudged data that didn't exist in SW.  Bind all tickets to the 1st/default 
	 * store for the given retailer.  This is done via static map so it's always consistent.
	 */
	private static Map<String, String> retailLocnMap = new HashMap<>(20, 1);

	private Map<String, String> defectMap;
	private String deleteSql;
	private String deleteNote = "deleted %d existing attributes from ticket %s";

	private int saveCnt;
	private int delCnt;

	private UUIDGenerator uuid = new UUIDGenerator();

	static {
		retailLocnMap.put("16","90133a088d6121f57f0001018b72cfa3");
		retailLocnMap.put("AB","90133a088d6121f57f0001018b72cfa3");
		retailLocnMap.put("28","276e3b428da92ff77f000101e7198688");
		retailLocnMap.put("17","be47aaa18d6c3b437f000101acac5721");
		retailLocnMap.put("18","45f3c6378d8793bf7f000101eebc6788");
		retailLocnMap.put("1","b373f2e58dba2cad7f000101c13b8a50");
		retailLocnMap.put("AA","b373f2e58dba2cad7f000101c13b8a50");
		retailLocnMap.put("AC","b373f2e58dba2cad7f000101c13b8a50");
		retailLocnMap.put("20","447222a98d9fe8357f000101485cff25");
		retailLocnMap.put("15","a1dd8e82fdb01aacac10028449ab6ec7");
		retailLocnMap.put("5","b17a2df0fdbcf9c8ac100284f6ad2f92");
		retailLocnMap.put("36","28593420v94b673dac100239f2a953d5");
		retailLocnMap.put("33","4c5cebdav7892982ac1002398222deea");
		retailLocnMap.put("32","19ae559dvd70987cac100239a6316f0d");
		retailLocnMap.put("2","595d69fbvc9ceb05ac10023949ff1fbf");
		retailLocnMap.put("31","973415dcv8e21d5cac100239dd1f005f");
		retailLocnMap.put("21","f7626a309c5bdb94ac100239e45e91a3");
		retailLocnMap.put("34","a71bc9cav7a53611ac100239b5931c20");
		retailLocnMap.put("35","aa3a9038v7b3a34aac10023937c64599");
		retailLocnMap.put("26","RETAILER_AMAZON_1");
		retailLocnMap.put("AE","RETAILER_AMAZON_1");
		retailLocnMap.put("11","RETAILER_ANDE_1");
		retailLocnMap.put("9","RETAILER_CASA_BLNC_1");
		retailLocnMap.put("22","RETAILER_COMM_MX_1");
		retailLocnMap.put("19","RETAILER_COSTCO_1");
		retailLocnMap.put("AD","RETAILER_COSTCO_1");
		retailLocnMap.put("25","RETAILER_ELEC_MERI_1");
		retailLocnMap.put("12","RETAILER_ESC_1");
		retailLocnMap.put("13","RETAILER_KARMA_1");
		retailLocnMap.put("7","RETAILER_MAX_CNTRL_1");
		retailLocnMap.put("24","RETAILER_MAYORAMSA_1");
		retailLocnMap.put("10","RETAILER_MONGE_1");
		retailLocnMap.put("27","RETAILER_MUEBLES_AMER_1");
		retailLocnMap.put("4","RETAILER_MX_DESCON_1");
		retailLocnMap.put("6","RETAILER_PLZ_LAMA_1");
		retailLocnMap.put("23","RETAILER_PRICE_SMART_1");
		retailLocnMap.put("37","RETAILER_SEARS_1");
		retailLocnMap.put("8","RETAILER_SHOPPERS_1");
		retailLocnMap.put("14","RETAILER_SS_UNIDOS_1");
		retailLocnMap.put("3","RETAILER_USA_1");
	}

	@Override
	protected void setAttributes(Connection conn, Properties props, String[] args) {
		super.setAttributes(conn, props, args);
		deleteSql = StringUtil.join("delete from ", schema, "wsla_ticket_data where attribute_cd=? and ticket_id=?");
	}

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		File[] files = super.listFilesMatching(props.getProperty("soExtendedDataFile"), "(.*)SOXDD(.*)");

		for (File f : files)
			data.addAll(readFile(f, SOXDDFileVO.class, SHEET_1));
		log.info(String.format("loaded %d records from %d XDD files", data.size(), files.length));

		defectMap = SOHeader.loadDefectCodes(db, schema);

		//transpose soNumbers into ticketIds
		loadTicketIds();
		setTicketIds();

		//save the data - involves ticket_data and ticket_ledger
		save();

		//update purchase dates on the tickets
		updatePurchaseDates();

		//update all ledger entries to be dispositioned by the SW User ID
		updateLedgerDispositions();

		//affiliate the Retailers to the tickets
		addRetailerAssignments();
	}

	/**
	 * Loop through the tickets/rows and update the ticket data for each using 
	 * the @Column annotations found on the bean
	 * @param data
	 * @throws Exception 
	 */
	@Override
	protected void save() throws Exception {
		//loop the tickets, save each individually so we can overcome failures
		for (SOXDDFileVO row : data) {
			Map<String, TicketDataVO> tktData = findDataCols(row);
			if (tktData == null || tktData.isEmpty() || StringUtil.isEmpty(row.getSoNumber())) continue;
			log.debug(String.format("found %d data points in ticket %s", tktData.size(), row.getSoNumber()));

			try {
				delCnt += deleteTicketData(tktData.keySet(), row.getSoNumber());
				saveCnt += writeToDB(new ArrayList<>(tktData.values()));
			} catch (Exception e) {
				log.error("could not save ticket data", e);
			}
		}
		log.info(String.format("replaced %d ticket_data records", delCnt));
		log.info(String.format("added %d ticket_data records", saveCnt-delCnt));
	}


	/**
	 * Create a ticket_assignment to the retailers
	 * @throws Exception 
	 */
	private void addRetailerAssignments() throws Exception {
		List<TicketAssignmentVO> assgs = new ArrayList<>(data.size());
		TicketAssignmentVO vo;
		String retailerId;
		for (SOXDDFileVO tkt : data) {
			if (StringUtil.isEmpty(tkt.getRetailer()) || isBogusData(tkt.getRetailer())) continue;

			retailerId = retailLocnMap.get(tkt.getRetailer());
			if (!StringUtil.isEmpty(retailerId)) {
				vo = new TicketAssignmentVO();
				vo.setTypeCode(TypeCode.RETAILER);
				vo.setTicketId(tkt.getSoNumber());
				vo.setLocationId(retailerId);
				assgs.add(vo);
			} else {
				log.warn("not a retail location " + tkt.getRetailer());
			}
		}
		writeToDB(assgs);

		//these also get saved in wsla_ticket.retailer_id
		String sql = StringUtil.join(DBUtil.UPDATE_CLAUSE, schema, "wsla_ticket set retailer_id=? where ticket_id=?");
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (TicketAssignmentVO assg : assgs) {
				ps.setString(1, assg.getLocationId());
				ps.setString(2, assg.getTicketId());
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.info(String.format("updated %d tickets with retailer_id", cnt.length));

		} catch (SQLException sqle) {
			log.error("could not save ticket retailer_id", sqle);
		}
	}


	/**
	 * update the ticket_ledger entries for the tickets (entries where dispositioned_by_id=null) 
	 * to reference the default SW userId
	 */
	private void updateLedgerDispositions() {
		String sql = StringUtil.join(DBUtil.UPDATE_CLAUSE, schema, "wsla_ticket_ledger a ",
				"set disposition_by_id=? from ", schema, "wsla_ticket t ",
				"where a.ticket_id=t.ticket_id and t.historical_flg=1 and a.disposition_by_id is null");
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, SOHeader.LEGACY_USER_ID);
			int cnt = ps.executeUpdate();
			log.info(String.format("updated %d ledger dispositions", cnt));
		} catch (Exception e) {
			log.error("could not save ledger dispositions", e);
		}
	}


	/**
	 * update the ticket table with purchase dates for the units we have data for
	 */
	private void updatePurchaseDates() {
		String sql = StringUtil.join(DBUtil.UPDATE_CLAUSE, schema, "wsla_ticket set purchase_dt=? where ticket_id=?");
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (SOXDDFileVO vo : data) {
				if (vo.getPurchaseDate() != null) {
					ps.setDate(1, Convert.formatSQLDate(vo.getPurchaseDate()));
					ps.setString(2, vo.getSoNumber());
					ps.addBatch();
				}
			}
			int[] cnt = ps.executeBatch();
			log.info(String.format("updated %d tickets with purchase dates", cnt.length));

		} catch (Exception e) {
			log.error("could not save purchase dates", e);
		}
	}


	/**
	 * transpose ticketIds.  If some are missing print them, then throw a Runtime to stop the script
	 */
	private void setTicketIds() {
		Set<String> blanks = new HashSet<>();
		for (SOXDDFileVO row : data) {
			String ticketId = ticketMap.get(row.getSoNumber());
			if (!StringUtil.isEmpty(ticketId)) {
				row.setSoNumber(ticketId);
			} else {
				blanks.add(row.getSoNumber());
			}
		}
		if (blanks.isEmpty()) return;

		//print the blanks, then throw an error so they can be added, or removed from the file
		log.error("\nMISSING TICKETS.  DATA FOR THESE TICKETS IS BEING IGNORED:");
		for (String s : blanks)
			System.err.println(s);

		//remove any records for tickets we can't save
		ListIterator<SOXDDFileVO> iter = data.listIterator();
		while (iter.hasNext()) {
			SOXDDFileVO vo = iter.next();
			if (blanks.contains(vo.getSoNumber()))
				iter.remove();
		}
	}


	/**
	 * load up the ticketIds from the database to cross-reference the soNumbers
	 */
	private void loadTicketIds() {
		String sql = StringUtil.join("select ticket_no as key, ticket_id as value from ", schema, "wsla_ticket");
		List<GenericVO> tkts = db.executeSelect(sql, null, new GenericVO());

		for (GenericVO vo : tkts)
			ticketMap.put(vo.getKey().toString(), vo.getValue().toString());

		log.info("loaded " + ticketMap.size() + " ticketIds from database");
	}


	/**
	 * Delete the data for attributes XYZ on ticket ABC.  This is run prior to insertion
	 * to prevent duplicate data.  It also makes this import script re-runnable.
	 * @param attributeIds
	 * @param ticketId
	 */
	private int deleteTicketData(Set<String> attributeIds, String ticketId) {
		try (PreparedStatement ps = dbConn.prepareStatement(deleteSql)) {
			for (String attributeId : attributeIds) {
				ps.setString(1, attributeId);
				ps.setString(2, ticketId);
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.debug(String.format(deleteNote, cnt.length, ticketId));
			return cnt.length;
		} catch (Exception e) {
			log.error("could not delete ticket data for attributes", e);
		}
		return 0;
	}


	/**
	 * Find methods annotated @Column and return a list of those fields as data VOs
	 * @param row
	 * @return
	 */
	private Map<String, TicketDataVO> findDataCols(SOXDDFileVO row) {
		Map<String, TicketDataVO> attribs = new HashMap<>(50);

		for (Method m : row.getClass().getMethods()) {
			//determine if this method is one we want to process
			Column anno = m.getAnnotation(Column.class);
			if (anno == null || anno.name() == null) continue;

			try {
				Object value = m.invoke(row);
				//use the isIdentity() hook to know when we have to transpose defect codes
				String valueStr = anno.isIdentity() ? lookupDefectCode((String)value) : formatValue(value);
				if (StringUtil.isEmpty(valueStr) || isBogusData(valueStr)) continue;

				//create a TicketDataVO and add it to the stack
				TicketDataVO vo = new TicketDataVO();
				vo.setTicketId(row.getSoNumber());
				vo.setAttributeCode(anno.name());
				vo.setValue(valueStr);
				//possibly create a ledger entry if isAutoGen is set true
				if (anno.isAutoGen())
					vo.setLedgerEntryId(createLedgerEntry(row.getSoNumber()));

				attribs.put(vo.getAttributeCode(), vo);

			} catch (Exception e) {
				log.error("could not read data value", e);
			}
		}

		return attribs;
	}


	/**
	 * Creates a ledger entry for the ticket to bind to the ticket_data table.
	 * Typically these trigger for attributes which are event-based, like "the user uploaded a receipt".
	 * @param soNumber
	 * @return
	 * @throws DatabaseException
	 * @throws InvalidDataException
	 */
	private String createLedgerEntry(String ticketId) throws Exception {
		TicketLedgerVO vo = new TicketLedgerVO();
		vo.setTicketId(ticketId);
		vo.setDispositionBy(SOHeader.LEGACY_USER_ID);
		vo.setSummary("Usuario Agrega Evidencia");
		vo.setStatusCode(StatusCode.USER_DATA_APPROVAL_PENDING);
		vo.setLedgerEntryId(uuid .getUUID());

		db.save(vo);
		return vo.getLedgerEntryId();
	}

	/**
	 * transpose partial defect & repair codes to the full pkId values stored in our DB.
	 * Note: this method is duplicated in SOHeader - change both when revising
	 * @param problemCode
	 * @return
	 */
	private String lookupDefectCode(String partialDefectCode) {
		if (StringUtil.checkVal(partialDefectCode).trim().isEmpty()) return null;
		if (partialDefectCode.matches("0+")) partialDefectCode="0";

		String code = defectMap.get(partialDefectCode);
		if (StringUtil.isEmpty(code)) {
			log.warn("missing defect code " + partialDefectCode);
			return partialDefectCode; //preserve what we have, these can be fixed via query manually
		}
		return code;
	}


	/**
	 * Cleanup the data value, format any dates for consistency 
	 * @param value
	 * @param valueStr
	 * @return
	 */
	private String formatValue(Object value) {
		if (value != null && value instanceof Date) {
			return Convert.formatDate((Date) value, Convert.DATE_DASH_PATTERN);
		}
		return StringUtil.checkVal(value);
	}


	/**
	 * @param value
	 * @return
	 */
	private boolean isBogusData(String value) {
		return value.matches("0+") || "0.0".equals(value) || "N/A".equals(value);
	}
}
