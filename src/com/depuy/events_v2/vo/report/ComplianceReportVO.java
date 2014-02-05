package com.depuy.events_v2.vo.report;

// JDK 1.5.0
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;

// DOM4J libs
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;

// Log4j 1.2.8
import org.apache.log4j.Logger;

import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.depuy.events_v2.vo.PersonVO;
import com.depuy.events_v2.vo.PersonVO.Role;
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
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("eventDate", Convert.formatDate(event.getStartDate(), Convert.DATE_LONG));
		data.put("eventTime", event.getLocationDesc());
		data.put("admSignature", "<u>&nbsp; &nbsp;" + adv.getFullName() + "&nbsp; &nbsp;</u>");
		data.put("approvalDt", "<u>&nbsp; &nbsp;" + Convert.formatDate(adv.getApproveDate(), Convert.DATE_SLASH_PATTERN) + "&nbsp; &nbsp;</u>");
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
		return makeIntoPDF(buf.toString().getBytes());
	}

	private byte[] makeIntoPDF(byte[] bytes) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Tidy tidy = new Tidy(); // obtain a new Tidy instance
		tidy.setXHTML(true);
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			Document doc = tidy.parseDOM(bais, new ByteArrayOutputStream());

			ITextRenderer renderer = new ITextRenderer();
			renderer.setDocument(doc, "http://events.depuy.com");
			renderer.layout();
			renderer.createPDF(os);
		} catch (Exception e) {
			log.error("Error creating PDF File", e);
		}

		return os.toByteArray();
	}

}