package com.wsla.util.migration;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.MapUtil;
import com.siliconmtn.util.StringUtil;
import com.wsla.util.migration.archive.AssetParserArchive;
import com.wsla.util.migration.archive.SOCMTFileVO;
import com.wsla.util.migration.archive.SOHDRFileVO;
import com.wsla.util.migration.archive.SOLNIFileVO;
import com.wsla.util.migration.archive.SOXDDFileVO;

/****************************************************************************
 * <p><b>Title:</b> ExcelImport.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Oct 10, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class ExcelImport extends AbsImporter {

	/**
	 * a list of the tickets already in the DB - we're not going to re-import these from dup/older files
	 */
	Map<String, String> existingTickets = new HashMap<>(100000);
	/**
	 * the tickets being imported this run (from HDR cascading into LNI, CMT, etc.)
	 */
	Map<String, String> tickets = new HashMap<>(100000);

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbsImporter#save()
	 */
	@Override
	void run() throws Exception {
		loadTicketIds();

		//hdr
		runHDR();

		//xdd
		runXDD();

		//lni
		runLNI();

		//comments
		runCMT();

		//assets
		AssetParserArchive ap = new AssetParserArchive(tickets);
		ap.setAttributes(dbConn, props, args);
		ap.run();

		//comment updates to LNIs - this was post-processed and only runs against the open LNI file
		//to run only this file, uncomment this line and comment all the ones above.  Adjust config path for LNI
		//runLNICommentUpdate();
	}

	private void runHDR() throws Exception {
		List<SOHDRFileVO> data = new ArrayList<>(10000);
		File[] files = listFilesMatching(props.getProperty("soHeaderFile"), "(.*)SOHDR(.*)");
		for (File f : files) {
			List<SOHDRFileVO> lst = readFile(f, SOHDRFileVO.class, SHEET_1);
			for (SOHDRFileVO vo : lst) {
				//skip any tickets already in the DB
				if (existingTickets.containsKey(vo.getSoNumber())) {
					log.warn("skipping ticket " + vo.getSoNumber() + ", already imported");
					continue;
				}
				vo.setFileName(f.getAbsolutePath());
				data.add(vo);
				tickets.put(vo.getSoNumber(), null);
			}
		}

		log.info(String.format("loaded %d rows of HDR data from %d files", data.size(), files.length));
		writeToDB(data);
	}

	private void runXDD() throws Exception {
		List<SOXDDFileVO> data = new ArrayList<>(10000);
		File[] files = listFilesMatching(props.getProperty("soExtendedDataFile"), "(.*)SOXDD(.*)");
		for (File f : files) {
			List<SOXDDFileVO> lst = readFile(f, SOXDDFileVO.class, SHEET_1);
			for (SOXDDFileVO vo : lst) {
				//skip any tickets we didn't import from HDR
				if (!tickets.containsKey(vo.getSoNumber())) continue;
				vo.setFileName(f.getAbsolutePath());
				data.add(vo);
			}
		}

		log.info(String.format("loaded %d rows of XDD data from %d files", data.size(), files.length));
		writeToDB(data);
	}

	private void runLNI() throws Exception {
		List<SOLNIFileVO> data = new ArrayList<>(10000);
		File[] files = listFilesMatching(props.getProperty("soLineItemsFile"), "(.*)SOLNI(.*)");
		for (File f : files) {
			List<SOLNIFileVO> lst = readFile(f, SOLNIFileVO.class, SHEET_1);
			for (SOLNIFileVO vo : lst) {
				//skip any tickets we didn't import from HDR
				if (!tickets.containsKey(vo.getSoNumber())) continue;
				vo.setFileName(f.getAbsolutePath());
				data.add(vo);
			}
		}

		log.info(String.format("loaded %d rows of LNI data from %d files", data.size(), files.length));
		writeToDB(data);
	}

	private void runCMT() throws Exception {
		List<SOCMTFileVO> data = new ArrayList<>(10000);
		File[] files = listFilesMatching(props.getProperty("soCommentFile"), "(.*)OSCMT(.*)");
		for (File f : files) {
			List<SOCMTFileVO> lst = readFile(f, SOCMTFileVO.class, SHEET_1);
			for (SOCMTFileVO vo : lst) {
				//skip any tickets we didn't import from HDR
				if (!tickets.containsKey(vo.getSoNumber())) continue;
				vo.setFileName(f.getAbsolutePath());
				data.add(vo);
			}
		}

		log.info(String.format("loaded %d rows of CMT data from %d files", data.size(), files.length));
		writeToDB(data);
	}

	private void runLNICommentUpdate() throws Exception {
		List<SOLNIFileVO> data = new ArrayList<>(10000);
		File[] files = listFilesMatching(props.getProperty("soLineItemsFile"), "(.*)SOLNI(.*)");
		for (File f : files) {
			List<SOLNIFileVO> lst = readFile(f, SOLNIFileVO.class, SHEET_1);
			for (SOLNIFileVO vo : lst) {
				//skip any that don't have any comments
				if (StringUtil.isEmpty(vo.getComment1())) continue;
				data.add(vo);
			}
		}

		log.info(String.format("loaded %d rows of LNI data (contianing comments) from %d files", data.size(), files.length));

		String sql = StringUtil.join(DBUtil.UPDATE_CLAUSE, schema, "wsla_sw_lni set item_text_1=?, item_text_2=?, ",
				"item_text_3=?, item_text_4=?, item_text_5=?, item_text_6=?, item_text_7=?, item_text_8=?,",
				"item_text_9=?, item_text_10=?, item_text_11=?, item_text_12=?, item_text_13=?, item_text_14=?,",
				"item_text_15=?, item_text_16=?, item_text_17=?, item_text_18=?, item_text_19=? where so_number=? and line_number=?");
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (SOLNIFileVO vo : data) {
				ps.setString(1, vo.getComment1());
				ps.setString(2, vo.getComment2());
				ps.setString(3, vo.getComment3());
				ps.setString(4, vo.getComment4());
				ps.setString(5, vo.getComment5());
				ps.setString(6, vo.getComment6());
				ps.setString(7, vo.getComment7());
				ps.setString(8, vo.getComment8());
				ps.setString(9, vo.getComment9());
				ps.setString(10, vo.getComment10());
				ps.setString(11, vo.getComment11());
				ps.setString(12, vo.getComment12());
				ps.setString(13, vo.getComment13());
				ps.setString(14, vo.getComment14());
				ps.setString(15, vo.getComment15());
				ps.setString(16, vo.getComment16());
				ps.setString(17, vo.getComment17());
				ps.setString(18, vo.getComment18());
				ps.setString(19, vo.getComment19());
				ps.setString(20, vo.getSoNumber());
				ps.setInt(21, vo.getOrderNo());
				ps.addBatch();
			}
			log.debug("executing batch of " + data.size());
			int[] cnt = ps.executeBatch();
			log.info("finished updating " + cnt.length + " LNI entries with comments");

		} catch (SQLException sqle) {
			log.error("could not update LNI comments", sqle);
		}
	}


	/**
	 * load up the ticketIds from the database to cross-reference the soNumbers
	 */
	private void loadTicketIds() {
		String sql = StringUtil.join("select so_number as key, null as value from ", schema, "wsla_sw_hdr");
		MapUtil.asMap(existingTickets, db.executeSelect(sql, null, new GenericVO()));
		log.info("loaded " + existingTickets.size() + " ticketIds from database");
	}

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbsImporter#run()
	 */
	@Override
	void save() throws Exception {
		throw new RuntimeException("not implemented");
	}
}
