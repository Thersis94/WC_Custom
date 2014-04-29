package com.fastsigns.action;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.util.Convert;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.contact.ContactDataContainer;
import com.smt.sitebuilder.action.contact.ContactDataModuleVO;

/*****************************************************************************
 <p><b>Title</b>: TVSpotCSVReportVO.java</p>
 <p>Description: <b/>Excel representation of the TV Spot contact us portlet/data.</p>
 <p>Copyright: Copyright (c) 2000 - 2014 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Eric Damschroder
 @version 1.0
 @since April 28, 2014
 ***************************************************************************/

public class TVSpotCSVReportVO extends AbstractSBReportVO {
	private static final long serialVersionUID = 1l;
	private ContactDataContainer cdc = null;

	public TVSpotCSVReportVO() {
		super();
		setContentType("text/csv");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("Commercial Consultation Report.csv");
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
		
		for (ContactDataModuleVO vo  : cdc.getData())
			appendRow(vo, rpt);
		
		log.debug("report=" + rpt);
		return rpt.toString().getBytes();
	}
	
	/**
	 * Generate a map of reports for each center
	 * @return
	 */
	public Map<String, StringBuilder> generateCenterReport() {
		log.debug("starting generateReport()");
		Map<String, StringBuilder> byCenter = new HashMap<String, StringBuilder>();
		
		for (ContactDataModuleVO vo  : cdc.getData()) {
			StringBuilder rpt = byCenter.get(vo.getDealerLocation().getOwnerEmail());
			
			// If we don't have this center in the map already start a new one..
			if (rpt == null) {
				rpt = new StringBuilder();
				getHeader(rpt);
			}
			
			appendRow(vo, rpt);
			byCenter.put(vo.getDealerLocation().getOwnerEmail(), rpt);
		}
		
		return byCenter;
	}
	
	
	/**
	 * appends a row to the report.
	 * isolated to it's own method to be reusable by both the website and the batch process
	 * @param vo
	 * @param rpt
	 * @return
	 */
	private void appendRow(ContactDataModuleVO vo, StringBuilder rpt) {
		Date d = vo.getSubmittalDate();
		PhoneNumberFormat pnf = new PhoneNumberFormat(vo.getMainPhone(), PhoneNumberFormat.DASH_FORMATTING);
		rpt.append(Convert.formatDate(d, Convert.DATE_SLASH_PATTERN)).append("\t");
		rpt.append(Convert.formatDate(d, Convert.TIME_LONG_PATTERN)).append("\t");
		rpt.append(vo.getDealerLocationId()).append("\t");
		rpt.append(vo.getDealerLocation().getOwnerName()).append("\t");
		rpt.append(vo.getFullName()).append("\t");
		rpt.append(vo.getEmailAddress()).append("\t");
		rpt.append(pnf.getFormattedNumber()).append("\t");
		rpt.append(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.zipcode.id()))).append("\t");
		rpt.append(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.state.id()))).append("\t");
//		rpt.append(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.industry.id()))).append("\t");
//		rpt.append(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.department.id()))).append("\t");
//		rpt.append(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.title.id()))).append("\t");
//		rpt.append(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.companyNm.id()))).append("\t");
//		rpt.append(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.businessChallenge.id()))).append("\t");
		rpt.append(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.inquiry.id()))).append("\t");
		rpt.append(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.saleAmount.id()))).append("\t");
		Calendar surveySentDt = Calendar.getInstance();
		surveySentDt.setTime(vo.getSubmittalDate());
		surveySentDt.add(Calendar.DAY_OF_YEAR, 7);
		surveySentDt.set(Calendar.HOUR, 6); //6am is when FS email campaigns kick-off
		surveySentDt.set(Calendar.MINUTE, 0);
		surveySentDt.set(Calendar.SECOND, 0);
		rpt.append((Calendar.getInstance().getTime().before(surveySentDt.getTime())) ? "No" : "Yes").append("\t");
		rpt.append(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.rating.id()))).append("\t");
		rpt.append(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.feedback.id()))).append("\t");
		rpt.append(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.consultation.id()))).append("\t");
		TVSpotUtil.Status status = TVSpotUtil.Status.valueOf(vo.getExtData().get(TVSpotUtil.ContactField.status.id()));
		rpt.append(status.getLabel()).append("\t");
		String notes = StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.transactionNotes.id()));
		rpt.append(notes).append("\t\r\n");
	}
	
	private void getHeader(StringBuilder hdr) {
		hdr.append("sep=\t;");
		hdr.append("Commercial Consultation Report - ");
		hdr.append(Convert.formatDate(new Date(),  Convert.DATE_SLASH_PATTERN)).append("\r\n");
		hdr.append("Date\t");
		hdr.append("Time\t");
		hdr.append("Web Number\t");
		hdr.append("Franchise Owner\t");
		hdr.append("Prospect Name\t");
		hdr.append("Prospect Email\t");
		hdr.append("Phone Number\t");
		hdr.append("Zip Code\t");
		hdr.append("State\t");
//		hdr.append("Industry\t");
//		hdr.append("Department\t");
//		hdr.append("Title\t");
//		hdr.append("Company Name\t");
//		hdr.append("Business Challenge\t");
		hdr.append("Customer Request\t");
		hdr.append("Sale Amount\t");
		hdr.append("Survey Sent\t");
		hdr.append("Survey Rating\t");
		hdr.append("Survey Feedback\t");
		hdr.append("Consultation Complete\t");
		hdr.append("Status\t");
		hdr.append("Notes (internal)\t\r\n");
	}
	
}
