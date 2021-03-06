package com.wsla.util.migration;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.MapUtil;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.ticket.TicketCommentVO;
import com.wsla.util.migration.vo.SOLNIFileVO;

/****************************************************************************
 * <p><b>Title:</b> SOLineItemBillableCodes.java</p>
 * <p><b>Description:</b> Phase 2 re-run of the LNI file.  This one UPDATES the existing 
 * records to replace billable codes for activities.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Sep 30, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class SOLineItemBillableCodes extends AbsImporter {

	private List<SOLNIFileVO> data = new ArrayList<>(500000);
	private Map<String, String> ticketIds = new HashMap<>(100000);
	private Map<String, String> ticketCommentIds = new HashMap<>(800000);
	private Map<String, String> activityMap = new HashMap<>(200);

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		File[] files = listFilesMatching(props.getProperty("soLineItemsFile"), "(.*)SOLNI(.*)");

		for (File f : files)
			data.addAll(readFile(f, SOLNIFileVO.class, SHEET_1));

		log.info(String.format("loaded %d rows from %d files", data.size(), files.length));
		loadTicketIds();
		loadTicketCommentIds();
		loadActivities();

		//Note we don't have a delete here; deleting the tickets will cascade into the tables affecting LineItems

		save();
	}


	/**
	 * update the ticket comments
	 * @param data
	 * @throws Exception 
	 */
	@Override
	protected void save() throws Exception {
		List<TicketCommentVO> activities = new ArrayList<>(data.size());
		Set<String> missingTicketIds = new HashSet<>(data.size());

		for (SOLNIFileVO vo : data) {
			if (!isImportable(vo.getSoNumber())) continue;

			if (vo.isService()) {
				TicketCommentVO cmt = new TicketCommentVO();
				transposeCommentData(vo, cmt);

				//we can only update records where we married their unique key - report all failures
				if (!StringUtil.isEmpty(cmt.getTicketCommentId()) && !StringUtil.isEmpty(cmt.getActivityType())) {
					activities.add(cmt);
				} else if (StringUtil.isEmpty(cmt.getTicketCommentId())) {
					missingTicketIds.add(cmt.getComment());
				}
			}
		}
		log.info(String.format("found %d service line items (activities) to update", activities.size()));

		if (!missingTicketIds.isEmpty()) {
			log.warn(String.format("skipping %d tickets that do not exist", missingTicketIds.size()));
			for (String id : missingTicketIds)
				log.warn(id);
		}

		saveActivities(activities);
	}


	/**
	 * @param activities
	 */
	private void saveActivities(List<TicketCommentVO> activities) {
		String sql = StringUtil.join(DBUtil.UPDATE_CLAUSE, schema, "wsla_ticket_comment set activity_type_cd=? where ticket_comment_id=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (TicketCommentVO vo : activities) {
				ps.setString(1, vo.getActivityType());
				ps.setString(2, vo.getTicketCommentId());
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.info(String.format("updated %d ticket activities (_comments tbl) with proper comments", cnt.length));

		} catch (SQLException sqle) {
			log.error("could not update activity comments", sqle);
		}
	}


	/**
	 * activities:
	 * Transpose and enhance the data we get from the import file into what the new schema needs
	 * @param dataVo
	 * @param commentVO
	 * @return
	 **/
	private TicketCommentVO transposeCommentData(SOLNIFileVO dataVo, TicketCommentVO vo) {
		vo.setTicketId(ticketIds.get(dataVo.getSoNumber())); //transposed
		vo.setCreateDate(dataVo.getChronoReceivedDate());
		vo.setActivityType(activityMap.get(dataVo.getProductId()));
		String key = vo.getTicketId()+"--"+Convert.formatDate(vo.getCreateDate(), "yyyy-MM-dd HH:mm:ss");
		vo.setComment(dataVo.getSoNumber() + "~"+key);
		vo.setTicketCommentId(ticketCommentIds.get(key));
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


	/**
	 * Populate the Map<Ticket#||createDate, TicketCommentId>
	 */
	private void loadTicketCommentIds() {
		String sql = StringUtil.join("select ticket_id||'--'||to_char(create_dt, 'yyyy-MM-dd HH24:MI:SS') as key, ticket_comment_id as value from ", schema, "wsla_ticket_comment");
		MapUtil.asMap(ticketCommentIds, db.executeSelect(sql, null, new GenericVO()));
		log.debug(String.format("loaded %d ticketCommentIds", ticketCommentIds.size()));
	}


	/**
	 * Populate the Map of activities.
	 * These came from the master listing Excel file - from Steve.
	 * Take what doesn't align in Cypher and throw them into 'other'.
	 */
	private void loadActivities() {
		activityMap.put("CHATENTR01","EMAIL_END_USER");
		activityMap.put("CHATENTR02","EMAIL_CAS");
		activityMap.put("CHATENTR03","EMAIL_RETAILER");
		activityMap.put("CHATENTR04","EMAIL_OEM");
		activityMap.put("CHATENTR05","EMAIL_ESC_TECH");
		activityMap.put("CHATSALD01","EMAIL_OUT_END_USER");
		activityMap.put("CHATSALD02","EMAIL_OUT_CAS");
		activityMap.put("CHATSALD03","EMAIL_OUT_RETAILER");
		activityMap.put("CHATSALD04","EMAIL_OUT_OEM");
		activityMap.put("CHATSALD05","EMAIL_OUT_ESC_TECH");
		activityMap.put("CONTINILAM","PHONE_END_USER");
		activityMap.put("CONTINIWEB","EMAIL_END_USER");
		activityMap.put("EMLENTR01","EMAIL_END_USER");
		activityMap.put("EMLENTR02","EMAIL_CAS");
		activityMap.put("EMLENTR03","EMAIL_RETAILER");
		activityMap.put("EMLENTR04","EMAIL_OEM");
		activityMap.put("EMLENTR05","EMAIL_ESC_TECH");
		activityMap.put("EMLSALD01","EMAIL_OUT_END_USER");
		activityMap.put("EMLSALD02","EMAIL_OUT_CAS");
		activityMap.put("EMLSALD03","EMAIL_OUT_RETAILER");
		activityMap.put("EMLSALD04","EMAIL_OUT_OEM");
		activityMap.put("EMLSALD05","EMAIL_OUT_ESC_TECH");
		activityMap.put("EMLSALD06","EMAIL_OUT_ESC_MGMT");
		activityMap.put("LLAMENTR01","PHONE_END_USER");
		activityMap.put("LLAMENTR02","PHONE_CAS");
		activityMap.put("LLAMENTR03","PHONE_RETAILER");
		activityMap.put("LLAMENTR04","PHONE_OEM");
		activityMap.put("LLAMENTR05","PHONE_ESC_TECH");
		activityMap.put("LLAMSALD01","PHONE_OUT_END_USER");
		activityMap.put("LLAMSALD02","PHONE_OUT_CAS");
		activityMap.put("LLAMSALD03","PHONE_OUT_RETAILER");
		activityMap.put("LLAMSALD04","PHONE_OUT_OEM");
		activityMap.put("LLAMSALD05","PHONE_OUT_ESC_TECH");
		activityMap.put("LLAMSALD06","PHONE_OUT_ESC_MGMT");
		activityMap.put("ZENENTR01","EMAIL_END_USER");
		activityMap.put("ZENENTR02","EMAIL_CAS");
		activityMap.put("ZENENTR03","EMAIL_RETAILER");
		activityMap.put("ZENENTR04","EMAIL_OEM");
		activityMap.put("ZENENTR05","EMAIL_ESC_TECH");
		activityMap.put("ZENSALD01","EMAIL_OUT_END_USER");
		activityMap.put("ZENSALD02","EMAIL_OUT_CAS");
		activityMap.put("ZENSALD03","EMAIL_OUT_RETAILER");
		activityMap.put("ZENSALD04","EMAIL_OUT_OEM");
		activityMap.put("ZENSALD05","EMAIL_OUT_ESC_TECH");
		activityMap.put("ZENSALD06","EMAIL_OUT_ESC_MGMT");

		log.debug("loaded " + activityMap.size() + " activities");
	}
}
