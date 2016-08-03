/**
 *
 */
package com.depuysynthesinst.lms;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.depuysynthesinst.DSIUserDataVO;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.action.pdf.AbstractLabelPDF;
import com.smt.sitebuilder.action.pdf.PDFLabelException;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: NursingCertificateLabeler.java <b>Project</b>: WC_Custom
 * <b>Description: </b> PDFLabel class that adds a Nurses name, location and
 * 	cert completion date to the Certification PDF.
 * <b>Copyright:</b> Copyright (c) 2016
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since May 16, 2016
 ****************************************************************************/
public class NursingCertificateLabeler extends AbstractLabelPDF {
	public static final String DEPUY_FONT = "font/fruitiger/FrutigerLTStd-Light.otf";
	private static BaseFont baseFont = null;


	/**
	 * @throws IOException
	 * @throws DocumentException
	 */
	public void setFonts(String path) {
		try {
			log.debug("Getting fonts: " + path + DEPUY_FONT);
			baseFont = BaseFont.createFont(path	+ DEPUY_FONT, BaseFont.WINANSI, BaseFont.EMBEDDED);
		} catch (Exception e) {
			log.error("Error setting up fonts", e);
		}
	}


	/*
	 * (non-Javadoc)
	 * @see
	 * com.smt.sitebuilder.action.pdf.AbstractLabelPDF#labelPDFFile(java.util.
	 * Map, java.lang.String)
	 */
	@Override
	public byte[] labelPDFFile(Map<String, Object> params, String path) throws PDFLabelException {
		// Set the fonts
		this.setFonts(path);

		// Get the user information
		UserDataVO user = (UserDataVO) params.get(Constants.USER_DATA);
		if (user == null)
			user = new DSIUserDataVO();

		// Retrieve the cover letter
		String file = this.getFileName(path);
		PdfReader reader = this.getReader(file);

		// we retrieve the size of the first page
		Rectangle psize = reader.getPageSize(1);

		// step 1: creation of a document-object
		Document document = new Document(psize);

		// step 2: we create a writer that listens to the document
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PdfWriter writer = this.getWriter(document, baos);

		// step 3: we open the document
		document.open();

		// Import the pages for editing
		PdfImportedPage page1 = writer.getImportedPage(reader, 1);

		// Add the first page to the new document
		PdfContentByte cb = writer.getDirectContent();
		cb.addTemplate(page1, 1f, 0, 0, 1f, 0, 0);
		cb.setFontAndSize(baseFont, 24);

		// Need to call Begin and End Text otherwise Adobe wont recognize Text.
		cb.beginText();

		// Write Nurse Name
		cb.showTextAligned(PdfContentByte.ALIGN_CENTER, getUserLabel(user), psize.getWidth() / 2, 482, 0);
		cb.setFontAndSize(baseFont, 16);

		// Write the Course Name.
		cb.showTextAligned(PdfContentByte.ALIGN_CENTER, getCourseName(params.get("courseName")), psize.getWidth() / 2, 414, 0);

		// Write the Course Complete Date
		cb.showTextAligned(PdfContentByte.ALIGN_CENTER, getCourseCompleteDate(params.get("COURSECOMPLETEDATE")), psize.getWidth() / 2, 389, 0);

		// Call EndText to ensure Adobe recognizes text.
		cb.endText();

		// step 5: we close the document
		document.close();

		return baos.toByteArray();
	}


	/**
	 * Method returns a PDFWriter for the given Document.
	 * @param document
	 * @param baos
	 * @return
	 * @throws PDFLabelException
	 */
	protected PdfWriter getWriter(Document document, ByteArrayOutputStream baos) throws PDFLabelException {
		PdfWriter writer = null;
		try {
			writer = PdfWriter.getInstance(document, baos);
		} catch (DocumentException e) {
			throw new PDFLabelException("Error getting instance", e);
		}

		return writer;
	}


	/**
	 * Method returns a PDFReader for the given FilePath.
	 * @param file
	 * @return
	 * @throws PDFLabelException
	 */
	public PdfReader getReader(String file) throws PDFLabelException {
		PdfReader reader = null;

		try {
			reader = new PdfReader(file);
		} catch (IOException e) {
			throw new PDFLabelException("Error getting instance", e);
		}

		return reader;
	}


	/**
	 * Method returns the UserName for the certificate.
	 * @param doc
	 * @param user
	 * @throws DocumentException
	 */
	protected String getUserLabel(UserDataVO user) {
		String userName = "";

		// Make sure we have user info before displaying the header
		if (user != null && user.getFullName().length() > 0) {
			userName = user.getFullName();
		}

		return userName;
	}

	/**
	 * Method returns the CourseName for the certificate.
	 * @param courseName
	 * @return
	 */
	protected String getCourseName(Object courseName) {
		String name = "";
		if(courseName != null) {
			name = ((String[])courseName)[0];
		}

		return name;
	}

	/**
	 * MEthod returns the Course Complete Date for the certificate.
	 * @param courseComplete
	 * @return
	 */
	protected String getCourseCompleteDate(Object courseComplete) {
		String completeDate = "";
		try {
			Date d = new SimpleDateFormat("yyyy-MM-dd").parse(((String[])courseComplete)[0]);
			completeDate = new SimpleDateFormat("MMM dd, yyyy").format(d);
		} catch (ParseException e) {
			log.error("Problem Formatting Nurse Certificate Course Complete Date", e);
		}

		return completeDate;
	}

	/**
	 * Returns the FileName of the PDF in Templates directory.
	 */
	@Override
	public String getFileName() {
		return "Nurse_CertificateV3.pdf";
	}


	/**
	 * Method returns the PDF File Name for the Certificate
	 * @param path
	 * @return
	 */
	protected String getFileName(String path) {
		String file = path + "Nurse_CertificateV3.pdf";
		return file;
	}
}