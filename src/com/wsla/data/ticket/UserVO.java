package com.wsla.data.ticket;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;
import java.util.Locale;

// SMT Base Libs 3.5
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: UserVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Extension of the Profile to avoid encryption of names as
 * well as to add additional data elements
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 14, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_user")
public class UserVO extends BeanDataVO {

	private static final long serialVersionUID = 4322911178471795899L;

	// Member Variables
	private String userId;
	private String profileId;
	private String firstName;
	private String lastName;
	private String email;
	private String locale;
	private String roleId;
	private String profileRoleId;
	private String roleName;
	private int activeFlag;
	private Date createDate;
	private Date updateDate;

	// Bean Sub-Elements
	private UserDataVO profile;

	// Variables picked up at login for downstream usage ("My Location", "My Provider")
	private String locationId;
	private String providerId;

	/**
	 * 
	 */
	public UserVO() {
		super();
	}

	/**
	 * @param req
	 */
	public UserVO(ActionRequest req) {
		super(req);
		setProfile(new UserDataVO(req));
		//make a special case for workPhone, which isn't supported by UserDataVO natively
		if (req.hasParameter("workPhone"))
			profile.addPhone(new PhoneVO(PhoneVO.WORK_PHONE, req.getParameter("workPhone"), profile.getCountryCode()));
	}

	/**
	 * @param rs
	 */
	public UserVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * Returns a Java Locale object for the corresponding locale string.  Defaults
	 * to spanish and mexico if not provided
	 * @return
	 */
	public Locale getUserLocale() {
		if (StringUtil.isEmpty(locale)) locale = "es_MX";
		String language = locale.substring(0, 2);
		String country = locale.substring(3);
		return new Locale(language, country);
	}

	/**
	 * @return the userId
	 */
	@Column(name="user_id", isPrimaryKey=true)
	public String getUserId() {
		return userId;
	}

	/**
	 * @return the profileId
	 */
	@Column(name="profile_id")
	public String getProfileId() {
		return profileId;
	}

	/**
	 * @return the firstName
	 */
	@Column(name="first_nm")
	public String getFirstName() {
		return firstName;
	}

	/**
	 * @return the lastName
	 */
	@Column(name="last_nm")
	public String getLastName() {
		return lastName;
	}

	/**
	 * @return the email
	 */
	@Column(name="email_address_txt")
	public String getEmail() {
		return email;
	}

	/**
	 * @return the activeFlag
	 */
	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
	}

	/**
	 * @return the locale
	 */
	@Column(name="locale_txt")
	public String getLocale() {
		return locale;
	}

	/**
	 * @return the roleId
	 */
	@Column(name="role_id", isReadOnly=true)
	public String getRoleId() {
		return roleId;
	}

	/**
	 * @return the roleName
	 */
	@Column(name="role_nm", isReadOnly=true)
	public String getRoleName() {
		return roleName;
	}

	/**
	 * @return the profileRoleId
	 */
	@Column(name="profile_role_id", isReadOnly=true)
	public String getProfileRoleId() {
		return profileRoleId;
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
	 * @return the profile
	 */
	public UserDataVO getProfile() {
		return profile;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @param profileId the profileId to set
	 */
	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	/**
	 * @param firstName the firstName to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/**
	 * @param lastName the lastName to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
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
	 * @param profile the profile to set
	 */
	@BeanSubElement
	public void setProfile(UserDataVO profile) {
		this.profile = profile;
	}

	/**
	 * @param locale the locale to set
	 */
	public void setLocale(String locale) {
		this.locale = locale;
	}

	/**
	 * @param roleId the roleId to set
	 */
	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	/**
	 * @param roleName the roleName to set
	 */
	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	/**
	 * @param profileRoleId the profileRoleId to set
	 */
	public void setProfileRoleId(String profileRoleId) {
		this.profileRoleId = profileRoleId;
	}

	@Column(name="location_id", isReadOnly=true)
	public String getLocationId() {
		return locationId;
	}

	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}

	@Column(name="provider_id", isReadOnly=true)
	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}
}