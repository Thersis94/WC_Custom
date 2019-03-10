package com.biomed.smarttrak.vo;

import java.io.Serializable;
//Java 7
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.gis.Location;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.user.HumanNameIntfc;

/*****************************************************************************
 <p><b>Title</b>: AccountVO.java</p>
 <p><b>Description: </b>VO representing a Smarttrak Account database record.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Feb 10, 2017
 <b>Changes:</b> 
 ***************************************************************************/
@Table(name="BIOMEDGPS_ACCOUNT")
public class AccountVO implements HumanNameIntfc, Serializable {
	private static final long serialVersionUID = 6748640274663992918L;
	private String accountId;
	private String companyId;
	private String companyName;
	private String parentCompanyTxt;
	private String companyUrl;
	private String corpPhoneTxt;
	private String accountName;
	private String typeId;
	private int classificationId;
	private String ownerProfileId;
	private String coownerProfileId;
	private String ownerEmailAddr;
	private Location location;
	private String statusNo;
	private Date startDate;
	private Date expirationDate;
	private Date createDate;
	private Date updateDate;
	private String firstName;
	private String lastName;
	private String leadFirstName;
	private String leadLastName;
	private String leadEmail;

	private String title;
	private int fdAuthFlg;
	private int gaAuthFlg;
	private int mktAuthFlg;
	private int seatsNo;

	/*
	 * Account Type enum - not to be confused with status, which is Active or Inactive only.  (e.g. Inactive Staff account)
	 * The labels here are used on the "Edit Account" form (<select>).
	 */
	public enum Type {
		FULL("1","Full Access"),
		STAFF("2","Staff"),
		TRIAL("3","Pilot"),
		UPDATE("4", "FastTRAK");

		String id;
		String label;
		private Type(String id, String label) {
			this.id = id;
			this.label = label;
		}
		public String getId() { return id; }
		public String getLabel() { return label; }

		public static Type getFromId(String id) {
			for (Type t : Type.values()) {
				if (t.getId().equals(id)) return t;
			}
			return null;
		}
	}
	
	/*
	 * Account Classification enum
	 */
	public enum Classification {
		ORTHO(1,"Ortho"),
		WOUND(2,"Wound"),
		NEURO(3,"Neuro"),
		REGEN(4, "Regen"),
		COMBO(5, "Combo");

		int id;
		String label;
		private Classification(int id, String label) {
			this.id = id;
			this.label = label;
		}
		public int getId() { return id; }
		public String getLabel() { return label; }

		public static Classification getFromId(int id) {
			for (Classification t : Classification.values()) {
				if (t.getId() == id) return t;
			}
			return null;
		}
	}
	
	/**
	 * Account Status enum mapping
	 */
	public enum Status{
		ACTIVE("A", "Active"), INACTIVE("I", "Inactive");
		
		private String statusNo;
		private String label;
		private Status(String statusNo, String label) {
			this.statusNo = statusNo;
			this.label = label;
		}
		/*==Getters==*/
		public String getStatusNo() { return statusNo; }
		public String getLabel() { return label; }
	}

	public AccountVO() {
		super();
		location = new Location();
	}

	public AccountVO(ActionRequest req) {
		this();
		setAccountId(req.getParameter("accountId"));
		setAccountName(req.getParameter("accountName"));
		setCompanyId(StringUtil.checkVal(req.getParameter("companyId"), null)); //nullable foreign key
		setCorpPhoneTxt(req.getParameter("corpPhoneTxt"));
		setParentCompanyTxt(req.getParameter("parentCompanyTxt"));
		setCompanyUrl(req.getParameter("companyUrl"));
		setTypeId(req.getParameter("typeId"));
		setClassificationId(Convert.formatInteger(req.getParameter("classificationId")));
		setOwnerProfileId(req.getParameter("ownerProfileId"));
		setCoownerProfileId(req.getParameter("coownerProfileId"));
		setStatusNo(req.getParameter("statusNo"));
		setStartDate(Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("startDate")));
		setExpirationDate(Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("expirationDate")));
		setAddress(req.getParameter("addressText"));
		setAddress2(req.getParameter("address2Text"));
		setCity(req.getParameter("cityName"));
		setState(req.getParameter("stateCode"));
		setZipCode(req.getParameter("zipCode"));
		setCountry(req.getParameter("countryCode"));
		setFdAuthFlg(Convert.formatInteger(req.getParameter("fdAuthFlg")));
		setGaAuthFlg(Convert.formatInteger(req.getParameter("gaAuthFlg")));
		setMktAuthFlg(Convert.formatInteger(req.getParameter("mktAuthFlg")));
		setSeatsNo(Convert.formatInteger(req.getParameter("seatsNo")));
	}


	/**
	 * @return the accountId
	 */
	@Column(name="account_id", isPrimaryKey=true)
	public String getAccountId() {
		return accountId;
	}

	/**
	 * @param accountId the accountId to set
	 */
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	/**
	 * @return the accountName
	 */
	@Column(name="account_nm")
	public String getAccountName() {
		return accountName;
	}

	/**
	 * @param accountName the accountName to set
	 */
	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	/**
	 * @return the companyId
	 */
	@Column(name="company_id")
	public String getCompanyId() {
		return companyId;
	}

	/**
	 * @param companyId the companyId to set
	 */
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}

	/**
	 * @return the typeId
	 */
	@Column(name="type_id")
	public String getTypeId() {
		return typeId;
	}

	public String getTypeName() {
		Type t = Type.getFromId(getTypeId());
		return t != null ? t.getLabel() : null;
	}

	/**
	 * @param typeId the typeId to set
	 */
	public void setTypeId(String typeId) {
		this.typeId = typeId;
	}

	/**
	 * @param stringVal
	 */
	public void setAddress(String stringVal) {
		getLocation().setAddress(stringVal);		
	}


	/**
	 * @param stringVal
	 */
	public void setAddress2(String stringVal) {
		getLocation().setAddress2(stringVal);
	}


	/**
	 * @param stringVal
	 */
	public void setCity(String stringVal) {
		getLocation().setCity(stringVal);
	}


	/**
	 * @param stringVal
	 */
	public void setState(String stringVal) {
		getLocation().setState(stringVal);
	}


	/**
	 * @param stringVal
	 */
	public void setZipCode(String stringVal) {
		getLocation().setZipCode(stringVal);
	}

	/**
	 * @param stringVal
	 */
	public void setCountry(String stringVal) {
		getLocation().setCountry(stringVal);
	}

	/**
	 * @return the ownerProfileId
	 */
	@Column(name="owner_profile_id")
	public String getOwnerProfileId() {
		return ownerProfileId;
	}

	/**
	 * @param ownerProfileId the ownerProfileId to set
	 */
	public void setOwnerProfileId(String ownerProfileId) {
		this.ownerProfileId = ownerProfileId;
	}

	/**
	 * @return the ownerEmailAddr
	 */
	@Column(name="owner_email_addr", isReadOnly=true)
	public String getOwnerEmailAddr() {
		return ownerEmailAddr;
	}

	/**
	 * @param ownerEmailAddr the ownerEmailAddr to set
	 */
	public void setOwnerEmailAddr(String ownerEmailAddr) {
		this.ownerEmailAddr = ownerEmailAddr;
	}
	
	/**
	 * @return the location
	 *NOTE no setter for location - we don't want outsiders to be able to nullify it, or our setter/getters around address will fail
	 */
	public Location getLocation() {
		return location;
	}

	@Column(name="address_txt")
	public String getAddress() {
		return getLocation().getAddress();
	}

	@Column(name="address2_txt")
	public String getAddress2() {
		return getLocation().getAddress2();
	}

	@Column(name="city_nm")
	public String getCity() {
		return getLocation().getCity();
	}

	@Column(name="state_cd")
	public String getState() {
		return getLocation().getState();
	}

	@Column(name="zip_cd")
	public String getZipCode() {
		return getLocation().getZipCode();
	}

	@Column(name="country_cd")
	public String getCountry() {
		return getLocation().getCountry();
	}

	/**
	 * @return the statusNo
	 */
	@Column(name="status_no")
	public String getStatusNo() {
		return statusNo;
	}

	public String getStatusName() {
		return "A".equals(getStatusNo()) ? "Active" : "Inactive";
	}

	/**
	 * @param statusNo the statusNo to set
	 */
	public void setStatusNo(String statusNo) {
		this.statusNo = statusNo;
	}

	/**
	 * @return the startDate
	 */
	@Column(name="start_dt")
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * @param startDate the startDate to set
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * @return the expirationDate
	 */
	@Column(name="expiration_dt")
	public Date getExpirationDate() {
		return expirationDate;
	}

	/**
	 * @param expirationDate the expirationDate to set
	 */
	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	@Column(name="first_nm", isReadOnly=true)
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	@Column(name="last_nm", isReadOnly=true)
	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@Column(name="fd_auth_flg")
	public int getFdAuthFlg() {
		return fdAuthFlg;
	}

	public void setFdAuthFlg(int fdAuthFlg) {
		this.fdAuthFlg = fdAuthFlg;
	}

	@Column(name="ga_auth_flg")
	public int getGaAuthFlg() {
		return gaAuthFlg;
	}

	public void setGaAuthFlg(int gaAuthFlg) {
		this.gaAuthFlg = gaAuthFlg;
	}

	@Column(name="mkt_auth_flg")
	public int getMktAuthFlg() {
		return mktAuthFlg;
	}

	public void setMktAuthFlg(int mktAuthFlg) {
		this.mktAuthFlg = mktAuthFlg;
	}

	@Column(name="seats_no")
	public int getSeatsNo() {
		return seatsNo;
	}

	public void setSeatsNo(int seatsNo) {
		this.seatsNo = seatsNo;
	}

	/**
	 * @return the title
	 */
	@Column(name="title", isReadOnly=true)
	public String getTitle() {
		return title;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	@Column(name="company_url")
	public String getCompanyUrl() {
		return companyUrl;
	}

	public void setCompanyUrl(String companyUrl) {
		this.companyUrl = companyUrl;
	}

	@Column(name="coowner_profile_id")
	public String getCoownerProfileId() {
		return coownerProfileId;
	}

	public void setCoownerProfileId(String coownerProfileId) {
		this.coownerProfileId = coownerProfileId;
	}

	@Column(name="company_nm", isReadOnly=true)
	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	@Column(name="parent_company_txt")
	public String getParentCompanyTxt() {
		return parentCompanyTxt;
	}

	public void setParentCompanyTxt(String parentCompanyTxt) {
		this.parentCompanyTxt = parentCompanyTxt;
	}

	@Column(name="corp_phone_txt")
	public String getCorpPhoneTxt() {
		return corpPhoneTxt;
	}

	public void setCorpPhoneTxt(String corpPhoneTxt) {
		this.corpPhoneTxt = corpPhoneTxt;
	}

	@Column(name="classification_id")
	public int getClassificationId() {
		return classificationId;
	}

	public String getClassificationName() {
		Classification t = Classification.getFromId(getClassificationId());
		return t != null ? t.getLabel() : null;
	}

	public void setClassificationId(int classificationId) {
		this.classificationId = classificationId;
	}

	/**
	 * @return the leadFirstName
	 */
	public String getLeadFirstName() {
		return leadFirstName;
	}

	/**
	 * @return the leadLastName
	 */
	public String getLeadLastName() {
		return leadLastName;
	}

	/**
	 * @param leadFirstName the leadFirstName to set.
	 */
	public void setLeadFirstName(String leadFirstName) {
		this.leadFirstName = leadFirstName;
	}

	/**
	 * @param leadLastName the leadLastName to set.
	 */
	public void setLeadLastName(String leadLastName) {
		this.leadLastName = leadLastName;
	}

	public String getLeadEmail() {
		return leadEmail;
	}

	public void setLeadEmail(String leadEmail) {
		this.leadEmail = leadEmail;
	}
}