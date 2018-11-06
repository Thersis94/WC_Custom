package com.wsla.action.ticket.transaction;

import java.io.File;

import org.apache.commons.io.FileUtils;

// JDK 1.8.x

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.report.PDFGenerator;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.action.ticket.TicketEditAction;
import com.wsla.action.ticket.TicketLedgerAction;
import com.wsla.data.ticket.TicketVO;

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
		log.info("Building PDF: " + req.getParameter("ticketIdText"));
		String path = "/Users/james/Code/git/java/WebCrescendo/binary/service_order.ftl";
		String pdfPath = "/Users/james/Code/git/java/WebCrescendo/binary/service_order.pdf";
		String ticketIdText = req.getParameter("ticketIdText");
		TicketEditAction tea = new TicketEditAction(getDBConnection(), getAttributes());
		TicketLedgerAction tla = new TicketLedgerAction(getDBConnection(), getAttributes());
		try {
			TicketVO ticket = tea.getCompleteTicket(ticketIdText);
			ticket.setTimeline(tla.getLedgerForTicket(ticketIdText));
			PDFGenerator pdf = new PDFGenerator(path, ticket);
			byte[] pdfFile = pdf.generate();
			FileUtils.writeByteArrayToFile(new File(pdfPath), pdfFile);
			
		} catch (Exception e) {
			log.info("Unable to retrieve ticket data", e);
			setModuleData(null, 0, e.getLocalizedMessage());
		}
	}
}
