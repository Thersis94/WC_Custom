package com.biomed.smarttrak.admin.vo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.admin.CRMAction;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: CustomerVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO container information related to a single CRM customer
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Eric Damschroder
 * @version 1.0
 * @since Mar 18, 2017
 ****************************************************************************/

@Table(name="CUSTOMER")
public class CustomerVO {

	private UserDataVO profile;
	private String transactionId;
	private String customerId;
	private String companyName;
	private String prospectType;
	private String status;
	private String subscriptionLevel;
	private String expirationDate;
	private List<DealVO> deals;
	private List<DataFeedNoteVO> notes;
	private List<ReminderVO> reminders;
	
	public enum CustomerField {
		COMPANY("CUSTOMER_1", 7),
		PROSPECT("CUSTOMER_2", 8),
		STATUS("CUSTOMER_3", 9),
		SUBSCRIPTION("CUSTOMER_4", 10),
		EXPIRATION("CUSTOMER_5", 11);
		
		private String questionCd;
		private int mapId;
		CustomerField(String dbField, int mapId) {
			this.questionCd = dbField;
			this.mapId = mapId;
		}
		
		public String getQuestionCd() {
			return questionCd;
		}
		
		public int getMapId() {
			return mapId;
		}
		
		public static CustomerField getForDb(int mapId) {
			switch (mapId) {
			case 7: return CustomerField.COMPANY;
			case 8: return CustomerField.PROSPECT;
			case 9: return CustomerField.STATUS;
			case 10: return CustomerField.SUBSCRIPTION;
			case 11: return CustomerField.EXPIRATION;
			default: return null;
			}
		}
	}
	
	public CustomerVO() {
		deals = new ArrayList<>();
		notes = new ArrayList<>();
		reminders = new ArrayList<>();
	}
	
	public CustomerVO(ActionRequest req) {
		this();
		setData(req);
	}
	
	public void setData(ActionRequest req) {
		transactionId = req.getParameter("transactionId");
		customerId = req.getParameter("customerId");
		companyName = req.getParameter("companyName");
		prospectType = req.getParameter("prospectType");
		status = req.getParameter("status");
		subscriptionLevel = req.getParameter("subscriptionLevel");
		expirationDate = req.getParameter("expirationDate");
		setProfile(new UserDataVO(req));
	}

	public UserDataVO getProfile() {
		return profile;
	}

	public void setProfile(UserDataVO profile) {
		this.profile = profile;
	}

	@Column(name="transaction_id")
	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	
	@Column(name="customer_id", isPrimaryKey=true)
	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}
	
	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public String getProspectType() {
		return prospectType;
	}

	public void setProspectType(String prospectType) {
		this.prospectType = prospectType;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getSubscriptionLevel() {
		return subscriptionLevel;
	}

	public void setSubscriptionLevel(String subscriptionLevel) {
		this.subscriptionLevel = subscriptionLevel;
	}

	public String getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(String expirationDate) {
		this.expirationDate = expirationDate;
	}

	public List<DealVO> getDeals() {
		return deals;
	}

	public void setDeals(List<DealVO> deals) {
		this.deals = deals;
	}
	
	public void addDeal(DealVO deal) {
		deals.add(deal);
	}

	public List<DataFeedNoteVO> getNotes() {
		return notes;
	}

	public void setNotes(List<DataFeedNoteVO> notes) {
		this.notes = notes;
	}

	public void addNote(DataFeedNoteVO note) {
		notes.add(note);
	}

	public List<ReminderVO> getReminders() {
		return reminders;
	}

	public void setReminders(List<ReminderVO> reminders) {
		this.reminders = reminders;
	}
	
	public void addReminder(ReminderVO reminder) {
		reminders.add(reminder);
	}

	@Column(name="tms_customer_id", isInsertOnly=true)
	public String getTMSId(){return CRMAction.TMSID;}
	public void setTMSId(String tmsid) {
		// This method exists solely for dbproccessor. 
		//Since the set value is always empty it doesn't need to save anything
	}
	
	@Column(name="attempt_dt", isInsertOnly=true, isAutoGen=true)
	public Date getAttemptDt(){return null;}
	public void setAttemptDt(Date d) {
		// This method exists solely for dbproccessor. 
		//Since the set value is always empty it doesn't need to save anything
	}
	
	@Column(name="result_cd", isInsertOnly=true)
	public String getResultCd(){return CRMAction.RESULT_CD;}
	public void setResultCd(String resultCd) {
		// This method exists solely for dbproccessor. 
		//Since the set value is always empty it doesn't need to save anything
	}
	
	@Column(name="call_source_cd", isInsertOnly=true)
	public String getCallSourceCd(){return CRMAction.CALL_SOURCE_ID;}
	public void setCallSourceCd(String callSourceCd) {
		// This method exists solely for dbproccessor. 
		//Since the set value is always empty it doesn't need to save anything
	}
	
	@Column(name="product_cd", isInsertOnly=true)
	public String getProductCd(){return CRMAction.PRODUCT_CD;}
	public void setProductCd(String productId) {
		// This method exists solely for dbproccessor. 
		//Since the set value is always empty it doesn't need to save anything
	}
	
	@Column(name="selection_cd", isInsertOnly=true)
	public String getSelectionCd(){return CRMAction.SELECTION_CD;}
	public void setSelectionCd(String selectionCd) {
		// This method exists solely for dbproccessor. 
		//Since the set value is always empty it doesn't need to save anything
	}
	
	@Column(name="lead_type_id", isInsertOnly=true)
	public int getLeadTypeId(){return CRMAction.LEAD_TYPE_ID;}
	public void setLeadTypeId(int leadTypeId) {
		// This method exists solely for dbproccessor. 
		//Since the set value is always empty it doesn't need to save anything
	}
	
	@Column(name="profile_id", isInsertOnly=true)
	public String getProfileId() {return profile.getProfileId();}
	public void setProfileId(String profileId) {
		if (profile == null) profile= new UserDataVO();
		profile.setProfileId(profileId);
	}
	
	

	
	/**
	 * Set the field in the vo based on the supplied result set row
	 * @param fieldId
	 * @param value
	 * @param valueId
	 * @throws SQLException 
	 */
	public void setField(ResultSet rs) throws SQLException {
		CustomerField field = CustomerField.getForDb(rs.getInt("QUESTION_MAP_ID"));
		if (field == null) return;
		
		String value = rs.getString("RESPONSE_TXT");

		switch (field) {
			case COMPANY:
				companyName = StringUtil.checkVal(value);
				break;
			case PROSPECT:
				prospectType = StringUtil.checkVal(value);
				break;
			case STATUS:
				status = StringUtil.checkVal(value);
				break;
			case SUBSCRIPTION:
				subscriptionLevel = StringUtil.checkVal(value);
				break;
			case EXPIRATION:
				expirationDate = StringUtil.checkVal(value);
				break;
		}
	}

	
	/**
	 * Create a map of question id/response text items out of fields on the
	 * customer vo that need to be placed in the customer response table
	 * @param customer
	 * @return
	 */
	public Map<Integer, String> makeValueMap() {
		Map<Integer, String> values = new HashMap<>();
		values.put(CustomerField.COMPANY.getMapId(), companyName);
		values.put(CustomerField.EXPIRATION.getMapId(), expirationDate);
		values.put(CustomerField.PROSPECT.getMapId(), prospectType);
		values.put(CustomerField.STATUS.getMapId(), status);
		values.put(CustomerField.SUBSCRIPTION.getMapId(), subscriptionLevel);
		return values;
	}

}
