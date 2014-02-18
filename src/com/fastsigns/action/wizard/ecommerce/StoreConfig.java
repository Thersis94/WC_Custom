package com.fastsigns.action.wizard.ecommerce;

/****************************************************************************
 * <b>Title</b>: StoreConfig.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Template for Storing all necessary information that changes 
 * between countries.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Dec 2013<p/>
 ****************************************************************************/
public class StoreConfig {

	private String eCommId = null;
	private String storeId = null;
	private String sfsId = null;
	private String registeredRoleId = null;
	private String orgPrefix = null;
	private String galleryId = null;
	private String logInHeaderId = null;
	private String anonHeaderId = null;
	private String searchId = null;
	public StoreConfig(){
		
	}
	
	/**
	 * @return the eCommId
	 */
	public String getECommId() {
		return eCommId;
	}
	/**
	 * @param eCommId the eCommId to set
	 */
	public void setECommId(String eCommId) {
		this.eCommId = eCommId;
	}
	/**
	 * @return the storeId
	 */
	public String getStoreId() {
		return storeId;
	}
	/**
	 * @param storeId the storeId to set
	 */
	public void setStoreId(String storeId) {
		this.storeId = storeId;
	}
	/**
	 * @return the sfsId
	 */
	public String getSfsId() {
		return sfsId;
	}
	/**
	 * @param sfsId the sfsId to set
	 */
	public void setSfsId(String sfsId) {
		this.sfsId = sfsId;
	}
	/**
	 * @return the registeredRoleId
	 */
	public String getRegisteredRoleId() {
		return registeredRoleId;
	}
	/**
	 * @param registeredRoleId the registeredRoleId to set
	 */
	public void setRegisteredRoleId(String registeredRoleId) {
		this.registeredRoleId = registeredRoleId;
	}

	public void setOrgPrefix(String orgPrefix) {
		this.orgPrefix = orgPrefix;
	}

	public String getOrgPrefix() {
		return orgPrefix;
	}
	
	/**
	 * We write the js tags to the pages script field. 
	 * @return
	 */
	public String getJsText() {
		StringBuilder sb = new StringBuilder();
		sb.append("<script type='text/javascript' src='/binary/themes/FAST_SIGNS_2/scripts/bootstrap.js'></script>");
		sb.append("<link href='/binary/themes/FAST_SIGNS_2/scripts/bootstrap.css' type='text/css' rel='stylesheet'/>");
		return sb.toString();
	}

	public void setGalleryId(String galleryId) {
		this.galleryId = galleryId;
	}

	public String getGalleryId() {
		return galleryId;
	}

	public void setLogInHeaderId(String logInHeaderId) {
		this.logInHeaderId = logInHeaderId;
	}

	public String getLogInHeaderId() {
		return logInHeaderId;
	}

	public void setAnonHeaderId(String anonHeaderId) {
		this.anonHeaderId = anonHeaderId;
	}

	public String getAnonHeaderId() {
		return anonHeaderId;
	}

	public void setSearchId(String searchId) {
		this.searchId = searchId;
	}

	public String getSearchId() {
		return searchId;
	}
	
}
