/**
 * 
 */
package com.codman.cu.tracking.vo;

import java.text.DateFormat;

import java.util.Date;
import java.util.List;

import com.codman.cu.tracking.UnitAction;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
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
 * updated: Feb 13, 2012, added state and country to report.
 ****************************************************************************/
public class UnitReportVO extends AbstractSBReportVO {

	private static final long serialVersionUID = 1407073622024040274L;
	private List<UnitVO> data;
	protected SiteVO siteVo;
	
	public UnitReportVO(SiteVO site) {
		super();
		this.siteVo = site;
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName("Control Unit summary Report.xls");
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("starting Account Unit Report");
		StringBuilder rpt = new StringBuilder(this.getHeader());
		
		//loop the accounts, physians, units, and requests
		for (UnitVO v : data) {
			rpt.append(formatUnit(v));
		}
		
		return rpt.toString().getBytes();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#setData(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void setData(Object o) {
		data = (List<UnitVO>) o;
		
	}
	
	protected StringBuilder getHeader() {
		StringBuilder hdr = new StringBuilder();
		hdr.append("<tr><td><table border='1'>\r");
		hdr.append("<tr><td colspan='11' style='background-color: #ccc;'><b>Codman CU Tracking System - Unit Summary</b></td></tr>\r");
		hdr.append("\t<td>Account</td>");
		hdr.append("\t<td>Rep Name</td>");
		hdr.append("\t<td>Physician Name</td>");
		hdr.append("\t<td>Unit Type</td>");
		hdr.append("\t<td>Unit Status</td>");
		hdr.append("\t<td>Date Deployed</td>");
		hdr.append("\t<td>Serial No.</td>");
		hdr.append("\t<td>Software Rev No.</td>");
		hdr.append("\t<td>Hardware Rev No.</td>");
		hdr.append("\t<td>City</td>");
		hdr.append("\t<td>Country</td>");
		hdr.append("\t<td>Comments</td></tr>\r");
		
		return hdr;
	}

	
	protected StringBuilder getFooter() {
		return new StringBuilder("</table>");
	}
	
	protected String formatUnit(UnitVO u) {
		StringBuilder rpt = new StringBuilder();
		rpt.append("<tr>");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getAccountName())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getRepName())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getPhysicianName())).append("</td>\r");
		rpt.append("\t<td>").append(u.getProductType().toString()).append("</td>\r");
		rpt.append("\t<td>").append(UnitAction.getStatusName(u.getStatusId())).append("</td>\r");
		rpt.append("\t<td>").append(this.formatDate(u.getDeployedDate())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getSerialNo())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getSoftwareRevNo())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getHardwareRevNo())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getAccountCity())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getAccountCountry())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getCommentsText())).append("</td>\r");
		rpt.append("</tr>\r");
		
		return rpt.toString();
	}
	
	protected String formatDate(Date d) {
		DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, siteVo.getLocale());

		if (d != null) return df.format(d);
		else return "";
	}

}
