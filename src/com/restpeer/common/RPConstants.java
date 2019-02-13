package com.restpeer.common;

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
public class RPConstants {
		
	/**
	 * Defines the roles used by this site
	 */
	public enum RPRole {
		PATRON("MEMBER", "Member"),
		TOUR("KITCHEN", "Commisary / Kitchen"),
		ADMIN("100", "Site Administrators");
		
		private String roleId;
		private String roleName;
		RPRole(String roleId, String roleName) { 
			this.roleId = roleId; 
			this.roleName = roleName;
		}
		public String getRoleId() {	return roleId; }
		public String getRoleName() {	return roleName; }
	}
	
	/**
	 * 
	 */
	public enum DataType {
		LIST("List of Items"),
		ITEM ("Single Item");
		
		private String typeName;
		DataType(String typeName) { 
			this.typeName = typeName;
		}

		public String getTypeName() {	return typeName; }
	}

	private RPConstants() {
		//don't need a constructor - static class
	}
}
