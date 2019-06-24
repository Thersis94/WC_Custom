package com.wsla.util.migration;

import java.io.File;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.ticket.TicketDataVO;
import com.wsla.util.migration.vo.SOExtendedFileVO;

/****************************************************************************
 * <p><b>Title:</b> SOLineItems.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Feb 1, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class SOExtendedData extends AbsImporter {

	private List<SOExtendedFileVO> data = new ArrayList<>(50000);

	private static Map<String, String> ticketMap = new HashMap<>();


	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		File[] files = super.listFilesMatching(props.getProperty("soExtendedDataFile"), "(.*)SOXDD(.*)");

		for (File f : files)
			data.addAll(readFile(f, SOExtendedFileVO.class, SHEET_1));

		log.debug(data.size());

		loadTicketIds();

		save();
	}


	/**
	 * Loop through the tickets/rows and update the ticket data for each using 
	 * the @Column annotations found on the bean
	 * @param data
	 * @throws Exception 
	 */
	@Override
	protected void save() throws Exception {
		//transpose soNumbers into ticketIds
		setTicketIds();

		//loop the tickets, save each individually so we can overcome failures
		for (SOExtendedFileVO row : data) {
			Map<String, TicketDataVO> tktData = findDataCols(row);
			if (tktData == null || tktData.isEmpty() || StringUtil.isEmpty(row.getSoNumber())) continue;
			log.debug(String.format("found %d data points in ticket %s", tktData.size(), row.getSoNumber()));

			try {
				deleteTicketData(tktData.keySet(), row.getSoNumber());
				writeToDB(new ArrayList<>(tktData.values()));
			} catch (Exception e) {
				log.error("could not save ticket data", e);
			}
		}
	}


	/**
	 * transpose ticketIds.  If some are missing print them, then throw a Runtime to stop the script
	 */
	private void setTicketIds() {
		Set<String> blanks = new HashSet<>();
		for (SOExtendedFileVO row : data) {
			String soNum = row.getSoNumber();
			row.setSoNumber(ticketMap.get(soNum));
			if (StringUtil.isEmpty(row.getSoNumber()))
				blanks.add(soNum);
		}
		if (blanks.isEmpty()) return;

		//print the blanks, then throw an error so they can be added, or removed from the file
		log.error("\nMISSING TICKETS, CANNOT PROCEED "
				+ "UNTIL THESE ARE ADDED, OR REMOVED FROM THE XDD FILE:");
		for (String s : blanks) 
			System.err.println(s);
		throw new RuntimeException();
	}


	/**
	 * load up the ticketIds from the database to cross-reference the soNumbers
	 */
	private void loadTicketIds() {
		String sql = StringUtil.join("select ticket_no as key, ticket_id as value from ", schema, "wsla_ticket");
		DBProcessor db = new DBProcessor(dbConn, schema);
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
	private void deleteTicketData(Set<String> attributeIds, String ticketId) {
		String sql = StringUtil.join("delete from ", schema, "wsla_ticket_data where attribute_cd=? and ticket_id=?");
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (String attributeId : attributeIds) {
				ps.setString(1, attributeId);
				ps.setString(2, ticketId);
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.debug(String.format("deleted %d existing attributes from ticket %s", cnt.length, ticketId));
		} catch (Exception e) {
			log.error("could not delete ticket data for attributes", e);
		}
	}


	/**
	 * Find methods annotated @Column and return a list of those fields as data VOs
	 * @param row
	 * @return
	 */
	private Map<String, TicketDataVO> findDataCols(SOExtendedFileVO row) {
		Map<String, TicketDataVO> attribs = new HashMap<>(50);

		for (Method m : row.getClass().getMethods()) {
			//determine if this method is one we want to process
			Column anno = m.getAnnotation(Column.class);
			if (anno == null || anno.name() == null) continue;

			try {
				Object value = m.invoke(row);
				String valueStr = formatValue(value);
				if (!StringUtil.isEmpty(valueStr) && !isBogusData(valueStr)) {
					//create a TicketDataVO and add it to the stack
					TicketDataVO vo = new TicketDataVO();
					vo.setTicketId(row.getSoNumber());
					vo.setAttributeCode(anno.name());
					vo.setValue(valueStr);
					attribs.put(vo.getAttributeCode(), vo);
				}

			} catch (Exception e) {
				log.error("could not read data value", e);
			}
		}

		return attribs;
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
