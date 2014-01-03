package com.sjm.corp.mobile.collection;

import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>: PracticeVO.java<p/>
 * <b>Description: Object that handles the data collected from SJM related to the practice and stores it temporarily(until we put it in the db at the end)</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since June 21, 2012
 ****************************************************************************/

public class PracticeVO extends SBModuleVO{
	private static final long serialVersionUID = 1L;
	private String name;
	private String practicioner;
	private String description;
	private String location;
	private String comment;
	private String primaryContactName;
	private boolean readyToMove;
	private boolean wantingConferenceCall;
	private boolean wantingVisit;
	private boolean wantingReviewTime;
	private String primaryContactTitle;
	private String primaryContactPhone;
	private String primaryContactEmail;
	private String altContactName;
	private String altContactTitle;
	private String altContactPhone;
	private String altContactEmail;
	private String adminEmail;
	private String officeName;
	
	public PracticeVO(){
		super();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPracticioner() {
		return practicioner;
	}

	public void setPracticioner(String practicioner) {
		this.practicioner = practicioner;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public boolean isReadyToMove() {
		return readyToMove;
	}

	public void setReadyToMove(boolean readyToMove) {
		this.readyToMove = readyToMove;
	}

	public String getPrimaryContactName() {
		return primaryContactName;
	}

	public void setPrimaryContactName(String primaryContactName) {
		this.primaryContactName = primaryContactName;
	}

	public boolean isWantingVisit() {
		return wantingVisit;
	}

	public void setWantingVisit(boolean wantingVisit) {
		this.wantingVisit = wantingVisit;
	}

	public boolean isWantingConferenceCall() {
		return wantingConferenceCall;
	}

	public void setWantingConferenceCall(boolean wantingConferenceCall) {
		this.wantingConferenceCall = wantingConferenceCall;
	}

	public boolean isWantingReviewTime() {
		return wantingReviewTime;
	}

	public void setWantingReviewTime(boolean wantingReviewTime) {
		this.wantingReviewTime = wantingReviewTime;
	}

	public String getPrimaryContactTitle() {
		return primaryContactTitle;
	}

	public void setPrimaryContactTitle(String primaryContactTitle) {
		this.primaryContactTitle = primaryContactTitle;
	}

	public String getPrimaryContactPhone() {
		return primaryContactPhone;
	}

	public void setPrimaryContactPhone(String primaryContactPhone) {
		this.primaryContactPhone = primaryContactPhone;
	}

	public String getPrimaryContactEmail() {
		return primaryContactEmail;
	}

	public void setPrimaryContactEmail(String primaryContactEmail) {
		this.primaryContactEmail = primaryContactEmail;
	}

	public String getAltContactName() {
		return altContactName;
	}

	public void setAltContactName(String altContactName) {
		this.altContactName = altContactName;
	}

	public String getAltContactTitle() {
		return altContactTitle;
	}

	public void setAltContactTitle(String altContactTitle) {
		this.altContactTitle = altContactTitle;
	}

	public String getAltContactPhone() {
		return altContactPhone;
	}

	public void setAltContactPhone(String altContactPhone) {
		this.altContactPhone = altContactPhone;
	}

	public String getAltContactEmail() {
		return altContactEmail;
	}

	public void setAltContactEmail(String altContactEmail) {
		this.altContactEmail = altContactEmail;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getAdminEmail() {
		return adminEmail;
	}

	public void setAdminEmail(String adminEmail) {
		this.adminEmail = adminEmail;
	}

	public String getOfficeName() {
		return officeName;
	}

	public void setOfficeName(String officeName) {
		this.officeName = officeName;
	}
}