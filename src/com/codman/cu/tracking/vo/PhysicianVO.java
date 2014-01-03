/**
 * 
 */
package com.codman.cu.tracking.vo;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: PhysicianVO<p/>
 * <b>Description: Data bean for Physician</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Nov 02, 2010
 ****************************************************************************/
public class PhysicianVO extends UserDataVO {
	
	private static final long serialVersionUID = 1L;
	private String physicianId = null;
	private String accountId = null;
	private String organizationId = null;
	private String centerText = null;
	private String departmentText = null;
				
	public PhysicianVO() {
	}
	
	public PhysicianVO(SMTServletRequest req) {
		super(req);
		physicianId = req.getParameter("physicianId");
		accountId = req.getParameter("accountId");
		centerText = req.getParameter("centerText");
		departmentText = req.getParameter("departmentText");
		
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		organizationId = site.getOrganizationId();
	}

	public PhysicianVO(ResultSet rs) {
		super.setData(rs);
		DBUtil util = new DBUtil();
		physicianId = util.getStringVal("physician_id", rs);
		accountId = util.getStringVal("account_id", rs);
		organizationId = util.getStringVal("organization_id", rs);
		centerText = util.getStringVal("center_txt", rs);
		departmentText = util.getStringVal("department_txt", rs);
		
		if (util.getStringVal("phys_profile_id", rs) != null)
			setProfileId(util.getStringVal("phys_profile_id", rs));
		
		util = null;
	}

	public String toString() {
		return StringUtil.getToString(this) + super.toString();
	}

	/**
	 * @return the physicianId
	 */
	public String getPhysicianId() {
		return physicianId;
	}

	/**
	 * @param physicianId the physicianId to set
	 */
	public void setPhysicianId(String physicianId) {
		this.physicianId = physicianId;
	}

	/**
	 * @return the accountId
	 */
	public String getAccountId() {
		return accountId;
	}

	/**
	 * @param accountId the accountId to set
	 */
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	public String getOrganizationId() {
		return organizationId;
	}

	public String getCenterText() {
		return centerText;
	}

	public void setCenterText(String centerText) {
		this.centerText = centerText;
	}

	public String getDepartmentText() {
		return departmentText;
	}

	public void setDepartmentText(String departmentText) {
		this.departmentText = departmentText;
	}

}
