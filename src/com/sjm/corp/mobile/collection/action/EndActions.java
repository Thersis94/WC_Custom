package com.sjm.corp.mobile.collection.action;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.SMBFileManager;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.io.mail.SMTMailHandler;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;
import com.sjm.corp.mobile.collection.MobileCollectionVO;

/****************************************************************************
 * <b>Title</b>: EndActions.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Hnadles end actions for the SJM Mobile Collection App
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since Jul 26, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class EndActions extends CollectionAbstractAction {

	private String practiceTable; //These members store the custom location of the various tables that we handle.
	private String marketingTable;
	private String goalsTable;
	private String templateTable;
	private String patientsTable;
	
	/*
	 * (non-Javadoc)
	 * @see com.sjm.corp.mobile.collection.action.CollectionAbstractAction#update(com.siliconmtn.http.SMTServletRequest, com.sjm.corp.mobile.collection.MobileCollectionVO)
	 */
	public void update(SMTServletRequest req, MobileCollectionVO vo) {
		practiceTable =  req.getAttribute("custom") + "sjm_mobile_practice";
		marketingTable =  req.getAttribute("custom") + "sjm_mobile_marketing";
		goalsTable =  req.getAttribute("custom") + "sjm_mobile_goals";
		templateTable =  req.getAttribute("custom") + "sjm_mobile_template_practice";	
		patientsTable =  req.getAttribute("custom") + "sjm_mobile_patients";
		dbConn = (SMTDBConnection) req.getAttribute("dbConn");
		
		
		updatePractice(req,vo);
		if(!vo.isEmailSent() && (req.getParameter("error") == null || req.getParameter("error").isEmpty())){
			try{
				sendEmail(req, vo);
			} catch(Exception e){
				log.error("Error sending email out: ", e);
			}
			dbUpdate(req, vo);
			super.blockBack(req);
			super.blockNext(req);
		}
	}
	
	/**
	 * Update Practice object
	 * @param req
	 * @param vo
	 */
	public void updatePractice(SMTServletRequest req, MobileCollectionVO vo){
		vo.getPractice().setAltContactEmail(req.getParameter("altContactEmail"));
		vo.getPractice().setAltContactName(req.getParameter("altContactName"));
		vo.getPractice().setAltContactPhone(req.getParameter("altContactPhone"));
		vo.getPractice().setAltContactTitle(req.getParameter("altContactTitle"));
		vo.getPractice().setComment(req.getParameter("comment"));
		vo.getPractice().setPrimaryContactEmail(req.getParameter("primaryContactEmail"));
		vo.getPractice().setPrimaryContactName(req.getParameter("primaryContactName"));
		vo.getPractice().setPrimaryContactPhone(req.getParameter("primaryContactPhone"));
		vo.getPractice().setPrimaryContactTitle(req.getParameter("primaryContactTitle"));
		vo.getPractice().setReadyToMove(Convert.formatBoolean(req.getParameter("readyToMove")));
		vo.getPractice().setWantingConferenceCall(Convert.formatBoolean(req.getParameter("conferenceCall")));
		vo.getPractice().setWantingVisit(Convert.formatBoolean(req.getParameter("visit")));
		vo.getPractice().setWantingReviewTime(Convert.formatBoolean(req.getParameter("reviewTime")));
		if( req.getParameter("primaryContactEmail") == null || req.getParameter("primaryContactEmail").isEmpty()){ //Uses short circuiting here to avoid a null pointer exception
			req.setParameter("pageNumber", "10");
			req.setParameter("error", "Please input a valid primary email address");
		}
	}
	
	/**
	 * Sends email to administrator, as well as to the doctor
	 * @param req
	 * @param vo
	 */
	public void sendEmail(SMTServletRequest req, MobileCollectionVO vo){
		vo.setEmailSent(true);
		emailAdmin(req,vo);
		emailDoc(req, vo);
	}
	
	/**
	 * Emails the doctor/practitioner using the email entered into the 'next steps' stage
	 * @param req
	 * @param vo
	 */
	public void emailDoc(SMTServletRequest req, MobileCollectionVO vo){
		EmailMessageVO messageVO = new EmailMessageVO();
		messageVO.setSubject("Thank you for taking our survey");
		try {
		    messageVO.addRecipient(vo.getPractice().getPrimaryContactEmail());
		    messageVO.setReplyTo(vo.getPractice().getAdminEmail());
		    messageVO.setFrom("no-reply");
		} catch (InvalidDataException ide) {
			log.error("Error setting 'reply-to' and/or 'from' and/or 'recipient' values to email message, ", ide);
			return;
		}
				
		messageVO.setTextBody("Thank you for your participation in our survey. Attached is a PDF of the templates that you choose.");
		//Find the PDF files for the templates that they chose, add them as attachments
		Map<String, byte[]> templatePDFs = new HashMap<String, byte[]>();
		SMBFileManager fm = new SMBFileManager();
		File choice1 = new File(vo.getThemes().getThemePdf().get(vo.getThemes().getThemeId().indexOf(vo.getTemplates().getThemes().get(0))));
		File choice2 = new File(vo.getThemes().getThemePdf().get(vo.getThemes().getThemeId().indexOf(vo.getTemplates().getThemes().get(1))));
		File choice3 = new File(vo.getThemes().getThemePdf().get(vo.getThemes().getThemeId().indexOf(vo.getTemplates().getThemes().get(2))));
		try {
			templatePDFs.put("Choice 1 - " + choice1.getName(), fm.retrieveFile((String)req.getAttribute("pathToBinary") + choice1.getPath()));
			templatePDFs.put("Choice 2 - " + choice2.getName(), fm.retrieveFile((String)req.getAttribute("pathToBinary") + choice2.getPath()));
			templatePDFs.put("Choice 3 - " + choice3.getName(), fm.retrieveFile((String)req.getAttribute("pathToBinary") + choice3.getPath()));
		} catch (Exception e) {
			log.error("Failed to correctly place the PDF's in the map, ",e);
		}
		for(String s: templatePDFs.keySet()){
			messageVO.addAttachment(s, templatePDFs.get(s));
		}
		
		Map<Object, Object> params = new HashMap<Object, Object>();		
	    params.put(GlobalConfig.KEY_SMTP_SERVER, "smtp.sendgrid.net");
	    params.put(GlobalConfig.KEY_SMTP_PORT, 25);
	    params.put(GlobalConfig.KEY_SMTP_USER, "smtemail");
	    params.put(GlobalConfig.KEY_SMTP_PASSWORD, "smtrul3s");
	    try{
	    	SMTMailHandler smh = new SMTMailHandler(params);
	    	smh.sendMessage(messageVO); //send the actual email
	    } catch(Exception e) {
	    	log.error("Error sending an email to the doctor: ",e);
	    }
	}
	 
	/**
	 * Emails the administrator, SJM inputs the email to send this to upon creation of the portlet
	 * @param req
	 * @param vo
	 */
	public void emailAdmin(SMTServletRequest req, MobileCollectionVO vo){
		EmailMessageVO messageVO = new EmailMessageVO();
		messageVO.setSubject("New Order");
		try {
			messageVO.setReplyTo("no-reply@siliconmtn.com");
			messageVO.setFrom("no-reply");	 
		    messageVO.addRecipient(vo.getPractice().getAdminEmail());
		} catch (InvalidDataException ide) {
			log.error("Error setting 'reply-to' and/or 'from' and/or 'recipient' values to email message, ", ide);
			return;
		}
		StringBuffer message = new StringBuffer();
		message.append("Hello. There was a new order from " + vo.getPractice().getName() + " for the following services: ");
		//Figure out what items they want for the specific instance of the MarketingWantsVO object
		Field[] items = vo.getMarketing().getWants().getClass().getDeclaredFields(); // getDeclaredFields returns all of the member's of a class
		try{
			StringBuffer itemList = new StringBuffer(); // we are using a seperate StringBuffer here, so that we can easily detect if no items were ordered
			int i = 0;
			for(Field s: items){
				s.setAccessible(true); 
				if(s.get(vo.getMarketing().getWants()).equals(true)){ 
					if(i == 0)
						itemList.append(vo.getMarketing().getWants().getNames().get(s.getName())); 
					else
						itemList.append(", " + vo.getMarketing().getWants().getNames().get(s.getName())); 
					i++;
				}
			}
			if(itemList.length() == 0){ // if no items were ordered
				itemList.append("No items ordered");
			}
			message.append(itemList);
		} catch (Exception e) {
			message.append("No items Ordered");
			log.error("Error adding in the items ordered for the administrator email: ",e);
		}
		
		message.append(".");
		messageVO.setTextBody(message.toString());
		 
		//send the email
		Map<Object, Object> params = new HashMap<Object, Object>();
	    params.put(GlobalConfig.KEY_SMTP_SERVER, "smtp.sendgrid.net");
	    params.put(GlobalConfig.KEY_SMTP_PORT, 25);
	    params.put(GlobalConfig.KEY_SMTP_USER, "smtemail");
	    params.put(GlobalConfig.KEY_SMTP_PASSWORD, "smtrul3s");
	    
	    try{
	    	SMTMailHandler smh = new SMTMailHandler(params);
	    	smh.sendMessage(messageVO);
	    } catch(Exception e) {
	    	log.error("Error sending the email to the Administrator: ", e);
	    }
	}
	
	/**
	 * updates all the tables with the data collected
	 * @param req
	 * @param vo
	 */
	public void dbUpdate(SMTServletRequest req, MobileCollectionVO vo){
		try{
			dbConn.setAutoCommit(false);
			updateTablePractice(req, vo);
			updateTableTemplatePractice(req, vo);
			updateTableMarketing(req, vo);
			updateTablePatients(req, vo);
			updateTableGoals(req, vo);
			dbConn.commit();
			dbConn.setAutoCommit(true);
		} catch(Exception e) {
			log.error("Error trying to update the db, attempting to roll back", e);
	        if (dbConn != null) {
	            try {
	            	log.error("system is being rolled back");
	                dbConn.rollback();
	            } catch(Exception ex) {
	            	
	            }
	        }
		}
	}
	
	/**
	 * Update Practice table
	 * @param req
	 * @param vo
	 */
	public void updateTablePractice(SMTServletRequest req, MobileCollectionVO vo){
		StringBuffer sql = new StringBuffer();
		
		String goalId = vo.getGoals().getGoalId();
		String marketingId = vo.getMarketing().getMarketingId();
		String patientsId = vo.getPatients().getPatientId();
		String templatesId = vo.getTemplates().getTemplateId();
		
		boolean isInsert = false;
		
		if(goalId == null || marketingId == null || patientsId == null || templatesId == null){
			isInsert = true; //If we don't have our candidate keys, we're inserting a new record into the system;
		}
		
		if(isInsert){
			sql.append("insert into ").append(practiceTable).append(" (primary_contact_name, ");
			sql.append("primary_contact_email,primary_contact_phone, primary_contact_title, alt_contact_name, ");
			sql.append("alt_contact_email, alt_contact_phone, alt_contact_title, ready_to_move, ");
			sql.append("wanting_conference_call, wanting_visit, comment, goal_id, marketing_id, patient_id, ");
			sql.append("template_id, date_visited, review_time, action_id, region_id, admin_email, name, location, office_name) values");
			sql.append("(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		} else {
			sql.append("update ").append(practiceTable).append(" set primary_contact_name = ?, primary_contact_email = ?, ");
			sql.append("primary_contact_phone = ?, primary_contact_title = ?, ");
			sql.append("alt_contact_name = ?, alt_contact_email = ?, alt_contact_phone =?,");
			sql.append("alt_contact_title = ?, ready_to_move = ?, wanting_conference_call = ?,");
			sql.append("wanting_visit = ?, comment = ?, goal_id = ?, marketing_id =?, ");
			sql.append("patient_id = ?, template_id =?, date_visited =  ?, review_time = ?, action_id = ?, ");
			sql.append("region_id = ?, admin_email = ? where name like ? and location like ? and office_name like ?");
		}
		log.debug("Updating Table Practice SQL: " + sql.toString());
		
		
		if(goalId == null){
			req.setAttribute("goalId",(new UUIDGenerator()).getUUID()); //We place these in the request object to make it easy to detect if we have already inserted a record or not for this entry
			goalId = (String) req.getAttribute("goalId");
			vo.getGoals().setGoalId(goalId);
		}
		if(marketingId == null){
			req.setAttribute("marketingId",(new UUIDGenerator()).getUUID());
			marketingId = (String)req.getAttribute("marketingId");
			vo.getMarketing().setMarketingId(marketingId);
		}
		if(patientsId == null){
			req.setAttribute("patientsId", (new UUIDGenerator()).getUUID());
			patientsId = (String)req.getAttribute("patientsId");
			vo.getPatients().setPatientId(patientsId);
		}
		if(templatesId == null){
			req.setAttribute("templatesId",(new UUIDGenerator()).getUUID());
			templatesId = (String)req.getAttribute("templatesId");
			vo.getTemplates().setTemplateId(templatesId);
		}
		
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, vo.getPractice().getPrimaryContactName());
			ps.setString(2, vo.getPractice().getPrimaryContactEmail());
			ps.setString(3, vo.getPractice().getPrimaryContactPhone());
			ps.setString(4, vo.getPractice().getPrimaryContactTitle());
			ps.setString(5, vo.getPractice().getAltContactName());
			ps.setString(6, vo.getPractice().getAltContactEmail());
			ps.setString(7, vo.getPractice().getAltContactPhone());
			ps.setString(8, vo.getPractice().getAltContactTitle());
			ps.setString(9, Boolean.toString(vo.getPractice().isReadyToMove()));
			ps.setString(10, Boolean.toString(vo.getPractice().isWantingConferenceCall()));
			ps.setString(11, Boolean.toString(vo.getPractice().isWantingVisit()));
			ps.setString(12, vo.getPractice().getComment());
			ps.setString(13, goalId);
			ps.setString(14, marketingId);
			ps.setString(15, patientsId);
			ps.setString(16, templatesId);
			ps.setString(17, Convert.getCurrentTimestamp().toString());
			ps.setString(18, Boolean.toString(vo.getPractice().isWantingReviewTime()));
			ps.setString(19, vo.getActionId());
			ps.setString(20, vo.getActionId());
			ps.setString(21, vo.getPractice().getAdminEmail());
			ps.setString(22, vo.getPractice().getPracticioner());
			ps.setString(23, vo.getPractice().getLocation());
			ps.setString(24, vo.getPractice().getOfficeName());
			ps.execute();
		} catch(Exception sqle) {
			sqle.printStackTrace();
		} finally {
        	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
		}
	}
	 
	/**
	 * update Patients table
	 * @param req
	 * @param vo
	 */
	public void updateTablePatients(SMTServletRequest req, MobileCollectionVO vo){
		StringBuffer sql = new StringBuffer();
		boolean isInsert = false;
		if(req.getAttribute("patientsId") != null){
			isInsert = true;
		}
		
		if(isInsert){
			sql.append("insert into ").append(patientsTable).append("(source_primary, ");
			sql.append("source_podatrist, source_chiropractor, source_pt, source_other, ");
			sql.append("source_other_desc, source_surgon, practice_id) values(?,?,?,?,?,?,?,?)");
		} else {
			sql.append("update ").append(patientsTable).append(" set source_primary = ?, ");
			sql.append("source_podatrist = ?, source_chiropractor = ?, source_pt = ?, ");
			sql.append("source_other = ?, source_other_desc = ?, source_surgon = ? where practice_id = ?");
		}
		
		log.debug("updatePatientsTable SQL: " + sql.toString());
    	
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, Integer.toString(vo.getPatients().getPrimaryCareRef()));
			ps.setString(2, Integer.toString(vo.getPatients().getPodiatristRef()));
			ps.setString(3, Integer.toString(vo.getPatients().getChiropractorRef()));
			ps.setString(4, Integer.toString(vo.getPatients().getPhysicalTherepistRef()));
			ps.setString(5, Integer.toString(vo.getPatients().getOtherRef()));
			ps.setString(6, vo.getPatients().getOtherRefName());
			ps.setString(7, Integer.toString(vo.getPatients().getOrthopedicRef()));
			ps.setString(8, vo.getPatients().getPatientId());
			ps.execute();
		} catch(Exception sqle) {
			log.error("error updating patients table", sqle);
		} finally {
        	if (ps != null) {
        		try {
        			ps.close();
        		} catch(Exception e) {}
        	}
		}
	}
	 
	/**
	 * update goal table
	 * @param req
	 * @param vo
	 */
	public void updateTableGoals(SMTServletRequest req, MobileCollectionVO vo){
		StringBuffer sql = new StringBuffer();
		boolean isInsert = false;
		if(req.getAttribute("goalId") != null){
			isInsert = true;
		}
		
		if(isInsert){
			sql.append("insert into ").append(goalsTable).append("(new_practice, ");
			sql.append("rebrand_practice, consolidate, overall_patients, interventional, hcp_patients, ");
			sql.append("goal_id) values (?,?,?,?,?,?,?)");
		} else {
			sql.append("update ").append(goalsTable).append(" set new_practice = ?, ");
			sql.append("rebrand_practice = ?, consolidate = ?, overall_patients = ?, ");
			sql.append("interventional = ?, hcp_patients = ? where goal_id = ?");
		}
		log.debug("updateGoalsTable SQL: " + sql.toString());
    	
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, Boolean.toString(vo.getGoals().isNewPractice()));
			ps.setString(2, Boolean.toString(vo.getGoals().isRebrandPractice()));
			ps.setString(3, Boolean.toString(vo.getGoals().isConsolidation()));
			ps.setString(4, Boolean.toString(vo.getGoals().isOverallPatients()));
			ps.setString(5, Boolean.toString(vo.getGoals().isInterventionalPatients()));
			ps.setString(6, Boolean.toString(vo.getGoals().isHcpPatients()));
			ps.setString(7, vo.getGoals().getGoalId());
			ps.execute();
		} catch(Exception sqle) {
			log.error("Error updating Goal Table",sqle);
		} finally {
        	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
		}
	}
	 
	/**
	 * Update marketing table
	 * @param req
	 * @param vo
	 */
	public void updateTableMarketing(SMTServletRequest req, MobileCollectionVO vo){
		updateTableMarketingUsing(req, vo);
		updateTableMarketingWants(req, vo);
	}
	 
	//Put the data for the MarketingWants object into the Marketing table
	//the exsistance of this table is guarenteed, since this method is called after the MarketingUsing method 
	public void updateTableMarketingWants(SMTServletRequest req, MobileCollectionVO vo){
		StringBuffer sql = new StringBuffer();
		sql.append("update ").append(marketingTable).append(" set WANT_APPOINTMENT_CARD = ?, ");
		sql.append("WANT_FAX_REFFERALS = ?, want_newsletters = ?, want_brochures = ?, ");
		sql.append("want_folders = ?, want_newspaper_ads = ?, want_buisiness_cards = ?, ");
		sql.append("want_letterhead = ?, want_rolodex = ?, want_postcards = ?, ");
		sql.append("want_logo = ?, want_social_media = ?, want_envelopes = ?, ");
		sql.append("want_magazine_ads = ?, want_website = ? where marketing_id = ?");
		 
		log.debug("updateMarketingWantsTable SQL: " + sql.toString());
			
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, Boolean.toString(vo.getMarketing().getWants().isWantAppointmentCards()));
			ps.setString(2, Boolean.toString(vo.getMarketing().getWants().isWantFaxReferrals()));
			ps.setString(3, Boolean.toString(vo.getMarketing().getWants().isWantNewsletters()));
			ps.setString(4, Boolean.toString(vo.getMarketing().getWants().isWantBrochures()));
			ps.setString(5, Boolean.toString(vo.getMarketing().getWants().isWantFolders()));
			ps.setString(6, Boolean.toString(vo.getMarketing().getWants().isWantNewspaperAds()));
			ps.setString(7, Boolean.toString(vo.getMarketing().getWants().isWantBusinessCards()));
			ps.setString(8, Boolean.toString(vo.getMarketing().getWants().isWantLetterhead()));
			ps.setString(9, Boolean.toString(vo.getMarketing().getWants().isWantRolodex()));
			ps.setString(10, Boolean.toString(vo.getMarketing().getWants().isWantPostcards()));
			ps.setString(11, Boolean.toString(vo.getMarketing().getWants().isWantLogo()));
			ps.setString(12, Boolean.toString(vo.getMarketing().getWants().isWantSocialMedia()));
        	ps.setString(13, Boolean.toString(vo.getMarketing().getWants().isWantEnvelopes()));
        	ps.setString(14, Boolean.toString(vo.getMarketing().getWants().isWantMagazineAds()));
        	ps.setString(15, Boolean.toString(vo.getMarketing().getWants().isWantWebsite()));
            ps.setString(16, vo.getMarketing().getMarketingId());
        	ps.execute();
		} catch(Exception sqle) {
			log.error("Error updating Markting Table - Wants", sqle);
		} finally {
        	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
		}
	}
	 
	/**
	 * write to the Marketing Table, using the data contained within the MarketingUsing object
	 * @param req
	 * @param vo
	 */
	public void updateTableMarketingUsing(SMTServletRequest req, MobileCollectionVO vo){
		StringBuffer sql = new StringBuffer();
		boolean isInsert = false;
		if(req.getAttribute("marketingId") != null){
			isInsert = true;
		}
		
		if(isInsert){
			sql.append("insert into ").append(marketingTable).append("(using_appointment_card, ");
			sql.append("using_fax_referrals, using_brochures, using_folders, using_newspaper_ads, ");
			sql.append("using_buisiness_cards, using_letterhead, using_rolodex, using_social_media, ");
			sql.append("using_magazine_ads, marketing_id) values(?,?,?,?,?,?,?,?,?,?,?)");
		} else {
			sql.append("update ").append(marketingTable).append(" set using_appointment_card = ?, ");
			sql.append("using_fax_referrals = ?, using_brochures = ?, using_folders = ?, ");
			sql.append("using_newspaper_ads = ?, using_buisiness_cards = ?, using_letterhead = ?, ");
			sql.append("using_rolodex = ?, using_social_media = ?, using_magazine_ads = ? where marketing_id = ?");
		}
		log.debug("Updating Marketing Using: " + sql.toString());
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, Boolean.toString(vo.getMarketing().getUsing().isUsingAppointmentCards()));
			ps.setString(2, Boolean.toString(vo.getMarketing().getUsing().isUsingFaxReferrals()));
			ps.setString(3, Boolean.toString(vo.getMarketing().getUsing().isUsingBrochures()));
			ps.setString(4, Boolean.toString(vo.getMarketing().getUsing().isUsingFolders()));
			ps.setString(5, Boolean.toString(vo.getMarketing().getUsing().isUsingNewspaperAds()));
			ps.setString(6, Boolean.toString(vo.getMarketing().getUsing().isUsingBusinessCards()));
			ps.setString(7, Boolean.toString(vo.getMarketing().getUsing().isUsingLetterhead()));
			ps.setString(8, Boolean.toString(vo.getMarketing().getUsing().isUsingRolodex()));
        	ps.setString(9, Boolean.toString(vo.getMarketing().getUsing().isUsingSocialMedia()));
        	ps.setString(10, Boolean.toString(vo.getMarketing().getUsing().isUsingMagazineAds()));
        	ps.setString(11, vo.getMarketing().getMarketingId());
        	ps.execute();
		} catch(Exception sqle) {
			log.error("Error updating Markting Table - using",sqle);
		} finally {
        	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
		 }
	}
	
	/**
	 * update the template table
	 * @param req
	 * @param vo
	 */
	public void updateTableTemplatePractice(SMTServletRequest req, MobileCollectionVO vo){
		StringBuffer sql = new StringBuffer();
		boolean isInsert = false;
		if(req.getAttribute("templatesId") != null){
			isInsert = true;
		}
		if(isInsert){
			sql.append("insert into ").append(templateTable).append(" (template_1, ");
			sql.append("template_2, template_3, template_id) values(?,?,?,?)"); 
		} else {
			sql.append("update ").append(templateTable).append(" set template_1 = ?, ");
			sql.append("template_2 = ?, template_3 = ? where template_id = ?");
		}
		log.debug("updateTemplateTable SQL: " + sql.toString());
		
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, vo.getTemplates().getThemes().get(0));
			ps.setString(2, vo.getTemplates().getThemes().get(1));
			ps.setString(3, vo.getTemplates().getThemes().get(2));
			ps.setString(4, vo.getTemplates().getTemplateId());
			ps.execute();
		} catch(Exception sqle){
			log.error("Error updating Template table",sqle);
		} finally {
			if (ps != null) {
				try {
		        	ps.close();
		        } catch(Exception e) {}
	        }
		 }
	}
}
