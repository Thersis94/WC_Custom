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
		SUBSCRIBER("MEMBER", "Member"),
		AUTHOR("KITCHEN", "Commisary / Kitchen"),
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

	private MTSConstants() {
		//don't need a constructor - static class
	}
	
	/**
	 * Site org ID
	 */
	public static final String ORGANIZATON_ID = "MTS";
}
