package com.fastsigns.action;

import java.sql.PreparedStatement;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.AbstractLoginModule;
import com.siliconmtn.security.SecurityModuleFactoryImpl;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.contact.SubmittalAction;
import com.smt.sitebuilder.action.dealer.DealerContactAction;
import com.smt.sitebuilder.action.dealer.DealerLocatorAction;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: RequestAQuote.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 16, 2010
 ****************************************************************************/
public class RequestAQuote extends SBActionAdapter {

	//private static final String FILES_SENT_TO_YSI = "Files Sent to YouSendIt";
	
	public RequestAQuote(ActionInitVO actionInit) {
		super(actionInit);
	}

	public RequestAQuote() {
	}

	/* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
    public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	
	/* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
    public void build(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		
		//determine which senderEmail to brand our emails with
		String senderEmail = "requestaquote@fastsigns.com";
		if (Convert.formatBoolean(req.getParameter("saf"))) senderEmail = "sendafile@fastsigns.com";
		req.setAttribute("senderEmail", senderEmail);
		

		//if this is the postback call from yousendit.com, trigger the notification emails and exit. (no redirect needed)
		if (req.hasParameter("csi")) { //csi=ContactSubmittalId
			log.error("processing postback on behalf of " + senderEmail + " for CSI=" + req.getParameter("csi"));
			this.recordStatus(req, false);
			this.logFiles(req);
			this.recordStatus(req, false);
			this.sendNotifications(req, senderEmail);
			this.recordStatus(req, false);
			return;
		}
		
		//preformat the Center and User-destined emails
		StringBuilder sb = new StringBuilder();
		sb.append("<p>This message is to advise you that a customer has filled out a ");
		sb.append("Request a Quote Form from the website and is sending a file.</p>");
		sb.append("<p>You will receive a second email notification with the file ");
		sb.append("attached after it has downloaded.  The second email will have the ");
		sb.append("same email address in the subject field and will be titled \"SAF Completed\" ");
		sb.append("and will include the customer's email address.</p>");
		sb.append("<p>Please note: A file may take a few seconds to a few hours to download.  ");
		sb.append("If you do not receive the follow up email within a few hours, please follow ");
		sb.append("up directly with your customer.</p><p>Thank you.</p>");
		req.setParameter("contactEmailSubject", "SAF Initiated File: " + req.getParameter("pfl_EMAIL_ADDRESS_TXT"));
		req.setParameter("contactEmailHeader", sb.toString());
		
		//if session.createAccount and != null password, create login account for this user!
		//this gets done before we save their profile, so authId gets inserted into the profile table
		if (req.hasParameter("password")) {
			log.debug("pswd=" + req.getParameter("password") + " and email=" + req.getParameter("pfl_EMAIL_ADDRESS_TXT"));
			try {
				this.addAuthentication(req);
			} catch (Exception e) {
				log.error("could not add profileRole or auto-login user " + req.getParameter("profileId"));
			}
		}
		
		/*
		if (req.hasParameter("reprocessEmails")) {
			procEmails(req);
			return;
		}
		*/
		
		//we need to get the sb_action variables for this DlrContactAction before we call it.
		//this action is also a facade, which requires it's own attributes to call the ContactFacadeAction
		req.setParameter(SB_ACTION_ID, String.valueOf(mod.getAttribute(ModuleVO.ATTRIBUTE_2)));
		super.retrieve(req);
		ModuleVO mod2 = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		SBModuleVO sbMod = (SBModuleVO) mod2.getActionData();
		mod.setActionId(sbMod.getActionId());
		mod.setModuleTypeId(sbMod.getModuleTypeId());
		mod.setActionName(sbMod.getActionName());
		mod.setActionDesc(sbMod.getActionDesc());
		mod.setAttribute(SBModuleVO.ATTRIBUTE_1, sbMod.getAttribute(SBModuleVO.ATTRIBUTE_1));
        mod.setAttribute(SBModuleVO.ATTRIBUTE_2, sbMod.getAttribute(SBModuleVO.ATTRIBUTE_2));
        
        actionInit.setActionId(mod.getActionId());
		SMTActionInterface sai = new DealerContactAction(actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		sai.build(req);
		
		//if we successfully created a login account, then grant them access to login.
		if (req.hasParameter("pfl_AUTHENTICATION_ID")) {
			log.debug("pswd=" + req.getParameter("password") + " and email=" + req.getParameter("pfl_EMAIL_ADDRESS_TXT"));
			try {
				this.addSiteRole(req);
			} catch (Exception e) {
				log.error("could not add profileRole or auto-login user " + req.getParameter("profileId"));
			}
		}
		
		//setup the redirect
		String csi = StringUtil.checkVal(req.getAttribute(SubmittalAction.CONTACT_SUBMITTAL_ID));
		StringBuffer redir = new StringBuffer();
		redir.append(req.getRequestURI()).append("?contactSubmitted=true&dlrInfoSub=true");
		redir.append("&dealerLocationId=").append(req.getParameter("dealerLocationId"));
		if (StringUtil.checkVal(req.getParameter("con_c0a8023721565d1bdd5add6a42b2f3c8")).endsWith("now")) {
			redir.append("&files=1&csi=").append(csi);
			
			//record this status
			req.setParameter("status", "Prompting for Files");
			
		} else {
			//record status complete
			req.setParameter("status", "Transaction Complete");
		}
		
		req.setParameter("csi", csi);
		this.recordStatus(req, false);
		
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, redir.toString());
		
		//flush placeholder value so the user can start over if they want
		req.getSession().removeAttribute("acctType");
	}
	
	/*
	private final void procEmails(SMTServletRequest req) {
		ReprocessContactEmails rep = new ReprocessContactEmails(this.actionInit);
		rep.setDBConnection(dbConn);
		rep.setAttributes(attributes);
		rep.procEmails(req);
		rep = null;
	}
	*/
	
	/* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
    public void retrieve(SMTServletRequest req) throws ActionException {
		SMTActionInterface sai = null;
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String pmid = mod.getPageModuleId();
		HttpSession ses = req.getSession();

		if (Convert.formatBoolean(req.getParameter("start")))
			ses.removeAttribute("acctType");
		
		SBUserRole role = null;
		try {
			role = (SBUserRole) ses.getAttribute(Constants.ROLE_DATA);
		} catch (Exception e) {}
		
		//record account type for use on the form (the final of 'x' screens)
		//are we creating a new account, submitting as a guest, or submitting as a registered user?:
		if (role != null && ses.getAttribute("acctType") == null) {
			ses.setAttribute("acctType", "registered");
			if (req.hasParameter("dealerLocationId"))
				req.setParameter("dealerLocationId", StringUtil.checkVal(ses.getAttribute("franchiseId")));
		} else if (req.getParameter("acctType") != null) {
			ses.setAttribute("acctType", req.getParameter("acctType"));
		}
		//ses.removeAttribute("acctType");
		String dlrLocnId = StringUtil.checkVal(req.getParameter("dealerLocationId"));
		log.debug("dlrLocn=" + dlrLocnId + ", acctType=" + ses.getAttribute("acctType"));
		
		if ((dlrLocnId.length() == 0 && ses.getAttribute("acctType") != null) ||
				dlrLocnId.length() > 0 && Convert.formatBoolean(req.getParameter("contactSubmitted"))) {
			
			//summons the Dealer Locator
			mod.setPageModuleId(String.valueOf(mod.getAttribute(ModuleVO.ATTRIBUTE_1)));
			actionInit.setActionId(mod.getPageModuleId());
			req.setParameter("pmid", mod.getPageModuleId());
			sai = new DealerLocatorAction(actionInit);
			sai.setAttributes(attributes);
			sai.setDBConnection(dbConn);
			sai.retrieve(req);
			req.setParameter("pmid", pmid);
			
		} else if (dlrLocnId.length() > 0) {
			//we need to get the sb_action variables for this DlrContactAction before we call it.
			//this action is also a facade, which requires it's own attributes to call the ContactFacadeAction
			req.setParameter(SB_ACTION_ID, String.valueOf(mod.getAttribute(ModuleVO.ATTRIBUTE_2)));
			super.retrieve(req);
			ModuleVO mod2 = (ModuleVO) attributes.get(Constants.MODULE_DATA);
			SBModuleVO sbMod = (SBModuleVO) mod2.getActionData();
			mod.setActionId(sbMod.getActionId());
			mod.setModuleTypeId(sbMod.getModuleTypeId());
			mod.setActionName(sbMod.getActionName());
			mod.setActionDesc(sbMod.getActionDesc());
			mod.setAttribute(SBModuleVO.ATTRIBUTE_1, sbMod.getAttribute(SBModuleVO.ATTRIBUTE_1));
            mod.setAttribute(SBModuleVO.ATTRIBUTE_2, sbMod.getAttribute(SBModuleVO.ATTRIBUTE_2));
			
			//load the Contact Us
			log.debug("starting contact us for "  + mod.getAttribute(ModuleVO.ATTRIBUTE_2));
			sai = new DealerContactAction(actionInit);
			sai.setAttributes(attributes);
			sai.setDBConnection(dbConn);
			sai.retrieve(req);
			
		}
		
		//restore the values of the facade action
		mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		mod.setPageModuleId(pmid);
		attributes.put(Constants.MODULE_DATA, mod);
		
		//record passed status
		if (req.hasParameter("contactSubmitted") && req.hasParameter("csi") && req.hasParameter("status"))
			this.recordStatus(req, false);
	}
	
	/*
	 * evaluates and submits the PROFILE_ROLE entry to give this user login permissions
	 */
	private void addSiteRole(SMTServletRequest req) throws Exception {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		String profileId = req.getParameter("profileId"); //set by Contact's SubmittalAction
		if (profileId == null || profileId.length() == 0) return; //cannot continue without a profile!
		String authId = req.getParameter("authenticationId");
		
		log.debug("inserting ProfileRoleAssoc");
		ProfileRoleManager prm = new ProfileRoleManager();
		try {
			//if the user doesn't already have a role on this website add one.
			if (StringUtil.checkVal(prm.checkRole(profileId, site.getSiteId(), dbConn)).length() == 0) {
				SBUserRole role = new SBUserRole(site.getSiteId());
				role.setProfileId(profileId);
				role.setRoleId(String.valueOf(SecurityController.PUBLIC_REGISTERED_LEVEL));
				role.setStatusId(SecurityController.STATUS_ACTIVE);
				prm.addRole(role, dbConn);
				log.debug("finished adding role");
			}
			log.debug("done with roles " + prm.checkRole(profileId, site.getSiteId(), dbConn) + " for proId=" + profileId + " and site=" + site.getSiteId());
		} catch (Exception e) {
        	log.error("could not add profile_role for profileId=" + profileId + " on site=" + site.getSiteId(), e);
        	throw(e);
        }
		
		//perform automatic login
		try {
			UserDataVO user = new UserDataVO();
			user.setProfileId(profileId);
			user.setAuthenticationId(authId);
        	SecurityController sc = new SecurityController(site.getLoginModule(), site.getRoleModule(), attributes);
	        sc.checkRole(user, req, site.getSiteId(), dbConn, site.getOrganizationId());
	        log.debug("login complete");
        } catch (Exception e) {
        	log.error("could not auto-login user after registration, profileId=" + profileId, e);
        }
	}
	
	/*
	 * evaluates and submits the PROFILE_ROLE entry to give this user login permissions
	 */
	private void addAuthentication(SMTServletRequest req) throws Exception {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		String emailAddress = req.getParameter("pfl_EMAIL_ADDRESS_TXT");
		String password = req.getParameter("password");
		String authId = null;
		
		// ADD THE AUTHENTICATION ENTRY
		try {
			log.debug("adding authentication record");
			Map<String, Object> lm = new HashMap<String, Object>();
			lm.put(Constants.ENCRYPT_KEY, getAttribute(Constants.ENCRYPT_KEY));
			lm.put(GlobalConfig.KEY_DB_CONN, dbConn);
			AbstractLoginModule loginModule = SecurityModuleFactoryImpl.getLoginInstance(site.getLoginModule(), lm);
			
			//try to discover the authId via emailAddress lookup
			authId = loginModule.retrieveAuthenticationId(emailAddress);

			//insert or update the AUTHENTICATION table, returns the pkId
			authId = loginModule.manageUser(authId, emailAddress, password, 0);  //pwd will be encrypted at qry
		} catch (Exception e) {
			log.error("AuthId exception", e);
			throw(e);
		}
		
		//this gets passed to ContactSubmittalAction and saved to the PROFILE table.
		req.setParameter("pfl_AUTHENTICATION_ID", authId);
	}
	
	
	public void sendNotifications(SMTServletRequest req, String senderEmail) {
		String userEmail = req.getParameter("userEmail");
		String status = "";
		
		// email the STORE
		try {
    		SMTMail mail = new SMTMail((String)getAttribute(Constants.CFG_SMTP_SERVER));
    		mail.setUser((String)getAttribute(Constants.CFG_SMTP_USER));
    		mail.setPassword((String)getAttribute(Constants.CFG_SMTP_PASSWORD));
    		mail.setPort(Integer.valueOf((String)getAttribute(Constants.CFG_SMTP_PORT)));
    		mail.setRecpt(req.getParameter("dealerEmail").split(","));
    		mail.setSubject("SAF Completed: " + userEmail);
    		mail.setFrom(senderEmail);
    		mail.setHtmlBody(this.buildEmail(true, req));
    		mail.postMail();
        	status = "sent Center email to " + req.getParameter("dealerEmail");
    	} catch (Exception me) {
    		log.error("EmailToStore", me);
        	log.error("could not send Center email to " + req.getParameter("dealerEmail"));
        	status = " Email failed to " + req.getParameter("dealerEmail");
    	}
    	log.error("sent Center email to " + req.getParameter("dealerEmail") + " for CSI=" + req.getParameter("csi"));
    	
		// email the USER
		try { 
    		SMTMail mail = new SMTMail((String)getAttribute(Constants.CFG_SMTP_SERVER));
    		mail.setUser((String)getAttribute(Constants.CFG_SMTP_USER));
    		mail.setPassword((String)getAttribute(Constants.CFG_SMTP_PASSWORD));
    		mail.setPort(Integer.valueOf((String)getAttribute(Constants.CFG_SMTP_PORT)));
    		mail.setRecpt(new String[] { userEmail });
    		mail.setSubject("Your file(s) have been delivered to FASTSIGNS");
    		mail.setFrom(senderEmail);
    		mail.setHtmlBody(this.buildEmail(false, req));
    		mail.postMail();
    		status += ", sent user email to " + userEmail;
    	} catch (Exception me) {
    		log.error("EmailToUser", me);
        	log.error("could not send user email to " + userEmail);
        	status = status + " Email failed to " + userEmail;
    	}
    	log.error("sent user email to " + userEmail + " for CSI=" + req.getParameter("csi"));
    	
    	//document the failed emails, this gets written into the database via this.recordStatus(req);
    	if (status.length() > 0) 
        	req.setParameter("status", status);
    	
		return;
	}
	
	@SuppressWarnings("deprecation")
	protected String buildEmail(Boolean isStore, SMTServletRequest req) {
		String userEmail = req.getParameter("userEmail");
		String storeEmail = req.getParameter("dealerEmail");
		
		StringBuffer sb = new StringBuffer();
		if (isStore) {
			sb.append("<p>The following files have been sent to you from ").append(userEmail);
			sb.append(".  You should have received the request in a SAF Initiated File email.  ");
			sb.append("The file has downloaded and is now available.").append(".</p>");
			sb.append("<p>Click the links below to download the file(s).<br/>");
			sb.append("**These files will expire 14 days from the time this email was generated.</p>");
		} else {
			sb.append("<p>Thank you for sending your file to FASTSIGNS&reg;.  Your file(s) has been delivered to ");
			sb.append(storeEmail).append(".</p>");
			sb.append("<p>You may click the links below to download your file(s).  ");
			sb.append("The download links will expire 14 days from the time this email was generated.</p>");
		}

		//add the file names/URLs to the email
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		List<?> files = (List<?>) mod.getActionData();
		for (int x=0; x < files.size(); x++) {
			try {
				if (x > 0) sb.append("<br/>");
				FilePartDataBean f = (FilePartDataBean) files.get(x);
				sb.append("File Name: ").append(f.getFileName()).append("<br/>");
				sb.append("<a href=\"").append(f.getPath()).append("\">").append(f.getPath()).append("</a><br/>");
				try {
					Date d = Convert.formatDate(Convert.DATE_TIME_DASH_PATTERN, f.getKey());
					if (d == null) d = new Date();  //this is a safe alternative because files are processed in real time.
					sb.append("Sent: ").append(Convert.formatDate(d, "MM-dd-yyyy")).append(" at ");
					sb.append(Convert.formatDate(d, Convert.TIME_LONG_PATTERN)).append("<br/>");
				} catch (Exception e) { log.error("casting dates for the email", e); }
			} catch (Exception e) {
				log.error("assembling files for email, CSI=" + req.getParameter("csi"));
			}
		}

		sb.append("<p>Please do not reply to this email address -- contact ");
		if (isStore) {
			sb.append("the customer if needed at: ").append(userEmail).append("</p>");
		} else {
			sb.append(storeEmail).append("</p>");
		}
		
		return sb.toString();
	}
	
	
	/** this method updates the contact_data table and adds a few extra fields to the input.
	 * the fields contain the URLs to the download file(s) associated with this User Request 
	 * @param req
	 */
	@SuppressWarnings("deprecation")
	private void logFiles(SMTServletRequest req) {
		String csi = req.getParameter("csi"); //contactSubmittalId
		String fieldId = "7f000101580d3209dd677866f73ed913"; //this came straight out of the FS database!
		StringBuffer sb = new StringBuffer();
		StringBuffer sql = new StringBuffer();
		PreparedStatement ps  = null;
		String status = null;
		
		sql.append("insert into contact_data (contact_data_id, contact_submittal_id, ");
		sql.append("contact_field_id, value_txt, data_enc_flg, create_dt) ");
		sql.append("values (?,?,?,?,?,?)");
		log.debug(sql);
		
		//load the files
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		List<?> files = (List<?>) mod.getActionData();
		for (int x=0; x < files.size(); x++) {
			try {
				if (x > 0) sb.append("\r\n");
				FilePartDataBean f = (FilePartDataBean) files.get(x);
				sb.append(f.getFileName()).append(", ");
				sb.append(f.getPath()).append(", ");
				sb.append(f.getKey());
			} catch (Exception e) {
				status = "iterating files to write to DB, CSI=" + csi + ", modData=" + mod.getActionData().toString();
				log.error(status);
			}
		}
		
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, csi);
			ps.setString(3, fieldId);
			ps.setString(4, sb.toString());
			ps.setInt(5, 0);
			ps.setTimestamp(6, Convert.getCurrentTimestamp());
			int x = ps.executeUpdate();
			if (x < 1) throw new SQLException();
			status = "Files Saved Successfully";
		} catch (SQLException sqle) {
			status = "Files not recorded for CSI=" + csi + ", files=" + sb.toString();
			log.error(status, sqle);
			
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		req.setParameter("status", status);
	}
	
	public void recordStatus(SMTServletRequest req, boolean isInsert) {
		String fieldId = "7f000101ed12428e6f503d8d58e4ef90"; //this came straight out of the FS database!
		String csi = req.getParameter("csi");
		String status = req.getParameter("status");
		if (status != null) {
			try {
				StringEncoder se = new StringEncoder();
				status = se.decodeValue(req.getParameter("status"));
			} catch (Exception e) {
				status = req.getParameter("status");
			}
		} else {
			log.error("no status provided, isInsert=" + isInsert + ", nothing updated for CSI=" + csi);
			return;
		}
		
		StringBuilder sql = new StringBuilder();
		if (isInsert) {
			sql.append("insert into contact_data (value_txt, create_dt, ");
			sql.append("contact_submittal_id, contact_field_id, contact_data_id) values (?,?,?,?,?)");
		} else {
			sql.append("update contact_data set value_txt=?, update_dt=? ");
			sql.append("where contact_submittal_id=? and contact_field_id=?");
		}
		log.debug(sql);
		
		int x = 0;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, status);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, csi);
			ps.setString(4, fieldId);
			if (isInsert) ps.setString(5, new UUIDGenerator().getUUID());
			x = ps.executeUpdate();
			
		} catch (SQLException sqle) {
			log.error(sqle);
			
		} finally {
			try { ps.close(); } catch (Exception e) {}

			//if the update failed, attempt to create a new record.  
			//this should ensure no blank records exist in the DB
			if (x < 1 && !isInsert) {
				log.error("update failed for CSI=" + csi + ", attempting insert");
				this.recordStatus(req, true);
			} else if (x > 0) {
				log.error(((isInsert)?"Inserted":"Updated") + " status on CSI=" + csi + " to " + status);
			}
		}
	}
}
