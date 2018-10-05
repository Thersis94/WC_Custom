package com.biomed.smarttrak.admin.report;

import java.util.HashMap;
import java.util.Map;

import com.biomed.smarttrak.vo.AccountVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.util.PageViewVO;

/****************************************************************************
 * <b>Title:</b> AccountPageviewReportVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> TODO
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Oct 5, 2018
 ****************************************************************************/
public class AccountPageViewReportVO extends AccountVO {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private Map<String, PageViewVO> pageViews;

	public AccountPageViewReportVO() {
		super();
		this.pageViews = new HashMap<>();
	}

	public void setPageViews(Map<String, PageViewVO> pageViews) {
		this.pageViews = pageViews;
	}

	public Map<String, PageViewVO> getPageViews() {
		return this.pageViews;
	}

	public void addPageView(PageViewVO pv) {
		pageViews.put(StringUtil.join(pv.getRequestUri(), pv.getQueryString()), pv);
	}
}