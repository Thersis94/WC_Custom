package com.depuy.sitebuilder.datafeed;

import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <b>Title</b>: AbstractDataFeedReportVo.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> This method is used in depuy data feeds. it adds a method 
 * needed to pull params and attributes off of a request object.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Aug 16, 2016<p/>
 * @updates:
 ****************************************************************************/
public abstract class AbstractDataFeedReportVO extends AbstractSBReportVO {
	
	private static final long serialVersionUID = 1L;

	/**
	 * this method is used when the report needs data from the request.
	 * such as parameters or attributes to generate a complete binary report
	 * must be overwritten by all implementing classes.
	 * 
	 */
	public abstract void setRequestData(SMTServletRequest req);
	

}
