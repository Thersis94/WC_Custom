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
		PUBLIC("0"),
		REGISTERED("10"),
		ADMIN("100");
		
		private String roleId;
		PSRole(String roleId) { this.roleId = roleId; }
		public String getRoleId() {	return roleId; }
	}

	private PSConstants() {
		//don't need a constructor - static class
	}
}
