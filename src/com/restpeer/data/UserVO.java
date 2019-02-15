package com.restpeer.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.security.UserDataVO;

/****************************************************************************
 * <b>Title</b>: UserVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> manages the extended (non-encrypted) user information
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 15, 2019
 * @updates:
 ****************************************************************************/
@Table(name="rp_user")
public class UserVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8846752021614138570L;
	
	// Members
	private String userId;
	private String profileId;
	private String firstName;
	private String lastName;
	private String emailAddress;
	private String phoneNumber;
	private Date createDate;
	private Date updateDate;
	
	// Subbeans
	private UserDataVO profile;

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
	}

	/**
	 * @param rs
	 */
	public UserVO(ResultSet rs) {
		super(rs);
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
	 * @return the emailAddress
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
	 * @param emailAddress the emailAddress to set
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
	public void setProfile(UserDataVO profile) {
		this.profile = profile;
	}

}

