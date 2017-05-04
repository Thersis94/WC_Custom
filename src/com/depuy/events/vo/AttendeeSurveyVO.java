package com.depuy.events.vo;

import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.security.UserDataVO;

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
public class AttendeeSurveyVO extends UserDataVO {
	private static final long serialVersionUID = 4125971076299968645L;
	private String eventEntryId;
	private String rsvpCode;
	private Map<String, Object> responses = new HashMap<>();

	public AttendeeSurveyVO() {
		super();
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