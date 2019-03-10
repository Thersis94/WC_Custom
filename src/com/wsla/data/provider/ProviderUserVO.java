package com.wsla.data.provider;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.util.StringUtil;
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
public class ProviderUserVO extends UserVO {

	private static final long serialVersionUID = 8202735555518602640L;

	// Member Variables
	private String providerUserId;
	private String departmentName;
	private int primaryContactFlag;

	// Convenience Variables
	private String formattedPhoneNumbers;
	private String workPhoneNumber;
	private String mobilePhoneNumber;

	// Bean Sub-Elements
	private ProviderLocationVO location;


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
	 * 
	 * @return
	 */
	public String getFormattedPhoneNumbers() {
		return formattedPhoneNumbers;
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
	@Override
	@Column(name="location_id")
	public String getLocationId() {
		return super.getLocationId();
	}

	/**
	 * @return the departmentName
	 */
	@Column(name="dept_nm")
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
	 * @return the workPhoneNumber
	 */
	public String getWorkPhoneNumber() {
		return workPhoneNumber;
	}

	/**
	 * @return the mobilePhoneNumber
	 */
	public String getMobilePhoneNumber() {
		return mobilePhoneNumber;
	}

	/**
	 * @return the location
	 */
	public ProviderLocationVO getLocation() {
		return location;
	}

	/**
	 * @param providerUserId the providerUserId to set
	 */
	public void setProviderUserId(String providerUserId) {
		this.providerUserId = providerUserId;
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
	 * @param location the location to set
	 */
	@BeanSubElement
	public void setLocation(ProviderLocationVO location) {
		this.location = location;
	}

	/**
	 * 
	 * @param formattedPhonenumbers
	 */
	public void setFormattedPhoneNumbers(String formattedPhoneNumbers) {
		this.formattedPhoneNumbers = formattedPhoneNumbers;
	}

	/**
	 * @param formattedPhoneNumbers the formattedPhoneNumbers to set
	 */
	public void setFormattedPhoneNumbers(List<PhoneVO> phoneNumbers, String sep) {
		if (phoneNumbers == null) return; 

		StringBuilder pn = new StringBuilder(100);
		for (PhoneVO phone : getProfile().getPhoneNumbers()) {
			if (! Arrays.asList("MOBILE", "WORK").contains(phone.getPhoneType())) continue;

			phone.setCountry(StringUtil.checkVal(getLocale()).substring(2));

			pn.append(StringUtil.capitalize(phone.getPhoneType(), true));
			pn.append(": ").append(phone.getFormattedNumber()).append(sep);
		}

		this.formattedPhoneNumbers =  pn.toString();
	}

	/**
	 * @param workPhoneNumber the workPhoneNumber to set
	 */
	public void setWorkPhoneNumber(String workPhoneNumber) {
		this.workPhoneNumber = workPhoneNumber;
	}

	/**
	 * @param mobilePhoneNumber the mobilePhoneNumber to set
	 */
	public void setMobilePhoneNumber(String mobilePhoneNumber) {
		this.mobilePhoneNumber = mobilePhoneNumber;
	}
}