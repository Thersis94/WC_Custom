package com.mindbody.vo.classes;

import java.util.List;

import com.mindbody.MindBodyClassApi.ClassDocumentType;

/****************************************************************************
 * <b>Title:</b> MindBodyAddClientsToClassConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages AddClientsToClass Endpoint unique Configuration.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 8, 2017
 ****************************************************************************/
public class MindBodyAddClientsToClassConfig extends MindBodyClassConfig {

	private boolean requirePayment;
	private boolean useWaitList;
	private boolean sendEmail;
	private Integer waitListEntryId;
	private Integer clientServiceId;
	/**
	 * @param type
	 * @param sourceName
	 * @param sourceKey
	 * @param siteIds
	 */
	public MindBodyAddClientsToClassConfig(String sourceName, String sourceKey, List<Integer> siteIds) {
		super(ClassDocumentType.ADD_CLIENTS_TO_CLASS, sourceName, sourceKey, siteIds);
	}

	/**
	 * Override root isValid and ensure data contained is correct.
	 */
	@Override
	public boolean isValid() {
		boolean isValid = super.isValid() && !getClientIds().isEmpty() && !getClassIds().isEmpty();

		if(isValid) {
			//If we have a user and are requiring payment, verify we have clientServiceId or fail.
			if(hasUser() && requirePayment && clientServiceId == null) isValid = false;
	
			//If we have a user and are using waitList, verify we have waitListEntryId or fail.
			if(hasUser() && useWaitList && waitListEntryId == null) isValid = false;
		}

		return isValid;
	}

	/**
	 * @return the requirePayment
	 */
	public boolean isRequirePayment() {
		return requirePayment;
	}

	/**
	 * @return the useWaitList
	 */
	public boolean isUseWaitList() {
		return useWaitList;
	}

	/**
	 * @return the sendEmail
	 */
	public boolean isSendEmail() {
		return sendEmail;
	}

	/**
	 * @return the waitListEntryId
	 */
	public Integer getWaitListEntryId() {
		return waitListEntryId;
	}

	/**
	 * @return the clientServiceId
	 */
	public Integer getClientServiceId() {
		return clientServiceId;
	}

	/**
	 * @param requirePayment the requirePayment to set.
	 */
	public void setRequirePayment(boolean requirePayment) {
		this.requirePayment = requirePayment;
	}

	/**
	 * @param useWaitList the useWaitList to set.
	 */
	public void setUseWaitList(boolean useWaitList) {
		this.useWaitList = useWaitList;
	}

	/**
	 * @param sendEmail the sendEmail to set.
	 */
	public void setSendEmail(boolean sendEmail) {
		this.sendEmail = sendEmail;
	}

	/**
	 * @param waitListEntryId the waitListEntryId to set.
	 */
	public void setWaitListEntryId(Integer waitListEntryId) {
		this.waitListEntryId = waitListEntryId;
	}

	/**
	 * @param clientServiceId the clientServiceId to set.
	 */
	public void setClientServiceId(Integer clientServiceId) {
		this.clientServiceId = clientServiceId;
	}
}