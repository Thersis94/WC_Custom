package com.depuy.events_v2.vo.report;

// JDK 1.5.0
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.w3c.dom.Document;

// DOM4J libs
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;

// Log4j 1.2.8
import org.apache.log4j.Logger;

import com.smt.sitebuilder.action.AbstractSBReportVO;

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

	}

	public byte[] generateReport() {
		// load the proper html-formated report
		StringBuilder doc = getHeader();
		
		//substitute in the replacement items using FreeMarker

		//convert the html to a PDF, and return it
		return makeIntoPDF(doc);
	}

	private byte[] makeIntoPDF(StringBuilder sb) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Tidy tidy = new Tidy(); // obtain a new Tidy instance
		tidy.setXHTML(true);
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(sb.toString().getBytes());
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

	private StringBuilder getHeader() {
		return new StringBuilder("this should be a report in PDF format");
	}
}