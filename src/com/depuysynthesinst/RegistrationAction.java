package com.depuysynthesinst;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.depuysynthesinst.lms.LMSWSClient;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.registration.RegistrationFacadeAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

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
		
		//if page=3 and this is a new registration, probe to see if the user is eligible for migration
		//all users getting page 3 are TTLMS eligible; decision is made in the View of which modal comes next.
		if ("3".equals(req.getParameter("pg")) && req.hasParameter("newReg"))
			checkHoldingUser(req);
		
	}
	

	/**
	 * extend Registration's build method with added logic for:
	 * determining if we need to display pages 3 & 4 of registration
	 */
	public void build(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		mod.setActionId(actionInit.getActionId());
		mod.setActionGroupId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_2));
		setAttribute(Constants.MODULE_DATA, mod);
		
		//TODO
		if (req.hasParameter("revokeDirector")) {

			return;
		}

		SMTActionInterface reg = new RegistrationFacadeAction(actionInit);
		reg.setDBConnection(dbConn);
		reg.setAttributes(getAttributes());
		reg.build(req);
		reg = null;		

		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);

		//if they're not using My Assignments, we're done:
		if (!DSIRoleMgr.isAssgUser(user)) return;
		
		//if this is an edit, call the LMS after each modal saves 1,2 and 3.  
		//Otherwise only when modal #3 is submitted.
		if ("4".equals(req.getParameter("page")) && req.hasParameter("newReg")) {
			boolean migrated = migrateUser(req);
			
			//if not migrated, call save (create||update)
			if (!migrated) saveUser(user);
			
		} else if (!req.hasParameter("newReg")) {
			//existing user updating their data
			saveUser(user);
		}
		
	}
	
	
	/**
	 * Tests to see if the user has a migratable LMS account, and prepares for that 
	 * transaction on the next modal save.
	 * @param req
	 */
	private void checkHoldingUser(SMTServletRequest req) {
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		String pswd = getLegacyPassword(user.getEmailAddress());
		
		if (pswd.length() > 0) {
			//verify the user has an account on the LMS to migrate
			boolean inHolding = false;
			try {
				LMSWSClient lms = new LMSWSClient((String)getAttribute(LMSWSClient.CFG_SECURITY_KEY));
				Map<Object,Object> data = lms.getUserHoldingIDByEmail(user.getEmailAddress());
				DSIUserDataVO.setAttributesFromMap(user, data);
				inHolding = (data.containsKey("SYNTHESID") && Convert.formatInteger((String)data.get("SYNTHESID")) > 0);
				//save the responses onto UserDataVO for when the form is submitted
				if (inHolding) {
					for (Object o : data.values()) user.addAttribute(o.toString(), o);
					req.getSession().setAttribute(Constants.USER_DATA, user);
				}
				
			} catch (ActionException ae) {
				log.error("could not query LMS for user holding", ae);
			}
			
			if (inHolding) req.setAttribute("isLegacyUser", "true");
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
	private boolean migrateUser(SMTServletRequest req) {
		log.debug("checking migrate");
		//do not migrate the user if they didn't consent to migration
		if (!"1".equals(req.getParameter("dsrpAuthorized"))) return false;
		
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		String pswd = getLegacyPassword(user.getEmailAddress());
		
		//verify their typed password matches what's on record in the DB
		if (pswd.length() == 0 || !pswd.equalsIgnoreCase(req.getParameter("dsrpPassword"))) 
			return false;
		
		//migrate the user
		LMSWSClient lms = new LMSWSClient((String)getAttribute(LMSWSClient.CFG_SECURITY_KEY));
		try {
			double d = lms.migrateUser(new DSIUserDataVO(user));
			DSIUserDataVO.setTTLMSID(user, d);
			log.debug("user migrated, TTLMSID=" + d);
		} catch (ActionException e) {
			log.error("could not migrate user", e);
			return false;
		}
		
		return true;
	}
	
	
	/**
	 * transparently adds or updates the LMS user after performing a check to 
	 * see if they already have an account.
	 * Puts the user's TTLMSID onto their VO once returned from the LMS.
	 * @param user
	 * @throws ActionException 
	 */
	private void saveUser(UserDataVO user) throws ActionException {
		log.debug("creating/updaing user");
		LMSWSClient lms = new LMSWSClient((String)getAttribute(LMSWSClient.CFG_SECURITY_KEY));
		Map<Object, Object> data = null;
		
		//check to see if the user has an active account
		try {
			data = lms.getUserActiveIDByEmail(user.getEmailAddress());
			DSIUserDataVO.setAttributesFromMap(user, data);
			log.debug("activeId=" + data.get("TTLMSID"));
		} catch (ActionException ae) {
			log.error("could not get userActiveId from LMS", ae);
		}
		
		DSIUserDataVO dsiUser = new DSIUserDataVO(user);
		double d;
		if (dsiUser.getTtLmsId() != null) {
			//call update
			d= lms.updateUser(dsiUser);
			log.debug("LMS user updated: " + d);
		} else {
			//call create
			d = lms.createUser(dsiUser);
			//save the newly created TTLMSID to their UserDataVO
			log.debug("LMS user created: " + d);
		}
		dsiUser.setTtLmsId(Integer.valueOf(Double.valueOf(d).intValue()).toString());
		
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
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, email);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) return StringUtil.checkVal(rs.getString(1));
			
		} catch (SQLException sqle) {
			log.error("could not lookup legacy user record", sqle);
		}
		
		return "";
	}
}
