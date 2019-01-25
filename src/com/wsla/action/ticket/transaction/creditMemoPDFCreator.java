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
import com.siliconmtn.data.GenericVO;
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
import com.wsla.data.ticket.CreditMemoVO;
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
public class creditMemoPDFCreator extends SBActionAdapter {

	/**
	 * Transaction key for the facade
	 */
	public static final String AJAX_KEY = "CMemoTemplate";
	
	/**
	 * 
	 */
	public creditMemoPDFCreator() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public creditMemoPDFCreator(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("build called in pdf generator");
		// Determine path dynamically to the template file
		String templateDir = req.getRealPath() + attributes.get(Constants.INCLUDE_DIRECTORY) + "templates/";
		String path = templateDir + "credit_memo.ftl";
		UserVO user = (UserVO) this.getAdminUser(req).getUserExtendedInfo();
		String creditMemoId = req.getParameter("creditMemoId");
		try {
			byte[] pdfFile = getCreditMemoPDF(creditMemoId, path, user.getUserLocale());
			getReportObj(creditMemoId, pdfFile, req);
		} catch(Exception e) {
			log.debug("can not generate report" +e);
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
	public byte[] getCreditMemoPDF(String creditMemoId, String path2Templ, Locale locale) throws Exception {
		log.debug("getting credit memo pdf");
		TicketEditAction tea = new TicketEditAction(getDBConnection(), getAttributes());
		TicketLedgerAction tla = new TicketLedgerAction(getDBConnection(), getAttributes());
		ResourceBundle rb = ResourceBundle.getBundle(WSLAConstants.RESOURCE_BUNDLE, locale);

		RefundReplacementTransaction rrt = new RefundReplacementTransaction();
		rrt.setActionInit(actionInit);
		rrt.setAttributes(getAttributes());
		rrt.setDBConnection(getDBConnection());
		
		CreditMemoVO cMemo = rrt.getCompleteCreditMemo(creditMemoId);

		TicketVO ticket = tea.getCompleteTicket(cMemo.getTicketId());
		ticket.setTimeline(tla.getLedgerForTicket(cMemo.getTicketId()));
		ticket.setDiagnosticRun(tea.getDiagnostics(cMemo.getTicketId()));
		//using a generic vo to get both data objects to the pdf generator
		GenericVO gvo = new GenericVO();
		gvo.setKey(ticket);
		gvo.setValue(cMemo);
		
		// Generate the pdf
		PDFGenerator pdf = new PDFGenerator(path2Templ, gvo, rb);
		return pdf.generate();

	}
	
	/**
	 * Builds the WC Report Object to be streamed
	 * @param ticket
	 * @param pdf
	 * @param req
	 * @return
	 */
	public void getReportObj(String creditMemoId, byte[] pdf, ActionRequest req) {
		AbstractSBReportVO report = new DownloadReportVO();
		report.setFileName("so_" + creditMemoId + ".pdf");
		report.setData(pdf);
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, report);
	}
}
