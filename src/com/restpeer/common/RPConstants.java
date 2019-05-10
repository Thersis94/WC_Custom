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
		MEMBER("MEMBER", "Member"),
		KITCHEN("KITCHEN", "Commisary / Kitchen"),
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
	 * Custom Dealer Attribute Group Codes
	 */
	public enum AttributeGroupCode {
		ADD_ONS("Facility Add Ons"),
		KITCHEN_INFO("Kitchen Info");
		
		private String codeName;
		private AttributeGroupCode(String codeName) { this.codeName = codeName; }
		public String getCodeName() { return codeName; }
	}
	
	/**
	 * Member Types
	 */
	public enum MemberType {
		KITCHEN(13000, "Kitchen Facility"),
		RESTAURANT_PEER(13001, "Restaurant Peer"),
		CUSTOMER(13002, "Mobile Restaurateur");
		
		private String memberName;
		private int dealerId;
		MemberType(int dealerId, String memberName) { 
			this.memberName = memberName;
			this.dealerId = dealerId;
		}

		public String getMemberName() {	return memberName; }
		public int getDealerId() {return dealerId; }
	}

	/**
	 * Site org ID
	 */
	public static final String ORGANIZATON_ID = "REST_PEER";
	
	/**
	 * Has Schedule Product Attribute Id
	 */
	public static final String HAS_SCHEDULE = "RP_HAS_SCHEDULE";

	private RPConstants() {
		//don't need a constructor - static class
	}
}
