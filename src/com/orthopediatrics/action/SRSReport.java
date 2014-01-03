package com.orthopediatrics.action;

import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <b>Title</b>: SRSReport.java <p/>
 * <b>Project</b>: SB_Orthopediatrics <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jul 11, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class SRSReport extends AbstractSBReportVO {

	private byte[] data = null;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public SRSReport() {
		this.setContentType("text/html");
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {

		return data;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#setData(java.lang.Object)
	 */
	@Override
	public void setData(Object o) {
		if (o != null && o instanceof byte[]) {
			data = (byte[]) o;
		} else {
			data = new byte[0];
		}

	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#setFileName(java.lang.String)
	 */
	public void setFileName(String fName) {
		super.setFileName(fName);
		this.setContentTypeByExtension(fName.substring(fName.lastIndexOf(".") + 1));
	}

}
