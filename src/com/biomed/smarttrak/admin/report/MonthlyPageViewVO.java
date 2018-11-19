package com.biomed.smarttrak.admin.report;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> MonthlyPageViewVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> VO stores PageViews by month for the MonthlyPageViewReport.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 13, 2018
 ****************************************************************************/
public class MonthlyPageViewVO {

	private String requestUri;
	private String sectionName;
	private String pageName;
	private String pageTitle;
	private Map<String, Integer> pageCounts;
	
	public MonthlyPageViewVO() {
		this.pageCounts = new HashMap<>();
	}

	/**
	 * @param rs
	 */
	public MonthlyPageViewVO(ResultSet rs) {
		this();
		setData(rs);
	}

	/**
	 * Helper method that extracts data from given ResultSet
	 * @param rs
	 */
	private void setData(ResultSet rs) {
		DBUtil util = new DBUtil();
		this.requestUri = util.getStringVal("request_uri", rs);
		this.sectionName = util.getStringVal("section_nm", rs);
		this.pageName = util.getStringVal("page_nm", rs);
		this.pageTitle = StringUtil.checkVal(pageName, sectionName);
		this.addPageCount(util.getStringVal("visit_dt", rs), util.getIntVal("hit_cnt", rs));
	}

	/**
	 * @return the requestUri
	 */
	public String getRequestUri() {
		return requestUri;
	}

	/**
	 * @return the sectionName
	 */
	public String getSectionName() {
		return sectionName;
	}

	/**
	 * @return the pageName
	 */
	public String getPageName() {
		return pageName;
	}

	/**
	 * @return the pageTitle
	 */
	public String getPageTitle() {
		return pageTitle;
	}

	/**
	 * @return the pageCounts
	 */
	public Map<String, Integer> getPageCounts() {
		return pageCounts;
	}

	/**
	 * @param requestUri the requestUri to set.
	 */
	public void setRequestUri(String requestUri) {
		this.requestUri = requestUri;
	}

	/**
	 * @param sectionName the sectionName to set.
	 */
	public void setSectionName(String sectionName) {
		this.sectionName = sectionName;
	}

	/**
	 * @param pageName the pageName to set.
	 */
	public void setPageName(String pageName) {
		this.pageName = pageName;
	}

	/**
	 * @param pageCounts the pageCounts to set.
	 */
	public void setPageCounts(Map<String, Integer> pageCounts) {
		this.pageCounts = pageCounts;
	}

	/**
	 * @param pageTitle the pagetitle to set
	 */
	public void setPageTitle(String pageTitle) {
		this.pageTitle = pageTitle;
	}

	/**
	 * Adds a pageCount record if dateKey is not empty.
	 * @param dateKey
	 * @param count
	 */
	public void addPageCount(String dateKey, int count) {
		if(!StringUtil.isEmpty(dateKey))
			this.pageCounts.put(dateKey, count);
	}

	/**
	 * Retrieves a hit count for given dateKey if present.  Otherwise returns 0.
	 * @param dateKey
	 * @return
	 */
	public int getPageCounts(String dateKey) {
		if(pageCounts.containsKey(dateKey))
			return pageCounts.get(dateKey);
		else return 0;
	}
}