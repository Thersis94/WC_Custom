package com.depuy.events.vo.report;

// JDK 1.5.0
import java.util.HashMap;
import java.util.Map;

import com.depuy.events.vo.DePuyEventSeminarVO;
import com.depuy.events.vo.PersonVO;
import com.depuy.events.vo.PersonVO.Role;
import com.siliconmtn.data.report.PDFReport;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
import com.smt.sitebuilder.util.MessageParser;
import com.smt.sitebuilder.util.ParseException;

/****************************************************************************
 * <b>Title</b>: ComplianceReportVO.java
 * <p/>
 * <b>Description: generates a PDF version of the Seminar Compliance form,
 * customized for the Seminar provided.</b>
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author James McKain
 * @version 1.0
 * @since Jan 24, 2014
 ****************************************************************************/
public class ComplianceReportVO extends AbstractSBReportVO {
	private static final long serialVersionUID = 1l;
	private DePuyEventSeminarVO sem;

	public ComplianceReportVO() {
		super();
		setContentType("application/pdf");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("Compliance Form.pdf");
	}

	@Override
	public void setData(Object o) {
		this.sem = (DePuyEventSeminarVO) o;
		setFileName("Seminar " + sem.getRSVPCodes() + " Compliance Form.pdf");
	}

	@Override
	public byte[] generateReport() {
		EventEntryVO event = sem.getEvents().get(0);
		log.debug("printing PDF for "+ event.getEventTypeCd());
		boolean isMitek = StringUtil.checkVal(event.getEventTypeCd()).startsWith("MITEK-");
		AbstractSBReportVO rpt = createReport(event.getEventTypeCd());

		PersonVO adv = new PersonVO(); //if there isn't one yet, we'll need this VO
		for (PersonVO p : sem.getPeople()) {
			if (p.getRoleCode() == Role.ADV) {
				adv = p;
				break;
			}
		}


		//run Freemarker replacements to populate the compliance form for this seminar
		//if the signature is missing, print a bunch of spaces to make it look like a line capable of being signed.
		//same for approval date
		String admSig = adv.getFullName();
		if (admSig.length() == 0) admSig = "          ";
		String apprDt = Convert.formatDate(adv.getApproveDate(), Convert.DATE_SLASH_PATTERN);
		if (apprDt.length() == 0) apprDt =  "      ";
		Map<String, Object> data = new HashMap<>();
		data.put("eventDate", Convert.formatDate(event.getStartDate(), Convert.DATE_LONG));
		data.put("eventLocation", event.getEventName() + " " + event.getAddressText() + " " + event.getCityName() + ", " + event.getStateCode() + " " + event.getZipCode());
		data.put("admSignature", admSig);
		data.put("approvalDt", apprDt);
		data.put("ownerName", sem.getOwner().getFullName());
		data.put("territoryNo", sem.getTerritoryNumber());
		StringBuilder rep = new StringBuilder(50);
		for (PersonVO p : sem.getPeople()) {
			if (p.getRoleCode() == Role.TGM && !isMitek) continue;
			//combine both reps into one String
			if (rep.length() > 0) rep.append(", ");
			rep.append(p.getFullName());
		}
		data.put("repName", rep.toString());

		String msg;
		try {
			msg = MessageParser.parse(new String(rpt.generateReport()), data, event.getEventTypeCd());
		} catch (ParseException e) {
			log.error("could not generate PDF for Seminar", e);
			msg = "The compliance form could not be populated.  Please contact the site administrator for assistance";
		}


		//convert the html to a PDF, and return it
		PDFReport pdf = new PDFReport("http://events.depuysynthes.com");
		pdf.setData(msg);
		return pdf.generateReport();
	}


	/**
	 * load the proper html-formated report
	 * @param type
	 * @return
	 */
	protected AbstractSBReportVO createReport(String type) {
		switch (StringUtil.checkVal(type).toUpperCase()) {
			case "CPSEM":
				return new CPSEMReportVO();
			case "CFSEM50":
				return new CFSEM50ReportVO();
			case "MITEK-PEER": //Mitek P2P
				return new MitekPEERReportVO();
			case "MITEK-ESEM": //Mitek Patient
				return new MitekESEMReportVO();
			default:
				return new ESEMReportVO();
		}
	}
}