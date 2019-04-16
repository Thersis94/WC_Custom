package com.perfectstorm.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;
import java.util.Locale;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: MemberVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for the member data
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 4, 2019
 * @updates:
 ****************************************************************************/
@Table(name="ps_member")
public class MemberVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115255124200557677L;
	
	// Members
	private String memberId;
	private String profileId;
	private String firstName;
	private String lastName;
	private String emailAddress;
	private String phoneNumber;
	private String locale;
	private int sendSmsFlag;
	private Date createDate;
	private Date updateDate;
	
	// Bean SubElements
	private UserDataVO profile;
	
	// Helper Members
	private String customers;
	private String roleId;
	private String profileRoleId;
	private String authenticationId;
	private String roleName;
	private String genderCode;
	private String prefixName;
	private String aliasName;

	/**
	 * 
	 */
	public MemberVO() {
		super();
	}

	/**
	 * @param req
	 */
	public MemberVO(ActionRequest req) {
		super(req);
		
		// Adds the UserDataVO to the bean
		setProfile(new UserDataVO(req));
		getProfile().getLocation().setCassValidated(false);

		//make a special case for workPhone, which isn't supported by UserDataVO natively
		if (req.hasParameter("workPhone"))
			profile.addPhone(new PhoneVO(PhoneVO.WORK_PHONE, req.getParameter("phoneNumber"), profile.getCountryCode()));

	}

	/**
	 * @param rs
	 */
	public MemberVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * Returns a Java Locale object for the corresponding locale string.  Defaults
	 * to english and US if not provided
	 * @return
	 */
	public Locale getUserLocale() {
		if (StringUtil.isEmpty(locale)) locale = "en_US";
		String language = locale.substring(0, 2);
		String country = locale.substring(3);
		return new Locale(language, country);
	}
	
	/**
	 * @return the memberId
	 */
	@Column(name="member_id", isPrimaryKey=true)
	public String getMemberId() {
		return memberId;
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
	public String getEmailAddress() {
		return emailAddress;
	}

	/**
	 * @return the phoneNumber
	 */
	@Column(name="phone_number_txt")
	public String getPhoneNumber() {
		return phoneNumber;
	}

	/**
	 * @return the locale
	 */
	@Column(name="locale_txt")
	public String getLocale() {
		return locale;
	}

	/**
	 * @return the customers
	 */
	@Column(name="customers_txt", isReadOnly=true)
	public String getCustomers() {
		return customers;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
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
	 * @return the genderCode
	 */
	@Column(name="gender_cd", isReadOnly=true)
	public String getGenderCode() {
		return genderCode;
	}

	/**
	 * @return the prefixName
	 */
	@Column(name="prefix_nm", isReadOnly=true)
	public String getPrefixName() {
		return prefixName;
	}

	/**
	 * @return the aliasName
	 */
	@Column(name="alias_nm", isReadOnly=true)
	public String getAliasName() {
		return aliasName;
	}

	/**
	 * @return the profileRoleId
	 */
	@Column(name="profile_role_id", isReadOnly=true)
	public String getProfileRoleId() {
		return profileRoleId;
	}

	/**
	 * @return the authenticationId
	 */
	@Column(name="authentication_id", isReadOnly=true)
	public String getAuthenticationId() {
		return authenticationId;
	}

	/**
	 * @return the profile
	 */
	public UserDataVO getProfile() {
		return profile;
	}

	/**
	 * @param memberId the memberId to set
	 */
	public void setMemberId(String memberId) {
		this.memberId = memberId;
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
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	/**
	 * @param phoneNumber the phoneNumber to set
	 */
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
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
	 * @param customers the customers to set
	 */
	public void setCustomers(String customers) {
		this.customers = customers;
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
	 * @param authenticationId the authenticationId to set
	 */
	public void setAuthenticationId(String authenticationId) {
		this.authenticationId = authenticationId;
	}

	/**
	 * @param genderCode the genderCode to set
	 */
	public void setGenderCode(String genderCode) {
		this.genderCode = genderCode;
	}

	/**
	 * @param prefixName the prefixName to set
	 */
	public void setPrefixName(String prefixName) {
		this.prefixName = prefixName;
	}

	/**
	 * @param aliasName the aliasName to set
	 */
	public void setAliasName(String aliasName) {
		this.aliasName = aliasName;
	}

	/**
	 * @return the sendSmsFlag
	 */
	@Column(name="send_sms_flg")
	public int getSendSmsFlag() {
		return sendSmsFlag;
	}

	/**
	 * @param sendSmsFlag the sendSmsFlag to set
	 */
	public void setSendSmsFlag(int sendSmsFlag) {
		this.sendSmsFlag = sendSmsFlag;
	}

}

