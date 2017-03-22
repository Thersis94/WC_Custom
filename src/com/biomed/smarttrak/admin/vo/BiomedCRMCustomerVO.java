package com.biomed.smarttrak.admin.vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.depuy.datafeed.tms.modules.CustomerVO;
import com.depuy.datafeed.tms.modules.ProfileVO;
import com.depuy.datafeed.tms.modules.ResponseVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: BiomedCRMCustomerVO.java
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
public class BiomedCRMCustomerVO extends CustomerVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8434113546305260110L;
	private String companyName;
	private String prospectType;
	private String status;
	private String subscriptionLevel;
	private String expirationDate;
	
	public enum CustomerField {
		COMPANY("CUSTOMER_1", "7"),
		PROSPECT("CUSTOMER_2", "8"),
		STATUS("CUSTOMER_3", "9"),
		SUBSCRIPTION("CUSTOMER_4", "10"),
		EXPIRATION("CUSTOMER_5", "11");
		
		private String questionCd;
		private String mapId;
		CustomerField(String dbField, String mapId) {
			this.questionCd = dbField;
			this.mapId = mapId;
		}
		
		public String getQuestionCd() {
			return questionCd;
		}
		
		public String getMapId() {
			return mapId;
		}
		
		public static CustomerField getForDb(String mapId) {
			switch (mapId) {
			case "7": return CustomerField.COMPANY;
			case "8": return CustomerField.PROSPECT;
			case "9": return CustomerField.STATUS;
			case "10": return CustomerField.SUBSCRIPTION;
			case "11": return CustomerField.EXPIRATION;
			default: return null;
			}
		}
	}
	
	// Predefined values for the required fields in the customer table
	public static final Integer LEAD_TYPE_ID = 20;
	public static final String RESULT_CD = "CRM_SUBMISSION";
	public static final int CALL_SOURCE_ID = 1;
	public static final String PRODUCT_CD = "SMARTTRAK";
	public static final String SELECTION_CD = "WEBSITE";
	public static final String TMSID = "";
	public static final String SOURCE_ID = "1";
	public static final String SOURCE_NAME = "Biomed Smarttrak CRM";
	
	public BiomedCRMCustomerVO() {
		super();
		profile = new ProfileVO();
	}
	
	public BiomedCRMCustomerVO(ActionRequest req) {
		this();
		setData(req);
	}
	
	public void setData(ActionRequest req) {
		setDealData(req, false);
		setTransactionId(req.getParameter("transactionId"));
		setCustomerId(req.getParameter("customerId"));
		companyName = req.getParameter("companyName");
		prospectType = req.getParameter("prospectType");
		status = req.getParameter("status");
		subscriptionLevel = req.getParameter("subscriptionLevel");
		expirationDate = req.getParameter("expirationDate");
		UserDataVO user =(UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		if (user != null) setPhysicianId(user.getProfileId());
		ProfileVO profile = new ProfileVO();
		profile.setFirstName(req.getParameter("firstName"));
		profile.setLastName(req.getParameter("lastName"));
		profile.setEmailAddress(req.getParameter("emailAddress"));
		profile.setProfileId(req.getParameter("profileId"));
		PhoneVO p = new PhoneVO(req.getParameter("mainPhone"));
		p.setPhoneType("HOME");
		profile.addPhone(p);
		setProfile(profile);
		setLeadTypeId(LEAD_TYPE_ID);
	}
	
	
	/**
	 * Only set the deal specific parameters
	 * @param req
	 */
	public void setDealData(ActionRequest req, boolean update) {
		if (!update) {
			setAttemptDate(Convert.formatDate(req.getParameter("estCloseDate")));
			setRepComments(req.getParameter("dealName"));
		}
		setAcquisitionValue(Convert.formatFloat(req.getParameter("amount")));
		setLeadTypeId(Convert.formatInteger(req.getParameter("stage")));
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

	@Override
	@Column(name="tms_customer_id", isInsertOnly=true)
	public String getTmsCustomerId(){return TMSID;}
	
	@Override
	@Column(name="result_cd", isInsertOnly=true)
	public String getResultCode(){return RESULT_CD;}
	
	@Override
	@Column(name="call_source_cd", isInsertOnly=true)
	public String getCallSourceCd(){return StringUtil.checkVal(CALL_SOURCE_ID);}
	
	@Override
	@Column(name="product_cd", isInsertOnly=true)
	public String getProductCode(){return PRODUCT_CD;}
	
	@Override
	@Column(name="selection_cd", isInsertOnly=true)
	public String getSelectionCode(){return SELECTION_CD;}
	
	

	
	/**
	 * Set the field in the vo based on the supplied result set row
	 * @param fieldId
	 * @param value
	 * @param valueId
	 * @throws SQLException 
	 */
	public void setField(ResponseVO resp) {
		CustomerField field = CustomerField.getForDb(resp.getQuestionId());
		if (field == null) return;

		switch (field) {
			case COMPANY:
				companyName = StringUtil.checkVal(resp.getResponseText());
				break;
			case PROSPECT:
				prospectType = StringUtil.checkVal(resp.getResponseText());
				break;
			case STATUS:
				status = StringUtil.checkVal(resp.getResponseText());
				break;
			case SUBSCRIPTION:
				subscriptionLevel = StringUtil.checkVal(resp.getResponseText());
				break;
			case EXPIRATION:
				expirationDate = StringUtil.checkVal(resp.getResponseText());
				break;
		}
	}
	
	
	/**
	 * Build out a list of responseVOs that contain the customer
	 * response values for this type of customer
	 * @return
	 */
	public void buildResponseList() {
		List<ResponseVO> responses = new ArrayList<>();
		ResponseVO resp = new ResponseVO();
		resp.setQuestionId(CustomerField.COMPANY.getMapId());
		resp.setResponseText(companyName);
		responses.add(resp);
		resp = new ResponseVO();
		resp.setQuestionId(CustomerField.EXPIRATION.getMapId());
		resp.setResponseText(expirationDate);
		responses.add(resp);
		resp = new ResponseVO();
		resp.setQuestionId(CustomerField.PROSPECT.getMapId());
		resp.setResponseText(prospectType);
		responses.add(resp);
		resp = new ResponseVO();
		resp.setQuestionId(CustomerField.STATUS.getMapId());
		resp.setResponseText(status);
		responses.add(resp);
		resp = new ResponseVO();
		resp.setQuestionId(CustomerField.SUBSCRIPTION.getMapId());
		resp.setResponseText(subscriptionLevel);
		responses.add(resp);
		setResponses(responses);
	}

	/**
	 * Populate fields from response vos
	 */
	public void buildFromResponses() {
		if (getResponses() != null) {
			for (ResponseVO resp : getResponses()) {
				setField(resp);
			}
		}
	}

	/**
	 * Cast a customerVo to a 
	 * @param customer
	 * @return
	 */
	public static BiomedCRMCustomerVO cast(CustomerVO customer) {
		BiomedCRMCustomerVO newCustomer = new BiomedCRMCustomerVO();

		newCustomer.setTransactionId(customer.getTransactionId());
		newCustomer.setCustomerId(customer.getCustomerId());
		newCustomer.setAcquisitionValue(customer.getAcquisitionValue());
		newCustomer.setAttemptDate(customer.getAttemptDate());
		newCustomer.setLeadTypeId(customer.getLeadTypeId());
		newCustomer.setRepComments(customer.getRepComments());
		newCustomer.setProfile(customer.getProfile());
		newCustomer.setCreateDt(customer.getCreateDt());
		newCustomer.setPhysician(customer.getPhysician());
		newCustomer.setResponses(customer.getResponses());
		newCustomer.setReminders(customer.getReminders());
		newCustomer.setNotes(customer.getNotes());
		
		return newCustomer;
	}

	public static Map<String, Integer> buildQuestionMap() {
		Map<String, Integer> map = new HashMap<>();
		for (CustomerField c : CustomerField.values()) {
			map.put(c.getMapId(), Convert.formatInteger(c.getMapId()));
		}
		return map;
	}
}
