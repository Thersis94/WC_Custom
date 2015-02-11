package com.depuy.events_v2.vo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/****************************************************************************
 * <b>Title</b>: AttendeeSurveyVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Erik Wingo
 * @version 1.0
 * @since Nov 11, 2014
 * @updates
 * 		JM 1.27.15 - converted separate Lists of questions & answers to a Map(k,v) pairs.
 ****************************************************************************/
public class AttendeeSurveyVO implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String eventEntryId = null;
	private String profileId = null;
	private String firstName = null;
	private String lastName = null;
	private String rsvpCode = null;
	private Map<String, Object> responses = new HashMap<>();

	public AttendeeSurveyVO() {
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

	public Map<String, Object> getResponses() {
		return responses;
	}
	
	public void setResponses(Map<String, Object> responses) {
		this.responses = responses;
	}
	
	public void addResponse(String cd, String resp) {
		responses.put(cd, resp);
	}
}