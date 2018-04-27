package com.depuysynthes.srt.vo;

import java.sql.ResultSet;
import java.util.Date;

import com.depuysynthes.srt.util.SRTUtil;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.annotations.DataType;
import com.siliconmtn.annotations.Importable;
import com.siliconmtn.data.parser.BeanDataMapper;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.user.HumanNameIntfc;

/****************************************************************************
 * <b>Title:</b> SRTRoster.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Stores SRT Roster Data.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 5, 2018
 ****************************************************************************/
@Table(name="DPY_SYN_SRT_ROSTER")
public class SRTRosterVO extends UserDataVO implements HumanNameIntfc {

	public enum Role {ADMIN, SALES_ROSTER, PUBLIC}

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private String rosterId;
	private String registerSubmittalId;
	private String opCoId;
	private String coRosterId;
	private String workgroupId;
	private String wwid;
	private String territoryId;
	private String accountNo;
	private String region;
	private String area;
	private String engineeringContact;
	private String companyRole;
	private String rosterEmailAddress;
	private int isActive;
	private int isAdmin;
	private Date createDt;
	private Date updateDt;
	private Date deactivatedDt;

	/**
	 * Default Constructor
	 */
	public SRTRosterVO() {
		super();
	}

	/**
	 * Initializes the class and sets the data using the request data
	 * @param req
	 */
	public SRTRosterVO(ActionRequest req) {
		super(req);
		setData(req);
		BeanDataMapper.parseBean(this, req.getParameterMap());

		if(StringUtil.isEmpty(opCoId)) {
			opCoId = SRTUtil.getOpCO(req);
		}
	}


	/**
	 * Initializes the class and sets the data using the row data
	 * @param rs
	 */
	public SRTRosterVO(ResultSet rs) {
		super(rs);
		setData(rs);
		new DBProcessor(null).executePopulate(this, rs, null);
	}

	/**
	 * @return the rosterId
	 */
	@Column(name="ROSTER_ID", isPrimaryKey=true)
	public String getRosterId() {
		return rosterId;
	}

	/**
	 * @return the opCoId
	 */
	@Column(name="OP_CO_ID")
	public String getOpCoId() {
		return opCoId;
	}

	/**
	 * @return the coRosterId
	 */
	@Column(name="CO_ROSTER_ID")
	public String getCoRosterId() {
		return coRosterId;
	}

	/**
	 * @return the workgroupId
	 */
	@Column(name="WORKGROUP_ID")
	public String getWorkgroupId() {
		return workgroupId;
	}

	/**
	 * @return the wwid
	 */
	@Column(name="WWID")
	public String getWwid() {
		return wwid;
	}

	/**
	 * @return the territoryId
	 */
	@Column(name="TERRITORY_ID")
	public String getTerritoryId() {
		return territoryId;
	}

	/**
	 * @return the accountNo
	 */
	@Column(name="ACCOUNT_NO")
	public String getAccountNo() {
		return accountNo;
	}

	/**
	 * @return the region
	 */
	@Column(name="REGION")
	public String getRegion() {
		return region;
	}

	/**
	 * @return the area
	 */
	@Column(name="AREA")
	public String getArea() {
		return area;
	}

	/**
	 * @return the engineeringContact
	 */
	@Column(name="ENGINEERING_CONTACT")
	public String getEngineeringContact() {
		return engineeringContact;
	}

	/**
	 * @return the role
	 */
	@Column(name="COMPANY_ROLE")
	public String getCompanyRole() {
		return companyRole;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="CREATE_DT", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @return the updateDt
	 */
	@Column(name="UPDATE_DT", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDt() {
		return updateDt;
	}

	/**
	 * @param rosterId the rosterId to set.
	 */
	public void setRosterId(String rosterId) {
		this.rosterId = rosterId;
	}

	/**
	 * @param opCoId the opCoId to set.
	 */
	public void setOpCoId(String opCoId) {
		this.opCoId = opCoId;
	}

	/**
	 * @param coRosterId the coRosterId to set.
	 */
	public void setCoRosterId(String coRosterId) {
		this.coRosterId = coRosterId;
	}

	/**
	 * @param accountNo the accountNo to set.
	 */
	@Importable(name="Account #", type=DataType.STRING, index=12)
	public void setAccountNo(String accountNo) {
		this.accountNo = accountNo;
	}

	/**
	 * @param workgroupId the workgroupId to set.
	 */
	@Importable(name="WorkgroupId", type=DataType.STRING, index=13, isRequired=true)
	public void setWorkgroupId(String workgroupId) {
		this.workgroupId = workgroupId;
	}

	/**
	 * @param wwid the wwid to set.
	 */
	@Importable(name="WWID", type=DataType.STRING, index=14, isRequired=true)
	public void setWwid(String wwid) {
		this.wwid = wwid;
	}

	/**
	 * @param territoryId the territoryId to set.
	 */
	@Importable(name="Territory Id", type=DataType.STRING, index=15)
	public void setTerritoryId(String territoryId) {
		this.territoryId = territoryId;
	}

	/**
	 * @param region the region to set.
	 */
	@Importable(name="Region", type=DataType.STRING, index=16)
	public void setRegion(String region) {
		this.region = region;
	}

	/**
	 * @param area the area to set.
	 */
	@Importable(name="Area", type=DataType.STRING, index=17)
	public void setArea(String area) {
		this.area = area;
	}

	/**
	 * @param engineeringContact the engineeringContact to set.
	 */
	@Importable(name="Engineering Contact", type=DataType.STRING, index=18)
	public void setEngineeringContact(String engineeringContact) {
		this.engineeringContact = engineeringContact;
	}

	/**
	 * @param role the role to set.
	 */
	public void setCompanyRole(String companyRole) {
		this.companyRole = companyRole;
	}

	/**
	 * @param createDt the createDt to set.
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}

	/**
	 * @param updateDt the updateDt to set.
	 */
	public void setUpdateDt(Date updateDt) {
		this.updateDt = updateDt;
	}

	/**
	 * @return the isActive
	 */
	@Column(name="IS_ACTIVE")
	public int getIsActive() {
		return isActive;
	}

	/**
	 * @param isActive the isActive to set.
	 */
	public void setIsActive(int isActive) {
		this.isActive = isActive;
	}

	public boolean isActive() {
		return Convert.formatBoolean(isActive);
	}

	public void setIsActive(boolean isActive) {
		this.isActive = Convert.formatInteger(isActive);
	}

	/**
	 * @return the isActive
	 */
	@Column(name="IS_ADMIN")
	public int getIsAdmin() {
		return isAdmin;
	}

	/**
	 * @param isActive the isActive to set.
	 */
	public void setIsAdmin(int isAdmin) {
		this.isAdmin = isAdmin;
	}

	public boolean isAdmin() {
		return Convert.formatBoolean(isAdmin);
	}

	public void setIsAdmin(boolean isAdmin) {
		this.isAdmin = Convert.formatInteger(isAdmin);
	}

	@Column(name="REGISTER_SUBMITTAL_ID")
	public String getRegisterSubmittalId() {
		return registerSubmittalId;
	}

	/**
	 * @param regSubId
	 */
	public void setRegisterSubmittalId(String registerSubmittalId) {
		this.registerSubmittalId = registerSubmittalId;
	}

	@Column(name="PROFILE_ID")
	@Override
	public String getProfileId() {
		return super.profileId;
	}

	@Column(name="DEACTIVATED_DT")
	public Date getDeactivatedDt() {
		return deactivatedDt;
	}

	public void setDeactivatedDt(Date deactivatedDt) {
		this.deactivatedDt = deactivatedDt;
	}

	@Column(name="FIRST_NM")
	@Override
	public String getFirstName() {
		return super.getFirstName();
	}

	@Column(name="LAST_NM")
	@Override
	public String getLastName() {
		return super.getLastName();
	}

	@Column(name="ROSTER_EMAIL_ADDRESS_TXT")
	public String getRosterEmailAddress() {
		return rosterEmailAddress;
	}

	public void setRosterEmailAddress(String rosterEmailAddress) {
		this.rosterEmailAddress = rosterEmailAddress;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.UserDataVO#setFirstName(java.lang.String)
	 */
	@Override
	@Importable(name="First Name", type=DataType.STRING, index=1, isRequired=true)
	public void setFirstName(String firstName) {
		super.setFirstName(firstName);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.UserDataVO#setLastName(java.lang.String)
	 */
	@Override
	@Importable(name="Last Name", type=DataType.STRING, index=2, isRequired=true)
	public void setLastName(String lastName) {
		super.setLastName(lastName);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.UserDataVO#setEmailAddress(java.lang.String)
	 */
	@Override
	@Importable(name="Email Address", type=DataType.STRING, index=3, isRequired=true)
	public void setEmailAddress(String emailAddress) {
		super.setEmailAddress(emailAddress);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.UserDataVO#setMainPhone(java.lang.String)
	 */
	@Override
	@Importable(name="Main Phone No", type=DataType.STRING, index=4)
	public void setMainPhone(String mainPhone) {
		super.setMainPhone(mainPhone);
	}

	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.security.UserDataVO#setMobilePhone(java.lang.String)
	 */
	@Override
	@Importable(name="Mobile Phone No", type=DataType.STRING, index=5)
	public void setMobilePhone(String mobilePhone) {
		super.setMobilePhone(mobilePhone);
	}
	/* (non-Javadoc)
	 * @see com.siliconmtn.security.UserDataVO#setAddress(java.lang.String)
	 */
	@Override
	@Importable(name="Address", type=DataType.STRING, index=6)
	public void setAddress(String address) {
		super.setAddress(address);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.UserDataVO#setAddress2(java.lang.String)
	 */
	@Override
	@Importable(name="Address 2", type=DataType.STRING, index=7)
	public void setAddress2(String address2) {
		super.setAddress2(address2);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.UserDataVO#setCity(java.lang.String)
	 */
	@Override
	@Importable(name="City", type=DataType.STRING, index=8)
	public void setCity(String city) {
		super.setCity(city);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.UserDataVO#setState(java.lang.String)
	 */
	@Override
	@Importable(name="State Cd", type=DataType.STRING, index=9)
	public void setState(String state) {
		super.setState(state);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.UserDataVO#setZipCode(java.lang.String)
	 */
	@Override
	@Importable(name="Zip Code", type=DataType.STRING, index=10)
	public void setZipCode(String zipCode) {
		super.setZipCode(zipCode);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.UserDataVO#setCountryCode(java.lang.String)
	 */
	@Override
	@Importable(name="Country Cd", type=DataType.STRING, index=11)
	public void setCountryCode(String country) {
		super.setCountryCode(country);
	}
}