package com.fastsigns.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import com.fastsigns.action.saf.SAFConfig;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.AbstractLoginModule;
import com.siliconmtn.security.SecurityModuleFactoryImpl;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.contact.ContactDataAction;
import com.smt.sitebuilder.action.contact.ContactDataContainer;
import com.smt.sitebuilder.action.contact.SubmittalAction;
import com.smt.sitebuilder.action.contact.SubmittalDataAction;
import com.smt.sitebuilder.action.dealer.DealerContactAction;
import com.smt.sitebuilder.action.dealer.DealerLocatorAction;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>: RequestAQuoteSTF.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 16, 2010
 * @Updates
 * James McKain, Feb 28, 2014 - refactored recordStatus() to move it's functionality 
 * 		to a common/reusable location in the WC core.
 * Billy Larsen, July 8, 2014 - added comments with instructions on what to do in the event
 * of an email Reprocess call.  Search for for the tag EmailReprocess in this document to
 * quickly reach relevant parts of code. Also see ContactFacadeAction for changes that need
 * to be made there.
 ****************************************************************************/
public class RequestAQuoteSTF extends SBActionAdapter {
	
	private static final String LOCATION_NAME =  "locationName";
	private static final String ALIAS_PATH = "aliasPath";
	private static final String PHONE_NUMBER = "phoneNumber";
	private static final String FASTSIGNS_TEXT = "fastsignsTxt";
	private static final String USER_EMAIL = "userEmail";
	private static final String DEALER_EMAIL = "dealerEmail";
	public static final String DEALER_LOCATION_ID = "dealerLocationId";
	
	
	public RequestAQuoteSTF(ActionInitVO actionInit) {
		super(actionInit);
	}

	public RequestAQuoteSTF() {
	}

	/* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
    public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	/**
	 * process postback.  this captures the file names/URLs returned from SendThisFile.com
	 * and sends the emails to the User and Center, completing the Transaction.
	 * @param req
	 * @throws ActionException
	 */
	public void processPostback(SMTServletRequest req) throws ActionException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		SAFConfig safConfig = SAFConfig.getInstance(site.getCountryCode());
		String csi = req.getParameter("csi");
		
		//log milestone "File Transfer Complete"
		this.recordStep(csi, TransactionStep.fileReceived, safConfig);
		
		//record the file URLs to the DB
		this.logFiles(req, safConfig);
		
		//send notification emails
		this.sendEmail(req,safConfig);
		
		//this transaction is done!
		this.recordStep(csi, TransactionStep.complete,safConfig);
		
	}
	
	/**
	 * controller method for sending the SAF emails.
	 * Calls Contact Report actions to load the data from the database prior to sending.
	 * @param req
	 * @throws ActionException
	 */
	public void sendEmail(SMTServletRequest req, SAFConfig safConfig) throws ActionException {
		String csi = req.getParameter("csi");
		boolean isSAF = Convert.formatBoolean(req.getParameter("saf"));
		Map<String, String> vals = null;
		try {
			//puts dealerEmail and userEmail on the request object
			vals = loadEmailInfo(req, csi);
		} catch (InvalidDataException e) {
			log.error(e);
		}
		
		//load the data from the database
		req.setParameter("countryCode", safConfig.getCountryCode());
		req.setParameter("contactSubmittalId", csi);
		req.setParameter(Constants.DEALER_LOCATION_ID_KEY, "");
		req.setParameter("contactId", safConfig.getContactUsActionId() + "|Request A Quote");
		ContactDataAction cda = new ContactDataAction(actionInit);
		cda.setDBConnection(dbConn);
		cda.setAttributes(attributes);
		cda.update(req);  //loads the data as a report (ContactDataContainer)
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		ContactDataContainer cdc = (ContactDataContainer) mod.getActionData();
		
		try {
			this.sendCenterEmail(req, cdc, isSAF, safConfig, vals);
		} catch (MailException e) {
			//EmailReprocess - Comment this line so we don't change status when we send
			this.recordStatus(csi, "Center email failed. " + e.getMessage(), safConfig);
		}
		
		try {
			//EmailReprocess - Comment this line so we don't change status when we send
			this.sendUserEmail(req, cdc, isSAF, safConfig, vals);
		} catch (MailException e) {
			this.recordStatus(csi, "User email failed. " + e.getMessage(), safConfig);
		}
		//EmailReprocess - Comment this line so we don't change status when we send
		this.recordStep(csi, TransactionStep.emailSent, safConfig);
	}
	
	
	
	/* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
    public void build(SMTServletRequest req) throws ActionException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		SAFConfig safConfig = SAFConfig.getInstance(site.getCountryCode());
		
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		boolean promptForFiles = StringUtil.checkVal(req.getParameter("con_" + safConfig.getSendingFilesNowFieldId())).endsWith("now");
		String dlrEmails = req.getParameter("contactEmailAddress");
				
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
		
		//prevent ContactSubmitallAction from sending emails at this stage if we're asking for files next.
        req.setParameter("contactEmailAddress", "", true);
        req.setParameter("overrideEmails", "true", true);
        
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
		
		String csi = StringUtil.checkVal(req.getAttribute(SubmittalAction.CONTACT_SUBMITTAL_ID));
		this.recordStep(csi, TransactionStep.initiated, safConfig);
		
		//setup the redirect
		StringBuffer redir = new StringBuffer();
		redir.append(req.getRequestURI()).append("?contactSubmitted=true&dlrInfoSub=true");
		redir.append("&dealerLocationId=").append(req.getParameter(Constants.DEALER_LOCATION_ID_KEY));
		
		if (promptForFiles) {
			redir.append("&files=1&csi=").append(csi).append("&userEmail=").append(req.getParameter("pfl_EMAIL_ADDRESS_TXT"));
			redir.append("&dealerEmail=").append(dlrEmails);
			
			//record "going to step 2" status
			this.recordStep(csi, TransactionStep.filePromptRedirect, safConfig);
			
		} else {
			req.setParameter("csi", csi);
			
			//send notification emails
			this.sendEmail(req, safConfig);
			
			//this transaction is done!
			this.recordStep(csi, TransactionStep.complete, safConfig);
		}
		
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, redir.toString());
		
		//flush our placeholder value so the user can start over if they want
		req.getSession().removeAttribute("acctType");
	}
	
	
	/* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
    public void retrieve(SMTServletRequest req) throws ActionException {
		SMTActionInterface sai = null;
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String pmid = mod.getPageModuleId();
		HttpSession ses = req.getSession();
		String dlrLocnId = StringUtil.checkVal(req.getParameter(Constants.DEALER_LOCATION_ID_KEY));
		
		/*
		 * EmailReprocess - Uncomment this block to enable the hook for email reprocessing.
		 */
//		if (req.hasParameter("reprocessEmails")) {
//			ReprocessContactEmails rep = new ReprocessContactEmails(this.actionInit);
//			rep.setDBConnection(dbConn);
//			rep.setAttributes(attributes);
//			rep.procEmails(req);
//			rep = null;
//			return;
//		}
		
		
		//record passed status.  This scenario is called when a firewall block occurs.
		if (req.hasParameter("contactSubmitted") && req.hasParameter("csi") && req.hasParameter("status")) {
			SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
			SAFConfig safConfig = SAFConfig.getInstance(site.getCountryCode());
			String csi = req.getParameter("csi");
			this.recordStatus(csi, req.getParameter("status"), safConfig);
			this.recordStep(csi, TransactionStep.fileCanceled, safConfig);
			this.sendEmail(req, safConfig);
			this.recordStep(csi, TransactionStep.complete, safConfig);
		}	
		
		if (Convert.formatBoolean(req.getParameter("start")))
			ses.removeAttribute("acctType");
		
		SBUserRole role = null;
		try {
			role = (SBUserRole) ses.getAttribute(Constants.ROLE_DATA);
		} catch (Exception e) {}
		//record account type for use on the form (the final of 'x' screens)
		//are we creating a new account, submitting as a guest, or submitting as a registered user?:
		//If we have logged in and we already have an acctType of guest we also upgrade them here
		if (role != null && !"registered".equals(ses.getAttribute("acctType"))) {
			log.debug("Something");
			ses.setAttribute("acctType", "registered");
			if (!req.hasParameter(Constants.DEALER_LOCATION_ID_KEY))
				req.setParameter(Constants.DEALER_LOCATION_ID_KEY, StringUtil.checkVal(ses.getAttribute("franchiseId")));
			dlrLocnId = req.getParameter(Constants.DEALER_LOCATION_ID_KEY);
		} else if (req.hasParameter("acctType")) {
			log.debug("Something else");
			ses.setAttribute("acctType", req.getParameter("acctType"));
		}
		
		
		if ((dlrLocnId.length() == 0 && ses.getAttribute("acctType") != null) ||
				dlrLocnId.length() > 0 && req.hasParameter("contactSubmitted")) {
			//summons the Dealer Locator
			req.setParameter(Constants.DEALER_LOCATION_ID_KEY, dlrLocnId);
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
			log.debug("actionGroupId: " + mod.getAttribute(ModuleVO.ATTRIBUTE_2));
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
	}
	
	
	/**
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
	        sc.checkRole(user, req, site.getSiteId(), dbConn);
	        log.debug("login complete");
        } catch (Exception e) {
        	log.error("could not auto-login user after registration, profileId=" + profileId, e);
        }
	}
	
	
	/**
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
	
	
	/**
	 * sends the email to the Center, called from sendEmail()
	 * @param req
	 * @param cdc
	 * @param senderEmail
	 * @throws MailException
	 */
	private void sendCenterEmail(SMTServletRequest req, ContactDataContainer cdc, Boolean isSAF, SAFConfig safConfig, Map<String, String> vals) 
	throws MailException {
		EmailMessageVO msg = new EmailMessageVO();
		try {
			msg.addRecipients(req.getParameter(DEALER_EMAIL).split(","));
			msg.setSubject(safConfig.getEmailSubjectCenter(req.getParameter(USER_EMAIL)));
			msg.setFrom(safConfig.getNoReplyEmailAddress());
			msg.setHtmlBody(safConfig.buildEmail(true, cdc, vals));
		} catch (InvalidDataException e) {
			log.error("could not send SAF Center email", e);
			return;
		}
		
		MessageSender ms = new MessageSender(attributes, dbConn);
		ms.sendMessage(msg);
		
		log.info("sent Center email to " + req.getParameter(DEALER_EMAIL) + " for CSI=" + req.getParameter("csi"));
	}
	
	
	/**
	 * sends the email to the User, called from sendEmail()
	 * @param req
	 * @param cdc
	 * @param senderEmail
	 * @throws MailException
	 */
	private void sendUserEmail(SMTServletRequest req, ContactDataContainer cdc, Boolean isSAF, SAFConfig safConfig, Map<String, String> vals) 
	throws MailException {
		EmailMessageVO msg = new EmailMessageVO();
		try {
			msg.addRecipient(req.getParameter("userEmail"));
			msg.setSubject(safConfig.getEmailSubjectUser());
			msg.setFrom(safConfig.getSenderEmailAddress(vals));
			msg.setHtmlBody(safConfig.buildEmail(false, cdc, vals));
		} catch (InvalidDataException e) {
			log.error("could not send SAF User email", e);
			return;
		}
		
		MessageSender ms = new MessageSender(attributes, dbConn);
		ms.sendMessage(msg);
		log.info("sent User email to " + req.getParameter("userEmail") + " for CSI=" + req.getParameter("csi"));
	}
	
	
	/** this method updates the contact_data table and adds a few extra fields to the input.
	 * the fields contain the URLs to the download file(s) associated with this User Request 
	 * @param req
	 */
	@SuppressWarnings("deprecation")
	private void logFiles(SMTServletRequest req, SAFConfig safConfig) {
		String csi = req.getParameter("csi"); //contactSubmittalId
		StringBuffer sb = new StringBuffer();
		String status = null;
		
		//load the files from ModuleVO
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		List<?> files = (List<?>) mod.getActionData();
		for (int x=0; x < files.size(); x++) {
			try {
				if (x > 0) sb.append("\r\n");
				FilePartDataBean f = (FilePartDataBean) files.get(x);
				sb.append(StringUtil.replace(f.getFileName(),",","%2C")).append(", ");
				sb.append(f.getPath());
				
				//expiration date is not supported at STF, so we'll leave this out for YSI as well.
				//if (f.getKey() != null)
				//	sb.append(", ").append(f.getKey());
				
			} catch (Exception e) {
				log.error(e);
				this.recordStatus(csi, "Could not parse files received. " + e.getMessage(), safConfig);
			}
		}
		
		if (sb.length() == 0)
			return;
		
		//write the container to the database
		StringBuffer sql = new StringBuffer();
		PreparedStatement ps  = null;
		sql.append("insert into contact_data (contact_data_id, contact_submittal_id, ");
		sql.append("contact_field_id, value_txt, data_enc_flg, create_dt) ");
		sql.append("values (?,?,?,?,?,?)");
		log.debug(sql + " " + sb);
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, csi);
			ps.setString(3, safConfig.getFilesFieldId());
			ps.setString(4, sb.toString());
			ps.setInt(5, 0);
			ps.setTimestamp(6, Convert.getCurrentTimestamp());
			ps.executeUpdate();

		} catch (SQLException sqle) {
			status = "Files not recorded for CSI=" + csi + ", files=" + sb.toString();
			log.error(status, sqle);
			this.recordStatus(csi, status, safConfig);
			
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}

		this.recordStep(csi, TransactionStep.fileSaved, safConfig);
	}
	
	
	/**
	 * documents that a condition was reached that is noteworthy.
	 * @param csi
	 * @param message
	 */
	public void recordStatus(String csi, String message, SAFConfig safConfig) {
		this.recordStatus(message, csi, safConfig.getStatusFieldId());
		
	}
	
	
	/** 
	 * documents that a milestone in the process has been reached.
	 * @param step
	 * @param csi
	 * @param message
	 */
	public void recordStep(String csi, TransactionStep step, SAFConfig safConfig) {
		this.recordStatus(step.toString(), csi, safConfig.getTransactionStageFieldId());
	}
	
	private final void recordStatus(String message, String csi, String fieldId) {
		// cleanup any HTML passed in the message
		try {
			StringEncoder se = new StringEncoder();
			message = se.decodeValue(message);
		} catch (Exception e) {}
		
		SubmittalDataAction sda = new SubmittalDataAction(actionInit);
		sda.setDBConnection(dbConn);
		try {
			sda.updateField(message, csi, fieldId);
		} catch (SQLException sqle) {
			log.error("DB update failed for csi=" + csi + ", fieldId=" + fieldId + ", msg=" + message);
			log.error("could not update contact field", sqle);
		}
	}
	
	/**
	 * retrieves the User and Center emails from the database prior to sending emails
	 * @param req
	 * @param csi
	 * @throws InvalidDataException
	 */
	private final Map<String, String> loadEmailInfo(SMTServletRequest req, String csi) 
	throws InvalidDataException {
		Map<String, String> vals = new HashMap<String, String>();
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sb = new StringBuilder();
		sb.append("select a.email_address_txt, b.email_address_txt, b.attrib2_txt, ");
		sb.append("b.location_nm, d.alias_path_nm, b.primary_phone_no, b.dealer_location_id ");
		sb.append("from contact_submittal a inner join dealer_location b "); 
		sb.append("on a.dealer_location_id = b.dealer_location_id ");
		sb.append("left outer join ").append(customDb).append("FTS_FRANCHISE c on ");
		sb.append("b.dealer_location_id = c.franchise_id ");
		sb.append("left outer join site d on d.SITE_NM = 'Center Page ' + c.FRANCHISE_ID ");
		sb.append("where contact_submittal_id = ? ");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, csi);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				log.debug("Emails: " + rs.getString(1) + "|" + rs.getString(2));
				req.setParameter(USER_EMAIL, rs.getString(1));
				req.setParameter(DEALER_EMAIL, rs.getString(2));
				vals.put(USER_EMAIL, rs.getString(1));
				vals.put(DEALER_EMAIL, rs.getString(2));
				vals.put(FASTSIGNS_TEXT, rs.getString(3));
				vals.put(LOCATION_NAME, rs.getString(4));
				vals.put(ALIAS_PATH, rs.getString(5));
				vals.put(PHONE_NUMBER, rs.getString(6));
				vals.put(DEALER_LOCATION_ID, rs.getString(7));
				
			} 
		} catch (Exception e) { 
			throw new InvalidDataException("Unable to locate emailinf or CSI: " + csi, e);
		} finally {
			try { ps.close(); } catch(Exception e) {}
		}
		return vals;
	}
	
	
	/** Not presently needed.  Required if YSI sends multiple postbacks per transaction
	public TransactionStep getStep(String csi) {
		TransactionStep ts = null;
		String sql = "select value_txt from contact_data where contact_field_id=? and contact_submittal_id=?";
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql);
			ps.setString(1, STAGE_FIELD_ID);
			ps.setString(2, csi);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				TransactionStep.valueOf(rs.getString(1));
		} catch (SQLException sqle) {
			log.error("could not load transactionStep for csi=" + csi, sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		return ts;
	}
	**/
	
	
	
	/**
	 * Simple Enum of the steps involved in this process.
	 * These determine where (in the process) the user is, 
	 * Should be used to block duplicate efforts on previously-completed steps.
	 */
	public static enum TransactionStep {
		initiated, // initial form submitted
		filePromptRedirect, //redirecting user to file-upload page
		filePrompt, //the user is looking at the file-upload form (AJAX call here!)
		fileSent, //user clicked "submit" button to send files
		fileReceived, //STF acknowledgement of files (postback initiated)
		fileSaved, //STF files recorded in our database
		fileCanceled, //File upload canceled by user.  Typically a firewall block
		emailSent, //emails sent to Center and User
		complete;  //transaction complete
	}
}
