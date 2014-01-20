package com.depuy.events_v2;

// JDK 1.6.3
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

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
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.depuy.events.AbstractPostcardEmailer;
import com.depuy.events.LeadsDataTool;
import com.depuy.events_v2.vo.DePuyEventSeminarVO;
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
		eventInfo, submitSeminar, srcApproveSeminar, approveSeminar, 
		cancelSeminar, orderBox, uploadPostcard, approvePostcardFile, uploadAdFile, approveNewspaperAd
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
		 boolean isNewSeminar = (eventPostcardId == null);
		 
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
					if (isNewSeminar) {
						req.setParameter("eventPostcardId", eventPostcardId);
						saveEventPostcardAssoc(eventPostcardId, eventEntryId);
						saveLocatorXr(eventPostcardId, req);
					}
					saveEventPersonXr(eventPostcardId, req);
					saveNewspaperAd(eventPostcardId, req);
					saveEventSurgeon(eventPostcardId, req, site);
					if (nextPage.length() == 0) nextPage = "summary";
					break;
					
				case submitSeminar:
					this.submitPostcard(req, eventPostcardId);
					break;
					
				case approveSeminar:
					this.approvePostcard(req, eventPostcardId);
					break;
					
				case srcApproveSeminar:
					this.srcApprovePostcard(req, eventPostcardId);
					break;
						
				case cancelSeminar:
					this.cancelPostcard(req, eventPostcardId);
					break;
					
				case uploadAdFile:
				case approveNewspaperAd:
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
			}
		} catch (SQLException e) {
			log.error("could not save transaction " + reqType + ", " + e.getMessage(), e);
			throw new ActionException();
		}

		/*
		 * 
		 * } else if (reqType.equals("postcardLeads")) { SBUserRole roles =
		 * (SBUserRole) ses.getAttribute(Constants.ROLE_DATA); Integer roleId
		 * = Convert.formatInteger(roles.getRoleId());
		 * 
		 * this.saveLeads(req, eventPostcardId, user, roleId);
		 * 
		 * } else if (reqType.startsWith("coopAds") ||
		 * reqType.startsWith("radioAds")) { SMTActionInterface ac = new
		 * CoopAdsAction(this.actionInit); ac.setDBConnection(dbConn);
		 * ac.setAttributes(this.attributes); ac.build(req); }
		 */

		// setup the redirect url
		StringBuilder redirectPg = new StringBuilder();
		redirectPg.append(req.getRequestURI()).append("?reqType=").append(nextPage);
		redirectPg.append("&eventPostcardId=").append(eventPostcardId);
		redirectPg.append("&msg=").append(message);
		log.debug("redirUrl=" + redirectPg);

		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, redirectPg.toString());
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
		if (pkId != null) {
			sql.append("update event_postcard set update_dt=?, quantity_no=?, ");
			sql.append("mailing_addr_txt=?, label_txt=?, content_no=? where event_postcard_id=?");
		} else {
			sql.append("insert into event_postcard (organization_id, profile_id, ");
			sql.append("create_dt, quantity_no, mailing_addr_txt, label_txt, content_no, ");
			sql.append("event_postcard_id) values (?,?,?,?,?,?,?,?)");
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
			ps.setString(x++, req.getParameter("postcardLabel"));
			ps.setString(x++, req.getParameter("contentNo"));
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
	    	vo.setContactName(null); //overlaps paramNm from CoopAds. not used here.
	    	
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
		sql.append("DEPUY_EVENT_PERSON_XR where event_postcard_id=?");
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
			if (ps.executeBatch().length < 1) 
				throw new SQLException(ps.getWarnings());

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
			user.setProfileId(pm.checkProfile(user, dbConn));
			if (user.getProfileId() == null) pm.updateProfile(user, dbConn);
		} catch (DatabaseException de) {
			log.error("could not save person's profile to WC", de);
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
	 * delete old leads-table entries and re-insert the new values. Call
	 * LeadsDataTool to get an count on the of leads for each zip (city counts
	 * come in on the req)
	 * 
	 * @param req
	 * @throws ActionException
	 */
	private void saveLeads(SMTServletRequest req, String eventPostcardId,
			UserDataVO user, Integer roleId) throws ActionException {
		message = "Leads Saved Successfully";
		PreparedStatement ps = null;
		String stateCd = StringUtil.checkVal(req.getParameter("state_cd"));

		// delete the old records to avoid the hassles of an update query
		StringBuilder sql = new StringBuilder();
		sql.append("delete from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("depuy_event_leads_datasource where event_postcard_id=? and state_cd=?");
		log.info("EventLeadsDeleteSQL: " + sql);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, eventPostcardId);
			ps.setString(2, stateCd);
			ps.executeUpdate();

		} catch (SQLException sqle) {
			log.error("error", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {
			}
		}

		// loop the cities on the request and insert each record
		sql = new StringBuilder();
		sql.append("insert into ").append(
				getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("depuy_event_leads_datasource (event_lead_source_id, ");
		sql.append("event_postcard_id, state_cd, city_nm, zip_cd, ");
		sql.append("est_leads_no, create_dt) values (?,?,?,?,?,?,?)");
		log.info("EventLeadsInsertSQL: " + sql);

		int psCnt = 0;
		try {
			ps = dbConn.prepareStatement(sql.toString());
		} catch (Exception e) {
			log.error("turning off auto-commit", e);
		}

		String[] cities = req.getParameterValues("city");
		try {
			for (int x = 0; cities != null && x < cities.length; x++) {
				String val = StringUtil.checkVal(cities[x]);
				String cityNm = val.substring(0, val.lastIndexOf("|"));
				int leadCnt = Convert.formatInteger(val.substring(val
						.lastIndexOf("|") + 1));

				// log.debug("city=" + cityNm + " size=" + leadCnt);
				try {
					ps.setString(1, new UUIDGenerator().getUUID());
					ps.setString(2, eventPostcardId);
					ps.setString(3, stateCd);
					ps.setString(4, cityNm);
					ps.setString(5, null);
					ps.setInt(6, leadCnt);
					ps.setTimestamp(7, Convert.getCurrentTimestamp());

					ps.addBatch();
					++psCnt;

					if (psCnt == Constants.MAX_SQL_BATCH_SIZE) { // occasionally
														// commit
														// the
														// transaction
						ps.executeBatch();
						log.debug("COMITTED AT " + psCnt);
						psCnt = 0;
					}
				} catch (SQLException sqle) {
					log.error("could not save lead city", sqle);
					message = "Could not save all lead cities";
				}

			}
			if (psCnt > 0) {
				ps.executeBatch();
				log.debug("COMITTED AT " + psCnt);
			}
		} catch (SQLException sqle) {
			log.error("could not save all lead cities", sqle);
			message = "Could not save lead cities";
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {
				}
			}
		}

		// setup zipCodes SQL for insert
		String zipString = StringUtil.replace(req.getParameter("zipcodes"),
				"\r\n", ",");
		StringTokenizer zips = new StringTokenizer(zipString, ",");
		LeadsDataTool edt = new LeadsDataTool(actionInit);
		edt.setAttributes(this.attributes);
		edt.setDBConnection(dbConn);
		while (zips.hasMoreTokens()) {
			String zip = StringUtil.checkVal(zips.nextToken()).trim();
			log.debug("found zip " + zip);
			if (zip.length() == 0 || zip.length() > 10)
				continue; // skip empty or malformed

			Integer leadCnt = edt.countLeadsByZip(zip,
					req.getParameter("groupCd"), stateCd, user, roleId);
			try {
				// this stmt uses the same sql as the cities (above)
				ps = dbConn.prepareStatement(sql.toString());
				ps.setString(1, new UUIDGenerator().getUUID());
				ps.setString(2, eventPostcardId);
				ps.setString(3, stateCd);
				ps.setString(4, null);
				ps.setString(5, zip);
				ps.setInt(6, leadCnt);
				ps.setTimestamp(7, Convert.getCurrentTimestamp());
				ps.executeUpdate();
				// NOTE: We do not batch these statements because rarely are
				// there more than a handful of zips (if any)
			} catch (SQLException sqle) {
				log.error("could not save lead zips", sqle);
				message = "Could not save lead Zips";
			} finally {
				try {
					dbConn.commit();
					ps.close();
				} catch (Exception e) {
				}
			}

		}

		edt = null;
		return;
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
		req.setParameter("reqType", "report");
		req.setParameter("rptType", Integer.valueOf(	LeadsDataTool.POSTCARD_SUMMARY_PULL).toString());
		
		DePuyEventSeminarVO sem = fetchSeminar(req);
		req.setAttribute("postcard", sem);
		
		// send approval request email
		AbstractPostcardEmailer epe = AbstractPostcardEmailer.newInstance(null, attributes, dbConn);
		epe.sendApprovalRequest(req);

		return;
	}
	
	/**
	 * called when the admin approves a postcard/events
	 * 
	 * @param req
	 * @param eventPostcardId
	 * @throws ActionException
	 */
	private void srcApprovePostcard(SMTServletRequest req, String eventPostcardId)
			throws ActionException {
		// change the postcard status to approved
		this.changePostcardStatus(EventFacadeAction.STATUS_APPROVED, eventPostcardId);
		message = "The Seminar was approved by SRC";
//
//		// get the postcard data for emailing & approving each event
//		req.setParameter("reqType", "report");
//		req.setParameter("rptType", Integer.valueOf(LeadsDataTool.POSTCARD_SUMMARY_PULL).toString());
//		
//		DePuyEventSeminarVO sem = fetchSeminar(req);
//		req.setAttribute("postcard", sem);
//
//		// send notification emails w/postcard data
//		AbstractPostcardEmailer epe = AbstractPostcardEmailer.newInstance(null, attributes, dbConn);
//
//		// send owner "event approved" response email
//		log.debug("starting rep notification email");
//		epe.sendApprovedResponse(req);
//
//		// email vendor, also sends to DePuy proj managers (CC: on email)
//		log.debug("starting vendor notification email");
//		epe.sendVendorSummary(req);
//		log.debug("completed vendor notification email");
//
//		// email territory admin and rep the pre-auth paperwork
//		log.debug("starting pre-auth documents email");
//		epe.sendPreAuthPaperwork(req);
//		log.debug("completed pre-auth documents email");

		return;
	}
	

	/**
	 * called when the admin approves a postcard/events
	 * 
	 * @param req
	 * @param eventPostcardId
	 * @throws ActionException
	 */
	private void approvePostcard(SMTServletRequest req, String eventPostcardId)
			throws ActionException {
		// change the postcard status to approved
		this.changePostcardStatus(EventFacadeAction.STATUS_PENDING_PREV_ATT, eventPostcardId);
		message = "The Seminar was Approved";

		// get the postcard data for emailing & approving each event
		req.setParameter("reqType", "report");
		req.setParameter("rptType", Integer.valueOf(LeadsDataTool.POSTCARD_SUMMARY_PULL).toString());
		
		DePuyEventSeminarVO sem = fetchSeminar(req);
		req.setAttribute("postcard", sem);
//
//		// send notification emails w/postcard data
//		AbstractPostcardEmailer epe = AbstractPostcardEmailer.newInstance(null, attributes, dbConn);
//
//		// send owner "event approved" response email
//		log.debug("starting rep notification email");
//		epe.sendApprovedResponse(req);
//
//		// email vendor, also sends to DePuy proj managers (CC: on email)
//		log.debug("starting vendor notification email");
//		epe.sendVendorSummary(req);
//		log.debug("completed vendor notification email");
//
//		// email territory admin and rep the pre-auth paperwork
//		log.debug("starting pre-auth documents email");
//		epe.sendPreAuthPaperwork(req);
//		log.debug("completed pre-auth documents email");

		return;
	}
	
	
	/**
	 * Helper method that gets reused, simply calles the Select action to retrieve the Seminar data
	 * Used for outgoing emails.
	 * @param req
	 * @return
	 */
	private DePuyEventSeminarVO fetchSeminar(SMTServletRequest req) {
		PostcardSelectV2 epsa = new PostcardSelectV2(actionInit);
		epsa.setDBConnection(dbConn);
		epsa.setActionInit(actionInit);
		epsa.setAttributes(attributes);
		DePuyEventSeminarVO sem = null;
		try {
			epsa.retrieve(req);
			ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
			sem = (DePuyEventSeminarVO) mod.getActionData();
		} catch (ActionException ae) {
			log.error("retrievingPostcardInfoForEmails", ae);
		}
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
		req.setParameter("reqType", "report");
		req.setParameter("rptType", Integer.valueOf(LeadsDataTool.POSTCARD_SUMMARY_PULL).toString());
		
		DePuyEventSeminarVO sem = fetchSeminar(req);
		req.setAttribute("postcard", sem);
		
		// send notification email to the admins
		log.debug("starting cancellation email");
		AbstractPostcardEmailer epe = AbstractPostcardEmailer.newInstance(null, attributes, dbConn);
		epe.sendPostcardCancellation(req);

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
			try {
				ps.close();
			} catch (Exception e) {
			}
		}
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
		AbstractPostcardEmailer epe = AbstractPostcardEmailer.newInstance(null, attributes, dbConn);
		//epe.orderConsumableBox(req);
		message = "Email sent successfully";
		return;
	}

}