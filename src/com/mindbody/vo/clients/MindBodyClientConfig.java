package com.mindbody.vo.clients;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.mindbody.MindBodyClientApi.ClientDocumentType;
import com.mindbody.vo.MindBodyConfig;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodyClientVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Common Configuration for Client API Calls.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 6, 2017
 ****************************************************************************/
public class MindBodyClientConfig extends MindBodyConfig {

	private ClientDocumentType type;
	private List<String> clientIds;
	private List<Integer> locationIds;
	private Date startDate;
	private Date endDate;

	/**
	 * @param sourceName
	 * @param sourceKey
	 * @param siteIds
	 */
	public MindBodyClientConfig(ClientDocumentType type, MindBodyCredentialVO source, MindBodyCredentialVO user) {
		super(source, user);
		this.type = type;
		this.clientIds = new ArrayList<>();
		this.locationIds = new ArrayList<>();
	}

	/**
	 * @return the type
	 */
	public ClientDocumentType getType() {
		return type;
	}

	/**
	 * @param type the type to set.
	 */
	public void setType(ClientDocumentType type) {
		this.type = type;
	}

	/**
	 * @return the clientId
	 */
	public List<String> getClientIds() {
		return clientIds;
	}

	/**
	 * @return the locationId
	 */
	public List<Integer> getLocationIds() {
		return locationIds;
	}

	/**
	 * @param clientId the clientId to set.
	 */
	public void setClientId(List<String> clientIds) {
		this.clientIds = clientIds;
	}

	/**
	 *
	 * @param clientId
	 */
	public void addClientId(String clientId) {
		this.clientIds.add(clientId);
	}

	/**
	 * @param locationId the locationId to set.
	 */
	public void setLocationIds(List<Integer> locationIds) {
		this.locationIds = locationIds;
	}

	public void addLocationId(Integer locationId) {
		this.locationIds.add(locationId);
	}

	/**
	 * @return the startDate
	 */
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * @return the endDate
	 */
	public Date getEndDate() {
		return endDate;
	}

	/**
	 * @param startDate the startDate to set.
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * @param endDate the endDate to set.
	 */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	/**
	 * Retrieves the first client Id off ClientIds list.
	 * @return
	 */
	public String getClientId() {
		return getClientIds().get(0);
	}
}