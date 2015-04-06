package com.depuy.events_v2;

// JDK
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.depuy.events_v2.CoopAdsEmailer;
import com.depuy.events_v2.ReportBuilder.ReportType;
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
import com.smt.sitebuilder.common.ModuleVO;
// SB Libs
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
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
	//public static final int NO_ADS = 15;
	public static final int CLIENT_SUBMITTED = 1;
	public static final int PENDING_CLIENT_APPROVAL = 2;
	public static final int PENDING_AD_OPTIONS = 11;
	public static final int CLIENT_APPROVED_AD = 3;
	public static final int CLIENT_DECLINED_AD = 4;
	public static final int AD_BUY_COMPLETE = 50;
	//public static final int CLIENT_PAYMENT_RECD = 5;
	//public static final int AD_DETAILS_RECD = 6;
	//public static final int PENDING_SURG_APPROVAL = 7;
	public static final int SURG_APPROVED_AD = 8;
	public static final int SURG_DECLINED_AD = 9;
	//public static final int PENDING_HOSP_APPROVAL = 12;
	public static final int HOSP_APPROVED_AD = 13;
	public static final int HOSP_DECLINED_AD = 14;
	//public static final int REMIND_ME_LATER = 10;

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

		CoopAdVO vo = new CoopAdVO(req);
		
		//record the transaction
		switch(reqType) {
			case "approveAd":
				vo.setStatusFlg(CLIENT_APPROVED_AD);
				this.updateAdStatus(vo, req);
				break;
				
			case "declineAd":
				vo.setStatusFlg(CLIENT_DECLINED_AD);
				this.updateAdStatus(vo, req);
				break;
				
			case "coopAdsSurgeonApproval":
				// create the VO based on the incoming request object
				for (CoopAdVO v : createAdList(req)) {
					this.saveSurgeonsAdApproval(v);
				}
				break;
				
			case "coopAdsHospitalApproval":
				// create the VO based on the incoming request object
				for (CoopAdVO v : createAdList(req)) {
					this.saveHospitalAdApproval(v);
				}
				break;
				
			case "eventInfo":
				// create the VO based on the incoming request object
				for (CoopAdVO v : createAdList(req))
					this.saveAd(req,site,v);
				
				return;
				
			case "uploadAdFile": //this gets called once per ad, from the Promote page
				this.savePromoteAdData(req, site, vo);
				break;
				
			case "optionFeedback":
				this.saveAdOptionFeedback(vo);
				break;
			
			case "markAdsComplete":
				this.markAdsComplete(req.getParameter("eventPostcardId"));
				break;
		}

		req.setAttribute("coopAdId", vo.getCoopAdId());

		// grab the full VO from the database for use in the emails below...
		List<CoopAdVO> lst = retrieve(vo.getCoopAdId(), vo.getEventPostcardId());
		if (lst == null || lst.size() == 0) return; //nothing to send emails about!
		vo = lst.get(0);

		sendNotificationEmail(vo, reqType, site, user, req);

		return;
	}

	
	/**
	 * Creates a list of CoopAdVO's from the seminar submission form. Used for 
	 * both the print and online ads. Grouped by suffix in the step2Form.
	 * @param req
	 * @return
	 */
	private List<CoopAdVO> createAdList(SMTServletRequest req) {
		//determine how many iterations to run.  Internet ads start at #4
		//current UI supports 3
		final int MAX_ADS = 3;
		List<CoopAdVO> adList = new ArrayList<>();
		CoopAdVO vo = null;
		for (int i=1; i <= MAX_ADS; ++i) {
			//int onlineFlg = (i > 4) ? 1 : 0;

			vo = new CoopAdVO(req);
			assignAdFields(req, vo, i, 0);
			//the ad block/form was left blank and can be discarded if newspaperName was left blank
			if (StringUtil.checkVal(vo.getNewspaper1Text()).length() > 0 || vo.getCoopAdId() != null) 
				adList.add(vo);
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
	private void assignAdFields(SMTServletRequest req, CoopAdVO vo, int suffix, int onlineFlg) {
		vo.setNewspaper1Text(req.getParameter("newspaperText_"+suffix));
		vo.setNewspaper1Phone(req.getParameter("newspaperPhone_"+suffix));
		vo.setContactName(req.getParameter("contactName_"+suffix));
		vo.setAdCount(Convert.formatInteger(req.getParameter("adCount_"+suffix)));
		vo.setWeeksAdvance(Convert.formatInteger(req.getParameter("weeksAdvance_"+suffix)));
		vo.setAdType(req.getParameter("adType")); //same for all Ads
		vo.setInstructionsText( req.getParameter("instructionsText_"+suffix) );
		vo.setStatusFlg(Convert.formatInteger(req.getParameter("adStatusFlg_"+suffix), CLIENT_SUBMITTED));
		vo.setOnlineFlg(onlineFlg);

		//if this is an edit, grab the pkId of the ad
		if (req.hasParameter("coopAdId_"+suffix))
			vo.setCoopAdId(req.getParameter("coopAdId_"+suffix));
	}

	
	private void sendNotificationEmail(CoopAdVO vo, String reqType, SiteVO site, 
			UserDataVO user, SMTServletRequest req) {

		DePuyEventManageActionV2 epsa = new DePuyEventManageActionV2(actionInit);
		epsa.setDBConnection(dbConn);
		epsa.setAttributes(attributes);
		DePuyEventSeminarVO sem = null;
		String origReqType = req.getParameter("reqType");
		try {
			req.setParameter("reqType", PostcardSelectV2.ReqType.summary.name());
			req.setParameter(AdminConstants.FACADE_TYPE, "report");
			req.setParameter("rptType", ReportType.summary.name());
			epsa.build(req);
			ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
			sem = (DePuyEventSeminarVO) mod.getActionData();
		} catch (ActionException ae) {
			log.error("retrievingPostcardInfoForEmails", ae);
		}
		req.setParameter("reqType", origReqType);

		// send appropriate notification emails
		CoopAdsEmailer emailer = new CoopAdsEmailer(actionInit);
		emailer.setAttributes(attributes);
		emailer.setDBConnection(dbConn);

		// avoid nulls ahead when we compare statusFlgs
		if (vo.getStatusFlg() == null) vo.setStatusFlg(0);

		switch (vo.getStatusFlg()) {
			case CLIENT_SUBMITTED:
				//this is 'stuffed' here because of the status reversion - send the email back to the admins that the coordinator has reviewed the Ad Options.
				if ("optionFeedback".equals(reqType))
					emailer.feedbackAdOptions(sem, site, vo);
					
				break;

			case PENDING_CLIENT_APPROVAL:
				log.debug("sending client's approval notice email");
				
				//test if we should tell the coordinator all of the ads are ready for their review
				if (allAdsMeetStatus(sem, null, PENDING_CLIENT_APPROVAL, CLIENT_APPROVED_AD))
					emailer.requestCoordinatorApproval(sem, site, vo);
				
				break;
				
			case PENDING_AD_OPTIONS:
				log.debug("sending ad options email");
				// ask the Rep to review their ad options
				emailer.notifyNovusUpload(sem, site);
				emailer.reviewAdOptions(sem, site, vo);
				break;

			case CLIENT_APPROVED_AD:
				//determine if we need to enact on a surgeon or hospital approval
				if ("coopAdsSurgeonApproval".equals(reqType)) {
					// surgeon approved or declined ads - the email clarifies which
					log.debug("sending surgeon approval email");
					emailer.notifyAdminOfSurgeonsApproval(sem, site, false);

					//email the hospital to approve their portion of the ad if this is a CFSEM25 and all ads were approved.
					//if (allAdsMeetStatus(sem, "surgeon_status_flg", SURG_APPROVED_AD))
						//emailer.requestAdApprovalOfConsignee(sem, site, true);
// no longer supporting 50/25/25 - Rachel W. - 04.02.15 
//				} else if ("coopAdsHospitalApproval".equals(reqType)) {
//					// surgeon approved or declined ads - the email clarifies which
//					log.debug("sending surgeon approval email");
//					emailer.notifyAdminOfSurgeonsApproval(sem, site, true);
					
				} else if (req.hasParameter("isCoordinator")) {
					//only send this if the coordinator submitted the approved status
					log.debug("sending client approved email");
					emailer.notifyAdminOfAdApproval(sem, site, user, Convert.formatInteger(req.getParameter("adCount"), 1), vo);
				}	
				break;

			case CLIENT_DECLINED_AD:
				log.debug("sending admin declined email");
				emailer.notifyAdminOfAdDeclined(sem, site, user, req.getParameter("notesText"), Convert.formatInteger(req.getParameter("adCount"), 1), vo);
				break;

//			case CLIENT_PAYMENT_RECD:
//				log.debug("sending payment recieved email");
//				emailer.notifyAdminOfAdPaymentRecd(sem, site, user);
//				break;

			case AD_BUY_COMPLETE:
				log.debug("sending ad-buy-complete email");
				emailer.allAdsComplete(sem, site);
		}
	}
	
	/**
	 * tests to see if all newspaper ads for this Seminar have ad files uploaded
	 * @param sem
	 * @return
	 */
	private boolean allAdsMeetStatus(DePuyEventSeminarVO sem, String field, int... status) {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		if (field == null) field = "status_flg";
		StringBuilder sql = new StringBuilder(100);
		sql.append("select 1 from ").append(schema);
		sql.append("DEPUY_EVENT_COOP_AD where ").append(field).append(" not in (");
		for (int x=0; x < status.length; x++)
			sql.append((x > 0) ? ",?" : "?");
		sql.append(") and event_postcard_id=? ");
		log.debug(sql);
		
		int cnt=0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (int s : status) ps.setInt(++cnt, s);
			ps.setString(++cnt, sem.getEventPostcardId());
			
			ResultSet rs = ps.executeQuery();
			return !(rs.next()); //no results is a TRUE for this test.
		} catch (SQLException sqle) {
			log.error("could not check ad status", sqle);
		}
		
		return false;
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
	private CoopAdVO saveAd(SMTServletRequest req, SiteVO site, CoopAdVO vo)
			throws ActionException {
		StringBuilder sql = new StringBuilder(300);
		boolean insertRecord = StringUtil.checkVal(vo.getCoopAdId()).length() == 0;

		if (insertRecord) {
			sql.append("INSERT INTO ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DEPUY_EVENT_COOP_AD (EVENT_POSTCARD_ID, ");
			sql.append("NEWSPAPER1_TXT, TOTAL_COST_NO, COST_TO_REP_NO, ");
			sql.append("APPROVED_PAPER_NM, AD_FILE_URL, STATUS_FLG, CREATE_DT, ");
			sql.append("NEWSPAPER1_PHONE_NO, ");
			sql.append("RUN_DATES_TXT, TERRITORY_NO, AD_TYPE_TXT, ");
			sql.append("CONTACT_NM, contact_email_txt, INSTRUCTIONS_TXT, ONLINE_FLG, ");
			sql.append("WEEKS_ADVANCE_NO, AD_COUNT_NO, ");
			sql.append("COST_TO_DEPUY_NO, ");
			sql.append("COST_TO_HOSPITAL_NO, COST_TO_SURGEON_NO, ");
			sql.append("INVOICE_FILE_URL, COOP_AD_ID ) ");
			sql.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

			vo.setCoopAdId(new UUIDGenerator().getUUID());
			vo.setStatusFlg(CLIENT_SUBMITTED);

		} else {
			sql.append("UPDATE ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DEPUY_EVENT_COOP_AD set EVENT_POSTCARD_ID=?, ");
			sql.append("NEWSPAPER1_TXT=?, TOTAL_COST_NO=?, COST_TO_REP_NO=?, ");
			sql.append("APPROVED_PAPER_NM=?, AD_FILE_URL=?, STATUS_FLG=?, UPDATE_DT=?, ");
			sql.append("NEWSPAPER1_PHONE_NO=?, ");
			sql.append("RUN_DATES_TXT=?, TERRITORY_NO=?, AD_TYPE_TXT=?, ");
			sql.append("CONTACT_NM=?, CONTACT_EMAIL_TXT=?, INSTRUCTIONS_TXT=?, ONLINE_FLG=?, ");
			sql.append("WEEKS_ADVANCE_NO=?, AD_COUNT_NO=?, ");
			sql.append("COST_TO_DEPUY_NO=?,COST_TO_HOSPITAL_NO=?,");
			sql.append("COST_TO_SURGEON_NO=?, INVOICE_FILE_URL=? ");
			if (PENDING_CLIENT_APPROVAL == vo.getStatusFlg())
				sql.append(", AD_SUBMIT_DT=?, SURGEON_STATUS_FLG=? "); 
			sql.append("WHERE COOP_AD_ID=?");
		}
		log.debug("Co-op Ad SQL: " + sql);
		log.debug(vo);

		// perform the execute
		int i = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(++i, vo.getEventPostcardId());
			ps.setString(++i, vo.getNewspaper1Text());
			ps.setDouble(++i, vo.getTotalCostNo());
			ps.setDouble(++i, vo.getCostToRepNo());
			ps.setString(++i, vo.getApprovedPaperName());
			ps.setString(++i, vo.getAdFileUrl());
			ps.setInt(++i, vo.getStatusFlg());
			ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			ps.setString(++i, vo.getNewspaper1Phone());
			ps.setString(++i, vo.getAdDatesText());
			ps.setString(++i, vo.getTerritoryNo());
			ps.setString(++i, vo.getAdType());
			ps.setString(++i, vo.getContactName());
			ps.setString(++i, vo.getContactEmail());
			ps.setString(++i, vo.getInstructionsText());
			ps.setInt(++i, vo.getOnlineFlg());
			ps.setInt(++i, vo.getWeeksAdvance());
			ps.setInt(++i, vo.getAdCount());
			ps.setDouble(++i, vo.getCostToDepuyNo());
			ps.setDouble(++i, vo.getCostToHospitalNo());
			ps.setDouble(++i, vo.getCostToSurgeonNo());
			ps.setString(++i, vo.getInvoiceFile());
			if (vo.getStatusFlg() == PENDING_CLIENT_APPROVAL && !insertRecord) {
				ps.setTimestamp(++i, Convert.getCurrentTimestamp());
				ps.setInt(++i, 0);
			}
			ps.setString(++i, vo.getCoopAdId());

			if (ps.executeUpdate() < 1)
				throw new SQLException("No Co-Op Ad records updated");

		} catch (SQLException sqle) {
			log.error("Error saving Co-op Ad", sqle);
			throw new ActionException(sqle);
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
	private void updateAdStatus(CoopAdVO vo, SMTServletRequest req) 
			throws ActionException {
		Integer statusLvl = Convert.formatInteger( vo.getStatusFlg() );

		StringBuilder sql = new StringBuilder(100);
		sql.append("UPDATE ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_EVENT_COOP_AD SET STATUS_FLG=?, UPDATE_DT=? ");
		sql.append("where COOP_AD_ID=?");
		log.debug(sql);

		// perform the update
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, statusLvl );
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, vo.getCoopAdId());

			if (ps.executeUpdate() < 1)
				throw new SQLException("No Co-Op Ad status records updated");
		} catch (SQLException sqle) {
			log.error("Error saving Co-op Ad status", sqle);
			throw new ActionException(sqle);
		}
	}
	

	/**
	 * captures the surgeon's approval of their ad/expense. CFSEM only.
	 * 
	 * @param req
	 * @throws ActionException
	 */
	private void saveSurgeonsAdApproval(CoopAdVO vo) throws ActionException {
		int statusLvl = Convert.formatInteger(vo.getStatusFlg()).intValue(); //uses generic statusFlg field
		boolean surgApproved = (statusLvl == SURG_APPROVED_AD);
		boolean haveReason = StringUtil.checkVal(vo.getInstructionsText()).length() > 0;
		
		StringBuilder sql = new StringBuilder(100);
		sql.append("UPDATE ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_EVENT_COOP_AD SET SURGEON_STATUS_FLG=?, UPDATE_DT=? ");
		if (!surgApproved && haveReason) sql.append(", instructions_txt=? ");
		sql.append("where COOP_AD_ID=?");
		log.debug(sql);

		// perform the execute
		int i = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(++i, statusLvl);
			ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			if (!surgApproved && haveReason) ps.setString(++i,  vo.getInstructionsText()); //capture the reason it was not approved.
			ps.setString(++i, vo.getCoopAdId());

			if (ps.executeUpdate() < 1)
				throw new SQLException("No Co-Op Ad status records updated");
		} catch (SQLException sqle) {
			log.error("Error saving Co-op Ad Surgeon approval", sqle);
			throw new ActionException(sqle);
		}
	}
	
	
	/**
	 * captures the surgeon's approval of their ad/expense. CFSEM only.
	 * 
	 * @param req
	 * @throws ActionException
	 */
	private void saveHospitalAdApproval(CoopAdVO vo) throws ActionException {
		int statusLvl = Convert.formatInteger(vo.getStatusFlg()).intValue(); //uses generic statusFlg field
		boolean surgApproved = (statusLvl == HOSP_APPROVED_AD);
		boolean haveReason = StringUtil.checkVal(vo.getInstructionsText()).length() > 0;
		
		StringBuilder sql = new StringBuilder(100);
		sql.append("UPDATE ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_EVENT_COOP_AD SET HOSPITAL_STATUS_FLG=?, UPDATE_DT=? ");
		if (!surgApproved && haveReason) sql.append(", instructions_txt=? ");
		sql.append("where COOP_AD_ID=?");
		log.debug(sql);

		// perform the execute
		int i = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(++i, statusLvl);
			ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			if (!surgApproved && haveReason) ps.setString(++i,  vo.getInstructionsText()); //capture the reason it was not approved.
			ps.setString(++i, vo.getCoopAdId());

			if (ps.executeUpdate() < 1)
				throw new SQLException("No Co-Op Ad status records updated");
		} catch (SQLException sqle) {
			log.error("Error saving Co-op Ad Surgeon approval", sqle);
			throw new ActionException(sqle);
		}
	}
	
	
	/**
	 * captures the coordinator's ad option feedback
	 * 
	 * @param req
	 * @throws ActionException
	 */
	private void saveAdOptionFeedback(CoopAdVO vo) throws ActionException {		
		StringBuilder sql = new StringBuilder(150);
		sql.append("UPDATE ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_EVENT_COOP_AD SET ");
		sql.append("OPTION_FEEDBACK_TXT=?, STATUS_FLG=?, UPDATE_DT=? ");
		sql.append("where COOP_AD_ID=?");
		log.debug(sql);

		// perform the execute
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, vo.getOptionFeedbackText());
			ps.setInt(2, CLIENT_SUBMITTED);
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.setString(4, vo.getCoopAdId());

			if (ps.executeUpdate() < 1)
				throw new SQLException("No Co-Op Ad status records updated");
		} catch (SQLException sqle) {
			log.error("Error saving Co-op Ad option feedback", sqle);
			throw new ActionException(sqle);
		}
	}
	
	

	/**
	 * simple facade to our standard file upload util.
	 * @param req
	 * @param paramNm
	 * @param subPath
	 * @param site
	 * @return
	 */
	private String saveFile(SMTServletRequest req, String paramNm, String subPath, SiteVO site) {
		//log.debug("starting saveAdFile");
		PostcardInsertV2 pi2 = new PostcardInsertV2();
		pi2.setAttributes(attributes);
		return pi2.saveFile(req, paramNm, subPath, site);
	}

	
	/**
	 * retrieves a CoopAdVO or List<CoopAdVO> from the database
	 * @param coopAdId
	 * @param postcardId
	 * @return
	 * @throws ActionException
	 */
	protected List<CoopAdVO> retrieve(String coopAdId, String postcardId)
			throws ActionException {
		final String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		coopAdId = StringUtil.checkVal(coopAdId);
		postcardId = StringUtil.checkVal(postcardId);
		List<CoopAdVO> ads = new ArrayList<CoopAdVO>();

		StringBuilder sql = new StringBuilder(125);
		sql.append("SELECT * FROM ").append(schema);
		sql.append("DEPUY_EVENT_COOP_AD where EVENT_POSTCARD_ID=? ");
		if (coopAdId.length() > 0) sql.append("and COOP_AD_ID=? ");
		sql.append("order by AD_COUNT_NO");
		log.debug(sql);

		// perform the execute
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, postcardId);
			if (coopAdId.length() > 0) ps.setString(2, coopAdId);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				ads.add(new CoopAdVO(rs));

		} catch (SQLException sqle) {
			log.error("Error retrieving Co-op Ad, pkId=" + coopAdId
					+ ", postcard=" + postcardId, sqle);
			throw new ActionException(sqle);
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
	private void savePromoteAdData(SMTServletRequest req, SiteVO site, 
			CoopAdVO vo) throws ActionException {
		final String customDB = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		//Create prepared statement
		StringBuilder sql = new StringBuilder(200);
		sql.append("update ").append(customDB).append("DEPUY_EVENT_COOP_AD ");
		sql.append("set ad_file_url=?, option_file_url=?, approved_paper_nm=?, run_dates_txt=?, ");
		sql.append("total_cost_no=?, cost_to_depuy_no=?, cost_to_rep_no=?, ");
		sql.append("cost_to_surgeon_no=?, cost_to_hospital_no=?, status_flg=?, ");
		sql.append("surgeon_status_flg=?, hospital_status_flg=?, UPDATE_DT=? ");
		sql.append("where COOP_AD_ID=?");
		log.debug(sql);

		//Update path in db
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i = 0;
			ps.setString(++i, saveFile(req, "adFileUrl", "/ads/" ,site ));
			ps.setString(++i, saveFile(req, "optionFileUrl", "/ads/" ,site ));
			ps.setString(++i, vo.getApprovedPaperName());
			ps.setString(++i, vo.getAdDatesText());
			ps.setDouble(++i, vo.getTotalCostNo());
			ps.setDouble(++i, vo.getCostToDepuyNo());
			ps.setDouble(++i, vo.getCostToRepNo());
			ps.setDouble(++i, vo.getCostToSurgeonNo());
			ps.setDouble(++i, vo.getCostToHospitalNo());
			ps.setInt(++i, vo.getStatusFlg());
			ps.setInt(++i, vo.getSurgeonStatusFlg());
			ps.setInt(++i, vo.getHospitalStatusFlg());
			ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			ps.setString(++i, vo.getCoopAdId());

			ps.executeUpdate();

		} catch (SQLException e) {
			log.error("Failed to update ad from promote page", e);
			throw new ActionException(e);
		}
	}
	
	
	private void markAdsComplete(String eventPostcardId) throws ActionException {
		final String customDB = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		//Create prepared statement
		StringBuilder sql = new StringBuilder(200);
		sql.append("update ").append(customDB).append("DEPUY_EVENT_COOP_AD ");
		sql.append("set status_flg=?, UPDATE_DT=? where EVENT_POSTCARD_ID=?");
		log.debug(sql);

		//Update path in db
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, AD_BUY_COMPLETE);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, eventPostcardId);
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			throw new ActionException(sqle);
		}
	}
}