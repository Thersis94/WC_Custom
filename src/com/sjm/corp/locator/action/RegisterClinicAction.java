package com.sjm.corp.locator.action;

// JDK 1.6.x
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

// J2EE
import javax.servlet.http.HttpSession;

// JCaptcha
import com.octo.captcha.service.image.ImageCaptchaService;

// SMT BAseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.AbstractRoleModule;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// WC libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.dealer.DealerInfoAction;
import com.smt.sitebuilder.action.dealer.DealerLocatorAction;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.http.captcha.CaptchaServiceSingleton;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.security.UserLogin;

/****************************************************************************
 * <b>Title</b>: RegisterClinicAction.java <p/>
 * <b>Project</b>: SB_ANS_Medical <p/>
 * <b>Description: </b> Manages clinic data for the SJM corporate locator
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Mar 10, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class RegisterClinicAction extends SBActionAdapter {
	
	public static final String CLINIC_FORM_VALUES = "clinicFormValues";
	
	/**
	 * 
	 */
	public RegisterClinicAction() {}

	/**
	 * @param actionInit
	 */
	public RegisterClinicAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		log.debug("************ " + mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		
		this.manageSessionBean(req, false, false);
		
		boolean dlrSub = Convert.formatBoolean(req.getParameter("dlrSub"));
		if (dlrSub) {
			ActionInitVO ai = new ActionInitVO();
			ai.setActionId((String) mod.getAttribute(ModuleVO.ATTRIBUTE_1));
			ai.setName(actionInit.getName());
			SMTActionInterface sai = new DealerLocatorAction(ai);
			sai.setAttributes(attributes);
			sai.setDBConnection(dbConn);
			sai.retrieve(req);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("Starting Clinic Build Action: " + req.getParameter("adminPhone"));
		String msg = "You have successfully updated the clinic.";
		Boolean adminSubmitted = Convert.formatBoolean(req.getParameter("adminSubmitted"));
		String dealerLocationId = req.getParameter("dealerLocationId");
		boolean isValidCaptcha = this.validateCaptcha(req);
		
		if (isValidCaptcha) {
			try {
				// Add the dealer/dealer_location data
				if (! adminSubmitted)
					dealerLocationId = this.addDealerLocation(req);
				
				// add the user
				this.assignUserData(req,dealerLocationId);
				
				// Send the email ** removed if (!adminSubmitted) 
				this.sendEmail(req);
			} catch(MailException me) {
				msg = "Unable to send email message";
				log.error("Email notification not sent", me);
			} catch(Exception e) {
				msg = "Unable to add clinic, " + e.getMessage(); 
			}			
		}
		
		// manage form values on session
		this.manageSessionBean(req, true, isValidCaptcha);
		
		// Build the Redirect URL
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		String url = StringUtil.checkVal(page.getFullPath());
		if (url.length() == 0) url = "/" + page.getAliasName();
		if (isValidCaptcha) {
			url += "/qs/complete/" + adminSubmitted + "/" + StringEncoder.urlEncode(msg);
		} else {
			if (! adminSubmitted) {
				url += "?addClinic=true";
			} else {
				url += "?dealerLocationId=" + dealerLocationId;	
			}
			url += "&revalidate=true&errorCode=1";
		}
		log.debug("URL Redirect: " + url);
		this.sendRedirect(url, "", req);
 	}
	
	/**
	 * Checks for a valid captcha response.
	 * @param req
	 * @return Returns true if captcha response is valid.  Otherwise, returns false.
	 */
	private boolean validateCaptcha(SMTServletRequest req) {
		log.debug("verifying captcha");
		HttpSession ses = req.getSession();
		boolean isValid = false;
		ImageCaptchaService service = CaptchaServiceSingleton.getInstance();
		if (service.validateResponseForID(ses.getId(), req.getParameter("captchaText"))) {
			isValid = true;
		}
		log.debug("captcha valid? " + isValid);
		return isValid;
	}
	
	/**
	 * Sets or removes session values for form fields that need to be preserved if captcha validation fails.
	 * @param req
	 * @param adminSubmitted
	 * @param isValidCaptcha
	 */
	private void manageSessionBean(SMTServletRequest req, boolean isBuild, boolean isValidCaptcha) {
		if (isBuild) {
			// build op, remove stale bean
			req.getSession().removeAttribute(CLINIC_FORM_VALUES);
			log.debug("build...removing bean");
			if (! isValidCaptcha) {
				// captcha validation failed, set bean values on session	
				ClinicRegisterFormFieldsVO vo = new ClinicRegisterFormFieldsVO(req);
				req.getSession().setAttribute(CLINIC_FORM_VALUES, vo);
				log.debug("build...setting bean");
			}
		} else {
			if (! Convert.formatBoolean(req.getParameter("revalidate"))) {
				// retrieve op and not performing captcha validation, remove bean from session
				req.getSession().removeAttribute(CLINIC_FORM_VALUES);
				log.debug("retrieve, no captcha validation...removing bean");
				return;
			}
		}
	}
	
	/**
	 * Sends the email to the country admin when a site is registered
	 * @param req
	 * @throws MailException
	 */
	public void sendEmail(SMTServletRequest req) throws MailException {
		// Share the data form the dmin approval action
		AdminApprovalAction aaa = new AdminApprovalAction(this.actionInit);
		aaa.setAttributes(attributes);
		aaa.setDBConnection(dbConn);
		
		// Get the email to address for the country.  Return if no address found
		String country = req.getParameter("country");
		List<String> countryAdmin = null;
		try {
			countryAdmin = aaa.getAdminEmail(req.getParameter("country"), 70, req);
			if (countryAdmin.size() == 0) return;
		} catch(SQLException sqle) {
			throw new MailException("Unable to email country admin for: " + country, sqle);
		}
		
		// build the html message
		StringBuilder sb = new StringBuilder();
		sb.append("You have a new request that requires your attention.  To view ");
		sb.append("the request for approval, visit ");
		sb.append("www.sjmcliniclocator.com/clinic_admin and select Clinic Requests.");
		sb.append("<br/><br/>");
		sb.append("Thank you,");
		sb.append("<br/><br/>");
		sb.append("St. Jude Medical Patient Travel Application"); 
		sb.append("<br/><br/>");
		sb.append(aaa.getEmailFooter());
		log.debug("Email HTML: " + sb);
		
		// Send the email
		SMTMail mail = new SMTMail();
		mail.setUser((String)getAttribute(Constants.CFG_SMTP_USER));
		mail.setPassword((String)getAttribute(Constants.CFG_SMTP_PASSWORD));
		mail.setPort(Integer.valueOf((String)getAttribute(Constants.CFG_SMTP_PORT)));
		mail.setSmtpServer((String)getAttribute(Constants.CFG_SMTP_SERVER));
		mail.setRecpt((String[])countryAdmin.toArray(new String[0]));
		//mail.setRecpt(new String[] {"IChristison@sjm.com"});
		mail.setSubject("Clinic Locator Update from St. Jude Medical");
		mail.setFrom("cliniclocator@sjm.com");
		mail.setReplyTo("cliniclocator@sjm.com");
		mail.setHtmlBody(sb.toString());
		mail.postMail();
	}
	
	/**
	 * Adds a user to the DB
	 * @param req
	 * @throws DatabaseException 
	 */
	public void assignUserData(SMTServletRequest req, String dlid) 
	throws DatabaseException {
		log.debug("assigning user data");
		
		// Add user profile
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		UserDataVO user = new UserDataVO();
		user.setFirstName(req.getParameter("firstName"));
		user.setLastName(req.getParameter("lastName"));
		user.setEmailAddress(req.getParameter("emailAddress"));
		user.setPassword(req.getParameter("Password"));
		user.setMainPhone(req.getParameter("adminPhone"));
		
		try {
			// Get the Auth ID
			String encKey = (String) this.getAttribute(Constants.ENCRYPT_KEY);
			UserLogin ul = new UserLogin(dbConn, encKey);
			
			// Add the authentication entry
			String authId = StringUtil.checkVal(ul.checkAuth(user.getEmailAddress()));
			authId = ul.modifyUser(authId, user.getEmailAddress(), user.getPassword(), 0);
			user.setAuthenticationId(authId);
			
			// Add the user profile
			user.setProfileId(pm.checkProfile(user, dbConn));
			pm.updateProfile(user, dbConn);
			
		} catch (Exception e) {
			log.error("Unable to add profile for the user", e);
		}
		
		// Update the role and dealer location
		log.debug("Adding new role");
		this.addRole(req, user.getProfileId(), dlid);
	}
	
	/**
	 * 
	 * @param req
	 * @throws DatabaseException
	 */
	public void addRole(SMTServletRequest req, String profileId, String dlid) 
	throws DatabaseException {
		String siteId = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getSiteId();
		if (this.isRoleAssigned(profileId, siteId)) return;
		
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		
		ProfileRoleManager prm = new ProfileRoleManager();
		SBUserRole role = new SBUserRole();
		role.setOrganizationId(site.getOrganizationId());
		role.setSiteId(site.getSiteId());
		role.setRoleId("c0a802376dd05e19a0a268136058b860");
		role.setRoleLevel(0);
		role.setStatusId(SecurityController.STATUS_ACTIVE);
		role.setProfileId(profileId);
		role.addAttribute(AbstractRoleModule.ATTRIBUTE_KEY_1, dlid);
		prm.addRole(role, dbConn);
	}
	
	/**
	 * Checks to see if a role exists for the user just assigned.
	 * @param profileId
	 * @return true if a user has a role.  False if no role is assigned
	 * @throws DatabaseException
	 */
	public boolean isRoleAssigned(String profileId, String siteId) throws DatabaseException {
		log.debug("Checking role assignment");
		
		ProfileRoleManager prm = new ProfileRoleManager();
		String roleId = prm.checkRole(profileId, siteId, dbConn);
		
		return roleId != null && roleId.length() > 0;
	}
	
	/**
	 * Adds the franchise to the dealer location tables
	 * @param vo
	 * @throws DatabaseException 
	 * @throws Exception
	 */
	public String addDealerLocation(SMTServletRequest req) 
	throws ActionException, SQLException, DatabaseException {
		DealerInfoAction sai = new DealerInfoAction(this.actionInit);
		String dealerLocationId = new UUIDGenerator().getUUID();
		
		// Add Dealer
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		req.setParameter("organizationId", site.getOrganizationId());
		req.setParameter("dealerSubmitted", "true");
		req.setParameter("dealerName", req.getParameter("locationName"));
		req.setParameter("dealerId", new UUIDGenerator().getUUID());
		req.setParameter("insertAction", "true");
		req.setParameter("dealerTypeId", "5");
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.update(req);
		
		// Add Dealer Location
		req.setParameter("dealerSubmitted", "false");
		req.setParameter("locationName", req.getParameter("locationName"));
		req.setParameter("dealerLocationId", dealerLocationId);
		req.setParameter("activeFlag", "0");
		sai.updateDealerLocation(req, false);
		
		// update the ICD and pcmkr data
		updateClinicParams(req, dealerLocationId);
		
		return dealerLocationId;
	}
	
	/**
	 * Adds the extended clinic data
	 * @param req
	 */
	public void updateClinicParams(SMTServletRequest req, String id) {
		StringBuilder s = new StringBuilder();
		s.append("update dealer_location set cass_validate_flg = ?, ");
		s.append("bar_code_id = ? where dealer_location_id = ?");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setInt(1, Convert.formatInteger(req.getParameter("icd")));
			ps.setString(2, req.getParameter("pcmkr"));
			ps.setString(3, id);
			ps.executeUpdate();
		} catch (Exception e) {
			log.error("Unable to update extended info", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	public void update(SMTServletRequest req) throws ActionException {
		super.update(req);
	}
	
}
