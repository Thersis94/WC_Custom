package com.ansmed.sb.security;

// JDK 1.4.2
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.common.constants.ErrorCode;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.security.AbstractRoleModule;
import com.siliconmtn.security.AuthorizationException;
import com.siliconmtn.security.UserRoleVO;
import com.siliconmtn.security.UserRoleContainer;
import com.siliconmtn.util.StringUtil;

// SB Libs
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.common.constants.ErrorCodes;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SBUserRoleContainer;
import com.smt.sitebuilder.security.SecurityController;

//Log4J API
import org.apache.log4j.Logger;

/****************************************************************************
 * <b>Title</b>: DBRoleModule.java<p/>
 * <b>Description: Implements the AbstractRoleModule to retrieve the user
 * roles in the database.  A database connection is required to be added
 * to the initVals Map using the DBRoleModule.DB_CONN parameter as the key</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2005<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 1.0
 * @since Jul 28, 2005
 ****************************************************************************/
public class AnsRoleModule extends AbstractRoleModule {
	public static final String ANS_REGION_ID = "ansRegionId";
	public static final String ANS_AREA_ID = "ansAreaId";
	
	/**
	 * Requires a database connection be assigned to the initialization Collection
	 * using the DB_CONN string value as the Map key
	 */
	public static final String DB_CONN = GlobalConfig.KEY_DB_CONN;
	public static final String HTTP_REQUEST = GlobalConfig.HTTP_REQUEST;
	private static final Logger log = Logger.getLogger(AnsRoleModule.class);
	
	public AnsRoleModule() {
		super();
	}
	
	/**
	 * @param init Map of init objects used by the class.
	 */
	public AnsRoleModule(Map<String, Object> init) {
		super(init);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractRoleModule#getUserRoles(java.lang.String)
	 */
	public UserRoleVO getUserRole(String profileId, String siteId)
	throws AuthorizationException {
		SBUserRole vo = new SBUserRole();
		
		// check authentication ID to determine if site user, corporate user/sales rep
		if (authenticationId != null) {
			// check to see if this was a site user login
			if (this.isSiteUser(authenticationId)) {
				vo = this.getSiteUserRole(profileId, siteId);
			} else {
				// this was either a rep or corporate user login via AD
				vo = this.getRepRole(profileId, siteId);
			}
		} else {
			log.error("Error: Authentication ID is null...throwing AuthorizationException.");
			throw new AuthorizationException(ErrorCodes.ERR_NOT_AUTHORIZED);
		}
		
		return vo;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractRoleModule#getUserSiteRole(java.lang.String)
	 */
	public UserRoleContainer getUserRoles(String authId)
	throws AuthorizationException {
		SBUserRole vo = null;
		UserRoleContainer sites = new UserRoleContainer();
		
		// Get the database Connection
		Connection dbConn = (Connection)initVals.get(DB_CONN);
		
		StringBuffer sb = new StringBuffer();
        sb.append("select b.site_id, a.role_nm, a.role_order_no, b.profile_id, ");
        sb.append("c.site_nm, c.organization_id, b.status_id, b.role_expire_dt ");
        sb.append("from role a, profile_role b, site c, authentication d ");
        sb.append("where a.role_id=b.role_id and b.site_id=c.site_id ");
        sb.append("and d.authentication_id=? ");
		log.debug("Roles SQL: " + sb.toString() + " - " + authId);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, authId);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				vo = new SBUserRole();
				vo.setSiteId(rs.getString(1));
				vo.setRoleName(rs.getString(2));
				vo.setRoleLevel(rs.getInt(3));
				vo.setProfileId(rs.getString(4));
                vo.setSiteName(rs.getString(5));
				vo.setOrganizationId(rs.getString(6));
				vo.setStatusId(rs.getInt(7));
				vo.setRoleExpireDate(rs.getDate(8));
				
				// Add the role to the container
				sites.addRole(vo.getSiteId(), vo);
			}
		} catch (SQLException sqle) {
			throw new AuthorizationException(ErrorCode.DB_CONN_ERROR,sqle);
		} finally {
        	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
			
			try {
				dbConn.close();
			} catch (SQLException e) {}
		}

		return sites;
	}
	
	
	public SBUserRoleContainer adminAuthorized(String authId) 
	throws AuthorizationException {
		log.debug("Starting admin Authorized ..." + authId);
		if (authId == null || authId.length() == 0) 
			throw new AuthorizationException(ErrorCode.DATA_NULL);
		
		// Get the database Connection
		Connection dbConn = (Connection) initVals.get(DB_CONN);
		int count = 0;
		log.debug("starting to run roles");
		SBUserRole vo = null;
		SBUserRoleContainer roles = new SBUserRoleContainer();
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT organization_id, 0 AS global_admin_flg, b.profile_id ");
		sb.append("FROM organization_role a, PROFILE b ");
		sb.append("WHERE b.profile_id = a.profile_id ");
		sb.append("AND b.authentication_id = ? ");
		sb.append("UNION ");
		sb.append("SELECT '', global_admin_flg, profile_id ");
		sb.append("FROM PROFILE ");
		sb.append("WHERE authentication_id = ? ");
		sb.append("ORDER BY global_admin_flg DESC");
		log.debug("Roles SQL: " + sb.toString() + " - " + authId);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, authId);
			ps.setString(2, authId);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				count ++;
				vo = new SBUserRole();
				vo.setOrganizationId(rs.getString(1));
				vo.setRoleName("ADMIN");
				vo.setRoleLevel(SecurityController.ADMIN_ROLE_LEVEL);
				vo.setProfileId(rs.getString(3));
				
				int global = rs.getInt(2);
				if (global == 1) roles.setGlobalAdmin(true);
				
				roles.addRole(vo.getOrganizationId(), vo);
			}
		} catch (SQLException sqle) {
			throw new AuthorizationException(ErrorCode.DB_CONN_ERROR,sqle);
		} finally {
        	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
			
			try {
				dbConn.close();
			} catch (SQLException e) {}
		}
		
		if (count > 0) roles.setAuthorized(true);
		return roles;
	}
	
	/**
	 * Queries authentication table for authentication ID value passed in.  If 
	 * authentication ID exists then this is a site user and a value of true is
	 * returned.
	 * @param authId
	 * @return boolean value representing whether or not this user is a site user.
	 */
	private boolean isSiteUser(String authId) {
		boolean isSiteUser = false;
		
		Connection dbConn = (Connection)initVals.get(DB_CONN);
		
		StringBuffer sb = new StringBuffer();
		sb.append("select a.authentication_id from authentication a ");
        sb.append("where a.authentication_id = ? ");
		log.info("Retrieving user's authentication ID: " + sb.toString() + " - " + authId);
        PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, authId);
			
			ResultSet rs = ps.executeQuery();
			if (rs.next()) isSiteUser = true;
		} catch (SQLException sqle) {
			log.debug("SQL error: " + sqle);
		} finally {
        	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
		}
		log.debug("isSiteUser: " + isSiteUser);
		return isSiteUser;
	}
	
	/**
	 * Retrieves role for a site user.
	 * @param profileId
	 * @param siteId
	 * @return SBUserRole
	 * @throws AuthorizationException
	 */
	private SBUserRole getSiteUserRole(String profileId, String siteId)
	throws AuthorizationException {
		
		log.debug("Retrieving site user role...");
		
		SBUserRole vo = new SBUserRole();
		
		// Get the database Connection
		Connection dbConn = (Connection)initVals.get(DB_CONN);
		
		StringBuffer sb = new StringBuffer();
		sb.append("select b.site_id, a.role_nm, a.role_order_no, b.profile_id, ");
		sb.append("c.site_nm, c.organization_id, a.role_id, b.status_id, profile_role_id, ");
		sb.append("role_expire_dt from role a inner join profile_role b on a.role_id = b.role_id ");
		sb.append("inner join site c on b.site_id = c.site_id ");
        sb.append("where b.profile_id = ? and b.site_id = ? ");
        sb.append("order by c.site_nm");
		log.info("Retrieving site user role: " + sb.toString() + " - " + siteId + " - " + profileId);
        PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, profileId);
			ps.setString(2, siteId);
			
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				vo.setSiteId(rs.getString(1));
				vo.setRoleName(rs.getString(2));
				vo.setRoleLevel(rs.getInt(3));
				vo.setProfileId(rs.getString(4));
                vo.setSiteName(rs.getString(5));
                vo.setOrganizationId(rs.getString(6));
                vo.setRoleId(rs.getString(7));
                vo.setStatusId(rs.getInt(8));
                vo.setProfileRoleId(rs.getString(9));
                vo.setRoleExpireDate(rs.getDate(10));
			}
		} catch (SQLException sqle) {
			log.debug("SQL error: " + sqle);
			throw new AuthorizationException(ErrorCode.DB_CONN_ERROR,sqle);
		} finally {
        	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
		}
		
		return vo;
	}
	
	/**
	 * Retrieves the role assigned to a sales rep or a corporate user
	 * @param profileId
	 * @param siteId
	 * @return SBUserRole object containing the user's role information.
	 */
	private SBUserRole getRepRole(String profileId, String siteId) {
		
		log.debug("Retrieving sales rep or corporate user role...");
		
		// Get the database Connection
		Connection dbConn = (Connection) initVals.get(DB_CONN);
		String schema = (String) initVals.get(Constants.CUSTOM_DB_SCHEMA);
		SBUserRole vo = new SBUserRole();;
		String orgId = (String) initVals.get("ORGANIZATION_ID");
		StringBuffer sql = new StringBuffer();
		sql.append("select role_nm, role_order_no, ans_login_id, a.role_id, ");
		sql.append("organization_id, sales_rep_id, profile_id, area_id, ");
		sql.append("b.region_id, atm_rep_id from role a inner join ");
		sql.append(schema).append("ans_sales_rep b on a.role_id = b.role_id ");
		sql.append("left outer join ");
		sql.append(schema).append("ans_sales_region c on b.region_id = c.region_id ");
		sql.append("where organization_id = ? and profile_id = ? ");
		log.debug("Rep Role sql: " + sql + "|" + orgId + "|" + profileId);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, orgId); 
			ps.setString(2, profileId);
			
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				// If the user is an ATM(TM1), use the profileId of the TM(TM3) for whom 
				// they work
				if (StringUtil.checkVal(rs.getString("atm_rep_id")).length() > 0)
					profileId = rs.getString("atm_rep_id");
				
				vo.setSiteId(siteId);
				vo.setRoleName(rs.getString(1));
				vo.setRoleLevel(rs.getInt(2));
                vo.setRoleId(rs.getString(4));
                vo.setOrganizationId(rs.getString(5));
                vo.setProfileRoleId(rs.getString(6));
                vo.setProfileId(profileId);
                vo.setStatusId(20);
                
                // Add the region and area IDs
                log.debug("Adding Attributes: " + rs.getString("area_id") + "|" + rs.getString("region_id"));
                vo.addAttribute(ANS_AREA_ID, rs.getString("area_id"));
                vo.addAttribute(ANS_REGION_ID, rs.getString("region_id"));
                log.debug("Retrieved a sales rep role...");
			} else {
				// If the user isn't a sales rep, assign them as a registered user
				vo.setSiteId(siteId);
				vo.setRoleName("Registered User");
				vo.setRoleId("10");
				vo.setStatusId(20);
				vo.setRoleLevel(Integer.valueOf(10));
				log.debug("Retrieved a default role for corporate user.");
			}
		} catch (SQLException sqle) {
			log.error(ErrorCode.DB_CONN_ERROR,sqle);
			return null;
		} finally {
        	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
        	
			try {
				dbConn.close();
			} catch (SQLException e) {}
		}
		return vo;
	}

}
