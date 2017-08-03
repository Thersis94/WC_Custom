package com.ram.action.user;

// JDK 1.8.x
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ram.datafeed.data.CustomerVO;
// SMT Base Libs
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.security.AuthorizationException;
import com.siliconmtn.util.Convert;
// WC Libs
import com.smt.sitebuilder.security.DBRoleModule;
import com.smt.sitebuilder.security.SBUserRole;

/********************************************************************
 * <b>Title: </b>RAMRoleModule.java<br/>
 * <b>Description: </b>Custom ram role module to assign the OEMS and providers
 * assigned to a given user to the attributes for the role<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Jul 27, 2017
 * Last Updated: 
 *******************************************************************/
public class RAMRoleModule extends DBRoleModule {
	
	/**
	 * Constant utilized for storing whether or not a user has the admin role assigned
	 */
	public static final String ADMIN_ROLE = "admin_role";

	/**
	 * 
	 */
	public RAMRoleModule() {
		super();
	}

	/**
	 * @param init
	 */
	public RAMRoleModule(Map<String, Object> init) {
		super(init);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.security.DBRoleModule#getUserRole(java.lang.String, java.lang.String)
	 */
	@Override
	public SBUserRole getUserRole(String profileId, String siteId) throws AuthorizationException {
		SBUserRole role = super.getUserRole(profileId, siteId);
		role.getAttributes().putAll(getUserInfo(role.getProfileRoleId()));
		role.getAttributes().put(ADMIN_ROLE, hasAdmin(role));
		
		log.debug(role.getAttributes());
		return role;
	}

	/**
	 * Gets the list of providers, IDNs, OEMs etc ... for the given user role
	 * @param profileRoleId
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map<String, Object> getUserInfo(String profileRoleId) {
		Map<String, Set<Integer>> data = new HashMap<>();
		data.put(CustomerVO.CustomerType.OEM.toString(), new HashSet<Integer>());
		data.put(CustomerVO.CustomerType.PROVIDER.toString(), new HashSet<Integer>());
		data.put(CustomerVO.CustomerType.RAM.toString(), new HashSet<Integer>());
		Object schema = initVals.get(Constants.CUSTOM_DB_SCHEMA);
		Connection dbConn = (Connection) initVals.get(DB_CONN);
		
		// Build the sql
		StringBuilder sql = new StringBuilder(256);
		sql.append("select b.customer_id, c.customer_type_id ");
		sql.append("from ").append(schema).append("ram_user_role a ");
		sql.append("inner join ").append(schema).append("ram_user_role_customer_xr b on a.user_role_id = b.user_role_id ");
		sql.append("inner join ").append(schema).append("ram_customer c on b.customer_id = c.customer_id ");
		sql.append("where profile_role_id = ? order by customer_type_id ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, profileRoleId);
			
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				data.get(rs.getString(2)).add(rs.getInt(1));
			}
		} catch(Exception e) {
			log.info("Unable to retrieve user attributes", e);
		}
		
		return (Map)data;
	}
	
	/**
	 * Checks to see if the user has admin abilities
	 * @param profile_role_id
	 * @return
	 */
	public boolean hasAdmin(SBUserRole role) {
		if (role.getRoleLevel() == 100) return true;
		
		// Build the SQL
		Connection dbConn = (Connection) initVals.get(DB_CONN);
		StringBuilder sql = new StringBuilder(128);
		sql.append("select admin_flg from ").append(initVals.get(Constants.CUSTOM_DB_SCHEMA)).append("ram_user_role ");
		sql.append("where profile_role_id = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, role.getProfileRoleId());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) return Convert.formatBoolean(rs.getInt(1));
		} catch(SQLException sqle) {
			log.error("unable to retrieve admin flag", sqle);
		}
		
		return false;
	}
}
