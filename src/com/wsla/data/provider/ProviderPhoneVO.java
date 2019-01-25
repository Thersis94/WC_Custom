package com.wsla.data.provider;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ProviderPhoneVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value Object storing phone numbers for the provider 800
 * numbers
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 25, 2019
 * @updates:
 ****************************************************************************/
@Table(name="wsla_provider_phone")
public class ProviderPhoneVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1971760958693549989L;
	
	// Members
	private String providerPhoneId;
	private String providerId;
	private String phoneNumber;
	private String formattedPhoneNumber;
	private String countryCode;
	private String providerName;
	private int activeFlag;
	private Date createDate;

	/**
	 * 
	 */
	public ProviderPhoneVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ProviderPhoneVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public ProviderPhoneVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the providerPhoneId
	 */
	@Column(name="provider_phone_id", isPrimaryKey=true)
	public String getProviderPhoneId() {
		return providerPhoneId;
	}

	/**
	 * @return the providerId
	 */
	@Column(name="provider_id")
	public String getProviderId() {
		return providerId;
	}

	/**
	 * @return the phoneNumber
	 */
	@Column(name="phone_number_txt")
	public String getPhoneNumber() {
		return phoneNumber;
	}
	
	/**
	 * Formats the phone number and returns
	 * @return
	 */
	public String getFormattedNumber() {
		return formattedPhoneNumber;
		
	}
	
	/**
	 * 
	 */
	public void setFormattedPhoneNumber() {
		if (StringUtil.isEmpty(phoneNumber)) return;
		
		PhoneNumberFormat pnf = new PhoneNumberFormat();
		pnf.setPhoneNumber(phoneNumber);
		pnf.setCountryCode(getCountryCode());
		formattedPhoneNumber = pnf.getFormattedNumber();
	}

	/**
	 * @return the countryCode
	 */
	@Column(name="country_cd")
	public String getCountryCode() {
		return countryCode;
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
	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the providerName
	 */
	@Column(name="provider_nm", isReadOnly=true)
	public String getProviderName() {
		return providerName;
	}

	/**
	 * @param providerPhoneId the providerPhoneId to set
	 */
	public void setProviderPhoneId(String providerPhoneId) {
		this.providerPhoneId = providerPhoneId;
	}

	/**
	 * @param providerId the providerId to set
	 */
	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	/**
	 * @param phoneNumber the phoneNumber to set
	 */
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
		this.setFormattedPhoneNumber();
	}

	/**
	 * @param countryCode the countryCode to set
	 */
	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
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
	 * @param providerName the providerName to set
	 */
	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}

}

