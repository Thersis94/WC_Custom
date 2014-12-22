package com.codman.cu.tracking.vo;

import java.util.List;

import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <b>Title</b>: DataExport.java<p/>
 * <b>Description: Exports a suite of data to an Excel report</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 25, 2011
 ****************************************************************************/
public class PhysicianReportVO extends AbstractSBReportVO {
	private static final long serialVersionUID = -4473379747242916803L;
	private List<AccountVO> data;
	
	public PhysicianReportVO() {
		super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName("Control Unit Physician Export.xls");
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("starting Physician Report");
		StringBuilder rpt = new StringBuilder(this.getHeader());
		
		//loop the accounts, physians, units, and requests
		for (AccountVO acct : data) {
			for (PhysicianVO v : acct.getPhysicians()) {
				rpt.append("<tr>");
				rpt.append("\t<td>").append(StringUtil.checkVal(acct.getAccountName())).append("</td>\r");
				rpt.append("\t<td>").append(StringUtil.checkVal(v.getFirstName())).append(" ").append(StringUtil.checkVal(v.getLastName())).append("</td>\r");
				rpt.append("\t<td>").append(StringUtil.checkVal(v.getEmailAddress())).append("</td>\r");
				rpt.append("\t<td>").append(StringUtil.checkVal(v.getMainPhone())).append("</td>\r");
				rpt.append("\t<td>").append(StringUtil.checkVal(v.getAddress())).append("</td>\r");
				rpt.append("\t<td>").append(StringUtil.checkVal(v.getAddress2())).append("</td>\r");
				rpt.append("\t<td>").append(StringUtil.checkVal(v.getCity())).append("</td>\r");
				rpt.append("\t<td>").append(StringUtil.checkVal(v.getState())).append("</td>\r");
				rpt.append("\t<td>").append(StringUtil.checkVal(v.getZipCode())).append("</td>\r");
				rpt.append("\t<td>").append(StringUtil.checkVal(v.getCountryCode())).append("</td>\r");			
				rpt.append("</tr>\r");
			}
		}
		
		rpt.append(this.getFooter());
		return rpt.toString().getBytes();
	}
	

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#setData(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void setData(Object o) {
		data = (List<AccountVO>) o;
		
	}
	
	private StringBuilder getHeader() {
		StringBuilder hdr = new StringBuilder();
		hdr.append("<table border='1'>\r");
		hdr.append("<tr><td colspan='10' style='background-color: #ccc;'><b>Codman CU Tracking System - Physician Summary</b></td></tr>\r");
		hdr.append("\t<tr><td>Account</td>");
		hdr.append("\t<td>Name</td>");
		hdr.append("\t<td>Email</td>");
		hdr.append("\t<td>Phone</td>");
		hdr.append("\t<td>Address</td>");
		hdr.append("\t<td>Suite/Box</td>");
		hdr.append("\t<td>City</td>");
		hdr.append("\t<td>State</td>");
		hdr.append("\t<td>Zip</td>");
		hdr.append("\t<td>Country</td></tr>\r");
		
		return hdr;
	}

	
	private StringBuilder getFooter() {
		return new StringBuilder("</table>");
	}
}
