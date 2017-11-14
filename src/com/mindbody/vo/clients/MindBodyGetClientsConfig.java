package com.mindbody.vo.clients;

import java.util.Date;

import com.mindbody.MindbodyClientApi.ClientDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodyGetClientsConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages GetClients Endpoint unique Configuration.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 13, 2017
 ****************************************************************************/
public class MindBodyGetClientsConfig extends MindBodyClientConfig {

	private String searchText = "";
	private boolean isProspect;
	private Date lastModifiedDate;

	/**
	 * 
	 */
	public MindBodyGetClientsConfig(MindBodyCredentialVO source, MindBodyCredentialVO user) {
		super(ClientDocumentType.GET_CLIENTS, source, user);
	}

	@Override
	public boolean isValid() {
		return super.isValid() && getUserCredentials() != null && searchText != null;
	}
	/**
	 * @return the searchText
	 */
	public String getSearchText() {
		return searchText;
	}

	/**
	 * @return the isProspect
	 */
	public boolean isProspect() {
		return isProspect;
	}

	/**
	 * @return the lastModifiedDate
	 */
	public Date getLastModifiedDate() {
		return lastModifiedDate;
	}

	/**
	 * @param searchText the searchText to set.
	 */
	public void setSearchText(String searchText) {
		this.searchText = searchText;
	}

	/**
	 * @param isProspect the isProspect to set.
	 */
	public void setProspect(boolean isProspect) {
		this.isProspect = isProspect;
	}

	/**
	 * @param lastModifiedDate the lastModifiedDate to set.
	 */
	public void setLastModifiedDate(Date lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}
}