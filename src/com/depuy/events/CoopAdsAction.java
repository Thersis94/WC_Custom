package com.depuy.events;

// JDK
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// SMT BaseLibs
import com.depuy.events.vo.CoopAdVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.databean.FilePartDataBean;

// SB Libs
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.action.FileLoader;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: CoopAdsAction.java
 * <p/>
 * <b>Description: Manages Co-op ads for the DePuy Events/Postcards.</b>
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author James McKain
 * @version 1.0
 * @since May 14, 2008
 ****************************************************************************/
public class CoopAdsAction extends SBActionAdapter {

	public static final String RETR_ADS = "retrAds";

	// Ad status levels
	public static final int NO_ADS = 15;
	public static final int CLIENT_SUBMITTED = 1;
	public static final int PENDING_CLIENT_APPROVAL = 2;
	public static final int CLIENT_APPROVED_AD = 3;
	public static final int CLIENT_DECLINED_AD = 4;
	public static final int CLIENT_PAYMENT_RECD = 5;
	public static final int PENDING_SURGEON_APPROVAL = 7; //CFSEM only
	public static final int REMIND_ME_LATER = 10;

	public CoopAdsAction() {
		super();
	}

	public CoopAdsAction(ActionInitVO arg0) {
		super(arg0);
	}

	public void build(SMTServletRequest req) throws ActionException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		String reqType = StringUtil.checkVal(req.getParameter("reqType"));

		// create the VO based on the incoming request object
		CoopAdVO vo = new CoopAdVO(req);

		// record the transaction
		if (reqType.equals("coopAdsApproval")) {
			this.saveAdApproval(req);
		} else if (reqType.equals("coopAdsSurgeonApproval")) {
			this.saveSurgeonsAdApproval(req);
		} else if (!reqType.equals("coopAdsSendApproval")) {
			if (reqType.equals("coopAdsSubmit")) {
				vo.setStatusFlg(CLIENT_SUBMITTED);
			} else if (reqType.equals("radioAdsSubmit")) {
				vo.setStatusFlg(CLIENT_APPROVED_AD);
			}

			vo = this.saveAd(req, site, vo);
		}
		req.setAttribute("coopAdId", vo.getCoopAdId());

		// grab the full VO from the database for use in the emails below...
		vo = retrieve(vo.getCoopAdId(), null);

		// radio ads send no emails, neither do data updates (Seminar form submissions)
		if ("radio".equalsIgnoreCase(vo.getAdType()))
			return;

		// avoid nulls ahead when we compare statusFlgs
		if (vo.getStatusFlg() == null)
			vo.setStatusFlg(0);
		
		sendNotificationEmail(vo, reqType, site, user, req);

		return;
	}
	
	private void sendNotificationEmail(CoopAdVO vo, String reqType, SiteVO site, 
			UserDataVO user, SMTServletRequest req ) {

		// send appropriate notification emails
		CoopAdsEmailer emailer = new CoopAdsEmailer(actionInit);
		emailer.setAttributes(attributes);
		emailer.setDBConnection(dbConn);

		if (reqType.equals("coopAdsSurgeonApproval")) { 
			// intercept this first since it doesn't used our statusFlg
			log.debug("sending surgeon approved email");
			if ("5".equals(req.getParameter("surgeonStatusFlg"))) {
				// surgeon declined ad
				emailer.notifyAdminOfSurgeonsDecline(vo, site, req.getParameter("notesText"));
			} else {
				// surgeon approved ad
				emailer.notifyAdminOfSurgeonsApproval(vo, site);
			}

		} else if (CLIENT_SUBMITTED == vo.getStatusFlg()) {
			// log.debug("sending client submitted email");
			// emailer.notifyAdminOfAdSubmittal(vo, site, user, req);

		} else if (reqType.equals("coopAdsSendApproval")) {
			log.debug("sending client's approval notice email");
			if ("CFSEM".equalsIgnoreCase(req.getParameter("eventTypeCd"))) {
				// ask the Surgeon to approve their portion
				emailer.requestAdApprovalOfSurgeon(vo, site,
						req.getParameter("ownersEmail"),
						req.getParameter("product"),
						req.getParameter("eventName"),
						req.getParameter("eventDt"));
			}

			// ask the Rep to approve their portion
			emailer.requestClientApproval(vo, site,
					req.getParameter("ownersEmail"));

		} else if (CLIENT_APPROVED_AD == vo.getStatusFlg()) {
			log.debug("sending client approved email");
			emailer.notifyAdminOfAdApproval(vo, site, user);

		} else if (CLIENT_DECLINED_AD == vo.getStatusFlg()) {
			log.debug("sending admin declined email");
			emailer.notifyAdminOfAdDeclined(vo, site, user);

		} else if (CLIENT_PAYMENT_RECD == vo.getStatusFlg()) {
			log.debug("sending payment recieved email");
			emailer.notifyAdminOfAdPaymentRecd(vo, site, user);
		}
	}

	private CoopAdVO saveAd(SMTServletRequest req, SiteVO site, CoopAdVO vo)
			throws ActionException {
		StringBuilder sql = new StringBuilder();
		PreparedStatement ps = null;
		boolean insertRecord = StringUtil.checkVal(vo.getCoopAdId()).length() == 0;

		// if inserting, double-check the DB to avoid duplicates
		if (insertRecord) {
			sql.append("select coop_ad_id from ").append(
					getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DEPUY_EVENT_COOP_AD where event_postcard_id=? and ad_type_txt=?");
			try {
				ps = dbConn.prepareStatement(sql.toString());
				ps.setString(1, vo.getEventPostcardId());
				ps.setString(2, vo.getAdType());
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					insertRecord = false;
					vo.setCoopAdId(rs.getString(1));
				}
			} catch (SQLException sqle) {
				log.error("could not verify coopAd existance", sqle);
			} finally {
				try {
					ps.close();
				} catch (Exception e) {
				}

				sql = new StringBuilder();
			}
		}

		if (Convert.formatInteger(req.getParameter("adStatusFlg")) > 0)
			vo.setStatusFlg(Convert.formatInteger(req
					.getParameter("adStatusFlg")));

		if (insertRecord) {
			sql.append("INSERT INTO ").append(
					getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DEPUY_EVENT_COOP_AD (EVENT_POSTCARD_ID, ");
			sql.append("NEWSPAPER1_TXT, NEWSPAPER2_TXT, TOTAL_COST_NO, COST_TO_REP_NO, ");
			sql.append("APPROVED_PAPER_NM, AD_FILE_URL, STATUS_FLG, CREATE_DT, ");
			sql.append("NEWSPAPER1_PHONE_NO, NEWSPAPER2_PHONE_NO, NEWSPAPER3_TXT, NEWSPAPER3_PHONE_NO, ");
			sql.append("RUN_DATES_TXT, TERRITORY_NO, AD_TYPE_TXT, SURGEON_NM, SURGEON_TITLE_TXT, ");
			sql.append("SURGEON_EMAIL_TXT, SURGEON_IMG_URL, CLINIC_NM, CLINIC_ADDRESS_TXT, ");
			sql.append("CLINIC_PHONE_TXT, CLINIC_HOURS_TXT, SURG_EXPERIENCE_TXT, COOP_AD_ID) ");
			sql.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

			vo.setCoopAdId(new UUIDGenerator().getUUID());
			if (vo.getStatusFlg() == 0)
				vo.setStatusFlg(CLIENT_SUBMITTED);

		} else {
			sql.append("UPDATE ").append(
					getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DEPUY_EVENT_COOP_AD set EVENT_POSTCARD_ID=?, ");
			sql.append("NEWSPAPER1_TXT=?, NEWSPAPER2_TXT=?, TOTAL_COST_NO=?, COST_TO_REP_NO=?, ");
			sql.append("APPROVED_PAPER_NM=?, AD_FILE_URL=?, STATUS_FLG=?, UPDATE_DT=?, ");
			sql.append("NEWSPAPER1_PHONE_NO=?, NEWSPAPER2_PHONE_NO=?, NEWSPAPER3_TXT=?, NEWSPAPER3_PHONE_NO=?, ");
			sql.append("RUN_DATES_TXT=?, TERRITORY_NO=?, AD_TYPE_TXT=?, SURGEON_NM=?, SURGEON_TITLE_TXT=?, ");
			sql.append("SURGEON_EMAIL_TXT=?, SURGEON_IMG_URL=?, CLINIC_NM=?, CLINIC_ADDRESS_TXT=?, ");
			sql.append("CLINIC_PHONE_TXT=?, CLINIC_HOURS_TXT=?, SURG_EXPERIENCE_TXT=? ");
			if (vo.getStatusFlg() == PENDING_CLIENT_APPROVAL)
				sql.append(", AD_SUBMIT_DT=?, SURGEON_STATUS_FLG=? ");
			sql.append("WHERE COOP_AD_ID=?");

			// upload the ad file if it exists
			vo.setAdFileUrl(this.saveFile(req, "adFileUrl", "oldAdFileUrl"));
		}
		log.debug("Co-op Ad SQL: " + sql.toString());

		// save the surgeon's photo
		vo.setSurgeonImageUrl(this.saveFile(req, "surgeonImageUrl",
				"oldSurgeonImageUrl"));

		// perform the execute
		try {
			int i = 0;
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(++i, vo.getEventPostcardId());
			ps.setString(++i, vo.getNewspaper1Text());
			ps.setString(++i, vo.getNewspaper2Text());
			ps.setDouble(++i, vo.getTotalCostNo());
			ps.setDouble(++i, vo.getCostToRepNo());
			ps.setString(++i, vo.getApprovedPaperName());
			ps.setString(++i, vo.getAdFileUrl());
			ps.setInt(++i, vo.getStatusFlg());
			ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			ps.setString(++i, vo.getNewspaper1Phone());
			ps.setString(++i, vo.getNewspaper2Phone());
			ps.setString(++i, vo.getNewspaper3Text());
			ps.setString(++i, vo.getNewspaper3Phone());
			ps.setString(++i, vo.getAdDatesText());
			ps.setString(++i, vo.getTerritoryNo());
			ps.setString(++i, vo.getAdType());
			ps.setString(++i, vo.getSurgeonName());
			ps.setString(++i, vo.getSurgeonTitle());
			ps.setString(++i, vo.getSurgeonEmail());
			ps.setString(++i, vo.getSurgeonImageUrl());
			ps.setString(++i, vo.getClinicName());
			ps.setString(++i, vo.getClinicAddress());
			ps.setString(++i, vo.getClinicPhone());
			ps.setString(++i, vo.getClinicHours());
			ps.setString(++i, vo.getSurgicalExperience());
			if (vo.getStatusFlg() == PENDING_CLIENT_APPROVAL
					&& !insertRecord) {
				ps.setTimestamp(++i, Convert.getCurrentTimestamp());
				ps.setInt(++i, 0);
			}
			ps.setString(++i, vo.getCoopAdId());

			if (ps.executeUpdate() < 1) {
				throw new SQLException("No Co-Op Ad records updated");
			} else {
				this.saveAdXR(vo, req);
			}

		} catch (SQLException sqle) {
			log.error("Error saving Co-op Ad", sqle);
			throw new ActionException(sqle);

		} finally {
			try {
				ps.close();
			} catch (Exception e) {
			}
		}

		return vo;
	}

	/**
	 * inserts the XP table reference between the ad and it's events
	 * 
	 * @param vo
	 * @param req
	 */
	private void saveAdXR(CoopAdVO vo, SMTServletRequest req)
			throws ActionException {
		StringBuilder sql = new StringBuilder();
		PreparedStatement ps = null;

		sql.append("INSERT INTO ").append(
				getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_EVENT_XR_COOP_AD (XR_COOP_AD_ID, EVENT_ENTRY_ID, ");
		sql.append("COOP_AD_ID, ORDER_NO, CREATE_DT) values (?,?,?,?,?)");
		log.debug("Co-op Ad XR SQL: " + sql.toString());

		for (int cnt = 1; StringUtil.checkVal(
				req.getParameter("eventEntryId_" + cnt)).length() > 0; cnt++) {
			// perform the execute at least once, maybe more
			try {
				ps = dbConn.prepareStatement(sql.toString());
				ps.setString(1, new UUIDGenerator().getUUID());
				ps.setString(2, req.getParameter("eventEntryId_" + cnt));
				ps.setString(3, vo.getCoopAdId());
				ps.setInt(4, cnt);
				ps.setTimestamp(5, Convert.getCurrentTimestamp());

				if (ps.executeUpdate() < 1)
					throw new SQLException("No Co-Op XR records updated");
			} catch (SQLException sqle) {
				log.error("Error saving Co-op Ad XR", sqle);
				throw new ActionException(sqle);

			} finally {
				try {
					ps.close();
				} catch (Exception e) {
				}
			}
		}

		return;
	}

	/**
	 * status update ONLY on the _COOP_AD table...used when client approves or
	 * declines the ad
	 * 
	 * @param vo
	 * @param req
	 * @throws ActionException
	 */
	private void saveAdApproval(SMTServletRequest req) throws ActionException {
		StringBuilder sql = new StringBuilder();
		PreparedStatement ps = null;
		int statusLvl = Convert
				.formatInteger(req.getParameter("adStatusFlg"));

		sql.append("UPDATE ")
				.append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_EVENT_COOP_AD SET STATUS_FLG=?, UPDATE_DT=? where COOP_AD_ID=?");
		log.debug(sql);

		// perform the update
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, statusLvl);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, req.getParameter("coopAdId"));

			if (ps.executeUpdate() < 1)
				throw new SQLException("No Co-Op Ad status records updated");
		} catch (SQLException sqle) {
			log.error("Error saving Co-op Ad status", sqle);
			throw new ActionException(sqle);

		} finally {
			try {
				ps.close();
			} catch (Exception e) {
			}
		}

		return;
	}

	/**
	 * captures the surgeon's approval of their ad/expense. CFSEM only.
	 * 
	 * @param req
	 * @throws ActionException
	 */
	private void saveSurgeonsAdApproval(SMTServletRequest req)
			throws ActionException {
		StringBuilder sql = new StringBuilder();
		PreparedStatement ps = null;
		int statusLvl = Convert.formatInteger(req
				.getParameter("surgeonStatusFlg"));

		sql.append("UPDATE ")
				.append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_EVENT_COOP_AD SET SURGEON_STATUS_FLG=?, UPDATE_DT=? ");
		sql.append("where COOP_AD_ID=?");
		log.debug("Co-op Ad client approval SQL: " + sql.toString());

		// perform the execute
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, statusLvl);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, req.getParameter("coopAdId"));

			if (ps.executeUpdate() < 1)
				throw new SQLException("No Co-Op Ad status records updated");
		} catch (SQLException sqle) {
			log.error("Error saving Co-op Ad Surgeon approval", sqle);
			throw new ActionException(sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {
			}
		}

		return;
	}

	private String saveFile(SMTServletRequest req, String paramNm,
			String oldParamNm) {
		log.debug("starting saveAdFile");
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);

		StringBuilder filePath = new StringBuilder(
				(String) getAttribute("pathToBinary"));
		filePath.append(getAttribute("orgAlias")).append(
				site.getOrganizationId());
		filePath.append("/").append(site.getSiteId()).append("/ads/");

		FileLoader fl = null;
		FilePartDataBean fpdb = req.getFile(paramNm);
		String fileNm = StringUtil.checkVal(req.getParameter(oldParamNm))
				.trim();
		String newFile = (fpdb != null) ? fpdb.getFileName() : "";
		log.debug("newFile=" + newFile + " oldFile=" + fileNm);

		// delete old file
		if (fileNm.length() > 0 && newFile.length() > 0) {
			try {
				fl = new FileLoader(attributes);
				fl.setPath(filePath.toString());
				fl.setFileName(fileNm);
				fl.deleteFile();
				fl = null;
			} catch (Exception e) {
				log.error("Error Deleting Ad File", e);
			}
			log.debug("finished deletion of " + filePath.toString() + fileNm);
		}

		// Write new file
		if (newFile.length() > 0) {
			try {
				fl = new FileLoader(attributes);
				fl.setFileName(fpdb.getFileName());
				fl.setPath(filePath.toString());
				fl.setRename(true);
				fl.setOverWrite(false);
				fl.setData(fpdb.getFileData());
				fileNm = fl.writeFiles();
			} catch (Exception e) {
				log.error("Error Writing Ad File", e);
			}
			log.debug("finished write");
		}

		fpdb = null;
		fl = null;
		return fileNm;
	}

	/**
	 * retrieves the ad for the requested postcard
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		CoopAdVO vo = retrieve(req.getParameter("coopAdId"),
				req.getParameter("eventPostcardId"));
		req.setAttribute(RETR_ADS, vo);
	}

	protected CoopAdVO retrieve(String coopAdId, String postcardId)
			throws ActionException {
		final String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		PreparedStatement ps = null;
		coopAdId = StringUtil.checkVal(coopAdId);
		postcardId = StringUtil.checkVal(postcardId);
		CoopAdVO vo = new CoopAdVO();

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT a.*, b.order_no, c.rsvp_code_txt ");
		sql.append("FROM ").append(schema);
		sql.append("DEPUY_EVENT_COOP_AD a INNER JOIN ").append(schema);
		sql.append("DEPUY_EVENT_XR_COOP_AD b on a.COOP_AD_ID=b.COOP_AD_ID ");
		sql.append("INNER JOIN EVENT_ENTRY c on b.event_entry_id=c.event_entry_id ");
		sql.append("where 1=0 ");
		if (coopAdId.length() > 0)
			sql.append("or a.COOP_AD_ID=? ");
		else if (postcardId.length() > 0)
			sql.append("or a.EVENT_POSTCARD_ID=? ");
		sql.append("order by b.order_no");
		log.debug("Co-op Ad retrieve SQL: " + sql.toString());

		// perform the execute
		try {
			ps = dbConn.prepareStatement(sql.toString());
			if (coopAdId.length() > 0)
				ps.setString(1, coopAdId);
			else if (postcardId.length() > 0)
				ps.setString(1, postcardId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				vo.setData(rs);
				vo.addEvent(rs.getString("rsvp_code_txt"),
						rs.getInt("order_no"));
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving Co-op Ad, pkId=" + coopAdId
					+ ", postcard=" + postcardId, sqle);
			throw new ActionException(sqle);

		} finally {
			try {
				ps.close();
			} catch (Exception e) {
			}
		}
		return vo;
	}
}
