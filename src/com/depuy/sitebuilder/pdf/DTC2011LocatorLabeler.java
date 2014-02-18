package com.depuy.sitebuilder.pdf;

// JDK 1.6.x
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

// iText 2.1.x
import com.depuy.datafeed.SurgeonListFactory;
import com.depuy.datafeed.da.AbstractSurgeonList;
import com.depuy.datafeed.da.SurgeonVO;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

//SMT Base Libs
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.Location;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

//Site Builder Imports
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
public class DTC2011LocatorLabeler extends AbstractLabelPDF {
	private static final int PARAGRAPH_MARGIN = 10;
	private static final float PAGE_MARGIN_LEFT = 35;
	private static final float PAGE_MARGIN_TOP = 100;
	private static final float PAGE_MARGIN_BOTTOM = 10;
	private static final float PAGE_MARGIN_RIGHT = 40;
	
	public static final String DEPUY_FONT = "font/fruitiger/FrutigerLTStd-Light.otf";
	protected static final String KNEE_SEARCH = "5";
	protected static final String HIP_SEARCH = "4";
	private static Font baseFont = null;
	private static Font baseFontBold = null;
	
	/**
	 * 
	 */
	public DTC2011LocatorLabeler() {
				
	}
	
	/**
	 * @throws IOException 
	 * @throws DocumentException 
	 * 
	 */
	public void setFonts(String path) {
		try {
			log.debug("Getting fonts: " + path + DEPUY_FONT);
			BaseFont bf = BaseFont.createFont(path + DEPUY_FONT, BaseFont.WINANSI, BaseFont.EMBEDDED);
			baseFont = new Font(bf);
			baseFont.setSize(10);
			
			baseFontBold = new Font(baseFont);
			baseFontBold.setStyle(Font.BOLD);
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
        Document document = new Document(psize,0,PAGE_MARGIN_RIGHT,20,PAGE_MARGIN_BOTTOM);
        
        // step 2: we create a writer that listens to the document
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = this.getWriter(document, baos);
		
        // step 3: we open the document
        document.open();
        List<SurgeonVO> surgeons = null;
        try {
        	surgeons = this.returnSurgeons(user, type);
        	log.debug("Number of surgeons: " + surgeons.size());
        	
			addHeaderLabel(document, user);
			addLocIntro(document, type);
			this.addLocatorResults(document, surgeons.subList(0, 6));
		} catch (Exception e1) {
			log.debug("adding text labels");
		}
       
        // Add the first page to the new document
		PdfImportedPage page1 = writer.getImportedPage(reader, 1);
        PdfContentByte cb = writer.getDirectContent();
        cb.addTemplate(page1, 1f, 0, 0, 1f, 0, 0);
        cb.endText();
        
        // Add a new page to the document
        document.newPage();
        try {
        	this.addLocatorResults(document, surgeons.subList(6, surgeons.size()));
		} catch (Exception e1) {
			log.error("Unable to add text to document", e1);
		}
        
        // step 5: we close the document
        document.close();
        
		return baos.toByteArray();
	}
	
	/**
	 * 
	 * @param doc
	 * @throws DocumentException
	 */
	protected void addLocIntro(Document doc, String type) throws DocumentException {
		StringBuilder s = new StringBuilder();
		s.append("We are pleased to supply you with a list of orthopaedic surgeons in ");
		s.append("your area who use DePuy Orthopaedic products. While our database of ");
		s.append("orthopaedic surgeons is large, it is not a complete listing of all ");
		s.append("orthopaedic surgeons in your area. A surgeon's use of DePuy ");
		s.append("Orthopaedics products is the sole criterion for being listed ");
		s.append("below. No orthopaedic surgeon has paid a fee to participate.");
		
		Paragraph par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingBefore(PARAGRAPH_MARGIN);
		par.setIndentationLeft(PAGE_MARGIN_LEFT);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("DePuy Orthopaedics, Inc. does not make any recommendation or ");
		s.append("referral or any representations regarding any of the specific ");
		s.append("surgeons listed below.");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingBefore(PARAGRAPH_MARGIN);
		par.setIndentationLeft(PAGE_MARGIN_LEFT);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Schedule an appointment today with an orthopaedic surgeon to learn more about ");
		if ("knee".equalsIgnoreCase(type)) {
			s.append("SIGMA\u00A8 Knees.");
		} else {
			s.append("PINNACLE\u00A8 Hips.");
		}
		
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingBefore(PARAGRAPH_MARGIN);
		par.setIndentationLeft(PAGE_MARGIN_LEFT);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("(Orthopaedic surgeons are listed in order of distance from the address ");
		s.append("you provided. For Real Life Tested stories or additional surgeons, ");
		s.append("please visit ");
		
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.add(new Chunk("www.RealLifeTested.com", baseFontBold));
		par.add(new Chunk(")", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingBefore(PARAGRAPH_MARGIN);
		par.setSpacingAfter(PARAGRAPH_MARGIN * 2);
		par.setIndentationLeft(PAGE_MARGIN_LEFT);
		doc.add(par);
	}
	/**
	 * 
	 * @param doc
	 * @param left
	 * @throws DocumentException
	 */
	protected void addHeaderLabel(Document doc, UserDataVO user) throws DocumentException {
		Paragraph par = new Paragraph(new Chunk("", baseFont));
		par.setSpacingAfter(PAGE_MARGIN_TOP);
		doc.add(par);
		
		StringBuffer address = new StringBuffer("Dear ");
		if (StringUtil.checkVal(user.getPrefixName()).length() > 0)
			address.append(user.getPrefixName()).append(" ");
		address.append(StringUtil.checkVal(user.getFirstName(), "Patient"));
		address.append(" ").append(StringUtil.checkVal(user.getLastName()));
		address.append(",");
		
		par = new Paragraph(new Chunk(address.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		
		par.setIndentationLeft(PAGE_MARGIN_LEFT);
		doc.add(par);
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
		String file = path + "oadtc_2011q2_" + joint + "_loc.pdf";
		return file;
	}
	
	
	/**
	 * Adds the locator page form a template, gets the closest surgeons and 
	 * adds a table with the surgeons to the document
	 * @param writer
	 * @param cb
	 * @param path
	 * @throws IOException
	 */
	protected void addLocatorResults(Document doc, List<SurgeonVO> surgeons) 
	throws IOException, DocumentException {
		PdfPTable table = new PdfPTable(2);
		//table.setTotalWidth(500);
		table.setWidthPercentage(88);
		table.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.getDefaultCell().setFixedHeight(14);
		table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
		table.getDefaultCell().setBorderWidth(0);

		// Calculate the numbers
		int half = (int) surgeons.size() / 2;
		if ((surgeons.size() % 2) == 1) half--;
		log.debug("Half: " + half + "|" + surgeons.size());
		
		for (int i=0; i < half; i++) {
			log.debug("Numbers: " + i + "|" + (i + half));
			SurgeonVO first = surgeons.get(i);
			SurgeonVO second = surgeons.get(i + half);
			
			for (int j=0; j < 7; j++) {
				this.genTableCell(table, j, first, baseFont);
				this.genTableCell(table, j, second, baseFont);
			}
			
			if (i < half) {
				PdfPCell cell = new PdfPCell(new Phrase(""));
				cell.setFixedHeight(10);
				cell.setBorderWidth(0);
				table.addCell(cell);
				table.addCell(cell);
			}
		}
		
		//table.writeSelectedRows(0, -1, 250, 500, cb);
		doc.add(table);
		
	}

	/**
	 * Retrieves a list of surgeons for the provided user
	 * @param user
	 * @return
	 * @throws InvalidDataException
	 */
	public List<SurgeonVO> returnSurgeons(UserDataVO user, String specialty) 
	throws InvalidDataException {
		// Store the user data 
		Location loc = user.getLocation();
		
		String specialtyCode = KNEE_SEARCH;
		if ("HIP".equalsIgnoreCase(specialty)) specialtyCode = HIP_SEARCH;
		
		//Get the class for the surgeon list creator
		AbstractSurgeonList asl = SurgeonListFactory.getSurgeonList("DEPUY");

		// Retrieve the surgeons and ensure enough were returned
		List<SurgeonVO> surgeons = asl.getSurgeons((GeocodeLocation) loc, null, specialtyCode);
		
		log.debug("Surgeons Returned: " + surgeons.size());
		return surgeons;
	}
	
	/**
	 * builds an individual cell in the table.  Makes adjustments if the user 
	 * doesn't have a second address
	 * @param table
	 * @param count
	 * @param surgeon
	 */
	public void genTableCell(PdfPTable table, int count, SurgeonVO surgeon, Font baseFont) {
		switch(count) {
			case 0:
				table.addCell(new Phrase(surgeon.getClinicName(), baseFont));
				break;
				
			case 1:
				table.addCell(new Phrase(surgeon.getFullName(), baseFont));
				break;
				
			case 2:
				table.addCell(new Phrase(surgeon.getAddress(), baseFont));
				break;
				
			case 3:
				if (StringUtil.checkVal(surgeon.getAddress2()).length() > 0) {
					table.addCell(new Phrase(surgeon.getAddress2(), baseFont));
				} else {
					table.addCell(new Phrase(surgeon.getCityStateZip(), baseFont));
				}
				break;
			case 4:
				if (StringUtil.checkVal(surgeon.getAddress2()).length() > 0) {
					table.addCell(new Phrase(surgeon.getCityStateZip(), baseFont));
				} else {
					table.addCell(new Phrase(surgeon.getPhone(), baseFont));
				}
				break;
			case 5:
				if (StringUtil.checkVal(surgeon.getAddress2()).length() > 0) {
					table.addCell(new Phrase(surgeon.getPhone(), baseFont));
				} else {
					table.addCell(new Phrase("Approx miles to surgeon: " + surgeon.getMiles(), baseFont));
				}
				break;
			case 6:
				if (StringUtil.checkVal(surgeon.getAddress2()).length() > 0) {
					table.addCell(new Phrase("Approx miles to surgeon: " + surgeon.getMiles(), baseFont));
				} else {
					table.addCell("");
				}
				break;
			case 7:
				table.addCell(new Phrase("Clinic Name", baseFont));
				break;

		}
	}
}
