package com.sjm.corp.mobile.collection;

import java.util.ArrayList;
import java.util.List;

import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>: MobileDataVO.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Containter for the data tool portion of the Mobile Collection portlet
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since Jul 5, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class MobileDataVO extends SBModuleVO{
	private static final long serialVersionUID = 1L;
	private String practiceName;
	private String location;
	private String additionalComments;
	private String primaryTitle;
	private String primaryEmail;
	private String primaryName;
	private String primaryPhone;
	private String altTitle;
	private String altEmail;
	private String altName;
	private String altPhone;
	private boolean moveForward;
	private boolean conferenceCall;
	private boolean wantVisit;
	private boolean newPractice;
	private boolean rebrandPractice;
	private boolean overallPatients;
	private boolean consolidatePractice;
	private boolean referrals;
	private boolean interventionalPatients;
	private boolean reviewTime;
	private List<String> templateNames;
	private String templateId;
	private String goalId;
	private boolean hcpPatients;
	private String date;
	private String officeName;
	
	public MobileDataVO(){
		setTemplateNames(new ArrayList<String>());
	}
	public String getPracticeName() {
		return practiceName;
	}
	public void setPracticeName(String practiceName) {
		this.practiceName = practiceName;
	}
	public String getAdditionalComments() {
		return additionalComments;
	}
	public void setAdditionalComments(String additionalComments) {
		this.additionalComments = additionalComments;
	}
	public String getPrimaryTitle() {
		return primaryTitle;
	}
	public void setPrimaryTitle(String primaryTitle) {
		this.primaryTitle = primaryTitle;
	}
	public String getPrimaryEmail() {
		return primaryEmail;
	}
	public void setPrimaryEmail(String primaryEmail) {
		this.primaryEmail = primaryEmail;
	}
	public String getPrimaryName() {
		return primaryName;
	}
	public void setPrimaryName(String primaryName) {
		this.primaryName = primaryName;
	}
	public String getPrimaryPhone() {
		return primaryPhone;
	}
	public void setPrimaryPhone(String primaryPhone) {
		this.primaryPhone = primaryPhone;
	}
	public String getAltTitle() {
		return altTitle;
	}
	public void setAltTitle(String altTitle) {
		this.altTitle = altTitle;
	}
	public String getAltEmail() {
		return altEmail;
	}
	public void setAltEmail(String altEmail) {
		this.altEmail = altEmail;
	}
	public String getAltName() {
		return altName;
	}
	public void setAltName(String altName) {
		this.altName = altName;
	}
	public String getAltPhone() {
		return altPhone;
	}
	public void setAltPhone(String altPhone) {
		this.altPhone = altPhone;
	}
	public boolean isMoveForward() {
		return moveForward;
	}
	public void setMoveForward(boolean moveForward) {
		this.moveForward = moveForward;
	}
	public boolean isConferenceCall() {
		return conferenceCall;
	}
	public void setConferenceCall(boolean conferenceCall) {
		this.conferenceCall = conferenceCall;
	}
	public boolean isWantVisit() {
		return wantVisit;
	}
	public void setWantVisit(boolean wantVisit) {
		this.wantVisit = wantVisit;
	}
	public boolean isNewPractice() {
		return newPractice;
	}
	public void setNewPractice(boolean newPractice) {
		this.newPractice = newPractice;
	}
	public boolean isRebrandPractice() {
		return rebrandPractice;
	}
	public void setRebrandPractice(boolean rebrandPractice) {
		this.rebrandPractice = rebrandPractice;
	}
	public boolean isConsolidatePractice() {
		return consolidatePractice;
	}
	public void setConsolidatePractice(boolean consolidatePractice) {
		this.consolidatePractice = consolidatePractice;
	}
	public boolean isOverallPatients() {
		return overallPatients;
	}
	public void setOverallPatients(boolean overallPatients) {
		this.overallPatients = overallPatients;
	}
	public boolean isReferrals() {
		return referrals;
	}
	public void setReferrals(boolean referrals) {
		this.referrals = referrals;
	}
	public boolean isInterventionalPatients() {
		return interventionalPatients;
	}
	public void setInterventionalPatients(boolean interventionalPatients) {
		this.interventionalPatients = interventionalPatients;
	}
	public String getTemplateId() {
		return templateId;
	}
	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}
	public String getGoalId() {
		return goalId;
	}
	public void setGoalId(String goalId) {
		this.goalId = goalId;
	}
	public boolean isHcpPatients() {
		return hcpPatients;
	}
	public void setHcpPatients(boolean hcpPatients) {
		this.hcpPatients = hcpPatients;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public boolean isReviewTime() {
		return reviewTime;
	}
	public void setReviewTime(boolean reviewTime) {
		this.reviewTime = reviewTime;
	}
	public List<String> getTemplateNames() {
		return templateNames;
	}
	public void setTemplateNames(List<String> templateNames) {
		this.templateNames = templateNames;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getOfficeName() {
		return officeName;
	}
	public void setOfficeName(String officeName) {
		this.officeName = officeName;
	}
}