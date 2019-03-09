package com.wsla.action.ticket.transaction;

//JDK 1.8.x
import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.ResourceBundle;

// itext 2.2
import com.lowagie.text.DocumentException;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.report.PDFGenerator;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.smt.sitebuilder.action.AbstractSBReportVO;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.report.vo.DownloadReportVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.wsla.action.ticket.TicketEditAction;
import com.wsla.action.ticket.TicketLedgerAction;
import com.wsla.common.WSLAConstants;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.UserVO;

import freemarker.template.TemplateException;

/****************************************************************************
 * <b>Title</b>: TicketPDFCreator.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Builds a PDF formatted view of the service order
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Nov 5, 2018
 * @updates:
 ****************************************************************************/
public class TicketPDFCreator extends SBActionAdapter {

	/**
	 * Transaction key for the facade
	 */
	public static final String AJAX_KEY = "pdf";
	
	/**
	 * 
	 */
	public TicketPDFCreator() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TicketPDFCreator(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// Determine path dynamically to the template file
		String templateDir = req.getRealPath() + attributes.get(Constants.INCLUDE_DIRECTORY) + "templates/";
		String path = templateDir + "service_order.ftl";
		UserVO user = (UserVO) this.getAdminUser(req).getUserExtendedInfo();
		String ticketIdText = req.getParameter("ticketIdText");
		
		try {
			byte[] pdfFile = getServiceOrderPDF(ticketIdText, path, user.getUserLocale());
			getReportObj(ticketIdText, pdfFile, req);
		} catch(Exception e) {
			setModuleData(null,  0, e.getLocalizedMessage());
		}
		
	}
	
	/**
	 * Builds the PDF File and returns in a byte array
	 * @param ticketIdText
	 * @param path2Templ
	 * @param locale
	 * @return
	 * @throws DatabaseException
	 * @throws SQLException
	 * @throws InvalidDataException
	 * @throws IOException
	 * @throws TemplateException
	 * @throws DocumentException
	 */
	public byte[] getServiceOrderPDF(String ticketIdText, String path2Templ, Locale locale) 
	throws DatabaseException, SQLException, InvalidDataException, IOException, TemplateException, DocumentException {
		TicketEditAction tea = new TicketEditAction(getDBConnection(), getAttributes());
		TicketLedgerAction tla = new TicketLedgerAction(getDBConnection(), getAttributes());
		ResourceBundle rb = ResourceBundle.getBundle(WSLAConstants.RESOURCE_BUNDLE, locale);

		// Retrieve all of the ticket data
		TicketVO ticket = tea.getCompleteTicket(ticketIdText);
		ticket.setTimeline(tla.getLedgerForTicket(ticketIdText));
		ticket.setDiagnosticRun(tea.getDiagnostics(ticket.getTicketId()));
		
		// Generate the pdf
		PDFGenerator pdf = new PDFGenerator(path2Templ, ticket, rb);
		return pdf.generate();

	}
	
	/**
	 * Builds the WC Report Object to be streamed
	 * @param ticket
	 * @param pdf
	 * @param req
	 * @return
	 */
	public void getReportObj(String ticketIdText, byte[] pdf, ActionRequest req) {
		
		AbstractSBReportVO report = new DownloadReportVO();
		report.setFileName("so_" + ticketIdText + ".pdf");
		report.setData(pdf);
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, report);
	}
}
