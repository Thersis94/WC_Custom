package com.mindbody.vo;

import java.util.ArrayList;
import java.util.List;

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
	private MindBodyCredentialVO sourceCredentials;
	private MindBodyCredentialVO userCredentials;
	private List<Integer> siteIds;
	protected List<String> fields;
	protected List<String> filters;
	private int pageNo;
	private int pageSize = DEFAULT_PAGE_SIZE;

	protected MindBodyConfig(MindBodyCredentialVO source) {
		this.fields = new ArrayList<>();
		this.filters = new ArrayList<>();
		this.sourceCredentials = source;
	}

	/**
	 * 
	 */
	protected MindBodyConfig(MindBodyCredentialVO source, MindBodyCredentialVO user) {
		this(source);
		this.userCredentials = user;
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

	public List<String> getFields() {
		return fields;
	}

	public void addField(String field) {
		fields.add(field);
	}

	public int getPageSize() {
		return pageSize;
	}

	public List<String> getFilters() {
		return filters;
	}

	public void addFilter(String filter) {
		filters.add(filter);
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

	public void setFilters(List<String> filters) {
		this.filters = filters;
	}
	public void setPagesize(int pageSize) {
		this.pageSize = pageSize;
	}
	
	/**
	 * @return the sourceCredentials
	 */
	public MindBodyCredentialVO getSourceCredentials() {
		return sourceCredentials;
	}

	/**
	 * @return the userCredentials
	 */
	public MindBodyCredentialVO getUserCredentials() {
		return userCredentials;
	}

	/**
	 * @param sourceCredentials the sourceCredentials to set.
	 */
	public void setSourceCredentials(MindBodyCredentialVO sourceCredentials) {
		this.sourceCredentials = sourceCredentials;
	}

	/**
	 * @param userCredentials the userCredentials to set.
	 */
	public void setUserCredentials(MindBodyCredentialVO userCredentials) {
		this.userCredentials = userCredentials;
	}

	public boolean hasUser() {
		return userCredentials != null && userCredentials.isValid();
	}

	public boolean isValid() {
		return sourceCredentials != null && sourceCredentials.isValid();
	}

	public boolean hasFields() {
		return fields != null && !fields.isEmpty();
	}

	public boolean hasFilters() {
		return filters != null && !filters.isEmpty();
	}
}