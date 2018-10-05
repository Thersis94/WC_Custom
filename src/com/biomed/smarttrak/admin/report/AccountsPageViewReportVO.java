package com.biomed.smarttrak.admin.report;

import java.util.ArrayList;
import java.util.List;

import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.common.SiteVO;

/****************************************************************************
 * <b>Title:</b> AccountsPageViewReportVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> TODO
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Oct 5, 2018
 ****************************************************************************/
public class AccountsPageViewReportVO extends AbstractSBReportVO {

	/**
	 *
	 */
	
	private static final long serialVersionUID = 1L;
	List<AccountPageViewReportVO> accountPageViews;

	private SiteVO site;

	public AccountsPageViewReportVO() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		accountPageViews = new ArrayList<>();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		if(o instanceof List<?>) {
			accountPageViews = (List<AccountPageViewReportVO>)o;
		}
	}

	/**
	 * @param site
	 */
	public void setSite(SiteVO site) {
		this.site = site;
	}

	public SiteVO getSite() {
		return this.site;
	}
}
