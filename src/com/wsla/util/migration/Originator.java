package com.wsla.util.migration;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.StringUtil;
import com.wsla.util.migration.vo.SOXDDFileVO;

/****************************************************************************
 * <p><b>Title:</b> Originator.java</p>
 * <p><b>Description:</b> Phase 2 re-run to fix the originators.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Oct 04, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class Originator extends AbsImporter {

	private List<SOXDDFileVO> data = new ArrayList<>(50000);
	private static Map<String, String> userMap = new HashMap<>(70);

	static {
		userMap.put("AA-NC","WSLA_AA-NC");
		userMap.put("AL-NC","WSLA_AL-NC");
		userMap.put("AC-NC","WSLA_AC-NC");
		userMap.put("ATA-NC","WSLA_ATA-NC");
		userMap.put("AR-NC","WSLA_AR-NC");
		userMap.put("AX-NC","WSLA_AX-NC");
		userMap.put("CO-NC","WSLA_CO-NC");
		userMap.put("CO","WSLA_CO-NC");
		userMap.put("CR-NC","WSLA_CR-NC");
		userMap.put("CP-NC","WSLA_CP-NC");
		userMap.put("DS-NC","WSLA_DS-NC");
		userMap.put("DA-NC","WSLA_DA-NC");
		userMap.put("DB-NC","WSLA_DB-NC");
		userMap.put("DC-NC","WSLA_DC-NC");
		userMap.put("DL-NC","WSLA_DL-NC");
		userMap.put("DT-NC","WSLA_DT-NC");
		userMap.put("DH-NC","WSLA_DH-NC");
		userMap.put("ES-NC","WSLA_ES-NC");
		userMap.put("ES-N","WSLA_ES-NC");
		userMap.put("EC-NC","WSLA_EC-NC");
		userMap.put("EB-NC","WSLA_EB-NC");
		userMap.put("EM-NC","WSLA_EM-NC");
		userMap.put("EM-NC.","WSLA_EM-NC");
		userMap.put("GR-NC","WSLA_GR-NC");
		userMap.put("JA-NC","WSLA_JA-NC");
		userMap.put("JR-NC","WSLA_JR-NC");
		userMap.put("JV-NC","df503f4f9bff1f07ac10023924c9f9b4");
		userMap.put("JG-NC","WSLA_JG-NC");
		userMap.put("JL-NC","WSLA_JL-NC");
		userMap.put("JLA-NC","WSLA_JLA-NC");
		userMap.put("JM-NC","WSLA_JM-NC");
		userMap.put("JB-NC","WSLA_JB-NC");
		userMap.put("KG-NC","WSLA_KG-NC");
		userMap.put("KJ-NC","WSLA_KJ-NC");
		userMap.put("KC-NC","WSLA_KC-NC");
		userMap.put("KC--NC","WSLA_KC-NC");
		userMap.put("LG-NC","WSLA_LG-NC");
		userMap.put("LG-N","WSLA_LG-NC");
		userMap.put("MA-NC","WSLA_MA-NC");
		userMap.put("MH-NC","WSLA_MH-NC");
		userMap.put("MO-NC","WSLA_MO-NC");
		userMap.put("MS-NC","WSLA_MS-NC");
		userMap.put("NV-NC","WSLA_NV-NC");
		userMap.put("NT-NC","WSLA_NT-NC");
		userMap.put("OL-NC","WSLA_OL-NC");
		userMap.put("OR-NC","WSLA_OR-NC");
		userMap.put("PR-NC","WSLA_PR-NC");
		userMap.put("RJ-NC","WSLA_RJ-NC");
		userMap.put("RS-NC","WSLA_RS-NC");
		userMap.put("RS-M","WSLA_RS-NC");
		userMap.put("SC-NC","WSLA_SC-NC");
		userMap.put("SP-NC","WSLA_SP-NC");
		userMap.put("SL-NC","WSLA_SL-NC");
		userMap.put("SE-NC","WSLA_SE-NC");
		userMap.put("TS-NC","5be22e378ac4f422ac10021b88c59c7b");
		userMap.put("VR-NC","WSLA_VR-NC");
		userMap.put("VR","WSLA_VR-NC");
		userMap.put("VH-NC","WSLA_VH-NC");
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

		pruneData();

		save();
	}


	/**
	 * 
	 */
	private void pruneData() {
		List<SOXDDFileVO> newData = new ArrayList<>(data.size());
		for (SOXDDFileVO vo : data) {
			if (isImportable(vo.getSoNumber()))
				newData.add(vo);
		}
		log.info(String.format("pruned Originator scope from %d to %d tickets", data.size(), newData.size()));
		data = newData;
	}


	/**
	 * update the ticket originators
	 * @param data
	 * @throws Exception 
	 */
	@Override
	protected void save() throws Exception {
		String sql = StringUtil.join(DBUtil.UPDATE_CLAUSE, schema, "wsla_ticket set originator_user_id=? where ticket_id=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (SOXDDFileVO vo : data) {
				if (!isImportable(vo.getSoNumber())) continue;
				ps.setString(1, userMap.getOrDefault(vo.getSwUserId(), SOHeader.LEGACY_USER_ID));
				ps.setString(2, vo.getSoNumber());
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.info(String.format("updated %d ticket originators based on %d XDD entries", cnt.length, data.size()));

		} catch (SQLException sqle) {
			log.error("could not update ticket originators", sqle);
		}
	}
}
