package com.mindbody.vo;

import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> MindBodyCallVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages All the CallData for a MindBodyCall.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 3, 2017
 ****************************************************************************/
public abstract class MindBodyConfig {

	public static final int DEFAULT_PAGE_SIZE = 10;
	private String sourceName;
	private String sourceKey;
	private String userName;
	private String userPass;
	private List<Integer> siteIds;
	protected List<String> fields;
	private int pageNo;
	private int pageSize = DEFAULT_PAGE_SIZE;

	/**
	 * 
	 */
	protected MindBodyConfig(String sourceName, String sourceKey, List<Integer> siteIds) {
		this.siteIds = siteIds;
		this.sourceName = sourceName;
		this.sourceKey = sourceKey;
		this.fields = new ArrayList<>();
	}

	/**
	 * @return
	 */
	public String getSourceName() {
		return sourceName;
	}
	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	/**
	 * @return
	 */
	public String getSourceKey() {
		return sourceKey;
	}
	public void setSourceKey(String sourceKey) {
		this.sourceKey = sourceKey;
	}

	public List<Integer> getSiteIds() {
		return siteIds;
	}

	public void addSiteId(int siteId) {
		siteIds.add(siteId);
	}

	public void clearSiteIds() {
		siteIds.clear();
	}

	public int getPageNo() {
		return pageNo;
	}

	public void setPageNo(int pageNo) {
		this.pageNo = pageNo;
	}

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @return the userPass
	 */
	public String getUserPass() {
		return userPass;
	}

	public List<String> getFields() {
		return fields;
	}

	public void addField(String field) {
		fields.add(field);
	}

	public int getPageSize() {
		return pageSize;
	}
	/**
	 * @param userName the userName to set.
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @param userPass the userPass to set.
	 */
	public void setUserPass(String userPass) {
		this.userPass = userPass;
	}

	/**
	 * @param siteIds the siteIds to set.
	 */
	public void setSiteIds(List<Integer> siteIds) {
		this.siteIds = siteIds;
	}

	public void setFields(List<String> fields) {
		this.fields = fields;
	}

	public void setPagesize(int pageSize) {
		this.pageSize = pageSize;
	}
	public boolean hasUser() {
		return !StringUtil.isEmpty(userName) && !StringUtil.isEmpty(userPass);
	}

	public boolean isValid() {
		return !StringUtil.isEmpty(sourceName)
				&& !StringUtil.isEmpty(sourceKey)
				&& !siteIds.isEmpty();
	}

	public boolean hasFields() {
		return fields != null && !fields.isEmpty();
	}
}