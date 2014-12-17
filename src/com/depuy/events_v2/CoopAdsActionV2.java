package com.depuy.events_v2;

// JDK
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.depuy.events_v2.CoopAdsEmailer;
import com.depuy.events_v2.vo.DePuyEventSeminarVO;
// SMT BaseLibs
import com.depuy.events.vo.CoopAdVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// SB Libs
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: CoopAdsAction.java
 * <p/>
 * <b>Description: Manages Co-op ads for the DePuy Events/Postcards.</b>
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author James McKain
 * @version 1.0
 * @since Jan 16, 2014
 ****************************************************************************/
public class CoopAdsActionV2 extends SBActionAdapter {

	// Ad status levels
	public static final int NO_ADS = 15;
	public static final int CLIENT_SUBMITTED = 1;
	public static final int PENDING_CLIENT_APPROVAL = 2;
	public static final int CLIENT_APPROVED_AD = 3;
	public static final int CLIENT_DECLINED_AD = 4;
	public static final int CLIENT_PAYMENT_RECD = 5;
	public static final int AD_DETAILS_RECD = 6;
	public static final int PENDING_SURG_APPROVAL = 7;
	public static final int SURG_APPROVED_AD = 8;
	public static final int SURG_DECLINED_AD = 9;
	public static final int REMIND_ME_LATER = 10;

	public CoopAdsActionV2() {
		super();
	}

	public CoopAdsActionV2(ActionInitVO arg0) {
		super(arg0);
	}

	public void build(SMTServletRequest req) throws ActionException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		String reqType = StringUtil.checkVal(req.getParameter("reqType"));
		
		// create the VO based on the incoming request object
		List<CoopAdVO> voList = createAdList(req);
		
		for( int i=0; i<voList.size(); i++ ){
			CoopAdVO vo = voList.get(i);
			
			//record the transaction
			switch (reqType){
			case "approveNewspaperAd":
				vo.setStatusFlg(CLIENT_APPROVED_AD);
				this.saveAdApproval(vo, req);
				break;
			case "declineNewspaperAd":
				vo.setStatusFlg(CLIENT_DECLINED_AD);
				this.saveAdApproval(vo, req);
				break;	
			case "coopAdsSurgeonApproval":
				this.saveSurgeonsAdApproval(vo);
				break;
				
			case "coopAdsSubmit":
				vo.setStatusFlg(CLIENT_SUBMITTED);
				vo = this.saveAd(req, site, vo);
				break;
				
			case "radioAdsSubmit":
				vo.setStatusFlg(CLIENT_APPROVED_AD);
				vo = this.saveAd(req, site, vo);
				break;
				
			case "eventInfo":
				this.saveAd(req,site,vo);
				continue;
				
			case "uploadAdFile":
				this.saveAd(req, site, vo);
				break;
				
			default:
				break;
			}
			
			req.setAttribute("coopAdId", vo.getCoopAdId());
	
			// radio ads send no emails.
			if ("radio".equalsIgnoreCase(vo.getAdType()) || "eventInfo".equals(req.getParameter("reqType")))
				continue;
	
			// grab the full VO from the database for use in the emails below...
			vo = retrieve(vo.getCoopAdId(), vo.getEventPostcardId()).get(0);
			
			// avoid nulls ahead when we compare statusFlgs
			if (vo.getStatusFlg() == null)
				vo.setStatusFlg(0);
	
			sendNotificationEmail(vo, reqType, site, user, req, i);
		}
		
		return;
	}
	
	/**
	 * Creates a list of CoopAdVO's from the seminar submission form. Used for 
	 * both the print and online ads. Grouped by suffix in the step2Form.
	 * @param req
	 * @return
	 */
	private List<CoopAdVO> createAdList( SMTServletRequest req ){
		//Since the maximum number of ads (per type) is 3, and the only 2 types
		//we have are print and online, the maximum allowed is 6
		final int MAX_ADS = 6;
		List<CoopAdVO> adList = new ArrayList<CoopAdVO>();
		//Submitted print ad count
		Integer printAds = Convert.formatInteger( req.getParameter("printAds") );
		//Submitted online ad count
		Integer onlineAds = Convert.formatInteger( req.getParameter("onlineAds") );
		
		//Since there may be multiple ads, and they're grouped by suffix, they won't
		//be set by passing request object to CoopAdVO(req)
		for (int i=0; i < MAX_ADS; ++i){
			CoopAdVO vo = null;
			boolean hasOnlineFlg = req.hasParameter("online_flg_"+i);
			Integer onlineFlg = Convert.formatInteger( req.getParameter("onlineFlg") );
			
			//0-2 for print ads
			if ( i < printAds ){
				vo = new CoopAdVO(req);
				assignAdFields(req, vo, i, (hasOnlineFlg ? onlineFlg : 0 ) );
				adList.add(vo);
			//3-5 for online ads
			} else if (i < (onlineAds + (MAX_ADS/2)) && i >= (MAX_ADS/2)){
				vo = new CoopAdVO(req);
				assignAdFields(req, vo, i, (hasOnlineFlg ? onlineFlg : 1 ));
				adList.add(vo);
			}
		}
		
		return adList;
	}
	
	/**
	 * Sets ad info for a CoopAdVO, using the specified suffix to identify 
	 * values from the request object.
	 * @param req
	 * @param vo
	 * @param suffix
	 * @param onlineFlg 0 for print ads, 1 for online ads
	 */
	private void assignAdFields( SMTServletRequest req, CoopAdVO vo, int suffix, int onlineFlg ){
		vo.setNewspaper1Phone( req.getParameter("newspaper1Phone_"+suffix) );
		vo.setNewspaper1Text( req.getParameter("newspaper1Text_"+suffix) );
		vo.setContactName( req.getParameter("contactName_"+suffix) );
		vo.setAdCount( Convert.formatInteger(req.getParameter("adCount_"+suffix)) );
		vo.setWeeksAdvance( Convert.formatInteger( req.getParameter("weeksAdvance_"+suffix)));
		vo.setAdType( req.getParameter("adType_"+suffix) );
		vo.setInstructionsText( req.getParameter("instructionsText_"+suffix) );
		vo.setHospitalInfo( req.getParameter("hospitalInfo_"+suffix) );
		vo.setSurgeonInfo( req.getParameter("surgeonInfo_"+suffix) );
		vo.setTotalCostNo( Convert.formatDouble(req.getParameter("totalCostNo_"+suffix)));
		vo.setCostToDepuyNo( Convert.formatDouble(req.getParameter("costToDepuyNo_"+suffix)));
		vo.setCostToRepNo( Convert.formatDouble( req.getParameter("costToRepNo_"+suffix)));
		vo.setCostToSurgeonNo( Convert.formatDouble( req.getParameter("costToSurgeonNo_"+suffix)));
		vo.setCostToHospitalNo( Convert.formatDouble( req.getParameter("costToHospitalNo_"+suffix)));
		vo.setCostToPartyNo( Convert.formatDouble( req.getParameter("costToPartyNo_"+suffix)));
		vo.setStatusFlg( Convert.formatInteger( req.getParameter("adStatusFlg_"+suffix)));
		vo.setOnlineFlg( onlineFlg );
		vo.setSurgeonStatusFlg( Convert.formatInteger( req.getParameter("surgeonStatusFlg_"+suffix) ));
		vo.setApprovedPaperName( StringUtil.checkVal( req.getParameter("approvedPaperName_"+suffix)) );
		vo.setAdDatesText( req.getParameter("adDatesText_"+suffix) );
		
		if (req.hasParameter("coopAdId_"+suffix)){
			vo.setCoopAdId( req.getParameter("coopAdId_"+suffix));
		}
	}
	
	private void sendNotificationEmail(CoopAdVO vo, String reqType, SiteVO site, 
			UserDataVO user, SMTServletRequest req, int index ) {
		
		DePuyEventSeminarVO sem = (DePuyEventSeminarVO) req.getAttribute("postcard");
		
		// send appropriate notification emails
		CoopAdsEmailer emailer = new CoopAdsEmailer(actionInit);
		emailer.setAttributes(attributes);
		emailer.setDBConnection(dbConn);

		switch( vo.getStatusFlg() ){
		
		case SURG_DECLINED_AD:
			// surgeon declined ad
			log.debug("sending surgeon declined email");
			emailer.notifyAdminOfSurgeonsDecline(sem, site, req.getParameter("notesText_"+index));
			break;
		
		case SURG_APPROVED_AD:
			// surgeon approved ad
			log.debug("sending surgeon approved email");
			emailer.notifyAdminOfSurgeonsApproval(sem, site);
			break;
		
		case CLIENT_SUBMITTED:
			// log.debug("sending client submitted email");
			// emailer.notifyAdminOfAdSubmittal(vo, site, user, req);
			break;
			
		case PENDING_CLIENT_APPROVAL:
			log.debug("sending client's approval notice email");

			// ask the Rep to approve their portion
			emailer.requestCoordinatorApproval(sem, site);
			break;
			
		case CLIENT_APPROVED_AD:
			log.debug("sending client approved email");
			emailer.notifyAdminOfAdApproval(sem, site, user);
			
		case PENDING_SURG_APPROVAL:
			//email the surgeon to approve their portion of the ad
			String eventType = sem.getEvents().get(0).getEventTypeCd();
			if (eventType.equalsIgnoreCase("CFSEM50") || eventType.equalsIgnoreCase("CFSEM25")) {
				// ask the Surgeon to approve their portion
				emailer.requestAdApprovalOfSurgeon(sem, site);
			}
			break;

		case CLIENT_DECLINED_AD:
			log.debug("sending admin declined email");
			emailer.notifyAdminOfAdDeclined(sem, site, user, req.getParameter("notesText_"+index));
			break;
			
		case CLIENT_PAYMENT_RECD:
			log.debug("sending payment recieved email");
			emailer.notifyAdminOfAdPaymentRecd(sem, site, user);
			break;
			
		case AD_DETAILS_RECD:
			log.debug("sending novus file received email");
			emailer.notifyNovusUpload(sem, site);
			break;
		}
	}

	
	/**
	 * adds or updates the enter Ad, first checking to see if this ad already exists 
	 * for the given type and postcard. (was an issue with duplicates and people
	 * hitting their 'back' button in Events V1.)
	 * @param req
	 * @param site
	 * @param vo
	 * @return
	 * @throws ActionException
	 */
	public CoopAdVO saveAd(SMTServletRequest req, SiteVO site, CoopAdVO vo)
			throws ActionException {
		StringBuilder sql = new StringBuilder();
		PreparedStatement ps = null;
		boolean insertRecord = StringUtil.checkVal(vo.getCoopAdId()).length() == 0;
		boolean updateSurgeon = (SURG_DECLINED_AD >= vo.getStatusFlg() && PENDING_SURG_APPROVAL <= vo.getStatusFlg());

		// if inserting, double-check the DB to avoid duplicates
		if (insertRecord) {
			sql.append("select coop_ad_id, status_flg from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DEPUY_EVENT_COOP_AD where event_postcard_id=? and ad_type_txt=?");
			try {
				ps = dbConn.prepareStatement(sql.toString());
				ps.setString(1, vo.getEventPostcardId());
				ps.setString(2, vo.getAdType());
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					insertRecord = false;
					vo.setCoopAdId(rs.getString(1));
					if (vo.getStatusFlg() == null) vo.setStatusFlg(rs.getInt("status_flg"));
				}
			} catch (SQLException sqle) {
				log.error("could not verify coopAd existence", sqle);
			} finally {
				try {
					ps.close();
				} catch (Exception e) {
				}

				sql = new StringBuilder();
			}
		}

		if (Convert.formatInteger(req.getParameter("adStatusFlg")) > 0)
			vo.setStatusFlg(Convert.formatInteger(req.getParameter("adStatusFlg")));

		if (insertRecord) {
			sql.append("INSERT INTO ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DEPUY_EVENT_COOP_AD (EVENT_POSTCARD_ID, ");
			sql.append("NEWSPAPER1_TXT, NEWSPAPER2_TXT, TOTAL_COST_NO, COST_TO_REP_NO, ");
			sql.append("APPROVED_PAPER_NM, AD_FILE_URL, AD_FILE2_URL, AD_FILE3_URL, STATUS_FLG, CREATE_DT, ");
			sql.append("NEWSPAPER1_PHONE_NO, NEWSPAPER2_PHONE_NO, NEWSPAPER3_TXT, NEWSPAPER3_PHONE_NO, ");
			sql.append("RUN_DATES_TXT, TERRITORY_NO, AD_TYPE_TXT, SURGEON_NM, SURGEON_TITLE_TXT, ");
			sql.append("SURGEON_EMAIL_TXT, SURGEON_IMG_URL, CLINIC_NM, CLINIC_ADDRESS_TXT, ");
			sql.append("CLINIC_PHONE_TXT, CLINIC_HOURS_TXT, SURG_EXPERIENCE_TXT, ");
			sql.append("CONTACT_NM, CONTACT_EMAIL_TXT, INSTRUCTIONS_TXT, ONLINE_FLG, ");
			sql.append("HOSPITAL1_IMG, HOSPITAL2_IMG, HOSPITAL3_IMG, SURGEON_INFO_TXT, ");
			sql.append("HOSPITAL_INFO_TXT, WEEKS_ADVANCE_NO, AD_COUNT_NO, ");
			sql.append("COST_TO_DEPUY_NO, COST_TO_PARTY_NO, COST_TO_HOSPITAL_NO, COST_TO_SURGEON_NO, ");
			sql.append("INVOICE_FILE_URL, COOP_AD_ID ) ");
			sql.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

			vo.setCoopAdId(new UUIDGenerator().getUUID());
			vo.setStatusFlg(CLIENT_SUBMITTED);

		} else {
			sql.append("UPDATE ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DEPUY_EVENT_COOP_AD set EVENT_POSTCARD_ID=?, ");
			sql.append("NEWSPAPER1_TXT=?, NEWSPAPER2_TXT=?, TOTAL_COST_NO=?, COST_TO_REP_NO=?, ");
			sql.append("APPROVED_PAPER_NM=?, AD_FILE_URL=?, AD_FILE2_URL=?,AD_FILE3_URL=?, STATUS_FLG=?, UPDATE_DT=?, ");
			sql.append("NEWSPAPER1_PHONE_NO=?, NEWSPAPER2_PHONE_NO=?, NEWSPAPER3_TXT=?, NEWSPAPER3_PHONE_NO=?, ");
			sql.append("RUN_DATES_TXT=?, TERRITORY_NO=?, AD_TYPE_TXT=?, SURGEON_NM=?, SURGEON_TITLE_TXT=?, ");
			sql.append("SURGEON_EMAIL_TXT=?, SURGEON_IMG_URL=?, CLINIC_NM=?, CLINIC_ADDRESS_TXT=?, ");
			sql.append("CLINIC_PHONE_TXT=?, CLINIC_HOURS_TXT=?, SURG_EXPERIENCE_TXT=?, ");
			sql.append("CONTACT_NM=?, CONTACT_EMAIL_TXT=?, INSTRUCTIONS_TXT=?, ONLINE_FLG=?, ");
			sql.append("HOSPITAL1_IMG=?, HOSPITAL2_IMG=?, HOSPITAL3_IMG=?, SURGEON_INFO_TXT=?, ");
			sql.append("HOSPITAL_INFO_TXT=?, WEEKS_ADVANCE_NO=?, AD_COUNT_NO=?, ");
			sql.append("COST_TO_DEPUY_NO=?,COST_TO_PARTY_NO=?,COST_TO_HOSPITAL_NO=?,COST_TO_SURGEON_NO=?, INVOICE_FILE_URL=? ");
			if (PENDING_CLIENT_APPROVAL == vo.getStatusFlg())
				sql.append(", AD_SUBMIT_DT=?, SURGEON_STATUS_FLG=? ");
			else if (updateSurgeon)
				sql.append(", SURGEON_STATUS_FLG=? ");//If the status is changed on promote page; ensures views that check surgeon flag won't be incorrect 
			sql.append("WHERE COOP_AD_ID=?");

			// upload the ad file if it exists
			vo.setAdFileUrl(this.saveFile(req, "adFileUrl", "/ads/", site));
			vo.setAdFile2Url( saveFile(req, "adFile2Url", "/ads/", site) );
			vo.setAdFile3Url( saveFile(req, "adFile3Url", "/ads/", site) );
			vo.setInvoiceFile(saveFile(req, "invoiceFile", "/invoice/" ,site ));
		}
		log.debug("Co-op Ad SQL: " + sql.toString());

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
			ps.setString(++i, vo.getAdFile2Url());
			ps.setString(++i, vo.getAdFile3Url());
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
			ps.setString(++i, saveFile(req, "surgImgUrl", "/ads/logos", site));
			ps.setString(++i, vo.getClinicName());
			ps.setString(++i, vo.getClinicAddress());
			ps.setString(++i, vo.getClinicPhone());
			ps.setString(++i, vo.getClinicHours());
			ps.setString(++i, vo.getSurgicalExperience());
			ps.setString(++i, vo.getContactName());
			ps.setString(++i, vo.getContactEmail());
			ps.setString(++i, vo.getInstructionsText());
			ps.setInt(++i, vo.getOnlineFlg());
			ps.setString(++i, saveFile(req, "hospital1Logo", "/ads/logos", site));
			ps.setString(++i, saveFile(req, "hospital2Logo", "/ads/logos", site));
			ps.setString(++i, saveFile(req, "hospital3Logo", "/ads/logos", site));
			ps.setString(++i, vo.getSurgeonInfo());
			ps.setString(++i,  vo.getHospitalInfo());
			ps.setInt(++i, vo.getWeeksAdvance());
			ps.setInt(++i, vo.getAdCount());
			ps.setDouble(++i, vo.getCostToDepuyNo());
			ps.setDouble(++i, vo.getCostToPartyNo());
			ps.setDouble(++i, vo.getCostToHospitalNo());
			ps.setDouble(++i, vo.getCostToSurgeonNo());
			ps.setString(++i, vo.getInvoiceFile());
			if (vo.getStatusFlg() == PENDING_CLIENT_APPROVAL && !insertRecord) {
				ps.setTimestamp(++i, Convert.getCurrentTimestamp());
				ps.setInt(++i, 0);
			} else if (updateSurgeon)
				ps.setInt(++i,  (SURG_APPROVED_AD == vo.getStatusFlg() ? 1:0));
			ps.setString(++i, vo.getCoopAdId());

			if (ps.executeUpdate() < 1)
				throw new SQLException("No Co-Op Ad records updated");

		} catch (SQLException sqle) {
			log.error("Error saving Co-op Ad", sqle);
			throw new ActionException(sqle);

		} finally {
			try { ps.close(); } catch (Exception e) { }
		}

		return vo;
	}


	/**
	 * status update ONLY on the _COOP_AD table...used when client approves or
	 * declines the ad
	 * 
	 * @param vo
	 * @param req
	 * @throws ActionException
	 */
	private void saveAdApproval(CoopAdVO vo, SMTServletRequest req) throws ActionException {
		final DePuyEventSeminarVO sem = (DePuyEventSeminarVO) req.getAttribute("postcard");
		Integer statusLvl = Convert.formatInteger( vo.getStatusFlg() );
		if ( statusLvl == CLIENT_APPROVED_AD && sem.getEvents().get(0).getEventTypeCd().startsWith("CFSEM")){
			//Skip straight to surgeon approval for co-funded
			statusLvl = PENDING_SURG_APPROVAL;
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_EVENT_COOP_AD SET STATUS_FLG=?, UPDATE_DT=? ");
		sql.append("where COOP_AD_ID=?");
		log.debug(sql);

		// perform the update
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, statusLvl );
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, vo.getCoopAdId());

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
	private void saveSurgeonsAdApproval(CoopAdVO vo) throws ActionException {
		Integer statusLvl = Convert.formatInteger( vo.getSurgeonStatusFlg() );
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_EVENT_COOP_AD SET SURGEON_STATUS_FLG=?, UPDATE_DT=?, ");
		sql.append("STATUS_FLG=? where COOP_AD_ID=?");
		log.debug("Co-op Ad client approval SQL: " + sql.toString());

		// perform the execute
		PreparedStatement ps = null;
		try {
			int i = 0;
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(++i, (statusLvl == SURG_APPROVED_AD ? 1 : 0 ));
			ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			ps.setInt(++i, statusLvl);
			ps.setString(++i, vo.getCoopAdId());

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

	private String saveFile(SMTServletRequest req, String paramNm, String subPath, SiteVO site) {
		//log.debug("starting saveAdFile");
		PostcardInsertV2 pi2 = new PostcardInsertV2();
		pi2.setAttributes(attributes);
		return pi2.saveFile(req, paramNm, subPath, site);
	}

	protected List<CoopAdVO> retrieve(String coopAdId, String postcardId)
			throws ActionException {
		final String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		PreparedStatement ps = null;
		coopAdId = StringUtil.checkVal(coopAdId);
		postcardId = StringUtil.checkVal(postcardId);
		List<CoopAdVO> ads = new ArrayList<CoopAdVO>();

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM ").append(schema);
		sql.append("DEPUY_EVENT_COOP_AD where EVENT_POSTCARD_ID=? ");
		if (coopAdId.length() > 0)
			sql.append("and COOP_AD_ID=? ");
		log.debug(sql);

		// perform the execute
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, postcardId);
			if (coopAdId.length() > 0) ps.setString(2, coopAdId);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				ads.add(new CoopAdVO(rs));

		} catch (SQLException sqle) {
			log.error("Error retrieving Co-op Ad, pkId=" + coopAdId
					+ ", postcard=" + postcardId, sqle);
			throw new ActionException(sqle);

		} finally {
			try { ps.close(); } catch (Exception e) { }
		}
		return ads;
	}
	
	/**
	 * Save the ad's invoice to binary directory and path to db, and send email notification
	 * @param req
	 * @param site
	 * @param coopAdId
	 * @throws ActionException
	 */
	protected void saveAdInvoice( SMTServletRequest req, SiteVO site,
			DePuyEventSeminarVO sem, String coopAdId ) throws ActionException {
		final String customDB = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		final String fieldName = "invoiceFile"; //parameter containing file
		final String subPath = "/invoice/"; //sub-path to store the file in /binary
		final String adId = StringUtil.checkVal(coopAdId);
		if (adId.isEmpty())
			throw new ActionException("Missing ad Id.");
		
		//Create prepared statement
		StringBuilder sql = new StringBuilder(90);
		sql.append("update ").append(customDB).append("DEPUY_EVENT_COOP_AD set INVOICE_FILE_URL=?, ");
		sql.append("UPDATE_DT=? where COOP_AD_ID=?");
		log.debug(sql+" | "+adId);
		
		//Update path in db
		try( PreparedStatement ps = dbConn.prepareStatement(sql.toString()) ){
			
			int i = 0;
			ps.setString(++i, saveFile(req, fieldName, subPath ,site ));
			ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			ps.setString(++i, adId);
			
			ps.executeUpdate();
			
		} catch ( SQLException e ){
			log.error("Failed to update invoice file path");
			throw new ActionException(e);
		}
		
		//Send notification email
		CoopAdsEmailer mailer = new CoopAdsEmailer(actionInit);
		mailer.setAttributes(attributes);
		mailer.setDBConnection(dbConn);
		mailer.notifyNovusUpload(sem , site); 
	}
}
