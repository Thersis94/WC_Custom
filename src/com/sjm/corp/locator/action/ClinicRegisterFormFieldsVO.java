package com.sjm.corp.locator.action;

// WC 2.0 libs
import java.io.Serializable;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: ClinicRegisterFormValidationVO.java <p/>
 * <b>Project</b>: SB_ANS_Medical <p/>
 * <b>Description: </b> Contains fields representing the form fields for registering an admin and clinic
 * for the clinic locator.  This bean is placed on the session so that if 'captcha' validation fails, we can
 * return the form input to the JSTL.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Dec 06, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class ClinicRegisterFormFieldsVO implements Serializable {
	
	private static final long serialVersionUID = 8263061999527072837L;
	private boolean adminSubmitted = false;
	private String firstName;
	private String lastName;
	private String adminPhone;
	private String emailAddress;
	private String password;
	private String password2;
	private String locationName;
	private String address;
	private String address2;
	private String country;
	private String city;
	private String state;
	private String zip;
	private String phone;
	private String fax;
	private String website;
	private String pcmkr;
	private String icd;
	
	/**
	 * Constructor
	 * @param req
	 */
	public ClinicRegisterFormFieldsVO(ActionRequest req) {
		setData(req);
	}
	
	/**
	 * Sets bean values based on the request parameters.
	 * @param req
	 */
	private void setData(ActionRequest req) {
		setAdminSubmitted(Convert.formatBoolean(req.getParameter("adminSubmitted")));
		setFirstName(req.getParameter("firstName"));
		setLastName(req.getParameter("lastName"));
		setAdminPhone(req.getParameter("adminPhone"));
		setEmailAddress(req.getParameter("emailAddress"));
		setPassword(req.getParameter("Password"));
		setPassword2(req.getParameter("password2"));

		if (! isAdminSubmitted()) {
			setLocationName(req.getParameter("locationName"));
			setAddress(req.getParameter("address"));
			setAddress2(req.getParameter("address2"));
			setCountry(req.getParameter("country"));
			setCity(req.getParameter("city"));
			setState(req.getParameter("state"));
			setZip(req.getParameter("zip"));
			setPhone(req.getParameter("phone"));
			setFax(req.getParameter("fax"));
			setWebsite(req.getParameter("website"));
			setPcmkr(req.getParameter("pcmkr"));
			setIcd(req.getParameter("icd"));
		}
	}

	/**
	 * @return the adminSubmitted
	 */
	public boolean isAdminSubmitted() {
		return adminSubmitted;
	}

	/**
	 * @param adminSubmitted the adminSubmitted to set
	 */
	public void setAdminSubmitted(boolean adminSubmitted) {
		this.adminSubmitted = adminSubmitted;
	}

	/**
	 * @return the firstName
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * @param firstName the firstName to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/**
	 * @return the lastName
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * @param lastName the lastName to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/**
	 * @return the adminPhone
	 */
	public String getAdminPhone() {
		return adminPhone;
	}

	/**
	 * @param adminPhone the adminPhone to set
	 */
	public void setAdminPhone(String adminPhone) {
		this.adminPhone = adminPhone;
	}

	/**
	 * @return the emailAddress
	 */
	public String getEmailAddress() {
		return emailAddress;
	}

	/**
	 * @param emailAddress the emailAddress to set
	 */
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the password2
	 */
	public String getPassword2() {
		return password2;
	}

	/**
	 * @param password2 the password2 to set
	 */
	public void setPassword2(String password2) {
		this.password2 = password2;
	}

	/**
	 * @return the locationName
	 */
	public String getLocationName() {
		return locationName;
	}

	/**
	 * @param locationName the locationName to set
	 */
	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

	/**
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * @param address the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}

	/**
	 * @return the address2
	 */
	public String getAddress2() {
		return address2;
	}

	/**
	 * @param address2 the address2 to set
	 */
	public void setAddress2(String address2) {
		this.address2 = address2;
	}

	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}

	/**
	 * @param country the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * @return the city
	 */
	public String getCity() {
		return city;
	}

	/**
	 * @param city the city to set
	 */
	public void setCity(String city) {
		this.city = city;
	}

	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}

	/**
	 * @param state the state to set
	 */
	public void setState(String state) {
		this.state = state;
	}

	/**
	 * @return the zip
	 */
	public String getZip() {
		return zip;
	}

	/**
	 * @param zip the zip to set
	 */
	public void setZip(String zip) {
		this.zip = zip;
	}

	/**
	 * @return the phone
	 */
	public String getPhone() {
		return phone;
	}

	/**
	 * @param phone the phone to set
	 */
	public void setPhone(String phone) {
		this.phone = phone;
	}

	/**
	 * @return the fax
	 */
	public String getFax() {
		return fax;
	}

	/**
	 * @param fax the fax to set
	 */
	public void setFax(String fax) {
		this.fax = fax;
	}

	/**
	 * @return the website
	 */
	public String getWebsite() {
		return website;
	}

	/**
	 * @param website the website to set
	 */
	public void setWebsite(String website) {
		this.website = website;
	}

	/**
	 * @return the pcmkr
	 */
	public String getPcmkr() {
		return pcmkr;
	}

	/**
	 * @param pcmkr the pcmkr to set
	 */
	public void setPcmkr(String pcmkr) {
		this.pcmkr = pcmkr;
	}

	/**
	 * @return the icd
	 */
	public String getIcd() {
		return icd;
	}

	/**
	 * @param icd the icd to set
	 */
	public void setIcd(String icd) {
		this.icd = icd;
	}
	
}
