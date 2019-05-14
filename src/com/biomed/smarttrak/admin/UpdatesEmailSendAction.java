package com.biomed.smarttrak.admin;

import java.util.ArrayList;
// Java 8
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.action.UpdatesEditionAction;
// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.exception.DatabaseException;
// WC EmailCampaigns
import com.siliconmtn.sb.email.util.EmailCampaignBuilderUtil;
import com.siliconmtn.sb.email.vo.EmailRecipientVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
// WC
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.emailcampaign.embed.EmbedWidgetManager;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;

/****************************************************************************
 * Title: UpdatesEmailSendAction.java <p/>
 * Project: WC_Custom <p/>
 * Description: <p/>
 * Copyright: Copyright (c) 2017<p/>
 * Company: Silicon Mountain Technologies<p/>
 * @author Devon Franklin
 * @version 1.0
 * @since Apr 11, 2017
 ****************************************************************************/

public class UpdatesEmailSendAction extends SBActionAdapter {
	private String uniqueSendFlg= "0"; //use String due to cross tab query in datasource
	protected enum KeyValueType {
		SECTION_KEY_TYPE("SECTION"), 
		MESSAGE_KEY_TYPE("MESSAGE"), 
		TIME_RANGE_KEY_TYPE("timeRangeCd"),
		DATE("date");
		
		private String keyName;
		private KeyValueType(String keyName){
			this.keyName = keyName;
		}
		/*Getters*/
		public String getKeyName(){ return this.keyName; }
	}
	
	/**
	 * No-arg constructor for simple initialization
	 */
	public UpdatesEmailSendAction(){
		super();
	}
	
	/**
	 * Initializes the class with an ActionInitVO
	 * @param init
	 */
	public UpdatesEmailSendAction(ActionInitVO init){
		super(init);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException{
		log.debug("Processing email send for updates...");
		String profileId = StringUtil.checkVal(req.getParameter(UpdatesEditionAction.PROFILE_ID));

		//if user doesn't already have a profile, create one
		if(profileId.isEmpty()) {
			createUserProfile(req);

			//Updates Send Now Emails to always show all updates just like in the Scheduled Tasks.
			uniqueSendFlg = "1";
		}

		//send off "send now email"
		processEmailSend(req);
	}
	
	/**
	 * Returns a user profile id after creating one and saving to the database
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	protected void createUserProfile(ActionRequest req) throws ActionException{
		//Create a user data vo and pass to the manager
		UserDataVO user = new UserDataVO(req);
		user.setAllowCommunication(1);
		log.debug("user emailAddress: " + user.getEmailAddress());
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		try {
			//save profile record
			pm.updateProfile(user, dbConn);
			
			//retrieve the user profile id
			String profileId = pm.checkProfile(user, dbConn);

			req.setParameter(UpdatesEditionAction.PROFILE_ID, profileId);

			//add record for communication
			String orgId = StringUtil.checkVal(req.getParameter("organizationId"));
			pm.assignCommunicationFlg(orgId, profileId, user.getAllowCommunication(), dbConn);
		} catch (DatabaseException e) {
			log.error("Error attempting to add user profile: " + e);
			throw new ActionException(e);
		}
	}
	
	/**
	 * Handles the process for the email send to recipient(s)
	 * @param req
	 * @throws ActionException 
	 */
	protected void processEmailSend(ActionRequest req) {		
		String campInstId = StringUtil.checkVal(req.getParameter("campaignInstanceId"));

		//build the emailConfig
		Map<String, Object> emailParams = makeEmailParams(req);

		//perform the email send
		EmailCampaignBuilderUtil ecbu = new EmailCampaignBuilderUtil(dbConn, attributes);
		List<EmailRecipientVO> recipients = new ArrayList<>();
		recipients.add(new EmailRecipientVO((String) emailParams.get(UpdatesEditionAction.PROFILE_ID), (String)emailParams.get("emailAddress"), EmailRecipientVO.TO));
		ecbu.sendMessage(emailParams, recipients, campInstId);
	}


	/**
	 * Sets the general information to profile config vo first, then sets the custom
	 * key name and value text based on key type passed.
	 * @param req
	 * @return
	 */
	protected Map<String, Object> makeEmailParams(ActionRequest req){
		Map<String, Object> config = new HashMap<>();

		//assign the key/value for each config type
		for(KeyValueType type: KeyValueType.values()){
			assignKeyValuePair(req, type, config);
		}

		UserDataVO u = new UserDataVO();

		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		List<UserDataVO> userData = new ArrayList<>();
		
		u.setProfileId(req.getParameter(UpdatesEditionAction.PROFILE_ID));
		userData.add(u);
		try {
			pm.populateRecords(dbConn, userData);
		} catch (DatabaseException e) {
			log.error("Error Processing Code", e);
		}
		u = userData.get(0);

		config.put("firstName", u.getFirstName());
		config.put("lastName", u.getLastName());
		config.put("emailAddress", u.getEmailAddress());
		config.put("createDt", Convert.getCurrentTimestamp());
		config.put(UpdatesEditionAction.PROFILE_ID, req.getParameter(UpdatesEditionAction.PROFILE_ID));

		//add the unique send flag to config
		config.put("uniqueSendFlg", uniqueSendFlg);

		return config;
	} 
	/**
	 * Assigns the appropriate key/value pairings to email config based on key type
	 * @param req
	 * @param keyType
	 * @param config
	 * @return
	 */
	protected void assignKeyValuePair(ActionRequest req, KeyValueType type, Map<String, Object> config){
		//determine type
		switch(type){
		case SECTION_KEY_TYPE:
			setSectionIdValue(req, type, config);
			break;
		case MESSAGE_KEY_TYPE:
			config.put(type.getKeyName(), StringUtil.checkVal(req.getParameter("emailMessageText")));
			break;
		case TIME_RANGE_KEY_TYPE:
			setTimeRangeValue(req, type, config);
			break;
		case DATE:
			config.put(KeyValueType.DATE.getKeyName(), req.getParameter("endDt"));
			break;
		}
	}

	/**
	 * Helper method to section id value(s) to the passed campaign config vo
	 * @param req
	 * @param configVo
	 */
	private void setSectionIdValue(ActionRequest req, KeyValueType keyType,  Map<String, Object>emailConfig){
		String[] sectionIds = req.getParameterValues("sectionId");
		StringBuilder sections = new StringBuilder(50);
		//store section id's delimited by special delimited token string
		for (int i = 0; i< sectionIds.length; i++) {
			if(i != 0) {
				sections.append(EmbedWidgetManager.ARRAY_DELIMIT_TOKEN);
			}
			sections.append(sectionIds[i]);
		}
		emailConfig.put(keyType.getKeyName(), sections.toString());
	 }

	/**
	 * Helper method to time range value to the passed campaign config vo
	 * @param req
	 * @param type
	 * @param config
	 */
	private void setTimeRangeValue(ActionRequest req, KeyValueType type, Map<String, Object> config){
		String campaignInstanceId = StringUtil.checkVal(req.getParameter("campaignInstanceId"));
		//compare the ids. Determine time range value to assign
		if(req.hasParameter(type.getKeyName())) {
			config.put(type.getKeyName(), req.getParameter(type.getKeyName()));
		} else if(campaignInstanceId.contains("DAILY")){
			config.put(type.getKeyName(), "daily");	
		} else if(campaignInstanceId.contains("WEEKLY")){
			config.put(type.getKeyName(), "weekly");	
		}
	}
}