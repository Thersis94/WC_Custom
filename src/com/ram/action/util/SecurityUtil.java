package com.ram.action.util;

// JDK 1.8
import java.util.List;
import java.util.Set;

// RAM Data Feed
import com.ram.action.user.RAMRoleModule;
import com.ram.datafeed.data.CustomerVO;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
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
	 * Enum to hold the User Role Information 
	 */
	private enum UserRole {
		SITE_ADMINISTRATOR("100", "Site Administrator", 100),
		AUDITOR("7f00010171808811df6cdad521171326", "Auditor", 15),
		OEM("c0a80237c5fb9ed0bf6aa0dd5a44c26c", "OEM", 30),
		PROVIDER("c0a80237c5fbdf399dfe510432b2f611", "Provider", 25),
		ASSOCIATE("c0a80237c5fca8c563fa800c1fae463d", "Associate", 20),
		OEM_SALES_REP("E6754A627EB04AACA1BE51D7ED1F8D75", "OEM Sales Rep", 35);
		
		// Member Variables
		private String id;
		private String name;
		private int level;
		
		/**
		 * Constructor for the enum
		 * @param id
		 * @param name
		 * @param level
		 */
		private UserRole(String id, String name, int level) {
			this.id = id;
			this.name= name;
			this.level = level;
		}
		
		// Getters and setters for the vars
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
	 * Determines if the user has admin privileges
	 * @param req
	 * @return
	 */
	public static boolean hasAdminFlag(ActionRequest req) {
		// Get user role
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		if(role.getRoleLevel() == 100) return false;

		return Convert.formatBoolean(role.getAttribute(RAMRoleModule.ADMIN_ROLE));
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
		return (oemId != null && oemId > 0 && ! authOEM.contains(oemId) || ! role.getRoleName().contains("OEM"));

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
		Set<Object> customers = (Set<Object>)role.getAttributes().get(CustomerVO.CustomerType.PROVIDER.toString());
		sql.append("and ").append(alias).append(StringUtil.isEmpty(alias) ? "" : ".").append("customer_id in (");
		sql.append(StringUtil.getToString(customers.toArray(new Object[customers.size()]), false, false,",")).append(") ");
		
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
	 * Determines if the given roles has providers mapped
	 * @param roleId
	 * @return
	 */
	public static boolean hasProviders(String roleId) {
		return !(UserRole.SITE_ADMINISTRATOR.getId().equalsIgnoreCase(roleId) || UserRole.OEM.getId().equalsIgnoreCase(roleId)); 
	}
	
	/**
	 * Creates a filter to filter the roles based upon role
	 * @param req
	 * @return
	 */
	public static String getRoleFilter(ActionRequest req) {
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		StringBuilder sql = new StringBuilder(64);
		sql.append(" and role_order_no > 10 ");
		
		// No filter for aditors, asscoiates and admins
		if (isAdministratorRole(role.getRoleId()) || isAssociateRole(role.getRoleId()) || isAuditorRole(role.getRoleId())) 
			return sql.toString();
		
		// Limit to OEM Roles
		if (isOEMGroup(role.getRoleId())) {
			String[] roles = new String[] { UserRole.OEM.getId(), UserRole.OEM.getId() };
			sql.append("and role_id in (").append(StringUtil.getDelimitedList(roles, true, ",")).append(") ");
			return sql.toString();
		}
		
		sql.append(" and role_id = ").append(StringUtil.checkVal(UserRole.PROVIDER.getId(), true));
		return  sql.toString();
	}
	
	// ******* Define wrappers for the various roles
	/**
	 * Returns an SBUserRole based upon the role id passed 
	 * @param roleId
	 * @return
	 */
	public SBUserRole getRoleInfo(String roleId) {
		for(UserRole r : UserRole.values()) {
			if (r.getId().equals(roleId)) return new SBUserRole(r.getId(), r.getName(), r.getLevel());
		}
		
		return null;
	}
	
	/**
	 * Determines if the role is an OEM
	 * @param roleId
	 * @return
	 */
	public static boolean isOEMGroup(String roleId) {
		return (UserRole.OEM.getId().equalsIgnoreCase(roleId)|| UserRole.OEM_SALES_REP.getId().equalsIgnoreCase(roleId)); 
	}
	
	/**
	 * Determines if the user role is an OEM
	 * @param roleId
	 * @return
	 */
	public static boolean isOEMRole(String roleId) { return UserRole.OEM.getId().equalsIgnoreCase(roleId);	}
	public static String getOEMRoleId() { return UserRole.OEM.getId(); }
	
	/**
	 * Determines if the user role is an OEM Sales Rep
	 * @param roleId
	 * @return
	 */
	public static boolean isOEMSalesRepRole(String roleId) { return UserRole.OEM_SALES_REP.getId().equalsIgnoreCase(roleId); }
	public static String getOEMSalesRepRoleId() { return UserRole.OEM_SALES_REP.getId(); }
	
	/**
	 * Determines if the user role is a provider
	 * @param roleId
	 * @return
	 */
	public static boolean isProviderRole(String roleId) { return UserRole.PROVIDER.getId().equalsIgnoreCase(roleId); }
	public static String getProviderRoleId() { return UserRole.PROVIDER.getId(); }
	
	/**
	 * Determines if the user role is an auditor
	 * @param roleId
	 * @return
	 */
	public static boolean isAuditorRole(String roleId) { return UserRole.AUDITOR.getId().equalsIgnoreCase(roleId); }
	public static String getAuditorRoleId() { return UserRole.AUDITOR.getId(); }
	
	/**
	 * Determines if the user role is an asscoiate
	 * @param roleId
	 * @return
	 */
	public static boolean isAssociateRole(String roleId) { return UserRole.ASSOCIATE.getId().equalsIgnoreCase(roleId); }
	public static String getAccociateRoleId() { return UserRole.ASSOCIATE.getId(); }
	
	/**
	 * Determines if the user role is an asscoiate
	 * @param roleId
	 * @return
	 */
	public static boolean isAdministratorRole(String roleId) { return UserRole.SITE_ADMINISTRATOR.getId().equalsIgnoreCase(roleId); }
	public static String getAdministratorRoleId() { return UserRole.SITE_ADMINISTRATOR.getId(); }
}
