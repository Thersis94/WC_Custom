package com.fastsigns.product.keystone.vo;

import com.smt.sitebuilder.action.AbstractSBReportVO;

public class InvoiceReportVO extends AbstractSBReportVO{

	/**
	 * 
	 */
	private byte [] data = null;
	private static final long serialVersionUID = 4964153052439376227L;
	/**
     * 
     */
    public InvoiceReportVO() {
        super();
        setContentType("application/pdf");
        isHeaderAttachment(Boolean.TRUE);
    }
    
	@Override
	public byte[] generateReport() {
		return data;
	}

	@Override
	public void setData(Object o) {
		data = (byte[]) o;
	}

}
