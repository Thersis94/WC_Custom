package com.depuysynthes.srt;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.security.UserDataVO;

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
@Table(name="SRT_ROSTER")
public class SRTRosterVO extends UserDataVO {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private String rosterId;
	private String opCoId;
	private String workgroupId;
	private String wwid;
	private String territoryId;
	private String accountNo;
	private String region;
	private String area;
	private String territory;
	private String engineeringContact;
	private String role;
	private Date createDt;
	private Date updateDt;

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
	@Column(name="ROLE")
	public String getRole() {
		return role;
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
	 * @param opCoId the opCoId to set.
	 */
	public void setOpCoId(String opCoId) {
		this.opCoId = opCoId;
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
	public void setRole(String role) {
		this.role = role;
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
}