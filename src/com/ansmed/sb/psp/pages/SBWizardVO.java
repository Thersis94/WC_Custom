package com.ansmed.sb.psp.pages;

import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.Date;

import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>SBWizardVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Dec 3, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class SBWizardVO {
	private String organizationId = null;
	private String organizationName = null;
	private String siteId = null;
	private String siteName = null;
	private String themeId = null;
	private String siteAliasUrl = null;
	private String copyright = null;
	private String wizardClass = null;
	private String overrideSiteId = null;
	
	/**
	 * Preset values that can be overridden
	 */
	private String requestType = "reqBuild";
	private String mainEmail = "info@siliconmtn.com";
	private String adminName = "Webmaster";
	private String adminEmail = "info@siliconmtn.com";
	private String liveDate = Convert.formatDate(new Date());
	private String primaryFlg = "1";
	private String numberColumns = "1";
	private String defaultColumn = "1";
	private String pageTitle= "Welcome";
	private String defaultFlag = "1";
	private String defaultLocationFlag = "Yes";
	private String layoutName = "Default One Column";
	private String cPage = "index";
	private String actionId = "SITE_WIZARD";
	//private String wizardId = "0a0015a420640cc2c21018564c406c71";
	private String wizardId = "c0a8021e4d8748c2338a06d74c00e20c";
	private String origOrgId = "SJM_DOCS";
	private String[] roles = new String[] { "0", "10", "100" };
	private String allModuleFlag= "0";
	private String skipModule = "true";
	private String[] module_type_id = new String[] {"CONTENT","CONTACT","MAPS","PRINTER_FRIENDLY","SITE_MAP"};
	
	/**
	 * 
	 */
	public SBWizardVO() {
		
	}
	
	/**
	 * Converts the fields into a POST formatted data stream
	 * @return
	 */
	public String getPostData() {
		StringBuilder sb = new StringBuilder("1=1");
		Class<?> c = this.getClass();
		Field[] fields = c.getDeclaredFields();
		
		for(int i=0; i < fields.length; i++) {
			Field f = fields[i];
			if (f.getType().toString().indexOf("[L") > -1) {
				try {
					String[] val = (String[]) f.get(this);
					for(int j=0; j < val.length; j++) {
						sb.append("&").append(f.getName()).append("=").append(val[j]);
					}
				} catch(Exception e) {}
			} else {
				try {
					String val = StringUtil.checkVal(f.get(this));
					sb.append("&").append(f.getName()).append("=").append(URLEncoder.encode(val, "UTF-8"));
				} catch(Exception e) {}
			}
		}
		
		return sb.toString();
	}

	/**
	 * @return the siteId
	 */
	public String getSiteId() {
		return siteId;
	}

	/**
	 * @param siteId the siteId to set
	 */
	public void setSiteId(String siteId) {
		this.siteId = siteId;
	}

	/**
	 * @return the siteName
	 */
	public String getSiteName() {
		return siteName;
	}

	/**
	 * @param siteName the siteName to set
	 */
	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}

	/**
	 * @return the themeId
	 */
	public String getThemeId() {
		return themeId;
	}

	/**
	 * @param themeId the themeId to set
	 */
	public void setThemeId(String themeId) {
		this.themeId = themeId;
	}

	/**
	 * @return the copyright
	 */
	public String getCopyright() {
		return copyright;
	}

	/**
	 * @param copyright the copyright to set
	 */
	public void setCopyright(String copyright) {
		this.copyright = copyright;
	}

	/**
	 * @return the requestType
	 */
	public String getRequestType() {
		return requestType;
	}

	/**
	 * @param requestType the requestType to set
	 */
	public void setRequestType(String requestType) {
		this.requestType = requestType;
	}

	/**
	 * @return the mainEmail
	 */
	public String getMainEmail() {
		return mainEmail;
	}

	/**
	 * @param mainEmail the mainEmail to set
	 */
	public void setMainEmail(String mainEmail) {
		this.mainEmail = mainEmail;
	}

	/**
	 * @return the adminName
	 */
	public String getAdminName() {
		return adminName;
	}

	/**
	 * @param adminName the adminName to set
	 */
	public void setAdminName(String adminName) {
		this.adminName = adminName;
	}

	/**
	 * @return the adminEmail
	 */
	public String getAdminEmail() {
		return adminEmail;
	}

	/**
	 * @param adminEmail the adminEmail to set
	 */
	public void setAdminEmail(String adminEmail) {
		this.adminEmail = adminEmail;
	}

	/**
	 * @return the liveDate
	 */
	public String getLiveDate() {
		return liveDate;
	}

	/**
	 * @param liveDate the liveDate to set
	 */
	public void setLiveDate(String liveDate) {
		this.liveDate = liveDate;
	}

	/**
	 * @return the primaryFlg
	 */
	public String getPrimaryFlg() {
		return primaryFlg;
	}

	/**
	 * @param primaryFlg the primaryFlg to set
	 */
	public void setPrimaryFlg(String primaryFlg) {
		this.primaryFlg = primaryFlg;
	}

	/**
	 * @return the numberColumns
	 */
	public String getNumberColumns() {
		return numberColumns;
	}

	/**
	 * @param numberColumns the numberColumns to set
	 */
	public void setNumberColumns(String numberColumns) {
		this.numberColumns = numberColumns;
	}

	/**
	 * @return the defaultColumn
	 */
	public String getDefaultColumn() {
		return defaultColumn;
	}

	/**
	 * @param defaultColumn the defaultColumn to set
	 */
	public void setDefaultColumn(String defaultColumn) {
		this.defaultColumn = defaultColumn;
	}

	/**
	 * @return the pageTitle
	 */
	public String getPageTitle() {
		return pageTitle;
	}

	/**
	 * @param pageTitle the pageTitle to set
	 */
	public void setPageTitle(String pageTitle) {
		this.pageTitle = pageTitle;
	}

	/**
	 * @return the defaultFlag
	 */
	public String getDefaultFlag() {
		return defaultFlag;
	}

	/**
	 * @param defaultFlag the defaultFlag to set
	 */
	public void setDefaultFlag(String defaultFlag) {
		this.defaultFlag = defaultFlag;
	}

	/**
	 * @return the layoutName
	 */
	public String getLayoutName() {
		return layoutName;
	}

	/**
	 * @param layoutName the layoutName to set
	 */
	public void setLayoutName(String layoutName) {
		this.layoutName = layoutName;
	}

	/**
	 * @return the cPage
	 */
	public String getCPage() {
		return cPage;
	}

	/**
	 * @param page the cPage to set
	 */
	public void setCPage(String page) {
		cPage = page;
	}

	/**
	 * @return the actionId
	 */
	public String getActionId() {
		return actionId;
	}

	/**
	 * @param actionId the actionId to set
	 */
	public void setActionId(String actionId) {
		this.actionId = actionId;
	}

	/**
	 * @return the wizardClass
	 */
	public String getWizardClass() {
		return wizardClass;
	}

	/**
	 * @param wizardClass the wizardClass to set
	 */
	public void setWizardClass(String wizardClass) {
		this.wizardClass = wizardClass;
	}

	/**
	 * @return the wizardId
	 */
	public String getWizardId() {
		return wizardId;
	}

	/**
	 * @param wizardId the wizardId to set
	 */
	public void setWizardId(String wizardId) {
		this.wizardId = wizardId;
	}

	/**
	 * @return the origOrgId
	 */
	public String getOrigOrgId() {
		return origOrgId;
	}

	/**
	 * @param origOrgId the origOrgId to set
	 */
	public void setOrigOrgId(String origOrgId) {
		this.origOrgId = origOrgId;
	}

	/**
	 * @return the roles
	 */
	public String[] getRols() {
		return roles;
	}

	/**
	 * @param roles the roles to set
	 */
	public void setRoles(String[] roles) {
		this.roles = roles;
	}

	/**
	 * @return the allModuleFlag
	 */
	public String getAllModuleFlag() {
		return allModuleFlag;
	}

	/**
	 * @param allModuleFlag the allModuleFlag to set
	 */
	public void setAllModuleFlag(String allModuleFlag) {
		this.allModuleFlag = allModuleFlag;
	}

	/**
	 * @return the skipModule
	 */
	public String getSkipModule() {
		return skipModule;
	}

	/**
	 * @param skipModule the skipModule to set
	 */
	public void setSkipModule(String skipModule) {
		this.skipModule = skipModule;
	}

	/**
	 * @return the organizationId
	 */
	public String getOrganizationId() {
		return organizationId;
	}

	/**
	 * @param organizationId the organizationId to set
	 */
	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	/**
	 * @return the organizationName
	 */
	public String getOrganizationName() {
		return organizationName;
	}

	/**
	 * @param organizationName the organizationName to set
	 */
	public void setOrganizationName(String organizationName) {
		this.organizationName = organizationName;
	}

	/**
	 * @return the siteAliasUrl
	 */
	public String getSiteAliasUrl() {
		return siteAliasUrl;
	}

	/**
	 * @param siteAliasUrl the siteAliasUrl to set
	 */
	public void setSiteAliasUrl(String siteAliasUrl) {
		this.siteAliasUrl = siteAliasUrl;
	}

	/**
	 * @return the defaultLocationFlag
	 */
	public String getDefaultLocationFlag() {
		return defaultLocationFlag;
	}

	/**
	 * @param defaultLocationFlag the defaultLocationFlag to set
	 */
	public void setDefaultLocationFlag(String defaultLocationFlag) {
		this.defaultLocationFlag = defaultLocationFlag;
	}

	/**
	 * @return the module_type_id
	 */
	public String[] getModule_type_id() {
		return module_type_id;
	}

	/**
	 * @param module_type_id the module_type_id to set
	 */
	public void setModule_type_id(String[] module_type_id) {
		this.module_type_id = module_type_id;
	}

	/**
	 * @return the overrideSiteId
	 */
	public String getOverrideSiteId() {
		return overrideSiteId;
	}

	/**
	 * @param overrideSiteId the overrideSiteId to set
	 */
	public void setOverrideSiteId(String overrideSiteId) {
		this.overrideSiteId = overrideSiteId;
	}
	
}
