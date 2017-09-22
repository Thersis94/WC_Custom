package com.depuysynthesinst;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.depuysynthesinst.assg.MyAssignmentsAction;
import com.depuysynthesinst.assg.MyResidentsAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.registration.SubmittalAction;
import com.smt.sitebuilder.action.registration.RegistrationFacadeAction;
import com.smt.sitebuilder.action.registration.SubmittalDataVO;
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


	public RegistrationAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public RegistrationAction(ActionInitVO arg0) {
		super(arg0);
	}


	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	@Override
	public void update(ActionRequest req) throws ActionException {
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
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		mod.setActionId(actionInit.getActionId());
		mod.setActionGroupId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_2));
		setAttribute(Constants.MODULE_DATA, mod);

		ActionInterface reg = new RegistrationFacadeAction(actionInit);
		reg.setDBConnection(dbConn);
		reg.setAttributes(getAttributes());
		reg.retrieve(req);

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
	}
	

	/**
	 * extend Registration's build method with added logic for:
	 * determining if we need to display pages 3 & 4 of registration
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		//these 'hooks' are here because they live on the Registration/"My Profile" page in the UI.
		if (req.hasParameter("revokeDirector") || req.hasParameter("approveDirector")) {
			req.setParameter("reqType", "manageProctor");
			ActionInterface act = new MyResidentsAction();
			act.setAttributes(getAttributes());
			act.setDBConnection(dbConn);
			act.build(req);
			return;
		} else if (req.hasParameter("checkDSRPusername")) {
			//called via ajax on Modal #3, fail if the username typed does not match the value on record
			UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
			String pswd = getLegacyPassword(user != null ? user.getEmailAddress() : "");
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

		ActionInterface reg = new RegistrationFacadeAction(actionInit);
		reg.setDBConnection(dbConn);
		reg.setAttributes(getAttributes());
		reg.build(req);
	}


	/**
	 * writes back to the register_data table to update a couple fields on the user's registration
	 * @param user
	 */
	protected void captureLMSResponses(ActionRequest req, UserDataVO user, String[] formFields) {
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
					vo.setUserValue(dsiUser.isEligible() ? "yes" : "no");
					break;
				case "DSI_VERIFIED": //RegField.DSI_VERIFIED - can't use an object here
					vo.setUserValue(dsiUser.isVerified() ? "yes" : "no");
					break;
				case "c0a80241b71d27b038342fcb3ab567a0": //RegField for specialty
					vo.setUserValue(dsiUser.getSpecialty());
					break;
				default:
			}
			regData.add(vo);
		}
		
		SubmittalAction sa = new SubmittalAction();
		sa.setAttributes(getAttributes());
		sa.setDBConnection(dbConn);
		sa.updateRegisterData(req, user, registerSubmittalId, regData);
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
}
