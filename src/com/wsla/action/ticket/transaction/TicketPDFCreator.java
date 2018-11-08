package com.wsla.action.ticket.transaction;

//JDK 1.8.x
import java.util.ResourceBundle;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.report.PDFGenerator;
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
		String path = "/Users/james/Code/git/java/WebCrescendo/binary/service_order.ftl";
		
		String ticketIdText = req.getParameter("ticketIdText");
		TicketEditAction tea = new TicketEditAction(getDBConnection(), getAttributes());
		TicketLedgerAction tla = new TicketLedgerAction(getDBConnection(), getAttributes());
		
		UserVO user = (UserVO) this.getAdminUser(req).getUserExtendedInfo();
		ResourceBundle rb = ResourceBundle.getBundle(WSLAConstants.RESOURCE_BUNDLE, user.getUserLocale());
		
		try {
			// Retrieve all of the ticket data
			TicketVO ticket = tea.getCompleteTicket(ticketIdText);
			ticket.setTimeline(tla.getLedgerForTicket(ticketIdText));
			ticket.setDiagnosticRun(tea.getDiagnostics(ticket.getTicketId()));
			
			// Generate the pdf
			PDFGenerator pdf = new PDFGenerator(path, ticket, rb);
			byte[] pdfFile = pdf.generate();
			getReportObj(ticket, pdfFile, req);
		} catch (Exception e) {
			log.error("Unable to retrieve ticket data", e);
			setModuleData(null, 0, e.getLocalizedMessage());
		}
	}
	
	/**
	 * Builds the WC Report Object to be streamed
	 * @param ticket
	 * @param pdf
	 * @param req
	 * @return
	 */
	public void getReportObj(TicketVO ticket, byte[] pdf, ActionRequest req) {
		
		AbstractSBReportVO report = new DownloadReportVO();
		report.setFileName("so_" + ticket.getTicketIdText() + ".pdf");
		report.setData(pdf);
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, report);
	}
}
