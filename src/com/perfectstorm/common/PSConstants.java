package com.perfectstorm.common;

/****************************************************************************
 * <b>Title</b>: PSConstants.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Commonly Used Constants for the Perfect Storm Portal Project
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 4, 2019
 * @updates:
 ****************************************************************************/
public class PSConstants {
		
	/**
	 * Defines the roles used by this site
	 */
	public enum PSRole {
		PATRON("PATRON", "Subscriber"),
		TOUR("TOUR", "Tour Management"),
		VENUE("VENUE", "Vendor Management"),
		ADMIN("100", "Site Administrators");
		
		private String roleId;
		private String roleName;
		PSRole(String roleId, String roleName) { 
			this.roleId = roleId; 
			this.roleName = roleName;
		}
		public String getRoleId() {	return roleId; }
		public String getRoleName() {	return roleName; }
	}

	private PSConstants() {
		//don't need a constructor - static class
	}
}
