package com.depuy.sitebuilder.pdf;

// JDK 1.6.x
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

// iText 2.1.x
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

// Site Builder Imports
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.pdf.AbstractLabelPDF;
import com.smt.sitebuilder.action.pdf.PDFLabelException;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>:DePuyKneeLabeler.java<p/>
 * <b>Description: </b> Takes the DePuy knee PDF cover letter and adds 
 * the user name and address to the document
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Sep 29, 2008
 * <b>Changes: </b>
 ****************************************************************************/
public class PACertificateLabeler extends AbstractLabelPDF {
	public static final String DEPUY_FONT = "font/fruitiger/FrutigerLTStd-Light.otf";
	private static BaseFont baseFont = null;

	/**
	 * @throws IOException 
	 * @throws DocumentException 
	 * 
	 */
	public void setFonts(String path) {
		try {
			log.debug("Getting fonts: " + path + DEPUY_FONT);
			baseFont = BaseFont.createFont(path + DEPUY_FONT, BaseFont.WINANSI, BaseFont.EMBEDDED);
		} catch (Exception e) {
			log.error("Error setting up fonts", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.pdf.AbstractLabelPDF#labelPDFFile(java.util.Map, java.lang.String)
	 */
	@Override
	public byte[] labelPDFFile(Map<String, Object> params, String path)
			throws PDFLabelException {
		// Set the fonts
		this.setFonts(path);

		// Get the joint information
		String type = StringUtil.checkVal(params.get("JOINT"), "knee").toLowerCase();
		log.debug("Type: " + type);

		// Get the user information
		UserDataVO user = (UserDataVO) params.get(Constants.USER_DATA);
		if (user == null) user = new UserDataVO();

		// Retrieve the cover letter
		String file = this.getFileName(path, type);
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
		cb.setFontAndSize(baseFont, 36);
		cb.showTextAligned(PdfContentByte.ALIGN_CENTER, getUserLabel(user), psize.getWidth()/2, 330, 0);
		cb.endText();

		// step 5: we close the document
		document.close();

		return baos.toByteArray();
	}

	/**
	 * 
	 * @param document
	 * @param baos
	 * @return
	 * @throws PDFLabelException
	 */
	protected PdfWriter getWriter(Document document, ByteArrayOutputStream baos) 
			throws PDFLabelException {
		PdfWriter writer = null;
		try {
			writer = PdfWriter.getInstance(document, baos);
		} catch (DocumentException e) {
			log.debug("Error", e);
			throw new PDFLabelException("Error getting instance", e);
		}

		return writer;
	}

	/**
	 * 
	 * @param file
	 * @return
	 * @throws PDFLabelException
	 */
	public PdfReader getReader(String file) throws PDFLabelException {
		PdfReader reader = null;

		try {
			reader = new PdfReader(file);
		} catch (IOException e) {
			log.debug("Error", e);
			throw new PDFLabelException("Error getting instance", e);
		}

		return reader;
	}

	/**
	 * 
	 * @param path
	 * @return
	 */
	protected String getFileName(String path, String type) {
		String joint = type.toLowerCase();
		String file = path + "DePuy_Certificate_" + joint + ".pdf";
		return file;
	}


	/**
	 * 
	 * @param doc
	 * @param user
	 * @throws DocumentException
	 */
	protected String getUserLabel(UserDataVO user) {
		// Make sure we have user info before displaying the header
		if (user != null && user.getFullName().length() > 0) {
			return user.getFullName();
		} else {
			return "";
		}
	}

	@Override
	public String getFileName() {
		return "Patient_Ambassador_Certificate.pdf";
	}
}