package com.depuysynthes.scripts.showpad;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.depuysynthes.scripts.MediaBinDeltaVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.smt.sitebuilder.common.constants.Constants;

import opennlp.tools.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ReconcileReport.java<p/>
 * <b>Description</b>: Generates CSV output containing assets in Showpad 
 * that shouldn't be there...according to what we have in the local DB.
 * (not in ours = shouldn't be in theirs)
 * 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Mar 21, 2017
 ****************************************************************************/
public class ReconcileReport extends ShowpadMediaBinDecorator {


	/**
	 * @param args
	 * @throws IOException 
	 * @throws QuotaException 
	 */
	public ReconcileReport(String[] args) throws IOException {
		super(args);
	}


	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws Exception {
		//Create an instance of the MedianBinImporter
		ReconcileReport dmb = new ReconcileReport(args);
		dmb.run();
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		//load the divisions
		loadShowpadDivisionList();

		Map<String, GenericVO> data = new HashMap<>(); //divisionName, GenericVO<repo, assetList>

		for (ShowpadDivisionUtil util : divisions) {

			// get the list of assets from the database.  loads all assets for all divisions across both importCodes
			Map<String, MediaBinDeltaVO> localAssets = loadDivisionFromDB(util.getDivisionId());
			Map<String, MediaBinDeltaVO> remoteAssets = util.getAllAssets();

			//build two lists - assets only in SMT & assets only in Showpad
			List<MediaBinDeltaVO> smtAssets = new ArrayList<>(8000);
			List<MediaBinDeltaVO> showpadAssets = new ArrayList<>(8000);

			for (Map.Entry<String, MediaBinDeltaVO> entry : localAssets.entrySet()) {
				if (!remoteAssets.containsKey(entry.getKey()))
					smtAssets.add(entry.getValue());
			}
			for (Map.Entry<String, MediaBinDeltaVO> entry : remoteAssets.entrySet()) {
				if (!localAssets.containsKey(entry.getKey()))
					showpadAssets.add(entry.getValue());
			}

			//add the colated lists as a generic VO to the Map we'll send to the report
			log.debug(util.getDivisionNm() + ": smtExtras=" + smtAssets.size() + " showpadExtras=" + showpadAssets.size());
			data.put(util.getDivisionNm(), new GenericVO(smtAssets, showpadAssets));
		}

		//format the data into an excel report
		ReconcileExcelReport rpt = new ReconcileExcelReport(data);

		//email the report to the admins
		sendEmail(rpt);
	}


	/**
	 * send the email summary, with the attachment, to the desired contacts
	 * @param rpt
	 */
	protected void sendEmail(ReconcileExcelReport rpt) {
		EmailMessageVO eml = new EmailMessageVO();
		try {
			eml.setFrom("appsupport@siliconmtn.com");
			eml.addRecipients(props.getProperty("reconcileReportEmail"));
			eml.setSubject(props.getProperty("reconcileReportEmailSubj"));
			byte[] rptBytes = rpt.generateReport();
			if (rptBytes.length > 0)
				eml.addAttachment(rpt.getFileName(), rptBytes);

			eml.setHtmlBody(rpt.getEmailSummary());
			super.sendEmail(eml);

		} catch (Exception e) {
			log.error("could not send report email", e);
		}
	}


	/**
	 * calls to the local DB for all of our assets
	 * @return
	 */
	protected Map<String, MediaBinDeltaVO> loadDivisionFromDB(String divisionId) {
		Map<String, MediaBinDeltaVO> data = new HashMap<>(8000);
		StringBuilder sql = new StringBuilder(300);
		sql.append("select * from ").append(props.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("dpy_syn_mediabin a ");
		sql.append("inner join ").append(props.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("dpy_syn_showpad b on a.dpy_syn_mediabin_id=b.dpy_syn_mediabin_id ");
		sql.append("where b.division_id=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, divisionId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				MediaBinDeltaVO vo = new MediaBinDeltaVO(rs);
				String pkId = vo.getShowpadId();
				//cannot have multiple records with the same primary key
				if (StringUtil.isEmpty(pkId) || ShowpadDivisionUtil.FAILED_PROCESSING.equals(pkId))
					pkId = vo.getDpySynMediaBinId();

				data.put(pkId, vo);
			}

		} catch (SQLException sqle) {
			log.error("could not load records", sqle);
		}

		log.debug("loaded " + data.size() + " records from the database");
		return data;
	}
}