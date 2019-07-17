package com.restpeer.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.user.UserVO;

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
public class RPUserVO extends UserVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8846752021614138570L;
	
	// Members
	private String driverLicense;
	private String driverLicensePath;

	// helpers
	private long memberAssociations;
	private String formattedPhoneNumber;
	private Map<String, Integer> mobileRestStatus;
	
	/**
	 * 
	 */
	public RPUserVO() {
		super();
	}

	/**
	 * @param req
	 */
	public RPUserVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public RPUserVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * @param rs
	 */
	public RPUserVO(UserVO user) {
		super();
		setUserId(user.getUserId());
		setFirstName(user.getFirstName());
		setLastName(user.getLastName());
		setEmailAddress(user.getEmailAddress());
		setPhoneNumber(user.getPhoneNumber());
		setProfileId(user.getProfileId());
		setProfile(user.getProfile());
	}

	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.parser.BeanDataVO#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this);
	}
	/**
	 * @return the driverLicense
	 */
	@Column(name="driver_license_txt")
	public String getDriverLicense() {
		return driverLicense;
	}

	/**
	 * @return the driverLicensePath
	 */
	@Column(name="driver_license_path")
	public String getDriverLicensePath() {
		return driverLicensePath;
	}

	/**
	 * @return the memberAssociations
	 */
	@Column(name="member_assoc_no")
	public long getMemberAssociations() {
		return memberAssociations;
	}

	/**
	 * @return the formattedPhoneNumber
	 */
	public String getFormattedPhoneNumber() {
		return formattedPhoneNumber;
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.user.UserVO#setPhoneNumber(java.lang.String)
	 */
	@Override
	public void setPhoneNumber(String phoneNumber) {
		super.setPhoneNumber(phoneNumber);
		
		PhoneNumberFormat pnf = new PhoneNumberFormat(getPhoneNumber(), PhoneNumberFormat.INTERNATIONAL_FORMAT);
		formattedPhoneNumber = pnf.getFormattedNumber();
	}
	
	/**
	 * @param driverLicense the driverLicense to set
	 */
	public void setDriverLicense(String driverLicense) {
		this.driverLicense = driverLicense;
	}

	/**
	 * @param driverLicensePath the driverLicensePath to set
	 */
	public void setDriverLicensePath(String driverLicensePath) {
		this.driverLicensePath = driverLicensePath;
	}

	/**
	 * @param memberAssociations the memberAssociations to set
	 */
	public void setMemberAssociations(long memberAssociations) {
		this.memberAssociations = memberAssociations;
	}

	/**
	 * @param formattedPhoneNumber the formattedPhoneNumber to set
	 */
	public void setFormattedPhoneNumber(String formattedPhoneNumber) {
		this.formattedPhoneNumber = formattedPhoneNumber;
	}

	/**
	 * @return the mobileRestStatus
	 */
	public Map<String, Integer> getMobileRestStatus() {
		return mobileRestStatus;
	}

	/**
	 * @param dealerId
	 * @return the mobileRestStatus for a particular dealer
	 */
	public int getMobileRestStatus(String dealerId) {
		return mobileRestStatus == null || mobileRestStatus.get(dealerId) == null ? 0 : mobileRestStatus.get(dealerId);
	}

	/**
	 * @param mobileRestStatus the mobileRestStatus to set
	 */
	public void setMobileRestStatus(Map<String, Integer> mobileRestStatus) {
		this.mobileRestStatus = mobileRestStatus;
	}

}

