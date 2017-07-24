package com.biomed.smarttrak.admin;

//jdk 1.8
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//base/email campaigns libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.sb.email.util.EmailCampaignBuilderUtil;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.emailcampaign.DataFilterVO;
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
	private String profileId; 
	private String uniqueSendFlg= "0"; //use String due to cross tab query in datasource
	protected enum KeyValueType {
		SECTION_KEY_TYPE("SECTION"), MESSAGE_KEY_TYPE("MESSAGE"), 
		TIME_RANGE_KEY_TYPE("TIME_RANGE");
		
		private String keyName;
		private KeyValueType(String keyName){
			this.keyName = keyName;
		}
		/*Getters*/
		public String getKeyName(){ return this.keyName; }
	}
	/*Instance ids for updates daily/weekly campaign*/
	private static final String UPDATE_DAILY_INSTANCE_ID = "smarttrakUpdatesDailyInstanceId";
	private static final String UPDATE_WEEKLY_INSTANCE_ID = "smarttrakUpdatesWeeklyInstanceId";
	
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
		profileId = StringUtil.checkVal(req.getParameter("profileId"));
		
		//if user doesn't already have a profile, create one
		if(profileId.isEmpty()) {
			uniqueSendFlg = "1";
			createUserProfile(req);
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
		log.debug("user data: " + user.getEmailAddress());
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		try {
			//save profile record
			pm.updateProfile(user, dbConn);
			
			//retrieve the user profile id
			profileId = pm.checkProfile(user, dbConn);
			
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
	protected void processEmailSend(ActionRequest req) throws ActionException{		
		String campInstId = StringUtil.checkVal(req.getParameter("campaignInstanceId"));
		List<DataFilterVO> dataFilters = new ArrayList<>();
		
		//build the emailConfig
		Map<String, Object> emailConfig = generateEmailConfig(req);
		
		//perform the email send
		EmailCampaignBuilderUtil ecbu = new EmailCampaignBuilderUtil(dbConn, attributes);
		ecbu.sendCampaignMessage(campInstId, profileId, emailConfig, dataFilters, true);
	}
	
	
	/**
	 * Sets the general information to profile config vo first, then sets the custom
	 * key name and value text based on key type passed.
	 * @param req
	 * @return
	 */
	protected Map<String, Object> generateEmailConfig(ActionRequest req){
		Map<String, Object> config = new HashMap<>();
		
		//assign the key/value for each config type
		for(KeyValueType type: KeyValueType.values()){
			assignKeyValuePair(req, type, config);
		}
		
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
	protected void assignKeyValuePair(ActionRequest req, KeyValueType type, 
			Map<String, Object> config){
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
			if(i != 0) sections.append(EmbedWidgetManager.ARRAY_DELIMIT_TOKEN);
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
		if(attributes.get(UPDATE_DAILY_INSTANCE_ID).toString().equals(campaignInstanceId)){
			config.put(type.getKeyName(), "daily");	
		}else if(attributes.get(UPDATE_WEEKLY_INSTANCE_ID).toString().equals(campaignInstanceId)){
			config.put(type.getKeyName(), "weekly");	
		}
	}
	
}
