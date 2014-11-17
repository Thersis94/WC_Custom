package com.depuy.events_v2.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/****************************************************************************
 * <b>Title</b>: AttendeeSurveyVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Erik Wingo
 * @version 1.0
 * @since Nov 11, 2014
 ****************************************************************************/
public class AttendeeSurveyVO implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String eventEntryId = null;
	private String profileId = null;
	private String customerId = null;
	private String firstName = null;
	private String lastName = null;
	private String rsvpCode = null;
	private List<String> questionList = new ArrayList<>();
	private List<String> answerList = new ArrayList<>();

	public AttendeeSurveyVO(){}
	
	/**
	 * @return the questionList
	 */
	public List<String> getQuestionList() {
		return questionList;
	}

	/**
	 * @param questionList the questionList to set
	 */
	public void setQuestionList(List<String> questionList) {
		this.questionList = questionList;
	}

	/**
	 * @return the answerList
	 */
	public List<String> getAnswerList() {
		return answerList;
	}

	/**
	 * @param answerList the answerList to set
	 */
	public void setAnswerList(List<String> answerList) {
		this.answerList = answerList;
	}

	/**
	 * @return the rsvpCode
	 */
	public String getRsvpCode() {
		return rsvpCode;
	}

	/**
	 * @param rsvpCode the rsvpCode to set
	 */
	public void setRsvpCode(String rsvpCode) {
		this.rsvpCode = rsvpCode;
	}

	/**
	 * @return the eventEntryId
	 */
	public String getEventEntryId() {
		return eventEntryId;
	}

	/**
	 * @param eventEntryId the eventEntryId to set
	 */
	public void setEventEntryId(String eventEntryId) {
		this.eventEntryId = eventEntryId;
	}

	/**
	 * @return the profileId
	 */
	public String getProfileId() {
		return profileId;
	}

	/**
	 * @param profileId the profileId to set
	 */
	public void setProfileId(String profileId) {
		this.profileId = profileId;
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
	 * @return the customerId
	 */
	public String getCustomerId() {
		return customerId;
	}

	/**
	 * @param customerId the customerId to set
	 */
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}
}
