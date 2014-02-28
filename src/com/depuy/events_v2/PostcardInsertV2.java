package com.depuy.events_v2;

// JDK 1.6.3
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

// SMT BaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.databean.FilePartDataBean;

// SB Libs
import com.smt.sitebuilder.action.FileLoader;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.event.EventEntryAction;
import com.smt.sitebuilder.action.event.EventFacadeAction;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.depuy.events_v2.LeadsDataToolV2.SortType;
import com.depuy.events_v2.ReportBuilder.ReportType;
import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.depuy.events_v2.vo.PersonVO;
import com.depuy.events_v2.vo.PersonVO.Role;

/****************************************************************************
 * <b>Title</b>: PostcardInsertV2.java <p/>
 * <b>Description: Handles all the data insertion for the DePuy Seminars/Events portlet.</b> <p/>
 * <b>Copyright:</b> Copyright (c) 2014 <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 14, 2014
 ****************************************************************************/
public class PostcardInsertV2 extends SBActionAdapter {
	private String message = "Seminar Saved Successfully";

	// transaction types understood by this action
	public enum ReqType {
		eventInfo, leads,
		cancelSeminar, orderBox, uploadPostcard, approvePostcardFile, uploadAdFile, 
		approveNewspaperAd, postseminar, coopAdsSurgeonApproval,
		//status levels
		//submittedByCoord, approvedByAFD, approvedBySRC, pendingSurgeon, approvedMedAffairs
		submitSeminar, approveSeminar, srcApproveSeminar, pendingSurgeonReview, approvedMedAffairs
	}

	public PostcardInsertV2() {
		super();
	}

	public PostcardInsertV2(ActionInitVO arg0) {
		super(arg0);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.
	 * http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		// validate the request is something we can understand
		ReqType reqType = null;
		try {
			reqType = ReqType.valueOf(req.getParameter("reqType"));
		} catch (Exception e) {
			throw new ActionException("unknown request type " + req.getParameter("reqType"));
		}
		
		String nextPage = StringUtil.checkVal(req.getParameter("nextPage"));
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		 String eventPostcardId = (req.hasParameter("eventPostcardId")) ? req.getParameter("eventPostcardId") : null;
			
		 /**
		  * This switch statement provides good OO structuring by using an enum.
		  * The sequence of transactions within each step, combined with the throwing of SQLException
		  * by each one, ensures subsequent transactions are not run if the predecessor fails.
		  */
		try {
			switch (reqType) {
				case eventInfo:
					eventPostcardId = saveEventPostcard(req, site, user, eventPostcardId);
					String eventEntryId = saveEventEntry(req);
					if (eventPostcardId == null) { //isNewSeminar
						req.setParameter("eventPostcardId", eventPostcardId);
						saveEventPostcardAssoc(eventPostcardId, eventEntryId);
						saveLocatorXr(eventPostcardId, req);
					}
					saveEventPersonXr(eventPostcardId, req);
					saveNewspaperAd(eventPostcardId, req);
					saveEventSurgeon(eventPostcardId, req, site);
					break;
					
				case submitSeminar:
					this.submitPostcard(req, eventPostcardId);
					nextPage = "status";
					break;
					
				case approveSeminar:
					this.advApprovePostcard(req, eventPostcardId, user);
					break;
					
				case srcApproveSeminar:
					this.srcApprovePostcard(req, eventPostcardId);
					break;
				
				case pendingSurgeonReview:
					this.pendingSurgeonReview(req, eventPostcardId);
					break;
				
				case approvedMedAffairs:
					this.approvedMedAffairs(req, eventPostcardId);
					break;
						
				case cancelSeminar:
					this.cancelPostcard(req, eventPostcardId);
					break;
					
				case uploadAdFile:
				case approveNewspaperAd:
				case coopAdsSurgeonApproval:
					saveNewspaperAd(eventPostcardId, req);
					break;
				
				case orderBox: //Order Consumable Box - just fires an email
					this.orderBox(req, eventPostcardId);
					break;
					
				case uploadPostcard:
					this.uploadPostcard(req, eventPostcardId, site);
					break;
					
				case approvePostcardFile:
					this.approvePostcardFile(req, eventPostcardId);
					break;
				
				case leads:
					this.deleteSavedLeadCities(eventPostcardId);
					this.saveLeadCities(req, eventPostcardId, user, null);
					break;
					
				case postseminar:
					this.completePostcard(eventPostcardId);
					nextPage = "";
					eventPostcardId = null;
					break;
			}
		} catch (SQLException e) {
			log.error("could not save transaction " + reqType + ", " + e.getMessage(), e);
			throw new ActionException();
		}

		// setup the redirect url
		StringBuilder redirectPg = new StringBuilder();
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		redirectPg.append(page.getRequestURI()).append("?reqType=").append(nextPage);
		if (eventPostcardId != null) redirectPg.append("&eventPostcardId=").append(eventPostcardId);
		
		super.sendRedirect(redirectPg.toString(), message, req);
	}
	

	/**
	 * inserts or updates the EVENT_POSTCARD table.
	 * @param req
	 * @param site
	 * @param user
	 * @return
	 * @throws SQLException
	 */
	private String saveEventPostcard(SMTServletRequest req, SiteVO site, UserDataVO user, String pkId) throws SQLException {
		StringBuilder sql = new StringBuilder();
		String label = req.getParameter("postcardLabel");
		if (pkId != null) {
			sql.append("update event_postcard set update_dt=?, quantity_no=?, ");
			sql.append("mailing_addr_txt=?, label_txt=?, content_no=?, territory_no=? ");
			sql.append("where event_postcard_id=?");
		} else {
			sql.append("insert into event_postcard (organization_id, profile_id, ");
			sql.append("create_dt, quantity_no, mailing_addr_txt, label_txt, content_no, ");
			sql.append("territory_no, event_postcard_id) values (?,?,?,?,?,?,?,?,?)");
			if (label == null || label.length() == 0) label = "Local Orthopaedic Surgeon";
		}
		log.debug("saving event postcard: " + sql);

		int x = 1;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			if (pkId == null) {
				pkId = new UUIDGenerator().getUUID();
				ps.setString(x++, site.getOrganizationId());
				ps.setString(x++, user.getProfileId());
			}
			ps.setTimestamp(x++, Convert.getCurrentTimestamp());
			ps.setInt(x++, Convert.formatInteger(req.getParameter("postcardQuantity"), 0));
			ps.setString(x++, req.getParameter("postcardMailingAddress"));
			ps.setString(x++, label);
			ps.setString(x++, req.getParameter("contentNo"));
			ps.setInt(x++, Convert.formatInteger(req.getParameter("territoryNumber")));
			ps.setString(x++, pkId);

			if (ps.executeUpdate() < 1)
				throw new SQLException(ps.getWarnings());

		} finally {
			try { ps.close(); } catch (Exception e) { }
		}

		return pkId;
	}
	

	/**
	 * insert or update the EVENT_ENTRY table
	 * @param req
	 * @return
	 * @throws SQLException
	 */
	private String saveEventEntry(SMTServletRequest req) throws SQLException {
		EventEntryVO vo = new EventEntryVO(req);
		EventEntryAction ac = new EventEntryAction();
		ac.setAttributes(attributes);
	    	ac.setDBConnection(dbConn);
	    	
	    	//shuffle some data depending on how the user answered certain questions.
	    	if ("Other: ".equalsIgnoreCase(vo.getEventDesc())) {
	    		vo.setShortDesc(req.getParameter("eventDescOther"));
	    	} else if ("Art museums".equalsIgnoreCase(vo.getEventDesc())) {
	    		vo.setShortDesc(req.getParameter("justificationArt"));
	    	} else if ("Country clubs".equalsIgnoreCase(vo.getEventDesc())) {
	    		vo.setShortDesc(req.getParameter("justificationCC"));
	    	} else if ("Hospital".equalsIgnoreCase(vo.getEventDesc())) {
	    		vo.setShortDesc(req.getParameter("eventDescAffirmation"));
	    	}
	    	vo.setEventGroupId(actionInit.getActionId());
	    	vo.setContactName(null); //conflicts with paramNm from CoopAds. not used here.
	    	
		try {
			return ac.update(req, vo);
		} catch (ActionException ae) {
			throw new SQLException(ae.getMessage());
		}
	}

	
	/**
	 * inserts (only) the EVENT_POSTCARD_ASSOC table.
	 * Only gets called on brand new Seminar creation/insert.
	 * @param eventPostcardId
	 * @param eventEntryId
	 * @throws SQLException
	 */
	private void saveEventPostcardAssoc(String eventPostcardId, String eventEntryId) throws SQLException {
		PreparedStatement ps = null;
		StringBuilder sql = new StringBuilder();
		sql.append("insert into event_postcard_assoc (event_postcard_id, ");
		sql.append("event_entry_id, create_dt, event_postcard_assoc_id) ");
		sql.append("values (?,?,?,?)");
		log.debug(sql + "|" + eventPostcardId + "|" + eventEntryId);

		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, eventPostcardId);
			ps.setString(2, eventEntryId);
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.setString(4, new UUIDGenerator().getUUID());
			
			if (ps.executeUpdate() < 1) 
				throw new SQLException(ps.getWarnings());

		} finally {
			try { ps.close(); } catch (Exception e) { }
		}
	}
	
	
	/**
	 * inserts (only) the DEPUY_EVENT_SPECIALTY_XR table.
	 * Only gets called on brand new Seminar creation/insert.
	 * @param eventPostcardId
	 * @param req
	 * @throws SQLException
	 */
	private void saveLocatorXr(String eventPostcardId, SMTServletRequest req) throws SQLException {
		PreparedStatement ps = null;
		StringBuilder sql = new StringBuilder();
		sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_EVENT_SPECIALTY_XR (depuy_event_specialty_xr_id,  ");
		sql.append("event_postcard_id, joint_id, create_dt) values (?,?,?,?)");
		log.debug(sql + "|" + eventPostcardId + "|" + req.getParameter("joint"));

		try {
			ps = dbConn.prepareStatement(sql.toString());
			for (String jointId : req.getParameter("joint").split(",")) {
				ps.setString(1, new UUIDGenerator().getUUID());
				ps.setString(2, eventPostcardId);
				ps.setString(3, jointId);
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			if (ps.executeBatch().length < 1) 
				throw new SQLException(ps.getWarnings());

		} finally {
			try { ps.close(); } catch (Exception e) { }
		}
	}
	
	

	/**
	 * deletes & inserts the DEPUY_EVENT_PERSON_XR table.
	 * Only gets called on brand new Seminar creation/insert.
	 * @param eventPostcardId
	 * @param req
	 * @throws SQLException
	 */
	private void saveEventPersonXr(String eventPostcardId, SMTServletRequest req) throws SQLException {
		PreparedStatement ps = null;
		StringBuilder sql = new StringBuilder();
		
		//start by deleting the old data, since this is an _XR table this is easier than a double-cross-reference to do updates.
		sql.append("delete from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_EVENT_PERSON_XR where postcard_role_cd in ('TGM','REP') and event_postcard_id=?");
		log.debug(sql + "|" + eventPostcardId);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, eventPostcardId);
			ps.executeUpdate();  //a zero here is okay, an SQLException is not and will be thrown.
		} finally {
			try { ps.close(); } catch (Exception e) { }
		}
		
		//begin the inserts
		sql = new StringBuilder();
		sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_EVENT_PERSON_XR (depuy_event_person_xr_id,  ");
		sql.append("profile_id, event_postcard_id, postcard_role_cd, create_dt) values (?,?,?,?,?)");
		log.debug(sql + "|" + eventPostcardId);
		
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		List<GenericVO> data = new ArrayList<GenericVO>();
		
		//first grab the TGMs - create a UserDataVO for each and fire them off to WC to be saved.
		for (String email : req.getParameter("tgmEmail").split(","))
			data.add(this.saveUserProfile(pm, Role.TGM, email, ""));
		
		//now grab the two Sales Reps - create a UserDataVO for each and fire them off to WC to be saved.
		String repEmail = req.getParameter("1stRepEmail1") + req.getParameter("1stRepEmail2");
		if (StringUtil.isValidEmail(repEmail)) data.add(this.saveUserProfile(pm, Role.REP, repEmail, req.getParameter("1stRepName")));
		repEmail = req.getParameter("2ndRepEmail1") + req.getParameter("2ndRepEmail2"); 
		if (StringUtil.isValidEmail(repEmail)) data.add(this.saveUserProfile(pm, Role.REP, repEmail, req.getParameter("2ndRepName")));
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			for (GenericVO vo : data) {
				if (vo.getKey() == null) continue;  //no profileId!
				ps.setString(1, new UUIDGenerator().getUUID());
				ps.setString(2, (String) vo.getKey());
				ps.setString(3, eventPostcardId);
				ps.setString(4, ((Role) vo.getValue()).toString());
				ps.setTimestamp(5, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			ps.executeBatch();

		} finally {
			try { ps.close(); } catch (Exception e) { }
		}
	}
	
	
	/**
	 * An elementary helper method to the above saveEventPersonXr...
	 * @param pm
	 * @param roleCd
	 * @param email
	 * @param name
	 * @return
	 */
	private GenericVO saveUserProfile(ProfileManager pm, Role roleCd, String email, String name) {
		UserDataVO user = new UserDataVO();
		user.setEmailAddress(email);
		user.setName(name);
		try {
			//get the profileId for this person, we won't alter their account.
			user.setProfileId(pm.checkProfile(user, dbConn));
			//if the person doesn't exist in WC, add them.  This should rarely occur once the program ramps up.
			if (user.getProfileId() == null) pm.updateProfile(user, dbConn);
		} catch (DatabaseException de) {
			//these are recoverable, so don't sweat them too much.
			log.warn("could not save person's profile to WC");
		}
		return new GenericVO(user.getProfileId(), roleCd);
	}
	
	
	/**
	 * Ad management is done in a separate Object, because it's reused for Newspaper and Radio ads.
	 * @param eventPostcardId
	 * @param req
	 * @throws SQLException
	 */
	private void saveNewspaperAd(String eventPostcardId, SMTServletRequest req) throws SQLException {
		//pass the seminar along with this, so emails can be sent.
		DePuyEventSeminarVO sem = fetchSeminar(req, ReportType.summary);
		req.setAttribute("postcard", sem);
		
		CoopAdsActionV2 caa = new CoopAdsActionV2();
		caa.setAttributes(attributes);
		caa.setDBConnection(dbConn);
		try {
			caa.build(req);
		} catch (ActionException ae) {
			throw new SQLException(ae);
		}
		
		//if radio = adType, possibly trigger an email here.  There is only one submission that will be type=radio
	}
	
	
	/**
	 * inserts or updates the DEPUY_EVENT_SURGEON table.
	 * @param eventPostcardId
	 * @param req
	 * @throws SQLException
	 */
	private void saveEventSurgeon(String eventPostcardId, SMTServletRequest req, SiteVO site) throws SQLException {
		String pkId = req.hasParameter("surgeonId") ? req.getParameter("surgeonId") : null;
		StringBuilder sql = new StringBuilder();
		if (pkId != null) {
			sql.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DEPUY_EVENT_SURGEON set surgeon_nm=?, cv_file_url=?, ");
			sql.append("logo_file_url=?, seen_guidelines_flg=?, hosp_employee_flg=?, ");
			sql.append("hosp_address_txt=?, exp_yrs_no=?, practice_nm=?, pract_yrs_no=?, ");
			sql.append("pract_addr1_txt=?, pract_addr2_txt=?, pract_city_nm=?, pract_state_cd=?, ");
			sql.append("pract_zip_cd=?, pract_phone_txt=?, pract_email_txt=?, pract_website_url=?, ");
			sql.append("sec_phone_txt=?, sec_email_txt=?, bio_txt=?, update_dt=?, event_postcard_id=? where depuy_event_surgeon_id=?");
		} else {
			sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DEPUY_EVENT_SURGEON (surgeon_nm, cv_file_url, ");
			sql.append("logo_file_url, seen_guidelines_flg, hosp_employee_flg, ");
			sql.append("hosp_address_txt, exp_yrs_no, practice_nm, pract_yrs_no, ");
			sql.append("pract_addr1_txt, pract_addr2_txt, pract_city_nm, pract_state_cd, ");
			sql.append("pract_zip_cd, pract_phone_txt, pract_email_txt, pract_website_url, ");
			sql.append("sec_phone_txt, sec_email_txt, bio_txt, create_dt, event_postcard_id, depuy_event_surgeon_id) ");
			sql.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			pkId = new UUIDGenerator().getUUID();
		}
		log.debug(sql + "|" + pkId);

		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("surgeonName"));
			ps.setString(2, saveFile(req, "cvFile", "/cv-files/", site));
			ps.setString(3, saveFile(req, "logoFile", "/logos/", site));
			ps.setInt(4, Convert.formatInteger(req.getParameter("seenGuidelines"), 0));
			ps.setInt(5, Convert.formatInteger(req.getParameter("hospEmployee"), 0));
			ps.setString(6, req.getParameter("hospAddress"));
			ps.setInt(7, Convert.formatInteger(req.getParameter("experienceYears"), 0));
			ps.setString(8, req.getParameter("practName"));
			ps.setInt(9, Convert.formatInteger(req.getParameter("practYears"), 0));
			ps.setString(10, req.getParameter("practAddr1"));
			ps.setString(11, req.getParameter("practAddr2"));
			ps.setString(12, req.getParameter("practCity"));
			ps.setString(13, req.getParameter("practState"));
			ps.setString(14, req.getParameter("practZip"));
			ps.setString(15, req.getParameter("practPhone"));
			ps.setString(16, req.getParameter("practEmail"));
			ps.setString(17, req.getParameter("practWebsite"));
			ps.setString(18, req.getParameter("secPhone"));
			ps.setString(19, req.getParameter("secEmail"));
			ps.setString(20, req.getParameter("surgeonBio"));
			ps.setTimestamp(21, Convert.getCurrentTimestamp());
			ps.setString(22, eventPostcardId);
			ps.setString(23, pkId);
			
			if (ps.executeUpdate() < 1)
				throw new SQLException(ps.getWarnings());

		} finally {
			try { ps.close(); } catch (Exception e) { }
		}
	}
	
	
	/**
	 * helper method to above file upload requirements.  
	 * Intended to be reused by other Seminars Objects as needed.
	 * @param req
	 * @param paramNm
	 * @param subPath
	 * @param site
	 * @return
	 */
	protected String saveFile(SMTServletRequest req, String paramNm, String subPath, SiteVO site) {
		//build a file-system path the server can understand, using WC config
		StringBuilder absPath = new StringBuilder((String) getAttribute("pathToBinary"));
		absPath.append(getAttribute("orgAlias")).append(site.getOrganizationId());
		absPath.append("/").append(site.getSiteId()).append(subPath);

		FileLoader fl = null;
		FilePartDataBean fpdb = req.getFile(paramNm);
		String origFileNm = StringUtil.checkVal(req.getParameter("orig-" + paramNm));
		String newFileNm = (fpdb != null) ? StringUtil.checkVal(fpdb.getFileName()) : "";
		log.debug("newFile=" + newFileNm + " origFile=" + origFileNm);

		// delete the orig file if we have one to replace it
		if (origFileNm.length() > 0 && newFileNm.length() > 0) {
			try {
				fl = new FileLoader(attributes);
				fl.setPath(absPath.toString());
				fl.setFileName(origFileNm);
				fl.deleteFile();
				log.debug("file deleted");
				origFileNm = "";  //this file is gone now, so dereference it
			} catch (Exception e) {
				log.error("error deleting " + paramNm + " origFile", e);
			}
		}

		// Write new file
		if (newFileNm.length() > 0) {
			try {
				fl = new FileLoader(attributes);
				fl.setFileName(newFileNm);
				fl.setPath(absPath.toString());
				fl.setRename(true);
				fl.setOverWrite(false);
				fl.setData(fpdb.getFileData());
				origFileNm = fl.writeFiles();  //this re-assignment accounts for renamed files
			} catch (Exception e) {
				log.error("error writing " + paramNm + " newFile", e);
			}
		}

		fpdb = null;
		fl = null;
		return origFileNm;
	}
	

	/**
	 * delete the old records to avoid the hassles of an update query
	 * @param eventPostcardId
	 * @throws ActionException
	 */
	private void deleteSavedLeadCities( String eventPostcardId) throws SQLException {
		PreparedStatement ps = null;
		StringBuilder sql = new StringBuilder();
		sql.append("delete from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("depuy_event_leads_datasource where event_postcard_id=?");
		log.debug(sql);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, eventPostcardId);
			ps.executeUpdate();

		} finally {
			try { ps.close();	} catch (Exception e) { }
		}
	}
	/**
	 * delete old leads-table entries and re-insert the new values. Call
	 * LeadsDataTool to get an count on the of leads for each zip (city counts
	 * come in on the req)
	 * 
	 * @param req
	 * @throws ActionException
	 */
	private void saveLeadCities(SMTServletRequest req, String eventPostcardId,
			UserDataVO user, Integer roleId) throws SQLException {
		message = "Leads Saved Successfully";
		
		// loop the cities on the request and insert each record
		StringBuilder sql = new StringBuilder();
		sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("depuy_event_leads_datasource (event_lead_source_id, ");
		sql.append("event_postcard_id, state_cd, city_nm, zip_cd, ");
		sql.append("product_cd, max_age_no, create_dt) values (?,?,?,?,?,?,?,?)");
		log.debug(sql);

		int batchCnt = 0;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
		} catch (Exception e) {
			log.error("could not open PS to start batch query", e);
		}

		//iterate the request to find the fields we need to save.  They have ugly parameterNames!
		//"leads|${loc.state}|${loc.city}|${loc.zipCode}|${loc.address}|${loc.locationId}"
		Enumeration<String> reqParams = req.getParameterNames();
		UUIDGenerator uuid = new UUIDGenerator();
		int totalLeadCnt = 0; //gets insert into the event_postcard table later.
		SortType sortType = null;
		try {
			sortType = SortType.valueOf(req.getParameter("sortType")); 
		} catch (Exception e) {
			sortType = SortType.city;
		}
		try {
			while (reqParams.hasMoreElements()) {
				String param = reqParams.nextElement();
				if (!param.startsWith("leads|")) continue;  //not one we want!
				
				String[] tokens = param.split("\\|");
				String[] vals = req.getParameter(param).split("\\|");
				if (tokens.length != 6 || vals.length != 2) {
					log.error("nonconfirmist: param: " + param + " val: " + req.getParameter(param));
					log.error(StringUtil.getToString(tokens, false, false, "|"));
					continue;
				}
				
				try {
					ps.setString(1, uuid.getUUID());
					ps.setString(2, eventPostcardId);
					ps.setString(3, tokens[1]); //state
					ps.setString(4, (SortType.zip == sortType) ? null : tokens[2]); //city
					ps.setString(5, (SortType.zip != sortType) ? null : tokens[3]); //zip
					ps.setString(6, tokens[4].toUpperCase()); //product
					ps.setInt(7, Convert.formatInteger(vals[1], 240));
					ps.setTimestamp(8, Convert.getCurrentTimestamp());

					ps.addBatch();
					++batchCnt;

					// occasionally commit the transaction
					if (batchCnt == Constants.MAX_SQL_BATCH_SIZE) { 
						ps.executeBatch();
						batchCnt= 0;
					}
					totalLeadCnt += Convert.formatInteger(vals[0]);
					
				} catch (SQLException sqle) {
					log.error("could not save lead city", sqle);
					message = "Could not save all lead cities";
				}

			}
			if (batchCnt > 0)
				ps.executeBatch();

		} finally {
			try { ps.close(); } catch (Exception e) { }
		}
		
		this.updatePostcardLeadsStats(totalLeadCnt, eventPostcardId, sortType);
	}

	/**
	 * called when the event-owner submits their postcard/events for approval
	 * 
	 * @param req
	 * @param eventPostcardId
	 * @throws ActionException
	 */
	private void submitPostcard(SMTServletRequest req, String eventPostcardId)
			throws ActionException {
		// change the postcard status to submitted
		this.changePostcardStatus(EventFacadeAction.STATUS_PENDING, eventPostcardId);
		message = "Seminar Submitted Successfully";

		// get the postcard data for emailing & approving each event
		DePuyEventSeminarVO sem = fetchSeminar(req, ReportType.summary);
		req.setAttribute("postcard", sem);
		
		// send approval request email
		PostcardEmailer epe = new PostcardEmailer(attributes, dbConn);
		epe.sendApprovalRequest(req); //notification to site admin
		
		//reload the seminar with complaince form report
		sem = fetchSeminar(req, ReportType.compliance);
		req.setAttribute("postcard", sem);
		epe.sendAdvApprovalRequest(req); //request for approval from ASM (Adv. team leaders)

		return;
	}
	

	/**
	 * called when the admin approves a postcard/events
	 *  ADV-team approval - they do this themselves after reviewing the 
	 *  compliance PDF we emailed during submitPostcard()
	 * @param req
	 * @param eventPostcardId
	 * @throws ActionException
	 */
	private void advApprovePostcard(SMTServletRequest req, String eventPostcardId, UserDataVO user)
			throws SQLException, ActionException {
		// change the postcard status to approved
		this.changePostcardStatus(EventFacadeAction.STATUS_PENDING_PREV_ATT, eventPostcardId);
		
		//start by deleting the old data, since this is an _XR table this is easier than a double-cross-reference to do updates.
		StringBuilder sql = new StringBuilder();
		sql.append("delete from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_EVENT_PERSON_XR where postcard_role_cd ='ADV' and event_postcard_id=?");
		log.debug(sql + "|" + eventPostcardId);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, eventPostcardId);
			ps.executeUpdate();  //a zero here is okay, an SQLException is not and will be thrown.
		} finally {
			try { ps.close(); } catch (Exception e) { }
		}
		
		//begin the inserts
		sql = new StringBuilder();
		sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_EVENT_PERSON_XR (depuy_event_person_xr_id,  ");
		sql.append("profile_id, event_postcard_id, postcard_role_cd, create_dt) values (?,?,?,?,?)");
		log.debug(sql + "|" + eventPostcardId);

		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, user.getProfileId());
			ps.setString(3, eventPostcardId);
			ps.setString(4, PersonVO.Role.ADV.toString());
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			ps.executeUpdate();
		} finally {
			try { ps.close(); } catch (Exception e) { }
		}
		message = "The Seminar was Approved";

		//grab the compliance form and set it aside so we can also get the summary;
		//this email has two attachments.
		DePuyEventSeminarVO sem = fetchSeminar(req, ReportType.compliance);
		req.setAttribute("postcard", sem);
		req.setAttribute("complianceForm", req.getAttribute(Constants.BINARY_DOCUMENT));

		// this second call will put the summary report (Excel) on the request object for us.
		//we can disregard the returned SeminarVO, we already have it.
		fetchSeminar(req, ReportType.summary);
		
		// send approval request email
		PostcardEmailer epe = new PostcardEmailer(attributes, dbConn);
		epe.sendAdvApproved(req);

		return;
	}
	
	
	/**
	 * called when the SRC admin approves a postcard/events
	 * The site admin inputs this approval, which fires an email announcing the milestone
	 * @param req
	 * @param eventPostcardId
	 * @throws ActionException
	 */
	private void srcApprovePostcard(SMTServletRequest req, String eventPostcardId)
			throws ActionException {
		// change the postcard status to approved
		this.changePostcardStatus(EventFacadeAction.STATUS_APPROVED_SRC, eventPostcardId);
		message = "The Seminar was approved by SRC";
		
		// get the postcard data for emailing & approving each event
		DePuyEventSeminarVO sem = fetchSeminar(req, ReportType.summary);
		req.setAttribute("postcard", sem);
		
		// send approval request email
		PostcardEmailer epe = new PostcardEmailer(attributes, dbConn);
		epe.sendSrcApproved(req); //captured by the site admin clicking a button that SRC has approved the Seminar

		return;
	}
	
	/**
	 * called when the admin moves the status to pending surgeon review
	 * The site admin inputs this approval
	 * @param req
	 * @param eventPostcardId
	 * @throws ActionException
	 */
	private void pendingSurgeonReview(SMTServletRequest req, String eventPostcardId)
			throws ActionException {
		// change the postcard status to approved
		this.changePostcardStatus(EventFacadeAction.STATUS_PENDING_SURG, eventPostcardId);
		message = "The Seminar was updated successfully";
		
		return;
	}
	
	
	/**
	 * called when the SRC admin approves a postcard/events
	 * The site admin inputs this approval, which fires an email announcing the milestone
	 * @param req
	 * @param eventPostcardId
	 * @throws ActionException
	 */
	private void approvedMedAffairs(SMTServletRequest req, String eventPostcardId)
			throws ActionException {
		// change the postcard status to approved
		this.changePostcardStatus(EventFacadeAction.STATUS_APPROVED, eventPostcardId);
		message = "The Seminar was updated successfully";
		
		// get the postcard data for emailing & approving each event
		DePuyEventSeminarVO sem = fetchSeminar(req, ReportType.summary);
		req.setAttribute("postcard", sem);
		
		// send notification email to the admins
		log.debug("starting approval email");
		PostcardEmailer epe = new PostcardEmailer(attributes, dbConn);
		epe.sendMedicalAffairsApprovedNotice(req);
		
		return;
	}
	
	
	/**
	 * Helper method that gets reused, simply calles the Select action to retrieve the Seminar data
	 * Used for outgoing emails.
	 * @param req
	 * @return
	 */
	private DePuyEventSeminarVO fetchSeminar(SMTServletRequest req, ReportType reportType) {
		DePuyEventManageActionV2 epsa = new DePuyEventManageActionV2(actionInit);
		epsa.setDBConnection(dbConn);
		epsa.setAttributes(attributes);
		DePuyEventSeminarVO sem = null;
		try {
			req.setParameter(AdminConstants.FACADE_TYPE, "report");
			req.setParameter("rptType", reportType.toString());
			epsa.build(req);
			ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
			sem = (DePuyEventSeminarVO) mod.getActionData();
		} catch (ActionException ae) {
			log.error("retrievingPostcardInfoForEmails", ae);
		}
		
		//NOTE: we're returning the Seminar VO, but any requested report was
		//placed on the request object directly.  (See AbstractReportVO intfc)
		return sem;
	}

	/**
	 * generic method to change postcard status levels. automatically updates
	 * the dependant event_entry records.
	 * 
	 * @param statusLvl
	 * @param eventPostcardId
	 * @throws ActionException
	 */
	private void changePostcardStatus(int statusLvl, String eventPostcardId)
			throws ActionException {
		PreparedStatement ps = null;
		StringBuilder sql = new StringBuilder();
		sql.append("update event_postcard set status_flg=?, update_dt=? where event_postcard_id=?");
		log.debug("EventPostcardSubmitSQL: " + sql);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, statusLvl);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, eventPostcardId);
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("failed deleting eventPostcard", sqle);
			message = "Transaction Failed";
			throw new ActionException(sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {
			}
		}

		// change the dependant event's status as well
		sql = new StringBuilder();
		sql.append("update event_entry set status_flg=?, update_dt=? where event_entry_id in (");
		sql.append("select event_entry_id from event_postcard_assoc where event_postcard_id=?)");
		log.debug("EventEntryApproveSQL: " + sql);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, statusLvl);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, eventPostcardId);
			ps.executeUpdate();
		} catch (Exception de) {
			log.error("failed approving new events", de);
			message = "Transaction Failed";
			throw new ActionException(de);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {
			}
		}

	}

/*
	private void deletePostcard(SMTServletRequest req, String eventPostcardId)
			throws ActionException {
		// change the postcard status to deleted
		this.changePostcardStatus(EventFacadeAction.STATUS_DELETED, eventPostcardId);
		message = "The Seminar was Deleted";
		return;
	}
*/
	private void cancelPostcard(SMTServletRequest req, String eventPostcardId)
			throws ActionException {
		// change the postcard status to deleted
		this.changePostcardStatus(EventFacadeAction.STATUS_CANCELLED,eventPostcardId);
		message = "The Seminar was Cancelled";

		// get the postcard data for emailing & approving each event
		DePuyEventSeminarVO sem = fetchSeminar(req, ReportType.summary);
		req.setAttribute("postcard", sem);
		
		// send notification email to the admins
		log.debug("starting cancellation email");
		PostcardEmailer epe = new PostcardEmailer(attributes, dbConn);
		epe.sendPostcardCancellation(req);

		return;
	}
	
	
	/**
	 * marks the Seminar status as completed; called from the postseminar page. 
	 * @param eventPostcardId
	 * @throws ActionException
	 */
	private void completePostcard(String eventPostcardId)
			throws ActionException {
		// change the postcard status to completed
		this.changePostcardStatus(EventFacadeAction.STATUS_COMPLETE, eventPostcardId);
		message = "The Seminar was marked as Completed";
		return;
	}
	
	
	private void uploadPostcard(SMTServletRequest req, String eventPostcardId, SiteVO site)
			throws ActionException {
		PreparedStatement ps = null;
		StringBuilder sql = new StringBuilder();
		sql.append("update event_postcard set postcard_file_url=?, postcard_file_status_no=?, ");
		sql.append("update_dt=? where event_postcard_id=?");
		log.debug(sql);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, this.saveFile(req, "postcardFile", "/postcards/", site));
			ps.setInt(2, CoopAdsActionV2.PENDING_CLIENT_APPROVAL);
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.setString(4, eventPostcardId);
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("failed deleting eventPostcard", sqle);
			message = "Transaction Failed";
			throw new ActionException(sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {
			}
		}

		// get the postcard data for emailing & approving each event
		DePuyEventSeminarVO sem = fetchSeminar(req, ReportType.summary);
		req.setAttribute("postcard", sem);
		
		//send an email to the coordinator here...
		PostcardEmailer epe = new PostcardEmailer(attributes, dbConn);
		epe.requestPostcardApproval(req);
		
	}
	
	private void approvePostcardFile(SMTServletRequest req, String eventPostcardId)
			throws ActionException {
		PreparedStatement ps = null;
		StringBuilder sql = new StringBuilder();
		sql.append("update event_postcard set postcard_file_status_no=?, ");
		sql.append("update_dt=? where event_postcard_id=?");
		log.debug(sql);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, CoopAdsActionV2.CLIENT_APPROVED_AD);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, eventPostcardId);
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("failed approving postcard file", sqle);
			message = "Transaction Failed";
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) { }
		}
		
		// get the postcard data for emailing & approving each event
		DePuyEventSeminarVO sem = fetchSeminar(req, ReportType.summary);
		req.setAttribute("postcard", sem);
		
		//send an email to the Admins and Harmony here...
		PostcardEmailer epe = new PostcardEmailer(attributes, dbConn);
		epe.sendPostcardApproved(req);
		
	}
	
	
	/**
	 * sends an email to Lincon containing a PO for a consumable box.  
	 * Not captured in the database, per order of Depuy.
	 * @param req
	 * @param eventPostcardId
	 * @throws ActionException
	 */
	private void orderBox(SMTServletRequest req, String eventPostcardId)
			throws ActionException {
		log.debug("starting consumable box email");

		// get the postcard data for emailing
		DePuyEventSeminarVO sem = fetchSeminar(req, ReportType.summary);
		req.setAttribute("postcard", sem);
		
		PostcardEmailer epe = new PostcardEmailer(attributes, dbConn);
		epe.orderConsumableBox(req);
		epe.orderConsumableBoxConfirmation(req);
		
		message = "Emails sent successfully";
		return;
	}
	
	
	/**
	 * updates the EVENT_POSTCARD table with some stats about the leads, 
	 * so we don't need to re-retrieve all that bulky data on each load.
	 * @param leadCnt
	 * @param eventPostcardId
	 * @param sortType
	 * @throws ActionException
	 */
	private void updatePostcardLeadsStats(int leadCnt, String eventPostcardId, SortType sortType)
			throws SQLException {
		PreparedStatement ps = null;
		StringBuilder sql = new StringBuilder();
		sql.append("update event_postcard set attrib_1_txt=?, attrib_2_txt=?, ");
		sql.append("update_dt=? where event_postcard_id=?");
		log.debug(sql);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, Integer.valueOf(leadCnt).toString());
			ps.setString(2, sortType.toString());
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.setString(4, eventPostcardId);
			ps.executeUpdate();

		} finally {
			try { ps.close(); } catch (Exception e) { }
		}
	}

}