package com.biomed.smarttrak.security;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.biomed.smarttrak.vo.PermissionVO;
import com.siliconmtn.data.Node;
import com.smt.sitebuilder.admin.action.data.RoleAttributeVO;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: SmarttrakRoleVO.java<p/>
 * <b>Description: Extention of WC core roles - added support for Smarttrak permissions and 
 * permission checking as assets (company/market/product) are loaded for display.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 21, 2017
 ****************************************************************************/
public class SmarttrakRoleVO extends SBUserRole {

	private transient Logger log = Logger.getLogger(SmarttrakRoleVO.class);

	private static final long serialVersionUID = 5759752076778089016L;

	/**
	 * the WC role VO this object decorates.
	 */
	private SBUserRole wcRole;

	/**
	 * the permissions we load from Smarttrak for the user's ACCOUNT.  Used to enforce access rights.
	 */
	private List<PermissionVO> accountRoles;


	public SmarttrakRoleVO() {
		super();
	}

	/**
	 * @param siteId
	 * @param roleName
	 */
	public SmarttrakRoleVO(String siteId, String roleName) {
		super(siteId, roleName);
	}

	/**
	 * @param siteId
	 */
	public SmarttrakRoleVO(String siteId) {
		super(siteId);
	}

	/**
	 * @param role
	 */
	public SmarttrakRoleVO(SBUserRole role) {
		super();
		this.wcRole = role;
	}

	public List<PermissionVO> getAccountRoles() {
		return accountRoles;
	}

	public void setAccountRoles(List<Node> nodes) {
		List<PermissionVO> perms = new ArrayList<>(nodes.size());
		for (Node n : nodes) {
			PermissionVO vo = (PermissionVO) n.getUserObject();
			//we only care about level 4 nodes, which is where permissions are set.  Also toss any VOs that don't have permissions in them
			if (n.getDepthLevel() != 4 || vo.isUnauthorized()) continue;
			perms.add(vo);
		}
		log.debug("loaded " + perms.size() + " account permissions into the roleVO");
	}



	@Override
	public String toString() {
		return wcRole.toString();
	}

	@Override
	public String getProfileId() {
		return wcRole.getProfileId();
	}

	@Override
	public void setProfileId(String profileId) {
		wcRole.setProfileId(profileId);
	}

	@Override
	public int getRoleLevel() {
		return wcRole.getRoleLevel();
	}

	@Override
	public void setRoleLevel(int roleLevel) {
		wcRole.setRoleLevel(roleLevel);
	}

	@Override
	public String getRoleName() {
		return wcRole.getRoleName();
	}

	@Override
	public void setRoleName(String roleName) {
		wcRole.setRoleName(roleName);
	}

	@Override
	public Date getCreateDate() {
		return wcRole.getCreateDate();
	}

	@Override
	public Date getUpdateDate() {
		return wcRole.getUpdateDate();
	}

	@Override
	public void setCreateDate(Date createDate) {
		wcRole.setCreateDate(createDate);
	}

	@Override
	public void setUpdateDate(Date updateDate) {
		wcRole.setUpdateDate(updateDate);
	}

	@Override
	public String getRoleId() {
		return wcRole.getRoleId();
	}

	@Override
	public void setRoleId(String roleId) {
		wcRole.setRoleId(roleId);
	}

	@Override
	public void addAttribute(String key, Object value) {
		wcRole.addAttribute(key, value);
	}

	@Override
	public Object getAttribute(String key) {
		return wcRole.getAttribute(key);
	}

	@Override
	public Map<String, Object> getAttributes() {
		return wcRole.getAttributes();
	}

	@Override
	public void setAttributes(Map<String, Object> attributes) {
		wcRole.setAttributes(attributes);
	}

	@Override
	public Object getAttribute(int loc) {
		return wcRole.getAttribute(loc);
	}

	@Override
	public Date getRoleExpireDate() {
		return wcRole.getRoleExpireDate();
	}

	@Override
	public void setRoleExpireDate(Date roleExpireDate) {
		wcRole.setRoleExpireDate(roleExpireDate);
	}

	@Override
	public String getCachePmid() {
		return wcRole.getCachePmid();
	}

	@Override
	public String getCachePmid(String siteId) {
		return wcRole.getCachePmid(siteId);
	}

	@Override
	public String getOrganizationId() {
		return wcRole.getOrganizationId();
	}

	@Override
	public void setOrganizationId(String organizationId) {
		wcRole.setOrganizationId(organizationId);
	}

	@Override
	public String getSiteId() {
		return wcRole.getSiteId();
	}

	@Override
	public void setSiteId(String siteId) {
		wcRole.setSiteId(siteId);
	}

	@Override
	public String getSiteName() {
		return wcRole.getSiteName();
	}

	@Override
	public void setSiteName(String siteName) {
		wcRole.setSiteName(siteName);
	}

	@Override
	public Integer getStatusId() {
		return wcRole.getStatusId();
	}

	@Override
	public void setStatusId(Integer statusId) {
		wcRole.setStatusId(statusId);
	}

	@Override
	public String getProfileRoleId() {
		return wcRole.getProfileRoleId();
	}

	@Override
	public void setProfileRoleId(String profileRoleId) {
		wcRole.setProfileRoleId(profileRoleId);
	}

	@Override
	public String getIpAddress() {
		return wcRole.getIpAddress();
	}

	@Override
	public void setIpAddress(String ipAddress) {
		wcRole.setIpAddress(ipAddress);
	}

	@Override
	public String getAttrib1Txt() {
		return wcRole.getAttrib1Txt();
	}

	@Override
	public void setAttrib1Txt(String attrib1Txt) {
		wcRole.setAttrib1Txt(attrib1Txt);
	}

	@Override
	public List<RoleAttributeVO> getRoleAttributes() {
		return wcRole.getRoleAttributes();
	}

	@Override
	public void setRoleAttributes(List<RoleAttributeVO> roleAttributes) {
		wcRole.setRoleAttributes(roleAttributes);
	}

	@Override
	public void addRoleAttribute(RoleAttributeVO vo) {
		wcRole.addRoleAttribute(vo);
	}

	@Override
	public boolean hasRoleAttribute(String id) {
		return wcRole.hasRoleAttribute(id);
	}
}