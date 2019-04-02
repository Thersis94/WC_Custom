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

// WSLA Libs
import com.wsla.common.WSLAConstants;
import com.wsla.data.provider.ProviderType;

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
	private String lastNamePre;
	private String lastNamePost;
	private String email;
	private String mainPhone;
	private String phoneExtensionNumber;
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
	private ProviderType providerType;

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
		
		// Consolidate the last name fields
		if (StringUtil.isEmpty(lastName)) {
			lastName = (StringUtil.checkVal(lastNamePre) + " " + StringUtil.checkVal(lastNamePost)).trim();
			req.setParameter("lastName", lastName);
		}
		
		// Adds the UserDataVO to the bean
		setProfile(new UserDataVO(req));
		getProfile().getLocation().setCassValidated(false);

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
		
		//make sure the languate and country have been set if not set 
		//	them from locale if it exists
		
		if(StringUtil.isEmpty(profile.getCountryCode()) && getUserLocale() != null && !StringUtil.isEmpty(getUserLocale().getCountry())) {
			profile.setCountryCode(getUserLocale().getCountry());
		}
		
		if(StringUtil.isEmpty(profile.getLanguage()) && getUserLocale() != null && !StringUtil.isEmpty(getUserLocale().getLanguage())) {
			profile.setLanguage(getUserLocale().getLanguage());
		}
		
		return profile;
	}

	/**
	 * @return the phoneExtensionNumber
	 */
	@Column(name="phone_extension_no" )
	public String getPhoneExtensionNumber() {
		return phoneExtensionNumber;
	}

	/**
	 * @param phoneExtensionNumber the phoneExtensionNumber to set
	 */
	public void setPhoneExtensionNumber(String phoneExtensionNumber) {
		this.phoneExtensionNumber = phoneExtensionNumber;
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
		if (! WSLAConstants.NO_EMAIL_ADDRESS.equalsIgnoreCase(email))
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

	/**
	 * 
	 * @return
	 */
	@Column(name="location_id", isReadOnly=true)
	public String getLocationId() {
		return locationId;
	}

	/**
	 * 
	 * @param locationId
	 */
	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}

	/**
	 * 
	 * @return
	 */
	@Column(name="provider_id", isReadOnly=true)
	public String getProviderId() {
		return providerId;
	}

	/**
	 * 
	 * @param providerId
	 */
	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	/**
	 * @return the providerType
	 */
	@Column(name="provider_type_id", isReadOnly=true)
	public ProviderType getProviderType() {
		return providerType;
	}

	/**
	 * @param providerType the providerType to set
	 */
	
	public void setProviderType(ProviderType providerType) {
		this.providerType = providerType;
	}

	/**
	 * @return the lastNamePost
	 */
	public String getLastNamePost() {
		if (StringUtil.isEmpty(lastNamePost)  && ! StringUtil.isEmpty(lastName)) {
			int idx = lastName.indexOf(' ');
			return idx > -1 ? lastName.substring(idx + 1) : null;
		} else {
			return lastNamePost;
		}
	}

	/**
	 * @param lastNamePost the lastNamePost to set
	 */
	public void setLastNamePost(String lastNamePost) {
		this.lastNamePost = lastNamePost;
	}

	/**
	 * If the pre last name is empty and there is a lastName, parse the last
	 * name at the space.  If no space, return the last name
	 * @return the lastNamePre
	 */
	public String getLastNamePre() {
		if (StringUtil.isEmpty(lastNamePre)  && ! StringUtil.isEmpty(lastName)) {
			int idx = lastName.indexOf(' ');
			return idx > -1 ? lastName.substring(0, idx) : lastName;
		} else {
			return lastNamePre;
		}
	}

	/**
	 * @param lastNamePre the lastNamePre to set
	 */
	public void setLastNamePre(String lastNamePre) {
		this.lastNamePre = lastNamePre;
	}

	/**
	 * @return the mainPhone
	 */
	@Column(name="main_phone_txt")
	public String getMainPhone() {
		return mainPhone;
	}

	/**
	 * @param mainPhone the mainPhone to set
	 */
	public void setMainPhone(String mainPhone) {
		this.mainPhone = mainPhone;
	}
}