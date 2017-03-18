package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.biomed.smarttrak.admin.vo.CustomerVO;
import com.biomed.smarttrak.admin.vo.DataFeedNoteVO;
import com.biomed.smarttrak.admin.vo.DealVO;
import com.biomed.smarttrak.admin.vo.ReminderVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
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

	// Predefined fields for the question maps associated with each
	// area of the CRM tool
	public static final String REMINDER_QUESTION_MAP_ID = "REMINDER_%";
	public static final String CUSTOMER_QUESTION_MAP_ID = "CUSTOMER_%";
	public static final String DEAL_QUESTION_MAP_ID = "DEAL_%";
	
	// Predefined values for the required fields in the customer table
	public static final int LEAD_TYPE_ID = 20;
	public static final String RESULT_CD = "CRM_SUBMISSION";
	public static final String CALL_SOURCE_ID = "1";
	public static final String PRODUCT_CD = "BIOMED_CRM";
	public static final String SELECTION_CD = "BIOMED_CRM";
	public static final String TMSID = "";
	public static final int SOURCE_ID = 1;
	
	public enum ActionType {
		CUSTOMER, NOTE, REMINDER, DEAL
	}
	
	@Override
	public void build(ActionRequest req) throws ActionException {
		String buildAction = req.getParameter("buildAction");
		String msg = StringUtil.capitalizePhrase(buildAction) + " completed successfully.";
		try {
			if (req.hasParameter("alter")) {
				alterElement(req);
			} else if ("update".equals(buildAction)) {
				updateElement(req);
			}
		} catch(Exception e) {
			throw new ActionException(e);
		}
		redirectRequest(msg, buildAction, req.getParameter("customerId"), req);
	}
	
	/**
	 * Update deal, reminder, note. Change the particular record.
	 * Update customer creates new customer record with current information
	 * Retrieve grabs 
	 * @param req
	 * @throws ActionException
	 */
	protected void updateElement(ActionRequest req) throws ActionException {
		ActionType type;
		if (req.hasParameter("actionTarget")) {
			type = ActionType.valueOf(req.getParameter("actionTarget"));
		} else {
			type = ActionType.CUSTOMER;
		}
		
		switch (type) {
		case CUSTOMER:
			saveCustomer(new CustomerVO(req), req);
			break;
		case NOTE:
			saveNote(req);
			break;
		case REMINDER:
			saveReminder(req);
			break;
		case DEAL:
			saveDeal(req);
			break;
		}
	}
	
	/**
	 * Update an existing customer response.
	 * @param req
	 * @throws ActionException
	 */
	protected void alterElement(ActionRequest req) throws ActionException {
		ActionType type;
		if (req.hasParameter("actionTarget")) {
			type = ActionType.valueOf(req.getParameter("actionTarget"));
		} else {
			type = ActionType.CUSTOMER;
		}
		
		switch (type) {
		case REMINDER:
			saveResponse(req.getParameter("responseId"), req.getParameter("value"));
			break;
		case DEAL:
			saveResponse(req.getParameter("amountId"), req.getParameter("amount"));
			saveResponse(req.getParameter("stageId"), req.getParameter("stage"));
			break;
		default:  // Customers edits are handled through regular updates
				// and notes are not able to be altered.
			break;
		}
	}
	
	
	/**
	 * Make a change to an existing customer response
	 * @param id
	 * @param value
	 * @throws ActionException
	 */
	protected void saveResponse (String id, String value) throws ActionException {
		StringBuilder sql = new StringBuilder(175);
		sql.append("UPDATE ").append(attributes.get(Constants.DATA_FEED_SCHEMA));
		sql.append("CUSTOMER_RESPONSE SET RESPONSE_TXT = ? ");
		sql.append("WHERE CUSTOMER_RESPONSE_ID = ? ");
		log.debug(sql+"|"+value+"|"+id);
		try (PreparedStatement ps = dbConn.prepareCall(sql.toString())) {
			ps.setString(1, value);
			ps.setInt(2, Convert.formatInteger(id));
			
			ps.executeUpdate();
			log.debug("Excecuted update");
		} catch (SQLException e) {
			throw new ActionException(e);
		} 
	}

	
	
	/**
	 * Save the supplied customer
	 * @param customer
	 * @throws ActionException
	 */
	protected void saveCustomer(CustomerVO customer, ActionRequest req) throws ActionException {
		saveProfileInformation(customer);
		saveCustomerInformation(customer, req);
		saveMapInformation(customer);
	}

	
	/**
	 * Save the profile table data for the customer
	 * @param customer
	 * @throws ActionException
	 */
	protected void saveProfileInformation(CustomerVO customer) throws ActionException {
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
				log.debug(profile.getProfileId());
				log.debug(customer.getProfile().getProfileId());
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
	protected void saveCustomerInformation(CustomerVO customer, ActionRequest req) throws ActionException {
		saveTransaction(customer);
		customer.setCustomerId(new UUIDGenerator().getUUID());
		req.setParameter("customerId", customer.getCustomerId());
		
		DBProcessor db = new DBProcessor(dbConn, (String)attributes.get(Constants.DATA_FEED_SCHEMA));
		try {
			db.insert(customer);
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}

	
	/**
	 * Save the transaction data
	 * @param customer
	 * @throws ActionException
	 */
	protected void saveTransaction(CustomerVO customer) throws ActionException {
		StringBuilder sql = new StringBuilder(125);
		sql.append("INSERT INTO ").append(attributes.get(Constants.DATA_FEED_SCHEMA));
		sql.append("TRANSACTIONS(TRANSACTION_ID, SOURCE_ID,TRANSACTION_DT, CREATE_DT) VALUES(?,?,?,?)");
		
		customer.setTransactionId(new UUIDGenerator().getUUID());
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, customer.getTransactionId());
			ps.setInt(2, SOURCE_ID);
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}

	
	/**
	 * Save the customer specific values that need to go into the customer response table
	 * @param customer
	 * @throws ActionException
	 */
	protected void saveMapInformation(CustomerVO customer) throws ActionException {
		StringBuilder sql = new StringBuilder(200);
		sql.append("INSERT INTO ").append(attributes.get(Constants.DATA_FEED_SCHEMA));
		sql.append("CUSTOMER_RESPONSE (QUESTION_MAP_ID, CUSTOMER_ID, RESPONSE_TXT, CREATE_DT)");
		sql.append("VALUES(?,?,?,?)");
		
		try (PreparedStatement ps =dbConn.prepareStatement(sql.toString())) {
			for (Entry<Integer, String> entry : customer.makeValueMap().entrySet()) {
				ps.setInt(1, entry.getKey());
				ps.setString(2, customer.getCustomerId());
				ps.setString(3, entry.getValue());
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				
				ps.addBatch();
			}
			
			ps.executeBatch();
			
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}

	
	/**
	 * Save the note supplied by the action request
	 * @param req
	 * @throws ActionException
	 */
	protected void saveNote(ActionRequest req) throws ActionException {
		DataFeedNoteVO note = new DataFeedNoteVO(req);
		DBProcessor db = new DBProcessor(dbConn, (String)attributes.get(Constants.DATA_FEED_SCHEMA));
		try {
			db.insert(note);
		} catch (InvalidDataException
				| com.siliconmtn.db.util.DatabaseException e) {
			throw new ActionException(e);
		}
		
	}
	
	/**
	 * Save the reminder supplied by the action request
	 * @param req
	 * @throws ActionException
	 */
	protected void saveReminder(ActionRequest req) throws ActionException {
		getCustomerInfo(req.getParameter("customerId"), false);
		ModuleVO mod = (ModuleVO) this.getAttribute(Constants.MODULE_DATA);
		CustomerVO customer = (CustomerVO)mod.getActionData();
		saveCustomer(customer, req);
		ReminderVO reminder = new ReminderVO(req);
		
		StringBuilder sql = new StringBuilder(200);
		sql.append("INSERT INTO ").append(attributes.get(Constants.DATA_FEED_SCHEMA));
		sql.append("CUSTOMER_RESPONSE (QUESTION_MAP_ID, CUSTOMER_ID, RESPONSE_TXT, CREATE_DT)");
		sql.append("VALUES(?,?,?,?)");
		
		try (PreparedStatement ps =dbConn.prepareStatement(sql.toString())) {
			for (Entry<Integer, String> entry : reminder.makeValueMap().entrySet()) {
				ps.setInt(1, entry.getKey());
				ps.setString(2, customer.getCustomerId());
				ps.setString(3, entry.getValue());
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				
				ps.addBatch();
			}
			
			ps.executeBatch();
			
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}

	
	/**
	 * Save the supplied deal to the database
	 * @param deal
	 * @throws ActionException
	 */
	protected void saveDeal(ActionRequest req) throws ActionException {
		getCustomerInfo(req.getParameter("customerId"), false);
		ModuleVO mod = (ModuleVO) this.getAttribute(Constants.MODULE_DATA);
		CustomerVO customer = (CustomerVO)mod.getActionData();
		saveCustomer(customer, req);
		DealVO deal = new DealVO(req);
		
		StringBuilder sql = new StringBuilder(200);
		sql.append("INSERT INTO ").append(attributes.get(Constants.DATA_FEED_SCHEMA));
		sql.append("CUSTOMER_RESPONSE (QUESTION_MAP_ID, CUSTOMER_ID, RESPONSE_TXT, CREATE_DT)");
		sql.append("VALUES(?,?,?,?)");
		
		try (PreparedStatement ps =dbConn.prepareStatement(sql.toString())) {
			for (Entry<Integer, String> entry : deal.makeValueMap().entrySet()) {
				ps.setInt(1, entry.getKey());
				ps.setString(2, customer.getCustomerId());
				ps.setString(3, entry.getValue());
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				
				ps.addBatch();
			}
			
			ps.executeBatch();
			
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}

	
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if ("customer".equals(req.getParameter("type"))) {
			getCustomerInfo(null, true);
		} else if ("deals".equals(req.getParameter("type"))) {
			CustomerVO cust = new CustomerVO(req);
			addDeals(cust);
			putModuleData(cust);
		} else if ("notes".equals(req.getParameter("type"))) {
			CustomerVO cust = new CustomerVO(req);
			addNotes(cust);
			putModuleData(cust);
		} else if ("reminders".equals(req.getParameter("type"))) {
			UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
			CustomerVO cust = new CustomerVO(req);
			addReminders(cust, StringUtil.checkVal(user.getProfileId()));
			putModuleData(cust);
		} else if (!req.hasParameter("add") && req.hasParameter("customerId")) {
			getCustomerInfo(req.getParameter("customerId"), true);
		} 
	}
	

	/**
	 * Get all deals made with the supplied customer as associated via profile id.
	 * @param customer
	 * @param profileId
	 * @throws ActionException
	 */
	protected void addDeals(CustomerVO customer) throws ActionException {
		String sql = buildDealSQL(customer.getProfileId());
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			if (!StringUtil.isEmpty(customer.getProfileId())) ps.setString(1, customer.getProfileId());
			
			ResultSet rs = ps.executeQuery();
			DealVO deal = new DealVO();
			String currentSubmission = "";
			while(rs.next()) {
				if (!currentSubmission.equals(rs.getString("CUSTOMER_ID"))) {
					addDeal(deal, customer);
					deal = new DealVO();
					deal.setCompanyName(rs.getString("COMPANY_NM"));
					deal.setContact(new UserDataVO(rs));
					currentSubmission = rs.getString("CUSTOMER_ID");
				}

				if (!StringUtil.isEmpty(rs.getString("OWNER_ID"))) {
					UserDataVO owner = new UserDataVO();
					owner.setFirstName(rs.getString("OWNER_FIRST_NM"));
					owner.setLastName(rs.getString("OWNER_LAST_NM"));
					owner.setProfileId(rs.getString("OWNER_ID"));
					deal.setOwner(owner);
				} else {
					deal.setField(rs);
				}
			}
			addDeal(deal, customer);
			
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}

	
	/**
	 * Decode encrypted fields that exist on the deal vo
	 * @param deal
	 * @throws ActionException
	 */
	protected void decodeFields(DealVO deal) throws ActionException {
		UserDataVO owner = deal.getOwner();
		UserDataVO contact = deal.getContact();

		StringEncrypter se;
		try {
			se = new StringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
			if (owner != null) {
				owner.setFirstName(se.decrypt(owner.getFirstName()));
				owner.setLastName(se.decrypt(owner.getLastName()));
			}
			if (contact != null) {
				contact.setFirstName(se.decrypt(contact.getFirstName()));
				contact.setLastName(se.decrypt(contact.getLastName()));
			}
		} catch (EncryptionException e) {
			throw new ActionException(e);
		}
	}

	
	/**
	 * Check to see if the reminder has been assigned any data. If it is missing
	 * the required name field it is empty and can be skipped.
	 * Non-empty reminders are added to the customer.
	 * @param reminder
	 * @param customer
	 * @throws ActionException 
	 */
	protected void addDeal(DealVO deal, CustomerVO customer) throws ActionException {
		if (deal.getDealName() != null) {
			decodeFields(deal);
			customer.addDeal(deal);
		}
	}
	
	
	/**
	 * Build the sql query to get all deals for the 
	 * @return
	 */
	protected String buildDealSQL(String profileId) {
		StringBuilder sql = new StringBuilder(1000);
		String datafeedDb = (String) attributes.get(Constants.DATA_FEED_SCHEMA);
		sql.append("SELECT cr.*, qm.QUESTION_MAP_ID, owner.FIRST_NM as OWNER_FIRST_NM, ");
		sql.append("owner.LAST_NM as OWNER_LAST_NM, owner.PROFILE_ID as OWNER_ID, qm.QUESTION_CD, ");
		sql.append("customer.FIRST_NM, customer.LAST_NM, company.RESPONSE_TXT as COMPANY_NM ");
		sql.append("FROM ").append(datafeedDb).append("CUSTOMER_RESPONSE cr ");
		sql.append("LEFT JOIN ").append(datafeedDb).append("CUSTOMER c ");
		sql.append("ON c.CUSTOMER_ID = cr.CUSTOMER_ID ");
		sql.append("LEFT JOIN ").append(datafeedDb).append("CUSTOMER_RESPONSE company ");
		sql.append("ON c.CUSTOMER_ID = company.CUSTOMER_ID ");
		sql.append("LEFT JOIN ").append(datafeedDb).append("QUESTION_MAP qm ");
		sql.append("ON qm.QUESTION_MAP_ID = cr.QUESTION_MAP_ID ");
		sql.append("LEFT JOIN PROFILE owner on owner.PROFILE_ID = cr.RESPONSE_TXT ");
		sql.append("LEFT JOIN PROFILE customer on customer.PROFILE_ID = c.PROFILE_ID ");
		sql.append("WHERE qm.QUESTION_CD like '").append(DEAL_QUESTION_MAP_ID).append("' ");
		sql.append("and c.LEAD_TYPE_ID = ").append(LEAD_TYPE_ID).append(" ");
		sql.append("and company.QUESTION_MAP_ID = ").append(CustomerVO.CustomerField.COMPANY.getMapId()).append(" ");
		if (!StringUtil.isEmpty(profileId)) sql.append("and c.PROFILE_ID = ? ");
		sql.append("ORDER BY c.CUSTOMER_ID desc, qm.QUESTION_CD");
		log.debug(sql+"|"+profileId);
		return sql.toString();
	}
	
	
	/**
	 * Get information related to customers.
	 * @param customerId
	 * @param profileId
	 * @throws ActionException
	 */
	protected void getCustomerInfo(String customerId, boolean fullLoad) throws ActionException {
		String sql = buildCustomerSql(customerId);
		CustomerVO customer = null;
		List<CustomerVO> customers = new ArrayList<>();
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			if (customerId != null) ps.setString(1, customerId);
			
			ResultSet rs = ps.executeQuery();

			String currentCustomer = "";
			DBProcessor db = new DBProcessor(dbConn, (String) attributes.get(Constants.DATA_FEED_SCHEMA));
			
			while(rs.next()) {
				// Repeated customers indicate historical records that we don't need to show
				if (currentCustomer.equals(rs.getString("PROFILE_ID"))) continue;
				currentCustomer = rs.getString("PROFILE_ID");
				customer = new CustomerVO();
				db.executePopulate(customer, rs);
				customer.setProfile(new UserDataVO(rs));
				customer.setCompanyName(rs.getString("RESPONSE_TXT"));
				decodeFields(customer.getProfile(), rs.getString("PHONE_NUMBER_TXT"));
				customers.add(customer);
			}
			
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		
		// If a customer id was passed this is a detail lookup and 
		// all supplemental customer information is needed.
		if (customerId != null && customer != null) {
			populateCustomer(customer, fullLoad);
		} else {
			putModuleData(customers);
		}
	}
	
	
	/**
	 * Decode fields on the customer vo
	 * @param profile
	 * @param phoneNumber
	 * @throws ActionException
	 */
	protected void decodeFields(UserDataVO profile, String phoneNumber) throws ActionException {
		try {
			StringEncrypter se = new StringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
			profile.setFirstName(se.decrypt(profile.getFirstName()));
			profile.setLastName(se.decrypt(profile.getLastName()));
			profile.setEmailAddress(se.decrypt(profile.getEmailAddress()));
			profile.setMainPhone(se.decrypt(phoneNumber));
		} catch (EncryptionException | IllegalArgumentException e) {
			throw new ActionException(e);
		}
		
	}

	
	/**
	 * Create the sql for retrieving customers from the database
	 * @param customerId
	 * @return
	 */
	protected String buildCustomerSql(String customerId) {
		StringBuilder sql = new StringBuilder(700);
		String datafeedDb = (String) attributes.get(Constants.DATA_FEED_SCHEMA);
		sql.append("SELECT * FROM ").append(datafeedDb).append("CUSTOMER c ");
		sql.append("LEFT JOIN PROFILE p on p.PROFILE_ID = c.PROFILE_ID ");
		sql.append("LEFT JOIN PHONE_NUMBER pn on pn.PROFILE_ID = p.PROFILE_ID AND pn.PHONE_TYPE_CD = 'HOME' ");
		sql.append("LEFT JOIN ").append(datafeedDb).append("CUSTOMER_RESPONSE cr ");
		sql.append("ON cr.CUSTOMER_ID = c.CUSTOMER_ID ");
		sql.append("WHERE c.LEAD_TYPE_ID = ").append(LEAD_TYPE_ID).append(" ");
		sql.append("and cr.question_map_id = ").append(CustomerVO.CustomerField.COMPANY.getMapId()).append(" ");
		if (customerId != null) sql.append("AND c.CUSTOMER_ID = ? ");
		sql.append("ORDER BY c.PROFILE_ID, c.ATTEMPT_DT DESC ");
		log.debug(sql);
		return sql.toString();
	}

	
	/**
	 * Get all supplemental information for the provided customer
	 * @param customer
	 * @param customerId
	 * @param profileId
	 * @throws ActionException
	 */
	protected void populateCustomer(CustomerVO customer, boolean fullLoad) throws ActionException {
		addCustomerInfo(customer);
		putModuleData(customer);
	}

	
	/**
	 * Get customer information from the question map and add it to the customer
	 * @param customer
	 * @throws ActionException
	 */
	protected void addCustomerInfo(CustomerVO customer) throws ActionException {
		StringBuilder sql = new StringBuilder(350);
		String datafeedDb = (String) attributes.get(Constants.DATA_FEED_SCHEMA);
		sql.append("SELECT cr.*, qm.QUESTION_MAP_ID FROM ").append(datafeedDb).append("CUSTOMER_RESPONSE cr ");
		sql.append("LEFT JOIN ").append(datafeedDb).append("CUSTOMER c ");
		sql.append("ON c.CUSTOMER_ID = cr.CUSTOMER_ID ");
		sql.append("LEFT JOIN ").append(datafeedDb).append("QUESTION_MAP qm ");
		sql.append("ON qm.QUESTION_MAP_ID = cr.QUESTION_MAP_ID ");
		sql.append("WHERE c.CUSTOMER_ID = ? and qm.QUESTION_CD like '").append(CUSTOMER_QUESTION_MAP_ID);
		sql.append("' and c.LEAD_TYPE_ID = ").append(LEAD_TYPE_ID).append(" ORDER BY cr.CREATE_DT desc, qm.QUESTION_CD ");
		log.debug(sql+"|"+customer.getCustomerId());
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, customer.getCustomerId());
			
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				customer.setField(rs);
			}
			
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}
	
	
	/**
	 * Get all notes associated with this customer
	 * @param customer
	 * @param userId
	 */
	protected void addNotes(CustomerVO customer) throws ActionException {
		StringBuilder sql = new StringBuilder(325);
		String datafeedDb = (String) attributes.get(Constants.DATA_FEED_SCHEMA);
		sql.append("SELECT cn.*, p.* FROM ").append(datafeedDb).append("CUSTOMER_NOTE cn ");
		sql.append("LEFT JOIN ").append(datafeedDb).append("CUSTOMER c ");
		sql.append("ON c.CUSTOMER_ID = cn.CUSTOMER_ID ");
		sql.append("LEFT JOIN PROFILE p on p.PROFILE_ID = cn.PROFILE_ID ");
		sql.append("WHERE c.PROFILE_ID = ? and c.LEAD_TYPE_ID = ");
		sql.append(LEAD_TYPE_ID).append(" ");

		DBProcessor db = new DBProcessor(dbConn, datafeedDb);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, customer.getProfileId());
			
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				DataFeedNoteVO note = new DataFeedNoteVO();
				db.executePopulate(note, rs);
				note.setAuthor(new UserDataVO(rs));
				decodeFields(note);
				customer.addNote(note);
			}
			
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		
		
		List<Object> params = new ArrayList<>();
		params.add(customer.getProfileId());
		
		List<Object> results = db.executeSelect(sql.toString(), params, new DataFeedNoteVO());
		
		for (Object o : results) 
			customer.addNote((DataFeedNoteVO)o);
	}
	
	
	/**
	 * Decode fields on the note vo
	 * @param note
	 * @throws ActionException
	 */
	protected void decodeFields(DataFeedNoteVO note) throws ActionException {

		UserDataVO author = note.getAuthor();

		StringEncrypter se;
		try {
			se = new StringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
			if (author != null) {
				author.setFirstName(se.decrypt(author.getFirstName()));
				author.setLastName(se.decrypt(author.getLastName()));
			}
		} catch (EncryptionException e) {
			throw new ActionException(e);
		}
	}

	/**
	 * Get all reminders associated with any customer record owned by the supplied
	 * profile id and of the appropriate lead type.
	 * @param customer
	 * @param profileId
	 * @throws ActionException
	 */
	protected void addReminders(CustomerVO customer, String userProfileId) throws ActionException {
		StringBuilder sql = new StringBuilder(350);
		String datafeedDb = (String) attributes.get(Constants.DATA_FEED_SCHEMA);
		sql.append("SELECT cr.*, qm.QUESTION_MAP_ID FROM ").append(datafeedDb).append("CUSTOMER_RESPONSE cr ");
		sql.append("LEFT JOIN ").append(datafeedDb).append("CUSTOMER c ");
		sql.append("ON c.CUSTOMER_ID = cr.CUSTOMER_ID ");
		sql.append("LEFT JOIN ").append(datafeedDb).append("QUESTION_MAP qm ");
		sql.append("ON qm.QUESTION_MAP_ID = cr.QUESTION_MAP_ID ");
		sql.append("WHERE c.PROFILE_ID = ? and qm.QUESTION_CD like '").append(REMINDER_QUESTION_MAP_ID);
		sql.append("' and c.LEAD_TYPE_ID = ").append(LEAD_TYPE_ID).append(" ORDER BY c.CUSTOMER_ID desc, qm.QUESTION_CD ");
		log.debug(sql+"|"+customer.getProfileId());
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, customer.getProfileId());
			
			ResultSet rs = ps.executeQuery();
			ReminderVO reminder = new ReminderVO();
			String currentSubmission = "";
			while(rs.next()) {
				if (!currentSubmission.equals(rs.getString("CUSTOMER_ID"))) {
					addReminder(reminder, customer, userProfileId);
					reminder = new ReminderVO();
					currentSubmission = rs.getString("CUSTOMER_ID");
				}
				reminder.setField(rs);
			}
			addReminder(reminder, customer, userProfileId);
			
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}

	
	/**
	 * Check to see if the reminder has been assigned any data. If it is missing
	 * the required name field it is empty and can be skipped.
	 * Non-empty reminders are added to the customer.
	 * @param reminder
	 * @param customer
	 */
	protected void addReminder(ReminderVO reminder, CustomerVO customer, String userProfileId) {
		if (userProfileId.equals(reminder.getReminderAuthor().getProfileId()) &&
				reminder.getReminderName() != null)
			customer.addReminder(reminder);
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
		url.append(page.getFullPath()).append("?actionType=crm&").append("msg=").append(msg);

		// Only add a tab parameter if one was provided.
		if (req.hasParameter("tab")) {
			url.append("&tab=").append(req.getParameter("tab"));
		}
		//if a market is being deleted do not redirect the user to a market page
		if (!"delete".equals(buildAction) || 
				ActionType.valueOf(req.getParameter("actionTarget")) != ActionType.CUSTOMER) {
			url.append("&customerId=").append(customerId);
		}

		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}

}
