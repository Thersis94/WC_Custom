package com.depuysynthesinst;

// SMTBaseLibs
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionRequest;
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
	public static final String MY_COURSES = "myCourses"; //a constant for the user's LMS data loaded at login
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
		DSI_PGY,
		DSI_MIL_HOSP, //used for Proffer email notification
		DSI_DSRP_TRANSFER_AUTH,
		DSI_COUNTRY
		;
	}
	
	/**
	 *  needed by the login module for a work-around for WWID users
	 */
	public static final String REG_ACTION_GROUP_ID="c0a80241b7139e30186fd3b0749eb907";
	
	
	public DSIUserDataVO() {
		super();
		setUser(null);
		//called from jsp:useBean syntax only, in conjuction to the below setter
		//JSTL needs a concrete object in order to access static methods.
	}
	public DSIUserDataVO(UserDataVO user) {
		super();
		setUser(user);
	}
	
	public void setUser(UserDataVO user) {
		this.user = user == null ? new UserDataVO() : user;
	}
	

	/**
	 * @return the synthesId
	 */
	public String getSynthesId() {
		return StringUtil.checkVal(user.getAttribute(RegField.DSI_SYNTHES_ID.toString()));
	}
	
	
	/**
	 * the user is a transfer (@registration time) if they have a legacy Synthes ID
	 * @return
	 */
	public boolean isTransfer() {
		return getSynthesId().length() > 0;
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
	
	public Date getGraduationDate() {
		return Convert.formatDate(Convert.DATE_SLASH_PATTERN, "" + user.getAttribute(RegField.DSI_GRAD_DT.toString()));
	}
	
	/**
	 * returns true if the user is graduating within 6mos
	 * @return
	 */
	public boolean isGraduatingSoon() {
		if (getGraduationDate() == null) return false;
		Calendar then = Calendar.getInstance();
		Date now = then.getTime();
		then.setTime(getGraduationDate());
		then.add(Calendar.DAY_OF_YEAR, -365); //rollback 1yr
		return now.after(then.getTime()) && now.before(getGraduationDate());
	}

	/**
	 * @return the ttLmsId
	 */
	public String getTtLmsId() {
		return StringUtil.checkVal(user.getAttribute(RegField.DSI_TTLMS_ID.toString()), null);
	}

	/**
	 * @param ttLmsId the ttLmsId to set
	 */
	public void setTtLmsId(String ttLmsId) {
		Integer intVal = Convert.formatInteger(ttLmsId, 0);
		user.addAttribute(RegField.DSI_TTLMS_ID.toString(), intVal.toString());
	}
	public void setTtLmsId(double d) {
		setTtLmsId("" + d);
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
		setEligible(Convert.formatBoolean(Double.valueOf(eligible).intValue()).booleanValue());
	}

	/**
	 * @return the verified
	 */
	@Override
	public boolean isVerified() {
		return Convert.formatBoolean(user.getAttribute(RegField.DSI_VERIFIED.toString()));
	}

	/**
	 * @param verified the verified to set
	 */
	@Override
	public void setVerified(boolean verified) {
		user.addAttribute(RegField.DSI_VERIFIED.toString(), verified);
	}
	public void setVerified(double verified) {
		setVerified(Convert.formatBoolean(Double.valueOf(verified).intValue()).booleanValue());
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
			setEligible(Convert.formatDouble(o.toString()).doubleValue());
		
		o = data.get("VERIFIED");
		if (o != null)  {
			double d = Convert.formatDouble(o.toString()).doubleValue();
			if (d > 0) //this allows the value from 'migrate' to persist to new accounts - where the 2nd lookup would return 0 after the first returned 1
				setVerified(Convert.formatDouble(o.toString()).doubleValue());
		}
		
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
	
	@SuppressWarnings("unchecked")
	public Map<String,UserDataVO> getPendingResDirs() {
		return (Map<String,UserDataVO>) user.getAttribute("pendingResDirs");
	}
	
	public void setPendingResDirs(Map<String,UserDataVO> resDirs) {
		user.addAttribute("pendingResDirs", resDirs);
	}
	
	@SuppressWarnings("unchecked")
	public Map<String,UserDataVO> getResDirs() {
		return (Map<String,UserDataVO>) user.getAttribute("resDirs");
	}
	
	public void setResDirs(Map<String,UserDataVO> resDirs) {
		user.addAttribute("resDirs", resDirs);
	}



/****************************************************************************
 * 
 *	ALL METHODS BELOW WERE CREATED BY ECLIPSE TO COMPLETE THE DECORATOR.
 *	THEY SERVE NO PURPOSE OTHERWISE AND CONTAIN NO DSI/CUSTOM CODE.. 
 *	-JM 07-08-2015
 *
 ****************************************************************************/

	@Override
	public boolean equals(Object o) {
		return user.equals(o);
	}

	@Override
	public int hashCode() {
		return user.hashCode();
	}

	@Override
	public String toString() {
		return user.toString();
	}

	@Override
	public void setName(String name) {
		user.setName(name);
	}

	@Override
	public boolean isValidProfile() {
		return user.isValidProfile();
	}

	@Override
	public boolean isUserReachable() {
		return user.isUserReachable();
	}

	@Override
	public void setData(ResultSet rs) {
		user.setData(rs);
	}

	@Override
	public void setData(ActionRequest req) {
		user.setData(req);
	}

	@Override
	public Object getDisplayValue(String field) {
		return user.getDisplayValue(field);
	}

	@Override
	public Map<String, Object> getDataMap(boolean includeNulls) {
		return user.getDataMap(includeNulls);
	}

	@Override
	public Map<String, Object> getDataMap() {
		return user.getDataMap();
	}

	@Override
	public void setDisplayValue(String field, Object value) {
		user.setDisplayValue(field, value);
	}

	@Override
	public void setData(Map<String, Object> data) {
		user.setData(data);
	}

	@Override
	public void addPhone(PhoneVO vo) {
		user.addPhone(vo);
	}

	@Override
	public List<PhoneVO> getPhoneNumbers() {
		return user.getPhoneNumbers();
	}

	@Override
	public void setLocation(Location loc) {
		user.setLocation(loc);
	}

	@Override
	public void setLocation(GeocodeLocation location) {
		user.setLocation(location);
	}

	@Override
	public GeocodeLocation getLocation() {
		return user.getLocation();
	}

	@Override
	public void setPhoneNumbers(List<PhoneVO> phoneNumbers) {
		user.setPhoneNumbers(phoneNumbers);
	}

	@Override
	public String getBestTime() {
		return user.getBestTime();
	}

	@Override
	public Date getBirthDate() {
		return user.getBirthDate();
	}

	@Override
	public java.sql.Date getSQLBirthDate() {
		return user.getSQLBirthDate();
	}

	@Override
	public Integer getBirthYear() {
		return user.getBirthYear();
	}

	@Override
	public String getLanguage() {
		return user.getLanguage();
	}

	@Override
	public void setBestTime(String bestTime) {
		user.setBestTime(bestTime);
	}

	@Override
	public void setBirthDate(Date birthDate) {
		user.setBirthDate(birthDate);
	}

	@Override
	public void setBirthYear(Integer birthYear) {
		user.setBirthYear(birthYear);
	}

	@Override
	public void setLanguage(String language) {
		user.setLanguage(language);
	}

	@Override
	public Integer getGlobalAdminFlag() {
		return user.getGlobalAdminFlag();
	}

	@Override
	public Double getLatitude() {
		return user.getLatitude();
	}

	@Override
	public Double getLongitude() {
		return user.getLongitude();
	}

	@Override
	public Integer getValidEmailFlag() {
		return user.getValidEmailFlag();
	}

	@Override
	public void setGlobalAdminFlag(Integer globalAdminFlag) {
		user.setGlobalAdminFlag(globalAdminFlag);
	}

	@Override
	public void setLatitude(Double latitude) {
		user.setLatitude(latitude);
	}

	@Override
	public void setLongitude(Double longitude) {
		user.setLongitude(longitude);
	}

	@Override
	public void setValidEmailFlag(Integer validEmailFlag) {
		user.setValidEmailFlag(validEmailFlag);
	}

	@Override
	public Integer getAllowCommunication() {
		return user.getAllowCommunication();
	}

	@Override
	public void setAllowCommunication(Integer allowCommunication) {
		user.setAllowCommunication(allowCommunication);
	}

	@Override
	public String getAddress() {
		return user.getAddress();
	}

	@Override
	public String getAddress2() {
		return user.getAddress2();
	}

	@Override
	public String getAuthenticationId() {
		return user.getAuthenticationId();
	}

	@Override
	public String getCity() {
		return user.getCity();
	}

	@Override
	public String getCountryCode() {
		return StringUtil.checkVal(user.getAttribute(RegField.DSI_COUNTRY.toString()), "US");
	}

	@Override
	public String getCounty() {
		return user.getCounty();
	}

	@Override
	public String getEmailAddress() {
		return user.getEmailAddress();
	}

	@Override
	public String getFirstName() {
		return user.getFirstName();
	}

	@Override
	public String getGenderCode() {
		return user.getGenderCode();
	}

	@Override
	public String getLastName() {
		return user.getLastName();
	}

	@Override
	public String getMainPhone() {
		return user.getMainPhone();
	}

	@Override
	public String getWorkPhone() {
		return user.getWorkPhone();
	}

	@Override
	public String getMatchCode() {
		return user.getMatchCode();
	}

	@Override
	public String getMiddleName() {
		return user.getMiddleName();
	}

	@Override
	public String getMobilePhone() {
		return user.getMobilePhone();
	}

	@Override
	public String getPassword() {
		return user.getPassword();
	}

	@Override
	public String getPasswordResetKey() {
		return user.getPasswordResetKey();
	}

	@Override
	public void setPasswordResetKey(String passwordResetKey) {
		user.setPasswordResetKey(passwordResetKey);
	}

	@Override
	public String getPrefixName() {
		return user.getPrefixName();
	}

	@Override
	public String getProfileId() {
		return user.getProfileId();
	}

	@Override
	public String getState() {
		return user.getState();
	}

	@Override
	public String getSuffixName() {
		return user.getSuffixName();
	}

	@Override
	public String getZipCode() {
		return user.getZipCode();
	}

	@Override
	public String getZipSuffixCode() {
		return user.getZipSuffixCode();
	}

	@Override
	public void setAddress(String address) {
		user.setAddress(address);
	}

	@Override
	public void setAddress2(String address2) {
		user.setAddress2(address2);
	}

	@Override
	public void setAuthenticationId(String authenticationId) {
		user.setAuthenticationId(authenticationId);
	}

	@Override
	public void setCity(String city) {
		user.setCity(city);
	}

	@Override
	public void setCountryCode(String country) {
		user.addAttribute(RegField.DSI_COUNTRY.toString(), country);
	}

	@Override
	public void setCounty(String county) {
		user.setCounty(county);
	}

	@Override
	public void setEmailAddress(String emailAddress) {
		user.setEmailAddress(emailAddress);
	}

	@Override
	public void setFirstName(String firstName) {
		user.setFirstName(firstName);
	}

	@Override
	public void setGenderCode(String genderCode) {
		user.setGenderCode(genderCode);
	}

	@Override
	public void setLastName(String lastName) {
		user.setLastName(lastName);
	}

	@Override
	public void setMainPhone(String mainPhone) {
		user.setMainPhone(mainPhone);
	}

	@Override
	public void setMatchCode(String matchCode) {
		user.setMatchCode(matchCode);
	}

	@Override
	public void setMiddleName(String middleName) {
		user.setMiddleName(middleName);
	}

	@Override
	public void setMobilePhone(String mobilePhone) {
		user.setMobilePhone(mobilePhone);
	}

	@Override
	public void setPassword(String password) {
		user.setPassword(password);
	}

	@Override
	public void setPrefixName(String prefixName) {
		user.setPrefixName(prefixName);
	}

	@Override
	public void setProfileId(String profileId) {
		user.setProfileId(profileId);
	}

	@Override
	public void setState(String state) {
		user.setState(state);
	}

	@Override
	public void setSuffixName(String suffixName) {
		user.setSuffixName(suffixName);
	}

	@Override
	public void setZipCode(String zipCode) {
		user.setZipCode(zipCode);
	}

	@Override
	public void setZipSuffixCode(String zipSuffixCode) {
		user.setZipSuffixCode(zipSuffixCode);
	}

	@Override
	public String getBarCodeId() {
		return user.getBarCodeId();
	}

	@Override
	public void setBarCodeId(String id) {
		user.setBarCodeId(id);
	}

	@Override
	public int getCassValidated() {
		return user.getCassValidated();
	}

	@Override
	public void setCassValidated(int id) {
		user.setCassValidated(id);
	}

	@Override
	public Integer getValidAddressFlag() {
		return user.getValidAddressFlag();
	}

	@Override
	public void setValidAddressFlag(Integer validAddressFlag) {
		user.setValidAddressFlag(validAddressFlag);
	}

	@Override
	public String getFullName() {
		return user.getFullName();
	}

	@Override
	public String getAliasName() {
		return user.getAliasName();
	}

	@Override
	public void setAliasName(String aliasName) {
		user.setAliasName(aliasName);
	}

	@Override
	public int getAuthenticationLogId() {
		return user.getAuthenticationLogId();
	}

	@Override
	public void setAuthenticationLogId(int authenticationLogId) {
		user.setAuthenticationLogId(authenticationLogId);
	}

	@Override
	public boolean isAuthenticated() {
		return user.isAuthenticated();
	}

	@Override
	public void setAuthenticated(boolean isAuthenticated) {
		user.setAuthenticated(isAuthenticated);
	}

	@Override
	public boolean isLockedOut() {
		return user.isLockedOut();
	}

	@Override
	public void setLockedOut(boolean isLockedOut) {
		user.setLockedOut(isLockedOut);
	}

	@Override
	public String getAllowCommunicationReason() {
		return user.getAllowCommunicationReason();
	}

	@Override
	public void setAllowCommunicationReason(String reason) {
		user.setAllowCommunicationReason(reason);
	}

	@Override
	public Integer getPasswordResetFlag() {
		return user.getPasswordResetFlag();
	}

	@Override
	public void setPasswordResetFlag(Integer passwordResetFlag) {
		user.setPasswordResetFlag(passwordResetFlag);
	}

	@Override
	public Date getPasswordChangeDate() {
		return user.getPasswordChangeDate();
	}

	@Override
	public void setPasswordChangeDate(Date passwordChangeDate) {
		user.setPasswordChangeDate(passwordChangeDate);
	}

	@Override
	public Long getPasswordAge() {
		return user.getPasswordAge();
	}

	@Override
	public void setPasswordAge(Long passwordAge) {
		user.setPasswordAge(passwordAge);
	}

	@Override
	public Map<String, Object> getAttributes() {
		return user.getAttributes();
	}

	@Override
	public void setAttributes(Map<String, Object> attributes) {
		user.setAttributes(attributes);
	}

	@Override
	public void addAttribute(String key, Object val) {
		user.addAttribute(key, val);
	}

	@Override
	public Object getAttribute(String key) {
		return user.getAttribute(key);
	}

	@Override
	public Object getUserExtendedInfo() {
		return user.getUserExtendedInfo();
	}

	@Override
	public void setUserExtendedInfo(Object userExtendedInfo) {
		user.setUserExtendedInfo(userExtendedInfo);
	}

	@Override
	public AuthenticationType getAuthType() {
		return user.getAuthType();
	}

	@Override
	public void setAuthType(AuthenticationType authType) {
		user.setAuthType(authType);
	}

	@Override
	public void setAuthType(String authType) {
		user.setAuthType(authType);
	}

	@Override
	public String getProfileImage() {
		return user.getProfileImage();
	}

	@Override
	public void setProfileImage(String profileImagePath) {
		user.setProfileImage(profileImagePath);
	}
}
