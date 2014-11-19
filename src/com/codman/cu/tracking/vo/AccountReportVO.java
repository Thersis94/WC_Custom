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
public class AccountReportVO extends AbstractSBReportVO {
	private static final long serialVersionUID = -4473379747242916803L;
	private List<AccountVO> data;
	
	public AccountReportVO() {
		super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName("CU Accounts.xls");
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@SuppressWarnings("incomplete-switch")
	@Override
	public byte[] generateReport() {
		log.debug("starting Account Reports");
		StringBuilder rpt = new StringBuilder(this.getHeader());
		
		//loop the accounts, physians, units, and requests
		for (AccountVO acct : data) {
			rpt.append("<tr valign='top'>");
			rpt.append("<td>").append(StringUtil.checkVal(acct.getRep().getFirstName())).append(" ").append(StringUtil.checkVal(acct.getRep().getLastName())).append("</td>\r");
			rpt.append("\t<td>").append(StringUtil.checkVal(acct.getRep().getTerritoryId())).append("</td>\r");
			rpt.append("\t<td>").append(StringUtil.checkVal(acct.getRep().getSampleAccountNo())).append("</td>\r");
			rpt.append("\t<td>").append(StringUtil.checkVal(acct.getAccountName())).append("</td>\r");
			rpt.append("\t<td>").append(StringUtil.checkVal(acct.getAccountNo())).append("</td>\r");
			rpt.append("\t<td>").append(acct.getPhysicians().size()).append("</td>\r");
			rpt.append("\t<td>").append(acct.getUnitCount()).append("</td>\r");
			rpt.append("\t<td>");
			//iterated the transactions and classify by status
			int pending = 0, approved = 0, complete = 0, denied = 0;
			for (TransactionVO v : acct.getTransactions()) {
				switch (v.getStatus()) {
					case PENDING:
						++pending;
						break;
					case APPROVED:
						++approved;
						break;
					case COMPLETE:
						++complete;
						break;
					case DECLINED:
						++denied;
						break;
				}
				}
			if (pending > 0) rpt.append(pending).append(" Pending<br/>\r");
			if (approved > 0) rpt.append(approved).append(" Approved<br/>\r");
			if (complete > 0) rpt.append(complete).append(" Completed<br/>\r");
			if (denied > 0) rpt.append(denied).append(" Denied");
			rpt.append("</td>");
			
			rpt.append("\t<td>").append(StringUtil.checkVal(acct.getAccountPhoneNumber())).append("</td>\r");
			rpt.append("\t<td>").append(acct.getAccountAddress()).append("</td>\r");
			rpt.append("\t<td>").append(StringUtil.checkVal(acct.getAccountAddress2())).append("</td>\r");
			rpt.append("\t<td>").append(acct.getAccountCity()).append("</td>\r");
			rpt.append("\t<td>").append(acct.getAccountState()).append("</td>\r");
			rpt.append("\t<td>").append(acct.getAccountZipCode()).append("</td>\r");
			rpt.append("\t<td>").append(acct.getAccountCountry()).append("</td>\r");		
			rpt.append("</tr>\r");
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
		hdr.append("<tr><td colspan='15' style='background-color: #ccc;'><b>MedStream CU Tracking System - Account Summary</b></td></tr>\r");
		hdr.append("<tr><td>Rep Name</td>\r");
		hdr.append("\t<td>Territory</td>\r");
		hdr.append("\t<td>Sample Acct. No.</td>\r");
		hdr.append("\t<td>Account Name</td>\r");
		hdr.append("\t<td>Account No.</td>\r");
		hdr.append("\t<td>Physicians</td>\r");
		hdr.append("\t<td>Units</td>\r");
		hdr.append("\t<td>Requests</td>\r");
		hdr.append("\t<td>Phone</td>\r");
		hdr.append("\t<td>Address</td>\r");
		hdr.append("\t<td>Address2</td>\r");
		hdr.append("\t<td>City</td>\r");
		hdr.append("\t<td>State</td>\r");
		hdr.append("\t<td>Zip</td>\r");
		hdr.append("\t<td>Country</td></tr>\r");
		return hdr;
	}

	
	private StringBuilder getFooter() {
		return new StringBuilder("</table>");
	}
	
}
