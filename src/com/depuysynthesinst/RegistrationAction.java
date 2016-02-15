package com.depuysynthesinst;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;

import com.depuysynthesinst.DSIUserDataVO.RegField;
import com.depuysynthesinst.assg.MyAssignmentsAction;
import com.depuysynthesinst.assg.MyResidentsAction;
import com.depuysynthesinst.lms.LMSWSClient;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.registration.SubmittalAction;
import com.smt.sitebuilder.action.registration.RegistrationFacadeAction;
import com.smt.sitebuilder.action.registration.SubmittalDataVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>: RegistrationAction.java<p/>
 * <b>Description: Wraps WC's core Registration portlet with additional business rules and logic.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jun 8, 2015
 ****************************************************************************/
public class RegistrationAction extends SimpleActionAdapter {

	private Set<String> specialProfs = null;

	public RegistrationAction() {
		super();
		populateSpecialProfs();
	}

	/**
	 * @param arg0
	 */
	public RegistrationAction(ActionInitVO arg0) {
		super(arg0);
		populateSpecialProfs();
	}

	private void populateSpecialProfs() {
		specialProfs = new HashSet<>();
		specialProfs.add("RESIDENT");
		specialProfs.add("FELLOW");
		specialProfs.add("CHIEF");
		specialProfs.add("DIRECTOR");
	}

	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	
	public void update(SMTServletRequest req) throws ActionException {
		String[] regAction = req.getParameter("attrib1Text").split("~");
		//need to capture the actionId as well as the actionGroupId for submitting/retrieving Registration on the front end
		if (regAction != null && regAction.length == 2) {
			req.setParameter("attrib1Text", regAction[0]); //actionId
			req.setParameter("attrib2Text", regAction[1]); //actionGroupId
		}
		super.update(req);
	}
	

	/**
	 * Invokes WC Registration's retrieve method.  No further logic is needed.
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		mod.setActionId(actionInit.getActionId());
		mod.setActionGroupId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_2));
		setAttribute(Constants.MODULE_DATA, mod);

		SMTActionInterface reg = new RegistrationFacadeAction(actionInit);
		reg.setDBConnection(dbConn);
		reg.setAttributes(getAttributes());
		reg.retrieve(req);
		reg = null;
		
		//on the summary page, load the name(s) of this students Residency Director(s)
		DSIUserDataVO dsiUser = new DSIUserDataVO((UserDataVO)req.getSession().getAttribute(Constants.USER_DATA));
		DSIRoleMgr dsiRoleMgr = new DSIRoleMgr();
		if (!req.hasParameter("pg") && dsiRoleMgr.isAssgStudent(dsiUser)) {
			MyAssignmentsAction maa = new MyAssignmentsAction();
			maa.setAttributes(getAttributes());
			maa.setDBConnection(dbConn);
			Map<String,UserDataVO> resDirs = maa.loadResidencyDirectors(dsiUser.getProfileId(), false); //false=approved RDs only
			dsiUser.setResDirs(resDirs);
			req.getSession().setAttribute(Constants.USER_DATA, dsiUser.getUserDataVO());
		}
		
		//if page=3 and this is a new registration, probe to see if the user is eligible for migration
		//all users getting page 3 are TTLMS eligible; decision is made in the View of which modal comes next.
		if ("3".equals(req.getParameter("pg")) && req.hasParameter("newReg"))
			checkHoldingUser(req);
		
		//if there are no parameters on the request, this is the summary page/view.
		//Reload the user's transcript each time so they always see their latest transcript
		if (dsiUser.getTtLmsId() != null  && Convert.formatInteger(dsiUser.getTtLmsId()) > 0 && (req.hasParameter("coursesAjax") || StringUtil.checkVal(req.getQueryString()).length() == 0)) {
			log.debug("loading transcript from LMS");
			try {
				LMSWSClient lms = new LMSWSClient((String)getAttribute(LMSWSClient.CFG_SECURITY_KEY));
				dsiUser.setMyCourses(lms.getUserCourseList(dsiUser.getDsiId()));
			} catch (ActionException ae) {
				log.warn("could not load user course list", ae);
			}
		}
		
	}
	

	/**
	 * extend Registration's build method with added logic for:
	 * determining if we need to display pages 3 & 4 of registration
	 */
	public void build(SMTServletRequest req) throws ActionException {
		//these 'hooks' are here because they live on the Registration/"My Profile" page in the UI.
		if (req.hasParameter("revokeDirector") || req.hasParameter("approveDirector")) {
			req.setParameter("reqType", "manageProctor");
			SMTActionInterface act = new MyResidentsAction();
			act.setAttributes(getAttributes());
			act.setDBConnection(dbConn);
			act.build(req);
			return;
		} else if (req.hasParameter("checkDSRPusername")) {
			//called via ajax on Modal #3, fail if the username typed does not match the value on record
			UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
			String pswd = getLegacyPassword((user != null ? user.getEmailAddress() : ""));
			String userNm = req.getParameter("checkDSRPusername");
			if (!pswd.equalsIgnoreCase(userNm)) {
				ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
				mod.setErrorCondition(Boolean.TRUE);
			}
			return;
		} else if (req.hasParameter("sendVerifiedEmailFromAdmintool")) {
			//called from the admintool via an ajax call when the users account gets Verified
			//send the Verified email out and return
			RegistrationPostProcessor rpp = new RegistrationPostProcessor();
			rpp.setDBConnection(dbConn);
			rpp.setAttributes(getAttributes());
			rpp.update(req);
			return;
		}

		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		mod.setActionId(actionInit.getActionId());
		mod.setActionGroupId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_2));
		setAttribute(Constants.MODULE_DATA, mod);
		HttpSession ses = (HttpSession) req.getSession();

		//set a parameter to tell Registration to NOT dump the userVO if the user completes without logging in, we need it
		boolean unloadSessionIfNoRole = false;
		if (req.hasParameter("newReg")) {
			unloadSessionIfNoRole = true;
			req.setAttribute("dontUnloadSession", "true");
		}
		SMTActionInterface reg = new RegistrationFacadeAction(actionInit);
		reg.setDBConnection(dbConn);
		reg.setAttributes(getAttributes());
		reg.build(req);
		reg = null;

		DSIUserDataVO user = new DSIUserDataVO((UserDataVO)ses.getAttribute(Constants.USER_DATA));
		DSIRoleMgr dsiRoleMgr = new DSIRoleMgr();

		//if they're not using LMS, we're done:
		if (!dsiRoleMgr.isLMSAuthorized(user)) return;
		
		//if this is an edit, call the LMS after each modal saves 1,2 and 3.  
		//Otherwise only when modal #3 is submitted (which is page=4 on the request).
		if ("4".equals(req.getParameter("page")) && req.hasParameter("newReg")) {
			user.setCountryCode(req.getParameter("reg_||" + DSIUserDataVO.RegField.DSI_COUNTRY.toString()));
			boolean migrated = migrateUser(req, user);
			
			//if not migrated, call save (create||update)
			if (!migrated) saveUser(user);
			
			String[] regFields = new String[]{ RegField.DSI_TTLMS_ID.toString(), 
															 RegField.DSI_SYNTHES_ID.toString(), 
															 RegField.DSI_PROG_ELIGIBLE.toString(), 
															 RegField.DSI_VERIFIED.toString() };
			captureLMSResponses(req, user, regFields);
			
		} else if (!req.hasParameter("newReg")) {
			String[] fieldList = new String[]{ RegField.DSI_TTLMS_ID.toString() };
			
			//if the user is submitting page 2, and has changed either their profession or specialty, reset Verified=no
			log.debug(req.getParameter("oldProfession") + "=" + user.getProfession());
			log.debug(req.getParameter("oldSpecialty") + "=" + user.getSpecialty());
			if ("3".equals(req.getParameter("page"))) {
				if (!StringUtil.checkVal(req.getParameter("oldProfession")).equals(user.getProfession()) || 
						!StringUtil.checkVal(req.getParameter("oldSpecialty")).equals(user.getSpecialty())) {
					user.setVerified(false);
					fieldList = new String[]{ RegField.DSI_TTLMS_ID.toString(), RegField.DSI_VERIFIED.toString() };
					log.debug("verified flag set false");
				}
			}

			//existing user updating their data
			if (user.getTtLmsId() != null && Convert.formatInteger(user.getTtLmsId()) > 0) {
				saveUser(user);
				captureLMSResponses(req, user, fieldList);
			}
		}
		
		boolean isFinalPage = StringUtil.checkVal(req.getParameter("finalPage")).equals("1");
		log.debug("isFinalPage=" + isFinalPage + " " + req.getParameter("finalPage"));
		if (isFinalPage && unloadSessionIfNoRole && ses.getAttribute(Constants.ROLE_DATA) == null) {
			ses.removeAttribute(Constants.USER_DATA);
		} else if (isFinalPage) {
			log.debug("removing incomplete flag from UserDataVO");
			user.addAttribute("incomplete",false); //this comes from rereg scenarios
			ses.setAttribute(Constants.USER_DATA, user.getUserDataVO());
		} else {
			ses.setAttribute(Constants.USER_DATA, user.getUserDataVO());
		}
	}
	
	
	/**
	 * writes back to the register_data table to update a couple fields on the user's registration
	 * @param user
	 */
	protected void captureLMSResponses(SMTServletRequest req, UserDataVO user, String[] formFields) {
		String registerSubmittalId = StringUtil.checkVal(req.getAttribute("registerSubmittalId"));
		req.setParameter("formFields", formFields, Boolean.TRUE);
		DSIUserDataVO dsiUser = new DSIUserDataVO(user);
		
		//build a list of values to insert based on the ones we're going to delete
		List<SubmittalDataVO> regData = new ArrayList<>();
		for (String field : formFields) {
			SubmittalDataVO vo = new SubmittalDataVO(null); //encryption key=null, we don't need it.
			vo.setRegisterFieldId(field);
			switch (field) {
				case "DSI_TTLMS_ID": //RegField.DSI_TTLMS_ID - can't use an object here
					vo.setUserValue(dsiUser.getTtLmsId());
					break;
				case "DSI_SYNTHES_ID": //RegField.DSI_SYNTHES_ID - can't use an object here
					vo.setUserValue(dsiUser.getSynthesId());
					break;
				case "DSI_PROG_ELIGIBLE": //RegField.DSI_PROG_ELIGIBLE - can't use an object here
					vo.setUserValue((dsiUser.isEligible() ? "yes" : "no"));
					break;
				case "DSI_VERIFIED": //RegField.DSI_VERIFIED - can't use an object here
					vo.setUserValue((dsiUser.isVerified() ? "yes" : "no"));
					break;
			}
			regData.add(vo);
		}
		
		SubmittalAction sa = new SubmittalAction();
		sa.setAttributes(getAttributes());
		sa.setDBConnection(dbConn);
		sa.updateRegisterData(req, user, registerSubmittalId, regData);
		sa = null;
	}
	
	
	/**
	 * Tests to see if the user has a migratable LMS account, and prepares for that 
	 * transaction on the next modal save.
	 * @param req
	 */
	private void checkHoldingUser(SMTServletRequest req) {
		DSIUserDataVO user = new DSIUserDataVO((UserDataVO)req.getSession().getAttribute(Constants.USER_DATA));
		String pswd = getLegacyPassword(user.getEmailAddress());
		log.debug("holdingPwd=" + pswd);
		
		if (pswd.length() > 0) {
			//verify the user has an account on the LMS to migrate
			boolean inHolding = false;
			try {
				LMSWSClient lms = new LMSWSClient((String)getAttribute(LMSWSClient.CFG_SECURITY_KEY));
				Map<Object,Object> data = lms.getUserHoldingIDByEmail(user.getEmailAddress());
				user.setAttributesFromMap(data);
				inHolding = (Convert.formatInteger(user.getSynthesId()) > 0);
				log.debug(user.getEmailAddress() + " inHolding? " + inHolding);
			} catch (ActionException ae) {
				log.error("could not query LMS for user holding", ae);
			}
			
			if (inHolding) {
				req.setAttribute("isLegacyUser", "true");
				//save the responses onto UserDataVO for when the form is submitted
				req.getSession().setAttribute(Constants.USER_DATA, user.getUserDataVO());
			} else {
				//email bradley, the LMS lied to us
				String msg = user.getEmailAddress() + " (Legacy TTLMSID=" + user.getTtLmsId() + ") is on the holding list but their account was not In Holding on the LMS.  " +
				"As a result they were not given the option to migrate, and a new LMS account was likely created.";
				emailBradley("DSI duplicate account created - user not in holding", msg);
			}
		}
	}
	
	
	/**
	 * migrate the LMS user out of the legacy LMS
	 * returns false if the user could not be migrated - meaning must be inserted as new.
	 * returns true if the user was migrated & updated - meaning transaction complete.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private boolean migrateUser(SMTServletRequest req, DSIUserDataVO user) {
		log.debug("checking migrate");
		
		//do not migrate the user if they didn't consent to migration
		if ("1".equals(req.getParameter("reg_||DSI_DSRP_TRANSFER_AUTH"))) { //submitted a Yes
			//verify they gave us the correct legacy password
			String pswd = getLegacyPassword(user.getEmailAddress());
			if (pswd.length() > 0 && pswd.equalsIgnoreCase(req.getParameter("dsrpPassword")))  {
				//migrate the user
				LMSWSClient lms = new LMSWSClient((String)getAttribute(LMSWSClient.CFG_SECURITY_KEY));
				try {
					double d = lms.migrateUser(user);
					if (d < 1) throw new ActionException("could not migrate: " + d);
					user.setTtLmsId(d);
					log.debug("user migrated, TTLMSID=" + d);
					return true; //if we're not successful here, the catch-all below is designed to strip any old TTLMSID before returning, so a new account can be created
				} catch (ActionException e) {
					log.warn("could not migrate user", e);
					//email bradley
					String msg = user.getEmailAddress() + " (Legacy TTLMSID=" + user.getTtLmsId() + ") wanted to migrate their LMS account but something went wrong.  A new account was likely created and should be considered duplicate.  Please use the SMT/EP Reconcile process to correct their account.";
					StringWriter errors = new StringWriter();
					e.printStackTrace(new PrintWriter(errors));
					msg += "\r\n" + errors.toString();
					emailBradley("DSI migration failed - needs reconciled", msg);
				}
			} else {
				String msg = user.getEmailAddress() + " (Legacy TTLMSID=" + user.getTtLmsId() + ") did not submit the correct DSRP password, so the migration they desired did not occur.  Use the SMT/EP reconcile process to fix this.";
				emailBradley("DSI duplicate account created - needs reconciled", msg);
			}
		} else if ("1".equals(req.getParameter("clickedYes"))) { //attempted a Yes but settled for No
			log.debug("user did not provide the correct DSRP username, but wanted to " + user.getEmailAddress());
			//email bradley
			String msg = user.getEmailAddress() + " (Legacy TTLMSID=" + user.getTtLmsId() + ") seemingly tried to migrate their LMS account but could not provide the correct username, or changed their mind and did not want to migrate.  A new account was likely created and should be considered duplicate.  Please use the SMT/EP Reconcile process to correct their account.  You should confirm with the individual before doing so.  Their OLD account should be deleted if they do not desire migration.";
			emailBradley("DSI duplicate account created - needs reconciled - user changed their mind", msg);
		} else if (req.hasParameter("reg_||DSI_DSRP_TRANSFER_AUTH")) { //never clicked Yes, make sure the form field was actually presented though
			log.debug("user chose not to migrate: " + user.getEmailAddress() + " " + user.getTtLmsId());
			//email bradley
			String msg = user.getEmailAddress() + " (Legacy TTLMSID=" + user.getTtLmsId() + ") DID NOT want to migrate their LMS account.  Please delete their old account from the LMS.";
			emailBradley("DSI duplicate account created - no transfer requested", msg);
		}
		
		//remove their 'old' TTLMSID, so a new account will be created for this user
		user.setTtLmsId(0);
		return false;
	}
	
	
	/**
	 * transparently adds or updates the LMS user after performing a check to 
	 * see if they already have an account.
	 * Puts the user's TTLMSID onto their VO once returned from the LMS.
	 * @param user
	 * @throws ActionException 
	 */
	protected void saveUser(DSIUserDataVO user) throws ActionException {
		log.debug("creating/updating user");
		LMSWSClient lms = new LMSWSClient((String)getAttribute(LMSWSClient.CFG_SECURITY_KEY));
		Map<Object, Object> data = null;
		
		//check to see if the user has an active account if we don't already know
		if (user.getTtLmsId() == null || Convert.formatInteger(user.getTtLmsId()) <= 0) {
			try {
				data = lms.getUserActiveIDByEmail(user.getEmailAddress());
				user.setAttributesFromMap(data);
				log.debug("activeId=" + data.get("TTLMSID"));
			} catch (ActionException ae) {
				log.error("could not get userActiveId from LMS", ae);
			}
		}
		log.debug("TTLMSID=" + user.getTtLmsId());
		
		double d;
		if (Convert.formatInteger(user.getTtLmsId()) > 0) {
			//call update
			d= lms.updateUser(user);
			log.debug("LMS user updated: " + d);
		} else {
			//call create
			d = lms.createUser(user);
			//save the newly created TTLMSID to their UserDataVO
			log.debug("LMS user created: " + d);
			user.setTtLmsId(Convert.formatInteger("" + d).toString());
		}
	}

	
	/**
	 * calls legacy table to see if we have a user to migrate.
	 * Used first to see if a record exists (prior to modal 3), and then again to verify their password matches (modal 3 submission). 
	 * @param email
	 * @return String; password - never null
	 */
	private String getLegacyPassword(String email) {
		StringBuilder sql = new StringBuilder(100);
		sql.append("select password_txt from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_INST_MIGRATE_USR where email_address_txt=?");
		log.debug(sql + " " + email);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, email);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) return StringUtil.checkVal(rs.getString(1));
			
		} catch (SQLException sqle) {
			log.error("could not lookup legacy user record", sqle);
		}
		
		return "";
	}
	
	
	/**
	 * this is an emergency patch put in place at the launch of DSI Phase 2 to resolve LMS issues
	 * @param subject
	 * @param msg
	 */
	private void emailBradley(String subject, String body) {
		try {
			EmailMessageVO msg = new EmailMessageVO();
			msg.setFrom("no-reply@depuysynthesinstitute.com");
			msg.addRecipient("MagellanDSI2@gmail.com");
			msg.setSubject(subject);
			msg.setTextBody(body);
			new MessageSender(getAttributes(), dbConn).sendMessage(msg);
		} catch (Exception e) {
			log.error("could not email bradley", e);
		}
	}
}
