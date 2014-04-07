package com.depuy.events_v2.vo.report;

// JDK 1.5.0
import java.util.HashMap;
import java.util.Map;

// Log4j 1.2.8
import org.apache.log4j.Logger;

import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.depuy.events_v2.vo.PersonVO;
import com.depuy.events_v2.vo.PersonVO.Role;
import com.siliconmtn.data.report.PDFReport;
import com.siliconmtn.util.Convert;
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
	private static Logger log = null;
	private DePuyEventSeminarVO sem = null;

	/**
	 * 
	 */
	public ComplianceReportVO() {
		super();
		log = Logger.getLogger(getClass());
		setContentType("application/pdf");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("Compliance Form.pdf");
	}

	public void setData(Object o) {
		this.sem = (DePuyEventSeminarVO) o;
		setFileName("Seminar " + sem.getRSVPCodes() + " Compliance Form.pdf");
	}

	public byte[] generateReport() {
		AbstractSBReportVO rpt = null;
		EventEntryVO event = sem.getEvents().get(0);
		log.debug("printing PDF for "+ event.getEventTypeCd());
		
		PersonVO adv = new PersonVO(); //if there isn't one yet, we'll need this VO
		for (PersonVO p : sem.getPeople()) {
			if (p.getRoleCode() != Role.ADV) continue;
			adv = p;
			break;
		}
		
		// load the proper html-formated report
		if (event.getEventTypeCd().equalsIgnoreCase("CFSEM")) {
			rpt = new CFSEMReportVO();
		} else if (event.getEventTypeCd().equalsIgnoreCase("CPSEM")) {
			rpt = new CPSEMReportVO();
		} else {
			rpt = new ESEMReportVO();
		}
		
		//run Freemarker replacements to populate the compliance form for this seminar
		//if the signature is missing, print a bunch of spaces to make it look like a line capable of being signed.
		//same for approval date
		String admSig = adv.getFullName();
		if (admSig.length() == 0) admSig = "&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;";
		String apprDt = Convert.formatDate(adv.getApproveDate(), Convert.DATE_SLASH_PATTERN);
		if (apprDt.length() == 0) apprDt =  "&nbsp; &nbsp; &nbsp; &nbsp;";
		
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("eventDate", Convert.formatDate(event.getStartDate(), Convert.DATE_LONG));
		data.put("eventLocation", event.getEventName() + " " + event.getAddressText() + " " + event.getCityName() + ", " + event.getStateCode() + " " + event.getZipCode());
		data.put("admSignature", "<u>&nbsp; &nbsp;" + admSig + "&nbsp; &nbsp;</u>");
		data.put("approvalDt", "<u>&nbsp; &nbsp;" + apprDt + "&nbsp; &nbsp;</u>");
		data.put("ownerName", sem.getOwner().getFullName());
		data.put("territoryNo", sem.getTerritoryNumber());
		for (PersonVO p : sem.getPeople()) {
			if (p.getRoleCode() == Role.TGM) continue;
			//combine both reps into one String
			String rep =  p.getFullName();
			if (data.containsKey("repName")) 	rep += ", " + data.get("repName");
			data.put("repName", rep);
		}
		
		StringBuffer buf = null;
		try {
			buf = MessageParser.getParsedMessage(new String(rpt.generateReport()), data, event.getEventTypeCd());
		} catch (ParseException e) {
			log.error("could not generate PDF for Seminar", e);
			buf = new StringBuffer("The compliance form could not be populated.  Please contact the site administrator for assistance");
		}

		
		//convert the html to a PDF, and return it
		PDFReport pdf = new PDFReport("http://events.depuy.com");
		pdf.setData(buf.toString());
		return pdf.generateReport();
	}
}