package com.biomed.smarttrak.security;

//Java 8
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//WC Custom
import com.biomed.smarttrak.vo.PermissionVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.util.solr.AccessControlListGenerator;
import com.smt.sitebuilder.admin.action.data.RoleAttributeVO;
import com.smt.sitebuilder.search.SearchDocumentHandler;
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
	private static final long serialVersionUID = 5759752076778089016L;

	private boolean isFdAuth;
	private boolean isGaAuth;
	private boolean isBrowseAuth;
	private boolean isPeAuth;
	private boolean isAnAuth;
	private boolean acctOwnerFlg;

	/**
	 * the WC role VO this object decorates.
	 */
	private SBUserRole wcRole;

	/**
	 * the section permissions loaded for Smarttrak for the user's ACCOUNT.  Used to enforce access rights.
	 */
	private List<PermissionVO> accountRoles;

	private String[] authorizedSections;

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


	/**
	 * builds the roleACL string once, when accountRoles are set.  Then stores it in a member variable so we don't
	 * have to recalculate it every time we run a query to Solr.
	 * @return
	 */
	private void buildACL() {
		AccessControlListGenerator gen = new AccessControlListGenerator();
		if (accountRoles == null || accountRoles.isEmpty()) return;

		Set<String> groups = new HashSet<>(accountRoles.size());
		//back-trace the approved hierarchies and authorize all parent levels as well
		for (PermissionVO vo : accountRoles) {
			String[] tok = vo.getSolrTokenTxt().split(SearchDocumentHandler.HIERARCHY_DELIMITER);
			StringBuilder key = new StringBuilder(50);
			for (int x=0; x < tok.length; x++) {
				if (key.length() > 0) key.append(SearchDocumentHandler.HIERARCHY_DELIMITER);
				key.append(tok[x]);
				//System.err.println(key)
				groups.add(key.toString());
			}
		}

		authorizedSections = groups.toArray(new String[groups.size()]);
		setAccessControlList(gen.getQueryACL(null, authorizedSections));
	}

	/**
	 * sets the section permissions for the user's account into their role vo.
	 * package access modifier - only SmarttrakRoleModule should be setting this value
	 * @param nodes
	 */
	void setAccountRoles(List<Node> nodes) {
		accountRoles = new ArrayList<>();
		for (Node n : nodes) {
			PermissionVO vo = (PermissionVO) n.getUserObject();
			//we only care about level 4 nodes, which is where permissions are set.  Also toss any VOs that don't have permissions in them
			if (SecurityController.PERMISSION_DEPTH_LVL != n.getDepthLevel() || vo.isUnauthorized()) continue;
			vo.setHierarchyToken(n.getFullPath()); //transpose the value compiled in SmartTrakRoleModule
			//System.err.println("user authorized for hierarchy: " + vo.getHierarchyToken())
			accountRoles.add(vo);
		}
		buildACL();
	}

	public boolean isFdAuthorized() {
		return isFdAuth;
	}

	/**
	 * the user is authorized for FD if either their personal record or the account's record is authorized
	 * package access modifier - only SmarttrakRoleModule should be setting this value
	 * -1 at user level is a BLOCK setting.  -1 means no access.
	 * @param userAuth
	 * @param acctAuth
	 */
	void setFdAuthorized(int userAuth, int acctAuth) {
		this.isFdAuth = userAuth != -1 && acctAuth == 1;
	}

	public boolean isGaAuthorized() {
		return isGaAuth;
	}

	/**
	 * the user is authorized for GA if either their personal record or the account's record is authorized
	 * package access modifier - only SmarttrakRoleModule should be setting this value
	 * -1 at user level is a BLOCK setting.  -1 means no access.
	 * @param userAuth
	 * @param acctAuth
	 */
	void setGaAuthorized(int userAuth, int acctAuth) {
		this.isGaAuth = userAuth != -1 && acctAuth == 1;
	}

	public boolean isPeAuthorized() {
		return isPeAuth;
	}

	/**
	 * the user is authorized for Product Explorer if the account's record is authorized
	 * package access modifier - only SmarttrakRoleModule should be setting this value
	 * @param userAuth
	 * @param acctAuth
	 */
	void setPeAuthorized(int userAuth, int acctAuth) {
		this.isPeAuth = userAuth == 1 || acctAuth == 1;
	}

	public boolean isAnAuthorized() {
		return isAnAuth;
	}

	/**
	 * the user is authorized for ANalysis Articles if the account's record is authorized
	 * package access modifier - only SmarttrakRoleModule should be setting this value
	 * @param userAuth
	 * @param acctAuth
	 */
	void setAnAuthorized(int userAuth, int acctAuth) {
		this.isAnAuth = userAuth == 1 || acctAuth == 1;
	}
	

	/**
	 * @return
	 */
	public boolean isBrowseAuthorized() {
		return isBrowseAuth;
	}

	/**
	 * the user is authorized for any section in the 'Prof' heading, which is browseability of markets/companies/products
	 * package access modifier - only SmarttrakRoleModule should be setting this value
	 * @param userAuth
	 * @param acctAuth
	 */
	void setBrowseAuthorized(int userAuth, int acctAuth) {
		this.isBrowseAuth = userAuth == 1 || acctAuth == 1;
	}
	
	
	/**
	 * decides whether the 'tools' dropdown menu should visible at all.
	 * @return
	 */
	public boolean isToolsAuthorized() {
		return isFdAuth || isGaAuth || isPeAuth;
	}



	/**************************************************************
	 * 		BELOW METHOD ARE OVERLOADED FOR THE DECORATOR PATTERN   *
	 **************************************************************/

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

	/**
	 * @param acctOwnerFlg
	 */
	public void setAccountOwner(int acctOwnerFlg) {
		this.acctOwnerFlg = 1 == acctOwnerFlg;
	}

	/**
	 * you are or are-not an account owner.  This drives whether you see "My Teams" in the pulldown menu.
	 * @return
	 */
	public boolean isAccountOwner() {
		return acctOwnerFlg;
	}

	public String[] getAuthorizedSections() {
		return authorizedSections;
	}
}