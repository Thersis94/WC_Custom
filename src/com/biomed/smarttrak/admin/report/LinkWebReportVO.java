package com.biomed.smarttrak.admin.report;

import java.util.List;

import com.biomed.smarttrak.vo.LinkVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * Title: LinkWebReportVO.java <p/>
 * Project: WC_Custom <p/>
 * Description: Generates the broken link report as a web version<p/>
 * Copyright: Copyright (c) 2017<p/>
 * Company: Silicon Mountain Technologies<p/>
 * @author Devon Franklin
 * @version 1.0
 * @since Sep 20, 2017
 ****************************************************************************/

public class LinkWebReportVO extends AbstractSBReportVO {

	private static final long serialVersionUID = 1L;
	private transient List<LinkVO> links;

	public LinkWebReportVO(){
		super();
		setContentType("text/html");
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		//simply return an empty array, data will be handled off to view not streamed
		return new byte[0];
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		this.setLinks((List<LinkVO>) o);
	}

	/**
	 * @return the links
	 */
	public List<LinkVO> getLinks() {
		return links;
	}

	/**
	 * @param links the links to set
	 */
	public void setLinks(List<LinkVO> links) {
		this.links = links;
	}

}
