package com.mts.security;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <p><b>Title:</b> SSOProviderVO.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Aug 26, 2019
 * <b>Changes:</b>
 ****************************************************************************/
@Table(name="mts_sso")
public class SSOProviderVO extends BeanDataVO {

	private static final long serialVersionUID = 1983162563636401083L;

	private String ssoId;
	private String providerName;
	private String loginModuleXrId; //reference to core.login_module_xr table
	private String roleId;
	private String publicationId; //this holds comma delimited list
	private Date expirationDate;
	private Date createDate;
	private Date updateDate;
	private int activeFlag;

	//extra UI fields
	private String roleName;
	private String publicationNames;
	private String qualifierPatternText;

	public SSOProviderVO() {
		super();
	}

	/**
	 * 
	 * @param req
	 */
	public SSOProviderVO(ActionRequest req) {
		super(req);
		//flatten the array values to a comma-delimited string
		setPublicationId(StringUtil.getToString(req.getParameterValues("publications"), false, false, ","));
	}

	/**
	 * 
	 * @param rs
	 */
	public SSOProviderVO(ResultSet rs) {
		super(rs);
	}


	@Column(name="sso_id", isPrimaryKey=true)
	public String getSsoId() {
		return ssoId;
	}

	@Column(name="provider_nm")
	public String getProviderName() {
		return providerName;
	}

	@Column(name="site_login_module_xr_id")
	public String getLoginModuleXrId() {
		return StringUtil.checkVal(loginModuleXrId, null);
	}

	@Column(name="user_role_id")
	public String getRoleId() {
		return roleId;
	}

	@Column(name="user_publication_id")
	public String getPublicationId() {
		return publicationId;
	}

	@Column(name="user_expiration_dt")
	public Date getExpirationDate() {
		return expirationDate;
	}

	public void setSsoId(String ssoId) {
		this.ssoId = ssoId;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	@Column(name="role_nm", isReadOnly=true)
	public String getRoleName() {
		return roleName;
	}

	public String getPublicationNames() {
		return publicationNames;
	}

	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}

	public void setLoginModuleXrId(String loginModuleXrId) {
		this.loginModuleXrId = loginModuleXrId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	public void setPublicationId(String publicationId) {
		this.publicationId = publicationId;
	}

	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	/**
	 * populate the diplay value for publication names using the given map of lookup values
	 * @param publications
	 */
	public void setPublicationNames(Map<String, String> publications) {
		if (StringUtil.isEmpty(getPublicationId())) return;

		StringBuilder names = new StringBuilder(150);
		for (String pubId : getPublicationId().split(",")) {
			String nm = publications.get(pubId);
			if (!StringUtil.isEmpty(nm))
				names.append(names.length() > 0 ? ", " : "").append(nm);
		}
		this.publicationNames = names.toString();
	}

	@Column(name="active_flg", isReadOnly=true)
	public int getActiveFlag() {
		return activeFlag;
	}

	public void setActiveFlag(int activeFlag) {
		this.activeFlag = activeFlag;
	}

	@Column(name="qualifier_pattern_txt", isReadOnly=true)
	public String getQualifierPatternText() {
		return qualifierPatternText;
	}

	public void setQualifierPatternText(String qualifierPatternText) {
		this.qualifierPatternText = qualifierPatternText;
	}

	/**
	 * Return the comma-limited string as a list.  Used when creating a new user in
	 * the login module.
	 * @return
	 */
	public List<String> getPublications() {
		List<String> subs = new ArrayList<>();
		if (!StringUtil.isEmpty(getPublicationId())) {
			String[] arr = getPublicationId().split(",");
			for (String id : arr)
				subs.add(id);
		}
		return subs;
	}
}
