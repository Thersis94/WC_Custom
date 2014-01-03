package com.ansmed.sb.security;

import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.security.SBUserRole;

/*****************************************************************************
 <p><b>Title</b>: ANSRoleFiler.java</p>
 <p>Description: <b/>Adds filters and control to SQL statements and other
 actions based upon the users role level.  Role names and levels are
 stored in this class as constants to be used by other classes</p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Oct 11, 2007
 Last Updated:
 ***************************************************************************/

public class ANSRoleFilter {
	private SBUserRole role = null;
	private String alias = null;
	private Boolean includeStatus = Boolean.TRUE;
	
	/**
	 * SB Roles utilized by ANS Physician DB
	 */
	public static final Integer TM_ROLE = new Integer(25);
	public static final Integer CS_ROLE = new Integer(30);
	public static final Integer CFM_ROLE = new Integer(40);
	public static final Integer RSD_ROLE = new Integer(60);
	public static final Integer AREA_VP_ROLE = new Integer(70);
	public static final Integer VP_ROLE = new Integer(75);
	public static final Integer HCP_SURGEON_TYPE = Integer.valueOf(3);
	
	/**
	 * 
	 */
	public ANSRoleFilter() {
		
	}
	
	/**
	 * Helper method.  Role and alias must be assigned before this method is run
	 * @return
	 */
	public String getSearchFilter() {
		if (role == null || alias == null) return null;
		return getSearchFilter(role, alias, false, this.includeStatus);
	}
	
	/**
	 * Creates a SQL and statement to append to the sql.  Returns nothing if the 
	 * user is not filtered
	 * @param role SB User role object
	 * @param alias SQL Alias letter to be applied
	 * @return SQL String "AND" Clause.  Empty string if no filter applied
	 */
	public String getSearchFilter(SBUserRole role, String alias) {
		return getSearchFilter(role, alias, false,true);
	}
	
	/**
	 * Creates a SQL and statement to append to the SQL.  Returns nothing if the 
	 * user is not filtered
	 * @param role SB User role object
	 * @param alias SQL Alias letter to be applied
	 * @param edit used for CS and CFMs to view all in region
	 * @return SQL String "AND" Clause.  Empty string if no filter applied
	 */
	public String getSearchFilter(SBUserRole role, String alias, boolean edit) {
		return getSearchFilter(role, alias, false, true);
	}
	
	/**
	 * Creates a SQL and statement to append to the SQL.  Returns nothing if the 
	 * user is not filtered
	 * @param role SB User role object
	 * @param alias SQL Alias letter to be applied
	 * @param edit used for CS and CFMs to view all in region
	 * @param status Defines if the status should be filtered
	 * @return SQL String "AND" Clause.  Empty string if no filter applied
	 */
	public String getSearchFilter(SBUserRole role, String alias, boolean edit, boolean status) {
		String regionId = StringUtil.checkVal(role.getAttribute(AnsRoleModule.ANS_REGION_ID));
		String areaId = StringUtil.checkVal(role.getAttribute(AnsRoleModule.ANS_AREA_ID));
		StringBuffer sb = null;
		if (role.getRoleLevel() < 100 && status) {
			sb = new StringBuffer();
			sb.append(" and a.status_id < 10 ");
		} else {
			sb = new StringBuffer();
		}
		
		// If the user is limited to their area, add an area filter
		if(role.getRoleLevel() == 70) {
			sb.append(" and area_id = '").append(areaId).append("' ");
		
		// If the user is limited to their region, add it here
		} else if (role.getRoleLevel() == 60 || role.getRoleLevel() == 65) {
			sb.append(" and ").append(alias).append(".region_id = '");
			sb.append(regionId).append("' ");
		
		// If the user is limited to their region, and can only edit HCP, and
		// view all in region
		} else if (role.getRoleLevel() == 30 || role.getRoleLevel() == 40) {
			sb.append(" and ").append(alias).append(".region_id = '");
			sb.append(regionId).append("' ");
			
			// If the site is being edited
			if (edit) {
				sb.append(" and (").append("a.surgeon_type_id = ");
				sb.append(ANSRoleFilter.HCP_SURGEON_TYPE).append(" ");
				sb.append("or a.specialty_id = 16) ");
			}
		// If the user is a can only see their physicians(TM/TM3), add it here
		} else if (role.getRoleLevel() == 25) {
			sb.append(" and ").append(alias).append(".sales_rep_id = '");
			sb.append(role.getProfileId()).append("' ");
		/* 
		 * 05-18-09: ATM role has been renamed to TM1; TM role has been renamed to TM3
		 * If the user can see their physicians and their TM physicians, add it here.
		 */
		} else if (role.getRoleLevel() == 20) {
			sb.append(" and ").append(alias).append(".sales_rep_id in ('");
			sb.append(role.getProfileId()).append("', ");
			sb.append("'").append(role.getProfileRoleId()).append("') ");
		} else if (role.getRoleLevel() < 75){
			sb.append(" and ").append(alias).append(".sales_rep_id = 'NONE' ");
		}
		
		return sb.toString();
	}

	/**
	 * @return the role
	 */
	public SBUserRole getRole() {
		return role;
	}

	/**
	 * @param role the role to set
	 */
	public void setRole(SBUserRole role) {
		this.role = role;
	}

	/**
	 * @return the alias
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * @param alias the alias to set
	 */
	public void setAlias(String alias) {
		this.alias = alias;
	}

	/**
	 * @return the includeStatus
	 */
	public Boolean getIncludeStatus() {
		return includeStatus;
	}

	/**
	 * @param includeStatus the includeStatus to set
	 */
	public void setIncludeStatus(Boolean includeStatus) {
		this.includeStatus = includeStatus;
	}
}
