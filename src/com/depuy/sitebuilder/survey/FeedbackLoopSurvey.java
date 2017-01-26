package com.depuy.sitebuilder.survey;

import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.survey.SurveyFacadeAction;
import com.smt.sitebuilder.action.survey.SurveyResponseAction;
import com.smt.sitebuilder.action.survey.SurveyResultsContainer;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;


/*****************************************************************************
 <p><b>Title</b>: FeedbackLoopSurvey.java</p>
 <p>Simple Survey wrapper that looks-up profileId based on emailAddress passed on the request.</p>
 <p>Copyright: Copyright (c) 2000 - 2008 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Oct 24, 2008
 Code Updates
 ***************************************************************************/

public class FeedbackLoopSurvey extends SimpleActionAdapter {
	
	public void build(ActionRequest req) throws ActionException {
		log.debug("starting FeedbackLoopSurvey build");
		String emailAddress = StringUtil.checkVal(req.getParameter("SURVEY_c0a8022ddd3b8ecaaea94f9cc6a8888b")); //from KNEE survey
		if (emailAddress.length() == 0) 
			emailAddress = StringUtil.checkVal(req.getParameter("SURVEY_c0a8022ddd9a76df48b6257332da5547")); //from HIP survey
		
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		
		UserDataVO user = new UserDataVO();
		user.setEmailAddress(emailAddress);
		try {
			user.setProfileId(pm.checkProfile(user, dbConn));
		} catch (DatabaseException de) {
			log.warn("Could not find profileId for email " + emailAddress, de);
		} finally {
			req.getSession().setAttribute(Constants.USER_DATA, user);
		}
		
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		
		//process the survey as usual now that we've added UserDataVO to the session
		ActionInterface survey = new SurveyFacadeAction(actionInit);
		survey.setAttributes(attributes);
		survey.setDBConnection(dbConn);
		survey.build(req);
	}
	
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void retrieve(ActionRequest req) throws ActionException {
		String wrapperId = actionInit.getActionId();
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));

		//process the survey as usual
		ActionInterface survey = new SurveyFacadeAction(actionInit);
		survey.setAttributes(attributes);
		survey.setDBConnection(dbConn);
		survey.retrieve(req);
				
		if (Convert.formatBoolean(req.getParameter("surveySubmitted"))) {
			try {
				Map<String, SurveyResultsContainer> surveys = (Map)req.getSession().getAttribute(SurveyResponseAction.SURVEY_CHART_DATA);
				surveys.put(wrapperId, surveys.get(actionInit.getActionId()));
				req.getSession().setAttribute(SurveyResponseAction.SURVEY_CHART_DATA, surveys);
			} catch (NullPointerException npe) {
				//this is just someone trying to skip the survey, ignore
			}
		}
		
		mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		mod.setActionId(wrapperId);
		attributes.put(Constants.MODULE_DATA, mod);
	}

}
