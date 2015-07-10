package com.depuysynthesinst;

// SMTBaseLibs
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.Location;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title: </b>DSIUserDataVO.java <p/>
 * <b>Project: </b>DSI-WS2 <p/>
 * <b>Description: Wraps UserDataVO with some getters for Registration questions,
 * so invoking actions don't need to know we got the data from Registration (fields).</b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2015<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since Jun 11, 2015<p/>
 *<b>Changes: </b>
 * Jun 11, 2015: David Bargerhuff: Created class.
 ****************************************************************************/
public class DSIUserDataVO extends UserDataVO {
	
	private static final long serialVersionUID = 6745777068563527968L;
	private UserDataVO user;
	
	/**
	 * Register_field_id values from the database/registration portlet
	 */
	public enum RegField {
		DSI_SYNTHES_ID,
		DSI_TTLMS_ID,
		DSI_GRAD_DT,
		DSI_MY_ASSIGNMENTS,
		DSI_PROG_ELIGIBLE,
		DSI_VERIFIED,
		DSI_ACAD_NM,
		c0a80241b71c9d40a59dbd6f4b621260, //profession
		c0a80241b71d27b038342fcb3ab567a0, //specialty
		DSI_PGY
		;
	}
	
	
	/**
	 * static classloader - ensures the UserDataVO we put on session is not
	 * mangled by the WC core while in use. (like ProfileManager)
	 * @param o
	 * @return
	 */
	public static DSIUserDataVO getInstance(Object o) {
		if (o == null) {
			return new DSIUserDataVO(new UserDataVO());
		} else if (o instanceof DSIUserDataVO) {
			return (DSIUserDataVO) o;
		} else {
			return new DSIUserDataVO((UserDataVO)o);
		}
	}
	
	
	/**
	 * 
	 */
	public DSIUserDataVO(UserDataVO vo) {
		super();
		this.user = vo;
	}

	/**
	 * @return the synthesId
	 */
	public String getSynthesId() {
		return StringUtil.checkVal(user.getAttribute(RegField.DSI_SYNTHES_ID.toString()));
	}

	/**
	 * @param synthesId the synthesId to set
	 */
	public void setSynthesId(String synthesId) {
		user.addAttribute(RegField.DSI_SYNTHES_ID.toString(), synthesId);
	}

	/**
	 * @return the dsiId
	 */
	public String getDsiId() {
		return user.getProfileId();
	}

	/**
	 * @return the ttLmsId
	 */
	public String getTtLmsId() {
		return StringUtil.checkVal(user.getAttribute(RegField.DSI_TTLMS_ID.toString()));
	}

	/**
	 * @param ttLmsId the ttLmsId to set
	 */
	public void setTtLmsId(String ttLmsId) {
		user.addAttribute(RegField.DSI_TTLMS_ID.toString(), ttLmsId);
	}

	/**
	 * @return the hospital
	 */
	public String getHospital() {
		return StringUtil.checkVal(user.getAttribute(RegField.DSI_ACAD_NM.toString()));
	}

	/**
	 * @param hospital the hospital to set
	 */
	public void setHospital(String hospital) {
		user.addAttribute(RegField.DSI_ACAD_NM.toString(), hospital);
	}

	/**
	 * @return the specialty
	 */
	public String getSpecialty() {
		return StringUtil.checkVal(user.getAttribute(RegField.c0a80241b71d27b038342fcb3ab567a0.toString()));
	}

	/**
	 * @param specialty the specialty to set
	 */
	public void setSpecialty(String specialty) {
		user.addAttribute(RegField.c0a80241b71d27b038342fcb3ab567a0.toString(), specialty);
	}

	/**
	 * @return the profession
	 */
	public String getProfession() {
		return StringUtil.checkVal(user.getAttribute(RegField.c0a80241b71c9d40a59dbd6f4b621260.toString()));
	}

	/**
	 * @param profession the profession to set
	 */
	public void setProfession(String profession) {
		user.addAttribute(RegField.c0a80241b71c9d40a59dbd6f4b621260.toString(), profession);
	}

	/**
	 * @return the eligible
	 */
	public boolean isEligible() {
		return Convert.formatBoolean(user.getAttribute(RegField.DSI_PROG_ELIGIBLE.toString()));
	}

	/**
	 * @param eligible the eligible to set
	 */
	public void setEligible(boolean eligible) {
		user.addAttribute(RegField.DSI_PROG_ELIGIBLE.toString(), eligible);
	}
	public void setEligible(double eligible) {
		setEligible(Convert.formatBoolean(Double.valueOf(eligible)).booleanValue());
	}

	/**
	 * @return the verified
	 */
	public boolean isVerified() {
		return Convert.formatBoolean(user.getAttribute(RegField.DSI_VERIFIED.toString()));
	}

	/**
	 * @param verified the verified to set
	 */
	public void setVerified(boolean verified) {
		user.addAttribute(RegField.DSI_VERIFIED.toString(), verified);
	}
	public void setVerified(double verified) {
		setVerified(Convert.formatBoolean(Double.valueOf(verified)).booleanValue());
	}
	
	/**
	 * some static methods to isolate business logic without creating tons of Objects we really don't need.
	 * These are called from DSIRoleMgr
	 */
	public static String getProfession(UserDataVO user1) {
		if (user1 == null) return null;
		return StringUtil.checkVal(user1.getAttribute(RegField.c0a80241b71c9d40a59dbd6f4b621260.toString()));
	}
	
	
	/**
	 * called when we need to put the object back on session; Tomcat complains if we put a DSIUserDataVO in it's place.
	 * @return
	 */
	public UserDataVO getUserDataVO() {
		return user;
	}
	
	
	public void setAttributesFromMap(Map<Object, Object> data) {
		//check for null
		if (data == null || data.size() == 0) return;
		//check for errors
		Object o = data.get("ERROR");
		if (Convert.formatInteger(o.toString()) != 0) return;
		
		o = data.get("TTLMSID");
		if (o != null && Convert.formatInteger(o.toString()) > 0) 
			setTtLmsId(Convert.formatInteger(o.toString()).toString());
		
		o = data.get("ELIGIBLEPROGRAM");
		if (o != null) 
			setEligible(Convert.formatDouble(o.toString()));
		
		o = data.get("VERIFIED");
		if (o != null) 
			setVerified(Convert.formatDouble(o.toString()));
		
		o = data.get("SYNTHESID");
		if (o != null && Convert.formatInteger(o.toString()) > 0) 
			setSynthesId(Convert.formatInteger(o.toString()).toString());
		
	}
	
	
	/**
	 * Attaches the TTLMSID onto the userVO.  
	 * Done here, statically, so we keep all the business logic on one bean.
	 * @param user
	 * @param d
	 */
	public static void setTTLMSID(UserDataVO user, double d) {
		Integer id = Integer.valueOf(Convert.formatDouble(d).intValue());
		if (id > 0)
			user.addAttribute(RegField.DSI_TTLMS_ID.toString(), id.toString());
	}



/****************************************************************************
 * 
 *	ALL METHODS BELOW WERE CREATED BY ECLIPSE TO COMPLETE THE DECORATOR.
 *	THEY SERVE NO PURPOSE OTHERWISE AND CONTAIN NO DSI/CUSTOM CODE.. 
 *	-JM 07-08-2015
 *
 ****************************************************************************/
	
	public boolean equals(Object o) {
		return user.equals(o);
	}

	public int hashCode() {
		return user.hashCode();
	}

	public String toString() {
		return user.toString();
	}

	public void setName(String name) {
		user.setName(name);
	}

	public boolean isValidProfile() {
		return user.isValidProfile();
	}

	public boolean isUserReachable() {
		return user.isUserReachable();
	}

	public void setData(ResultSet rs) {
		user.setData(rs);
	}

	public void setData(HttpServletRequest req) {
		user.setData(req);
	}

	public Object getDisplayValue(String field) {
		return user.getDisplayValue(field);
	}

	public Map<String, Object> getDataMap(boolean includeNulls) {
		return user.getDataMap(includeNulls);
	}

	public Map<String, Object> getDataMap() {
		return user.getDataMap();
	}

	public void setDisplayValue(String field, Object value) {
		user.setDisplayValue(field, value);
	}

	public void setData(Map<String, Object> data) {
		user.setData(data);
	}

	public void addPhone(PhoneVO vo) {
		user.addPhone(vo);
	}

	public List<PhoneVO> getPhoneNumbers() {
		return user.getPhoneNumbers();
	}

	public void setLocation(Location loc) {
		user.setLocation(loc);
	}

	public void setLocation(GeocodeLocation location) {
		user.setLocation(location);
	}

	public GeocodeLocation getLocation() {
		return user.getLocation();
	}

	public void setPhoneNumbers(List<PhoneVO> phoneNumbers) {
		user.setPhoneNumbers(phoneNumbers);
	}

	public String getBestTime() {
		return user.getBestTime();
	}

	public Date getBirthDate() {
		return user.getBirthDate();
	}

	public java.sql.Date getSQLBirthDate() {
		return user.getSQLBirthDate();
	}

	public Integer getBirthYear() {
		return user.getBirthYear();
	}

	public String getLanguage() {
		return user.getLanguage();
	}

	public void setBestTime(String bestTime) {
		user.setBestTime(bestTime);
	}

	public void setBirthDate(Date birthDate) {
		user.setBirthDate(birthDate);
	}

	public void setBirthYear(Integer birthYear) {
		user.setBirthYear(birthYear);
	}

	public void setLanguage(String language) {
		user.setLanguage(language);
	}

	public Integer getGlobalAdminFlag() {
		return user.getGlobalAdminFlag();
	}

	public Double getLatitude() {
		return user.getLatitude();
	}

	public Double getLongitude() {
		return user.getLongitude();
	}

	public Integer getValidEmailFlag() {
		return user.getValidEmailFlag();
	}

	public void setGlobalAdminFlag(Integer globalAdminFlag) {
		user.setGlobalAdminFlag(globalAdminFlag);
	}

	public void setLatitude(Double latitude) {
		user.setLatitude(latitude);
	}

	public void setLongitude(Double longitude) {
		user.setLongitude(longitude);
	}

	public void setValidEmailFlag(Integer validEmailFlag) {
		user.setValidEmailFlag(validEmailFlag);
	}

	public Integer getAllowCommunication() {
		return user.getAllowCommunication();
	}

	public void setAllowCommunication(Integer allowCommunication) {
		user.setAllowCommunication(allowCommunication);
	}

	public String getAddress() {
		return user.getAddress();
	}

	public String getAddress2() {
		return user.getAddress2();
	}

	public String getAuthenticationId() {
		return user.getAuthenticationId();
	}

	public String getCity() {
		return user.getCity();
	}

	public String getCountryCode() {
		return user.getCountryCode();
	}

	public String getCounty() {
		return user.getCounty();
	}

	public String getEmailAddress() {
		return user.getEmailAddress();
	}

	public String getFirstName() {
		return user.getFirstName();
	}

	public String getGenderCode() {
		return user.getGenderCode();
	}

	public String getLastName() {
		return user.getLastName();
	}

	public String getMainPhone() {
		return user.getMainPhone();
	}

	public String getWorkPhone() {
		return user.getWorkPhone();
	}

	public String getMatchCode() {
		return user.getMatchCode();
	}

	public String getMiddleName() {
		return user.getMiddleName();
	}

	public String getMobilePhone() {
		return user.getMobilePhone();
	}

	public String getPassword() {
		return user.getPassword();
	}

	public String getPasswordResetKey() {
		return user.getPasswordResetKey();
	}

	public void setPasswordResetKey(String passwordResetKey) {
		user.setPasswordResetKey(passwordResetKey);
	}

	public String getPasswordHistory() {
		return user.getPasswordHistory();
	}

	public void setPasswordHistory(String passwordHistory) {
		user.setPasswordHistory(passwordHistory);
	}

	public String getPrefixName() {
		return user.getPrefixName();
	}

	public String getProfileId() {
		return user.getProfileId();
	}

	public String getState() {
		return user.getState();
	}

	public String getSuffixName() {
		return user.getSuffixName();
	}

	public String getZipCode() {
		return user.getZipCode();
	}

	public String getZipSuffixCode() {
		return user.getZipSuffixCode();
	}

	public void setAddress(String address) {
		user.setAddress(address);
	}

	public void setAddress2(String address2) {
		user.setAddress2(address2);
	}

	public void setAuthenticationId(String authenticationId) {
		user.setAuthenticationId(authenticationId);
	}

	public void setCity(String city) {
		user.setCity(city);
	}

	public void setCountryCode(String country) {
		user.setCountryCode(country);
	}

	public void setCounty(String county) {
		user.setCounty(county);
	}

	public void setEmailAddress(String emailAddress) {
		user.setEmailAddress(emailAddress);
	}

	public void setFirstName(String firstName) {
		user.setFirstName(firstName);
	}

	public void setGenderCode(String genderCode) {
		user.setGenderCode(genderCode);
	}

	public void setLastName(String lastName) {
		user.setLastName(lastName);
	}

	public void setMainPhone(String mainPhone) {
		user.setMainPhone(mainPhone);
	}

	public void setMatchCode(String matchCode) {
		user.setMatchCode(matchCode);
	}

	public void setMiddleName(String middleName) {
		user.setMiddleName(middleName);
	}

	public void setMobilePhone(String mobilePhone) {
		user.setMobilePhone(mobilePhone);
	}

	public void setPassword(String password) {
		user.setPassword(password);
	}

	public void setPrefixName(String prefixName) {
		user.setPrefixName(prefixName);
	}

	public void setProfileId(String profileId) {
		user.setProfileId(profileId);
	}

	public void setState(String state) {
		user.setState(state);
	}

	public void setSuffixName(String suffixName) {
		user.setSuffixName(suffixName);
	}

	public void setZipCode(String zipCode) {
		user.setZipCode(zipCode);
	}

	public void setZipSuffixCode(String zipSuffixCode) {
		user.setZipSuffixCode(zipSuffixCode);
	}

	public String getBarCodeId() {
		return user.getBarCodeId();
	}

	public void setBarCodeId(String id) {
		user.setBarCodeId(id);
	}

	public int getCassValidated() {
		return user.getCassValidated();
	}

	public void setCassValidated(int id) {
		user.setCassValidated(id);
	}

	public Integer getValidAddressFlag() {
		return user.getValidAddressFlag();
	}

	public void setValidAddressFlag(Integer validAddressFlag) {
		user.setValidAddressFlag(validAddressFlag);
	}

	public String getFullName() {
		return user.getFullName();
	}

	public String getAliasName() {
		return user.getAliasName();
	}

	public void setAliasName(String aliasName) {
		user.setAliasName(aliasName);
	}

	public int getAuthenticationLogId() {
		return user.getAuthenticationLogId();
	}

	public void setAuthenticationLogId(int authenticationLogId) {
		user.setAuthenticationLogId(authenticationLogId);
	}

	public boolean isAuthenticated() {
		return user.isAuthenticated();
	}

	public void setAuthenticated(boolean isAuthenticated) {
		user.setAuthenticated(isAuthenticated);
	}

	public boolean isLockedOut() {
		return user.isLockedOut();
	}

	public void setLockedOut(boolean isLockedOut) {
		user.setLockedOut(isLockedOut);
	}

	public String getAllowCommunicationReason() {
		return user.getAllowCommunicationReason();
	}

	public void setAllowCommunicationReason(String reason) {
		user.setAllowCommunicationReason(reason);
	}

	public Integer getPasswordResetFlag() {
		return user.getPasswordResetFlag();
	}

	public void setPasswordResetFlag(Integer passwordResetFlag) {
		user.setPasswordResetFlag(passwordResetFlag);
	}

	public Date getPasswordChangeDate() {
		return user.getPasswordChangeDate();
	}

	public void setPasswordChangeDate(Date passwordChangeDate) {
		user.setPasswordChangeDate(passwordChangeDate);
	}

	public Long getPasswordAge() {
		return user.getPasswordAge();
	}

	public void setPasswordAge(Long passwordAge) {
		user.setPasswordAge(passwordAge);
	}

	public Map<String, Object> getAttributes() {
		return user.getAttributes();
	}

	public void setAttributes(Map<String, Object> attributes) {
		user.setAttributes(attributes);
	}

	public void addAttribute(String key, Object val) {
		user.addAttribute(key, val);
	}

	public Object getAttribute(String key) {
		return user.getAttribute(key);
	}

	public Object getUserExtendedInfo() {
		return user.getUserExtendedInfo();
	}

	public void setUserExtendedInfo(Object userExtendedInfo) {
		user.setUserExtendedInfo(userExtendedInfo);
	}

	public AuthenticationType getAuthType() {
		return user.getAuthType();
	}

	public void setAuthType(AuthenticationType authType) {
		user.setAuthType(authType);
	}

	public void setAuthType(String authType) {
		user.setAuthType(authType);
	}

	public String getProfileImage() {
		return user.getProfileImage();
	}

	public void setProfileImage(String profileImagePath) {
		user.setProfileImage(profileImagePath);
	}
}
