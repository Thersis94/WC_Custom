/**
 * 
 */
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
 ****************************************************************************/
public class UnitHistoryReportRepsVO extends UnitHistoryReportVO {

	private static final long serialVersionUID = 1407073622234040274L;
	
	public UnitHistoryReportRepsVO(SiteVO site) {
		super(site);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("starting Unit History Report");
		StringBuilder rpt = new StringBuilder(getHeader(true));
		
		//loop the accounts, physians, units, and requests
		for (UnitVO v : data) {
			rpt.append(formatUnit(v, true));
		}
		
		return rpt.toString().getBytes();
	}	

}
