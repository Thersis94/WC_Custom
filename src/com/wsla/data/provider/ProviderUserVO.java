package com.wsla.data.provider;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: ProviderUserVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value Object to assign users to a given provider
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 17, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_provider_user_xr")
public class ProviderUserVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8202735555518602640L;
	
	// Member Variables
	private String providerUserId;
	private String locationId;
	private String userId;
	private String departmentName;
	private int primaryContactFlag;
	private int activeFlag; 
	private Date createDate;
	private Date updateDate;

	// Bean Sub-Elements
	private ProviderLocationVO location;
	private UserVO user;
	
	/**
	 * 
	 */
	public ProviderUserVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ProviderUserVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public ProviderUserVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the providerUserId
	 */
	@Column(name="provider_user_xr_id", isPrimaryKey=true)
	public String getProviderUserId() {
		return providerUserId;
	}

	/**
	 * @return the locationId
	 */
	@Column(name="location_id")
	public String getLocationId() {
		return locationId;
	}

	/**
	 * @return the userId
	 */
	@Column(name="user_id")
	public String getUserId() {
		return userId;
	}

	/**
	 * @return the departmentName
	 */
	@Column(name="department_nm")
	public String getDepartmentName() {
		return departmentName;
	}

	/**
	 * @return the primaryContactFlag
	 */
	@Column(name="primary_contact_flg")
	public int getPrimaryContactFlag() {
		return primaryContactFlag;
	}

	/**
	 * @return the activeFlag
	 */
	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @return the location
	 */
	public ProviderLocationVO getLocation() {
		return location;
	}

	/**
	 * @return the user
	 */
	public UserVO getUser() {
		return user;
	}

	/**
	 * @param providerUserId the providerUserId to set
	 */
	public void setProviderUserId(String providerUserId) {
		this.providerUserId = providerUserId;
	}

	/**
	 * @param locationId the locationId to set
	 */
	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @param departmentName the departmentName to set
	 */
	public void setDepartmentName(String departmentName) {
		this.departmentName = departmentName;
	}

	/**
	 * @param primaryContactFlag the primaryContactFlag to set
	 */
	public void setPrimaryContactFlag(int primaryContactFlag) {
		this.primaryContactFlag = primaryContactFlag;
	}

	/**
	 * @param activeFlag the activeFlag to set
	 */
	public void setActiveFlag(int activeFlag) {
		this.activeFlag = activeFlag;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	/**
	 * @param location the location to set
	 */
	@BeanSubElement
	public void setLocation(ProviderLocationVO location) {
		this.location = location;
	}

	/**
	 * @param user the user to set
	 */
	@BeanSubElement
	public void setUser(UserVO user) {
		this.user = user;
	}

}

