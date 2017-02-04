/**
 *
 */
package com.depuysynthes.huddle;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>: SiteSurveyVO.java
 * <p/>
 * <b>Project</b>: WebCrescendo
 * <p/>
 * <b>Description: </b> VO That manages Site Survey Data.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Jan 26, 2016
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class SiteSurveyVO extends SBModuleVO {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private String siteSurveyId;
	private String profileId;
	private String surveyId;
	private boolean isSubmitted;
	private boolean isRequired;

	/**
	 * 
	 */
	public SiteSurveyVO() {
	}

	public SiteSurveyVO(ResultSet rs) {
		setData(rs);
	}

	public SiteSurveyVO(ActionRequest req) {
		setData(req);
	}

	/**
	 * Save data off a SMTServletRequest
	 * @param req
	 */
	public void setData(ActionRequest req) {
		siteSurveyId = StringUtil.checkVal(req.getAttribute(SBActionAdapter.SB_ACTION_ID));
		if(siteSurveyId.length() == 0) {
			siteSurveyId = req.getParameter("amid");
		}
		if(siteSurveyId.length() == 0) {
			siteSurveyId = req.getParameter(SBActionAdapter.SB_ACTION_ID);
		}
		surveyId = req.getParameter("surveyId");
		setActionName(req.getParameter("actionName"));
		setActionDesc(req.getParameter("actionDesc"));
		setActionId((String)req.getAttribute(SBActionAdapter.SB_ACTION_ID));
	}

	/**
	 * Save data off a ResultSet
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		siteSurveyId = db.getStringVal("SITE_SURVEY_ID", rs);
		profileId = db.getStringVal("PROFILE_ID", rs);
		surveyId = db.getStringVal("SURVEY_ID", rs);
		isSubmitted = StringUtil.checkVal(profileId).length() > 0;
		isRequired = db.getBoolVal("REQUIRED_FLG", rs);
		setActionId(db.getStringVal("ACTION_ID", rs));
		setActionGroupId(db.getStringVal("ACTION_GROUP_ID", rs));
		setActionName(db.getStringVal("ACTION_NM", rs));
		setActionDesc(db.getStringVal("ACTION_DESC", rs));
		setPendingSyncFlag(db.getIntVal("PENDING_SYNC_FLG", rs));
	}

	//Getters
	public String getSiteSurveyId() {return siteSurveyId;}
	public String getProfileId() {return profileId;}
	public String getSurveyId() {return surveyId;}
	public boolean isSubmitted() {return isSubmitted;}
	public boolean isRequired() {return isRequired;}

	//Setters
	public void setSiteSurveyId(String siteSurveyId) {this.siteSurveyId = siteSurveyId;}
	public void setProfileId(String profileId) {this.profileId = profileId;}
	public void setSurveyId(String surveyId) {this.surveyId = surveyId;}
	public void setSubmitted(boolean isSubmitted) {this.isSubmitted = isSubmitted;}
	public void setRequired(boolean isRequired) {this.isRequired = isRequired;}
}