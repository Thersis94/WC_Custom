package com.mts.common;

/****************************************************************************
 * <b>Title</b>: MTSConstants.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Commonly Used Constants for the MTS Portal Project
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since April 18, 2019
 * @updates:
 ****************************************************************************/
public class MTSConstants {
		
	/**
	 * Defines the roles used by this site
	 */
	public enum MTSRole {
		SUBSCRIBER("SUBSCRIBER", "MTS Subscriber"),
		AUTHOR("AUTHOR", "MTS Author"),
		ADMIN("100", "Site Administrators");
		
		private String roleId;
		private String roleName;
		MTSRole(String roleId, String roleName) { 
			this.roleId = roleId; 
			this.roleName = roleName;
		}
		public String getRoleId() {	return roleId; }
		public String getRoleName() {	return roleName; }
	}
	
	/**
	 * Site org ID
	 */
	public static final String ORGANIZATON_ID = "MTS";
	
	/**
	 * Portal Site ID
	 */
	public static final String PORTAL_SITE_ID = "MTS_2";
	
	/**
	 * Portal Site ID
	 */
	public static final String SUBSCRIBER_SITE_ID = "MTS_1";
	
	/**
	 * Root folder to utilize when creating a new publication as all articles
	 * will go in the same folder
	 */
	public static final String ROOT_FOLDER_ID = "MTS_PUBLICATIONS";
	
	/**
	 * Root folder path to utilize when creating a new publication as all articles
	 * will go in the same folder
	 */
	public static final String ROOT_FOLDER_PATH = "/content/pubs/";
	
	/**
	 * 
	 */
	private MTSConstants() {
		// don't need a constructor - static class
	}
}
