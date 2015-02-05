package com.codman.cu.tracking.vo;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import com.codman.cu.tracking.vo.UnitVO.ProdType;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.common.SiteVO;

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
public class TransactionReportVO extends AbstractSBReportVO {
	private static final long serialVersionUID = -4473379747242916803L;
	private List<AccountVO> data;
	protected SiteVO siteVo;

	public TransactionReportVO(SiteVO site) {
		super();
		this.siteVo = site;
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("Control Unit Transaction Report.xls");
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
			for (TransactionVO t : acct.getTransactions()) {
				rpt.append("<tr>");
				rpt.append("\t<td>").append(StringUtil.checkVal(acct.getAccountName())).append("</td>\r");
				rpt.append("\t<td>").append(StringUtil.checkVal(t.getRequestorName())).append("</td>\r");
				rpt.append("\t<td>").append(t.getPhysician().getFirstName()).append(" ").append(t.getPhysician().getLastName()).append("</td>\r");
				rpt.append("\t<td>").append(t.getStatusStr()).append("</td>\r");
				rpt.append("\t<td>").append(this.formatDate(t.getCreateDate())).append("</td>\r");
				rpt.append("\t<td>").append(StringUtil.checkVal(t.getApprovorName())).append("</td>\r");
				rpt.append("\t<td>").append(this.formatDate(t.getCompletedDate())).append("</td>\r");
				rpt.append("\t<td>").append(StringUtil.checkVal(t.getRequestNo())).append("</td>\r");
				//request type
				if (t.getTransactionTypeId() == 2 && t.getProductType() == ProdType.ICP_EXPRESS) {
					rpt.append("\t<td>Return</td>\r");
				} else if (t.getTransactionTypeId() == 2) {
					rpt.append("\t<td>Transfer</td>\r");
				} else if (t.getTransactionTypeId() == 3) {
					rpt.append("\t<td>Refurbish</td>\r");
				} else {
					rpt.append("\t<td>New Request</td>\r");
				}
				rpt.append("\t<td>").append(t.getProductTypeStr()).append("</td>\r");
				rpt.append("\t<td>").append(t.getUnitCount()).append("</td>\r");
				rpt.append("\t<td>").append(t.getUnits().size()).append("</td>\r");
				rpt.append("\t<td>").append(t.getUnitSerialNos()).append("</td>\r");
				rpt.append("\t<td>").append(t.getShipToName()).append("<br/>\r").append(t.getShippingAddress().getFormattedLocation()).append("</td>\r");
				rpt.append("\t<td>").append(t.getRequestorName()).append("</td>\r");
				rpt.append("\t<td>").append(this.formatDate(t.getCreateDate())).append("</td>\r");
				rpt.append("\t<td>").append(StringUtil.checkVal(t.getNotesText())).append("</td>\r");
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
		hdr.append("<tr><td colspan='16' style='background-color: #ccc;'><b>Codman CU Tracking System - Request Summary</b></td></tr>\r");
		hdr.append("\t<tr><td>Account</td>");
		hdr.append("\t<td>Request Submitted By</td>");
		hdr.append("\t<td>Intended User (Physician)</td>");
		hdr.append("\t<td>Request Status</td>");
		hdr.append("\t<td>Date Submitted</td>");
		hdr.append("\t<td>Request Overviewed By</td>");
		hdr.append("\t<td>Date Completed</td>");
		hdr.append("\t<td>Request No.</td>");
		hdr.append("\t<td>Request Type</td>");
		hdr.append("\t<td>Unit Type</td>");
		hdr.append("\t<td>Units Requested</td>");
		hdr.append("\t<td>Units Sent</td>");
		hdr.append("\t<td>Unit Serial No.(s)</td>");
		hdr.append("\t<td>Shipped To</td>");
		hdr.append("\t<td>Transaction Assignee</td>");
		hdr.append("\t<td>Transaction Date</td>");
		hdr.append("\t<td>Comments</td></tr>\r");

		return hdr;
	}


	private StringBuilder getFooter() {
		return new StringBuilder("</table>");
	}

	protected String formatDate(Date d) {
		DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, siteVo.getLocale());

		if (d != null) return df.format(d);
		else return "";
	}
}
