package com.codman.cu.tracking.vo;

import com.smt.sitebuilder.common.SiteVO;

/****************************************************************************
 * <b>Title</b>: UnitDetailReportVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 25, 2011
 * @updates
 * 		09.07.2016, refactored from HTML to true Excel using the POI libraries - JM
 ****************************************************************************/
public class UnitHistoryReportRepsVO extends UnitHistoryReportVO {

	private static final long serialVersionUID = 140999223404987654L;

	public UnitHistoryReportRepsVO(SiteVO site) {
		super(site);
		super.isRepReport = true;
	}
}