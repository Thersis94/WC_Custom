/**
 * 
 */
package com.depuy.events_v2.vo;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.annotations.DataType;
import com.siliconmtn.annotations.Importable;
import com.smt.sitebuilder.action.event.vo.EventRsvpVO;

/*****************************************************************************
<p><b>Title</b>: DePuyEventRsvpVO.java</p>
<p>Stores information submitted for an attendee on DePuy Seminar site</p>
<p>Copyright: Copyright (c) 2014 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author Erik Wingo
@since Oct 15, 2014
***************************************************************************/
public class DePuyEventRsvpVO extends EventRsvpVO {

	private static final long serialVersionUID = 1L;
	private String firstName = null;
	private String lastName = null;
	private String emailAddress = null;
	private String prefix = null;
	private String address1Text = null;
	private String address2Text = null;
	private String city = null;
	private String state = null;
	private String zipCode = null;
	private String phoneNum = null;

	/**
	 * 
	 */
	public DePuyEventRsvpVO() {
		super();
	}

	/**
	 * @param rs
	 */
	public DePuyEventRsvpVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * First Name
	 * @return
	 */
	public String getFirstName(){
		return firstName;
	}
	
	/**
	 * Last Name
	 * @return
	 */
	public String getLastName(){
		return lastName;
	}
	
	/**
	 * Email Address
	 * @return
	 */
	public String getEmailAddress(){
		return emailAddress;
	}
	
	/**
	 * Name prefix (Mr., Dr., etc.)
	 * @return
	 */
	public String getPrefix(){
		return prefix;
	}
	
	/**
	 * @param firstName
	 */
	@Importable( name = "First Name", type = DataType.STRING, isRequired = true)
	public void setFirstName( String firstName ){
		this.firstName = firstName;
	}
	
	/**
	 * 
	 * @param lastName
	 */
	@Importable( name = "Last Name", type = DataType.STRING, isRequired = true)
	public void setLastName( String lastName ){
		this.lastName = lastName;
	}
	
	/**
	 * 
	 * @param emailAddress
	 */
	@Importable( name = "Email Address", type = DataType.STRING, isRequired = true )
	public void setEmailAddress( String emailAddress ){
		this.emailAddress = emailAddress;
	}
	
	/**
	 * 
	 * @param prefix
	 */
	@Importable( name = "Prefix", type = DataType.STRING )
	public void setPrefix(String prefix){
		this.prefix = prefix;
	}
	
	/**
	 * @return the address1Text
	 */
	public String getAddress1Text() {
		return address1Text;
	}

	/**
	 * @param address1Text the address1Text to set
	 */
	@Importable( name = "Address", type=DataType.STRING)
	public void setAddress1Text(String address1Text) {
		this.address1Text = address1Text;
	}

	/**
	 * @return the address2Text
	 */
	public String getAddress2Text() {
		return address2Text;
	}

	/**
	 * @param address2Text the address2Text to set
	 */
	@Importable( name = "Address2", type=DataType.STRING)
	public void setAddress2Text(String address2Text) {
		this.address2Text = address2Text;
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
	@Importable( name = "City", type=DataType.STRING)
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
	@Importable( name = "State", type=DataType.STRING)
	public void setState(String state) {
		this.state = state;
	}

	/**
	 * @return the zipCode
	 */
	public String getZipCode() {
		return zipCode;
	}

	/**
	 * @param zipCode the zipCode to set
	 */
	@Importable( name = "Zip Code", type = DataType.STRING)
	public void setZipCode(String zipCode) {
		this.zipCode = removeTrailingDecimal(zipCode);
	}

	/**
	 * @return the phoneNum
	 */
	public String getPhoneNum() {
		return phoneNum;
	}

	/**
	 * @param phoneNum the phoneNum to set
	 */
	@Importable( name = "Phone Number", type=DataType.STRING)
	public void setPhoneNum(String phoneNum) {
		this.phoneNum = phoneNum;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.event.vo.EventRsvpVO#setCallCenterFlg(int)
	 */
	@Importable( name = "Call Center Flag", type = DataType.INTEGER)
	public void setCallCenterFlg(int callCenterFlg) {
		super.setCallCenterFlg(callCenterFlg);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.event.vo.EventRsvpVO#setEventEntryId(java.lang.String)
	 */
	@Importable(name = "Event Entry Id", type = DataType.STRING)
	public void setEventEntryId(String eventEntryId) {
		super.setEventEntryId(eventEntryId);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.event.vo.EventRsvpVO#setEventRsvpId(java.lang.String)
	 */
	@Importable(name = "Event Rsvp Id", type = DataType.STRING)
	public void setEventRsvpId(String eventRsvpId) {
		super.setEventRsvpId(eventRsvpId);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.event.vo.EventRsvpVO#setFirstCallbackFlg(int)
	 */
	@Importable(name = "First Callback Flag", type = DataType.INTEGER)
	public void setFirstCallbackFlg(int firstCallbackFlg) {
		super.setFirstCallbackFlg(firstCallbackFlg); 
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.event.vo.EventRsvpVO#setGuestsNo(int)
	 */
	@Importable(name = "Guests", type = DataType.INTEGER)
	public void setGuestsNo(int guestsNo) {
		super.setGuestsNo(guestsNo);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.event.vo.EventRsvpVO#setInfoKitFlg(int)
	 */
	@Importable(name = "Info Kit Flag", type = DataType.INTEGER)
	public void setInfoKitFlg(int infoKitFlg) {
		super.setInfoKitFlg(infoKitFlg);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.event.vo.EventRsvpVO#setNotes(java.lang.String)
	 */
	@Importable(name = "Notes", type = DataType.STRING)
	public void setNotes(String notes) {
		super.setNotes(notes);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.event.vo.EventRsvpVO#setOptInFlg(int)
	 */
	@Importable(name = "Opt In Flag", type = DataType.INTEGER)
	public void setOptInFlg(int optInFlg) {
		super.setOptInFlg(optInFlg);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.event.vo.EventRsvpVO#setProfileId(java.lang.String)
	 */
	@Importable(name = "Profile Id", type = DataType.STRING)
	public void setProfileId(String profileId) {
		super.setProfileId(profileId);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.event.vo.EventRsvpVO#setReferral(java.lang.String)
	 */
	@Importable(name = "Referral", type = DataType.STRING)
	public void setReferral(String referral) {
		super.setReferral(referral);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.event.vo.EventRsvpVO#setReminderDt(java.util.Date)
	 */
	@Importable(name = "Reminder Date", type = DataType.DATE)
	public void setReminderDt(Date reminderDt) {
		super.setReminderDt(reminderDt);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.event.vo.EventRsvpVO#setReminderTypeNm(java.lang.String)
	 */
	@Importable(name = "Reminder Type", type = DataType.STRING)
	public void setReminderTypeNm(String reminderTypeNm) {
		super.setReminderTypeNm(reminderTypeNm);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.event.vo.EventRsvpVO#setRsvpStatusFlg(int)
	 */
	@Importable(name = "RSVP Status Flag", type = DataType.INTEGER)
	public void setRsvpStatusFlg(int rsvpStatusFlg) {
		super.setRsvpStatusFlg(rsvpStatusFlg);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.event.vo.EventRsvpVO#setSecondCallbackFlg(int)
	 */
	@Importable(name = "Second Callback Flag", type = DataType.INTEGER)
	public void setSecondCallbackFlg(int secondCallbackFlg) {
		super.setSecondCallbackFlg(secondCallbackFlg);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.event.vo.EventRsvpVO#setCreateDate(java.util.Date)
	 */
	@Importable(name = "Create Date", type = DataType.DATE)
	public void setCreateDate(Date createDate) {
		super.setCreateDate(createDate);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.event.vo.EventRsvpVO#setAttrib1Text(java.lang.String)
	 */
	@Importable(name = "Attribute Text", type = DataType.STRING)
	public void setAttrib1Text(String attrib1Text) {
		super.setAttrib1Text(attrib1Text);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.event.vo.EventRsvpVO#setEventRsvpCd(java.lang.String)
	 */
	@Importable(name = "RSVP Code", type = DataType.STRING)
	public void setEventRsvpCd(String eventRsvpCd) {
		super.setEventRsvpCd( removeTrailingDecimal(eventRsvpCd) );
	}

	/**
	 * Excel numeric values are returned as doubles with a trailing .0 at the end
	 * of zip codes and other numeric fields that we want string values for. 
	 * This removes the trailing decimal value.
	 * @param val
	 * @return
	 */
	private String removeTrailingDecimal( String val ){
		return val.replace( ".0", "");
	}
}
