package com.wsla.util.migration;

import java.io.File;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import com.siliconmtn.data.GenericVO;
import com.siliconmtn.util.MapUtil;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.ticket.TicketCommentVO;
import com.wsla.util.migration.vo.SOCMTFileVO;

/****************************************************************************
 * <p><b>Title:</b> SOComments.java</p>
 * <p><b>Description:</b> Ingest data from the ticket comment files.  Saves to ticket_comment.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Feb 1, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class SOComments extends AbsImporter {

	private static Map<String, String> ticketMap = new HashMap<>(30000, 1);
	private List<SOCMTFileVO> data = new ArrayList<>(50000);

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		File[] files = super.listFilesMatching(props.getProperty("soCommentFile"), "(.*)OSCMT(.*)");

		for (File f : files)
			data.addAll(readFile(f, SOCMTFileVO.class, SHEET_1));
		log.info(String.format("loaded %d records from %d comment files", data.size(), files.length));

		pruneData();

		//transpose soNumbers into ticketIds
		loadTicketIds();
		setTicketIds();

		//save the data
		save();
		saveTicketDesc();
	}


	/**
	 * 
	 */
	private void pruneData() {
		List<SOCMTFileVO> newData = new ArrayList<>(data.size());
		for (SOCMTFileVO vo : data) {
			if (isImportable(vo.getSoNumber()))
				newData.add(vo);
		}
		log.info(String.format("pruned data from %d to %d tickets", data.size(), newData.size()));
		data = newData;
	}

	/**
	 * Loop through the tickets/rows and update the ticket data for each using 
	 * the @Column annotations found on the bean
	 * @param data
	 * @throws Exception 
	 */
	@Override
	protected void save() throws Exception {
		List<TicketCommentVO> batch = new ArrayList<>(50000);

		//loop the tickets, add each one's comment to the larger batch
		for (SOCMTFileVO row : data) {
			TicketCommentVO cmt = row.getCobinedComment();
			if (cmt != null)
				batch.add(cmt);
		}

		try {
			writeToDB(batch);
		} catch (Exception e) {
			log.error("could not save ticket comments", e);
		}
	}


	/**
	 * Save comment#1 as the ticket's description - which is the user-reported problem
	 */
	private void saveTicketDesc() {
		String sql = StringUtil.join("update ", schema, "wsla_ticket set desc_txt=? where ticket_id=?");
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (SOCMTFileVO row : data) {
				String cmt = row.getCombinedComment();
				if (!StringUtil.isEmpty(cmt)) {
					ps.setString(1, cmt);
					ps.setString(2, row.getTicketId());
					ps.addBatch();
				}
			}
			int[] cnt = ps.executeBatch();
			log.info(String.format("Added descriptions to %d tickets", cnt.length));

		} catch (Exception e) {
			log.error("could not save ticket descriptions", e);
		}
	}


	/**
	 * transpose ticketIds.  If some are missing print them, then throw a Runtime to stop the script
	 */
	private void setTicketIds() {
		Set<String> blanks = new HashSet<>();
		for (SOCMTFileVO row : data) {
			String ticketId = ticketMap.get(row.getSoNumber());
			if (!StringUtil.isEmpty(ticketId)) {
				row.setTicketId(ticketId);
			} else {
				blanks.add(row.getSoNumber());
			}
		}
		if (blanks.isEmpty()) return;

		//print the blanks, then throw an error so they can be added, or removed from the file
		log.error("\nMISSING TICKETS.  COMMENTS FOR THESE TICKETS ARE BEING IGNORED:");
		for (String s : blanks)
			System.err.println(s);

		//remove any records for tickets we can't save
		ListIterator<SOCMTFileVO> iter = data.listIterator();
		while (iter.hasNext()) {
			SOCMTFileVO vo = iter.next();
			if (blanks.contains(vo.getSoNumber()))
				iter.remove();
		}
	}


	/**
	 * load up the ticketIds from the database to cross-reference the soNumbers
	 */
	private void loadTicketIds() {
		String sql = StringUtil.join("select ticket_no as key, ticket_id as value from ", schema, "wsla_ticket");
		MapUtil.asMap(ticketMap, db.executeSelect(sql, null, new GenericVO()));
		log.info("loaded " + ticketMap.size() + " ticketIds from database");
	}
}
