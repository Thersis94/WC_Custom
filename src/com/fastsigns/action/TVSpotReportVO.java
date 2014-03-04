package com.fastsigns.action;

import java.util.Date;

import com.siliconmtn.util.Convert;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.contact.ContactDataContainer;
import com.smt.sitebuilder.action.contact.ContactDataModuleVO;

/*****************************************************************************
 <p><b>Title</b>: TVSpotReportVO.java</p>
 <p>Description: <b/>Excel representation of the TV Spot contact us portlet/data.</p>
 <p>Copyright: Copyright (c) 2000 - 2014 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Feb 27, 2014
 ***************************************************************************/

public class TVSpotReportVO extends AbstractSBReportVO {
	private static final long serialVersionUID = 1l;
	private ContactDataContainer cdc = null;

	public TVSpotReportVO() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("Commercial Consultation Report.xls");
	}

	/**
	 * set the data object
	 */
	@Override
	public void setData(Object o) {
		cdc = (ContactDataContainer) o;
	}

	@Override
	public byte[] generateReport() {
		log.debug("starting generateReport()");
		StringBuilder rpt = new StringBuilder();
		this.getHeader(rpt);
		
		for (ContactDataModuleVO vo  : cdc.getData()) {
			Date d = vo.getSubmittalDate();
			PhoneNumberFormat pnf = new PhoneNumberFormat(vo.getMainPhone(), PhoneNumberFormat.DASH_FORMATTING);
			rpt.append("<tr><td>").append(Convert.formatDate(d, Convert.DATE_SLASH_PATTERN)).append("</td>");
			rpt.append("<td>").append(Convert.formatDate(d, Convert.TIME_LONG_PATTERN)).append("</td>");
			rpt.append("<td>").append(vo.getDealerLocationId()).append("</td>");
			rpt.append("<td>").append(vo.getFullName()).append("</td>");
			rpt.append("<td>").append(vo.getEmailAddress()).append("</td>");
			rpt.append("<td>").append(pnf.getFormattedNumber()).append("</td>");
			rpt.append("<td>").append(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.zipcode.id()))).append("</td>");
			rpt.append("<td>").append(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.industry.id()))).append("</td>");
			rpt.append("<td>").append(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.department.id()))).append("</td>");
			rpt.append("<td>").append(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.title.id()))).append("</td>");
			rpt.append("<td>").append(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.companyNm.id()))).append("</td>");
			rpt.append("<td>").append(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.businessChallenge.id()))).append("</td>");
			TVSpotUtil.Status status = TVSpotUtil.Status.valueOf(vo.getExtData().get(TVSpotUtil.ContactField.status.id()));
			if (status == TVSpotUtil.Status.initiated) {
				rpt.append("<td color=\"red\">").append(status.getLabel()).append("</td>");
			} else {
				rpt.append("<td>").append(status.getLabel()).append("</td>");
			}
			String notes = StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.transactionNotes.id()));
			notes = notes.replaceAll("\\r\\n", "<br>");
			rpt.append("<td>").append(notes).append("</td></tr>");
		}

		this.getFooter(rpt);
		log.debug("report=" + rpt);
		return rpt.toString().getBytes();
	}
	
	private void getHeader(StringBuilder hdr) {
		hdr.append("<table border='1'>\r");
		hdr.append("<tr><td colspan='14' style='background-color: #ccc;'><b>Commercial Consultation Report - ");
		hdr.append(Convert.formatDate(new Date(),  Convert.DATE_SLASH_PATTERN)).append("</b></td></tr>\r");
		hdr.append("<tr><th>Date</th>");
		hdr.append("<th>Time</th>");
		hdr.append("<th>Web Number</th>");
		hdr.append("<th>Prospect Name</th>");
		hdr.append("<th>Prospect Email</th>");
		hdr.append("<th>Phone Number</th>");
		hdr.append("<th>Zip Code</th>");
		hdr.append("<th>Industry</th>");
		hdr.append("<th>Department</th>");
		hdr.append("<th>Title</th>");
		hdr.append("<th>Company Name</th>");
		hdr.append("<th>Business Challenge</th>");
		hdr.append("<th>Status</th>");
		hdr.append("<th>Notes</th></tr>");
	}
	
	private void getFooter(StringBuilder sb) {
		sb.append("</table>");
	}
	
}