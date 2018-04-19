package com.depuysynthes.srt.vo;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
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
	private String territory;
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
	}


	/**
	 * Initializes the class and sets the data using the row data
	 * @param rs
	 */
	public SRTRosterVO(ResultSet rs) {
		super(rs);
		setData(rs);
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
	@Column(name="ACOUNT_NO")
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
	 * @return the territory
	 */
	@Column(name="TERRITORY")
	public String getTerritory() {
		return territory;
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
	 * @param workgroupId the workgroupId to set.
	 */
	public void setWorkgroupId(String workgroupId) {
		this.workgroupId = workgroupId;
	}

	/**
	 * @param wwid the wwid to set.
	 */
	public void setWwid(String wwid) {
		this.wwid = wwid;
	}

	/**
	 * @param territoryId the territoryId to set.
	 */
	public void setTerritoryId(String territoryId) {
		this.territoryId = territoryId;
	}

	/**
	 * @param accountNo the accountNo to set.
	 */
	public void setAccountNo(String accountNo) {
		this.accountNo = accountNo;
	}

	/**
	 * @param region the region to set.
	 */
	public void setRegion(String region) {
		this.region = region;
	}

	/**
	 * @param area the area to set.
	 */
	public void setArea(String area) {
		this.area = area;
	}

	/**
	 * @param territory the territory to set.
	 */
	public void setTerritory(String territory) {
		this.territory = territory;
	}

	/**
	 * @param engineeringContact the engineeringContact to set.
	 */
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
}