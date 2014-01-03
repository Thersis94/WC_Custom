/**
 * 
 */
package com.codman.cu.tracking.vo;

import java.sql.ResultSet;

import java.util.Date;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: SearchVO<p/>
 * <b>Description: Data bean for Person</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author jackson
 * @version 1.0
 * @since Aug 09, 2010
 ****************************************************************************/
public class PersonVO extends UserDataVO {
	private static final long serialVersionUID = 8103963075270234590L;
	
	private String personId = null;
	private String territoryId = null;
	private String sampleAccountNo = null;
	private String profileRoleId = null;
	private String roleId = null;
	private Date lastLoginDate = null;
	private String organizationId = null;

	public PersonVO() {
	}
	
	public PersonVO(SMTServletRequest req) {
		super(req);
		personId = req.getParameter("personId");
		territoryId = req.getParameter("territoryId");
		sampleAccountNo = req.getParameter("sampleAccountNo");
		roleId = req.getParameter("roleId");
		profileRoleId = req.getParameter("profileRoleId");
		
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		setOrganizationId(site.getOrganizationId());
		
	}

	public PersonVO(ResultSet rs) {
		super(rs);
		DBUtil util = new DBUtil();
		personId = util.getStringVal("person_id", rs);
		territoryId = util.getStringVal("territory_id", rs);
		sampleAccountNo = util.getStringVal("sample_acct_no", rs);
		profileRoleId = util.getStringVal("profile_role_id", rs);
		roleId = util.getStringVal("role_id", rs);
		setOrganizationId(util.getStringVal("organization_id", rs));
		util = null;
	}

	public String getTerritoryId() {
		return StringUtil.checkVal(territoryId, null);
	}

	public void setTerritoryId(String territoryId) {
		this.territoryId = territoryId;
	}
	
	public String toString() {
		return StringUtil.getToString(this) + super.toString();
	}

	public String getPersonId() {
		return personId;
	}

	public void setPersonId(String personId) {
		this.personId = personId;
	}

	/**
	 * @return the sampleAccountNo
	 */
	public String getSampleAccountNo() {
		return sampleAccountNo;
	}

	/**
	 * @param sampleAccountNo the sampleAccountNo to set
	 */
	public void setSampleAccountNo(String sampleAccountNo) {
		this.sampleAccountNo = sampleAccountNo;
	}

	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	public Date getLastLoginDate() {
		return lastLoginDate;
	}

	public void setLastLoginDate(Date lastLoginDate) {
		this.lastLoginDate = lastLoginDate;
	}

	public String getProfileRoleId() {
		return profileRoleId;
	}

	public void setProfileRoleId(String profileRoleId) {
		this.profileRoleId = profileRoleId;
	}

	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	public String getOrganizationId() {
		return organizationId;
	}

}
