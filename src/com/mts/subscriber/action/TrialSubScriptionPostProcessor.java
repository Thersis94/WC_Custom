package com.mts.subscriber.action;

import java.util.ArrayList;
// JDK 1.8.x
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

// MTS Libs
import com.mts.common.MTSConstants.MTSRole;
import com.mts.subscriber.action.SubscriptionAction.SubscriptionType;
import com.mts.subscriber.data.MTSUserVO;
import com.mts.subscriber.data.SubscriptionUserVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.registration.SubmittalAction;
import com.smt.sitebuilder.action.registration.SubmittalDataVO;


/****************************************************************************
 * <b>Title</b>: TrialSubScriptionPostProcessor.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Adds the user data to the MTS_USER table and 
 * sets up a 7 day trial period
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 28, 2020
 * @updates:
 ****************************************************************************/
public class TrialSubScriptionPostProcessor extends SBActionAdapter {
	/**
	 * Collection of the publications to subscribe
	 */
	private static final Set<String> publications = new TreeSet<String>() {
		private static final long serialVersionUID = 1L; {
			add("MEDTECH-STRATEGIST");
			add("MARKET-PATHWAYS");
		}
		
	};

	/**
	 * 
	 */
	public TrialSubScriptionPostProcessor() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TrialSubScriptionPostProcessor(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * Code to call the post processing needed for the registration
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// Get the user's profile
		UserDataVO user = (UserDataVO)req.getAttribute(SubmittalAction.REGISTRATION_USER_DATA);

		// Get the form data
		List<String> pubIds = getFormPubs(req);
		String company = getFormData(req, false);
		
		// Look for an existing account and update / add
		MTSUserVO mtsUser = checkMTSUser(user);
		try {
			updateMTSUser(mtsUser, company);
			mtsUser.setSubscriptions(getExistingSubscriptions(mtsUser.getUserId()));
			
			// Update/Assign the publication permissions
			for (String pubId : pubIds) {
				assignPublication(mtsUser, pubId);
			}
			
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Unable to update MTS User", e);
			return;
		}
	}
	
	/**
	 * Generically parses the company and publication info
	 * @param req
	 * @param isPub
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String getFormData(ActionRequest req, boolean isPub) {
		for ( SubmittalDataVO data : (List<SubmittalDataVO>)req.getAttribute(SubmittalAction.REGISTRATION_EXT_DATA)) {
			if (isPub && publications.contains(data.getUserValue())) {
				return data.getUserValue();
			} else if ((! isPub) && !publications.contains(data.getUserValue())){
				return data.getUserValue();
			}
		}
		
		return "";
	}
	
	/**
	 * Gets the list of publications selected
	 * @param req
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<String> getFormPubs(ActionRequest req) {
		List<String> pubs = new ArrayList<>();
		for ( SubmittalDataVO data : (List<SubmittalDataVO>)req.getAttribute(SubmittalAction.REGISTRATION_EXT_DATA)) {
			if (publications.contains(data.getUserValue())) {
				pubs.add(data.getUserValue());
			}
		}
		
		return pubs;
	}
	
	/**
	 * Assigns the user's publication as a trial
	 * @param user
	 * @param publicationId
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void assignPublication(MTSUserVO user, String publicationId) 
	throws InvalidDataException, DatabaseException {
		// Get the subscription and make sure its not already assigned
		SubscriptionUserVO sub = user.getSubscription(publicationId);
		if (sub != null && sub.getExpirationDate() == null) return;
		
		// Calculate the expiration date
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		Date exp = Convert.formatEndDate(Convert.formatDate(new Date(), Calendar.DAY_OF_YEAR, 7));
		
		// Insert or update the existing record
		if (sub == null) { 
			sub = new SubscriptionUserVO();
			sub.setTrialFlag(1);
			sub.setUserId(user.getUserId());
			sub.setPublicationId(publicationId);
			sub.setExpirationDate(exp);
			db.save(sub);
		} else {
			sub.setExpirationDate(exp);
			sub.setTrialFlag(1);
			db.update(sub, Arrays.asList("trial_flg", "expiration_dt", "subscription_publication_id"));
		}
	}
	
	/**
	 * Updates the user record
	 * @param user
	 * @param companyName
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void updateMTSUser(MTSUserVO user, String companyName) 
	throws InvalidDataException, DatabaseException {
		
		// Initialize the DB conn
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		// If the user isn't in the system, assign the proper data and add the user
		if (StringUtil.isEmpty(user.getUserId())) {
			user.setActiveFlag(1);
			user.setCompanyName(companyName);
			user.setRoleId(MTSRole.SUBSCRIBER.getRoleId());
			user.setSubscriptionType(SubscriptionType.USER);
			db.insert(user);
		} else if (! Convert.formatBoolean(user.getActiveFlag())) {
			user.setActiveFlag(1);
			db.update(user, Arrays.asList("active_flg", "user_id"));
		}
	}
	
	/**
	 * Retrieves a collection of existing subscriptions
	 * @param userId
	 * @return
	 */
	public List<SubscriptionUserVO> getExistingSubscriptions(String userId) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from ").append(getCustomSchema());
		sql.append("mts_subscription_publication_xr where user_id = ? ");
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), Arrays.asList(userId), new SubscriptionUserVO());
	}
	
	/**
	 * Check for an existing user id
	 * @param profileId
	 * @return
	 */
	public MTSUserVO checkMTSUser(UserDataVO user) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from ").append(getCustomSchema());
		sql.append("mts_user where profile_id = ?");
		
		DBProcessor db = new DBProcessor(getDBConnection());
		List<MTSUserVO> users = db.executeSelect(sql.toString(), Arrays.asList(user.getProfileId()), new MTSUserVO());
		return (users.isEmpty()) ? new MTSUserVO(user) : users.get(0);
	}

}
