package com.depuy.events;

// JDK 1.5.0
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.StringTokenizer;

import com.siliconmtn.http.session.SMTSession;


// SMT BaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.databean.FilePartDataBean;

// SB Libs
import com.smt.sitebuilder.action.FileLoader;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.event.EventFacadeAction;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.depuy.events.vo.DePuyEventPostcardVO;

/****************************************************************************
 * <b>Title</b>: EventPostcardInsert.java<p/>
 * <b>Description: Manages EventActions for the SB Sites</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2005<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 1.0
 * @since Mar 15, 2006
 ****************************************************************************/
public class PostcardInsert extends SBActionAdapter {
	public static final String RETR_EVENTS = "retrEvents";
	private String message = "Postcard Saved Successfully";
	
	public PostcardInsert() {
		super();
	}

	public PostcardInsert(ActionInitVO arg0) {
		super(arg0);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		SMTSession ses = req.getSession();
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		UserDataVO user = (UserDataVO) ses.getAttribute(Constants.USER_DATA);
		String reqType = StringUtil.checkVal(req.getParameter("reqType"));
		String nextPage = StringUtil.checkVal(req.getParameter("nextPage"));
		String profileId = (user != null) ? user.getProfileId() : "";
		
		//user must be logged in, or approving a surgeonAd for CFSEM
		if (StringUtil.checkVal(profileId).length() == 0 && !reqType.startsWith("coopAds")) return;
		
		String language = StringUtil.checkVal(req.getParameter("lang"));
		String product = StringUtil.checkVal(req.getParameter("product"));
		String eventEntryId = req.getParameter("eventEntryId");
		String eventPostcardId = req.getParameter("eventPostcardId");
		int eventNo = Convert.formatInteger(req.getParameter("eventNo"));
		int totalEvents = Convert.formatInteger(req.getParameter("eventCnt"));
		if (totalEvents == 0) totalEvents = Convert.formatInteger((Integer) ses.getAttribute("postcardSize"));
		log.debug("totalEvents=" + totalEvents + " eventNo=" + eventNo);
		
		if (nextPage.trim().length() > 0) {
			//leave it alone
		} else if (eventNo == totalEvents && language.equals("en") && !product.equalsIgnoreCase("shoulder") && !"CPSEM".equalsIgnoreCase(req.getParameter("eventTypeCd"))) {
			nextPage = "leads";
		} else if (eventNo == totalEvents && (language.equals("es") || product.equalsIgnoreCase("shoulder")) || "CPSEM".equalsIgnoreCase(req.getParameter("eventTypeCd"))) {
			nextPage = "review";
		} else if (eventNo < totalEvents) {
			nextPage = "event";
		}
		
		//insert postcard
		if (reqType.equals("postcardForm")) {
			if (Convert.formatInteger(req.getParameter("eventCnt")) > 0) {
				 //only save the postcard once, tied to first event
				eventPostcardId = this.saveEventPostcard(req, site, user);
				ses.setAttribute("postcardSize",totalEvents);
				log.debug("postcard saved");
			}

			eventEntryId = this.saveEvent(req);
			log.debug("event " + eventEntryId + " saved");
				
			//insert event_addtl_postcards
			this.saveEventAddtlPostcards(req, eventEntryId);
			log.debug("addtl postcards for event saved");

			Integer orderBy = Convert.formatInteger(req.getParameter("orderNumber"));
			if (orderBy == 0) orderBy = eventNo;			
			this.saveEventPostcardAssoc(eventPostcardId, eventEntryId, orderBy);
			log.debug("postcard assoc saved");
			
		} else if (reqType.equals("postcardLeads")) {
			SBUserRole roles = (SBUserRole) ses.getAttribute(Constants.ROLE_DATA);
			Integer roleId = Convert.formatInteger(roles.getRoleId());
			
			this.saveLeads(req, eventPostcardId, user, roleId);
			
		} else if (reqType.equals("submit")) {
			this.submitPostcard(req, eventPostcardId);
		
		} else if (reqType.equals("approve")) {
			this.approvePostcard(req, eventPostcardId);
			
		} else if (reqType.equals("delPostcard")) {
			this.deletePostcard(req, eventPostcardId);
			eventPostcardId = "";
			nextPage = ""; //back to the main list
		
		} else if (reqType.equals("cancelPostcard")) {
			this.cancelPostcard(req, eventPostcardId);
			eventPostcardId = "";
			nextPage = ""; //back to the main list
			
		} else if (reqType.equals("cancelEvent")) {
			this.cancelEvent(req, eventEntryId);
			nextPage = "review";

		} else if (reqType.startsWith("coopAds")) {
			SMTActionInterface ac = new CoopAdsAction(this.actionInit);
			ac.setDBConnection(dbConn);
			ac.setAttributes(this.attributes);
			ac.build(req);
			
		} else if (reqType.startsWith("radioAds")) {
			SMTActionInterface ac = new CoopAdsAction(this.actionInit);
			ac.setDBConnection(dbConn);
			ac.setAttributes(this.attributes);
			ac.build(req);
			
		} else if (reqType.equals("pre-authSubmit") && !eventPostcardId.equalsIgnoreCase("ADD")) {
			//only updates certain fields on the postcard record
			this.updatePreAuth(req, site, user, eventPostcardId);
			
		} else if (reqType.equals("pre-authSubmit")) {
			//saves a new postcard if not editing an existing one.
			eventPostcardId = this.saveEventPostcard(req, site, user);
		}
		
		//set redirect page
		log.debug("nextPage=" + nextPage);
		if (nextPage.equals("review")) eventEntryId = "";
		else if (nextPage.equals("event")) eventEntryId = "&eventNo=" + (eventNo+1);
		else if (nextPage.equals("leads") && Convert.formatInteger(req.getParameter("wizard")) == 1) eventEntryId = eventEntryId + "&wizard=" + totalEvents;
		else if (nextPage.equals("coopAdsForm")) eventEntryId = "&coopAdId=" + req.getAttribute("coopAdId") + "&ref=intro";
		else if (nextPage.equals("coopAdsFormUpd")) {
			nextPage = "coopAdsForm";
			eventEntryId = "&coopAdId=" + req.getAttribute("coopAdId");
		}
		
		StringBuilder redirectPg = new StringBuilder();
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		redirectPg.append(page.getRequestURI()).append("?facadeType=postcard&reqType=");
		redirectPg.append(nextPage).append("&eventEntryId=");
		redirectPg.append(StringUtil.checkVal(eventEntryId));
		redirectPg.append("&eventPostcardId=").append(StringUtil.checkVal(eventPostcardId));
		if (req.hasParameter("eventTypeId")) redirectPg.append("&eventTypeId=").append(req.getParameter("eventTypeId"));
		redirectPg.append("&lang=").append(language);
		redirectPg.append("&product=").append(product);
		redirectPg.append("&msg=").append(message);
		log.debug("redirUrl=" + redirectPg);
		
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, redirectPg.toString());
	}

	
	private String saveEventPostcard(ActionRequest req, SiteVO site, UserDataVO user) 
		throws ActionException {
		String eventPostcardId = StringUtil.checkVal(req.getParameter("eventPostcardId"));
		if (eventPostcardId.equalsIgnoreCase("ADD")) eventPostcardId = "";
		StringBuilder sql = new StringBuilder();
		boolean isInsert = false;
		
		if (eventPostcardId.length() > 0) {  //updates come from the event/postcard form
			sql.append("update event_postcard set organization_id=?, ");
			sql.append("update_dt=?, content_no=?, logo_img=?, logo_consent_txt=? ");
			sql.append("where event_postcard_id=?");
			
		} else { //inserts come from the event pre-Auth form
            eventPostcardId = new UUIDGenerator().getUUID();
            sql.append("insert into event_postcard (organization_id, profile_id, ");
            sql.append("AUTHORIZATION_TXT, PRESENTER_BIO_TXT, VENUE_TXT, ATTRIB_1_TXT, ATTRIB_2_TXT, ");
            sql.append("ATTRIB_3_TXT, ATTRIB_4_TXT, PRESENTER_OPT_IN_FLG, ");
            sql.append("presenter_experience_txt, presenter_email_txt, presenter_address_txt, ");
            sql.append("create_dt, content_no, logo_img, logo_consent_txt, ");
            sql.append("event_postcard_id) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            isInsert = true;
		}
		
		log.info("savePostcardSQL: " + sql);
		PreparedStatement ps = null;
		try {
			int i = 0;
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(++i, site.getOrganizationId());
			if (isInsert) {
				StringBuilder venueDesc = new StringBuilder();
				String[] venues = req.getParameterValues("venueDesc");
				if (venues != null) {
					for (int x=0; x < venues.length; x++) {
						if (x > 0) venueDesc.append(", ");
						if (venues[x].equals("other")) {
							venueDesc.append(req.getParameter("venueDescOther"));
						} else { 
							venueDesc.append(venues[x]);
						}
					}
				}
				venueDesc.append(".\n").append(req.getParameter("venueDescAffirmation"));
				
				ps.setString(++i, user.getProfileId());
				ps.setString(++i, this.saveAuthFile(req, "authFileUrl"));
				ps.setString(++i, this.saveAuthFile(req, "bioFileUrl"));
				ps.setString(++i, venueDesc.toString());
				ps.setString(++i, req.getParameter("tgmEmail"));
				ps.setString(++i, req.getParameter("repEmail") + "@" + req.getParameter("repEmailSuffix"));
				ps.setString(++i, req.getParameter("guidelinesOptIn"));
				ps.setString(++i, this.saveAuthFile(req, "cvFileUrl"));
				ps.setInt(++i, Convert.formatInteger(req.getParameter("presenterOptIn")));
				ps.setString(++i, req.getParameter("presenterExperience"));
				ps.setString(++i, req.getParameter("presenterEmail"));
				ps.setString(++i, req.getParameter("presenterAddress"));
			}
			ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			ps.setInt(++i, Convert.formatInteger(req.getParameter("postcardTypeNo")));
			ps.setString(++i, this.saveLogoImage(req));
			ps.setString(++i, req.getParameter("logoConsentText"));
			ps.setString(++i, eventPostcardId);
			
			if (ps.executeUpdate() < 1) {
                log.error("Error saving event postcard: " + ps.toString());
            }
		} catch (SQLException sqle) {
			message = "Transaction Failed";
			throw new ActionException("Error Writing EventPostcardInsert: " + sqle.getMessage());
		} finally {
        	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
		}
		
		return eventPostcardId;
	}

	private void saveEventAddtlPostcards(ActionRequest req, String eventEntryId) 
	throws ActionException {
		UUIDGenerator uuid = new UUIDGenerator();
		PreparedStatement ps = null;
		StringBuilder sql = new StringBuilder();
		sql.append("delete from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("depuy_event_addtl_postcard where event_entry_id=?");
		log.info("EventAddtlPostcardDeleteSQL: " + sql);
		
		try {
			int i = 0;
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(++i, eventEntryId);
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
	    	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
	    	}
		}
		
		sql = new StringBuilder();
		sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("depuy_event_addtl_postcard (event_entry_id, ");
		sql.append("surgeon_nm, postcard_qnty_no, create_dt, ");
		sql.append("event_addtl_postcard_id) values (?,?,?,?,?)");
		log.info("EventAddtlPostcardsInsertSQL: " + sql);
		try {
			dbConn.setAutoCommit(false);
			ps = dbConn.prepareStatement(sql.toString());
		} catch (Exception e) {}
		
		for (int x=1; StringUtil.checkVal(req.getParameter("AddtlQnty_" + x)).length() > 0; x++) {
			try {
				ps.setString(1, eventEntryId);
				ps.setString(2, req.getParameter("AddtlName_" + x));
				ps.setInt(3, Convert.formatInteger(req.getParameter("AddtlQnty_" + x)));
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				ps.setString(5, uuid.getUUID());
				ps.addBatch();
				log.debug("AddtlPostcardBatch added " + x);
				
			} catch (Exception de) {
				log.error("failed generation of event_addtl_postcard batch", de);
			}
		}
		
		try {
			ps.executeBatch();
			dbConn.commit();
		} catch (Exception e) {
			log.error("eventAddtlPostcardsInsert batch execution", e);
			message = "Transaction Failed";
			try { 
				dbConn.rollback();
			} catch (SQLException sqle) {
				log.error("could not restore autoCommit", sqle);
			}
		} finally {
			if (ps != null) {
	            try {
	                ps.close();
	            } catch(Exception e) {}
			}
			try {
				dbConn.setAutoCommit(true);
			} catch (SQLException sqle2) {
				log.error("could not restore autoCommit", sqle2);
			}
        }
		
		return;
	}

	private String saveEvent(ActionRequest req) throws ActionException {
		String eventEntryId = "";
		message = "You have successfully entered your Event";
		
		log.debug("starting call to DePuyEventEntryAction");
		DePuyEventEntryAction dee = new DePuyEventEntryAction(this.actionInit);
		dee.setAttributes(this.attributes);
		dee.setDBConnection(dbConn);
		try {
			dee.update(req);
			eventEntryId = (String) req.getAttribute("eventEntryId");
		} catch (ActionException ae) {
			message = "Transaction Failed";
			log.error("error saving event", ae);
			throw ae;
		}
		
		return eventEntryId;
	}

	private void saveEventPostcardAssoc(String eventPostcardId, String eventEntryId, Integer orderNo) 
	throws ActionException {
		PreparedStatement ps = null;
		StringBuilder sql = new StringBuilder();
		sql.append("delete from event_postcard_assoc ");
		sql.append("where event_postcard_id=? and event_entry_id=?");
		log.debug("delPostcardAssocSQL: " + sql);
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, eventPostcardId);
			ps.setString(2, eventEntryId);
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			log.error(sqle);
			message = "Transaction Failed";
		} finally {
	    	try { ps.close(); } catch(Exception e) {}
		}
		
		sql = new StringBuilder();
		sql.append("insert into event_postcard_assoc (event_postcard_id, ");
		sql.append("event_entry_id, create_dt, order_no, event_postcard_assoc_id) ");
		sql.append("values (?,?,?,?,?)");
		log.info("insertPostcardAssocSQL: " + sql + "postcardId=" + 
				eventPostcardId + " eventId=" + eventEntryId);
		
		ps = null;
		UUIDGenerator uuid = new UUIDGenerator();
        try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, eventPostcardId);
			ps.setString(2, eventEntryId);
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.setInt(4, orderNo);
			ps.setString(5, uuid.getUUID());
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			log.error("Error saving eventPostcardAssoc", sqle);
			message = "Transaction Failed";
		} finally {
	    	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
	    	}
		}
		
		return;
	}
	
	/**
	 * delete old leads-table entries and re-insert the new values.
	 * Call LeadsDataTool to get an count on the of leads for each zip (city counts come in on the req) 
	 * @param req
	 * @throws ActionException
	 */
	private void saveLeads(ActionRequest req, String eventPostcardId, UserDataVO user, Integer roleId) 
	throws ActionException {
		message = "Leads Saved Successfully";
		PreparedStatement ps = null;
		String stateCd = StringUtil.checkVal(req.getParameter("state_cd"));
		
		//delete the old records to avoid the hassles of an update query
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
	    	try { ps.close(); } catch(Exception e) {}
		}
		
		//loop the cities on the request and insert each record
		sql = new StringBuilder();
		sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
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
			for (int x=0; cities != null && x<cities.length; x++) {
				String val = StringUtil.checkVal(cities[x]);
				String cityNm = val.substring(0, val.lastIndexOf("|"));
				int leadCnt = Convert.formatInteger(val.substring(val.lastIndexOf("|")+1));
				
				//log.debug("city=" + cityNm + " size=" + leadCnt);
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
					
					if (psCnt == Constants.MAX_SQL_BATCH_SIZE) { //occasionally commit the transaction
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
	        	} catch(Exception e) {}
        	}
		}
		
		
		//setup zipCodes SQL for insert
		String zipString = StringUtil.replace(req.getParameter("zipcodes"),"\r\n",",");
		StringTokenizer zips = new StringTokenizer(zipString, ",");
		LeadsDataTool edt = new LeadsDataTool(actionInit);
		edt.setAttributes(this.attributes);
		edt.setDBConnection(dbConn);
		while (zips.hasMoreTokens()) {
			String zip = StringUtil.checkVal(zips.nextToken()).trim();
			log.debug("found zip " + zip);
			if (zip.length() == 0 || zip.length() > 10) continue;  //skip empty or malformed
			
			Integer leadCnt = edt.countLeadsByZip(zip, req.getParameter("groupCd"), stateCd, user, roleId);
			try {
				//this stmt uses the same sql as the cities (above)
				ps = dbConn.prepareStatement(sql.toString());
				ps.setString(1, new UUIDGenerator().getUUID());
				ps.setString(2, eventPostcardId);
				ps.setString(3, stateCd);
				ps.setString(4, null);
				ps.setString(5, zip);
				ps.setInt(6, leadCnt);
				ps.setTimestamp(7, Convert.getCurrentTimestamp());
				ps.executeUpdate();
				//NOTE:  We do not batch these statements because rarely are 
				// there more than a handful of zips (if any)
			} catch (SQLException sqle) {
				log.error("could not save lead zips", sqle);
				message = "Could not save lead Zips";
			} finally {
	        	try {
					dbConn.commit();
	        		ps.close();
	        	} catch(Exception e) {}
			}
			
		}

		edt = null;
		return;
	}
	
	/**
	 * called when the event-owner submits their postcard/events for approval
	 * @param req
	 * @param eventPostcardId
	 * @throws ActionException
	 */
	private void submitPostcard(ActionRequest req, String eventPostcardId) throws ActionException {
		PreparedStatement ps = null;
		StringBuilder sql = new StringBuilder();
		sql.append("update event_postcard set status_flg=1, update_dt=? where event_postcard_id=?");
		log.debug("EventPostcardSubmitSQL: " + sql);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setTimestamp(1, Convert.getCurrentTimestamp());
			ps.setString(2, eventPostcardId);
			ps.executeUpdate();
		} catch (Exception de) {
			log.error("failed submitting new eventPostcard for approval", de);
			message = "Transaction Failed";
			throw new ActionException(de);
		} finally {
			try { ps.close(); } catch(Exception e) {}
        }
		
		//get the postcard data for emailing & approving each event
		req.setParameter("reqType", "report");
		req.setParameter("rptType", new Integer(LeadsDataTool.POSTCARD_SUMMARY_PULL).toString());
		DePuyEventPostcardVO postcard = new DePuyEventPostcardVO();
		PostcardSelect epsa = new PostcardSelect(actionInit);
		epsa.setDBConnection(dbConn);
		epsa.setActionInit(actionInit);
		epsa.setAttributes(this.attributes);
		try {
			epsa.retrieve(req);
			List<?> cards = (List<?>) req.getAttribute(PostcardSelect.RETR_EVENTS);
			if (cards != null && !cards.isEmpty()) 
				postcard = (DePuyEventPostcardVO) cards.get(0);
			req.setAttribute("postcard", postcard);
			req.removeAttribute(PostcardSelect.RETR_EVENTS);
		} catch (ActionException ae) {
			log.error("retrievingPostcardInfoForEmails", ae);
		}
		
		//send approval request email
		AbstractPostcardEmailer epe = AbstractPostcardEmailer.newInstance(postcard.getGroupCd(), attributes, dbConn);
		epe.sendApprovalRequest(req);

		//strip the report(s) of the request and forward back to JSP for page display
		req.removeAttribute(Constants.BINARY_DOCUMENT_REDIR);
		req.removeAttribute(Constants.BINARY_DOCUMENT);
		message = "Postcard Submitted Successfully";
		return;
	}
	
	/**
	 * called when the admin approves a postcard/events
	 * @param req
	 * @param eventPostcardId
	 * @throws ActionException
	 */
	private void approvePostcard(ActionRequest req, String eventPostcardId) throws ActionException {
		PreparedStatement ps = null;
		StringBuilder sql = new StringBuilder();
		sql.append("update event_postcard set status_flg=2, update_dt=? where event_postcard_id=?");
		log.debug("EventPostcardSubmitSQL: " + sql);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setTimestamp(1, Convert.getCurrentTimestamp());
			ps.setString(2, eventPostcardId);
			ps.executeUpdate();
		} catch (Exception de) {
			log.error("failed approving new eventPostcard", de);
			message = "Transaction Failed";
			throw new ActionException(de);
		} finally {
			try { ps.close(); } catch(Exception e) {}
        }
		
		//activate the events
		sql = new StringBuilder();
		sql.append("update event_entry set status_flg=?, update_dt=? where event_entry_id in (");
		sql.append("select event_entry_id from event_postcard_assoc where event_postcard_id=?)");
		log.debug("EventEntryApproveSQL: " + sql);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, EventFacadeAction.STATUS_APPROVED);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, eventPostcardId);
			ps.executeUpdate();
		} catch (Exception de) {
			log.error("failed approving new events", de);
			message = "Transaction Failed";
			throw new ActionException(de);
		} finally {
			try { ps.close(); } catch(Exception e) {}
        }
		
		//get the postcard data for emailing & approving each event
		req.setParameter("reqType", "report");
		req.setParameter("rptType", new Integer(LeadsDataTool.POSTCARD_SUMMARY_PULL).toString());
		DePuyEventPostcardVO postcard = new DePuyEventPostcardVO();
		PostcardSelect epsa = new PostcardSelect(actionInit);
		epsa.setDBConnection(dbConn);
		epsa.setAttributes(attributes);
		try {
			epsa.retrieve(req);
			List<?> cards = (List<?>) req.getAttribute(PostcardSelect.RETR_EVENTS);
			if (cards != null && !cards.isEmpty()) 
				postcard = (DePuyEventPostcardVO) cards.get(0);
			req.setAttribute("postcard", postcard);
			req.removeAttribute(PostcardSelect.RETR_EVENTS);
		} catch (ActionException ae) {
			log.error("retrievingPostcardInfoForEmails", ae);
		}
		log.debug("pc = " + postcard);
		
		//send notification emails w/postcard data
		AbstractPostcardEmailer epe = AbstractPostcardEmailer.newInstance(postcard.getGroupCd(), attributes, dbConn);
		
		//send owner "event approved" response email
		log.debug("starting rep notification email");
		epe.sendApprovedResponse(req);

    	// email vendor, also sends to DePuy proj managers (CC: on email)
		log.debug("starting vendor notification email");
		epe.sendVendorSummary(req);
		log.debug("completed vendor notification email");
		
		// email territory admin and rep the pre-auth paperwork
		log.debug("starting pre-auth documents email");
		epe.sendPreAuthPaperwork(req);
		log.debug("completed pre-auth documents email");
		
		//strip the report(s) of the request and forward back to JSP for page display
		req.removeAttribute(Constants.BINARY_DOCUMENT_REDIR);
		req.removeAttribute(Constants.BINARY_DOCUMENT);
		message = "The Postcard was Approved";
		return;
	}
	
	private void deletePostcard(ActionRequest req, String eventPostcardId) throws ActionException {
		PreparedStatement ps = null;
		StringBuilder sql = new StringBuilder();
		sql.append("update event_postcard set status_flg=4, update_dt=? where event_postcard_id=?");
		log.debug("EventPostcardSubmitSQL: " + sql);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setTimestamp(1, Convert.getCurrentTimestamp());
			ps.setString(2, eventPostcardId);
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("failed deleting eventPostcard", sqle);
			message = "Transaction Failed";
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch(Exception e) {}
        }
		
		//terminate the events
		sql = new StringBuilder();
		sql.append("update event_entry set status_flg=?, update_dt=? where event_entry_id in (");
		sql.append("select event_entry_id from event_postcard_assoc where event_postcard_id=?)");
		log.debug("EventEntryApproveSQL: " + sql);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, EventFacadeAction.STATUS_DELETED);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, eventPostcardId);
			ps.executeUpdate();
		} catch (Exception de) {
			log.error("failed deleting events", de);
			message = "Transaction Failed";
			throw new ActionException(de);
		} finally {
			try { ps.close(); } catch(Exception e) {}
        }

		message = "The Postcard was Deleted";
		return;
	}
	
	
	private void cancelPostcard(ActionRequest req, String eventPostcardId) throws ActionException {
		PreparedStatement ps = null;
		StringBuilder sql = new StringBuilder();
		sql.append("update event_postcard set status_flg=3, update_dt=? where event_postcard_id=?");
		log.debug("EventPostcardSQL: " + sql);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setTimestamp(1, Convert.getCurrentTimestamp());
			ps.setString(2, eventPostcardId);
			ps.executeUpdate();
		} catch (Exception de) {
			log.error("failed cancelling eventPostcard", de);
			message = "Transaction Failed";
			throw new ActionException(de);
		} finally {
			try { ps.close(); } catch(Exception e) {}
        }
		
		//terminate the events
		sql = new StringBuilder();
		sql.append("update event_entry set status_flg=?, update_dt=? where event_entry_id in (");
		sql.append("select event_entry_id from event_postcard_assoc where event_postcard_id=?)");
				
		log.debug("EventEntryApproveSQL: " + sql);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, EventFacadeAction.STATUS_CANCELLED);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, eventPostcardId);
			ps.executeUpdate();
		} catch (Exception de) {
			log.error("failed cancelling events", de);
			message = "Transaction Failed";
			throw new ActionException(de);
		} finally {
			try { ps.close(); } catch(Exception e) {}
        }
		
		//get the postcard data for emailing & approving each event
		req.setParameter("reqType", "report");
		req.setParameter("rptType", Integer.valueOf(LeadsDataTool.POSTCARD_SUMMARY_PULL).toString());
		DePuyEventPostcardVO postcard = new DePuyEventPostcardVO();
		PostcardSelect epsa = new PostcardSelect(actionInit);
		epsa.setDBConnection(dbConn);
		epsa.setAttributes(attributes);
		try {
			epsa.retrieve(req);
			List<?> cards = (List<?>) req.getAttribute(PostcardSelect.RETR_EVENTS);
			if (cards != null && !cards.isEmpty()) 
				postcard = (DePuyEventPostcardVO) cards.get(0);
			req.setAttribute("postcard", postcard);
			req.removeAttribute(PostcardSelect.RETR_EVENTS);
		} catch (ActionException ae) {
			log.error("retrievingPostcardInfoForEmails", ae);
		}
		log.debug("pc = " + postcard);
		
		//send notification email to the admins
		log.debug("starting cancellation email");
		AbstractPostcardEmailer epe = AbstractPostcardEmailer.newInstance(postcard.getGroupCd(), attributes, dbConn);
		epe.sendPostcardCancellation(req);

		message = "The Postcard was Cancelled";
		return;
	}
	
	private void cancelEvent(ActionRequest req, String eventEntryId) throws ActionException {
		PreparedStatement ps = null;
		StringBuilder sql = new StringBuilder();
		sql.append("update event_entry set status_flg=?, update_dt=? where event_entry_id=?");
		log.debug("EventEntryCancelSQL: " + sql);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, EventFacadeAction.STATUS_CANCELLED);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, eventEntryId);
			ps.executeUpdate();
		} catch (Exception de) {
			log.error("failed cancelling event", de);
			message = "Transaction Failed";
			throw new ActionException(de);
		} finally {
			try { ps.close(); } catch(Exception e) {}
        }

		message = "The Postcard was Cancelled";
		return;
	}
	
	private String saveLogoImage(ActionRequest req) {
		log.debug("starting saveLogoImage");
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);

		StringBuilder imagePath =  new StringBuilder((String)getAttribute("pathToBinary"));
    	imagePath.append(getAttribute("orgAlias")).append(site.getOrganizationId());
    	imagePath.append("/").append(site.getSiteId()).append("/logos/");

    	FileLoader fl = null;
    	FilePartDataBean fpdb = req.getFile("logoImage");
    	String fileNm = StringUtil.checkVal(req.getParameter("oldLogoImage"));
		String newFile = (fpdb != null) ? StringUtil.checkVal(fpdb.getFileName()) : "";
		Boolean isNewLogo = (newFile.length() > 0);
		log.debug("newFile=" + newFile + " oldFile=" + fileNm);
		
    	
    	//delete old file
    	if (fileNm.length() > 0 && (isNewLogo || StringUtil.checkVal(req.getParameter("useLogo")).equals("0"))) {
    		 try {
    			 fl = new FileLoader(attributes);
    			 fl.setPath(imagePath.toString());
    			 fl.setFileName(fileNm);
	    	     fl.deleteFile();
	    	     fl = null;
    		 } catch (Exception e) { 
    			 log.error("Error Deleting Logo Image", e);
    		 }
    		 log.debug("finished deletion of " + imagePath.toString() + fileNm);
    	}
    	
    	// Write new file
    	if (newFile.length() > 0) {
    		try {
	    		fl = new FileLoader(attributes);
	        	fl.setFileName(fpdb.getFileName());
	        	fl.setPath(imagePath.toString());
	        	fl.setRename(true);
	    		fl.setOverWrite(false);
	        	fl.setData(fpdb.getFileData());
	        	fileNm = fl.writeFiles();
	    	} catch (Exception e) {
	    		log.error("Error Writing Logo Image", e);
	    	}
	    	log.debug("finished write");
    	}
    	
    	fpdb = null;
    	fl = null;
		return fileNm;
	}

	
	private String saveAuthFile(ActionRequest req, String paramName) {
		log.debug("starting saveAuthFile");
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);

		StringBuilder imagePath =  new StringBuilder((String)getAttribute("pathToBinary"));
    	imagePath.append(getAttribute("orgAlias")).append(site.getOrganizationId());
    	imagePath.append("/").append(site.getSiteId()).append("/auth/");

    	FileLoader fl = null;
    	FilePartDataBean fpdb = req.getFile(paramName);
    	String fileNm = (fpdb != null) ? StringUtil.checkVal(fpdb.getFileName()) : null;
		
    	// Write new file
    	if (fileNm != null && fileNm.length() > 0) {
    		try {
	    		fl = new FileLoader(attributes);
	        	fl.setFileName(fpdb.getFileName());
	        	fl.setPath(imagePath.toString());
	        	fl.setRename(true);
	    		fl.setOverWrite(false);
	        	fl.setData(fpdb.getFileData());
	        	fileNm = fl.writeFiles();
	    	} catch (Exception e) {
	    		log.error("Error Writing Auth File", e);
	    	}
	    	log.debug("finished write");
	    	
    	} else if (req.getParameter("old_" + paramName) != null) {
    		fileNm = req.getParameter("old_" + paramName);
    	}
    	
    	fpdb = null;
    	fl = null;
		return fileNm;
	}
	
	
	private void updatePreAuth(ActionRequest req, SiteVO site, UserDataVO user, String eventPostcardId) 
			throws ActionException {
		StringBuilder venueDesc = new StringBuilder();
		String[] venues = req.getParameterValues("venueDesc");
		if (venues != null) {
			for (int x=0; x < venues.length; x++) {
				if (x > 0) venueDesc.append(", ");
				if (venues[x].equals("other")) {
					venueDesc.append("Other: ").append(req.getParameter("venueDescOther"));
				} else { 
					venueDesc.append(venues[x]);
				}
			}
		}
		venueDesc.append(".\n").append(StringUtil.checkVal(req.getParameter("venueDescAffirmation")));
		
		String attrib2Text = null;
		if (req.hasParameter("repEmail")) 
				attrib2Text = req.getParameter("repEmail") + "@" + req.getParameter("repEmailSuffix");
		
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE EVENT_POSTCARD SET AUTHORIZATION_TXT=?, PRESENTER_BIO_TXT=?, ");
		sql.append("VENUE_TXT=?, ATTRIB_1_TXT=?, ATTRIB_2_TXT=?, ");
		sql.append("ATTRIB_3_TXT=?, ATTRIB_4_TXT=?, PRESENTER_OPT_IN_FLG=?, ");
		sql.append("PRESENTER_EXPERIENCE_TXT=?, PRESENTER_EMAIL_TXT=?, PRESENTER_ADDRESS_TXT=?, ");
		sql.append("UPDATE_DT=? WHERE EVENT_POSTCARD_ID=?");
		log.info("updatePreAuthSQL: " + sql);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, this.saveAuthFile(req, "authFileUrl"));
			ps.setString(2, this.saveAuthFile(req, "bioFileUrl"));
			ps.setString(3, venueDesc.toString());
			ps.setString(4, req.getParameter("tgmEmail"));
			ps.setString(5, attrib2Text);
			ps.setString(6, req.getParameter("guidelinesOptIn"));
			ps.setString(7, this.saveAuthFile(req, "cvFileUrl"));
			ps.setInt(8, Convert.formatInteger(req.getParameter("presenterOptIn")));
			ps.setString(9, req.getParameter("presenterExperience"));
			ps.setString(10, req.getParameter("presenterEmail"));
			ps.setString(11, req.getParameter("presenterAddress"));
			ps.setTimestamp(12, Convert.getCurrentTimestamp());
			ps.setString(13, eventPostcardId);
			
			if (ps.executeUpdate() < 1) {
	            log.error("Error saving event pre-Auth: " + ps.toString());
	        }
		} catch (SQLException sqle) {
			message = "Transaction Failed";
			throw new ActionException("Error Updating Event pre-Auth: " + sqle.getMessage());
		} finally {
	    	try { ps.close(); } catch(Exception e) {}
		}
		
		return;
	}
}