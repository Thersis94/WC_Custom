package com.sjm.corp.mobile.collection;

import com.sjm.corp.mobile.collection.GoalVO;
import com.sjm.corp.mobile.collection.MarketingVO;
import com.sjm.corp.mobile.collection.PracticeVO;
import com.sjm.corp.mobile.collection.TemplateVO;
import com.sjm.corp.mobile.collection.PatientsVO;
import com.sjm.corp.mobile.collection.ThemeVO;
import com.smt.sitebuilder.action.SBModuleVO;


/****************************************************************************
 * <b>Title</b>: MobileCollectionVO.java<p/>
 * <b>Description: Object that handles the data collected from SJM and stores it temporarily(until we put it in the db at the end)</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since June 21, 2012
 ****************************************************************************/

public class MobileCollectionVO extends SBModuleVO{
	private static final long serialVersionUID = 1L;
	private GoalVO goals;
	private MarketingVO marketing;
	private PracticeVO practice;
	private TemplateVO templates;
	private PatientsVO patients;
	private ThemeVO themes;
	private String[] subTitle;
	private String[] nextButtonNames;
	private boolean emailSent = false;
	private String actionId;
	private RegionVO region;
	public String actionName = "hi";
	public String actionDescription = super.actionDesc;

	public MobileCollectionVO(){
		super();
		marketing = new MarketingVO();
		goals = new GoalVO();
		practice = new PracticeVO();
		templates = new TemplateVO();
		patients = new PatientsVO();
		region = new RegionVO();
		String[] tempTitle = {"WHICH PRACTITIONER?","","OVERVIEW","GOALS","PRACTICE SNAPSHOT", "PRACTICE SNAPSHOT","OUR PORTFOLIO","PRICING","TEMPLATES","MY FAVORITES","MY SELECTIONS","NEXT STEPS","THANK YOU"};
		subTitle = tempTitle;
		String[] tempNext = {"Next","Begin","Next","Next","Next", "Next","Next","Next","Next","Next","Next","Submit","Next"};
		nextButtonNames = tempNext;

		themes = new ThemeVO();
	}
	
	public MarketingVO getMarketing(){
		return marketing;
	}
	
	public GoalVO getGoals(){
		return goals;
	}
	
	public PracticeVO getPractice(){
		return practice;
	}
	
	public PatientsVO getPatients(){
		return patients;
	}
	
	public void setGoals(GoalVO vo){
		goals = vo;
	}
	
	public void setMarketing(MarketingVO vo){
		marketing = vo;
	}
	
	public void setPractice(PracticeVO vo){
		practice = vo;
	}
	
	public void setPatients(PatientsVO vo){
		patients = vo;
	}

	public ThemeVO getThemes() {
		return themes;
	}

	public void setThemes(ThemeVO themes) {
		this.themes = themes;
	}

	public TemplateVO getTemplates() {
		return templates;
	}

	public void setTemplates(TemplateVO templates) {
		this.templates = templates;
	}

	public String[] getSubTitle() {
		return subTitle;
	}

	public void setSubTitle(String[] subTitle) {
		this.subTitle = subTitle;
	}

	public boolean isEmailSent() {
		return emailSent;
	}

	public void setEmailSent(boolean emailSent) {
		this.emailSent = emailSent;
	}
	
	public void setActionId(String actionId){
		this.actionId = actionId;
	}
	
	public String getActionId(){
		return actionId;
	}

	public RegionVO getRegion() {
		return region;
	}

	public void setRegion(RegionVO region) {
		this.region = region;
	}

	public String[] getNextButtonNames() {
		return nextButtonNames;
	}

	public void setNextButtonNames(String[] nextButtonNames) {
		this.nextButtonNames = nextButtonNames;
	}
}