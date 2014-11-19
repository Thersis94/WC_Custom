/**
 * 
 */
package com.codman.cu.tracking.vo;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import com.codman.cu.tracking.UnitAction;
import com.codman.cu.tracking.vo.UnitVO.ProdType;
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
 ****************************************************************************/
public class UnitHistoryReportVO extends AbstractSBReportVO {

	private static final long serialVersionUID = 1407073622234040274L;
	protected List<UnitVO> data;
	protected SiteVO siteVo;

	public UnitHistoryReportVO(SiteVO site) {
		super();
		this.siteVo = site;
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("Control Unit History Report.xls");
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("starting Unit History Report");
		StringBuilder rpt = new StringBuilder(this.getHeader(false));

		//loop the accounts, physians, units, and requests
		for (UnitVO v : data) {
			rpt.append(formatUnit(v, false));
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

	protected StringBuilder getHeader(boolean restricted) {
		int colCnt = restricted ? 24: 31;
		StringBuilder hdr = new StringBuilder();
		hdr.append("<tr><td><table border='1'>\r");
		hdr.append("<tr><td colspan='").append(colCnt).append("' style='background-color: #ccc;'><b>Codman CU Tracking System - Unit History</b></td></tr>\r");
		hdr.append("<tr><td>Date</td>");
		hdr.append("\t<td>Status</td>");
		hdr.append("\t<td>Unit Type</td>");
		hdr.append("\t<td>Transaction Type</td>");
		hdr.append("\t<td>Serial No.</td>");
		hdr.append("\t<td>User</td>");
		hdr.append("\t<td>Software Rev No.</td>");
		if (!restricted) { 
			hdr.append("\t<td>Hardware Rev No.</td>");
		}
		hdr.append("\t<td>IFU Article No.</td>");
		hdr.append("\t<td>IFU Rev No.</td>");
		hdr.append("\t<td>Prog Article No.</td>");
		hdr.append("\t<td>Prog Rev No.</td>");
		if (!restricted) { 
			hdr.append("\t<td>Battery Type</td>");
			hdr.append("\t<td>Battery Serial No.</td>");
			hdr.append("\t<td>Lot No.</td>");
			hdr.append("\t<td>Service/Repair No.</td>");
			hdr.append("\t<td>Service/Repair Date</td>");
		}
		hdr.append("\t<td>Comments</td>");
		if (!restricted) {
			hdr.append("\t<td>Production Comments</td>");
		}
		hdr.append("\t<td>Date Deployed</td>");
		hdr.append("\t<td>Account</td>");
		hdr.append("\t<td>Rep Name</td>");
		hdr.append("\t<td>Physician Name</td>");
		hdr.append("\t<td>Center</td>");
		hdr.append("\t<td>Department</td>");
		hdr.append("\t<td>Physician Phone#</td>");
		hdr.append("\t<td>Physician Address</td>");
		hdr.append("\t<td>Address2</td>");
		hdr.append("\t<td>City</td>");
		hdr.append("\t<td>State</td>");
		hdr.append("\t<td>Zip/Postal</td>");
		hdr.append("\t<td>Country</td>");
		hdr.append("</tr>\r");

		return hdr;
	}


	protected StringBuilder getFooter() {
		return new StringBuilder("</table>");
	}

	protected String formatUnit(UnitVO u, boolean restricted) {
		StringBuilder rpt = new StringBuilder();
		rpt.append("<tr>");
		rpt.append("\t<td>").append(this.formatDate(u.getCreateDate())).append("</td>\r");
		rpt.append("\t<td>").append(UnitAction.getStatusName(u.getStatusId())).append("</td>\r");
		rpt.append("\t<td>").append(u.getProductType().toString()).append("</td>\r");
		String transType = "";
		if (u.getTransactionType() == null || u.getTransactionType() == 0) transType = "Unit Update";
		else if (u.getTransactionType() == 2 && u.getProductType() == ProdType.ICP_EXPRESS) transType = "Return";
		else if (u.getTransactionType() == 2) transType = "Transfer";
		else if (u.getTransactionType() == 3) transType = "Refurbish";
		else if (u.getTransactionType() == 1) transType = "New Request";

		rpt.append("\t<td>").append(transType).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getSerialNo())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getModifyingUserName())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getSoftwareRevNo())).append("</td>\r");
		if (!restricted) {
			rpt.append("\t<td>").append(StringUtil.checkVal(u.getHardwareRevNo())).append("</td>\r");
		}
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getIfuArticleNo())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getIfuRevNo())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getProgramArticleNo())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getProgramRevNo())).append("</td>\r");
		if (!restricted) {
			rpt.append("\t<td>").append(StringUtil.checkVal(u.getBatteryType())).append("</td>\r");
			rpt.append("\t<td>").append(StringUtil.checkVal(u.getBatterySerNo())).append("</td>\r");
			rpt.append("\t<td>").append(StringUtil.checkVal(u.getLotNo())).append("</td>\r");
			rpt.append("\t<td>").append(StringUtil.checkVal(u.getServiceRefNo())).append("</td>\r");
			rpt.append("\t<td>").append(this.formatDate(u.getServiceDate())).append("</td>\r");
		}
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getCommentsText())).append("</td>\r");
		if (!restricted) {
			rpt.append("\t<td>").append(StringUtil.checkVal(u.getProductionCommentsText())).append("</td>\r");
		}
		rpt.append("\t<td>").append(this.formatDate(u.getDeployedDate())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getAccountName())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getRepName())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getPhysicianName())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getPhysician().getCenterText())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getPhysician().getDepartmentText())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getPhysician().getMainPhone())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getPhysician().getAddress())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getPhysician().getAddress2())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getPhysician().getCity())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getPhysician().getState())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getPhysician().getZipCode())).append("</td>\r");
		rpt.append("\t<td>").append(StringUtil.checkVal(u.getPhysician().getCountryCode())).append("</td>\r");
		rpt.append("</tr>\r");

		return rpt.toString();
	}

	protected String formatDate(Date d) {
		DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, siteVo.getLocale());

		if (d != null) return df.format(d);
		else return "";
	}


}
