package com.biomed.smarttrak.admin;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.biomed.smarttrak.admin.vo.BiomedCRMCustomerVO;
import com.depuy.datafeed.tms.TransactionManager;
import com.depuy.datafeed.tms.db.ResponseDB;
import com.depuy.datafeed.tms.modules.CustomerVO;
import com.depuy.datafeed.tms.modules.ErrorModule;
import com.depuy.datafeed.tms.modules.DataSourceVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.crm.CustomerLoadUtil;
import com.smt.crm.DealUtil;
import com.smt.crm.NoteUtil;
import com.smt.crm.ReminderUtil;
import com.smt.crm.vo.DataFeedNoteVO;
import com.smt.crm.vo.ReminderVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: CRMAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Action handling the retrieval and creation of CRM records
 * for smarttrak
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Eric Damschroder
 * @version 1.0
 * @since Mar 18, 2017
 ****************************************************************************/

public class CRMAction extends SBActionAdapter {

	public static final String CUSTOMER_ID = "customerId";
	public static final String ACTION_TARGET = "actionTarget";

	public enum ActionType {
		CUSTOMER, NOTE, REMINDER, DEAL
	}
	
	
	@Override
	public void build(ActionRequest req) throws ActionException {
		String buildAction = req.getParameter("buildAction");
		String msg = StringUtil.capitalizePhrase(buildAction) + " completed successfully.";
		try {
			ActionType type;
			if (req.hasParameter(ACTION_TARGET)) {
				type = ActionType.valueOf(req.getParameter(ACTION_TARGET));
			} else {
				type = ActionType.CUSTOMER;
			}
			if (req.hasParameter("alter")) {
				alterElement(req.getParameter(CUSTOMER_ID), req);
			} else if ("update".equals(buildAction)) {
				updateElement(type, req);
			}
		} catch(Exception e) {
			msg = StringUtil.capitalizePhrase(buildAction) + " failed to complete successfully. Please contact an administrator about this issue.";
		}
		redirectRequest(msg, buildAction, req.getParameter(CUSTOMER_ID), req);
	}

	
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String type = req.getParameter("type");
		if ("customer".equals(type)) {
			CustomerLoadUtil util = new CustomerLoadUtil((String)attributes.get(Constants.DATA_FEED_SCHEMA), dbConn, attributes);
			List<CustomerVO> customers = util.getCustomers(req.getParameter(CUSTOMER_ID));
			List<BiomedCRMCustomerVO> translatedCustomers = new ArrayList<>();
			for (CustomerVO customer: customers) {
				BiomedCRMCustomerVO trans = BiomedCRMCustomerVO.cast(customer);
				trans.buildFromResponses();
				translatedCustomers.add(trans);
			}  
			    
			putModuleData(translatedCustomers);
		} else if ("deals".equals(type)) {
			DealUtil util = new DealUtil((String)attributes.get(Constants.DATA_FEED_SCHEMA), dbConn, attributes);
			List<CustomerVO> deals = util.getDeals(req.getParameter("profileId"));
			List<BiomedCRMCustomerVO> translatedDeals = new ArrayList<>();
			for (CustomerVO deal : deals) {
				BiomedCRMCustomerVO trans = BiomedCRMCustomerVO.cast(deal);
				trans.buildFromResponses();
				translatedDeals.add(trans);
			}
			putModuleData(translatedDeals);
		} else if ("notes".equals(type)) {
			BiomedCRMCustomerVO cust = new BiomedCRMCustomerVO(req);
			addNotes(cust);
			putModuleData(cust);
		} else if ("reminders".equals(type)) {
			UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
			BiomedCRMCustomerVO cust = new BiomedCRMCustomerVO(req);
			addReminders(cust, StringUtil.checkVal(user.getProfileId()));
			putModuleData(cust);
		} else if (!req.hasParameter("add") && req.hasParameter(CUSTOMER_ID)) {
			CustomerLoadUtil util = new CustomerLoadUtil((String)attributes.get(Constants.DATA_FEED_SCHEMA), dbConn, attributes);
			BiomedCRMCustomerVO customer = BiomedCRMCustomerVO.cast(util.getCustomers(req.getParameter(CUSTOMER_ID)).get(0));
			customer.buildFromResponses();
			putModuleData(customer);
		} 
	}


	/**
	 * Build the redirect for build requests
	 * @param msg
	 * @param buildAction
	 * @param req
	 */
	protected void redirectRequest(String msg, String buildAction, String customerId, ActionRequest req) {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		// Redirect the user to the appropriate page
		StringBuilder url = new StringBuilder(128);
		url.append(page.getFullPath()).append("?actionType=crm&msg=").append(msg);

		// Only add a tab parameter if one was provided.
		if (req.hasParameter("tab")) {
			url.append("&tab=").append(req.getParameter("tab"));
		}
		//if a market is being deleted do not redirect the user to a market page
		if (!"delete".equals(buildAction) || 
				ActionType.valueOf(req.getParameter(ACTION_TARGET)) != ActionType.CUSTOMER) {
			url.append("&customerId=").append(customerId);
		}

		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}
	
	
	/**
	 * Get all reminders associated with any customer record owned by the supplied
	 * profile id and of the appropriate lead type.
	 * @param customer
	 * @param profileId
	 * @throws ActionException
	 */
	public void addReminders(BiomedCRMCustomerVO customer, String userProfileId) throws ActionException {
		ReminderUtil util = new ReminderUtil((String)attributes.get(Constants.DATA_FEED_SCHEMA), dbConn);
		util.addReminders(customer, userProfileId);
	}
	
	
	/**
	 * Update deal, reminder, note. Change the particular record.
	 * Update customer creates new customer record with current information
	 * Retrieve grabs 
	 * @param req
	 * @throws ActionException
	 */
	public void updateElement(ActionType type, ActionRequest req) throws ActionException {
		switch (type) {
			case CUSTOMER:
				saveCustomer(new BiomedCRMCustomerVO(req), true);
				break;
			case NOTE:
				saveNote(new DataFeedNoteVO(req));
				break;
			case REMINDER:
				saveReminder(req.getParameter(CUSTOMER_ID), new ReminderVO(req));
				break;
			case DEAL:
				saveDeal(req.getParameter(CUSTOMER_ID), req);
				break;
		}
	}
	
	
	/**
	 * Get all notes associated with this customer
	 * @param customer
	 * @param userId
	 */
	public void addNotes(BiomedCRMCustomerVO customer) throws ActionException {
		NoteUtil util = new NoteUtil((String)attributes.get(Constants.DATA_FEED_SCHEMA), dbConn);
		util.addNotes(customer);
	}

	
	
	/**
	 * Save the supplied customer
	 * @param customer
	 * @throws ActionException
	 */
	public void saveCustomer(BiomedCRMCustomerVO customer, boolean fullSave) throws ActionException {
		if (fullSave) saveProfileInformation(customer);
		saveCustomerInformation(customer, fullSave);
		if (fullSave) saveMapInformation(customer);
	}

	
	/**
	 * Save the profile table data for the customer
	 * @param customer
	 * @throws ActionException
	 */
	protected void saveProfileInformation(BiomedCRMCustomerVO customer) throws ActionException {
		try {
			UserDataVO profile = customer.getProfile();
			ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
			// If we don't have a profile Id check if one exists
			if (StringUtil.isEmpty(profile.getProfileId())) {
				profile.setProfileId(pm.checkProfile(profile, dbConn));
			}
			// If no profile for this address exists make one
			if (StringUtil.isEmpty(profile.getProfileId())) {
				pm.updateProfile(profile, dbConn);
			} else {
				pm.updateProfilePartially(profile.getDataMap(), profile, dbConn);
			}
		} catch (DatabaseException e) {

			throw new ActionException(e);
		}
	}

	
	/**
	 * Send off transaction save and save all customer table specific data
	 * @param customer
	 * @throws ActionException
	 */
	protected void saveCustomerInformation(BiomedCRMCustomerVO customer, boolean fullSave) throws ActionException {
		if (fullSave) {
			saveTransaction(customer);
		}

		if (customer.getLeadTypeId() == 0) customer.setLeadTypeId(BiomedCRMCustomerVO.LEAD_TYPE_ID);
		if (customer.getAttemptDate() == null) customer.setAttemptDate(new Date());
		
		DBProcessor db = new DBProcessor(dbConn, (String)attributes.get(Constants.DATA_FEED_SCHEMA));
		try {
			db.save(customer);
			if (StringUtil.isEmpty(customer.getCustomerId()))
				customer.setCustomerId(db.getGeneratedPKId());
		} catch (Exception e) {

			throw new ActionException(e);
		}
	}

	
	/**
	 * Save the transaction data
	 * @param customer
	 * @throws ActionException
	 */
	protected void saveTransaction(BiomedCRMCustomerVO customer) throws ActionException {
		TransactionManager manager = new TransactionManager();
		DataSourceVO data = new DataSourceVO();
		data.setSourceId(BiomedCRMCustomerVO.CALL_SOURCE_ID);
		data.setSourceName(BiomedCRMCustomerVO.SOURCE_NAME);
		customer.setTransactionId(manager.createTransaction(dbConn, (String)attributes.get(Constants.DATA_FEED_SCHEMA), data, 1, new Date()));
	}

	
	/**
	 * Save the customer specific values that need to go into the customer response table
	 * @param customer
	 * @throws ActionException
	 */
	protected void saveMapInformation(BiomedCRMCustomerVO customer) throws ActionException {
		customer.buildResponseList();
		ResponseDB db = new ResponseDB(BiomedCRMCustomerVO.CustomerField.getQuestionMap());
		try {
			db.store(dbConn, customer, (String)attributes.get(Constants.DATA_FEED_SCHEMA), new ArrayList<ErrorModule>());
		} catch (DatabaseException e) {

			throw new ActionException(e);
		}
	}
	
	
	/**
	 * Save the reminder supplied by the action request
	 * @param req
	 * @throws ActionException
	 */
	public void saveReminder(String customerId, ReminderVO reminder) throws ActionException {
		DBProcessor db = new DBProcessor(dbConn, (String)attributes.get(Constants.DATA_FEED_SCHEMA));
		try {
			db.save(reminder);
		} catch (InvalidDataException
				| com.siliconmtn.db.util.DatabaseException e) {

			throw new ActionException(e);
		}
	}

	
	/**
	 * Save the supplied deal to the database
	 * @param deal
	 * @throws ActionException
	 */
	public void saveDeal(String customerId, ActionRequest req) throws ActionException {
		CustomerLoadUtil util = new CustomerLoadUtil((String)attributes.get(Constants.DATA_FEED_SCHEMA), dbConn, attributes);
		BiomedCRMCustomerVO customer = BiomedCRMCustomerVO.cast(util.getCustomers(customerId).get(0));
		customer.setDealData(req, false);
		customer.buildFromResponses();
		saveCustomer(customer, true);
	}
	
	
	/**
	 * Update an existing customer response.
	 * @param req
	 * @throws ActionException
	 */
	public void alterElement(String customerId, ActionRequest req) throws ActionException {

		ActionType type;
		if (req.hasParameter(ACTION_TARGET)) {
			type = ActionType.valueOf(req.getParameter(ACTION_TARGET));
		} else {
			type = ActionType.CUSTOMER;
		}
		switch (type) {
			case DEAL: 
				updateDeal(customerId, req);
				break;
			case REMINDER:
				updateReminder(req.getParameter("reminderId"));
				break;
			default:
				break;
		}
	}
	
	
	/**
	 * Update the supplied reminder
	 * @param reminderId
	 * @throws ActionException
	 */
	protected void updateReminder(String reminderId) throws ActionException {
		ReminderUtil util = new ReminderUtil((String)attributes.get(Constants.DATA_FEED_SCHEMA), dbConn);
		util.closeReminder(reminderId);
	}


	/**
	 * Uppadte the supplied deal
	 * @param customerId
	 * @param req
	 * @throws ActionException
	 */
	public void updateDeal(String customerId, ActionRequest req) throws ActionException {
		CustomerLoadUtil util = new CustomerLoadUtil((String)attributes.get(Constants.DATA_FEED_SCHEMA), dbConn, attributes);
		BiomedCRMCustomerVO customer = BiomedCRMCustomerVO.cast(util.getCustomers(customerId).get(0));
		customer.setDealData(req, true);
		saveCustomer(customer, false);
	}

	
	/**
	 * Save the note supplied by the action request
	 * @param req
	 * @throws ActionException
	 */
	public void saveNote(DataFeedNoteVO note) throws ActionException {
		DBProcessor db = new DBProcessor(dbConn, (String)attributes.get(Constants.DATA_FEED_SCHEMA));
		try {
			db.insert(note);
		} catch (InvalidDataException
				| com.siliconmtn.db.util.DatabaseException e) {

			throw new ActionException(e);
		}
	}


}
