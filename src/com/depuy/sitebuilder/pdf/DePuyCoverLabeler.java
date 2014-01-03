package com.depuy.sitebuilder.pdf;

// JDK 1.6.x
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// iText 2.1.x
import com.depuy.datafeed.SurgeonListFactory;
import com.depuy.datafeed.da.AbstractSurgeonList;
import com.depuy.datafeed.da.SurgeonVO;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

// Site Builder Imports
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.Location;
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
public class DePuyCoverLabeler extends AbstractLabelPDF {

	private final Map<String, Integer[]> positions;
	public static final String DEPUY_FONT = "font/fruitiger/FrutigerLTStd-Light.otf";
	protected static final String KNEE_SEARCH = "5";
	protected static final String HIP_SEARCH = "4";
	private BaseFont bf = null;
	
	/**
	 * 
	 */
	public DePuyCoverLabeler() {
		//X position is measured up from the bottom.  Y is from left edge.
		//a smaller number would move the text down, or left.
		positions = new HashMap<String, Integer[]>();
		positions.put("knee_self", 	new Integer[]{ 700,255 });
		positions.put("knee_other", new Integer[]{ 700,255 });
		positions.put("knee_hcp", 	new Integer[]{ 700,255 });
		positions.put("hip_self", 	new Integer[]{ 700,255 });
		positions.put("hip_other", 	new Integer[]{ 700,255 });
		positions.put("hip_hcp", 	new Integer[]{ 700,255 });
		
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.pdf.AbstractLabelPDF#labelPDFFile(java.util.Map, java.lang.String)
	 */
	@Override
	public byte[] labelPDFFile(Map<String, Object> params, String path)
	throws PDFLabelException {
		// Get the user information
		UserDataVO user = (UserDataVO) params.get(Constants.USER_DATA);
		if (user == null) user = new UserDataVO();
		
		// Build the messages
		String name = StringUtil.capitalize(user.getFirstName()) + " " + StringUtil.capitalize(user.getLastName());
		StringBuffer sal = new StringBuffer("");
		if (name.length() > 2) {
			sal.append("Dear ");
			String prefixNm = StringUtil.capitalize(user.getPrefixName());
			if (prefixNm.length() > 0) sal.append(prefixNm).append(" ");
			sal.append(name).append(", ");
		}
		
		// Retrieve the cover letter
		String joint = StringUtil.checkVal(attributes.get("JOINT"), "knee").toLowerCase();
		PdfReader reader = null;
		String[] ctc = (String[])params.get("callTargetCode");
		String callTargetCode = "self";
		if (ctc != null) callTargetCode = StringUtil.checkVal(ctc[0], "self").toLowerCase();
		String file = null;
		try {
			file = path + (joint + "_" + callTargetCode + "_cover.pdf");
			
			log.debug("File to retrieve: " + file);
			reader = new PdfReader(file);
		} catch(Exception e) {
			throw new PDFLabelException("Unable to Retrieve PDF File for Labeling: " + file, e);
		}

        // we retrieve the size of the first page
        Rectangle psize = reader.getPageSize(1);
        float width = psize.getHeight();
        float height = psize.getWidth();
        log.debug("page size= " + width + "x" + height);

        // Printing location
        int vertLoc = 0;
        int horLoc = 0;
        Integer[] locs = positions.get(joint + "_" + callTargetCode);
        if (locs != null) {
        	vertLoc = locs[0];
        	horLoc = locs[1];
        }
        log.debug("printing text at: " + vertLoc + "x" + horLoc);
        
        // step 1: creation of a document-object
        Document document = new Document(new Rectangle(height, width));
        
        // step 2: we create a writer that listens to the document
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = null;
		try {
			writer = PdfWriter.getInstance(document, baos);
		} catch(Exception e) {
			throw new PDFLabelException("Unable to create new PDF File", e);
		}
		
        // step 3: we open the document
        document.open();
        
        // step 4: we add content
        PdfContentByte cb = writer.getDirectContent();
        
        // Import the pages for editing
        PdfImportedPage page1 = writer.getImportedPage(reader, 1);
        PdfImportedPage page2 = null;
        if (reader.getNumberOfPages() == 2) 
        	page2 = writer.getImportedPage(reader, 2);
        
        // Add the first page to the new document
        cb.addTemplate(page1, 1f, 0, 0, 1f, 0, 0);
        
        // Create the fonts
        try {
        	bf = BaseFont.createFont(path + DEPUY_FONT, BaseFont.WINANSI, BaseFont.EMBEDDED);
            cb.beginText();
            cb.setFontAndSize(bf, 10);
            cb.showTextAligned(PdfContentByte.ALIGN_LEFT, sal.toString(), horLoc, vertLoc, 0);
        } catch(Exception e) {}

        cb.endText();
        
        // Add a new page to the document
        document.newPage();
        
        // Add the 2nd page
        if (reader.getNumberOfPages() == 2)
        	cb.addTemplate(page2, 1f, 0, 0, 1f, 0, 0);
        
        // Add the locator data
        Boolean validUser = (Boolean) params.get("VALID_USER");
        log.debug("Valid User: " + validUser);
        if (validUser) {
	        try {
	    		document.newPage();
	    		PdfReader rdr = new PdfReader(path + joint + "_locator.pdf");
	    		PdfImportedPage page3 = writer.getImportedPage(rdr, 1);
	    		cb.addTemplate(page3, 1, 0, 0, 1, 0, 0);
	    		
	    		// get the surgeons
	    		List<SurgeonVO> surgeons = this.returnSurgeons(user, joint);
	    		this.addLocatorResults(cb, surgeons);
	        } catch (Exception e) {
	        	log.error("Unable to add locator page", e);
	        }
        }
        
        // step 5: we close the document
        document.close();
        
		return baos.toByteArray();
	}
	
	
	/**
	 * Adds the locator page form a template, gets the closest surgeons and 
	 * adds a table with the surgeons to the document
	 * @param writer
	 * @param cb
	 * @param path
	 * @throws IOException
	 */
	protected void addLocatorResults(PdfContentByte cb, List<SurgeonVO> surgeons) 
	throws IOException, DocumentException {
		PdfPTable table = new PdfPTable(2);
		table.setTotalWidth(375);
		table.setHorizontalAlignment(Element.ALIGN_LEFT);
		table.getDefaultCell().setFixedHeight(14);
		table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
		table.getDefaultCell().setBorderWidth(0);
		Font baseFont = new Font(bf);
		baseFont.setSize(9);

		// Calculate the numbers
		int half = (int) surgeons.size() / 2;
		if ((surgeons.size() % 2) == 0) half--;
		
		for (int i=0; i <= half; i++) {
			
			SurgeonVO first = surgeons.get(i);
			SurgeonVO second = surgeons.get(i + half);
			
			for (int j=0; j < 6; j++) {
				this.genTableCell(table, j, first, baseFont);
				this.genTableCell(table, j, second, baseFont);
			}
			
			table.addCell("");
			table.addCell("");
		}
		
		table.writeSelectedRows(0, -1, 250, 500, cb);
		
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
		
		// Make sure there is a valid address
		if (! loc.isValidAddress()) {
			throw new InvalidDataException("INVALID_ADDRESS");
		} else if (! loc.isCassValidated()) {
			throw new InvalidDataException("NOT_CASS_VALIDATED");
		}
		
		//Get the class for the surgeon list creator
		AbstractSurgeonList asl = SurgeonListFactory.getSurgeonList("DEPUY");

		// Retrieve the surgeons and ensure enough were returned
		List<SurgeonVO> surgeons = asl.getSurgeons((GeocodeLocation) loc, null, specialtyCode);
		
		if (surgeons.size() > 8) {
			log.debug("Number of surgeons: " + surgeons.size());
			surgeons = surgeons.subList(0, 8);
		} else if (surgeons.size() < 8) {
			log.error("Error Adding to fulfillment: " + user);
		}
		
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
			table.addCell(new Phrase(surgeon.getFullName(), baseFont));
			break;
		case 1:
			table.addCell(new Phrase(surgeon.getAddress(), baseFont));
			break;
		case 2:
			if (StringUtil.checkVal(surgeon.getAddress2()).length() > 0) {
				table.addCell(new Phrase(surgeon.getAddress2(), baseFont));
			} else {
				table.addCell(new Phrase(surgeon.getCityStateZip(), baseFont));
			}
			break;
		case 3:
			if (StringUtil.checkVal(surgeon.getAddress2()).length() > 0) {
				table.addCell(new Phrase(surgeon.getCityStateZip(), baseFont));
			} else {
				table.addCell(new Phrase(surgeon.getPhone(), baseFont));
			}
			break;
		case 4:
			if (StringUtil.checkVal(surgeon.getAddress2()).length() > 0) {
				table.addCell(new Phrase(surgeon.getPhone(), baseFont));
			} else {
				table.addCell(new Phrase("Approx miles to surgeon: " + surgeon.getMiles(), baseFont));
			}
			break;
		case 5:
			if (StringUtil.checkVal(surgeon.getAddress2()).length() > 0) {
				table.addCell(new Phrase("Approx miles to surgeon: " + surgeon.getMiles(), baseFont));
			} else {
				table.addCell("");
			}
			break;
			
	}
	}
}
