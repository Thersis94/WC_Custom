package com.ram.action.util;

import java.util.List;
import java.util.Set;

import com.ram.action.user.RAMRoleModule;
import com.ram.datafeed.data.CustomerVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/********************************************************************
 * <b>Title: </b>SecurityUtil.java<br/>
 * <b>Description: </b>Looks at the request data and role data and determines if
 * the user is authorized <br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Jul 31, 2017
 * Last Updated: 
 *******************************************************************/
public class SecurityUtil {
	/**
	 * Enum to hold the Role Information 
	 */
	public enum RAMRoles {
		SITE_ADMINISRATOR("100", "Site Administrator", 100),
		AUDITOR("7f00010171808811df6cdad521171326", "Auditor", 15),
		OEM("c0a80237c5fb9ed0bf6aa0dd5a44c26c", "OEM", 30),
		PROVIDER("c0a80237c5fbdf399dfe510432b2f611", "Provider", 25),
		ASSOCIATE("c0a80237c5fca8c563fa800c1fae463d", "Associate", 20),
		OEM_SALES_REP("e6754a627eb04aaca1be51d7ed1f8d75", "OEM Sales Rep", 35);
		
		private String id;
		private String name;
		private int level;
		
		private RAMRoles(String id, String name, int level) {
			this.id = id;
			this.name= name;
			this.level = level;
		}
		
		public String getName() { return this.name; }
		public String getId() { return this.id; }
		public int getLevel() { return this.level; }
	}

	/**
	 * 
	 */
	public SecurityUtil() {
		super();
	}

	/**
	 * Checks the user's ability to be authorized to perform the transaction
	 * @param role
	 * @param isAdmin
	 * @return
	 */
	@SuppressWarnings({"unchecked" })
	public static boolean isAuthorized(ActionRequest req, Integer oemId, String[] providers) {
		// Get user role
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		boolean isAdmin = (role.getRoleLevel() == 100);
		
		// if the user is an administrator, they are authorized
		if (isAdmin) return true;
		
		// Make sure the user has the admin flag set (for non Site admins)
		if (! Convert.formatBoolean(role.getAttribute(RAMRoleModule.ADMIN_ROLE))) return false;
		
		// Make sure the user has permissions for each of the locations being entered
		Set<Integer> authProviders = (Set<Integer>)role.getAttribute(CustomerVO.CustomerType.PROVIDER.toString()) ;
		Set<Integer> authOEM = (Set<Integer>)role.getAttribute(CustomerVO.CustomerType.OEM.toString()) ;
		
		// Make sure the user has authority to manage the particular provider/.  If not, always report unauth as
		// The user is probably doing something they shouldn't be doing
		for(String id : providers) {
			if (! authProviders.contains(Convert.formatInteger(id))) return false;
		}
		
		// Check to see if the user is authorized to manage an OEM
		if (oemId != null && oemId > 0 && ! authOEM.contains(oemId) || ! role.getRoleName().contains("OEM")) return false;
		
		return true;
	}
	
	/**
	 * Adds the filter for customer.  Determines of the user's role and adds the appropriate filters to the queries, ensuring
	 * that the user will only see the authorized information 
	 * @param req
	 * @param alias
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String addCustomerFilter(ActionRequest req, String alias) {
		// Get user role
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		boolean isAdmin = (role.getRoleLevel() == 100);
		if (isAdmin) return "";
		
		StringBuilder sql = new StringBuilder(128);
		
		// Add the filter for providers
		List<Object> customers = (List<Object>)role.getAttributes().get(CustomerVO.CustomerType.PROVIDER.toString());
		sql.append("and ").append(alias).append(".customer_id in (");
		sql.append(StringUtil.getDelimitedList(customers.toArray(new String[customers.size()]), false, ",")).append(") ");
		
		
		return sql.toString();
	}
	
	/**
	 * Adds a where clause filter for OEMs
	 * @param req
	 * @param alias
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String addOEMFilter(ActionRequest req, String alias) {
		// Get user role
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		boolean isAdmin = (role.getRoleLevel() == 100);
		if (isAdmin) return "";
		
		StringBuilder sql = new StringBuilder(128);
		List<Object> oem = (List<Object>)role.getAttributes().get(CustomerVO.CustomerType.OEM.toString());
		sql.append("and ").append(alias).append(".customer_id in (0").append(oem.isEmpty()? "," : "");
		sql.append(StringUtil.getDelimitedList(oem.toArray(new String[oem.size()]), false, ",")).append(") ");
		
		return sql.toString();
	}
	
	/**
	 * Determines if the role is an OEM
	 * @param roleId
	 * @return
	 */
	public static boolean isOEM(String roleId) {
		if (RAMRoles.OEM.getId().equalsIgnoreCase(roleId)) return true;
		if (RAMRoles.OEM_SALES_REP.getId().equalsIgnoreCase(roleId)) return true;
		
		return false;
	}
	
	/**
	 * Determines if the given rols has providers mapped
	 * @param roleId
	 * @return
	 */
	public static boolean hasProviders(String roleId) {
		if (RAMRoles.SITE_ADMINISRATOR.getId().equalsIgnoreCase(roleId)) return false;
		if (RAMRoles.OEM.getId().equalsIgnoreCase(roleId)) return false;
		
		return true;
	}
}
