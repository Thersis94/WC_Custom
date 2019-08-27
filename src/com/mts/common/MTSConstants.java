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
	 * Organiztion ID
	 */
	public static final String ORGANIZATON_ID = "MTS";
	
	/**
	 * Portal Site ID - /portal
	 * @deprecated - only used for initial data import.  If you need it, please dedeprecate. -JM- 08/27/19
	 */
	@Deprecated
	public static final String PORTAL_SITE_ID = "MTS_1";
	
	/**
	 * Subscriber Site ID - the parent site
	 */
	public static final String SUBSCRIBER_SITE_ID = "MTS_2";
	
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
	 * Default oocation of the feature image
	 */
	public static final String DEF_FEATURE_IMG_PATH = "/binary/file_transfer/000/000/feature.png";
	
	/**
	 * 
	 */
	private MTSConstants() {
		// don't need a constructor - static class
	}
}
